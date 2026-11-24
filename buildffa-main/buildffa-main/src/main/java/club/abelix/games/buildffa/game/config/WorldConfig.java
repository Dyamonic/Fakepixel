package club.abelix.games.buildffa.game.config;

import club.abelix.api.base.region.Region3Dim;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * @author milansky
 */
public record WorldConfig(
        String displayName,
        World world,
        Location spawnLocation,

        Region3Dim lobbyZone,

        FakePlayerConfig kitEditorConfig,
        FakePlayerConfig shopConfig,
        FakePlayerConfig divisionConfig,
        FakePlayerConfig exchangerConfig,
        FakePlayerConfig layoutChangeConfig,
        FakePlayerConfig questsConfig,
        FakePlayerConfig cosmeticConfig,

        Location killStreakLeaderboard,
        Location killsLeaderboard,
        Location ratingLeaderboard,

        Location ratingHologram,

        int moveThreshold,
        int setupMinimumHeight,
        int unvisiblePlayersHeight,
        int deathLevel
) {
}
