package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.events.PitLevelChangeEvent;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AchievementManager implements Listener {
    public enum EventType {
        PLAYER_KILL, PLAYER_ASSIST, LEVEL_CHANGED, PRESTIGE_CHANGED, PIT_GOLD_EARNED, RENOWN_EARNED,
        STREAK_CHANGED, BOUNTY_CLAIMED, BOUNTY_CHANGED, CONTRACT_COMPLETED, MYSTIC_WELL_USED,
        PASSIVE_PURCHASED, SHOP_PURCHASED, ITEM_CONSUMED, BLOCK_PLACED, BLOCK_BROKEN, FISHING_REWARD,
        TRADE_COMPLETED, EVENT_STARTED, EVENT_FINISHED, EVENT_SCORE_CHANGED, ENDER_CHEST_CHANGED,
        VOID_DEATH, RENOWN_UNLOCKED, RETURNED_TO_SPAWN
    }

    public record ProgressEvent(UUID playerUuid, EventType type, long amount, Map<String, Object> data,
                                boolean administrative) {
        public ProgressEvent {
            data = data == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(data));
        }

        public long number(String key) {
            Object value = data.get(key);
            return value instanceof Number number ? number.longValue() : 0L;
        }

        public boolean flag(String key) {
            Object value = data.get(key);
            return value instanceof Boolean bool && bool;
        }

        public String text(String key) {
            Object value = data.get(key);
            return value == null ? "" : String.valueOf(value);
        }
    }

    private final ThePitPlugin plugin;
    private final File file;
    private final Map<String, Definition> definitions = new LinkedHashMap<>();
    private final Map<String, TieredDefinition> tiered = new LinkedHashMap<>();
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> nextNotification = new ConcurrentHashMap<>();
    private final Set<String> warnings = ConcurrentHashMap.newKeySet();
    private YamlConfiguration config;
    private BukkitTask saveTask;
    private ZoneId seasonalZone = ZoneId.of("America/Los_Angeles");

    public AchievementManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "achievements.yml");
        if (!file.exists()) {
            try {
                plugin.saveResource("achievements.yml", false);
            } catch (Throwable throwable) {
                plugin.getLogger().severe("Could not create " + file.getAbsolutePath() + ": " + throwable.getMessage());
            }
        }
        reload();
    }

    public void start() {
        scheduleSaveTask();
        for (Player player : Bukkit.getOnlinePlayers()) load(player);
    }

    public void shutdown() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }
        for (UUID uuid : new ArrayList<>(profiles.keySet())) saveNow(uuid);
        profiles.clear();
        sessions.clear();
        dirty.clear();
        loading.clear();
        nextNotification.clear();
    }

    public boolean reload() {
        warnings.clear();
        plugin.getLogger().info("Loading achievements configuration from " + file.getAbsolutePath());
        try {
            YamlConfiguration loaded = loadAchievementConfiguration();
            if (loaded == null) return config != null;
            Map<String, Definition> nextDefinitions = new LinkedHashMap<>();
            Map<String, TieredDefinition> nextTiered = new LinkedHashMap<>();
            if (!loadDefinitions(loaded, nextDefinitions, nextTiered)) {
                plugin.getLogger().warning("No valid achievement definitions were found in " + file.getAbsolutePath() + ".");
                loaded = regenerateDefaults();
                nextDefinitions.clear();
                nextTiered.clear();
                if (loaded == null || !loadDefinitions(loaded, nextDefinitions, nextTiered)) return config != null;
            }
            try {
                seasonalZone = ZoneId.of(loaded.getString("achievements.seasonal.timezone", "America/Los_Angeles"));
            } catch (Exception exception) {
                seasonalZone = ZoneId.systemDefault();
                warn("seasonal-timezone", "Invalid achievements seasonal timezone. The server timezone will be used.");
            }
            config = loaded;
            definitions.clear();
            definitions.putAll(nextDefinitions);
            tiered.clear();
            tiered.putAll(nextTiered);
            if (saveTask != null) scheduleSaveTask();
            for (Player player : Bukkit.getOnlinePlayers()) {
                Session session = sessions.get(player.getUniqueId());
                if (session != null) openCategory(player, session.target, session.category, session.page);
            }
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().severe("Achievement configuration reload failed safely for " + file.getAbsolutePath() + ": " + throwable.getMessage());
            return config != null;
        }
    }

    private YamlConfiguration loadAchievementConfiguration() {
        try {
            SanitizedYaml sanitized = readSanitized(file);
            YamlConfiguration loaded = configuration(sanitized.root);
            if (!sanitized.invalidLocations.isEmpty()) {
                for (String location : sanitized.invalidLocations) {
                    warn("yaml-key-" + location, "Rejected invalid achievements YAML key at " + location + " in " + file.getAbsolutePath() + ".");
                }
                File backup = backup(file, false);
                if (backup != null) {
                    loaded.save(file);
                    plugin.getLogger().warning("Saved the sanitized achievements configuration after preserving " + backup.getAbsolutePath() + ".");
                } else {
                    plugin.getLogger().warning("The malformed achievements file was not overwritten because a backup could not be created.");
                }
            }
            return loaded;
        } catch (Throwable malformed) {
            plugin.getLogger().severe("Could not parse achievements configuration " + file.getAbsolutePath() + ": " + malformed.getMessage());
            File backup = backup(file, true);
            if (backup != null) plugin.getLogger().warning("Moved the malformed achievements configuration to " + backup.getAbsolutePath() + ".");
            else if (file.exists()) plugin.getLogger().warning("Could not preserve the malformed achievements file, so it will not be overwritten.");
            try {
                if (!file.exists()) plugin.saveResource("achievements.yml", false);
                if (file.exists() && backup != null) return configuration(readSanitized(file).root);
            } catch (Throwable regenerated) {
                plugin.getLogger().severe("Could not regenerate achievements.yml: " + regenerated.getMessage());
            }
            return loadBundledConfiguration();
        }
    }

    private SanitizedYaml readSanitized(File source) throws Exception {
        try (Reader reader = new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8)) {
            Object loaded = new Yaml().load(reader);
            if (!(loaded instanceof Map<?, ?> root)) throw new IllegalArgumentException("The YAML root is not a map");
            List<String> invalid = new ArrayList<>();
            return new SanitizedYaml(sanitizeMap(root, "", invalid), invalid);
        }
    }

    private YamlConfiguration loadBundledConfiguration() {
        try (InputStream input = plugin.getResource("achievements.yml")) {
            if (input == null) throw new IllegalStateException("Bundled achievements.yml is missing");
            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Object loaded = new Yaml().load(reader);
                if (!(loaded instanceof Map<?, ?> root)) throw new IllegalArgumentException("Bundled YAML root is not a map");
                List<String> invalid = new ArrayList<>();
                Map<String, Object> sanitized = sanitizeMap(root, "", invalid);
                if (!invalid.isEmpty()) throw new IllegalArgumentException("Bundled achievements.yml contains invalid keys at " + String.join(", ", invalid));
                return configuration(sanitized);
            }
        } catch (Throwable throwable) {
            plugin.getLogger().severe("Bundled achievements.yml could not be loaded safely: " + throwable.getMessage());
            return emergencyConfiguration();
        }
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> input, String parent, List<String> invalid) {
        Map<String, Object> output = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            String raw = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
            String key = raw.trim();
            String location = parent.isEmpty() ? key : parent + "." + key;
            if (key.isEmpty()) {
                invalid.add(parent.isEmpty() ? "<root>.<empty>" : parent + ".<empty>");
                continue;
            }
            if (hasEmptyPathSegment(key)) {
                invalid.add(location + " (empty Bukkit path segment)");
                continue;
            }
            if (isAchievementDefinitionParent(parent) && normalize(key).isEmpty()) {
                invalid.add(location + " (achievement ID normalizes to empty)");
                continue;
            }
            if (output.containsKey(key)) {
                invalid.add(location + " (duplicate after trimming)");
                continue;
            }
            output.put(key, sanitizeValue(entry.getValue(), location, invalid));
        }
        return output;
    }

    private boolean hasEmptyPathSegment(String key) {
        if (key == null || key.isEmpty()) return true;
        String[] segments = key.split("\\.", -1);
        for (String segment : segments) if (segment.trim().isEmpty()) return true;
        return false;
    }

    private Object sanitizeValue(Object value, String location, List<String> invalid) {
        if (value instanceof Map<?, ?> map) return sanitizeMap(map, location, invalid);
        if (value instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            int index = 0;
            for (Object entry : iterable) values.add(sanitizeValue(entry, location + "[" + index++ + "]", invalid));
            return values;
        }
        return value;
    }

    private boolean isAchievementDefinitionParent(String path) {
        return path.equals("achievements.tiered") || path.equals("achievements.one-time");
    }

    private YamlConfiguration configuration(Map<String, Object> values) {
        YamlConfiguration output = new YamlConfiguration();
        apply(output, values, "");
        return output;
    }

    private void apply(ConfigurationSection section, Map<String, Object> values, String parent) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            String location = parent.isEmpty() ? key : parent + "." + key;
            if (key.isEmpty() || hasEmptyPathSegment(key)) {
                warn("yaml-write-" + parent, "Refused to write an empty achievements YAML path below " + (parent.isEmpty() ? "<root>" : parent) + ".");
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> child = new LinkedHashMap<>();
                for (Map.Entry<?, ?> childEntry : map.entrySet()) {
                    String childKey = childEntry.getKey() == null ? "" : String.valueOf(childEntry.getKey()).trim();
                    if (childKey.isEmpty() || hasEmptyPathSegment(childKey)) {
                        warn("yaml-write-" + location, "Refused to write an empty achievements YAML path below " + location + ".");
                        continue;
                    }
                    child.put(childKey, childEntry.getValue());
                }
                ConfigurationSection created = section.createSection(key);
                apply(created, child, location);
            } else {
                section.set(key, value);
            }
        }
    }

    private File backup(File source, boolean move) {
        if (source == null || !source.exists()) return null;
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(new Date());
        String name = source.getName().endsWith(".yml")
                ? source.getName().substring(0, source.getName().length() - 4) + "." + timestamp + ".broken.yml"
                : source.getName() + "." + timestamp + ".broken.yml";
        File target = new File(source.getParentFile(), name);
        try {
            if (move) Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            else Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
            return target;
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not preserve malformed achievements configuration at " + target.getAbsolutePath() + ": " + exception.getMessage());
            return null;
        }
    }

    private YamlConfiguration emergencyConfiguration() {
        YamlConfiguration output = new YamlConfiguration();
        output.set("achievements.enabled", false);
        output.set("achievements.save-interval-seconds", 10L);
        output.set("achievements.seasonal.timezone", "America/Los_Angeles");
        output.set("achievements.systems.CORE", true);
        output.set("achievements.one-time.first-kill.enabled", true);
        output.set("achievements.one-time.first-kill.name", "&aFirst Kill");
        output.set("achievements.one-time.first-kill.category", "COMBAT");
        output.set("achievements.one-time.first-kill.points", 0);
        output.set("achievements.one-time.first-kill.requirement", 1L);
        output.set("achievements.one-time.first-kill.required-system", "CORE");
        output.set("achievements.one-time.first-kill.description", "Complete one valid player kill.");
        return output;
    }

    private YamlConfiguration regenerateDefaults() {
        File backup = backup(file, true);
        if (backup == null && file.exists()) {
            plugin.getLogger().warning("The achievements configuration was not regenerated because its backup could not be created.");
            return loadBundledConfiguration();
        }
        try {
            plugin.saveResource("achievements.yml", false);
            plugin.getLogger().warning("Regenerated achievements.yml after preserving " + (backup == null ? "the invalid configuration" : backup.getAbsolutePath()) + ".");
            return configuration(readSanitized(file).root);
        } catch (Throwable throwable) {
            plugin.getLogger().severe("Could not regenerate achievements.yml: " + throwable.getMessage());
            return loadBundledConfiguration();
        }
    }

    private void scheduleSaveTask() {
        if (saveTask != null) saveTask.cancel();
        long period = Math.max(100L, config.getLong("achievements.save-interval-seconds", 10L) * 20L);
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirty, period, period);
    }

    public void load(Player player) {
        if (player == null || !loading.add(player.getUniqueId())) return;
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String stored = plugin.getStorageManager().getOverride("achievements", uuid.toString()).orElse("");
            Profile profile = decode(stored);
            profile.uuid = uuid;
            Bukkit.getScheduler().runTask(plugin, () -> {
                loading.remove(uuid);
                if (!player.isOnline()) return;
                profiles.put(uuid, profile);
                if (config.getBoolean("achievements.migration.evaluate-existing-lifetime-stats", true) && !profile.migrated) {
                    evaluateExisting(player, profile);
                    profile.migrated = true;
                    dirty.add(uuid);
                }
            });
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        sessions.remove(uuid);
        nextNotification.remove(uuid);
        Profile profile = profiles.remove(uuid);
        if (profile != null) {
            String value = encode(profile);
            dirty.remove(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> plugin.getStorageManager().putOverride("achievements", uuid.toString(), value));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelChanged(PitLevelChangeEvent event) {
        publish(new ProgressEvent(event.getPlayerUuid(), EventType.LEVEL_CHANGED, event.getNewLevel(),
                Map.of("old", event.getOldLevel(), "source", event.getSource()), event.isAdministrative() || event.isPrestigeReset()));
    }

    public void publish(ProgressEvent event) {
        if (event == null || event.playerUuid() == null || event.administrative()
                || !config.getBoolean("achievements.enabled", true)) return;
        Profile profile = profiles.get(event.playerUuid());
        if (profile == null) return;
        Player player = Bukkit.getPlayer(event.playerUuid());
        switch (event.type()) {
            case PLAYER_KILL -> handleKill(player, profile, event);
            case LEVEL_CHANGED -> {
                setTierProgress(player, profile, "prestige", plugin.getStatsManager().getPrestige(event.playerUuid()), true);
                if (event.amount() >= 100L) complete(player, profile, "triple-digits", 1, true);
            }
            case PRESTIGE_CHANGED -> {
                setTierProgress(player, profile, "prestige", event.amount(), true);
                if (event.amount() >= 20L) complete(player, profile, "the-xx", 1, true);
                checkBigTime(player, profile);
            }
            case PIT_GOLD_EARNED -> {
                addTierProgress(player, profile, "gold", event.amount(), true);
                if (plugin.getPitGoldService().balance(event.playerUuid()) >= 250000) complete(player, profile, "gold-magnate", 1, true);
                if (event.text("source").equals("COLLECTIBLE")) progress(player, profile, "ingots-collector",
                        plugin.getStatsManager().getLongStat(event.playerUuid(), "ground_ingots_collected"), true);
            }
            case RENOWN_EARNED -> addTierProgress(player, profile, "renown", event.amount(), true);
            case STREAK_CHANGED -> handleStreak(player, profile, event.amount());
            case BOUNTY_CLAIMED -> {
                complete(player, profile, "bounty-claimed", 1, true);
                if (event.amount() >= 5000L) complete(player, profile, "tryhard-down", 1, true);
                if (event.amount() >= 500L && event.flag("bounty_hunter")) increment(player, profile, "bounty-hunter", 1L, true);
            }
            case BOUNTY_CHANGED -> {
                if (event.amount() >= 5000L) complete(player, profile, "theyre-after-me-now", 1, true);
            }
            case CONTRACT_COMPLETED -> {
                addTierProgress(player, profile, "contracts", 1L, true);
                if (event.number("duration_millis") > 0L && event.number("duration_millis") < 60000L) complete(player, profile, "fast-contract", 1, true);
            }
            case MYSTIC_WELL_USED -> addTierProgress(player, profile, "mysticism", 1L, true);
            case PASSIVE_PURCHASED -> {
                if (event.text("id").equalsIgnoreCase("el-gato") && event.amount() >= 5L) complete(player, profile, "maximizado", 1, true);
                checkFullyUpgraded(player, profile);
            }
            case ITEM_CONSUMED -> handleConsumed(player, profile, event.text("id"));
            case BLOCK_PLACED -> {
                if (event.text("material").equalsIgnoreCase("OBSIDIAN")) {
                    profile.sessionObsidianPlaced++;
                    progress(player, profile, "locked-in", profile.sessionObsidianPlaced, true);
                }
                checkCobblestone(player, profile);
            }
            case BLOCK_BROKEN -> {
                if (event.text("material").equalsIgnoreCase("OBSIDIAN") && !event.flag("golden_pickaxe")) {
                    increment(player, profile, "im-mining-here", 1L, true);
                }
            }
            case FISHING_REWARD -> {
                if (event.flag("fresh_aqua_pants") && event.number("fishing_club_tier") >= 5L) complete(player, profile, "in-the-club", 1, true);
                if (event.flag("special_fish") && inSeason("summer")) complete(player, profile, "this-isnt-the-lobby", 1, true);
            }
            case TRADE_COMPLETED -> complete(player, profile, "businessman", 1, true);
            case EVENT_STARTED -> addTierProgress(player, profile, "events", 1L, true);
            case EVENT_SCORE_CHANGED -> {
                if (event.text("id").equals("care_package_item")) complete(player, profile, "loot-with-care", 1, true);
                else if (event.text("id").equals("ground_ingots_collected")) progress(player, profile, "ingots-collector", event.amount(), true);
                else if (event.text("id").equals("trickle_down_bonus_gold")) progress(player, profile, "golden-age", event.amount(), true);
                else if (event.text("id").equals("lucky_diamond_armor")) progress(player, profile, "lucky-diamond", event.amount(), true);
                else if (event.text("id").equals("vampire_health_restored")) progress(player, profile, "nosferatu", event.amount(), true);
                else if (event.text("id").equals("spammer_arrows_received")) progress(player, profile, "infinite-quiver", event.amount(), true);
            }
            case ENDER_CHEST_CHANGED -> checkEnderChest(player, profile);
            case VOID_DEATH -> complete(player, profile, "skywars", 1, true);
            case RENOWN_UNLOCKED -> {
                if (event.text("id").equalsIgnoreCase("fast-pass")) complete(player, profile, "fast-pass", 1, true);
                if (event.text("id").equalsIgnoreCase("scam-artist") || event.text("id").equalsIgnoreCase("scam-artist-1")) complete(player, profile, "scam-artist", 1, true);
                checkBigTime(player, profile);
            }
            case RETURNED_TO_SPAWN -> resetLife(profile.uuid);
            default -> {
            }
        }
        dirty.add(event.playerUuid());
    }

    private void handleKill(Player player, Profile profile, ProgressEvent event) {
        addTierProgress(player, profile, "kills", 1L, true);
        long now = System.currentTimeMillis();
        if (now - profile.lastKillAt > 5000L) profile.pentakill = 0;
        profile.lastKillAt = now;
        profile.pentakill++;
        progress(player, profile, "pentakill", profile.pentakill, true);
        if (event.flag("strength_max")) complete(player, profile, "powerful", 1, true);
    }

    private void handleStreak(Player player, Profile profile, long streak) {
        progress(player, profile, "the-streak", streak, true);
        progress(player, profile, "god-walking-amongst-mortals", streak, true);
        if (streak >= 10L && player != null && plugin.getPerkManager().has(player, "rambo")) complete(player, profile, "rambo", 1, true);
    }

    private void handleConsumed(Player player, Profile profile, String id) {
        String normalized = normalize(id);
        long now = System.currentTimeMillis();
        profile.foods.entrySet().removeIf(entry -> now - entry.getValue() > 300000L);
        if (Set.of("golden-apple", "golden-head", "olympus-potion", "tasty-soup", "tasty-steak").contains(normalized)) {
            profile.foods.put(normalized, now);
            if (profile.foods.keySet().containsAll(Set.of("golden-apple", "golden-head", "olympus-potion", "tasty-soup", "tasty-steak"))) {
                complete(player, profile, "big-belly", 1, true);
            }
        }
        if (normalized.equals("golden-head")) {
            long consumed = plugin.getStatsManager().getLongStat(profile.uuid, StatsManager.GOLDEN_HEADS_EATEN);
            progress(player, profile, "golden-treat", consumed, true);
        }
    }

    public void resetLife(UUID uuid) {
        Profile profile = profiles.get(uuid);
        if (profile == null) return;
        profile.pentakill = 0;
        profile.lastKillAt = 0L;
        profile.sessionObsidianPlaced = 0L;
        profile.foods.clear();
    }

    public void resetSessions() {
        for (UUID uuid : profiles.keySet()) resetLife(uuid);
    }

    public void open(Player viewer, Player target) {
        if (viewer == null || target == null) return;
        if (!profiles.containsKey(target.getUniqueId())) {
            viewer.sendMessage(CC.color("&cThat player's achievement profile is not loaded."));
            return;
        }
        openMain(viewer, target.getUniqueId());
    }

    private void openMain(Player viewer, UUID target) {
        MainHolder holder = new MainHolder(target);
        Inventory inventory = Bukkit.createInventory(holder, menuSize(), CC.color(config.getString("achievements.title", "&8The Pit Achievements")));
        holder.inventory = inventory;
        fill(inventory);
        Profile profile = profiles.get(target);
        List<String> summary = Arrays.asList(
                "&7Achievement Points: &e" + number(points(profile)),
                "&7Completed: &a" + completedCount(profile) + "&7/&a" + totalCount(),
                "&7Completion: &b" + completionPercent(profile) + "%"
        );
        inventory.setItem(4, item(Material.DIAMOND, 0, "&eAchievement Summary", summary, false));
        ConfigurationSection categories = config.getConfigurationSection("achievements.menu.categories");
        if (categories != null) {
            for (String id : categories.getKeys(false)) {
                String base = "achievements.menu.categories." + id;
                int slot = config.getInt(base + ".slot", -1);
                Material material = material(config.getString(base + ".material", "PAPER"), Material.PAPER);
                if (slot < 0 || slot >= inventory.getSize()) continue;
                holder.actions.put(slot, id.toUpperCase(Locale.ENGLISH));
                long completed = definitions.values().stream().filter(definition -> definition.category.equalsIgnoreCase(id)).filter(definition -> isComplete(profile, definition.id)).count();
                inventory.setItem(slot, item(material, 0, config.getString(base + ".name", pretty(id)),
                        Arrays.asList("&7Completed: &a" + completed, "", "&eClick to view!"), false));
            }
        }
        int close = config.getInt("achievements.menu.close-slot", 49);
        holder.actions.put(close, "CLOSE");
        inventory.setItem(close, item(Material.BARRIER, 0, "&cClose", Collections.emptyList(), false));
        sessions.put(viewer.getUniqueId(), new Session(target, "MAIN", 0));
        viewer.openInventory(inventory);
    }

    private void openCategory(Player viewer, UUID target, String category, int requestedPage) {
        if (category == null || category.equals("MAIN")) {
            openMain(viewer, target);
            return;
        }
        List<Definition> entries = new ArrayList<>();
        if (category.equals("TIERED")) {
            entries.addAll(tiered.values());
        } else {
            for (Definition definition : definitions.values()) if (definition.category.equals(category)) entries.add(definition);
        }
        entries.sort(Comparator.comparing(definition -> definition.name));
        int pageSize = 28;
        int pages = Math.max(1, (entries.size() + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(pages - 1, requestedPage));
        CategoryHolder holder = new CategoryHolder(target, category, page);
        Inventory inventory = Bukkit.createInventory(holder, 54, CC.color("&8" + pretty(category) + " Achievements"));
        holder.inventory = inventory;
        fill(inventory);
        int[] slots = innerSlots();
        Profile profile = profiles.get(target);
        int start = page * pageSize;
        for (int index = 0; index < pageSize && start + index < entries.size(); index++) {
            Definition definition = entries.get(start + index);
            inventory.setItem(slots[index], achievementItem(profile, definition));
        }
        holder.actions.put(45, "BACK");
        inventory.setItem(45, item(Material.ARROW, 0, "&aGo Back", Collections.emptyList(), false));
        if (page > 0) {
            holder.actions.put(48, "PREVIOUS");
            inventory.setItem(48, item(Material.ARROW, 0, "&ePrevious Page", Collections.emptyList(), false));
        }
        if (page + 1 < pages) {
            holder.actions.put(50, "NEXT");
            inventory.setItem(50, item(Material.ARROW, 0, "&eNext Page", Collections.emptyList(), false));
        }
        holder.actions.put(53, "CLOSE");
        inventory.setItem(53, item(Material.BARRIER, 0, "&cClose", Collections.emptyList(), false));
        sessions.put(viewer.getUniqueId(), new Session(target, category, page));
        viewer.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AchievementHolder holder)) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= top.getSize()) return;
        String action = holder.actions().get(event.getRawSlot());
        if (action == null) return;
        if (action.equals("CLOSE")) player.closeInventory();
        else if (action.equals("BACK")) openMain(player, holder.target());
        else if (action.equals("PREVIOUS")) openCategory(player, holder.target(), holder.category(), holder.page() - 1);
        else if (action.equals("NEXT")) openCategory(player, holder.target(), holder.category(), holder.page() + 1);
        else openCategory(player, holder.target(), action, 0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AchievementHolder) event.setCancelled(true);
    }

    public boolean grant(Player target, String id, int tier) {
        Profile profile = profiles.get(target.getUniqueId());
        Definition definition = definition(id);
        if (profile == null || definition == null) return false;
        if (definition instanceof TieredDefinition tieredDefinition) {
            int bounded = Math.max(1, Math.min(tieredDefinition.thresholds.size(), tier));
            profile.progress.put(definition.id, tieredDefinition.thresholds.get(bounded - 1));
            for (int value = 1; value <= bounded; value++) profile.completions.put(key(definition.id, value), System.currentTimeMillis());
        } else {
            profile.progress.put(definition.id, definition.requirement);
            profile.completions.put(key(definition.id, 1), System.currentTimeMillis());
        }
        dirty.add(target.getUniqueId());
        return true;
    }

    public boolean revoke(Player target, String id, int tier) {
        Profile profile = profiles.get(target.getUniqueId());
        Definition definition = definition(id);
        if (profile == null || definition == null) return false;
        if (definition instanceof TieredDefinition tieredDefinition) {
            int bounded = Math.max(1, Math.min(tieredDefinition.thresholds.size(), tier));
            for (int value = bounded; value <= tieredDefinition.thresholds.size(); value++) profile.completions.remove(key(definition.id, value));
            profile.progress.put(definition.id, bounded <= 1 ? 0L : tieredDefinition.thresholds.get(bounded - 2));
        } else {
            profile.completions.remove(key(definition.id, 1));
            profile.progress.remove(definition.id);
        }
        dirty.add(target.getUniqueId());
        return true;
    }

    public boolean adminProgress(Player target, String id, long amount) {
        Profile profile = profiles.get(target.getUniqueId());
        Definition definition = definition(id);
        if (profile == null || definition == null || amount < 0L) return false;
        profile.progress.put(definition.id, safeAdd(profile.progress.getOrDefault(definition.id, 0L), amount));
        evaluate(target, profile, definition, false);
        dirty.add(target.getUniqueId());
        return true;
    }

    public boolean reset(Player target, String id) {
        Profile profile = profiles.get(target.getUniqueId());
        Definition definition = definition(id);
        if (profile == null || definition == null) return false;
        profile.progress.remove(definition.id);
        profile.completions.keySet().removeIf(value -> value.startsWith(definition.id + "#"));
        dirty.add(target.getUniqueId());
        return true;
    }

    public boolean resetAll(Player target) {
        Profile profile = profiles.get(target.getUniqueId());
        if (profile == null) return false;
        profile.progress.clear();
        profile.completions.clear();
        profile.migrated = false;
        resetLife(target.getUniqueId());
        dirty.add(target.getUniqueId());
        return true;
    }

    public void debug(org.bukkit.command.CommandSender sender, Player target) {
        Profile profile = profiles.get(target.getUniqueId());
        if (profile == null) {
            sender.sendMessage(CC.color("&cAchievement profile is not loaded."));
            return;
        }
        sender.sendMessage(CC.color("&eAchievements for &f" + target.getName()));
        sender.sendMessage(CC.color("&7Points: &e" + points(profile) + " &7Completed: &a" + completedCount(profile) + "&7/&a" + totalCount()));
        sender.sendMessage(CC.color("&7Progress keys: &f" + profile.progress.size() + " &7Dirty: &f" + dirty.contains(target.getUniqueId())));
    }

    public int points(UUID uuid) {
        return points(profiles.get(uuid));
    }

    public int completedCount(UUID uuid) {
        return completedCount(profiles.get(uuid));
    }

    public int totalCount() {
        int total = definitions.size();
        for (TieredDefinition definition : tiered.values()) total += definition.thresholds.size() - 1;
        return total;
    }

    public String completionPercent(UUID uuid) {
        return completionPercent(profiles.get(uuid));
    }

    public boolean isLoaded(UUID uuid) {
        return profiles.containsKey(uuid);
    }

    private void evaluateExisting(Player player, Profile profile) {
        profile.migrating = !config.getBoolean("achievements.migration.send-notifications", false);
        UUID uuid = player.getUniqueId();
        setTierProgress(player, profile, "kills", plugin.getStatsManager().getKills(uuid, Period.LIFETIME), false);
        setTierProgress(player, profile, "gold", plugin.getStatsManager().getLongStat(uuid, StatsManager.GOLD_EARNED), false);
        setTierProgress(player, profile, "contracts", plugin.getStatsManager().getLongStat(uuid, StatsManager.CONTRACTS_COMPLETED), false);
        setTierProgress(player, profile, "prestige", plugin.getStatsManager().getPrestige(uuid), false);
        setTierProgress(player, profile, "mysticism", plugin.getStatsManager().getLongStat(uuid, "mystic_well_uses"), false);
        setTierProgress(player, profile, "renown", plugin.getStatsManager().getLongStat(uuid, "lifetime_renown_earned"), false);
        if (plugin.getStatsManager().getLevel(uuid) >= 100) complete(player, profile, "triple-digits", 1, false);
        progress(player, profile, "golden-treat", plugin.getStatsManager().getLongStat(uuid, StatsManager.GOLDEN_HEADS_EATEN), false);
        progress(player, profile, "im-mining-here", plugin.getStatsManager().getLongStat(uuid, "obsidian_mined"), false);
        progress(player, profile, "ingots-collector", plugin.getStatsManager().getLongStat(uuid, "ground_ingots_collected"), false);
        progress(player, profile, "golden-age", plugin.getStatsManager().getLongStat(uuid, "trickle_down_bonus_gold"), false);
        progress(player, profile, "lucky-diamond", plugin.getStatsManager().getLongStat(uuid, "lucky_diamond_armor"), false);
        progress(player, profile, "nosferatu", (long) Math.floor(plugin.getStatsManager().getDoubleStat(uuid, "vampire_health_restored")), false);
        progress(player, profile, "infinite-quiver", plugin.getStatsManager().getLongStat(uuid, "spammer_arrows_received"), false);
        if (plugin.getPitGoldService().balance(uuid) >= 250000) complete(player, profile, "gold-magnate", 1, false);
        if (plugin.getStatsManager().getPrestige(uuid) >= 20) complete(player, profile, "the-xx", 1, false);
        checkFullyUpgraded(player, profile);
        checkBigTime(player, profile);
        profile.migrating = false;
    }

    private void addTierProgress(Player player, Profile profile, String id, long amount, boolean notify) {
        if (amount <= 0L) return;
        TieredDefinition definition = tiered.get(id);
        if (definition == null) return;
        profile.progress.put(id, safeAdd(profile.progress.getOrDefault(id, 0L), amount));
        evaluate(player, profile, definition, notify);
    }

    private void setTierProgress(Player player, Profile profile, String id, long amount, boolean notify) {
        TieredDefinition definition = tiered.get(id);
        if (definition == null) return;
        profile.progress.put(id, Math.max(profile.progress.getOrDefault(id, 0L), amount));
        evaluate(player, profile, definition, notify);
    }

    private void increment(Player player, Profile profile, String id, long amount, boolean notify) {
        Definition definition = definitions.get(id);
        if (definition == null || amount <= 0L) return;
        profile.progress.put(id, safeAdd(profile.progress.getOrDefault(id, 0L), amount));
        evaluate(player, profile, definition, notify);
    }

    private void progress(Player player, Profile profile, String id, long value, boolean notify) {
        Definition definition = definitions.get(id);
        if (definition == null) return;
        profile.progress.put(id, Math.max(profile.progress.getOrDefault(id, 0L), value));
        evaluate(player, profile, definition, notify);
    }

    private void evaluate(Player player, Profile profile, Definition definition, boolean notify) {
        if (!available(definition)) return;
        long current = profile.progress.getOrDefault(definition.id, 0L);
        if (definition instanceof TieredDefinition tieredDefinition) {
            for (int index = 0; index < tieredDefinition.thresholds.size(); index++) {
                if (current < tieredDefinition.thresholds.get(index)) break;
                complete(player, profile, definition.id, index + 1, notify);
            }
        } else if (current >= definition.requirement) {
            complete(player, profile, definition.id, 1, notify);
        }
    }

    private boolean complete(Player player, Profile profile, String id, int tierValue, boolean notify) {
        Definition definition = definition(id);
        if (definition == null || !available(definition) || !seasonActive(definition)) return false;
        String completionKey = key(id, tierValue);
        if (profile.completions.containsKey(completionKey)) return false;
        profile.completions.put(completionKey, System.currentTimeMillis());
        profile.progress.put(id, Math.max(profile.progress.getOrDefault(id, 0L), requirement(definition, tierValue)));
        dirty.add(profile.uuid);
        if (notify && !profile.migrating && player != null && player.isOnline()) notify(player, definition, tierValue);
        return true;
    }

    private void notify(Player player, Definition definition, int tierValue) {
        int points = points(definition, tierValue);
        String name = definition.name + (definition instanceof TieredDefinition ? " " + roman(tierValue) : "");
        long now = System.currentTimeMillis();
        long configuredDelay = Math.max(1L, config.getLong("achievements.notification.delay-ticks", 70L));
        long spacing = Math.max(1L, config.getLong("achievements.notification.spacing-ticks", 55L));
        long start = Math.max(now + configuredDelay * 50L, nextNotification.getOrDefault(player.getUniqueId(), 0L));
        nextNotification.put(player.getUniqueId(), start + spacing * 50L);
        long ticks = Math.max(1L, (start - now + 49L) / 50L);
        String title = config.getString("achievements.notification.title", "&e&lACHIEVEMENT UNLOCKED!");
        String subtitle = config.getString("achievements.notification.subtitle", "&a%achievement_name% &7+%points% Points")
                .replace("%achievement_name%", name).replace("%points%", String.valueOf(points));
        long scheduledTicks = plugin.getLevelUpFeedbackManager().queueTitle(player, title, subtitle, ticks);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            String chat = config.getString("achievements.notification.chat", "&e&lACHIEVEMENT! &7You completed &a%achievement_name% &7and earned &e%points% Achievement Points&7!")
                    .replace("%achievement_name%", name).replace("%points%", String.valueOf(points));
            player.sendMessage(CC.color(chat));
            try {
                player.playSound(player.getLocation(), Sound.valueOf(config.getString("achievements.notification.sound", "LEVEL_UP").toUpperCase(Locale.ENGLISH)), 1.0F, 1.0F);
            } catch (Exception exception) {
                warn("notification-sound", "Invalid achievement notification sound. Sound feedback is disabled.");
            }
            if (broadcast(definition, tierValue)) {
                String message = config.getString("achievements.broadcasts.format", "&e&lACHIEVEMENT! &r%abelix_prefix%%player% &7completed &a%achievement_name%&7!")
                        .replace("%player%", player.getName()).replace("%achievement_name%", name);
                Bukkit.broadcastMessage(CC.color(plugin.replaceNetworkPlaceholders(player, null, message)));
            }
        }, scheduledTicks);
    }

    private boolean broadcast(Definition definition, int tierValue) {
        if (!config.getBoolean("achievements.broadcasts.enabled", true)) return false;
        if (definition instanceof TieredDefinition tieredDefinition && tierValue == tieredDefinition.thresholds.size()) {
            return config.getBoolean("achievements.broadcasts.maximum-tiered", true);
        }
        return config.getStringList("achievements.broadcasts.achievements").contains(definition.id);
    }

    private void checkFullyUpgraded(Player player, Profile profile) {
        if (plugin.getPassiveManager().definitions().isEmpty()) return;
        for (me.outerspace.thepit.perk.PassiveDefinition definition : plugin.getPassiveManager().definitions()) {
            if (plugin.getStatsManager().getPassiveTier(profile.uuid, definition.id()) < 5) return;
        }
        complete(player, profile, "fully-upgraded", 1, true);
    }

    private void checkBigTime(Player player, Profile profile) {
        if (plugin.getStatsManager().getPrestige(profile.uuid) < 25) return;
        Set<String> required = plugin.getRenownShopManager().enabledUpgradeIds();
        if (required.isEmpty()) return;
        for (String id : required) if (!plugin.getStatsManager().hasRenownUpgrade(profile.uuid, id)) return;
        complete(player, profile, "big-time", 1, true);
    }

    private void checkCobblestone(Player player, Profile profile) {
        if (player == null) return;
        int amount = 0;
        for (ItemStack stack : player.getInventory().getContents()) if (stack != null && stack.getType() == Material.COBBLESTONE) amount += stack.getAmount();
        if (amount >= 64) complete(player, profile, "obelix", 1, true);
    }

    private void checkEnderChest(Player player, Profile profile) {
        if (player == null) return;
        for (ItemStack stack : player.getEnderChest().getContents()) if (stack == null || stack.getType() == Material.AIR) return;
        complete(player, profile, "full-storage", 1, true);
    }

    private ItemStack achievementItem(Profile profile, Definition definition) {
        boolean available = available(definition);
        int tierValue = highestTier(profile, definition);
        boolean completed = isComplete(profile, definition.id);
        long current = profile.progress.getOrDefault(definition.id, 0L);
        long requirement = nextRequirement(definition, tierValue);
        List<String> lore = new ArrayList<>();
        if (!available) {
            lore.add("&8Currently unavailable");
        } else if (completed && !(definition instanceof TieredDefinition tieredDefinition && tierValue < tieredDefinition.thresholds.size())) {
            long timestamp = profile.completions.getOrDefault(key(definition.id, Math.max(1, tierValue)), 0L);
            lore.add("&7Completed: &f" + date(timestamp));
            lore.add("&7Points: &e" + points(definition, Math.max(1, tierValue)));
        } else {
            lore.add("&7" + definition.description);
            lore.add("");
            lore.add("&7Progress: &e" + number(current) + "&7/&e" + number(requirement));
            lore.add("&7Reward: &e" + points(definition, Math.max(1, tierValue + 1)) + " Achievement Points");
        }
        String title = completed ? "&a" + definition.name : "&c" + definition.name;
        if (definition instanceof TieredDefinition) title += " " + roman(Math.max(1, Math.min(5, tierValue + (completed && tierValue == 5 ? 0 : 1))));
        return item(completed ? Material.DIAMOND : Material.COAL, 0, title, lore, completed);
    }

    private boolean loadDefinitions(YamlConfiguration loaded, Map<String, Definition> definitionsOut,
                                    Map<String, TieredDefinition> tieredOut) {
        ConfigurationSection tierSection = loaded.getConfigurationSection("achievements.tiered");
        if (tierSection != null) {
            for (String raw : tierSection.getKeys(false)) {
                String id = normalize(raw);
                if (id.isEmpty()) {
                    warn("definition-empty-tiered", "Skipped tiered achievement definition at achievements.tiered because its ID '" + raw + "' normalizes to empty.");
                    continue;
                }
                String base = "achievements.tiered." + raw;
                List<Long> thresholds = loaded.getLongList(base + ".thresholds");
                List<Integer> points = loaded.getIntegerList(base + ".points");
                if (definitionsOut.containsKey(id) || thresholds.size() != 5 || points.size() != 5 || !increasing(thresholds) || points.stream().anyMatch(value -> value < 0)) {
                    warn("definition-" + id, "Skipped invalid or duplicate tiered achievement definition '" + id + "'.");
                    continue;
                }
                TieredDefinition definition = new TieredDefinition(id, loaded.getString(base + ".name", pretty(id)),
                        "TIERED", loaded.getString(base + ".description", ""), loaded.getString(base + ".required-system", "CORE"),
                        loaded.getBoolean(base + ".enabled", true), loaded.getString(base + ".statistic", ""), "",
                        thresholds, points);
                definitionsOut.put(id, definition);
                tieredOut.put(id, definition);
            }
        }
        ConfigurationSection oneTime = loaded.getConfigurationSection("achievements.one-time");
        if (oneTime != null) {
            for (String raw : oneTime.getKeys(false)) {
                String id = normalize(raw);
                if (id.isEmpty()) {
                    warn("definition-empty-one-time", "Skipped one-time achievement definition at achievements.one-time because its ID '" + raw + "' normalizes to empty.");
                    continue;
                }
                String base = "achievements.one-time." + raw;
                int pointValue = loaded.getInt(base + ".points", 0);
                if (definitionsOut.containsKey(id) || pointValue < 0) {
                    warn("definition-" + id, "Skipped invalid or duplicate achievement definition '" + id + "'.");
                    continue;
                }
                definitionsOut.put(id, new Definition(id, loaded.getString(base + ".name", pretty(id)),
                        loaded.getString(base + ".category", "MISCELLANEOUS").toUpperCase(Locale.ENGLISH),
                        loaded.getString(base + ".description", ""), loaded.getString(base + ".required-system", "CORE"),
                        loaded.getBoolean(base + ".enabled", true), loaded.getString(base + ".statistic", ""),
                        loaded.getString(base + ".season", ""), Math.max(0L, loaded.getLong(base + ".requirement", 1L)), pointValue));
            }
        }
        ConfigurationSection systems = loaded.getConfigurationSection("achievements.systems");
        for (Definition definition : definitionsOut.values()) {
            if (systems == null || !systems.contains(definition.requiredSystem)) {
                warn("required-system-" + definition.id, "Achievement '" + definition.id + "' has an unknown required system '" + definition.requiredSystem + "'.");
            }
        }
        ConfigurationSection categories = loaded.getConfigurationSection("achievements.menu.categories");
        if (categories != null) {
            for (String id : categories.getKeys(false)) {
                String base = "achievements.menu.categories." + id;
                int slot = loaded.getInt(base + ".slot", -1);
                String materialName = loaded.getString(base + ".material", "");
                if (slot < 0 || slot >= 54 || Material.matchMaterial(materialName) == null) {
                    warn("menu-category-" + id, "Achievement menu category '" + id + "' has an invalid slot or material.");
                }
            }
        }
        return !definitionsOut.isEmpty();
    }

    private void flushDirty() {
        for (UUID uuid : new ArrayList<>(dirty)) {
            if (saveNow(uuid)) dirty.remove(uuid);
        }
    }

    private boolean saveNow(UUID uuid) {
        Profile profile = profiles.get(uuid);
        return profile == null || plugin.getStorageManager().putOverride("achievements", uuid.toString(), encode(profile));
    }

    private String encode(Profile profile) {
        StringBuilder output = new StringBuilder("v2|").append(profile.migrated ? '1' : '0').append('|');
        boolean first = true;
        for (Map.Entry<String, Long> entry : profile.progress.entrySet()) {
            String id = normalize(entry.getKey());
            if (id.isEmpty()) {
                warn("profile-save-progress-empty", "Skipped an achievement progress entry with an empty normalized ID for " + profile.uuid + ".");
                continue;
            }
            if (!first) output.append(',');
            first = false;
            output.append(id).append('=').append(entry.getValue());
        }
        output.append('|');
        first = true;
        for (Map.Entry<String, Long> entry : profile.completions.entrySet()) {
            String completion = normalizeCompletionKey(entry.getKey());
            if (completion.isEmpty()) {
                warn("profile-save-completion-empty", "Skipped an achievement completion entry with an empty normalized ID for " + profile.uuid + ".");
                continue;
            }
            if (!first) output.append(',');
            first = false;
            output.append(completion).append('=').append(entry.getValue());
        }
        output.append('|').append(points(profile));
        return output.toString();
    }

    private Profile decode(String value) {
        Profile profile = new Profile();
        if (value == null || value.isEmpty()) return profile;
        try {
            String[] sections = value.split("\\|", -1);
            profile.migrated = sections.length > 1 && sections[1].equals("1");
            if (sections.length > 2) decodeMap(sections[2], profile.progress);
            if (sections.length > 3) decodeMap(sections[3], profile.completions);
        } catch (Exception exception) {
            warn("profile-decode", "An achievement profile could not be decoded and will be rebuilt from lifetime statistics.");
        }
        return profile;
    }

    private void decodeMap(String encoded, Map<String, Long> output) {
        if (encoded.isEmpty()) return;
        for (String pair : encoded.split(",")) {
            int split = pair.lastIndexOf('=');
            if (split <= 0) continue;
            String raw = pair.substring(0, split);
            String id = raw.contains("#") ? normalizeCompletionKey(raw) : normalize(raw);
            if (id.isEmpty()) {
                warn("profile-load-empty-id", "Skipped stored achievement progress whose ID normalizes to empty.");
                continue;
            }
            output.put(id, Math.max(0L, Long.parseLong(pair.substring(split + 1))));
        }
    }

    private String normalizeCompletionKey(String value) {
        if (value == null) return "";
        int split = value.lastIndexOf('#');
        if (split <= 0 || split >= value.length() - 1) return "";
        String id = normalize(value.substring(0, split));
        if (id.isEmpty()) return "";
        try {
            int tierValue = Integer.parseInt(value.substring(split + 1));
            return tierValue < 1 ? "" : key(id, tierValue);
        } catch (NumberFormatException exception) {
            return "";
        }
    }

    private int points(Profile profile) {
        if (profile == null) return 0;
        long total = 0L;
        for (String value : profile.completions.keySet()) {
            int split = value.lastIndexOf('#');
            Definition definition = definition(split < 0 ? value : value.substring(0, split));
            int tierValue = split < 0 ? 1 : Integer.parseInt(value.substring(split + 1));
            if (definition != null) total += points(definition, tierValue);
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private int completedCount(Profile profile) {
        return profile == null ? 0 : profile.completions.size();
    }

    private String completionPercent(Profile profile) {
        int total = totalCount();
        return String.format(Locale.US, "%.2f", total == 0 ? 0.0D : completedCount(profile) * 100.0D / total);
    }

    private int points(Definition definition, int tierValue) {
        if (definition instanceof TieredDefinition tieredDefinition) {
            int index = Math.max(0, Math.min(tieredDefinition.points.size() - 1, tierValue - 1));
            return tieredDefinition.points.get(index);
        }
        return definition.points;
    }

    private int highestTier(Profile profile, Definition definition) {
        int maximum = definition instanceof TieredDefinition tieredDefinition ? tieredDefinition.thresholds.size() : 1;
        for (int value = maximum; value >= 1; value--) if (profile.completions.containsKey(key(definition.id, value))) return value;
        return 0;
    }

    private boolean isComplete(Profile profile, String id) {
        Definition definition = definition(id);
        return profile != null && definition != null && highestTier(profile, definition) > 0;
    }

    private long nextRequirement(Definition definition, int highestTier) {
        if (definition instanceof TieredDefinition tieredDefinition) {
            return tieredDefinition.thresholds.get(Math.min(tieredDefinition.thresholds.size() - 1, highestTier));
        }
        return definition.requirement;
    }

    private long requirement(Definition definition, int tierValue) {
        if (definition instanceof TieredDefinition tieredDefinition) {
            return tieredDefinition.thresholds.get(Math.max(0, Math.min(tieredDefinition.thresholds.size() - 1, tierValue - 1)));
        }
        return definition.requirement;
    }

    private boolean available(Definition definition) {
        return definition.enabled && config.getBoolean("achievements.systems." + definition.requiredSystem, false);
    }

    private boolean seasonActive(Definition definition) {
        return definition.season.isEmpty() || inSeason(definition.season);
    }

    private boolean inSeason(String season) {
        String base = "achievements.seasonal." + season;
        if (!config.getBoolean(base + ".enabled", false)) return false;
        try {
            MonthDay today = MonthDay.from(LocalDate.now(seasonalZone));
            MonthDay start = MonthDay.parse("--" + config.getString(base + ".start"));
            MonthDay end = MonthDay.parse("--" + config.getString(base + ".end"));
            return start.compareTo(end) <= 0 ? today.compareTo(start) >= 0 && today.compareTo(end) <= 0
                    : today.compareTo(start) >= 0 || today.compareTo(end) <= 0;
        } catch (Exception exception) {
            warn("season-" + season, "Invalid achievement season dates for '" + season + "'.");
            return false;
        }
    }

    private Definition definition(String id) {
        return definitions.get(normalize(id));
    }

    private String date(long timestamp) {
        if (timestamp <= 0L) return "Unknown";
        return new SimpleDateFormat("MM/dd/yy", Locale.US).format(new Date(timestamp));
    }

    private void fill(Inventory inventory) {
        Material material = material(config.getString("achievements.menu.filler.material", "STAINED_GLASS_PANE"), Material.STAINED_GLASS_PANE);
        short data = (short) config.getInt("achievements.menu.filler.data", 15);
        ItemStack filler = item(material, data, config.getString("achievements.menu.filler.name", " "), Collections.emptyList(), false);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack item(Material material, int data, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(material, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(CC.color(line));
            meta.setLore(colored);
            if (glow) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private int[] innerSlots() {
        int[] slots = new int[28];
        int index = 0;
        for (int row = 1; row <= 4; row++) for (int column = 1; column <= 7; column++) slots[index++] = row * 9 + column;
        return slots;
    }

    private int menuSize() {
        int size = Math.max(9, Math.min(54, config.getInt("achievements.size", 54)));
        return ((size + 8) / 9) * 9;
    }

    private Material material(String name, Material fallback) {
        Material value = Material.matchMaterial(name == null ? "" : name);
        if (value == null) warn("material-" + name, "Invalid achievement menu material '" + name + "'.");
        return value == null ? fallback : value;
    }

    private void warn(String id, String message) {
        if (warnings.add(id)) plugin.getLogger().warning(message);
    }

    private String number(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, value));
    }

    private String pretty(String value) {
        StringBuilder output = new StringBuilder();
        for (String part : value.toLowerCase(Locale.ENGLISH).split("[-_ ]")) {
            if (!output.isEmpty()) output.append(' ');
            if (!part.isEmpty()) output.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return output.toString();
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = value.trim().toLowerCase(Locale.ENGLISH).replace('_', '-').replace(' ', '-')
                .replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
        while (normalized.startsWith("-")) normalized = normalized.substring(1);
        while (normalized.endsWith("-")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }

    private String key(String id, int tierValue) {
        return id + "#" + tierValue;
    }

    private long safeAdd(long first, long second) {
        if (second > 0L && first > Long.MAX_VALUE - second) return Long.MAX_VALUE;
        return Math.max(0L, first + second);
    }

    private boolean increasing(List<Long> values) {
        long previous = Long.MIN_VALUE;
        for (long value : values) {
            if (value <= previous || value < 0L) return false;
            previous = value;
        }
        return true;
    }

    private static class Definition {
        private final String id;
        private final String name;
        private final String category;
        private final String description;
        private final String requiredSystem;
        private final boolean enabled;
        private final String statistic;
        private final String season;
        private final long requirement;
        private final int points;

        private Definition(String id, String name, String category, String description, String requiredSystem,
                           boolean enabled, String statistic, String season, long requirement, int points) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.description = description;
            this.requiredSystem = requiredSystem;
            this.enabled = enabled;
            this.statistic = statistic;
            this.season = season;
            this.requirement = requirement;
            this.points = points;
        }
    }

    private record SanitizedYaml(Map<String, Object> root, List<String> invalidLocations) {
    }

    private static final class TieredDefinition extends Definition {
        private final List<Long> thresholds;
        private final List<Integer> points;

        private TieredDefinition(String id, String name, String category, String description, String requiredSystem,
                                 boolean enabled, String statistic, String season, List<Long> thresholds, List<Integer> points) {
            super(id, name, category, description, requiredSystem, enabled, statistic, season, thresholds.get(0), points.get(0));
            this.thresholds = List.copyOf(thresholds);
            this.points = List.copyOf(points);
        }
    }

    private static final class Profile {
        private UUID uuid;
        private final Map<String, Long> progress = new LinkedHashMap<>();
        private final Map<String, Long> completions = new LinkedHashMap<>();
        private final Map<String, Long> foods = new HashMap<>();
        private boolean migrated;
        private boolean migrating;
        private long lastKillAt;
        private int pentakill;
        private long sessionObsidianPlaced;
    }

    private interface AchievementHolder extends InventoryHolder {
        UUID target();
        String category();
        int page();
        Map<Integer, String> actions();
    }

    private static final class MainHolder implements AchievementHolder {
        private final UUID target;
        private final Map<Integer, String> actions = new HashMap<>();
        private Inventory inventory;

        private MainHolder(UUID target) {
            this.target = target;
        }

        public UUID target() { return target; }
        public String category() { return "MAIN"; }
        public int page() { return 0; }
        public Map<Integer, String> actions() { return actions; }
        public Inventory getInventory() { return inventory; }
    }

    private static final class CategoryHolder implements AchievementHolder {
        private final UUID target;
        private final String category;
        private final int page;
        private final Map<Integer, String> actions = new HashMap<>();
        private Inventory inventory;

        private CategoryHolder(UUID target, String category, int page) {
            this.target = target;
            this.category = category;
            this.page = page;
        }

        public UUID target() { return target; }
        public String category() { return category; }
        public int page() { return page; }
        public Map<Integer, String> actions() { return actions; }
        public Inventory getInventory() { return inventory; }
    }

    private record Session(UUID target, String category, int page) {
    }
}
