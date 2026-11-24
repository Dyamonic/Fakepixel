package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import org.bukkit.plugin.java.JavaPlugin;

public final class SystemDefaultsRegistry {
    private final StaticConfigRegistry chatFilter;
    private final StaticConfigRegistry dungeons;
    private final StaticConfigRegistry guilds;
    private final StaticConfigRegistry languages;
    private final StaticConfigRegistry zones;
    private final StaticConfigRegistry hub;
    private final StaticConfigRegistry ranks;

    public SystemDefaultsRegistry(JavaPlugin plugin) {
        chatFilter = registry(plugin, StaticConfigDefaults.CHAT_FILTER);
        dungeons = registry(plugin, StaticConfigDefaults.DUNGEONS);
        guilds = registry(plugin, StaticConfigDefaults.GUILDS);
        languages = registry(plugin, StaticConfigDefaults.LANGUAGES);
        zones = registry(plugin, StaticConfigDefaults.ZONES);
        hub = registry(plugin, StaticConfigDefaults.HUB);
        ranks = registry(plugin, StaticConfigDefaults.RANKS);
    }

    public StaticConfigRegistry chatFilter() { return chatFilter; }
    public StaticConfigRegistry dungeons() { return dungeons; }
    public StaticConfigRegistry guilds() { return guilds; }
    public StaticConfigRegistry languages() { return languages; }
    public StaticConfigRegistry zones() { return zones; }
    public StaticConfigRegistry hub() { return hub; }
    public StaticConfigRegistry ranks() { return ranks; }

    private StaticConfigRegistry registry(JavaPlugin plugin, StaticConfigDefaults defaults) {
        StaticConfigRegistry registry = new StaticConfigRegistry(defaults) {
        };
        registry.reload(plugin);
        return registry;
    }
}
