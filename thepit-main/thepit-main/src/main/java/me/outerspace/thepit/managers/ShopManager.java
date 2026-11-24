package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopManager implements Listener {
    private static final String ITEM_TAG = "ThePitItem";
    private static final String ITEM_PREFIX = "pit_shop_";
    private final ThePitPlugin plugin;
    private final EquipmentUpgradeService equipmentUpgradeService = new EquipmentUpgradeService();
    private final Set<UUID> itemShopPurchaseProcessing = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ShopProfile> profiles = new ConcurrentHashMap<>();
    private final Set<UUID> loadedProfiles = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirtyProfiles = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> firstAidReady = new ConcurrentHashMap<>();
    private final Map<UUID, Long> solventUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> tacticalReady = new ConcurrentHashMap<>();
    private final Map<UUID, Location> tacticalLocations = new ConcurrentHashMap<>();
    private final Map<UUID, PickProgress> pickProgress = new HashMap<>();
    private final Set<UUID> pendingRespawn = ConcurrentHashMap.newKeySet();
    private final File fallbackFile;
    private YamlConfiguration fallback;
    private BukkitTask sharedTask;
    private long nextFlushAt;

    public ShopManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        fallbackFile = new File(plugin.getDataFolder(), "shop-profiles.yml");
        fallback = YamlConfiguration.loadConfiguration(fallbackFile);
    }

    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) load(player);
        if (sharedTask == null) sharedTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        if (sharedTask != null) {
            sharedTask.cancel();
            sharedTask = null;
        }
        for (Map.Entry<UUID, ShopProfile> entry : profiles.entrySet()) saveNow(entry.getKey(), entry.getValue().serialize());
        profiles.clear();
        loadedProfiles.clear();
        dirtyProfiles.clear();
        tacticalLocations.clear();
        pendingRespawn.clear();
        pickProgress.clear();
        itemShopPurchaseProcessing.clear();
    }

    public void reload() {
        for (Player player : Bukkit.getOnlinePlayers()) refreshIfOpen(player);
    }

    public void arenaCleanup() {
        tacticalLocations.clear();
        tacticalReady.clear();
        pickProgress.clear();
    }

    public void openShop(Player player) {
        if (player == null || !plugin.registry().shops().getBoolean("item-shop.enabled", true)) return;
        if (!loadedProfiles.contains(player.getUniqueId())) {
            player.sendMessage(CC.color("&cYour Item Shop profile is still loading."));
            return;
        }
        ShopHolder holder = new ShopHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, menuSize(), CC.color(plugin.registry().shops().getString("item-shop.title", "&8Item Shop")));
        holder.inventory = inventory;
        ItemStack filler = menuItem(material("item-shop.filler.material", Material.STAINED_GLASS_PANE),
                (short) plugin.registry().shops().getInt("item-shop.filler.data", 15),
                plugin.registry().shops().getString("item-shop.filler.name", " "), Collections.emptyList(), false);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        for (Definition definition : definitions()) {
            if (definition.slot < 0 || definition.slot >= inventory.getSize()) continue;
            inventory.setItem(definition.slot, icon(player, definition));
            holder.items.put(definition.slot, definition.id);
        }
        int goldSlot = plugin.registry().shops().getInt("item-shop.player-gold-slot", 40);
        if (goldSlot >= 0 && goldSlot < inventory.getSize()) {
            inventory.setItem(goldSlot, menuItem(Material.GOLD_INGOT, (short) 0, "&6Your Gold",
                    Arrays.asList("&7Balance: &6" + format(plugin.getPitGoldService().balance(player.getUniqueId())) + " Gold"), false));
        }
        int close = plugin.registry().shops().getInt("item-shop.close-slot", 44);
        if (close >= 0 && close < inventory.getSize()) inventory.setItem(close, menuItem(Material.BARRIER, (short) 0, "&cClose", Collections.emptyList(), false));
        player.openInventory(inventory);
    }

    public void refreshIfOpen(Player player) {
        if (player != null && player.getOpenInventory().getTopInventory().getHolder() instanceof ShopHolder) openShop(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
        InventoryHolder top = event.getView().getTopInventory().getHolder();
        if (top instanceof ShopHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player) || !holder.owner.equals(player.getUniqueId())) return;
            if (event.getRawSlot() == plugin.registry().shops().getInt("item-shop.close-slot", 44)) {
                player.closeInventory();
                return;
            }
            String id = holder.items.get(event.getRawSlot());
            if (id == null) return;
            if (event.isRightClick() && ownsAutobuy(player.getUniqueId())) toggleAutobuy(player, id);
            else purchase(player, id, false);
            return;
        }
        if (top instanceof BundleHolder holder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player) || !holder.owner.equals(player.getUniqueId())) return;
            if (event.getRawSlot() < 0 || event.getRawSlot() >= holder.inventory.getSize()) return;
            ShopProfile profile = profiles.get(player.getUniqueId());
            List<ItemStack> stored = profile == null ? null : profile.bundles.get(holder.bundleId);
            if (stored == null || event.getRawSlot() >= stored.size()) return;
            ItemStack item = stored.get(event.getRawSlot());
            if (!canFit(player.getInventory(), Collections.singletonList(item))) {
                player.sendMessage(CC.color("&cYour inventory is full."));
                return;
            }
            stored.remove(event.getRawSlot());
            player.getInventory().addItem(item.clone());
            saveAsync(player.getUniqueId());
            openBundle(player, holder.bundleId);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof ShopHolder || holder instanceof BundleHolder) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBundleInventoryMove(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        InventoryType type = event.getView().getTopInventory().getType();
        if (type == InventoryType.PLAYER || type == InventoryType.CRAFTING
                || event.getView().getTopInventory().getHolder() instanceof ShopHolder
                || event.getView().getTopInventory().getHolder() instanceof BundleHolder) return;
        if (!bundleId(event.getCurrentItem()).isEmpty() || !bundleId(event.getCursor()).isEmpty()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBundleDrop(PlayerDropItemEvent event) {
        if (!bundleId(event.getItemDrop().getItemStack()).isEmpty()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cPants Bundles cannot be dropped."));
        }
    }

    public boolean purchase(Player player, String rawId, boolean autobuy) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())
                || !loadedProfiles.contains(player.getUniqueId())) return false;
        Definition definition = definition(rawId);
        if (definition == null || !definition.enabled || autobuy && definition.id.equals("pants-bundle")
                && !plugin.registry().shops().getBoolean("item-shop.autobuy.allow-pants-bundle", false)) return false;
        if (definition.renown && (!isUnlocked(player.getUniqueId(), definition.id)
                || plugin.getStatsManager().getPrestige(player.getUniqueId()) < definition.requiredPrestige)) {
            if (!autobuy) player.sendMessage(CC.color("&cUnlock this item permanently in the Renown Shop."));
            return false;
        }
        UUID purchaseUuid = player.getUniqueId();
        if (!itemShopPurchaseProcessing.add(purchaseUuid)) return false;
        if (!plugin.getPitGoldService().lock(purchaseUuid)) {
            itemShopPurchaseProcessing.remove(purchaseUuid);
            return false;
        }
        EquipmentUpgradeService.InventorySnapshot inventorySnapshot = equipmentUpgradeService.snapshot(player);
        boolean charged = false;
        try {
            definition = definition(rawId);
            if (definition == null || !definition.enabled || definition.renown && !isUnlocked(player.getUniqueId(), definition.id)) return false;
            String nonce = UUID.randomUUID().toString();
            List<ItemStack> grant = grant(definition, player, nonce);
            if (grant.isEmpty()) return false;
            List<ItemStack> purchaseOverflow = new ArrayList<>();
            boolean equipmentUpgrade = definition.id.equals("diamond-sword") || definition.id.equals("diamond-chestplate") || definition.id.equals("diamond-boots");
            if (equipmentUpgrade) {
                EquipmentUpgradeResult placement = equipmentUpgradeService.apply(player, grant.get(0));
                if (placement == EquipmentUpgradeResult.ALREADY_HAS_ITEM) {
                    if (!autobuy) player.sendMessage(CC.color("&cYou already have this equipment upgrade."));
                    return false;
                }
                if (placement == EquipmentUpgradeResult.NO_SAFE_INVENTORY_SPACE || placement == EquipmentUpgradeResult.INVALID_ITEM || placement == EquipmentUpgradeResult.FAILED) {
                    inventorySnapshot.restore(player);
                    if (!autobuy) player.sendMessage(CC.color("&cYou need more inventory space."));
                    return false;
                }
            } else {
                Map<Integer, ItemStack> remaining = player.getInventory().addItem(grant.toArray(new ItemStack[0]));
                for (ItemStack item : remaining.values()) purchaseOverflow.add(item.clone());
            }
            if (!plugin.getPitGoldService().take(player, definition.price)) {
                inventorySnapshot.restore(player);
                if (autobuy) player.sendMessage(CC.color("&c&lAUTOFAIL! &7Couldn't afford " + plainName(definition) + "!"));
                else player.sendMessage(CC.color("&cYou need " + format(Math.max(0, definition.price - plugin.getPitGoldService().balance(player.getUniqueId()))) + " more Gold!"));
                return false;
            }
            charged = true;
            if (!purchaseOverflow.isEmpty()) {
                if (plugin.getPitCommandManager() == null) throw new IllegalStateException("stash service unavailable");
                for (int index = 0; index < purchaseOverflow.size(); index++) {
                    UUID stashId = UUID.nameUUIDFromBytes((nonce + ":" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    plugin.getPitCommandManager().addToStash(player.getUniqueId(), purchaseOverflow.get(index), stashId);
                }
            }
            if (definition.id.equals("pants-bundle")) {
                String bundleId = SecureItemTag.string(grant.get(0), "PitBundleId");
                profile(player.getUniqueId()).bundles.putIfAbsent(bundleId, new ArrayList<>());
                saveAsync(player.getUniqueId());
            }
            plugin.getStatsManager().addLongStat(player.getUniqueId(), StatsManager.SHOP_ITEMS_PURCHASED, 1L);
            plugin.getStatsManager().addLongStat(player.getUniqueId(), StatsManager.DIAMOND_ITEMS_PURCHASED, 1L);
            plugin.getStatsManager().addLongStat(player.getUniqueId(), StatsManager.GOLD_SPENT_ITEM_SHOP, definition.price);
            if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                    new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.SHOP_PURCHASED,
                            1L, Map.of("id", definition.id, "price", definition.price), false));
            if (!autobuy) player.sendMessage(CC.color("&aPurchased " + definition.displayName + " &afor &6" + format(definition.price) + " Gold&a."));
            plugin.getBuildScoreboardManager().update(player);
            refreshIfOpen(player);
            return true;
        } catch (Throwable throwable) {
            inventorySnapshot.restore(player);
            if (charged) {
                plugin.getPitGoldService().add(player, definition == null ? 0 : definition.price);
                player.sendMessage(CC.color("&cThe item could not be granted. Your Gold was restored."));
            }
            plugin.getLogger().severe("Item Shop purchase failed for " + purchaseUuid + ": " + throwable.getMessage());
            return false;
        } finally {
            plugin.getPitGoldService().unlock(purchaseUuid);
            itemShopPurchaseProcessing.remove(purchaseUuid);
        }
    }

    public double baseDamage(Player attacker, Player victim, double damage) {
        if (attacker == null || victim == null) return damage;
        ItemStack held = attacker.getItemInHand();
        if (isShopItem(held, "combat-spade")) {
            int pieces = 0;
            for (ItemStack armor : victim.getInventory().getArmorContents()) {
                if (armor != null && armor.getType().name().startsWith("DIAMOND_")) pieces++;
            }
            return 7.0D + Math.min(4, pieces);
        }
        return damage;
    }

    public double finalDamage(Player attacker, Player victim, double damage) {
        if (attacker == null || victim == null) return damage;
        ItemStack held = attacker.getItemInHand();
        Definition sword = definition("diamond-sword");
        if (isShopItem(held, "diamond-sword") && plugin.getStatsManager().getBounty(victim.getUniqueId()) > 0 && sword != null) {
            damage *= 1.0D + sword.decimal("bounty-damage-bonus", 0.20D);
        }
        Definition solvent = definition("bounty-solvent");
        if (solventActive(victim.getUniqueId()) && solvent != null
                && plugin.getStatsManager().getBounty(attacker.getUniqueId()) >= solvent.integer("bounty-threshold", 1000)) {
            damage *= Math.max(0.0D, 1.0D - solvent.decimal("damage-reduction", 0.30D));
        }
        return Math.max(0.0D, damage);
    }

    public int bountyReward(Player killer, int value) {
        Definition solvent = definition("bounty-solvent");
        if (killer == null || value <= 0 || solvent == null || !solventActive(killer.getUniqueId())) return Math.max(0, value);
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(value * (1.0D + solvent.decimal("bounty-gold-bonus", 0.50D))));
    }

    private boolean solventActive(UUID uuid) {
        return uuid != null && solventUntil.getOrDefault(uuid, 0L) > System.currentTimeMillis();
    }

    public void onKill(Player killer) {
        if (killer == null) return;
        UUID uuid = killer.getUniqueId();
        Long ready = firstAidReady.get(uuid);
        if (ready == null || ready <= System.currentTimeMillis()) return;
        Definition egg = definition("first-aid-egg");
        long reduction = Math.max(0L, egg == null ? 5000L : egg.integer("kill-reduction-seconds", 5) * 1000L);
        firstAidReady.put(uuid, Math.max(System.currentTimeMillis(), ready - reduction));
    }

    public List<String> effectLines(Player player) {
        if (player == null) return Collections.emptyList();
        long remaining = solventUntil.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
        if (remaining <= 0L) return Collections.emptyList();
        return Collections.singletonList("&fBounty Solvent &6I &7(" + ((remaining + 999L) / 1000L) + "s)");
    }

    public long blockLifetimeSeconds(ItemStack item) {
        if (isShopItem(item, "obsidian")) {
            Definition definition = definition("obsidian");
            return definition == null ? 120L : Math.max(1L, definition.integer("despawn-seconds", 120));
        }
        if (isShopItem(item, "obsidian-stack")) {
            Definition definition = definition("obsidian-stack");
            return definition == null ? 120L : Math.max(1L, definition.integer("despawn-seconds", 120));
        }
        return -1L;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        if (isShopItem(item, "bounty-solvent")) {
            Definition definition = definition("bounty-solvent");
            if (definition != null) solventUntil.put(player.getUniqueId(), System.currentTimeMillis() + definition.integer("duration-seconds", 60) * 1000L);
            plugin.getBuildScoreboardManager().update(player);
        } else if (isShopItem(item, "jump-boost")) {
            Definition definition = definition("jump-boost");
            if (definition == null) return;
            int duration = Math.max(1, definition.integer("duration-seconds", 30)) * 20;
            int amplifier = Math.max(1, definition.integer("amplifier", 4)) - 1;
            Bukkit.getScheduler().runTask(plugin, () -> player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration, amplifier), true));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK
                && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getItemInHand();
        if (held == null || held.getType() == Material.AIR) return;
        String taggedId = SecureItemTag.string(held, ITEM_TAG);
        String shopId = "";
        if (taggedId.startsWith(ITEM_PREFIX)) {
            String candidate = normalize(taggedId.substring(ITEM_PREFIX.length()));
            if (!candidate.isEmpty() && taggedId.equals(ITEM_PREFIX + candidate)) shopId = candidate;
        }
        if (shopId.isEmpty()) return;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && shopId.equals("golden-pickaxe")) {
            event.setCancelled(true);
            hitObsidian(player, event.getClickedBlock());
            return;
        }
        if (shopId.equals("first-aid-egg")) {
            event.setCancelled(true);
            useFirstAid(player);
        } else if (shopId.equals("tactical-insertion")) {
            event.setCancelled(true);
            useTactical(player);
        } else if (shopId.equals("pants-bundle")) {
            event.setCancelled(true);
            useBundle(player, held);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        beforeDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ShopProfile profile = profiles.remove(uuid);
        loadedProfiles.remove(uuid);
        dirtyProfiles.remove(uuid);
        if (profile != null) {
            String serialized = profile.serialize();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveNow(uuid, serialized));
        }
        tacticalLocations.remove(uuid);
        tacticalReady.remove(uuid);
        firstAidReady.remove(uuid);
        solventUntil.remove(uuid);
        pendingRespawn.remove(uuid);
        pickProgress.remove(uuid);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        tacticalLocations.remove(uuid);
        tacticalReady.remove(uuid);
        pickProgress.remove(uuid);
    }

    public void beforeDeath(Player player) {
        if (player == null || !pendingRespawn.add(player.getUniqueId())) return;
        ShopProfile profile = profiles.get(player.getUniqueId());
        if (profile != null) {
            for (ItemStack item : player.getInventory().getContents()) {
                String bundle = bundleId(item);
                if (!bundle.isEmpty()) profile.carriedBundles.add(bundle);
            }
        }
        firstAidReady.remove(player.getUniqueId());
        solventUntil.remove(player.getUniqueId());
        pickProgress.remove(player.getUniqueId());
    }

    public Location consumeRespawnLocation(Player player) {
        if (player == null) return null;
        Location location = tacticalLocations.remove(player.getUniqueId());
        return safeTactical(location) ? location.clone() : null;
    }

    public void afterRespawn(Player player) {
        if (player == null || !pendingRespawn.remove(player.getUniqueId())) return;
        restoreBundles(player);
        if (ownsAutobuy(player.getUniqueId()) && profile(player.getUniqueId()).autobuyEnabled) {
            for (String id : autobuyOrder(profile(player.getUniqueId()))) purchase(player, id, true);
        }
    }

    public boolean unlock(Player player, String id) {
        Definition definition = definition(id);
        return player != null && definition != null && definition.renown
                && plugin.getStatsManager().purchaseRenownUpgrade(player, unlockId(definition.id), definition.requiredPrestige, definition.renownPrice);
    }

    public boolean adminUnlock(Player player, String id) {
        Definition definition = definition(id);
        return player != null && definition != null && definition.renown
                && plugin.getStatsManager().grantRenownUpgrade(player, unlockId(definition.id));
    }

    public boolean adminLock(Player player, String id) {
        Definition definition = definition(id);
        return player != null && definition != null && definition.renown
                && plugin.getStatsManager().removeRenownUpgrade(player, unlockId(definition.id));
    }

    public boolean adminGive(Player player, String id, int amount) {
        Definition definition = definition(id);
        if (player == null || definition == null || amount < 1 || !loadedProfiles.contains(player.getUniqueId())) return false;
        List<ItemStack> all = new ArrayList<>();
        for (int index = 0; index < amount; index++) all.addAll(grant(definition, player, UUID.randomUUID().toString()));
        if (!canFit(player.getInventory(), all)) return false;
        player.getInventory().addItem(all.toArray(new ItemStack[0]));
        for (ItemStack item : all) {
            String bundle = bundleId(item);
            if (!bundle.isEmpty()) profile(player.getUniqueId()).bundles.putIfAbsent(bundle, new ArrayList<>());
        }
        saveAsync(player.getUniqueId());
        return true;
    }

    public String debug(Player player) {
        if (player == null) return "Player unavailable";
        ShopProfile profile = profiles.get(player.getUniqueId());
        return "loaded=" + (profile != null) + " gold=" + plugin.getPitGoldService().balance(player.getUniqueId())
                + " autobuy-owned=" + ownsAutobuy(player.getUniqueId()) + " autobuy=" + (profile != null && profile.autobuyEnabled)
                + " selected=" + (profile == null ? "[]" : profile.autobuy) + " bundles=" + (profile == null ? 0 : profile.bundles.size());
    }

    public Collection<Definition> definitions() {
        List<Definition> values = new ArrayList<>();
        loadDefinitions(values, "item-shop.defaults", false);
        loadDefinitions(values, "item-shop.renown-items", true);
        values.sort(Comparator.comparingInt(value -> value.slot));
        return values;
    }

    public Definition definition(String rawId) {
        String id = normalize(rawId);
        for (Definition definition : definitions()) if (definition.id.equals(id)) return definition;
        return null;
    }

    public boolean isUnlocked(UUID uuid, String id) {
        Definition definition = definition(id);
        return definition != null && (!definition.renown || plugin.getStatsManager().hasRenownUpgrade(uuid, unlockId(definition.id)));
    }

    public boolean ownsAutobuy(UUID uuid) {
        return plugin.getStatsManager().hasRenownUpgrade(uuid, "autobuy");
    }

    public void setAutobuyEnabled(Player player, boolean enabled) {
        ShopProfile profile = profile(player.getUniqueId());
        profile.autobuyEnabled = enabled;
        saveAsync(player.getUniqueId());
    }

    public boolean isAutobuyEnabled(UUID uuid) {
        ShopProfile profile = profiles.get(uuid);
        return profile != null && profile.autobuyEnabled;
    }

    private void toggleAutobuy(Player player, String id) {
        Definition definition = definition(id);
        if (definition == null || definition.renown && !isUnlocked(player.getUniqueId(), id)) return;
        if (id.equals("pants-bundle") && !plugin.registry().shops().getBoolean("item-shop.autobuy.allow-pants-bundle", false)) {
            player.sendMessage(CC.color("&cPants Bundle Autobuy is disabled."));
            return;
        }
        ShopProfile profile = profile(player.getUniqueId());
        if (!profile.autobuy.add(id)) profile.autobuy.remove(id);
        profile.autobuyEnabled = true;
        saveAsync(player.getUniqueId());
        openShop(player);
    }

    private ItemStack icon(Player player, Definition definition) {
        boolean unlocked = !definition.renown || isUnlocked(player.getUniqueId(), definition.id);
        boolean affordable = plugin.getPitGoldService().has(player.getUniqueId(), definition.price);
        List<String> lore = new ArrayList<>(definition.description);
        lore.add("");
        if (!unlocked) {
            lore.add("&7Requires: &ePrestige " + roman(definition.requiredPrestige));
            lore.add("&7Renown unlock: &e" + definition.renownPrice);
            lore.add("");
            lore.add("&cUnlock this in the Renown Shop!");
        } else {
            lore.add("&7Cost: &6" + format(definition.price) + " Gold");
            lore.add("&7You have: &6" + format(plugin.getPitGoldService().balance(player.getUniqueId())) + " Gold");
            lore.add("");
            lore.add(affordable ? "&eClick to purchase!" : "&cYou need " + format(Math.max(0, definition.price - plugin.getPitGoldService().balance(player.getUniqueId()))) + " more Gold!");
            if (ownsAutobuy(player.getUniqueId())) lore.add(profile(player.getUniqueId()).autobuy.contains(definition.id) ? "&aAutobuy enabled" : "&7Right-click to toggle Autobuy");
        }
        ItemStack icon = menuItem(definition.material, definition.data, (unlocked ? "&a" : "&c") + plainName(definition), lore,
                unlocked && profile(player.getUniqueId()).autobuy.contains(definition.id));
        return icon;
    }

    private List<ItemStack> grant(Definition definition, Player player, String nonce) {
        List<ItemStack> items = new ArrayList<>();
        if (definition.id.equals("iron-pack")) {
            items.add(tagged(new ItemStack(Material.IRON_CHESTPLATE), definition, nonce));
            items.add(tagged(new ItemStack(Material.IRON_LEGGINGS), definition, nonce));
            items.add(tagged(new ItemStack(Material.IRON_BOOTS), definition, nonce));
            return items;
        }
        ItemStack item = new ItemStack(definition.material, definition.amount, definition.data);
        if (definition.id.equals("pants-bundle")) {
            item = SecureItemTag.string(item, "PitBundleId", UUID.randomUUID().toString());
            item = SecureItemTag.string(item, "PitBundleOwner", player.getUniqueId().toString());
        }
        item = tagged(item, definition, nonce);
        if (definition.id.equals("diamond-sword") && plugin.getPerkManager().has(player, "barbarian")) item.setType(Material.DIAMOND_AXE);
        items.add(item);
        return items;
    }

    public ItemStack createMysticItem(String requestedType) {
        String type = normalize(requestedType);
        Material material;
        String id;
        String name;
        if (type.equals("sword") || type.equals("mystic-sword")) {
            material = Material.GOLD_SWORD;
            id = "mystic_sword";
            name = "&eMystic Sword";
        } else if (type.equals("bow") || type.equals("mystic-bow")) {
            material = Material.BOW;
            id = "mystic_bow";
            name = "&eMystic Bow";
        } else if (type.equals("pants") || type.equals("fresh-pants")) {
            material = Material.LEATHER_LEGGINGS;
            id = "fresh_pants";
            name = "&cFresh Pants";
        } else return null;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        meta.setLore(Arrays.asList(CC.color("&7Tier: &f0"), CC.color("&7Lives: &a5/5"),
                CC.color("&7Can be enchanted in the Mystic Well.")));
        if (meta instanceof LeatherArmorMeta leather) {
            Color[] colors = {
                    Color.fromRGB(0xFF5555), Color.fromRGB(0x55FFFF), Color.fromRGB(0xFFFF55),
                    Color.fromRGB(0xAA00AA), Color.fromRGB(0x55FF55), Color.fromRGB(0xFFAA00)
            };
            leather.setColor(colors[Math.floorMod(UUID.randomUUID().hashCode(), colors.length)]);
        }
        item.setItemMeta(meta);
        item = SecureItemTag.string(item, ITEM_TAG, id);
        item = SecureItemTag.string(item, "MysticItemId", UUID.randomUUID().toString());
        item = SecureItemTag.integer(item, "MysticTier", 0);
        item = SecureItemTag.integer(item, "MysticLives", 5);
        return item;
    }

    public boolean isMysticItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String id = SecureItemTag.string(item, ITEM_TAG);
        return !SecureItemTag.string(item, "MysticItemId").isEmpty()
                && (id.equals("fresh_pants") || id.equals("mystic_sword") || id.equals("mystic_bow"));
    }

    private ItemStack tagged(ItemStack item, Definition definition, String nonce) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(definition.displayName));
        item.setItemMeta(meta);
        item = SecureItemTag.string(item, ITEM_TAG, ITEM_PREFIX + definition.id);
        item = SecureItemTag.string(item, "PitShopNonce", nonce);
        item = SecureItemTag.integer(item, "PitShopLostOnDeath", definition.id.equals("pants-bundle") ? 0 : 1);
        if (definition.unbreakable) item = SecureItemTag.unbreakable(item);
        return item;
    }

    private void hitObsidian(Player player, Block block) {
        if (block == null || block.getType() != Material.OBSIDIAN || plugin.getArenaManager().isInCurrentSpawnProtection(block.getLocation())
                || !plugin.getGameListener().isPlayerPlaced(block)) {
            pickProgress.remove(player.getUniqueId());
            return;
        }
        Definition pickaxe = definition("golden-pickaxe");
        long now = System.currentTimeMillis();
        long timeout = pickaxe == null ? 3000L : pickaxe.integer("hit-timeout-millis", 3000);
        PickProgress previous = pickProgress.get(player.getUniqueId());
        int hits = previous != null && previous.block.equals(block) && now - previous.at <= timeout ? previous.hits + 1 : 1;
        int required = pickaxe == null ? 2 : Math.max(1, pickaxe.integer("obsidian-hits", 2));
        if (hits < required) {
            pickProgress.put(player.getUniqueId(), new PickProgress(block, hits, now));
            player.sendMessage(CC.color("&eHit this obsidian once more to break it."));
            return;
        }
        pickProgress.remove(player.getUniqueId());
        int limit = pickaxe == null ? 4 : Math.max(0, pickaxe.integer("connected-block-limit", 4));
        int broken = plugin.getGameListener().breakPlayerObsidian(block, limit);
        if (broken > 0) plugin.getStatsManager().addLongStat(player.getUniqueId(), StatsManager.BLOCKS_BROKEN, broken);
    }

    private void useFirstAid(Player player) {
        long now = System.currentTimeMillis();
        long ready = firstAidReady.getOrDefault(player.getUniqueId(), 0L);
        if (ready > now) {
            player.sendMessage(CC.color("&cFirst-Aid Egg is ready in " + ((ready - now + 999L) / 1000L) + "s."));
            return;
        }
        Definition definition = definition("first-aid-egg");
        double heal = (definition == null ? 2.5D : definition.decimal("heal-hearts", 2.5D)) * 2.0D;
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
        player.removePotionEffect(PotionEffectType.SPEED);
        firstAidReady.put(player.getUniqueId(), now + (definition == null ? 30L : definition.integer("cooldown-seconds", 30)) * 1000L);
        updateEggs(player, true);
    }

    private void useTactical(Player player) {
        long now = System.currentTimeMillis();
        long ready = tacticalReady.getOrDefault(player.getUniqueId(), 0L);
        if (ready > now) {
            player.sendMessage(CC.color("&cTactical Insertion is ready in " + ((ready - now + 999L) / 1000L) + "s."));
            return;
        }
        Location location = player.getLocation().clone();
        if (!safeTactical(location)) {
            player.sendMessage(CC.color("&cYou cannot place a Tactical Insertion here."));
            return;
        }
        Definition definition = definition("tactical-insertion");
        tacticalLocations.put(player.getUniqueId(), location);
        tacticalReady.put(player.getUniqueId(), now + (definition == null ? 60L : definition.integer("cooldown-seconds", 60)) * 1000L);
        player.sendMessage(CC.color("&aYour next respawn position was set."));
    }

    private boolean safeTactical(Location location) {
        if (location == null || location.getWorld() == null || plugin.getArenaManager().getCurrentArena() == null) return false;
        if (!location.getWorld().getName().equalsIgnoreCase(plugin.getArenaManager().getCurrentArena())) return false;
        if (!plugin.getArenaManager().isInCurrentPvpArena(location) || plugin.getArenaManager().isInCurrentSpawnProtection(location)) return false;
        if (location.getY() <= plugin.registry().settings().getInt("settings.void-y", 42)) return false;
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);
        return !floor.getType().isTransparent() && floor.getType() != Material.LAVA && floor.getType() != Material.STATIONARY_LAVA
                && (feet.getType() == Material.AIR || feet.getType() == Material.WATER || feet.getType() == Material.STATIONARY_WATER)
                && head.getType() == Material.AIR;
    }

    private void useBundle(Player player, ItemStack bundle) {
        if (!player.getUniqueId().toString().equals(SecureItemTag.string(bundle, "PitBundleOwner"))) return;
        String id = bundleId(bundle);
        if (id.isEmpty()) return;
        ShopProfile profile = profile(player.getUniqueId());
        List<ItemStack> stored = profile.bundles.computeIfAbsent(id, ignored -> new ArrayList<>());
        if (stored.isEmpty()) {
            int capacity = definition("pants-bundle") == null ? 10 : Math.max(1, definition("pants-bundle").integer("capacity", 10));
            for (int slot = 0; slot < player.getInventory().getSize() && stored.size() < capacity; slot++) {
                ItemStack item = player.getInventory().getItem(slot);
                if (!freshPants(item)) continue;
                stored.add(item.clone());
                player.getInventory().setItem(slot, null);
            }
            if (!stored.isEmpty()) {
                saveAsync(player.getUniqueId());
                player.sendMessage(CC.color("&aStored " + stored.size() + " Fresh Pants."));
            }
        }
        openBundle(player, id);
    }

    private void openBundle(Player player, String id) {
        ShopProfile profile = profile(player.getUniqueId());
        List<ItemStack> stored = profile.bundles.getOrDefault(id, Collections.emptyList());
        int capacity = definition("pants-bundle") == null ? 10 : Math.max(1, definition("pants-bundle").integer("capacity", 10));
        int size = Math.max(18, ((capacity + 8) / 9) * 9);
        BundleHolder holder = new BundleHolder(player.getUniqueId(), id);
        Inventory inventory = Bukkit.createInventory(holder, size, CC.color("&8Pants Bundle"));
        holder.inventory = inventory;
        for (int index = 0; index < Math.min(stored.size(), inventory.getSize()); index++) inventory.setItem(index, stored.get(index).clone());
        player.openInventory(inventory);
    }

    private boolean freshPants(ItemStack item) {
        if (item == null || item.getType() != Material.LEATHER_LEGGINGS) return false;
        String id = SecureItemTag.string(item, ITEM_TAG);
        return id.equals("fresh_pants") || id.equals("fresh_aqua_pants") || id.startsWith("fresh_");
    }

    private void restoreBundles(Player player) {
        ShopProfile profile = profiles.get(player.getUniqueId());
        if (profile == null) return;
        Set<String> existing = bundleIds(player.getInventory().getContents());
        existing.addAll(bundleIds(player.getEnderChest().getContents()));
        Set<String> wanted = profile.carriedBundles.isEmpty() ? profile.bundles.keySet() : new LinkedHashSet<>(profile.carriedBundles);
        for (String id : wanted) {
            if (existing.contains(id)) continue;
            Definition definition = definition("pants-bundle");
            if (definition == null) continue;
            ItemStack item = new ItemStack(Material.MINECART);
            item = SecureItemTag.string(item, "PitBundleId", id);
            item = SecureItemTag.string(item, "PitBundleOwner", player.getUniqueId().toString());
            item = tagged(item, definition, UUID.randomUUID().toString());
            if (canFit(player.getInventory(), Collections.singletonList(item))) player.getInventory().addItem(item);
        }
        profile.carriedBundles.clear();
        saveAsync(player.getUniqueId());
    }

    private Set<String> bundleIds(ItemStack[] contents) {
        Set<String> ids = new HashSet<>();
        if (contents != null) for (ItemStack item : contents) {
            String id = bundleId(item);
            if (!id.isEmpty()) ids.add(id);
        }
        return ids;
    }

    private String bundleId(ItemStack item) {
        return isShopItem(item, "pants-bundle") ? SecureItemTag.string(item, "PitBundleId") : "";
    }

    private boolean isShopItem(ItemStack item, String id) {
        return item != null && (ITEM_PREFIX + normalize(id)).equals(SecureItemTag.string(item, ITEM_TAG));
    }

    public boolean isItem(ItemStack item, String id) {
        return isShopItem(item, id);
    }

    private void removeNonce(PlayerInventory inventory, String nonce) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (nonce.equals(SecureItemTag.string(item, "PitShopNonce"))) inventory.setItem(slot, null);
        }
    }

    private boolean canFit(PlayerInventory inventory, List<ItemStack> additions) {
        ItemStack[] simulated = inventory.getContents();
        int storageSize = Math.min(36, simulated.length);
        List<ItemStack> slots = new ArrayList<>(storageSize);
        for (int index = 0; index < storageSize; index++) {
            ItemStack item = simulated[index];
            slots.add(item == null ? null : item.clone());
        }
        for (ItemStack addition : additions) {
            int remaining = addition.getAmount();
            for (ItemStack current : slots) {
                if (current == null || !current.isSimilar(addition)) continue;
                int space = Math.max(0, current.getMaxStackSize() - current.getAmount());
                int moved = Math.min(space, remaining);
                current.setAmount(current.getAmount() + moved);
                remaining -= moved;
                if (remaining == 0) break;
            }
            while (remaining > 0) {
                int empty = slots.indexOf(null);
                if (empty < 0) return false;
                ItemStack placed = addition.clone();
                placed.setAmount(Math.min(remaining, placed.getMaxStackSize()));
                remaining -= placed.getAmount();
                slots.set(empty, placed);
            }
        }
        return true;
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Long ready = firstAidReady.get(player.getUniqueId());
            if (ready != null) {
                boolean cooling = ready > now;
                updateEggs(player, cooling);
                if (!cooling) firstAidReady.remove(player.getUniqueId());
            }
        }
        if (now >= nextFlushAt) {
            nextFlushAt = now + 5000L;
            flushDirty();
        }
    }

    private void updateEggs(Player player, boolean cooling) {
        short desired = (short) (cooling ? 51 : 96);
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!isShopItem(item, "first-aid-egg") || item.getDurability() == desired) continue;
            ItemStack updated = item.clone();
            updated.setDurability(desired);
            player.getInventory().setItem(slot, updated);
        }
    }

    private List<String> autobuyOrder(ShopProfile profile) {
        List<String> configured = plugin.registry().shops().getStringList("item-shop.autobuy.order");
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String id : configured) if (profile.autobuy.contains(normalize(id))) result.add(normalize(id));
        List<String> remaining = new ArrayList<>(profile.autobuy);
        Collections.sort(remaining);
        result.addAll(remaining);
        return new ArrayList<>(result);
    }

    private void load(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String stored = plugin.getStorageManager().getOverride("item-shop", uuid.toString()).orElseGet(() -> fallbackValue(uuid));
            ShopProfile loaded = ShopProfile.deserialize(stored);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                profiles.put(uuid, loaded);
                loadedProfiles.add(uuid);
                Bukkit.getScheduler().runTaskLater(plugin, () -> restoreBundles(player), 3L);
            });
        });
    }

    private ShopProfile profile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, ignored -> new ShopProfile());
    }

    private void saveAsync(UUID uuid) {
        if (uuid != null && profiles.containsKey(uuid)) dirtyProfiles.add(uuid);
    }

    private void flushDirty() {
        Map<UUID, String> pending = new LinkedHashMap<>();
        for (UUID uuid : new HashSet<>(dirtyProfiles)) {
            ShopProfile profile = profiles.get(uuid);
            if (profile != null && dirtyProfiles.remove(uuid)) pending.put(uuid, profile.serialize());
        }
        if (!pending.isEmpty()) Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, String> entry : pending.entrySet()) saveNow(entry.getKey(), entry.getValue());
        });
    }

    private synchronized String fallbackValue(UUID uuid) {
        return fallback.getString("players." + uuid, "");
    }

    private synchronized void saveNow(UUID uuid, String serialized) {
        plugin.getStorageManager().putOverride("item-shop", uuid.toString(), serialized);
        try {
            fallbackFile.getParentFile().mkdirs();
            fallback.set("players." + uuid, serialized);
            fallback.save(fallbackFile);
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not save Item Shop profile " + uuid + ": " + exception.getMessage());
        }
    }

    private void loadDefinitions(List<Definition> output, String root, boolean renown) {
        for (String rawId : plugin.registry().shops().keys(root)) {
            String id = normalize(rawId);
            String base = root + "." + rawId;
            Material material = Material.matchMaterial(plugin.registry().shops().getString(base + ".material", fallbackMaterial(id).name()));
            if (material == null) continue;
            output.add(new Definition(id, base, plugin.registry().shops().getBoolean(base + ".enabled", true),
                    plugin.registry().shops().getString(base + ".display-name", pretty(id)),
                    plugin.registry().shops().getStringList(base + ".description"),
                    plugin.registry().shops().getInt(base + ".slot", -1), material,
                    (short) plugin.registry().shops().getInt(base + ".data", fallbackData(id)),
                    Math.max(1, Math.min(64, plugin.registry().shops().getInt(base + ".amount", 1))),
                    Math.max(0, plugin.registry().shops().getInt(base + ".price", 0)),
                    plugin.registry().shops().getBoolean(base + ".unbreakable", defaultUnbreakable(id)),
                    renown, Math.max(0, plugin.registry().shops().getInt(base + ".required-prestige", 0)),
                    Math.max(0, plugin.registry().shops().getInt(base + ".renown-unlock-price", 0))));
        }
    }

    private Material fallbackMaterial(String id) {
        return switch (id) {
            case "iron-pack" -> Material.IRON_CHESTPLATE;
            case "obsidian-stack" -> Material.OBSIDIAN;
            case "combat-spade" -> Material.DIAMOND_SPADE;
            case "diamond-leggings" -> Material.DIAMOND_LEGGINGS;
            case "bounty-solvent", "jump-boost" -> Material.POTION;
            case "first-aid-egg" -> Material.MONSTER_EGG;
            case "tactical-insertion" -> Material.BLAZE_ROD;
            case "pants-bundle" -> Material.MINECART;
            default -> Material.PAPER;
        };
    }

    private short fallbackData(String id) {
        return (short) (id.equals("first-aid-egg") ? 96 : 0);
    }

    private boolean defaultUnbreakable(String id) {
        return id.equals("iron-pack") || id.equals("combat-spade") || id.equals("diamond-leggings");
    }

    private ItemStack menuItem(Material material, short data, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        if (glow) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material material(String path, Material fallback) {
        Material value = Material.matchMaterial(plugin.registry().shops().getString(path, fallback.name()));
        return value == null ? fallback : value;
    }

    private int menuSize() {
        int size = Math.max(9, Math.min(54, plugin.registry().shops().getInt("item-shop.size", 45)));
        return size - size % 9;
    }

    private String unlockId(String id) {
        return "shop-" + normalize(id);
    }

    private String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ENGLISH).replace('_', '-').replace(' ', '-');
    }

    private String pretty(String id) {
        StringBuilder value = new StringBuilder();
        for (String part : id.split("-")) {
            if (value.length() > 0) value.append(' ');
            if (!part.isEmpty()) value.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return value.toString();
    }

    private String plainName(Definition definition) {
        return org.bukkit.ChatColor.stripColor(CC.color(definition.displayName));
    }

    private String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private String roman(int value) {
        if (value <= 0) return "0";
        int[] numbers = {50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < numbers.length; index++) while (value >= numbers[index]) {
            result.append(numerals[index]);
            value -= numbers[index];
        }
        return result.toString();
    }

    private static String encodeItems(List<ItemStack> items) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
                output.writeInt(items.size());
                for (ItemStack item : items) output.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static List<ItemStack> decodeItems(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new ArrayList<>();
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            try (BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                int size = Math.max(0, Math.min(10, input.readInt()));
                List<ItemStack> items = new ArrayList<>();
                for (int index = 0; index < size; index++) {
                    Object item = input.readObject();
                    if (item instanceof ItemStack) items.add((ItemStack) item);
                }
                return items;
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public final class Definition {
        public final String id;
        public final String path;
        public final boolean enabled;
        public final String displayName;
        public final List<String> description;
        public final int slot;
        public final Material material;
        public final short data;
        public final int amount;
        public final int price;
        public final boolean unbreakable;
        public final boolean renown;
        public final int requiredPrestige;
        public final int renownPrice;

        private Definition(String id, String path, boolean enabled, String displayName, List<String> description,
                           int slot, Material material, short data, int amount, int price, boolean unbreakable,
                           boolean renown, int requiredPrestige, int renownPrice) {
            this.id = id;
            this.path = path;
            this.enabled = enabled;
            this.displayName = displayName;
            this.description = description;
            this.slot = slot;
            this.material = material;
            this.data = data;
            this.amount = amount;
            this.price = price;
            this.unbreakable = unbreakable;
            this.renown = renown;
            this.requiredPrestige = requiredPrestige;
            this.renownPrice = renownPrice;
        }

        public int integer(String key, int fallback) {
            return plugin.registry().shops().getInt(path + "." + key, fallback);
        }

        public double decimal(String key, double fallback) {
            return plugin.registry().shops().getDouble(path + "." + key, fallback);
        }
    }

    private static final class ShopHolder implements InventoryHolder {
        private final UUID owner;
        private final Map<Integer, String> items = new HashMap<>();
        private Inventory inventory;

        private ShopHolder(UUID owner) {
            this.owner = owner;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class BundleHolder implements InventoryHolder {
        private final UUID owner;
        private final String bundleId;
        private Inventory inventory;

        private BundleHolder(UUID owner, String bundleId) {
            this.owner = owner;
            this.bundleId = bundleId;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class ShopProfile {
        private boolean autobuyEnabled = true;
        private final Set<String> autobuy = new LinkedHashSet<>();
        private final Map<String, List<ItemStack>> bundles = new LinkedHashMap<>();
        private final Set<String> carriedBundles = new LinkedHashSet<>();

        private String serialize() {
            List<String> bundleData = new ArrayList<>();
            for (Map.Entry<String, List<ItemStack>> entry : bundles.entrySet()) {
                bundleData.add(entry.getKey() + ":" + encodeItems(entry.getValue()));
            }
            return (autobuyEnabled ? "1" : "0") + "|" + String.join(",", autobuy) + "|" + String.join(";", bundleData);
        }

        private static ShopProfile deserialize(String value) {
            ShopProfile profile = new ShopProfile();
            if (value == null || value.isEmpty()) return profile;
            String[] parts = value.split("\\|", -1);
            profile.autobuyEnabled = parts.length == 0 || !parts[0].equals("0");
            if (parts.length > 1 && !parts[1].isEmpty()) {
                for (String id : parts[1].split(",")) if (!id.isEmpty()) profile.autobuy.add(id);
            }
            if (parts.length > 2 && !parts[2].isEmpty()) {
                for (String entry : parts[2].split(";")) {
                    int cut = entry.indexOf(':');
                    if (cut > 0) profile.bundles.put(entry.substring(0, cut), decodeItems(entry.substring(cut + 1)));
                }
            }
            return profile;
        }
    }

    private record PickProgress(Block block, int hits, long at) {}
}
