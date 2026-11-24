package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import me.outerspace.thepit.registry.RegistryNode;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestManager implements Listener {
    private enum QuestState {
        AVAILABLE, ACTIVE, COMPLETED
    }

    private enum Action {
        DAILY_HUNTER, DAILY_CONTRACTED, WEEKLY_DOUBLE_UP, NOVICE, BIG_TIME, ACHIEVEMENTS, AUTO_ACCEPT
    }

    private record QuestDefinition(String period, String id, String storageId, String type, int slot,
                                   String displayName, String material, long target, int pitGold,
                                   int networkExperience, int eventExperience, boolean eventExperienceEnabled,
                                   List<String> lore) {
    }

    private final ThePitPlugin plugin;
    private final Map<String, String> progress = new ConcurrentHashMap<>();
    private final Set<String> completions = ConcurrentHashMap.newKeySet();
    private final Set<String> warnings = ConcurrentHashMap.newKeySet();
    private volatile ZoneId resetZone = ZoneId.of("America/New_York");

    public QuestManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        reloadOverrides();
    }

    public void handleKill(Player player, int xpGained, int streak) {
        if (player == null) return;
        prepare(player.getUniqueId());
        progress(player, "daily", "hunter", 1L);
    }

    public void handleContractCompleted(Player player) {
        if (player == null) return;
        prepare(player.getUniqueId());
        progress(player, "daily", "contracted", 1L);
    }

    public void handlePitGoldEarned(Player player, int amount) {
        if (player == null || amount <= 0) return;
        prepare(player.getUniqueId());
        progress(player, "weekly", "double-up", amount);
    }

    public void open(Player player) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) {
            if (player != null) player.sendMessage(color(message("profile-loading", "&cYour profile is still loading.")));
            return;
        }
        prepare(player.getUniqueId());
        MenuHolder holder = new MenuHolder(player.getUniqueId());
        int size = normalizedSize(config().getInt("quests.menu.size", 54));
        Inventory inventory = Bukkit.createInventory(holder, size, color(config().getString("quests.menu.title", "Quests & Contracts")));
        holder.inventory = inventory;
        ItemStack filler = item(material(config().getString("quests.menu.filler.material", "STAINED_GLASS_PANE"), Material.STAINED_GLASS_PANE),
                (short) config().getInt("quests.menu.filler.data", 15), config().getString("quests.menu.filler.name", " "), Collections.emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        put(inventory, config().getInt("quests.menu.quest-master.slot", 4), questMaster(player));
        putQuest(holder, inventory, player, dailyHunter(), Action.DAILY_HUNTER);
        putQuest(holder, inventory, player, dailyContracted(), Action.DAILY_CONTRACTED);
        putQuest(holder, inventory, player, weeklyDoubleUp(), Action.WEEKLY_DOUBLE_UP);
        putAction(holder, inventory, config().getInt("quests.menu.contracts.novice.slot", 30), Action.NOVICE,
                contractItem(player, ContractManager.Difficulty.NOVICE));
        putAction(holder, inventory, config().getInt("quests.menu.contracts.big-time.slot", 32), Action.BIG_TIME,
                contractItem(player, ContractManager.Difficulty.BIG_TIME));
        putAction(holder, inventory, config().getInt("quests.menu.achievements.slot", 45), Action.ACHIEVEMENTS,
                achievementsItem(player));
        putAction(holder, inventory, config().getInt("quests.menu.auto-accept.slot", 53), Action.AUTO_ACCEPT,
                autoAcceptItem(player));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.owner.equals(player.getUniqueId())) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;
        Action action = holder.actions.get(event.getRawSlot());
        if (action == null) return;
        switch (action) {
            case DAILY_HUNTER -> start(player, dailyHunter());
            case DAILY_CONTRACTED -> start(player, dailyContracted());
            case WEEKLY_DOUBLE_UP -> start(player, weeklyDoubleUp());
            case NOVICE -> plugin.getContractManager().openDifficulty(player, ContractManager.Difficulty.NOVICE);
            case BIG_TIME -> plugin.getContractManager().openDifficulty(player, ContractManager.Difficulty.BIG_TIME);
            case ACHIEVEMENTS -> plugin.getAchievementManager().open(player, player);
            case AUTO_ACCEPT -> toggleAutoAccept(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    private void start(Player player, QuestDefinition definition) {
        prepare(player.getUniqueId());
        QuestState state = state(player.getUniqueId(), definition);
        if (state == QuestState.COMPLETED) {
            player.sendMessage(color(message("already-completed", "&aQuest completed!")));
            return;
        }
        if (state == QuestState.ACTIVE) {
            player.sendMessage(color(message("already-active", "&aYou've already started this quest!")));
            return;
        }
        put(key(player.getUniqueId(), definition.period, "active/" + definition.storageId), "true");
        play(player, "start");
        player.sendMessage(color(message("started", "&aQuest started!")));
        open(player);
    }

    private void progress(Player player, String period, String id, long amount) {
        QuestDefinition definition = definition(period, id);
        if (definition == null || amount <= 0L) return;
        UUID uuid = player.getUniqueId();
        if (!isActive(uuid, definition)) {
            if (!autoAccept(uuid) || !canAutoAccept(player)) return;
            put(key(uuid, definition.period, "active/" + definition.storageId), "true");
        }
        if (isCompleted(uuid, definition)) return;
        String progressKey = key(uuid, definition.period, definition.type);
        long next = Math.min(definition.target, safeLong(progress.get(progressKey)) + amount);
        put(progressKey, String.valueOf(next));
        if (next >= definition.target) complete(player, definition);
        else refreshIfOpen(player);
    }

    private void complete(Player player, QuestDefinition definition) {
        UUID uuid = player.getUniqueId();
        String completion = uuid + ":" + definition.period + ":" + definition.storageId + ":" + periodKey(definition.period);
        if (isCompleted(uuid, definition) || !completions.add(completion)) return;
        try {
            if (isCompleted(uuid, definition)) return;
            put(key(uuid, definition.period, "claimed/" + definition.storageId), "true");
            if (definition.pitGold > 0) plugin.getPitGoldService().award(player, definition.pitGold, ThePitPlugin.GoldSource.QUEST);
            if (definition.networkExperience > 0) awardExternalExperience(player, definition.networkExperience, false);
            if (definition.eventExperienceEnabled && definition.eventExperience > 0) {
                awardExternalExperience(player, definition.eventExperience, true);
            }
            play(player, "complete");
            player.sendMessage(color(message("completed", "&a&lQUEST COMPLETE! &7You completed &f%quest%&7 and earned &6%gold% Gold&7.")
                    .replace("%quest%", definition.displayName)
                    .replace("%gold%", number(definition.pitGold))
                    .replace("%network_experience%", number(definition.networkExperience))
                    .replace("%event_experience%", number(definition.eventExperience))));
            refreshIfOpen(player);
        } finally {
            completions.remove(completion);
        }
    }

    private void awardExternalExperience(Player player, int amount, boolean event) {
        boolean delivered = event
                ? plugin.getAbelixHook().addEventExperience(player, amount)
                : plugin.getAbelixHook().addExperience(player, amount);
        if (delivered) return;
        String path = event ? "quests.rewards.event-experience-command" : "quests.rewards.network-experience-command";
        String command = config().getString(path, "");
        if (command == null || command.trim().isEmpty()) {
            warn(path, "No compatible " + (event ? "event" : "network") + " experience reward provider is available.");
            return;
        }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()).replace("%amount%", String.valueOf(amount)));
    }

    private ItemStack questMaster(Player player) {
        UUID uuid = player.getUniqueId();
        int active = 0;
        int completedDaily = 0;
        for (QuestDefinition definition : definitions()) {
            QuestState state = state(uuid, definition);
            if (state == QuestState.ACTIVE) active++;
            if (definition.period.equals("daily") && state == QuestState.COMPLETED) completedDaily++;
        }
        int contractsRemaining = plugin.getContractManager().remainingToday(uuid);
        List<String> lore = lines("quests.menu.quest-master.lore", Arrays.asList(
                "&7Daily reset: &e%daily_reset%",
                "&7Weekly reset: &e%weekly_reset%",
                "",
                "&7Active quests: &e%active_quests%",
                "&7Completed daily quests: &e%completed_daily%",
                "&7Contracts remaining today: &e%contracts_remaining%"
        ));
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%daily_reset%", resetCountdown(false));
        replacements.put("%weekly_reset%", resetCountdown(true));
        replacements.put("%active_quests%", String.valueOf(active));
        replacements.put("%completed_daily%", String.valueOf(completedDaily));
        replacements.put("%contracts_remaining%", String.valueOf(contractsRemaining));
        return item(material(config().getString("quests.menu.quest-master.material", "DIRT"), Material.DIRT), (short) 0,
                config().getString("quests.menu.quest-master.name", "&eQuest Master"), replace(lore, replacements));
    }

    private void putQuest(MenuHolder holder, Inventory inventory, Player player, QuestDefinition definition, Action action) {
        putAction(holder, inventory, definition.slot, action, questItem(player, definition));
    }

    private ItemStack questItem(Player player, QuestDefinition definition) {
        UUID uuid = player.getUniqueId();
        QuestState state = state(uuid, definition);
        long current = Math.min(definition.target, questProgress(uuid, definition));
        List<String> lore = new ArrayList<>(definition.lore);
        if (lore.isEmpty()) {
            lore.add("&7%description%");
            lore.add("");
            lore.add("&7Progress: &e%progress%&7/&e%target%");
            lore.add("&7Reward: &6%pit_gold% Gold");
            lore.add("&7Network Experience: &b%network_experience%");
            if (definition.eventExperienceEnabled) lore.add("&7Event Experience: &d%event_experience%");
            lore.add("");
            lore.add("%state_line%");
        }
        String stateLine = switch (state) {
            case AVAILABLE -> config().getString("quests.states.available", "&eClick to start!");
            case ACTIVE -> config().getString("quests.states.active", "&aYou've already started this quest!");
            case COMPLETED -> config().getString("quests.states.completed", "&aQuest completed!");
        };
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%progress%", number(current));
        replacements.put("%target%", number(definition.target));
        replacements.put("%pit_gold%", number(definition.pitGold));
        replacements.put("%network_experience%", number(definition.networkExperience));
        replacements.put("%event_experience%", number(definition.eventExperience));
        replacements.put("%state%", state.name());
        replacements.put("%state_line%", stateLine);
        replacements.put("%description%", config().getString("quests." + definition.period + "." + definition.id + ".description", ""));
        return item(material(definition.material, Material.PAPER), (short) 0, definition.displayName, replace(lore, replacements));
    }

    private ItemStack contractItem(Player player, ContractManager.Difficulty difficulty) {
        String id = difficulty == ContractManager.Difficulty.NOVICE ? "novice" : "big-time";
        String base = "quests.menu.contracts." + id;
        int required = plugin.getContractManager().requiredLevel(difficulty);
        int price = plugin.getContractManager().price(player.getUniqueId(), difficulty);
        int completed = plugin.getContractManager().completedToday(player.getUniqueId());
        int limit = plugin.getContractManager().dailyLimit(player.getUniqueId());
        List<String> fallback = difficulty == ContractManager.Difficulty.NOVICE
                ? Arrays.asList("&7Complete an easier contract", "&7for a modest Gold reward.", "", "&7Requires: &bLevel %required_level%",
                "&7Cost: &6%price% Gold", "&7Completions today: &e%completed%&7/&e%daily_limit%", "", "&eClick to purchase!")
                : Arrays.asList("&7Complete a difficult contract", "&7for a larger Gold reward.", "", "&7Requires: &bLevel %required_level%",
                "&7Cost: &6%price% Gold", "&7Completions today: &e%completed%&7/&e%daily_limit%", "", "&eClick to purchase!");
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%required_level%", String.valueOf(required));
        replacements.put("%price%", number(price));
        replacements.put("%completed%", String.valueOf(completed));
        replacements.put("%daily_limit%", String.valueOf(limit));
        return item(material(config().getString(base + ".material", "ENCHANTED_BOOK"), Material.ENCHANTED_BOOK), (short) 0,
                config().getString(base + ".name", difficulty == ContractManager.Difficulty.NOVICE ? "&aNovice Contract" : "&cBig Time Contract"),
                replace(lines(base + ".lore", fallback), replacements));
    }

    private ItemStack achievementsItem(Player player) {
        UUID uuid = player.getUniqueId();
        int points = plugin.getAchievementManager().points(uuid);
        int completed = plugin.getAchievementManager().completedCount(uuid);
        int total = plugin.getAchievementManager().totalCount();
        List<String> fallback = Arrays.asList("&7Achievement Points: &e%points%", "&7Completed: &a%completed%&7/&a%total%",
                "&7Completion: &b%percentage%%", "", "&eClick to view!");
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%points%", number(points));
        replacements.put("%completed%", number(completed));
        replacements.put("%total%", number(total));
        replacements.put("%percentage%", plugin.getAchievementManager().completionPercent(uuid));
        String base = "quests.menu.achievements";
        return item(material(config().getString(base + ".material", "DIAMOND"), Material.DIAMOND), (short) 0,
                config().getString(base + ".name", "&bThe Pit Achievements"), replace(lines(base + ".lore", fallback), replacements));
    }

    private ItemStack autoAcceptItem(Player player) {
        boolean enabled = autoAccept(player.getUniqueId());
        boolean allowed = canAutoAccept(player);
        String base = "quests.menu.auto-accept";
        String material = enabled ? config().getString(base + ".enabled-material", "SLIME_BALL")
                : config().getString(base + ".disabled-material", "MAGMA_CREAM");
        String name = enabled ? config().getString(base + ".enabled-name", "&aAuto-Accept Quests: ON")
                : config().getString(base + ".disabled-name", "&cAuto-Accept Quests: OFF");
        List<String> lore = allowed
                ? lines(base + ".lore", Arrays.asList("&7Automatically starts new daily", "&7and weekly quests.", "", "&eClick to toggle!"))
                : lines(base + ".locked-lore", Arrays.asList("&7Automatically starts new daily", "&7and weekly quests.", "", "&cRequires MVP+ or higher!"));
        return item(material(material, enabled ? Material.SLIME_BALL : Material.MAGMA_CREAM), (short) 0, name, lore);
    }

    private void toggleAutoAccept(Player player) {
        if (!canAutoAccept(player)) {
            player.sendMessage(color(message("auto-accept-locked", "&cAuto-Accept Quests requires MVP+ or higher.")));
            return;
        }
        boolean enabled = !autoAccept(player.getUniqueId());
        put(player.getUniqueId() + "/settings/auto-accept", String.valueOf(enabled));
        if (enabled) {
            for (QuestDefinition definition : definitions()) {
                if (state(player.getUniqueId(), definition) == QuestState.AVAILABLE) {
                    put(key(player.getUniqueId(), definition.period, "active/" + definition.storageId), "true");
                }
            }
        }
        play(player, "toggle");
        open(player);
    }

    private boolean canAutoAccept(Player player) {
        String permission = config().getString("quests.auto-accept.permission", "thepit.quests.autoaccept");
        if (me.outerspace.thepit.util.PermissionCompat.has(plugin, player, permission)) return true;
        NetworkProfileSnapshot snapshot = plugin.getNetworkProfileCache().cached(player.getUniqueId());
        if (snapshot == null) return false;
        String group = normalizeRank(snapshot.getGroup());
        for (String allowed : config().getStringList("quests.auto-accept.allowed-rank-ids")) {
            if (group.equals(normalizeRank(allowed))) return true;
        }
        return false;
    }

    private String normalizeRank(String rank) {
        if (rank == null) return "DEFAULT";
        return rank.trim().toUpperCase(Locale.ENGLISH).replace("++", "_PLUS_PLUS").replace("+", "_PLUS")
                .replace('-', '_').replace(' ', '_');
    }

    private QuestState state(UUID uuid, QuestDefinition definition) {
        if (isCompleted(uuid, definition)) return QuestState.COMPLETED;
        return isActive(uuid, definition) ? QuestState.ACTIVE : QuestState.AVAILABLE;
    }

    private boolean isCompleted(UUID uuid, QuestDefinition definition) {
        return Boolean.parseBoolean(progress.getOrDefault(key(uuid, definition.period, "claimed/" + definition.storageId), "false"));
    }

    private boolean isActive(UUID uuid, QuestDefinition definition) {
        String activeKey = key(uuid, definition.period, "active/" + definition.storageId);
        if (Boolean.parseBoolean(progress.getOrDefault(activeKey, "false"))) return true;
        if (questProgress(uuid, definition) > 0L || isCompleted(uuid, definition)) {
            put(activeKey, "true");
            return true;
        }
        return false;
    }

    private boolean autoAccept(UUID uuid) {
        return Boolean.parseBoolean(progress.getOrDefault(uuid + "/settings/auto-accept", "false"));
    }

    private long questProgress(UUID uuid, QuestDefinition definition) {
        return safeLong(progress.get(key(uuid, definition.period, definition.type)));
    }

    private void prepare(UUID uuid) {
        resetZone = configuredZone();
        String daily = dailyKey();
        String weekly = weeklyKey();
        String base = uuid + "/";
        if (daily.equals(progress.get(base + "daily/key"))) migrateLegacy(uuid, "daily", daily);
        if (weekly.equals(progress.get(base + "weekly/key"))) migrateLegacy(uuid, "weekly", weekly);
        if (!daily.equals(progress.get(base + "daily/key"))) {
            put(base + "daily/key", daily);
            activateAutomatic(uuid, "daily");
        }
        if (!weekly.equals(progress.get(base + "weekly/key"))) {
            put(base + "weekly/key", weekly);
            activateAutomatic(uuid, "weekly");
        }
    }

    private void migrateLegacy(UUID uuid, String period, String periodKey) {
        String marker = uuid + "/" + period + "/data/" + periodKey + "/migrated";
        if (Boolean.parseBoolean(progress.getOrDefault(marker, "false"))) return;
        for (QuestDefinition definition : definitions()) {
            if (!definition.period.equals(period)) continue;
            copyLegacy(uuid + "/" + period + "/" + definition.type, key(uuid, period, definition.type));
            copyLegacy(uuid + "/" + period + "/active/" + definition.storageId,
                    key(uuid, period, "active/" + definition.storageId));
            copyLegacy(uuid + "/" + period + "/claimed/" + definition.storageId,
                    key(uuid, period, "claimed/" + definition.storageId));
        }
        put(marker, "true");
    }

    private void copyLegacy(String legacy, String current) {
        String value = progress.get(legacy);
        if (value != null && !progress.containsKey(current)) put(current, value);
    }

    private void activateAutomatic(UUID uuid, String period) {
        Player player = Bukkit.getPlayer(uuid);
        if (!autoAccept(uuid) || player == null || !canAutoAccept(player)) return;
        for (QuestDefinition definition : definitions()) {
            if (definition.period.equals(period)) put(key(uuid, period, "active/" + definition.storageId), "true");
        }
    }

    private String dailyKey() {
        return LocalDate.now(resetZone).toString();
    }

    private String weeklyKey() {
        LocalDate date = LocalDate.now(resetZone);
        int days = Math.floorMod(date.getDayOfWeek().getValue() - DayOfWeek.FRIDAY.getValue(), 7);
        return date.minusDays(days).toString();
    }

    private String periodKey(String period) {
        return period.equals("weekly") ? weeklyKey() : dailyKey();
    }

    private String resetCountdown(boolean weekly) {
        ZonedDateTime now = ZonedDateTime.now(resetZone);
        ZonedDateTime target;
        if (!weekly) target = now.toLocalDate().plusDays(1L).atStartOfDay(resetZone);
        else {
            int days = Math.floorMod(DayOfWeek.FRIDAY.getValue() - now.getDayOfWeek().getValue(), 7);
            if (days == 0) days = 7;
            target = now.toLocalDate().plusDays(days).atStartOfDay(resetZone);
        }
        long seconds = Math.max(0L, Duration.between(now, target).getSeconds());
        long days = seconds / 86400L;
        long hours = seconds % 86400L / 3600L;
        long minutes = seconds % 3600L / 60L;
        if (days > 0L) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0L) return hours + "h " + minutes + "m";
        return Math.max(1L, minutes) + "m";
    }

    private ZoneId configuredZone() {
        String value = config().getString("quests.resets.timezone", "America/New_York");
        try {
            return ZoneId.of(value);
        } catch (Exception exception) {
            warn("timezone-" + value, "Invalid quests reset timezone '" + value + "'; using America/New_York.");
            return ZoneId.of("America/New_York");
        }
    }

    private QuestDefinition dailyHunter() {
        return definition("daily", "hunter");
    }

    private QuestDefinition dailyContracted() {
        return definition("daily", "contracted");
    }

    private QuestDefinition weeklyDoubleUp() {
        return definition("weekly", "double-up");
    }

    private List<QuestDefinition> definitions() {
        return Arrays.asList(dailyHunter(), dailyContracted(), weeklyDoubleUp());
    }

    private QuestDefinition definition(String period, String id) {
        String base = "quests." + period + "." + id;
        String storageId = config().getString(base + ".storage-id", id);
        String type = config().getString(base + ".type", id).toLowerCase(Locale.ENGLISH);
        int slot = config().getInt(base + ".slot", period.equals("weekly") ? 14 : id.equals("hunter") ? 12 : 13);
        String name = config().getString(base + ".display-name", "&a" + id);
        String material = config().getString(base + ".material", "PAPER");
        long target = Math.max(1L, config().getLong(base + ".target", config().getLong(base + ".goal", 1L)));
        int gold = Math.max(0, config().getInt(base + ".rewards.pit-gold", config().getInt(base + ".reward-coins", 0)));
        int network = Math.max(0, config().getInt(base + ".rewards.network-experience", 0));
        int event = Math.max(0, config().getInt(base + ".rewards.event-experience", 0));
        boolean eventEnabled = config().getBoolean(base + ".rewards.event-experience-enabled", false);
        return new QuestDefinition(period, id, storageId, type, slot, name, material, target, gold, network, event,
                eventEnabled, config().getStringList(base + ".lore"));
    }

    private void refreshIfOpen(Player player) {
        if (player == null || !(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) open(player);
        });
    }

    private void putAction(MenuHolder holder, Inventory inventory, int slot, Action action, ItemStack item) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        if (action == Action.NOVICE) item = SecureItemTag.string(item, "BuildFFAContractId", "NOVICE_CONTRACT");
        else if (action == Action.BIG_TIME) item = SecureItemTag.string(item, "BuildFFAContractId", "BIG_TIME_CONTRACT");
        inventory.setItem(slot, item);
        holder.actions.put(slot, action);
    }

    private void put(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, item);
    }

    private ItemStack item(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(color(line));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material material(String name, Material fallback) {
        Material material = Material.matchMaterial(name == null ? "" : name);
        if (material == null && "EXPERIENCE_BOTTLE".equalsIgnoreCase(name)) material = Material.EXP_BOTTLE;
        return material == null ? fallback : material;
    }

    private List<String> lines(String path, List<String> fallback) {
        List<String> lines = config().getStringList(path);
        return lines == null || lines.isEmpty() ? new ArrayList<>(fallback) : new ArrayList<>(lines);
    }

    private List<String> replace(List<String> lines, Map<String, String> replacements) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String changed = line;
            for (Map.Entry<String, String> entry : replacements.entrySet()) changed = changed.replace(entry.getKey(), entry.getValue());
            result.add(changed);
        }
        return result;
    }

    private String key(UUID uuid, String period, String suffix) {
        return uuid + "/" + period + "/data/" + periodKey(period) + "/" + suffix;
    }

    private long safeLong(String value) {
        try {
            return value == null ? 0L : Math.max(0L, Long.parseLong(value));
        } catch (Exception exception) {
            return 0L;
        }
    }

    private String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, value));
    }

    private int normalizedSize(int value) {
        int size = Math.max(9, Math.min(54, value));
        return size % 9 == 0 ? size : Math.min(54, (size / 9 + 1) * 9);
    }

    private void play(Player player, String id) {
        if (!config().getBoolean("quests.sounds." + id + ".enabled", true)) return;
        String name = config().getString("quests.sounds." + id + ".name", id.equals("complete") ? "LEVEL_UP" : "CLICK");
        try {
            Sound sound = Sound.valueOf(name.toUpperCase(Locale.ENGLISH));
            player.playSound(player.getLocation(), sound,
                    (float) config().getDouble("quests.sounds." + id + ".volume", 1.0D),
                    (float) config().getDouble("quests.sounds." + id + ".pitch", 1.0D));
        } catch (Exception exception) {
            warn("sound-" + id + "-" + name, "Invalid quest sound '" + name + "'.");
        }
    }

    private String message(String id, String fallback) {
        return config().getString("quests.messages." + id, fallback);
    }

    private String color(String value) {
        return CC.color(value == null ? "" : value);
    }

    public void save() {
    }

    public void reloadOverrides() {
        warnings.clear();
        resetZone = configuredZone();
        Map<String, String> loaded = plugin.getStorageManager().getOverrides("quest");
        progress.clear();
        progress.putAll(loaded);
    }

    private void put(String key, String value) {
        progress.put(key, value);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> plugin.getStorageManager().putOverride("quest", key, value));
    }

    private void warn(String key, String message) {
        if (warnings.add(key)) plugin.getLogger().warning(message);
    }

    private RegistryNode config() {
        return plugin.registry().quests();
    }

    private static final class MenuHolder implements InventoryHolder {
        private final UUID owner;
        private final Map<Integer, Action> actions = new LinkedHashMap<>();
        private Inventory inventory;

        private MenuHolder(UUID owner) {
            this.owner = owner;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
