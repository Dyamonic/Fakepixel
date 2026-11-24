package club.abelix.games.buildffa.game.event;

import club.abelix.core.bukkit.event.YEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author milansky
 */
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class GameDeathEvent extends YEvent {
    Player killer, victim;
    int baseCoins;
    int baseKillRating;
    int streakBonusRating;
    int finalCoinsAwarded;
    int finalRatingAwarded;
    int victimRatingLost;
    int eventBonusCoins;
    int eventBonusRating;
    Location deathLocation;
    boolean progressionEligible;

    public GameDeathEvent(Player killer, Player victim, int baseCoins, int baseKillRating, int streakBonusRating,
                          int finalCoinsAwarded, int finalRatingAwarded, int victimRatingLost, Location deathLocation,
                          boolean progressionEligible) {
        this(killer, victim, baseCoins, baseKillRating, streakBonusRating, finalCoinsAwarded, finalRatingAwarded,
                victimRatingLost, 0, 0, deathLocation, progressionEligible);
    }
}
