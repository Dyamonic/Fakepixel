package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.network.SMPServerType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class LocationDisplayResolver {
    private final JavaPlugin plugin;

    public LocationDisplayResolver(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String resolve(ZoneManager.ZoneSnapshot zone, String dungeonName) {
        String zoneName = zone == null ? "" : zone.name();
        if (isNamedZone(zoneName)) return zoneName;
        return fallback(currentServerType(), currentServerId(), dungeonName);
    }

    public String fallback(SMPServerType serverType, String serverId, String dungeonName) {
        SMPServerType resolvedType = resolveServerType(serverType, serverId);
        if (resolvedType == SMPServerType.HUB) return setting("hub.defaultLocationName", "Hub");
        if (resolvedType == SMPServerType.DUNGEON) {
            if (dungeonName != null && !dungeonName.isBlank() && !dungeonName.equalsIgnoreCase("None")) return dungeonName;
            return setting("dungeon.defaultLocationName", "Dungeon");
        }
        return setting("overworld.defaultLocationName", "Wilderness");
    }

    public SMPServerType resolveServerType(SMPServerType serverType, String serverId) {
        String id = serverId == null ? "" : serverId.toLowerCase(Locale.ROOT);
        if (id.startsWith("smpdun-")) return SMPServerType.DUNGEON;
        if (id.startsWith("smpworld-")) return SMPServerType.OVERWORLD;
        if (id.startsWith("smp-")) return SMPServerType.HUB;
        return serverType == null || serverType == SMPServerType.UNKNOWN ? SMPServerType.OVERWORLD : serverType;
    }

    public ValidationReport validate() {
        String hub = fallback(SMPServerType.HUB, "smp-1", null);
        String hubById = fallback(SMPServerType.UNKNOWN, "smp-2", null);
        String overworld = fallback(SMPServerType.OVERWORLD, "smpworld-1", null);
        String dungeon = fallback(SMPServerType.DUNGEON, "smpdun-1", null);
        String named = resolveNamed("Castle", SMPServerType.HUB, "smp-1");
        return new ValidationReport(!hub.equalsIgnoreCase("Wilderness"), hub.equals("Hub"), hubById.equals("Hub"),
                overworld.equals("Wilderness"), !dungeon.isBlank() && !dungeon.equalsIgnoreCase("Wilderness"),
                named.equals("Castle"));
    }

    private String resolveNamed(String zoneName, SMPServerType serverType, String serverId) {
        if (isNamedZone(zoneName)) return zoneName;
        return fallback(serverType, serverId, null);
    }

    private boolean isNamedZone(String zoneName) {
        return zoneName != null && !zoneName.isBlank() && !zoneName.equalsIgnoreCase("Wilderness")
                && !zoneName.equalsIgnoreCase("None");
    }

    private SMPServerType currentServerType() {
        if (plugin instanceof Game game && game.getNetworkService() != null)
            return game.getNetworkService().getCurrentServerType();
        return SMPServerType.parse(RuntimeSettings.config(plugin).getString("server-type", "OVERWORLD"));
    }

    private String currentServerId() {
        if (plugin instanceof Game game && game.getNetworkService() != null)
            return game.getNetworkService().getCurrentServerId();
        return RuntimeSettings.config(plugin).getString("server-id", plugin.getServer().getName());
    }

    private String setting(String key, String fallback) {
        String value = RuntimeSettings.config(plugin).getString(key, fallback);
        return value == null || value.isBlank() ? fallback : value;
    }

    public record ValidationReport(boolean hubNotWilderness, boolean hubFallback, boolean hubIdFallback,
                                   boolean overworldFallback, boolean dungeonFallback, boolean namedZoneOverride) {
    }
}
