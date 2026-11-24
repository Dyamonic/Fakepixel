package me.outerspace.thepit.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.model.StatType;
import me.outerspace.thepit.util.CC;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class ThePitExpansion extends PlaceholderExpansion {
    private final ThePitPlugin plugin;
    private final String identifier;
    private final DecimalFormat kd = new DecimalFormat("0.00");

    public ThePitExpansion(ThePitPlugin plugin, String identifier) { this.plugin = plugin; this.identifier = identifier; }

    public String getIdentifier() { return identifier; }
    public String getAuthor() { return "outerspacepugyt"; }
    public String getVersion() { return plugin.getDescription().getVersion(); }
    public boolean persist() { return true; }

    public String onPlaceholderRequest(Player p, String id) {
        if (id == null) return "";
        String lower = id.toLowerCase();
        if (lower.equals("map")) return plugin.getArenaManager().getCurrentArena() == null ? "None" : plugin.getArenaManager().getDisplayName(plugin.getArenaManager().getCurrentArena());
        if (lower.equals("map_id")) return plugin.getArenaManager().getCurrentArena() == null ? "none" : plugin.getArenaManager().getCurrentArena();
        if (lower.equals("map_rotation_remaining")) return plugin.getMapRotationManager().remainingFormatted();
        if (lower.equals("map_rotation_days")) return String.valueOf(plugin.getMapRotationManager().remainingDays());
        if (lower.equals("map_rotation_hours")) return String.valueOf(plugin.getMapRotationManager().remainingHours());
        if (p != null) {
            if (lower.startsWith("stats_") || lower.startsWith("passive_")) {
                String token = "%thepit_" + lower + "%";
                String resolved = plugin.applyPlaceholders(p, token);
                return resolved.equals(token) ? null : resolved;
            }
            if (lower.equals("kills")) return String.valueOf(plugin.getStatsManager().getKills(p.getUniqueId(), Period.LIFETIME));
            if (lower.equals("deaths")) return String.valueOf(plugin.getStatsManager().getDeaths(p.getUniqueId(), Period.LIFETIME));
            if (lower.equals("kd") || lower.equals("kdr")) return kd.format(plugin.getStatsManager().getKd(p.getUniqueId(), Period.LIFETIME));
            if (lower.equals("abelix_prefix") || lower.equals("fakepixel_prefix")) return plugin.getNetworkPrefix(p);
            if (lower.equals("permission_prefix") || lower.equals("group_prefix")) return plugin.getPermissionPrefix(p);
            if (lower.equals("abelix_gold") || lower.equals("fakepixel_gold")) return plugin.getBuildScoreboardManager().formattedNumber(plugin.getNetworkGold(p));
            if (lower.equals("gold") || lower.equals("coins") || lower.equals("pit_gold")) return plugin.getBuildScoreboardManager().formattedNumber(plugin.getPitGoldService().balance(p.getUniqueId()));
            if (lower.equals("achievement_points")) return String.valueOf(plugin.getAchievementManager().points(p.getUniqueId()));
            if (lower.equals("achievements_completed")) return String.valueOf(plugin.getAchievementManager().completedCount(p.getUniqueId()));
            if (lower.equals("achievements_total")) return String.valueOf(plugin.getAchievementManager().totalCount());
            if (lower.equals("achievement_completion_percent")) return plugin.getAchievementManager().completionPercent(p.getUniqueId());
            if (lower.equals("date")) return plugin.getBuildScoreboardManager().formattedDate();
            if (lower.equals("level")) return String.valueOf(plugin.getStatsManager().getLevel(p.getUniqueId()));
            if (lower.equals("level_formatted")) return plugin.getStatsManager().getFormattedLevel(p.getUniqueId());
            if (lower.equals("level_color")) return plugin.getStatsManager().getLevelColor(p.getUniqueId());
            if (lower.equals("level_xp")) return String.valueOf(plugin.getStatsManager().getLevelXp(p.getUniqueId()));
            if (lower.equals("level_required_xp")) return String.valueOf(plugin.getStatsManager().getRequiredXp(p.getUniqueId()));
            if (lower.equals("level_progress")) return plugin.getStatsManager().getLevelProgress(p.getUniqueId());
            if (lower.equals("needed_xp")) return plugin.getBuildScoreboardManager().neededXp(p);
            if (lower.equals("lifetime_xp")) return String.valueOf(plugin.getStatsManager().getLifetimeXp(p.getUniqueId()));
            if (lower.equals("prestige")) return String.valueOf(plugin.getStatsManager().getPrestige(p.getUniqueId()));
            if (lower.equals("prestige_icon")) return plugin.getStatsManager().getPrestigeIcon(p.getUniqueId());
            if (lower.equals("prestige_roman")) return plugin.getStatsManager().getPrestigeRoman(p.getUniqueId());
            if (lower.equals("prestige_formatted")) return plugin.getStatsManager().getPrestigeFormatted(p.getUniqueId());
            if (lower.equals("next_prestige")) return String.valueOf(Math.min(plugin.getStatsManager().getMaxPrestige(), plugin.getStatsManager().getPrestige(p.getUniqueId()) + 1));
            if (lower.equals("next_prestige_roman")) return plugin.getStatsManager().getNextPrestigeRoman(p.getUniqueId());
            if (lower.equals("prestige_multiplier")) return String.valueOf(plugin.getStatsManager().getMultiplier(p.getUniqueId()));
            if (lower.equals("prestige_coins_required")) return java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(plugin.getStatsManager().getPrestigeCoinsRequired(p.getUniqueId()));
            if (lower.equals("prestige_coins_remaining")) return java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(plugin.getStatsManager().getPrestigeCoinsRemaining(p.getUniqueId()));
            if (lower.equals("prestige_coin_progress")) return plugin.getStatsManager().getPrestigeCoinProgress(p.getUniqueId());
            if (lower.equals("prestige_cooldown")) return plugin.getStatsManager().getPrestigeCooldownFormatted(p);
            if (lower.equals("prestige_bracket_color")) return plugin.getStatsManager().getPrestigeBracketColor(p.getUniqueId());
            if (lower.equals("renown")) return String.valueOf(plugin.getStatsManager().getRenown(p.getUniqueId()));
            if (lower.equals("renown_formatted")) return java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(plugin.getStatsManager().getRenown(p.getUniqueId()));
            if (lower.equals("prestige_line")) return CC.color(plugin.getBuildScoreboardManager().prestigeLine(p));
            if (lower.equals("prestige_level")) return plugin.getStatsManager().getFormattedLevel(p.getUniqueId());
            if (lower.equals("status")) return CC.color(plugin.getBuildScoreboardManager().status(p));
            if (lower.equals("bounty_line")) return CC.color(plugin.getBuildScoreboardManager().bountyLine(p));
            if (lower.equals("bounty")) return plugin.getBuildScoreboardManager().formattedNumber(plugin.getStatsManager().getBounty(p.getUniqueId()));
            if (lower.equals("streak_line")) return CC.color(plugin.getBuildScoreboardManager().streakLine(p));
            if (lower.equals("effect_lines")) return CC.color(plugin.getBuildScoreboardManager().effectSummary(p));
            if (lower.equals("fishing_catches_today")) return String.valueOf(plugin.getFishingManager().catchesToday(p));
            if (lower.equals("fishing_daily_limit")) return String.valueOf(plugin.getFishingManager().limit());
            if (lower.equals("fishing_remaining")) return String.valueOf(plugin.getFishingManager().remaining(p));
            if (lower.equals("fishing_club_tier")) return String.valueOf(plugin.getFishingManager().clubTier(p));
            if (lower.equals("multiplier")) return String.valueOf(plugin.getStatsManager().getMultiplier(p.getUniqueId()));
            if (lower.equals("killstreak") || lower.equals("streak")) return String.valueOf(plugin.getStatsManager().getKillStreak(p.getUniqueId(), Period.LIFETIME));
            if (lower.equals("perk_slots")) return String.valueOf(plugin.getPerkManager().slotCount(p.getUniqueId()));
            if (lower.equals("perks_unlocked")) return String.valueOf(plugin.getPerkManager().unlockedCount(p.getUniqueId()));
            if (lower.startsWith("perk_")) {
                try {
                    int slot = Integer.parseInt(lower.substring(5));
                    if (slot >= 1 && slot <= 4) return plugin.getPerkManager().equippedName(p.getUniqueId(), slot);
                } catch (Exception ignored) { }
            }
        }
        if (lower.startsWith("top_")) {
            String[] split = lower.split("_");
            if (split.length >= 3) {
                StatType type = StatType.KILLS;
                Period period = Period.LIFETIME;
                int placeIndex = 2;
                if (split.length >= 4) {
                    Period parsed = Period.from(split[2]);
                    if (parsed != null) { period = parsed; placeIndex = 3; }
                }
                try {
                    int pos = Integer.parseInt(split[placeIndex]);
                    if (pos < 1 || pos > 10) return "";
                    return plugin.getStatsManager().topLine(type, period, pos);
                } catch (Exception ignored) { }
            }
        }
        return null;
    }
}
