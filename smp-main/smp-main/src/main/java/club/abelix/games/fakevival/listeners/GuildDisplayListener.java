package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.GuildManager;
import club.abelix.games.fakevival.managers.PlayerLevelManager;
import club.abelix.games.fakevival.managers.TablistManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import club.abelix.games.fakevival.util.ScoreboardTeamUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuildDisplayListener implements Listener {
    private final JavaPlugin plugin;
    private final GuildManager guildManager;
    private final TablistManager tablistManager;
    private final PlayerLevelManager playerLevelManager;

    public GuildDisplayListener(JavaPlugin plugin, GuildManager guildManager, TablistManager tablistManager, PlayerLevelManager playerLevelManager) {
        this.plugin = plugin;
        this.guildManager = guildManager;
        this.tablistManager = tablistManager;
        this.playerLevelManager = playerLevelManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        guildManager.touchMember(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) { guildManager.touchMember(event.getPlayer()); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String tag = CC.color(guildManager.getGuildTag(player));
        String format = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("guilds.chat.format", "%guild_tag%%default_format%");
        boolean injectAfterPlayer = format.contains("%guild_tag%") && format.contains("%default_format%");
        format = replacePlayerPlaceholders(format, player, injectAfterPlayer ? "" : tag);

        if (format.contains("%default_format%")) {
            String defaultFormat = injectAfterPlayer ? injectGuildTag(event.getFormat(), tag) : event.getFormat();
            event.setFormat(CC.color(format
                    .replace("%default_format%", defaultFormat)).replace("  ", " "));
            return;
        }

        event.setFormat(CC.color(format
                .replace("%player%", escapeChatFormat(player.getName()))
                .replace("%displayname%", escapeChatFormat(player.getDisplayName()))
                .replace("%message%", escapeChatFormat(event.getMessage()))));
    }

    private String replacePlayerPlaceholders(String format, Player player, String tag) {
        int level = playerLevelManager == null ? 1 : playerLevelManager.getLevel(player);
        String display = playerLevelManager == null ? "" : playerLevelManager.getDisplay(player);
        String rawTag = guildManager.getRawGuildTag(player);
        String colorId = guildManager.getTagColorId(player);
        String formatted = guildManager.getGuildTag(player);
        String guildId = guildManager.getGuildId(player);
        return format
                .replace("%guild_tag%", tag)
                .replace("%smp_guild_tag_formatted%", formatted)
                .replace("%fakevival_guild_tag_formatted%", formatted)
                .replace("%smp_guild_tag_color%", colorId)
                .replace("%fakevival_guild_tag_color%", colorId)
                .replace("%smp_guild_tag%", rawTag)
                .replace("%fakevival_guild_tag%", rawTag)
                .replace("%smp_guild_member_count%", guildId == null ? "0" : String.valueOf(guildManager.getMemberCount(guildId)))
                .replace("%smp_guild_online_count%", guildId == null ? "0" : String.valueOf(guildManager.getOnlineCount(guildId)))
                .replace("%smp_guild_role%", guildId == null ? "" : guildManager.getMemberRole(player))
                .replace("%smp_guild_role_display%", guildId == null ? "" : guildManager.getMemberRole(player).replace('_', ' '))
                .replace("%smp_guild_war_count%", guildId == null ? "0" : String.valueOf(guildManager.getActiveWarCount(guildId)))
                .replace("%smp_guild_active_wars%", guildId == null ? "0" : String.valueOf(guildManager.getActiveWarCount(guildId)))
                .replace("%smp_guild_pending_wars%", guildId == null ? "0" : String.valueOf(guildManager.getPendingWarCount(guildId)))
                .replace("%smp_guild_ally_count%", guildId == null ? "0" : String.valueOf(guildManager.getActiveAllyCount(guildId)))
                .replace("%smp_guild_active_allies%", guildId == null ? "0" : String.valueOf(guildManager.getActiveAllyCount(guildId)))
                .replace("%smp_guild_pending_allies%", guildId == null ? "0" : String.valueOf(guildManager.getPendingAllyCount(guildId)))
                .replace("%smp_player_level_display%", display)
                .replace("%fakevival_player_level_display%", display)
                .replace("%player_level_display%", display)
                .replace("%smp_player_level_icon%", playerLevelManager == null ? "✦" : playerLevelManager.getIcon(level))
                .replace("%smp_player_level_color%", playerLevelManager == null ? "&7" : playerLevelManager.getColor(level))
                .replace("%smp_player_level_xp_needed%", String.valueOf(playerLevelManager == null ? 1000L : playerLevelManager.getXpNeeded(player)))
                .replace("%smp_player_level_xp%", String.valueOf(playerLevelManager == null ? 0L : playerLevelManager.getXp(player)))
                .replace("%smp_player_level_progress%", playerLevelManager == null ? "0" : String.format(java.util.Locale.US, "%.1f", playerLevelManager.getProgress(player)))
                .replace("%fakevival_player_level%", String.valueOf(level))
                .replace("%smp_player_level%", String.valueOf(level))
                .replace("%player_level%", String.valueOf(level));
    }

    private String injectGuildTag(String format, String tag) {
        if (tag == null || ChatColor.stripColor(tag).isBlank()) return format;
        int separator = format.indexOf(':');
        if (separator >= 0) return format.substring(0, separator).stripTrailing() + " " + tag + "&f" + format.substring(separator);
        int message = format.indexOf("%2$s");
        if (message >= 0) return format.substring(0, message).stripTrailing() + " " + tag + "&f " + format.substring(message);
        return format.stripTrailing() + " " + tag;
    }

    private String escapeChatFormat(String text) {
        return text == null ? "" : text.replace("%", "%%");
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) refresh(player);
    }

    public void refresh(Player player) {
        if (tablistManager != null) tablistManager.apply(player);
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = "guild_" + player.getUniqueId().toString().replace("-", "").substring(0, 10);
        Team team = ScoreboardTeamUtil.getOrCreate(scoreboard, teamName);
        if (team == null) return;
        String tag = CC.color(guildManager.getGuildTag(player));
        String suffix = ChatColor.stripColor(tag).isBlank() ? "" : " " + tag;
        if (suffix.length() > 64) suffix = suffix.substring(0, 64);
        team.setPrefix("");
        team.setSuffix(suffix);
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
    }
}
