package me.outerspace.thepit.registry;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YamlRegistryLoader {
    private YamlRegistryLoader() {}

    public static Map<String, Object> load(ThePitPlugin plugin, String name) throws Exception {
        File file = new File(plugin.getDataFolder(), name);
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(file);
        return map(configuration);
    }

    private static Map<String, Object> map(ConfigurationSection section) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) values.put(key, convert(section.get(key)));
        return values;
    }

    private static Object convert(Object value) {
        if (value instanceof ConfigurationSection) return map((ConfigurationSection) value);
        if (value instanceof List) {
            List<Object> converted = new ArrayList<>();
            for (Object item : (List<?>) value) converted.add(convert(item));
            return converted;
        }
        return value;
    }
}
