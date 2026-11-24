package club.abelix.games.buildffa.game.manager.player;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.manager.event.BuildFFAEventManager;
import org.bukkit.Bukkit;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.Map;
@RequiredArgsConstructor
public class RewardManager {

    private static final int DEFAULT_KILL_REWARD = 3;
    private static final Map<Integer, Integer> KILL_STREAK_REWARDS = Map.of(
            5, 10,
            10, 15,
            15, 20,
            20, 25,
            25, 40,
            30, 70
    );

    private final BuildFFAGame game;

    public KillReward calculateReward(Player killer, Player victim) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Kill rewards must be calculated on the server thread");
        val killerGamer = BuildFFAGamer.get(killer);
        val killStreak = killerGamer.incrementKillStreak();

        if (killStreak % 5 == 0) {
            game.broadcastLocalized("buildffa_kill_streak_broadcast",
                    "%player%", Gamer.getGamer(killer).getDisplayName(), "%kills%", killStreak);
        }

        int killCoins = DEFAULT_KILL_REWARD + getVictimKillStreakReward(BuildFFAGamer.get(victim).getCurrentKillStreak());
        int streakCoins = KILL_STREAK_REWARDS.getOrDefault(killStreak, 0);
        int baseCoins = killCoins;
        Gamer killerAccount = Gamer.getGamer(killer);
        int eventMultiplier = game.getEventManager().getRewardMultiplier();
        BuildFFAEventManager.EventBonus eventBonus = game.getEventManager().prepareKillBonus(killer, victim, killStreak);
        int prestigeBonusCoins = game.getPrestigeManager().applyMultiplier(killerAccount, killCoins) - killCoins;
        int eventBonusCoins = killCoins * (eventMultiplier - 1) + eventBonus.getCoins();
        int finalCoinsAwarded = killCoins + prestigeBonusCoins + eventBonusCoins + streakCoins;
        int calculatedBaseRating = 2;
        int baseKillRating = Math.max(2, Math.min(8, calculatedBaseRating));
        int streakBonusRating = killStreak >= 5 ? (killStreak >= 25 ? 15 : 10) : 0;
        streakBonusRating = game.getEventManager().getStreakRatingBonus(streakBonusRating);
        int prestigeBonusRating = game.getPrestigeManager().applyMultiplier(killerAccount, baseKillRating) - baseKillRating;
        int eventBonusRating = baseKillRating * (eventMultiplier - 1) + eventBonus.getRating();
        int finalRatingAwarded = baseKillRating + prestigeBonusRating + eventBonusRating + streakBonusRating;
        int victimRatingLost = -Math.min(Math.max(0, Gamer.getGamer(victim).getLifetimeStat("rating")), 1);
        return new KillReward(baseCoins, baseKillRating, streakBonusRating,
                finalCoinsAwarded, finalRatingAwarded, victimRatingLost, eventBonus.getCoins(), eventBonus.getRating(),
                prestigeBonusCoins, prestigeBonusRating, streakCoins,
                killCoins * (eventMultiplier - 1), baseKillRating * (eventMultiplier - 1));
    }

    private static int getVictimKillStreakReward(int victimKillStreak) {
        if (victimKillStreak >= 28) return 70;
        if (victimKillStreak >= 23) return 40;
        if (victimKillStreak >= 18) return 25;
        if (victimKillStreak >= 13) return 20;
        if (victimKillStreak >= 8) return 15;
        if (victimKillStreak >= 3) return 10;

        return 0;
    }

    public static final class KillReward {
        private final int eventMultiplierBonusCoins;
        private final int eventMultiplierBonusRating;
        private final int prestigeBonusCoins;
        private final int prestigeBonusRating;
        private final int streakBonusCoins;
        private final int baseCoins;
        private final int baseKillRating;
        private final int streakBonusRating;
        private final int finalCoinsAwarded;
        private final int finalRatingAwarded;
        private final int victimRatingLost;
        private final int eventBonusCoins;
        private final int eventBonusRating;

        KillReward(int baseCoins, int baseKillRating, int streakBonusRating, int finalCoinsAwarded,
                   int finalRatingAwarded, int victimRatingLost, int eventBonusCoins, int eventBonusRating,
                   int prestigeBonusCoins, int prestigeBonusRating, int streakBonusCoins,
                   int eventMultiplierBonusCoins, int eventMultiplierBonusRating) {
            this.eventMultiplierBonusCoins = eventMultiplierBonusCoins;
            this.eventMultiplierBonusRating = eventMultiplierBonusRating;
            this.prestigeBonusCoins = prestigeBonusCoins;
            this.prestigeBonusRating = prestigeBonusRating;
            this.streakBonusCoins = streakBonusCoins;
            this.baseCoins = baseCoins;
            this.baseKillRating = baseKillRating;
            this.streakBonusRating = streakBonusRating;
            this.finalCoinsAwarded = finalCoinsAwarded;
            this.finalRatingAwarded = finalRatingAwarded;
            this.victimRatingLost = victimRatingLost;
            this.eventBonusCoins = eventBonusCoins;
            this.eventBonusRating = eventBonusRating;
        }

        public int getEventMultiplierBonusCoins() { return eventMultiplierBonusCoins; }

        public int getEventMultiplierBonusRating() { return eventMultiplierBonusRating; }

        public int getPrestigeBonusCoins() { return prestigeBonusCoins; }

        public int getPrestigeBonusRating() { return prestigeBonusRating; }

        public int getStreakBonusCoins() { return streakBonusCoins; }

        public int getBaseCoins() {
            return baseCoins;
        }

        public int getBaseKillRating() {
            return baseKillRating;
        }

        public int getStreakBonusRating() {
            return streakBonusRating;
        }

        public int getFinalCoinsAwarded() {
            return finalCoinsAwarded;
        }

        public int getFinalRatingAwarded() {
            return finalRatingAwarded;
        }

        public int getVictimRatingLost() {
            return victimRatingLost;
        }

        public int getEventBonusCoins() {
            return eventBonusCoins;
        }

        public int getEventBonusRating() {
            return eventBonusRating;
        }
    }
}
