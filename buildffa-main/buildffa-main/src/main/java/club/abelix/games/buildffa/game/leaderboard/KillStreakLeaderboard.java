package club.abelix.games.buildffa.game.leaderboard;

import club.abelix.api.base.parkour.LobbyParkour;
import club.abelix.api.base.playertop.provider.PropertyTopProvider;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import lombok.NonNull;

/**
 * @author milansky
 */
public final class KillStreakLeaderboard extends PropertyTopProvider {
    public KillStreakLeaderboard() {
        super(BuildFFAGamer.BEST_KILL_STREAK_PROPERTY_NAME, 10, true);
    }
}
