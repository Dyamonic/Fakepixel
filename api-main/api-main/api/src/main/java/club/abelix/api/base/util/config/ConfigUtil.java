package club.abelix.api.base.util.config;

import lombok.experimental.UtilityClass;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class ConfigUtil {

    public List<ConfigurationSection> getList(ConfigurationSection config, String path) {
        if (!config.isList(path)) {
            throw new IllegalStateException();
        }

        return config.getList(path).stream()
                .filter(object -> object instanceof Map)
                .map(object -> fromMap(config, path, (Map<?, ?>) object))
                .collect(Collectors.toList());
    }

    public MemorySection fromMap(ConfigurationSection section, String path, Map<?, ?> map) {
        MemorySection configuration = new MemorySection(section, path);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            configuration.set(key, value instanceof Map ? fromMap(section, key, (Map<?, ?>) value) : value);
        }

        return configuration;
    }

    public static class MemorySection extends org.bukkit.configuration.MemorySection {
        protected MemorySection(ConfigurationSection parent, String path) {
            super(parent, path);
        }
    }
}
