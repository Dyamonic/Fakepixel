package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import me.outerspace.thepit.util.CC;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.Locale;

public final class PitPlayerNameFormatter {
    private final ThePitPlugin plugin;

    public PitPlayerNameFormatter(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public ResolvedName resolve(Player player, NetworkProfileSnapshot snapshot) {
        NetworkProfileSnapshot resolved = snapshot == null ? plugin.getNetworkProfileCache().get(player) : snapshot;
        String tabLevelRaw = plugin.getStatsManager().isProfileLoaded(player.getUniqueId()) ? rawColors(plugin.getStatsManager().getFormattedLevel(player.getUniqueId(), StatsManager.LevelRenderContext.TAB_LIST)) : "";
        String nametagLevelRaw = plugin.getStatsManager().isProfileLoaded(player.getUniqueId()) ? rawColors(plugin.getStatsManager().getFormattedLevel(player.getUniqueId(), StatsManager.LevelRenderContext.NAMETAG)) : "";
        String chatLevelRaw = plugin.getStatsManager().isProfileLoaded(player.getUniqueId()) ? rawColors(plugin.getStatsManager().getFormattedLevel(player.getUniqueId(), StatsManager.LevelRenderContext.CHAT)) : "";
        String rankRaw = rawColors(resolved.getPrefix());
        String rankColorRaw = rawColors(resolved.getRankColor());
        String name = player.getName();
        String supporterStarRaw = plugin.getPitCommandManager() == null ? "" : rawColors(plugin.getPitCommandManager().supporterStar(player, false).trim());
        int bountyValue = plugin.getStatsManager().getBounty(player.getUniqueId());
        String bountyRaw = bountyValue <= 0 ? "" : "&6" + NumberFormat.getIntegerInstance(Locale.US).format(bountyValue) + "g";
        String template = plugin.registry().tablist().template(me.outerspace.thepit.registry.TablistRegistry.TAB_PLAYER_NAME);
        String tabDisplay = resolveTemplate(template, tabLevelRaw, rankRaw, rankColorRaw, name, supporterStarRaw, bountyRaw);
        String nametagTemplate = plugin.registry().tablist().template(me.outerspace.thepit.registry.TablistRegistry.NAMETAG);
        String nametagDisplay = resolveTemplate(nametagTemplate, nametagLevelRaw, rankRaw, rankColorRaw, name, supporterStarRaw, bountyRaw);
        String chatTemplate = plugin.registry().tablist().template(me.outerspace.thepit.registry.TablistRegistry.CHAT_PLAYER_NAME);
        String chatDisplay = resolveTemplate(chatTemplate, chatLevelRaw, rankRaw, rankColorRaw, name, supporterStarRaw, "");
        String nametagLevel = CC.color(nametagLevelRaw);
        String rank = CC.color(rankRaw);
        String rankColor = CC.color(rankColorRaw);
        String supporterStar = CC.color(supporterStarRaw);
        String bounty = CC.color(bountyRaw);
        String usernameColor = canonicalNameColor(rankColor);
        String inheritedColor = ChatColor.getLastColors(nametagLevel);
        String teamPrefix = nametagLevel + " " + (usernameColor.equals(inheritedColor) ? "" : usernameColor);
        String teamSuffix = joinWithSpaces(supporterStar, bounty);
        if (!teamSuffix.isEmpty()) teamSuffix = " " + teamSuffix;
        boolean teamFieldsSupported = validTeamField(teamPrefix) && validTeamField(teamSuffix);
        return new ResolvedName(nametagLevel, rank, name, usernameColor, supporterStar, bounty, tabDisplay, nametagDisplay, chatDisplay,
                teamPrefix, teamSuffix, teamFieldsSupported);
    }

    public String decoratedName(Player player, NetworkProfileSnapshot snapshot, boolean tab) {
        ResolvedName value = resolve(player, snapshot);
        return CC.color(value.abelixPrefix + value.playerName + (value.supporterStar.isEmpty() ? "" : " " + value.supporterStar));
    }

    public String combatName(Player player) {
        NetworkProfileSnapshot snapshot = plugin.getNetworkProfileCache() == null ? null : plugin.getNetworkProfileCache().cached(player.getUniqueId());
        ResolvedName value = resolve(player, snapshot);
        String inherited = ChatColor.getLastColors(value.abelixPrefix);
        String color = inherited.equals(value.usernameColor) ? "" : value.usernameColor;
        return CC.color(value.abelixPrefix + color + value.playerName);
    }

    private String resolveTemplate(String template, String level, String rank, String rankColor, String name, String supporterStar, String bounty) {
        String cleanRank = rank == null ? "" : rank.trim();
        String prefix = cleanRank.isEmpty() ? rankColor : cleanRank + " " + rankColor;
        String output = template.replace("%level_formatted%", level)
                .replace("%abelix_prefix%", prefix)
                .replace("%abelix_rankcolor%", rankColor)
                .replace("%abelix_rank_color%", rankColor)
                .replace("%abelix_rankcolor_player%", rawColors(canonicalNameColor(CC.color(rankColor))) + name)
                .replace("%player%", name)
                .replace("%supporter_star%", supporterStar)
                .replace("%bounty_formatted%", bounty);
        return CC.color(output.replaceAll(" {2,}", " ").trim());
    }

    private String rawColors(String value) {
        return value == null ? "" : value.replace(ChatColor.COLOR_CHAR, '&');
    }

    private String canonicalNameColor(String value) {
        String colors = ChatColor.getLastColors(value == null ? "" : value);
        if (colors == null || colors.isEmpty()) return ChatColor.WHITE.toString();
        return colors;
    }

    private String joinWithSpaces(String... values) {
        StringBuilder output = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (output.length() > 0) output.append(' ');
            output.append(value.trim());
        }
        return output.toString();
    }

    private boolean validTeamField(String value) {
        return value.length() <= 16 && (value.isEmpty() || value.charAt(value.length() - 1) != ChatColor.COLOR_CHAR);
    }

    public record ResolvedName(String levelFormatted, String abelixPrefix, String playerName, String usernameColor, String supporterStar,
                               String bountyFormatted, String tabListDisplay, String nametagDisplay, String chatDisplay,
                               String teamPrefix, String teamSuffix, boolean teamFieldsSupported) {}
}
