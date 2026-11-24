package club.abelix.games.fakevival.proxy;

import java.util.Locale;

public final class ServerGroupResolver {
    public String normalize(String value) {
        if (value == null) return null;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "hub", "smp" -> "HUB";
            case "overworld", "world", "survival", "smpworld" -> "OVERWORLD";
            case "dungeon", "smpdun" -> "DUNGEON";
            default -> null;
        };
    }

    public boolean matchesServerId(String type, String serverId) {
        if (serverId == null) return false;
        return switch (type) {
            case "HUB" -> serverId.matches("(?i)^smp-\\d+$");
            case "OVERWORLD" -> serverId.matches("(?i)^smpworld-\\d+$");
            case "DUNGEON" -> serverId.matches("(?i)^smpdun-\\d+$");
            default -> false;
        };
    }
}
