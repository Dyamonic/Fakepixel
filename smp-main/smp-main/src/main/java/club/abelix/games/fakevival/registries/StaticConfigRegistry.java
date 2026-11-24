package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class StaticConfigRegistry {
    private final StaticConfigDefaults defaults;
    private FileConfiguration config;

    protected StaticConfigRegistry(StaticConfigDefaults defaults) {
        this.defaults = defaults;
    }

    public final void reload(JavaPlugin plugin) {
        config = defaults.load(plugin);
        rebuild();
    }

    protected void rebuild() {
    }

    public final FileConfiguration config() {
        return config;
    }

    public final ConfigurationSection section(String path) {
        return config.getConfigurationSection(path);
    }

    public final Set<String> keys(String path) {
        ConfigurationSection section = section(path);
        return section == null ? Set.of() : section.getKeys(false);
    }

    public final List<String> stringList(String path) {
        return config.getStringList(path);
    }

    public final boolean enabled(String path, boolean fallback) {
        return config.getBoolean(path, fallback);
    }

    protected final Map<String, Object> data(ConfigurationSection section) {
        if (section == null) return Map.of();
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nested) {
                values.put(key, data(nested));
            } else if (value instanceof List<?> list) {
                values.put(key, immutableList(list));
            } else {
                values.put(key, value);
            }
        }
        return Map.copyOf(values);
    }

    protected final String displayName(ConfigurationSection section, String fallbackId) {
        if (section == null) return prettify(fallbackId);
        String display = section.getString("display");
        if (display == null || display.isBlank()) display = section.getString("name");
        return display == null || display.isBlank() ? prettify(fallbackId) : display;
    }

    protected final String category(ConfigurationSection section, String fallback) {
        if (section == null) return fallback;
        String category = section.getString("category");
        return category == null || category.isBlank() ? fallback : category.toLowerCase(Locale.ROOT);
    }

    protected final <T> Map<String, List<T>> immutableGrouped(Map<String, List<T>> grouped) {
        Map<String, List<T>> copy = new LinkedHashMap<>();
        grouped.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    private List<?> immutableList(List<?> list) {
        List<Object> copy = new ArrayList<>();
        for (Object value : list) {
            if (value instanceof ConfigurationSection nested) {
                copy.add(data(nested));
            } else if (value instanceof List<?> nestedList) {
                copy.add(immutableList(nestedList));
            } else {
                copy.add(value);
            }
        }
        return List.copyOf(copy);
    }

    private String prettify(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        String[] words = id.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) builder.append(word.substring(1));
        }
        return builder.isEmpty() ? id : builder.toString();
    }
}
