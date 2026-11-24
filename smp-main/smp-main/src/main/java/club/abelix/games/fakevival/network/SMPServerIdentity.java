package club.abelix.games.fakevival.network;

import org.bukkit.plugin.java.JavaPlugin;

public record SMPServerIdentity(String serverId, SMPServerType serverType) {
    public static SMPServerIdentity resolve(JavaPlugin plugin) {
        String id = runtime("SMP_SERVER_ID");
        if (id == null || id.isBlank()) {
            id = "local-test";
            plugin.getLogger().warning("[SMP] SMP_SERVER_ID missing. Using local-test.");
        }
        String rawType = runtime("SMP_SERVER_TYPE");
        SMPServerType type;
        if (rawType == null || rawType.isBlank()) {
            type = SMPServerType.UNKNOWN;
            plugin.getLogger().warning("[SMP] SMP_SERVER_TYPE missing. Server-specific gameplay systems will remain guarded.");
        } else {
            type = SMPServerType.parse(rawType);
            if (!type.name().equalsIgnoreCase(rawType.trim())) {
                plugin.getLogger().warning("[SMP] Invalid SMP_SERVER_TYPE '" + rawType + "'. Server-specific gameplay systems will remain guarded.");
            }
        }
        return new SMPServerIdentity(SMPServerGroups.canonicalId(id), type);
    }

    private static String runtime(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) value = System.getenv(key);
        return value;
    }
}
