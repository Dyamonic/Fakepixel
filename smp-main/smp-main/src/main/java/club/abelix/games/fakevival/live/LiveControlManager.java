package club.abelix.games.fakevival.live;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class LiveControlManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final Map<String, LiveControlDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, LiveControlValue> overrides = new LinkedHashMap<>();
    private final Set<String> warnedInvalidBooleans = new java.util.HashSet<>();

    public LiveControlManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        registerDefaults();
    }

    public void setup() {
        if (!dataManager.isMySqlEnabled()) {
            plugin.getLogger().warning("SQL live controls are unavailable; using in-memory defaults only.");
        }
        reload();
    }

    public void reload() {
        overrides.clear();
        if (dataManager.isMySqlEnabled()) {
            loadFromDatabase();
        } else {
            loadFromYamlFallback();
        }
        applyToRuntimeConfig();
    }

    public Collection<LiveControlDefinition> definitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public LiveControlDefinition definition(String key) {
        return definitionOrDynamic(key);
    }

    public boolean hasOverride(String key) {
        return overrides.containsKey(normalizeKey(key));
    }

    public LiveControlValue override(String key) {
        return overrides.get(normalizeKey(key));
    }

    public String value(String key) {
        key = normalizeKey(key);
        LiveControlValue override = overrides.get(key);
        if (override != null) return override.value();
        LiveControlDefinition definition = definitions.get(key);
        return definition == null ? "" : definition.defaultValue();
    }

    public boolean bool(String key) {
        LiveControlDefinition definition = definitionOrDynamic(key);
        String raw = value(key);
        Boolean parsed = parseBoolean(raw);
        if (parsed != null) return parsed;
        if (!isBlank(raw)) warnInvalidBooleanOnce(key, raw);
        return definition != null && definition.defaultBoolean();
    }

    public double decimal(String key) {
        LiveControlDefinition definition = definitions.get(normalizeKey(key));
        String raw = value(key);
        if (!isValid(LiveControlType.DOUBLE, raw)) {
            plugin.getLogger().warning("Invalid live control decimal for " + key + ": " + raw + ". Using default.");
            return definition == null ? 1.0D : definition.defaultDouble();
        }
        return Math.clamp(Double.parseDouble(raw), 0.0D, 100.0D);
    }

    public boolean isMaintenanceEnabled() {
        return bool("maintenance.enabled");
    }

    public String maintenanceMessage() {
        return ChatColor.translateAlternateColorCodes('&', value("maintenance.message"));
    }

    public boolean isJoiningBlocked() {
        return bool("joining.blocked");
    }

    public String joiningBlockedMessage() {
        return ChatColor.translateAlternateColorCodes('&', value("joining.blocked_message"));
    }

    public boolean isGuiDisabled(String gui) {
        return bool("gui." + normalizeSegment(gui) + ".disabled");
    }

    public boolean isNpcDisabled(String npc) {
        return bool("npc." + normalizeSegment(npc) + ".disabled");
    }

    public boolean isSystemDisabled(String system) {
        return bool("system." + normalizeSegment(system) + ".disabled");
    }

    public boolean isQuestDisabled(String quest) {
        return bool("quest." + normalizeSegment(quest) + ".disabled");
    }

    public boolean isShopDisabled(String shop) {
        return bool("shop." + normalizeSegment(shop) + ".disabled");
    }

    public double xpMultiplier() {
        return decimal("multiplier.xp");
    }

    public double coinsMultiplier() {
        return decimal("multiplier.coins");
    }

    public boolean setOverride(String key, String rawValue, String updatedBy, String reason) {
        key = normalizeKey(key);
        LiveControlDefinition definition = definitionOrDynamic(key);
        if (definition == null || !isValid(definition.type(), rawValue)) return false;

        String oldValue = value(key);
        String normalizedValue = normalizeValue(definition.type(), rawValue);
        LiveControlValue value = new LiveControlValue(key, normalizedValue, definition.type(), definition.category(), clean(reason), clean(updatedBy), System.currentTimeMillis());
        if (dataManager.isMySqlEnabled()) {
            saveToDatabase(value);
        } else {
            saveToYamlFallback(value);
        }
        overrides.put(key, value);
        applyToRuntimeConfig();
        logChange(updatedBy, key, oldValue, normalizedValue, reason, value.updatedAt());
        return true;
    }

    public boolean resetOverride(String key, String updatedBy, String reason) {
        key = normalizeKey(key);
        LiveControlDefinition definition = definitionOrDynamic(key);
        if (definition == null || !overrides.containsKey(key)) return false;
        String oldValue = value(key);
        if (dataManager.isMySqlEnabled()) {
            deleteFromDatabase(key);
        } else {
            dataManager.getData().set("live-control-overrides." + encodedKey(key), null);
            dataManager.save();
        }
        overrides.remove(key);
        applyToRuntimeConfig();
        logChange(updatedBy, key, oldValue, definition.defaultValue(), reason + " (reset to default)", System.currentTimeMillis());
        return true;
    }

    private void registerDefaults() {
        control("maintenance.enabled", LiveControlType.BOOLEAN, "maintenance", "false", true);
        control("maintenance.message", LiveControlType.STRING, "maintenance", "&cSMP is currently under maintenance.", false);
        control("joining.blocked", LiveControlType.BOOLEAN, "joining", "false", true);
        control("joining.blocked_message", LiveControlType.STRING, "joining", "&cJoining the SMP is temporarily blocked.", false);
        control("gui.main-menu.disabled", LiveControlType.BOOLEAN, "gui", "false", false);
        control("gui.bazaar.disabled", LiveControlType.BOOLEAN, "gui", "false", false);
        control("gui.skills.disabled", LiveControlType.BOOLEAN, "gui", "false", false);
        control("gui.jobs.disabled", LiveControlType.BOOLEAN, "gui", "false", false);
        control("gui.quests.disabled", LiveControlType.BOOLEAN, "gui", "false", false);
        control("gui.shops.disabled", LiveControlType.BOOLEAN, "gui", "false", false);
        control("npc.blacksmith.disabled", LiveControlType.BOOLEAN, "npc", "false", false);
        control("npc.merchant.disabled", LiveControlType.BOOLEAN, "npc", "false", false);
        control("npc.adventure_guild.disabled", LiveControlType.BOOLEAN, "npc", "false", false);
        control("npc.sandbox.disabled", LiveControlType.BOOLEAN, "npc", "false", false);
        control("system.npcs.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("system.quests.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("system.shops.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("system.dungeons.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("system.guilds.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("system.bazaar.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("system.battlepass.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("system.loot-boxes.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("system.auctionhouse.disabled", LiveControlType.BOOLEAN, "system", "false", true);
        control("dupe-detection.enabled", LiveControlType.BOOLEAN, "system", "true", true);
        control("dupe-detection.scan-ender-chest", LiveControlType.BOOLEAN, "system", "true", false);
        control("dupe-detection.scan-world-drops", LiveControlType.BOOLEAN, "system", "false", false);
        control("dupe-detection.remove-duplicate", LiveControlType.BOOLEAN, "system", "false", true);
        control("dupe-detection.fake-menu-chests.enabled", LiveControlType.BOOLEAN, "system", "true", true);
        control("dupe-detection.track-all-items", LiveControlType.BOOLEAN, "system", "false", true);
        control("dupe-detection.track-stacked-items", LiveControlType.BOOLEAN, "system", "false", true);
        control("multiplier.xp", LiveControlType.DOUBLE, "multiplier", "1.0", false);
        control("multiplier.coins", LiveControlType.DOUBLE, "multiplier", "1.0", false);
        control("quest.daily_mining.disabled", LiveControlType.BOOLEAN, "quest", "false", false);
        control("shop.blocks.disabled", LiveControlType.BOOLEAN, "shop", "false", false);
    }

    private void control(String key, LiveControlType type, String category, String defaultValue, boolean major) {
        String normalizedKey = normalizeKey(key);
        definitions.put(normalizedKey, new LiveControlDefinition(normalizedKey, type, category, defaultValue, major));
    }

    private void loadFromDatabase() {
        String table = dataManager.getLiveControlsTable();
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT `control_key`, `control_value`, `value_type`, `category`, `reason`, `updated_by`, `updated_at` FROM `" + table + "`")) {
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    readOverride(result.getString("control_key"), result.getString("control_value"), result.getString("value_type"),
                            result.getString("category"), result.getString("reason"), result.getString("updated_by"), result.getLong("updated_at"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not load SMP live controls from database: " + e.getMessage());
        }
    }

    private void loadFromYamlFallback() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection("live-control-overrides");
        if (section == null) return;
        for (String encodedKey : section.getKeys(false)) {
            String key = decodedKey(encodedKey);
            readOverride(key, section.getString(encodedKey + ".control_value", ""), section.getString(encodedKey + ".value_type", ""),
                    section.getString(encodedKey + ".category", ""), section.getString(encodedKey + ".reason", ""), section.getString(encodedKey + ".updated_by", ""), section.getLong(encodedKey + ".updated_at", 0L));
        }
    }

    private void readOverride(String key, String rawValue, String rawType, String category, String reason, String updatedBy, long updatedAt) {
        key = normalizeKey(key);
        LiveControlDefinition definition = definitionOrDynamic(key);
        if (definition == null) {
            plugin.getLogger().warning("Ignoring unknown live control key from database: " + key);
            return;
        }
        LiveControlType type;
        try {
            type = LiveControlType.valueOf(String.valueOf(rawType).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            type = definition.type();
        }
        if (definition.type() == LiveControlType.BOOLEAN && isBlank(rawValue)) return;
        if (type != definition.type() || !isValid(definition.type(), rawValue)) {
            plugin.getLogger().warning("Ignoring invalid live control value for " + key + ": " + rawValue);
            return;
        }
        overrides.put(key, new LiveControlValue(key, normalizeValue(definition.type(), rawValue), definition.type(),
                category == null || category.isBlank() ? definition.category() : category, clean(reason), clean(updatedBy), updatedAt));
    }

    private void saveToDatabase(LiveControlValue value) {
        String table = dataManager.getLiveControlsTable();
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "REPLACE INTO `" + table + "` (`control_key`, `control_value`, `value_type`, `category`, `reason`, `updated_by`, `updated_at`) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, value.key());
            statement.setString(2, value.value());
            statement.setString(3, value.type().name());
            statement.setString(4, value.category());
            statement.setString(5, value.reason());
            statement.setString(6, value.updatedBy());
            statement.setLong(7, value.updatedAt());
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not save SMP live control " + value.key() + ": " + e.getMessage());
        }
    }

    private void saveToYamlFallback(LiveControlValue value) {
        String path = "live-control-overrides." + encodedKey(value.key());
        dataManager.getData().set(path + ".control_value", value.value());
        dataManager.getData().set(path + ".value_type", value.type().name());
        dataManager.getData().set(path + ".category", value.category());
        dataManager.getData().set(path + ".reason", value.reason());
        dataManager.getData().set(path + ".updated_by", value.updatedBy());
        dataManager.getData().set(path + ".updated_at", value.updatedAt());
        dataManager.save();
    }

    private void deleteFromDatabase(String key) {
        String table = dataManager.getLiveControlsTable();
        try (Connection connection = dataManager.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM `" + table + "` WHERE `control_key`=?")) {
            statement.setString(1, key);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not reset SMP live control " + key + ": " + e.getMessage());
        }
    }

    private void applyToRuntimeConfig() {
        for (LiveControlDefinition definition : definitions.values()) {
            applyRuntimeValue(definition.key(), value(definition.key()), definition.type());
        }
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("live-controls.maintenance.enabled", isMaintenanceEnabled());
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("live-controls.maintenance.message", value("maintenance.message"));
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("live-controls.joining.blocked", isJoiningBlocked());
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("live-controls.multipliers.xp", xpMultiplier());
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("live-controls.multipliers.coins", coinsMultiplier());
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("guilds.enabled", !isSystemDisabled("guilds"));
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("dungeons.enabled", !isSystemDisabled("dungeons"));
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("bazaar.enabled", !isSystemDisabled("bazaar"));
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("battlepass.enabled", !isSystemDisabled("battlepass"));
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("loot-boxes.enabled", !isSystemDisabled("loot-boxes"));
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("quests.enabled", !isSystemDisabled("quests"));
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("shops.enabled", !isSystemDisabled("shops"));
    }

    private void applyRuntimeValue(String key, String rawValue, LiveControlType type) {
        Object value = switch (type) {
            case BOOLEAN -> {
                LiveControlDefinition definition = definitions.get(normalizeKey(key));
                Boolean parsed = parseBoolean(rawValue);
                yield parsed == null ? definition != null && definition.defaultBoolean() : parsed;
            }
            case DOUBLE -> {
                try {
                    yield Math.clamp(Double.parseDouble(rawValue), 0.0D, 100.0D);
                } catch (NumberFormatException ignored) {
                    yield 0.0D;
                }
            }
            case STRING -> rawValue;
        };
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set(key, value);
    }

    private boolean isValid(LiveControlType type, String rawValue) {
        if (rawValue == null) return false;
        return switch (type) {
            case BOOLEAN -> parseBoolean(rawValue) != null;
            case DOUBLE -> {
                try {
                    double value = Double.parseDouble(rawValue);
                    yield value >= 0.0D && value <= 100.0D;
                } catch (NumberFormatException ignored) {
                    yield false;
                }
            }
            case STRING -> rawValue.length() <= 255;
        };
    }

    private String normalizeValue(LiveControlType type, String rawValue) {
        return switch (type) {
            case BOOLEAN -> String.valueOf(Boolean.TRUE.equals(parseBoolean(rawValue)));
            case DOUBLE -> String.format(Locale.US, "%.2f", Math.clamp(Double.parseDouble(rawValue), 0.0D, 100.0D));
            case STRING -> rawValue == null ? "" : rawValue.trim();
        };
    }

    private Boolean parseBoolean(String rawValue) {
        if (isBlank(rawValue)) return null;
        return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> Boolean.TRUE;
            case "false", "no", "off", "0" -> Boolean.FALSE;
            default -> null;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void warnInvalidBooleanOnce(String key, String rawValue) {
        String normalizedKey = normalizeKey(key);
        if (warnedInvalidBooleans.add(normalizedKey)) {
            plugin.getLogger().warning("Invalid live control boolean for " + normalizedKey + ": " + rawValue + ". Using default.");
        }
    }

    private String normalizeKey(String key) {
        return normalizeSegment(Objects.toString(key, "")).replace(' ', '.');
    }

    private LiveControlDefinition definitionOrDynamic(String key) {
        String normalizedKey = normalizeKey(key);
        LiveControlDefinition definition = definitions.get(normalizedKey);
        if (definition != null) return definition;
        if (!isDynamicBooleanControl(normalizedKey)) return null;
        LiveControlDefinition dynamic = new LiveControlDefinition(normalizedKey, LiveControlType.BOOLEAN, "bazaar", "false", false);
        definitions.put(normalizedKey, dynamic);
        return dynamic;
    }

    private boolean isDynamicBooleanControl(String key) {
        return (key.startsWith("bazaar.category.") && key.endsWith(".disabled"))
                || (key.startsWith("bazaar.item.") && key.endsWith(".disabled"))
                || (key.startsWith("shop.item.") && key.endsWith(".disabled"))
                || (key.startsWith("quest.") && key.endsWith(".disabled"));
    }

    private String normalizeSegment(String value) {
        return Objects.toString(value, "").trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String encodedKey(String key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    }

    private String decodedKey(String key) {
        try {
            return new String(Base64.getUrlDecoder().decode(key), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return key;
        }
    }

    private void logChange(String updatedBy, String key, String oldValue, String newValue, String reason, long timestamp) {
        plugin.getLogger().info("[LiveControl] " + clean(updatedBy) + " changed " + key + " from '" + oldValue + "' to '" + newValue + "' reason='" + clean(reason) + "' at " + timestamp);
    }
}
