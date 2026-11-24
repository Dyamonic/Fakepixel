package club.abelix.games.fakevival.network;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SMPServerGroups {
    private static final List<String> HUB = List.of("smp-1", "smp-2", "smp-3", "smp-4");
    private static final List<String> OVERWORLD = List.of("smpworld-1", "smpworld-2", "smpworld-3", "smpworld-4");
    private static final List<String> DUNGEON = List.of("smpdun-1", "smpdun-2", "smpdun-3", "smpdun-4");
    private static final Map<String, String> ALIASES = Map.of(
            "overworld-1", "smpworld-1",
            "overworld-2", "smpworld-2",
            "overworld-3", "smpworld-3",
            "overworld-4", "smpworld-4",
            "hub-1", "smp-1",
            "dungeon-1", "smpdun-1"
    );

    private SMPServerGroups() {
    }

    public static List<String> getServersForType(SMPServerType type) {
        return switch (type) {
            case HUB -> HUB;
            case OVERWORLD -> OVERWORLD;
            case DUNGEON -> DUNGEON;
            case UNKNOWN -> List.of();
        };
    }

    public static String canonicalId(String serverId) {
        if (serverId == null) return null;
        String normalized = serverId.trim().toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(normalized, normalized);
    }

    public static boolean contains(SMPServerType type, String serverId) {
        return getServersForType(type).contains(canonicalId(serverId));
    }
}
