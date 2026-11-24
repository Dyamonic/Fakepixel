package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.Period;
import org.bukkit.entity.Player;
import me.outerspace.thepit.util.PitGoldFormatter;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PitGoldService {
    private final ThePitPlugin plugin;
    private final Set<UUID> transactions = ConcurrentHashMap.newKeySet();

    public PitGoldService(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public String formatPitGold(long amount) { return PitGoldFormatter.formatPitGold(amount); }

    public int balance(UUID uuid) {
        return plugin.getStatsManager().getCoins(uuid, Period.LIFETIME);
    }

    public long getBalance(UUID uuid) {
        return balance(uuid);
    }

    public boolean has(UUID uuid, long amount) {
        return uuid != null && amount >= 0L && balance(uuid) >= amount;
    }

    public boolean take(Player player, int amount) {
        return plugin.getStatsManager().takeCoins(player, amount);
    }

    public void add(Player player, int amount) {
        plugin.getStatsManager().addCoins(player, amount);
    }

    public void set(Player player, int amount) {
        plugin.getStatsManager().setCoins(player, amount);
    }

    public void applyPersistedBalance(Player player, int amount) {
        plugin.getStatsManager().applyPersistedCoins(player, amount);
    }

    public int award(Player player, int baseAmount, ThePitPlugin.GoldSource source) {
        if (player == null || baseAmount <= 0) return 0;
        int amount = boostedAmount(player, baseAmount, source);
        if (amount <= 0) return 0;
        add(player, amount);
        recordEarned(player, amount, source);
        return amount;
    }

    public int boostedAmount(Player player, int baseAmount, ThePitPlugin.GoldSource source) {
        if (player == null || baseAmount <= 0) return 0;
        int amount = baseAmount;
        if (source != null && source.eligibleForPassive() && plugin.getPassiveManager() != null) {
            amount = (int) Math.min(Integer.MAX_VALUE, Math.floor(baseAmount
                    * (1.0D + plugin.getPassiveManager().bonus(player.getUniqueId(), "gold-boost")) + 0.0000001D));
        }
        if (plugin.getRenownShopManager() != null && (source == ThePitPlugin.GoldSource.KILL
                || source == ThePitPlugin.GoldSource.COLLECTIBLE)) {
            int tier = plugin.getRenownShopManager().tier(player.getUniqueId(), "renown-gold-boost");
            double boost = plugin.getRenownShopManager().effect("renown-gold-boost", "multiplier-per-tier", 0.025D) * tier;
            amount = (int) Math.min(Integer.MAX_VALUE, Math.floor(amount * (1.0D + Math.max(0.0D, boost)) + 0.0000001D));
        }
        if (plugin.getRenownShopManager() != null && source == ThePitPlugin.GoldSource.KILL
                && plugin.getRenownShopManager().owns(player.getUniqueId(), "celebrity")) {
            amount = (int) Math.min(Integer.MAX_VALUE, Math.floor(amount
                    * plugin.getRenownShopManager().effect("celebrity", "kill-gold-multiplier", 2.0D) + 0.0000001D));
        }
        if (plugin.getRewardMultiplierService() != null && (source == ThePitPlugin.GoldSource.KILL || source == ThePitPlugin.GoldSource.ASSIST)) {
            RewardMultiplierService.RewardSource rewardSource = source == ThePitPlugin.GoldSource.KILL
                    ? RewardMultiplierService.RewardSource.KILL_GOLD : RewardMultiplierService.RewardSource.ASSIST_GOLD;
            double multiplier = plugin.getRewardMultiplierService().getMultiplier(player, rewardSource, player.getLocation()).gold();
            amount = (int) Math.min(Integer.MAX_VALUE, Math.floor(amount * multiplier + 0.0000001D));
        }
        return Math.max(0, amount);
    }

    public void recordEarned(Player player, int amount, ThePitPlugin.GoldSource source) {
        if (player == null || amount <= 0) return;
        if (source == ThePitPlugin.GoldSource.ADMINISTRATIVE || source == ThePitPlugin.GoldSource.REFUND
                || source == ThePitPlugin.GoldSource.TRANSFER || source == ThePitPlugin.GoldSource.SHOP_SALE) return;
        plugin.getStatsManager().recordPitGold(player, amount);
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new AchievementManager.ProgressEvent(player.getUniqueId(), AchievementManager.EventType.PIT_GOLD_EARNED,
                        amount, java.util.Map.of("source", source == null ? "" : source.name()), false));
        if (source == ThePitPlugin.GoldSource.FISHING) {
            plugin.getStatsManager().addLongStat(player.getUniqueId(), StatsManager.GOLD_FROM_SELLING_FISH, amount);
        }
        if (source != ThePitPlugin.GoldSource.QUEST && plugin.getProgressManager() != null) {
            plugin.getProgressManager().recordProgress(player, ProgressManager.ObjectiveType.PIT_GOLD_EARNED,
                    source == null ? "" : source.name().toLowerCase(java.util.Locale.ENGLISH), amount,
                    new ProgressManager.ProgressContext("pit_gold", "", java.util.Map.of("source", source == null ? "" : source.name())));
        }
    }

    public boolean lock(UUID uuid) {
        return uuid != null && transactions.add(uuid);
    }

    public void unlock(UUID uuid) {
        if (uuid != null) transactions.remove(uuid);
    }

    public boolean isLocked(UUID uuid) {
        return uuid != null && transactions.contains(uuid);
    }
}
