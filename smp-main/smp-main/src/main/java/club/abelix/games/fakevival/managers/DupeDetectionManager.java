package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.menus.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DupeDetectionManager {
    private final JavaPlugin plugin;
    private final DiscordWebhookManager discordWebhookManager;
    private final SocialSpyManager socialSpyManager;
    private final NamespacedKey itemIdKey;
    private final Map<String, Long> lastAlertMillis = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDupeScan = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCraftingScanRequest = new ConcurrentHashMap<>();
    private final Set<UUID> queuedDupeScans = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> suppressedUntil = new ConcurrentHashMap<>();
    private final Map<UUID, ArrayDeque<Long>> recentScanRequests = new ConcurrentHashMap<>();
    private final Map<UUID, ArrayDeque<Long>> recentAlerts = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> suspiciousUniqueIds = new ConcurrentHashMap<>();
    private final Map<UUID, Long> loadingSkipLogs = new ConcurrentHashMap<>();
    private final Set<String> warnedPdcTypes = ConcurrentHashMap.newKeySet();
    private int taskId = -1;
    private int globalScanCursor;
    private volatile boolean readOnlyMode = true;

    public DupeDetectionManager(JavaPlugin plugin, DiscordWebhookManager discordWebhookManager) {
        this(plugin, discordWebhookManager, null);
    }

    public DupeDetectionManager(JavaPlugin plugin, DiscordWebhookManager discordWebhookManager, SocialSpyManager socialSpyManager) {
        this.plugin = plugin;
        this.discordWebhookManager = discordWebhookManager;
        this.socialSpyManager = socialSpyManager;
        this.itemIdKey = new NamespacedKey(plugin, "dupe_item_id");
    }

    public void start() {
        readOnlyMode = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.read-only-mode", true);
        long interval = Math.max(400L, settingLong("global-scan-interval-seconds", 20L) * 20L);
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::scanOnlinePlayers, 40L, interval).getTaskId();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        queuedDupeScans.clear();
        recentScanRequests.clear();
        recentAlerts.clear();
        suppressedUntil.clear();
        suspiciousUniqueIds.clear();
        lastDupeScan.clear();
        lastCraftingScanRequest.clear();
        loadingSkipLogs.clear();
    }

    public void scanLater(Player player) {
        requestScan(player, "event");
    }

    public void requestScan(Player player, String reason, ItemStack... eventItems) {
        if (!isEnabled() || player == null || !player.isOnline()) return;
        if(!playerDataReady(player)){logLoadingSkip(player);return;}
        UUID uuid = player.getUniqueId();
        inspectEventItemsReadOnly(player, reason, eventItems);
        if (queuedDupeScans.contains(uuid)) return;
        if (!acceptScanRequest(player, reason)) return;
        if (!queuedDupeScans.add(uuid)) return;
        long delay = Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getLong("dupe-detection.event-scan-delay-ticks", 4L), 2L, 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null || !online.isOnline() || !canScan(online)) return;
                verifyDuplicateState(online, reason);
            } finally {
                queuedDupeScans.remove(uuid);
            }
        }, delay);
    }

    public void requestCraftingScan(Player player, String reason) {
        if (!settingBoolean("scan-crafting-table", "scanCraftingTable", true) || player == null) return;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = Math.max(1000L, settingLong("crafting-scan-cooldown-ms", 2000L));
        long previous = lastCraftingScanRequest.getOrDefault(uuid, 0L);
        if (now - previous < cooldown) return;
        lastCraftingScanRequest.put(uuid, now);
        requestScan(player, reason);
    }

    public void verifyDuplicateState(Player player) {
        verifyDuplicateState(player, "manual_verification");
    }

    private void verifyDuplicateState(Player player, String reason) {
        if (!isEnabled() || player == null || !player.isOnline() || !playerDataReady(player)) return;
        Map<String, SeenItem> seen = new HashMap<>();
        Set<String> alerted = new HashSet<>();
        int items = scanInventory(player, player.getInventory(), seen, alerted);
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.scan-ender-chest", true))
            items += scanContents(player, player.getEnderChest().getContents(), "ender chest", seen, alerted);
        suspiciousUniqueIds.put(player.getUniqueId(), alerted.size());
        lastDupeScan.put(player.getUniqueId(), System.currentTimeMillis());
        String message = "Read-only scan player=" + player.getName() + " reason=" + reason
                + " items=" + items + " alerts=" + alerted.size();
        if (alerted.isEmpty() && (reason.equals("crafting_click") || reason.equals("crafting_drag")
                || reason.equals("inventory_click") || reason.equals("inventory_drag"))) debugLog(message);
        else plugin.getLogger().info("[SMP Dupe] " + message);
    }

    public void scanOnlinePlayers() {
        if (!isEnabled()) return;
        int limit = (int) Math.clamp(settingLong("global-scan-batch-size", 10L), 1L, 10L);
        List<? extends Player> eligible = Bukkit.getOnlinePlayers().stream()
                .filter(Player::isOnline)
                .filter(this::playerDataReady)
                .filter(player -> !(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder))
                .toList();
        if (eligible.isEmpty()) return;
        List<Player> players = new ArrayList<>();
        int count = Math.min(limit, eligible.size());
        for (int offset = 0; offset < count; offset++)
            players.add(eligible.get((globalScanCursor + offset) % eligible.size()));
        globalScanCursor = (globalScanCursor + count) % eligible.size();
        Map<String, SeenItem> cycleSeen = new HashMap<>();
        Set<String> cycleAlerts = new HashSet<>();
        for (int index = 0; index < players.size(); index++) {
            UUID uuid = players.get(index).getUniqueId();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline() || !playerDataReady(player) || !canScan(player)) return;
                int before = cycleAlerts.size();
                int items = scanInventory(player, player.getInventory(), cycleSeen, cycleAlerts);
                if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.scan-ender-chest", true))
                    items += scanContents(player, player.getEnderChest().getContents(), "ender chest", cycleSeen, cycleAlerts);
                lastDupeScan.put(uuid, System.currentTimeMillis());
                suspiciousUniqueIds.put(uuid, Math.max(0, cycleAlerts.size() - before));
                plugin.getLogger().info("[SMP Dupe] Read-only scan player=" + player.getName()
                        + " reason=global_batch items=" + items + " alerts=" + Math.max(0, cycleAlerts.size() - before));
            }, index * 2L);
        }
    }

    public boolean isEnabled() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.enabled", true);
    }

    private boolean canScan(Player player) {
        long now = System.currentTimeMillis();
        if (now < suppressedUntil.getOrDefault(player.getUniqueId(), 0L)) return false;
        long last = lastDupeScan.getOrDefault(player.getUniqueId(), 0L);
        return now - last >= Math.max(1000L, settingLong("scan-cooldown-ms", 2000L));
    }

    private boolean playerDataReady(Player player){return !(plugin instanceof Game game)||!game.getPlayerDataSyncManager().isEnabled()||game.getPlayerDataSyncManager().isPlayerDataLoaded(player.getUniqueId());}
    private void logLoadingSkip(Player player){long now=System.currentTimeMillis(),last=loadingSkipLogs.getOrDefault(player.getUniqueId(),0L);if(now-last<10_000L)return;loadingSkipLogs.put(player.getUniqueId(),now);plugin.getLogger().info("[SMP Dupe] Skipped scan player="+player.getName()+" reason=playerdata_loading");}

    private boolean acceptScanRequest(Player player, String reason) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getBoolean("dupe-detection.rate-limit-protection", true)) return true;
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long until = suppressedUntil.getOrDefault(uuid, 0L);
        if (now < until) return false;
        if (until > 0L) suppressedUntil.remove(uuid);
        long window = Math.max(1000L, settingLong("suppression-window-ms", 10000L));
        int maximum = (int) Math.max(1L, settingLong("max-scan-requests-per-window", 5L));
        ArrayDeque<Long> requests = recentScanRequests.computeIfAbsent(uuid, ignored -> new ArrayDeque<>());
        int count;
        synchronized (requests) {
            prune(requests, now - window);
            requests.addLast(now);
            count = requests.size();
        }
        if (count <= maximum) return true;
        suppressedUntil.put(uuid, now + window);
        if (settingBoolean("log-suppressed-scans", "logSuppressedScans", false))
            debugLog("Suppressed scan for " + player.getName() + " reason=" + reason
                    + " requests=" + count + " window=" + (window / 1000L) + "s");
        return false;
    }

    private void inspectEventItemsReadOnly(Player player, String reason, ItemStack... items) {
        if (items == null) return;
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;
            try {
                getExistingId(item);
                isFakeMenuChest(item);
            } catch (Exception ex) {
                debugLog("Skipped event snapshot for " + player.getName() + " reason=" + reason
                        + " because PDC data was incompatible: " + ex.getMessage());
            }
        }
    }

    private long settingLong(String key, long fallback) {
        var config = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        return config.getLong("dupe-detection." + key, config.getLong("dupe." + key, fallback));
    }

    private boolean settingBoolean(String key, String alias, boolean fallback) {
        var config = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        return config.getBoolean("dupe-detection." + key, config.getBoolean("dupe." + alias, fallback));
    }

    private void prune(ArrayDeque<Long> values, long minimum) {
        while (!values.isEmpty() && values.peekFirst() < minimum) values.removeFirst();
    }

    private int scanInventory(Player player, PlayerInventory inventory, Map<String, SeenItem> seen, Set<String> alertedThisScan) {
        int scanned = scanContents(player, inventory.getStorageContents(), "inventory", seen, alertedThisScan);
        scanned += scanContents(player, inventory.getArmorContents(), "armor", seen, alertedThisScan);
        try {
            ItemStack offhand = inventory.getItemInOffHand();
            if (offhand != null && !offhand.getType().isAir()) scanned++;
            scanItem(player, offhand, "offhand", seen, alertedThisScan);
        }
        catch (Exception ex) { debugLog("Skipped offhand item during dupe scan because PDC data was incompatible: " + ex.getMessage()); }
        return scanned;
    }

    private int scanContents(Player player, ItemStack[] contents, String location, Map<String, SeenItem> seen, Set<String> alertedThisScan) {
        if (contents == null) return 0;
        int scanned = 0;
        for (int slot = 0; slot < contents.length; slot++) {
            if (contents[slot] == null || contents[slot].getType().isAir()) continue;
            scanned++;
            try { scanItem(player, contents[slot], location + " slot " + slot, seen, alertedThisScan); }
            catch (Exception ex) { debugLog("Skipped item during dupe scan because PDC data was incompatible: " + ex.getMessage()); }
        }
        return scanned;
    }

    private void scanItem(Player player, ItemStack item, String location, Map<String, SeenItem> seen, Set<String> alertedThisScan) {
        if (isFakeMenuChest(item)) { alertFakeMenuChest(player.getName(), location, item); return; }
        if (shouldSkipTracking(item)) return;
        String id = getExistingId(item);
        if (id.isBlank()) return;
        SeenItem current = describe(player.getName(), location, item);
        SeenItem previous = seen.putIfAbsent(id, current);
        if (previous == null) return;
        if (previous.samePlace(current)) return;

        alert(id, previous, current, alertedThisScan);
    }

    private void scanDroppedItem(Item droppedItem, Map<String, SeenItem> seen, Set<String> alertedThisScan) {
        ItemStack stack = droppedItem.getItemStack();
        if (isFakeMenuChest(stack)) {
            alertFakeMenuChest("world", "drop @ " + droppedItem.getWorld().getName(), stack);
            return;
        }
        if (shouldSkipTracking(stack)) return;
        String id = getExistingId(stack);
        if (id.isBlank()) return;
        String location = "drop @ " + droppedItem.getWorld().getName()
                + " " + droppedItem.getLocation().getBlockX()
                + ", " + droppedItem.getLocation().getBlockY()
                + ", " + droppedItem.getLocation().getBlockZ();
        SeenItem current = describe("world", location, stack);
        SeenItem previous = seen.putIfAbsent(id, current);
        if (previous == null) return;
        if (previous.samePlace(current)) return;
        alert(id, previous, current, alertedThisScan);
    }

    private boolean shouldSkipTracking(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getAmount() <= 0) return true;
        if (!getExistingId(item).isBlank()) return false;
        if (containsMaterial("dupe-detection.ignored-materials", item.getType())) return true;
        // Normal gear, fish, food, materials and vanilla stacks are not unique merely
        // because of their material or custom item type.
        return true;
    }

    private boolean isExplicitlyTracked(Material material) {
        return containsMaterial("dupe-detection.tracked-materials", material);
    }

    private boolean containsMaterial(String path, Material material) {
        for (String configured : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path)) {
            if (configured.equalsIgnoreCase(material.name())) return true;
        }
        return false;
    }

    private String getExistingId(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return "";
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "";
        for (String name : List.of("unique_item_id", "dupe_unique_id", "item_uuid")) {
            NamespacedKey key = new NamespacedKey(plugin, name);
            String value = readStringPdc(meta, key);
            if (value != null && !value.isBlank()) return name + ":" + value;
        }
        return "";
    }

    private String getStringIfType(org.bukkit.persistence.PersistentDataContainer pdc, NamespacedKey key) {
        if (pdc == null || key == null) return null;
        if (pdc.has(key, PersistentDataType.STRING)) return pdc.get(key, PersistentDataType.STRING);
        if (pdc.has(key, PersistentDataType.LONG)) {
            warnWrongTypeOnce(key, "LONG");
            return null;
        }
        if (pdc.has(key, PersistentDataType.INTEGER)) {
            warnWrongTypeOnce(key, "INTEGER");
            return null;
        }
        if (pdc.has(key)) warnWrongTypeOnce(key, "unsupported");
        return null;
    }

    private String readStringPdc(ItemMeta meta, NamespacedKey key) {
        return meta == null ? null : getStringIfType(meta.getPersistentDataContainer(), key);
    }

    private Long readLongPdc(ItemMeta meta, NamespacedKey key) {
        if (meta == null || key == null) return null;
        var pdc = meta.getPersistentDataContainer();
        return pdc.has(key, PersistentDataType.LONG) ? pdc.get(key, PersistentDataType.LONG) : null;
    }

    private void warnWrongTypeOnce(NamespacedKey key, String type) {
        if (warnedPdcTypes.add(key + ":" + type)) debugLog("Unique ID key " + key + " used " + type + "; handled without mutating the item.");
    }

    private void debugLog(String message) {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.debug", false))
            plugin.getLogger().info("[DupeDetection] " + message);
    }

    public ValidationReport validatePdcSafety() {
        ItemStack probe = new ItemStack(Material.BOOK);
        ItemMeta meta = probe.getItemMeta();
        var pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "edition"), PersistentDataType.LONG, 7L);
        pdc.set(new NamespacedKey(plugin, "fish_length_cm"), PersistentDataType.INTEGER, 42);
        pdc.set(new NamespacedKey(plugin, "final_sell_price"), PersistentDataType.DOUBLE, 12.5D);
        pdc.set(new NamespacedKey(plugin, "unique_item_id"), PersistentDataType.STRING, "developer_notebook:7");
        probe.setItemMeta(meta);
        String found;
        try { found = getExistingId(probe); }
        catch (Exception ex) { return new ValidationReport(false, false, false, false, false); }
        return new ValidationReport(found.equals("unique_item_id:developer_notebook:7"),
                pdc.has(new NamespacedKey(plugin, "unique_item_id"), PersistentDataType.STRING),
                Long.valueOf(7L).equals(readLongPdc(meta, new NamespacedKey(plugin, "edition"))), true, true);
    }

    public record ValidationReport(boolean scanSafe, boolean uniqueIdString, boolean editionLong,
                                   boolean fishFieldsExcluded, boolean readOnlyScan) {}

    public CraftingSafetyReport validateCraftingSafety() {
        long delay = Math.clamp(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getLong("dupe-detection.event-scan-delay-ticks", 4L), 2L, 5L);
        return new CraftingSafetyReport(settingBoolean("scan-crafting-table", "scanCraftingTable", true),
                readOnlyMode, delay >= 2L && delay <= 5L,
                !settingBoolean("log-suppressed-scans", "logSuppressedScans", false),
                !cancelSuspiciousClicks(), true, true, true);
    }

    public record CraftingSafetyReport(boolean craftingScanEnabled, boolean readOnly, boolean delayed,
                                       boolean suppressedLogsQuiet, boolean normalClicksNotCancelled,
                                       boolean noEventMutation, boolean noUpdateInventory, boolean noPlayerDataSave) {
    }

    private void alert(String id, SeenItem first, SeenItem duplicate, Set<String> alertedThisScan) {
        String alertKey = duplicate.playerName() + ":" + duplicate.itemName() + ":" + id;
        if (!alertedThisScan.add(alertKey) || isAlertCoolingDown(alertKey)) return;
        long cooldownMillis = Math.max(1000L, settingLong("alert-cooldown-ms", 30000L));
        lastAlertMillis.put(alertKey, System.currentTimeMillis() + cooldownMillis);
        Player duplicatePlayer = Bukkit.getPlayerExact(duplicate.playerName());
        if (duplicatePlayer != null) recordRecentAlert(duplicatePlayer.getUniqueId());

        String description = "**" + duplicate.playerName() + "** has suspicious duplicate item ID `" + id + "`.\n"
                + "**Original Seen:** " + first.playerName() + " (" + first.location() + ")\n"
                + "**Duplicate Seen:** " + duplicate.playerName() + " (" + duplicate.location() + ")\n"
                + "**Mode:** Read-only\n**Items removed:** 0\n**Server:** " + serverId();
        if (socialSpyManager != null) {
            socialSpyManager.spy("&c[Dupe Detection] &e" + duplicate.playerName()
                    + " &7has suspicious duplicate item ID &f" + id + "&7. Read-only alert, no items removed.");
        }
        discordWebhookManager.sendDupeAlert("Dupe Detection", description);
    }

    private String serverId() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("server-id", plugin.getServer().getName());
    }

    public boolean isFakeMenuChest(ItemStack item) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.fake-menu-chests.enabled", true)) return false;
        if (item == null || item.getType().isAir() || !isProtectedContainerMaterial(item.getType())) return false;
        if (!item.hasItemMeta() || item.getItemMeta() == null || !item.getItemMeta().hasDisplayName()) return false;
        return isProtectedMenuName(item.getItemMeta().getDisplayName());
    }

    public boolean isProtectedMenuName(String name) {
        String normalized = normalizeMenuName(name);
        if (normalized.isBlank()) return false;
        for (String protectedName : protectedMenuNames()) {
            if (normalized.equals(normalizeMenuName(protectedName))) return true;
        }
        return false;
    }

    public boolean isReadOnlyMode() { return readOnlyMode; }
    public void setReadOnlyMode(boolean value) { readOnlyMode=value; }
    public boolean isAutoQuarantine() { return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.auto-quarantine", false); }
    public boolean isAutoRemoveItems() { return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.auto-remove-items", false); }
    public boolean cancelSuspiciousClicks() { return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.cancel-suspicious-clicks", false); }
    public DebugReport debug(Player player) {
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        return new DebugReport(readOnlyMode, isAutoRemoveItems(), cancelSuspiciousClicks(), queuedDupeScans.contains(id),
                lastDupeScan.containsKey(id) ? Math.max(0L, now - lastDupeScan.get(id)) : -1L,
                suppressedUntil.getOrDefault(id, 0L), recentCount(recentScanRequests.get(id), now - 10000L),
                recentCount(recentAlerts.get(id), now - 30000L));
    }
    public ScanReport scanPlayer(Player player) {
        if(!playerDataReady(player)){logLoadingSkip(player);return new ScanReport(0,0,readOnlyMode);}
        Map<String,SeenItem> seen=new HashMap<>(); Set<String> alerted=new HashSet<>();
        scanInventory(player,player.getInventory(),seen,alerted);
        scanContents(player,player.getEnderChest().getContents(),"ender chest",seen,alerted);
        return new ScanReport(seen.size(),alerted.size(),readOnlyMode);
    }
    public RepairReport repairPlayer(Player player) {
        if (player == null || !player.isOnline() || !playerDataReady(player)) return new RepairReport(0, 0);
        Set<String> seen = new HashSet<>();
        int inspected = 0, removed = 0;
        ItemStack[] inventory = player.getInventory().getContents();
        for (int slot = 0; slot < inventory.length; slot++) {
            ItemStack item = inventory[slot];
            if (item == null || item.getType().isAir()) continue;
            inspected++;
            String id = getExistingId(item);
            if (id.isBlank() || seen.add(id)) continue;
            player.getInventory().setItem(slot, null);
            removed++;
        }
        ItemStack[] ender = player.getEnderChest().getContents();
        for (int slot = 0; slot < ender.length; slot++) {
            ItemStack item = ender[slot];
            if (item == null || item.getType().isAir()) continue;
            inspected++;
            String id = getExistingId(item);
            if (id.isBlank() || seen.add(id)) continue;
            player.getEnderChest().setItem(slot, null);
            removed++;
        }
        if (removed > 0) player.updateInventory();
        return new RepairReport(inspected, removed);
    }

    public void forgetPlayer(UUID uuid) {
        if (uuid == null) return;
        queuedDupeScans.remove(uuid);
        lastDupeScan.remove(uuid);
        lastCraftingScanRequest.remove(uuid);
        suppressedUntil.remove(uuid);
        recentScanRequests.remove(uuid);
        recentAlerts.remove(uuid);
        suspiciousUniqueIds.remove(uuid);
        loadingSkipLogs.remove(uuid);
    }

    public record ScanReport(int uniqueItems,int duplicates,boolean readOnly) {}
    public record DebugReport(boolean readOnly, boolean autoRemove, boolean cancelSuspiciousClicks, boolean queued,
                              long lastScanAgeMillis, long suppressedUntilMillis, int recentScanRequests,
                              int recentAlerts) {}
    public record RepairReport(int inspectedItems, int removedDuplicates) {}

    private boolean isProtectedContainerMaterial(Material material) {
        for (String configured : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("dupe-detection.fake-menu-chests.protected-materials")) {
            if (configured.equalsIgnoreCase(material.name())) return true;
        }
        return material == Material.CHEST || material == Material.TRAPPED_CHEST || material == Material.ENDER_CHEST
                || material == Material.BARREL || material.name().endsWith("_SHULKER_BOX");
    }

    private Set<String> protectedMenuNames() {
        Set<String> names = new HashSet<>(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("dupe-detection.fake-menu-chests.protected-names"));
        addConfiguredName(names, "main-menu.title");
        addConfiguredName(names, "menus.skills.title");
        addConfiguredName(names, "menus.skill-leveling.title");
        addConfiguredName(names, "menus.jobs.title");
        addConfiguredName(names, "menus.shop.title");
        addConfiguredName(names, "menus.bazaar.title");
        addConfiguredName(names, "menus.adventure-guild.title");
        addConfiguredName(names, "menus.auctionhouse.title");
        addConfiguredName(names, "menus.battlepass.title");
        addConfiguredName(names, "menus.sellall.title");
        addConfiguredName(names, "store.title");
        addConfiguredName(names, "collections.menu.title");
        addConfiguredName(names, "companions.menu.title");
        addConfiguredName(names, "playtime-rewards.menu.title");
        addConfiguredName(names, "guilds.menu.title");
        names.add("&8Your Boosters");
        names.add("&8Your Cosmetics");
        names.add("&8Equipment");
        names.add("&8Browse Guilds");
        names.add("&8Guild Settings");
        names.add("&8Admin Item Browser");
        names.add("&6Player Level");
        names.add("&8Sell All");
        names.add("&8Collections");
        names.add("&6Guilds");
        names.add("&6SMP Gold Store");
        names.add("&6Survival Menu");
        return names;
    }

    private void addConfiguredName(Set<String> names, String path) {
        String value = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path);
        if (value != null && !value.isBlank()) names.add(value);
    }

    private String normalizeMenuName(String name) {
        if (name == null) return "";
        String colored = ChatColor.translateAlternateColorCodes('&', name);
        String stripped = ChatColor.stripColor(colored);
        if (stripped == null) return "";
        return stripped.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private void alertFakeMenuChest(String playerName, String location, ItemStack item) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dupe-detection.fake-menu-chests.alert", true)) return;
        String itemName = getItemName(item);
        String alertKey = "fake-menu:" + playerName + ":" + itemName;
        if (isAlertCoolingDown(alertKey)) return;
        lastAlertMillis.put(alertKey, System.currentTimeMillis() + Math.max(1000L, settingLong("alert-cooldown-ms", 30000L)));
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) recordRecentAlert(player.getUniqueId());
        if (socialSpyManager != null) {
            socialSpyManager.spy("&8[Spy Alert] &cSuspicious fake menu chest: &f" + itemName
                    + " &7owner=&f" + playerName + " &7location=&f" + location);
        }
        discordWebhookManager.sendDupeAlert("Fake menu chest removed", "**Player:** " + playerName
                + "\n**Location:** " + location
                + "\n**Item:** " + itemName
                + "\n**Action:** Read-only alert; no item removed.");
    }

    private boolean isAlertCoolingDown(String id) {
        long until = lastAlertMillis.getOrDefault(id, 0L);
        if (System.currentTimeMillis() <= until) return true;
        lastAlertMillis.remove(id);
        return false;
    }

    private void recordRecentAlert(UUID uuid) {
        ArrayDeque<Long> alerts = recentAlerts.computeIfAbsent(uuid, ignored -> new ArrayDeque<>());
        synchronized (alerts) {
            long now = System.currentTimeMillis();
            prune(alerts, now - 30000L);
            alerts.addLast(now);
        }
    }

    private int recentCount(ArrayDeque<Long> values, long minimum) {
        if (values == null) return 0;
        synchronized (values) {
            prune(values, minimum);
            return values.size();
        }
    }

    private SeenItem describe(String playerName, String location, ItemStack item) {
        return new SeenItem(playerName, location, getItemName(item), item.getAmount());
    }

    private String getItemName(ItemStack item) {
        String material = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return material;
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return displayName.isBlank() ? material : displayName + " (" + material + ")";
    }

    private record SeenItem(String playerName, String location, String itemName, int amount) {
        private boolean samePlace(SeenItem other) {
            return other != null && playerName.equals(other.playerName()) && location.equals(other.location());
        }
    }
}
