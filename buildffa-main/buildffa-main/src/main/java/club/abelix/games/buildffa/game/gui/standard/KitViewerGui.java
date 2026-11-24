package club.abelix.games.buildffa.game.gui.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.games.buildffa.game.dto.KitDTO;
import club.abelix.games.buildffa.game.dto.dao.impl.KitDAOImpl;
import club.abelix.games.buildffa.game.kit.BaseKit;
import club.abelix.games.buildffa.game.manager.event.EventItems;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import java.util.concurrent.CompletableFuture;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class KitViewerGui extends Gui {

    static final int INVENTORY_EDIT_END = 35;
    static final int PANEL_START = 36;
    static final int PANEL_END = 44;
    static final int ARMOR_START = 45;
    static final int ARMOR_END = 53;

    UUID editorId = UUID.randomUUID();
    @NonFinal
    int cursorOrigin = -1;
    BaseKit kit;
    int playerId;
    Plugin plugin;
    @NonFinal
    boolean loaded;
    @NonFinal
    boolean closed;
    @NonFinal
    boolean opened;
    @NonFinal
    boolean edited;
    @NonFinal
    long menuSession;
    @NonFinal
    List<KitDTO> lastLayout = new ArrayList<>();
    @NonFinal
    CompletableFuture<Void> layoutWrites = CompletableFuture.completedFuture(null);

    public KitViewerGui(
            final @NotNull Player player,
            final @NotNull BaseKit kit
    ) {
        super(player, Lang.of("buildffa_kit_viewer_gui_title").set("%inventory%", kit.getName()), 6);

        this.kit = kit;
        this.playerId = Gamer.getGamer(player).getPlayerId();
        this.plugin = JavaPlugin.getProvidingPlugin(KitViewerGui.class);
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kit editors must be constructed on the server thread");

        setAllowPlayerInvInteraction(false);

        for (int i = 0; i < this.getSize(); i++) {
            add(null, i, createClickAction());
        }

        populateKitInventory(new HashMap<>());
        loaded = true;
        lastLayout = captureKitLayout();
        draw();
        loadKitInventory();
    }

    private ClickAction createClickAction() {
        return event -> handleEditorClick(event);
    }

    public void handleEditorClick(InventoryClickEvent event) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kit editor clicks must run on the server thread");
        int slot = event.getRawSlot();
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean valid = event.getWhoClicked().getUniqueId().equals(player.getUniqueId())
                && isEditableSlot(slot) && !event.isShiftClick() && event.getHotbarButton() < 0;
        if (valid) {
            switch (event.getAction()) {
                case PICKUP_ALL:
                    valid = isEmpty(cursor) && isEditableItem(current);
                    if (valid) cursorOrigin = slot;
                    break;
                case PICKUP_HALF:
                case PICKUP_ONE:
                    valid = isEmpty(cursor) && isEditableItem(current) && current.getAmount() == 1;
                    if (valid) cursorOrigin = slot;
                    break;
                case PLACE_ONE:
                    valid = isEmpty(current) && isEditableItem(cursor) && cursor.getAmount() == 1;
                    break;
                case PLACE_ALL:
                    valid = isEmpty(current) && isEditableItem(cursor);
                    break;
                case SWAP_WITH_CURSOR:
                    valid = isEditableItem(current) && isEditableItem(cursor);
                    break;
                case NOTHING:
                    valid = (isEmpty(current) || isEditableItem(current)) && (isEmpty(cursor) || isEditableItem(cursor));
                    break;
                default:
                    valid = false;
            }
        }
        event.setCancelled(!valid);
    }

    public void handleEditorDrag(InventoryDragEvent event) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kit editor drags must run on the server thread");
        boolean valid = event.getWhoClicked().getUniqueId().equals(player.getUniqueId())
                && isEditableItem(event.getOldCursor()) && isEmpty(event.getCursor()) && event.getNewItems().size() == 1;
        if (valid) {
            Map.Entry<Integer, ItemStack> target = event.getNewItems().entrySet().iterator().next();
            valid = isEditableSlot(target.getKey()) && isEmpty(getInventory().getItem(target.getKey()))
                    && isEditableItem(target.getValue()) && target.getValue().getAmount() == event.getOldCursor().getAmount();
        }
        event.setCancelled(!valid);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0;
    }

    private boolean isEditableItem(ItemStack item) {
        if (!BaseKit.belongsToEditor(item, editorId) || EventItems.isTemporary(item)) return false;
        for (ItemStack definition : kit.getKitItems().keySet())
            if (BaseKit.matchesDefinition(definition, item) && definition.getAmount() == item.getAmount()) return true;
        return false;
    }

    public boolean isEditableSlot(int slot) {
        return slot >= 0 && slot <= INVENTORY_EDIT_END;
    }

    private boolean isPanelSlot(int slot) {
        return slot >= PANEL_START && slot <= PANEL_END;
    }

    private boolean isArmorSlot(int slot) {
        return slot >= ARMOR_START && slot <= ARMOR_END;
    }

    private boolean isValidKitItem(ItemStack item) {
        return isEditableItem(item);
    }

    private void updateKitLayout() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kit layouts must be captured on the server thread");
        if (!loaded) return;
        List<KitDTO> updates = captureKitLayout();
        if (!isCompleteLayout(updates)) return;
        if (sameLayout(updates, lastLayout)) return;
        lastLayout = updates;
        edited = true;
        saveKitLayout(updates);
    }

    public void queueLayoutUpdate() {
        edited = true;
        long session = menuSession;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (session == menuSession && !closed && player.isOnline()
                    && player.getOpenInventory().getTopInventory().getHolder() == this) updateKitLayout();
        });
    }

    private List<KitDTO> captureKitLayout() {
        List<KitDTO> updates = new ArrayList<>();
        for (int i = 0; i <= INVENTORY_EDIT_END; i++) {
            ItemStack item = this.getInventory().getItem(i);
            if (isValidKitItem(item)) {
                KitDTO kitDTO = KitDTO.builder()
                        .playerId(playerId)
                        .kitId(kit.getId())
                        .itemHash(getDefinitionHash(item))
                        .slot(i)
                        .build();
                updates.add(kitDTO);
            }
        }
        return updates;
    }

    private boolean sameLayout(List<KitDTO> first, List<KitDTO> second) {
        if (first.size() != second.size()) return false;
        for (int index = 0; index < first.size(); index++) {
            KitDTO a = first.get(index);
            KitDTO b = second.get(index);
            if (a.getItemHash() != b.getItemHash() || a.getSlot() != b.getSlot()) return false;
        }
        return true;
    }

    private boolean isCompleteLayout(List<KitDTO> updates) {
        Map<ItemStack, Integer> definitions = kit.getKitItems();
        if (updates.size() != definitions.size()) return false;
        java.util.Set<Integer> hashes = new java.util.HashSet<>();
        for (KitDTO update : updates) if (!hashes.add(update.getItemHash())) return false;
        for (ItemStack definition : definitions.keySet()) {
            boolean found = false;
            for (KitDTO update : updates) {
                ItemStack item = getInventory().getItem(update.getSlot());
                if (BaseKit.matchesDefinition(definition, item) && definition.getAmount() == item.getAmount()) found = true;
            }
            if (!found) return false;
        }
        return true;
    }

    private void saveKitLayout(List<KitDTO> updates) {
        if (updates.isEmpty() || !plugin.isEnabled()) return;
        Map<Integer, Integer> slots = new HashMap<>();
        for (KitDTO update : updates) slots.put(update.getItemHash(), update.getSlot());
        kit.cacheItemSlots(player.getUniqueId(), slots);
        layoutWrites = layoutWrites.handle((ignored, failure) -> null).thenCompose(ignored -> {
            CompletableFuture<Void> saved = new CompletableFuture<>();
            if (!plugin.isEnabled()) {
                saved.complete(null);
                return saved;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                CompletableFuture<?>[] writes = updates.stream()
                        .map(KitDAOImpl.INSTANCE::saveItemSlotAsync).toArray(CompletableFuture[]::new);
                CompletableFuture.allOf(writes).whenComplete((result, failure) -> {
                    if (failure == null) saved.complete(null);
                    else {
                        plugin.getLogger().log(java.util.logging.Level.WARNING, "Could not save BuildFFA kit layout", failure);
                        saved.completeExceptionally(failure);
                    }
                });
            });
            return saved;
        });
    }

    private int getDefinitionHash(ItemStack item) {
        for (ItemStack definition : kit.getKitItems().keySet())
            if (BaseKit.matchesDefinition(definition, item)) return definition.hashCode();
        throw new IllegalArgumentException("Unknown kit item");
    }

    private void loadKitInventory() {
        long session = menuSession;
        KitDAOImpl.INSTANCE.loadItemSlots(playerId, String.valueOf(kit.getId())).whenComplete((slots, failure) -> {
            if (failure != null) {
                plugin.getLogger().log(java.util.logging.Level.WARNING, "Could not load BuildFFA kit layout; keeping current layout", failure);
                return;
            }
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (session != menuSession || closed || edited || !player.isOnline()) return;
                if (opened && player.getOpenInventory().getTopInventory().getHolder() != this) return;
                if (player.getItemOnCursor() != null && player.getItemOnCursor().getType() != Material.AIR) return;
                populateKitInventory(slots);
                lastLayout = captureKitLayout();
            });
        });
    }

    private void populateKitInventory(Map<Integer, Integer> slots) {
        for (int slot = 0; slot < getSize(); slot++) setEditorItem(null, slot);
        fillPanel();
        boolean[] occupied = new boolean[INVENTORY_EDIT_END + 1];
        List<Map.Entry<ItemStack, Integer>> entries = new ArrayList<>(kit.getKitItems().entrySet());
        entries.sort(java.util.Comparator.comparingInt(Map.Entry::getValue));
        for (Map.Entry<ItemStack, Integer> entry : entries) {
            ItemStack item = entry.getKey();
            int slot = slots.getOrDefault(item.hashCode(), entry.getValue());
            if (!isEditableSlot(slot) || occupied[slot]) slot = entry.getValue();
            if (!isEditableSlot(slot) || occupied[slot]) {
                slot = -1;
                for (int candidate = 0; candidate <= INVENTORY_EDIT_END; candidate++)
                    if (!occupied[candidate]) { slot = candidate; break; }
            }
            if (slot < 0) continue;
            occupied[slot] = true;
            setEditorItem(BaseKit.markEditorItem(item, kit.getId(), editorId), slot);
        }
        ItemStack[] armor = kit.getArmor();
        if (armor != null) for (int index = 0; index < armor.length && ARMOR_START + index <= ARMOR_END; index++)
            if (armor[index] != null) setEditorItem(BaseKit.markEditorItem(armor[index], kit.getId(), editorId), ARMOR_START + index);
    }

    private void setEditorItem(ItemStack item, int slot) {
        add(item, slot, createClickAction());
        getInventory().setItem(slot, item);
    }

    public void editorOpened() {
        if (plugin == null) return;
        menuSession++;
        opened = true;
        closed = false;
        if (!edited) loadKitInventory();
    }

    public void editorClosed() {
        if (plugin == null || closed) return;
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kit editors must close on the server thread");
        ItemStack cursor = player.getItemOnCursor();
        if (BaseKit.belongsToEditor(cursor, editorId)) {
            int slot = isEditableSlot(cursorOrigin) && isEmpty(getInventory().getItem(cursorOrigin)) ? cursorOrigin : -1;
            if (slot < 0) for (int candidate = 0; candidate <= INVENTORY_EDIT_END; candidate++)
                if (isEmpty(getInventory().getItem(candidate))) { slot = candidate; break; }
            if (slot >= 0 && isEditableItem(cursor)) setEditorItem(cursor, slot);
            else restoreCompleteLayout();
            player.setItemOnCursor(null);
        }
        if (!isCompleteLayout(captureKitLayout())) restoreCompleteLayout();
        updateKitLayout();
        cursorOrigin = -1;
        menuSession++;
        closed = true;
    }

    private void restoreCompleteLayout() {
        Map<Integer, Integer> slots = new HashMap<>();
        for (KitDTO entry : lastLayout) slots.put(entry.getItemHash(), entry.getSlot());
        populateKitInventory(slots);
    }

    private void fillPanel() {
        ItemStack frame = ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
                .setDurability(7)
                .setName(" ")
                .build();

        for (int slot = PANEL_START; slot <= PANEL_END; slot++) {
            setEditorItem(frame.clone(), slot);
        }
    }

    @Override
    public void draw() {
        if (!Bukkit.isPrimaryThread()) {
            long session = menuSession;
            if (plugin != null && plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> {
                if (session == menuSession && !closed) draw();
            });
            return;
        }
        super.draw();
    }

    public int getItemIndex(ItemStack item) {
        if (item == null || EventItems.isTemporary(item)) return -1;
        for (val entry : kit.getKitItems().entrySet()) {
            if (BaseKit.matchesDefinition(entry.getKey(), item)) {
                return entry.getValue();
            }
        }

        return -1;
    }
}
