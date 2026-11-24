package club.abelix.games.fakevival.util;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/** Small idempotent guard for Bukkit teams that are mirrored through Bungee/FlameCord. */
public final class ScoreboardTeamUtil {
    private ScoreboardTeamUtil() {
    }

    public static Team getOrCreate(Scoreboard scoreboard, String requestedName) {
        if (scoreboard == null || requestedName == null || requestedName.isBlank()) return null;
        String safeName = requestedName.length() > 16 ? requestedName.substring(0, 16) : requestedName;
        Team existing = scoreboard.getTeam(safeName);
        if (existing != null) return existing;
        try {
            return scoreboard.registerNewTeam(safeName);
        } catch (IllegalArgumentException duplicate) {
            return scoreboard.getTeam(safeName);
        }
    }
}
