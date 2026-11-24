package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KillXpService {
    private final ThePitPlugin plugin;
    private final Set<String> processing = ConcurrentHashMap.newKeySet();
    private final Set<String> processed = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Integer> validKillsThisLife = new ConcurrentHashMap<>();

    public KillXpService(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public KillXpTransactionResult grantKillXp(Player killer, Player victim, PitDeathContext context, int externalFlatXp) {
        if (killer == null || victim == null || context == null || context.getDeathId() == null) return invalid();
        String transactionKey = killer.getUniqueId() + ":" + context.getDeathId() + ":PLAYER_KILL_XP";
        if (processed.contains(transactionKey) || !processing.add(transactionKey)) return new KillXpTransactionResult(KillXpTransactionResult.Status.DUPLICATE, null, 0, 0);
        try {
            KillXpResult calculation = calculate(killer, context, externalFlatXp);
            int calculatedXp = calculation.getFinalXp();
            if (plugin.getPerkManager() != null && plugin.getPerkManager().has(killer, "conglomerate")) {
                double conversionRate = plugin.registry().perks().getDouble("perks.conglomerate.mechanics.xp-to-coins-percent", 20.0D) / 100.0D;
                int convertedGold = Math.max(0, (int) Math.round(calculatedXp * Math.max(0.0D, conversionRate)));
                if (convertedGold > 0) {
                    plugin.getPitGoldService().award(killer, convertedGold, ThePitPlugin.GoldSource.KILL);
                    processed.add(transactionKey);
                    validKillsThisLife.merge(killer.getUniqueId(), 1, Integer::sum);
                    return new KillXpTransactionResult(KillXpTransactionResult.Status.CONVERTED_TO_GOLD, calculation, 0, convertedGold);
                }
            }
            plugin.getStatsManager().addXp(killer, calculatedXp, StatsManager.XpSource.GAMEPLAY);
            processed.add(transactionKey);
            validKillsThisLife.merge(killer.getUniqueId(), 1, Integer::sum);
            return new KillXpTransactionResult(KillXpTransactionResult.Status.SUCCESS, calculation, calculatedXp, 0);
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Kill XP transaction failed: player=" + killer.getUniqueId() + ", deathId=" + context.getDeathId() + ", reason=" + exception.getMessage());
            return new KillXpTransactionResult(KillXpTransactionResult.Status.SAVE_FAILURE, null, 0, 0);
        } finally {
            processing.remove(transactionKey);
        }
    }

    public KillXpResult calculate(Player killer, PitDeathContext context, int externalFlatXp) {
        int baseXp = Math.max(1, plugin.registry().settings().getInt("levels.rewards.kill-xp", 5));
        int lifeKills = validKillsThisLife.getOrDefault(killer.getUniqueId(), 0);
        int firstKillsBonus = lifeKills < 3 ? 4 : 0;
        int higherLevelDifference = Math.max(0, context.getVictimLevel() - context.getKillerLevel());
        int levelDifferenceBonus = higherLevelDifference / 5;
        int streak = Math.max(0, context.getKillerKillstreak());
        int killstreakBonus = streak >= 20 ? Math.min(30, (streak / 10) * 3) : streak >= 5 ? 5 : streak >= 3 ? 3 : 0;
        if (plugin.getPerkManager() != null && plugin.getPerkManager().has(killer, "streaker")) killstreakBonus *= 3;
        int victimStreak = Math.max(0, context.getVictimKillstreak());
        int streakShutdownBonus = victimStreak >= 20 ? Math.min(configuredShutdownCap(), (victimStreak / 10) * 15) : victimStreak >= 5 ? 25 : victimStreak >= 3 ? 15 : 0;
        int passiveBonus = 0;
        if (plugin.getRenownShopManager() != null) passiveBonus += plugin.getRenownShopManager().tier(killer.getUniqueId(), "renown-xp-bump") * plugin.getRenownShopManager().effect("renown-xp-bump", "xp-per-tier", 1);
        int perkBonus = Math.max(0, externalFlatXp);
        int mysticBonus = 0;
        double lowLevelMultiplier = context.getVictimLevel() <= 20 ? 0.9D : 1.0D;
        double passiveMultiplier = plugin.getPassiveManager() == null ? 1.0D : 1.0D + Math.max(0.0D, plugin.getPassiveManager().bonus(killer.getUniqueId(), "xp-boost"));
        double eventMultiplier = 1.0D;
        if (plugin.getRewardMultiplierService() != null) eventMultiplier = Math.max(1.0D, plugin.getRewardMultiplierService().getMultiplier(killer, RewardMultiplierService.RewardSource.KILL_XP, context.getDeathLocation()).xp());
        double multiplier = Math.max(0.0D, lowLevelMultiplier * passiveMultiplier * eventMultiplier);
        double subtotal = baseXp + firstKillsBonus + levelDifferenceBonus + killstreakBonus + streakShutdownBonus + passiveBonus + perkBonus + mysticBonus;
        int maximum = Math.max(1, plugin.registry().settings().getInt("levels.rewards.maximum-kill-xp", 400));
        int finalXp = Math.max(1, Math.min(maximum, (int) Math.round(subtotal * multiplier)));
        return new KillXpResult(baseXp, firstKillsBonus, levelDifferenceBonus, killstreakBonus, streakShutdownBonus, passiveBonus, perkBonus, mysticBonus, multiplier, finalXp);
    }

    public void resetLife(UUID playerId) {
        if (playerId != null) validKillsThisLife.remove(playerId);
    }

    private int configuredShutdownCap() {
        return Math.max(0, plugin.registry().settings().getInt("levels.rewards.maximum-shutdown-xp", 300));
    }

    private KillXpTransactionResult invalid() {
        return new KillXpTransactionResult(KillXpTransactionResult.Status.INVALID, null, 0, 0);
    }
}
