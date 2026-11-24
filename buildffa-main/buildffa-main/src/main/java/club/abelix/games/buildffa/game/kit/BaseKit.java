package club.abelix.games.buildffa.game.kit;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.Kit;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.language.Language;
import club.abelix.games.buildffa.game.dto.SkinDTO;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.manager.event.EventItems;
import org.bukkit.Bukkit;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import club.abelix.games.buildffa.game.dto.dao.SkinDAO;
import club.abelix.games.buildffa.game.dto.dao.impl.KitDAOImpl;
import club.abelix.games.buildffa.game.dto.dao.impl.SkinDAOImpl;
import club.abelix.games.buildffa.game.manager.additional.AdditionalItemManager;
import club.abelix.games.buildffa.game.utility.ConvertUtility;
import club.abelix.network.type.mode.Uncommon;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class BaseKit extends Kit {
    private volatile BuildFFAGame inventoryLifecycle;
    private final Map<UUID, Map<Integer, Integer>> loadedSlots = new HashMap<>();
    @Getter
    private final TObjectIntHashMap<ItemStack> items = new TObjectIntHashMap<>();
    @Getter
    private final String name;
    private final Material material;
    @Getter
    private final int price;
    private final SkinDAO skinDAO;
    @Getter
    private ItemStack[] armor;
    @Getter
    protected String freePermission;

    protected BaseKit(
            final int id,
            final @NotNull String localizationKey,
            final @NotNull KitCategory category,
            final @NotNull String name,
            final @NotNull Material material,
            final int price,
            final @Nullable String freePermission
    ) {
        super(id, localizationKey, category);

        this.name = name;
        this.material = material;
        this.price = price;
        this.skinDAO = SkinDAOImpl.getInstance();
        this.freePermission = freePermission;
    }

    protected BaseKit(
            final int id,
            final @NotNull String localizationKey,
            final @NotNull KitCategory category,
            final @NotNull String name,
            final @NotNull Material material,
            final int price
    ) {
        this(id, localizationKey, category, name, material, price, null);
    }

    @Override
    public ItemStack getIcon(Language language) {
        val kitName = language.getMessage("gui_kit_name", "%kit_name%", name);
        val kitLore = language.getMessageList("gui_kit_lore_with_price", "%kit_price%", price, "%golds%", ConvertUtility.toGold(price));

        return ItemUtil.getBuilder(material)
                .setName(ChatColor.translateAlternateColorCodes('&', kitName))
                .setLore(price == 0 ? Collections.singletonList("§f") : kitLore)
                .build();
    }

    public void addItem(final ItemStack item, int defaultSlot) {
        this.items.put(unbreakableCopy(item), defaultSlot);
    }

    @Override
    public void apply(Gamer gamer) {
        BuildFFAGame game = inventoryLifecycle;
        if (game == null) throw new IllegalStateException("Kit inventory lifecycle is not bound");
        apply(gamer, game, game.beginInventoryRequest(gamer.getPlayer()));
    }

    public void bindInventoryLifecycle(BuildFFAGame game) {
        inventoryLifecycle = Objects.requireNonNull(game, "game");
    }

    public void apply(Gamer gamer, BuildFFAGame game, UUID request) {
        bindInventoryLifecycle(game);
        long revision = game.getEventManager().getInventoryRevision();
        Runnable apply = () -> applyInventory(gamer, game, request, revision);
        if (Bukkit.isPrimaryThread()) apply.run();
        else if (game.getPlugin().isEnabled()) Bukkit.getScheduler().runTask(game.getPlugin(), apply);
    }

    private boolean validInventoryRequest(Player player, BuildFFAGame game, UUID request, long revision) {
        return !game.isSpectating(player) && !game.isBuildMode(player) && game.isInventoryRequestCurrent(player, request)
                && revision == game.getEventManager().getInventoryRevision()
                && player.getGameMode() == org.bukkit.GameMode.SURVIVAL
                && player.getWorld().equals(game.getWorldConfig().world()) && !game.isLobbyZone(player.getLocation());
    }

    private void discardInventoryRequest(Player player, BuildFFAGame game, UUID request) {
        if (!game.isInventoryRequestCurrent(player, request)) return;
        game.completeInventoryRequest(player, request);
        game.resetPlayerKitApplication(player);
    }

    private void applyInventory(Gamer gamer, BuildFFAGame game, UUID request, long revision) {
        Player player = gamer.getPlayer();
        if (!game.getPlugin().isEnabled() || !validInventoryRequest(player, game, request, revision)) {
            discardInventoryRequest(player, game, request);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        CompletableFuture<SkinDTO> skinFuture = skinDAO.getSelectedSkin(gamer.getPlayerId());
        CompletableFuture<Map<Integer, Integer>> slotsFuture = KitDAOImpl.INSTANCE.loadItemSlots(
                gamer.getPlayerId(), String.valueOf(getId()));
        CompletableFuture.allOf(skinFuture, slotsFuture).whenComplete((ignored, failure) -> {
            if (!game.getPlugin().isEnabled()) return;
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                if (!validInventoryRequest(player, game, request, revision) || failure != null) {
                    discardInventoryRequest(player, game, request);
                    if (failure != null) game.getPlugin().getLogger().log(java.util.logging.Level.WARNING,
                            "Could not load BuildFFA kit inventory", failure);
                    return;
                }
                SkinDTO skin = skinFuture.getNow(null);
                Map<Integer, Integer> slots = slotsFuture.getNow(Collections.emptyMap());
                rememberSlots(player, slots);
                getKitItems().forEach((stack, defaultSlot) -> {
                    int slot = slots.getOrDefault(stack.hashCode(), defaultSlot);
                    if (slot < 0 || slot >= 36) slot = defaultSlot;
                    if (slot < 0 || slot >= 36) return;
                    ItemStack item = stack.clone();
                    if (item.getType() == Material.SANDSTONE && skin != null && skin.getMaterial() != null) {
                        item.setType(skin.getMaterial());
                        item.setDurability(skin.getMaterialData());
                    }
                    inventory.setItem(slot, markGameplayItem(item, getId()));
                });
                applyArmor(inventory);
                giveAdditionalItems(player);
                game.getEventManager().onInventoryReady(player);
                game.completeInventoryRequest(player, request);
                player.updateInventory();
            });
        });
    }

    private void rememberSlots(Player player, Map<Integer, Integer> slots) {
        cacheItemSlots(player.getUniqueId(), slots);
    }

    public void cacheItemSlots(UUID playerId, Map<Integer, Integer> slots) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kit slot caches must be updated on the server thread");
        loadedSlots.put(playerId, new HashMap<>(slots));
    }

    public void restoreCombatItems(Gamer gamer, BuildFFAGame game) {
        Player player = gamer.getPlayer();
        UUID request = game.beginInventoryRequest(player);
        long revision = game.getEventManager().getInventoryRevision();
        Runnable restore = () -> {
            if (!validInventoryRequest(player, game, request, revision)) {
                game.completeInventoryRequest(player, request);
                return;
            }
            if (!game.getPlugin().isEnabled()) {
                restoreCombatItems(player, loadedSlots.getOrDefault(player.getUniqueId(), Collections.emptyMap()));
                game.completeInventoryRequest(player, request);
                return;
            }
            KitDAOImpl.INSTANCE.loadItemSlots(gamer.getPlayerId(), String.valueOf(getId())).whenComplete((slots, failure) -> {
                if (!game.getPlugin().isEnabled()) return;
                Bukkit.getScheduler().runTask(game.getPlugin(), () -> {
                    if (!validInventoryRequest(player, game, request, revision)
                            || game.getSelectedKit(Gamer.getGamer(player)).getId() != getId()) {
                        game.completeInventoryRequest(player, request);
                        return;
                    }
                    if (failure != null) {
                        game.getPlugin().getLogger().log(java.util.logging.Level.WARNING,
                                "Could not load BuildFFA combat item slots", failure);
                        game.completeInventoryRequest(player, request);
                        return;
                    }
                    rememberSlots(player, slots);
                    restoreCombatItems(player, slots);
                    game.completeInventoryRequest(player, request);
                });
            });
        };
        if (Bukkit.isPrimaryThread()) restore.run();
        else if (game.getPlugin().isEnabled()) Bukkit.getScheduler().runTask(game.getPlugin(), restore);
    }

    private void restoreCombatItems(Player player, Map<Integer, Integer> slots) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (EventItems.isKind(item, "roulette") || EventItems.isKind(item, "arrows")) inventory.setItem(slot, null);
        }
        List<ItemStack> definitions = new ArrayList<>();
        getKitItems().forEach((stack, defaultSlot) -> {
            Material type = stack.getType();
            if (!type.name().endsWith("_SWORD") && !type.name().endsWith("_AXE") && type != Material.BOW) return;
            definitions.add(stack);
            int required = 0;
            for (ItemStack definition : definitions) if (definition.isSimilar(stack)) required += definition.getAmount();
            int existing = 0;
            for (int index = 0; index < 36; index++) {
                ItemStack item = inventory.getItem(index);
                if (!EventItems.isTemporary(item) && matchesDefinition(stack, item)) existing += item.getAmount();
            }
            if (existing >= required) return;
            int slot = slots.getOrDefault(stack.hashCode(), defaultSlot);
            if (slot < 0 || slot >= 36) slot = defaultSlot;
            if (slot < 0 || slot >= 36) return;
            ItemStack current = inventory.getItem(slot);
            if (current != null && current.getType() != Material.AIR) slot = inventory.firstEmpty();
            if (slot >= 0 && slot < 36) {
                ItemStack item = stack.clone();
                item.setAmount(Math.min(item.getMaxStackSize(), required - existing));
                inventory.setItem(slot, markGameplayItem(item, getId()));
            }
        });
        player.updateInventory();
    }

    @Override
    public String getDisplayName(@NonNull Language language) {
        return name; // FIXME сделать нормально, через локализацию
    }

    private void applyArmor(PlayerInventory inventory) {
        ItemStack[] applied = new ItemStack[this.armor.length];
        for (int index = 0; index < this.armor.length; index++)
            applied[index] = markGameplayItem(this.armor[index], getId());
        inventory.setArmorContents(applied);
    }

    public boolean isAvailableTo(Player player) {
        Gamer gamer = Gamer.getGamer(player);

        if (getId() == 8) return true;
        if (gamer.getRanks().hasPermission("buildffa.kits.bypass")) return true;
        if (freePermission != null && gamer.getRanks().hasPermission(freePermission)) return true;

        try {
            return KitDAOImpl.INSTANCE.isKitPurchased(gamer.getPlayerId(), String.valueOf(getId())).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean purchaseKit(Player player) {
        val gamer = Gamer.getGamer(player);

        KitDAOImpl.INSTANCE.addPlayerKit(gamer.getPlayerId(), String.valueOf(this.getId()));

        return true;
    }

    public abstract void onSelect(Player player);

    public Map<ItemStack, Integer> getKitItems() {
        Map<ItemStack, Integer> map = new HashMap<>();
        items.forEachEntry((item, index) -> {
            map.put(item, index);
            return true;
        });
        return map;
    }

    private void giveAdditionalItems(Player player) {
        AdditionalItemManager itemManager = AdditionalItemManager.getInstance();
        Map<ItemStack, Integer> items = itemManager.getPlayerItems(player);

        for (Map.Entry<ItemStack, Integer> entry : items.entrySet()) {
            ItemStack item = entry.getKey().clone();
            item.setAmount(entry.getValue());
            player.getInventory().addItem(item);
        }

        itemManager.clearPlayerItems(player);
    }

    private static ItemStack unbreakableCopy(final ItemStack itemStack) {
        return ItemUtil.getBuilder(itemStack.clone()).setUnbreakable().build();
    }

    public void setArmor(final ItemStack[] armor) {
        for (int i = 0; i < armor.length; i++) {
            armor[i] = unbreakableCopy(armor[i]);
        }

        this.armor = armor;
    }
    private static final String KIT_ID_TAG = "buildffa_kit_id";
    private static final String EDITOR_TAG = "buildffa_kit_editor";

    public static ItemStack markGameplayItem(ItemStack source, int kitId) {
        return markItem(source, kitId, null);
    }

    public static ItemStack markEditorItem(ItemStack source, int kitId, UUID editor) {
        return markItem(source, kitId, editor);
    }

    private static ItemStack markItem(ItemStack source, int kitId, UUID editor) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kit items must be tagged on the server thread");
        if (source == null || source.getType() == Material.AIR) return source;
        ItemStack stack = craftCopy(source);
        NbtCompound tag = (NbtCompound) NbtFactory.fromItemTag(stack);
        if (tag == null) tag = NbtFactory.ofCompound("");
        tag.put(KIT_ID_TAG, kitId);
        if (editor == null) tag.remove(EDITOR_TAG);
        else tag.put(EDITOR_TAG, editor.toString());
        NbtFactory.setItemTag(stack, tag);
        return stack;
    }

    public static boolean isProtectedKitItem(ItemStack stack) {
        NbtCompound tag = itemTag(stack);
        return tag != null && tag.containsKey(KIT_ID_TAG);
    }

    public static boolean belongsToEditor(ItemStack stack, UUID editor) {
        NbtCompound tag = itemTag(stack);
        return tag != null && tag.containsKey(KIT_ID_TAG) && tag.containsKey(EDITOR_TAG)
                && editor.toString().equals(tag.getString(EDITOR_TAG));
    }

    public static boolean matchesDefinition(ItemStack definition, ItemStack applied) {
        if (!isProtectedKitItem(applied)) return definition.isSimilar(applied);
        ItemStack copy = craftCopy(applied);
        NbtCompound tag = (NbtCompound) NbtFactory.fromItemTag(copy);
        tag.remove(KIT_ID_TAG);
        tag.remove(EDITOR_TAG);
        NbtFactory.setItemTag(copy, tag);
        return definition.isSimilar(copy);
    }

    private static NbtCompound itemTag(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        ItemStack craft = MinecraftReflection.getBukkitItemStack(MinecraftReflection.getMinecraftItemStack(stack));
        return (NbtCompound) NbtFactory.fromItemOptional(craft).orElse(null);
    }

    private static ItemStack craftCopy(ItemStack source) {
        try {
            return (ItemStack) MinecraftReflection.getCraftItemStackClass()
                    .getMethod("asCraftCopy", ItemStack.class).invoke(null, source);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot copy BuildFFA kit item", exception);
        }
    }

}
