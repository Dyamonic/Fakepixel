package club.abelix.network.session;

import java.util.Map;

public record PlayerSession(
        String player,
        String proxyName,
        long lastUpdate
) {

    public static PlayerSession fromHash(Map<String, String> sessionData) {
        return new PlayerSession(
                sessionData.get("player"),
                sessionData.get("proxyName"),
                Long.parseLong(sessionData.get("lastUpdate"))
        );
    }

    public Map<String, String> asHash() {
        return Map.of(
                "player", player,
                "proxyName", proxyName,
                "lastUpdate", String.valueOf(lastUpdate)
        );
    }

}
