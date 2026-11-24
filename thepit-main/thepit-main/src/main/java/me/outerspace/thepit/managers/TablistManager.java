package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.NetworkProfileSnapshot;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.NameTagVisibility;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;

public class TablistManager {
    private final ThePitPlugin plugin;
    private final Map<UUID, TabState> tabStates = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> tabEntryNames = new HashMap<>();
    private final Map<UUID, Map<UUID, TeamAssignment>> nametagTeams = new HashMap<>();
    private PacketAccess packetAccess;
    private PlayerInfoAccess playerInfoAccess;
    private boolean packetAccessFailed;
    private BukkitTask task;
    private final Map<UUID, String> lastRefreshReasons = new HashMap<>();
    private long formattingGeneration;
    private final Set<UUID> pendingBountyRefreshes = new HashSet<>();
    private boolean bountyRefreshQueued;
    private final Set<String> unsupportedNametagFields = new HashSet<>();
    private final Map<UUID, String> lastTransports = new HashMap<>();
    private final Map<UUID, String> lastFallbacks = new HashMap<>();

    public TablistManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.registry().tablist().enabled()) return;
        long ticks = Math.max(10L, plugin.registry().tablist().updateTicks());
        task = Bukkit.getScheduler().runTaskTimer(plugin, new TablistUpdateTask(), 20L, ticks);
        for (Player player : Bukkit.getOnlinePlayers()) markRefresh(player, "TABLIST_STARTED_OR_RELOADED");
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        restoreVanillaNametags();
        tabStates.clear();
        tabEntryNames.clear();
        nametagTeams.clear();
        pendingBountyRefreshes.clear();
        unsupportedNametagFields.clear();
        lastTransports.clear();
        lastFallbacks.clear();
        bountyRefreshQueued = false;
    }

    public void update(Player player) {
        if (!plugin.registry().tablist().enabled() || player == null) return;
        update(player, plugin.getNetworkProfileCache().get(player));
    }

    public void updateNametags() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        updateNametags(players, plugin.getNetworkProfileCache().snapshots(players));
    }

    public void synchronizeViewerScoreboard(Player viewer, String reason) {
        if (viewer == null || !viewer.isOnline() || !plugin.registry().tablist().nametagsEnabled()) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> synchronizeViewerScoreboard(viewer, reason));
            return;
        }
        lastRefreshReasons.put(viewer.getUniqueId(), reason == null ? "VIEWER_SCOREBOARD_CHANGED" : reason);
        Map<UUID, TeamAssignment> previous = nametagTeams.remove(viewer.getUniqueId());
        Scoreboard board = viewer.getScoreboard();
        if (board == null) return;
        List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers());
        Map<UUID, NetworkProfileSnapshot> snapshots = plugin.getNetworkProfileCache().snapshots(targets);
        Map<UUID, TeamAssignment> assignments = nametagTeams.computeIfAbsent(viewer.getUniqueId(), ignored -> new HashMap<>());
        for (Player target : targets) applyNametag(viewer, board, assignments, target, renderNametag(target, snapshots.get(target.getUniqueId())));
        if (plugin.registry().tablist().debugEnabled()) plugin.getLogger().info("Nametag viewer scoreboard synchronized viewer="
                + viewer.getName() + " previous_assignments=" + (previous == null ? 0 : previous.size())
                + " current_assignments=" + assignments.size() + " reason=" + lastRefreshReasons.get(viewer.getUniqueId()));
    }

    private void runCycle() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Map<UUID, NetworkProfileSnapshot> snapshots = plugin.getNetworkProfileCache().snapshots(players);
        Set<UUID> online = new HashSet<>();
        for (Player player : players) {
            online.add(player.getUniqueId());
            update(player, snapshots.get(player.getUniqueId()));
        }
        tabStates.keySet().retainAll(online);
        tabEntryNames.keySet().retainAll(online);
        for (Map<UUID, String> entries : tabEntryNames.values()) entries.keySet().retainAll(online);
        nametagTeams.keySet().retainAll(online);
        updateNametags(players, snapshots);
    }

    private void update(Player player, NetworkProfileSnapshot snapshot) {
        RenderContext context = new RenderContext(player, snapshot);
        String header = join(plugin.registry().tablist().header(), context);
        String footer = join(plugin.registry().tablist().footer(), context);
        PitPlayerNameFormatter.ResolvedName resolvedName = plugin.getPlayerNameFormatter().resolve(player, snapshot);
        String name = resolvedName.tabListDisplay();
        TabState previous = tabStates.get(player.getUniqueId());
        String oldHeader = previous == null ? null : previous.header;
        String oldFooter = previous == null ? null : previous.footer;
        if (!header.equals(oldHeader) || !footer.equals(oldFooter)) sendHeaderFooter(player, header, footer);
        updatePlayerInfoEntry(player, name);
        tabStates.put(player.getUniqueId(), new TabState(header, footer, name));
        debugResolved(player, resolvedName);
    }

    private void updateNametags(List<Player> players, Map<UUID, NetworkProfileSnapshot> snapshots) {
        if (!plugin.registry().tablist().nametagsEnabled()) return;
        Map<UUID, RenderedNametag> rendered = new HashMap<>();
        for (Player target : players) {
            NetworkProfileSnapshot snapshot = snapshots.get(target.getUniqueId());
            rendered.put(target.getUniqueId(), renderNametag(target, snapshot));
        }
        Set<UUID> targets = rendered.keySet();
        for (Player viewer : players) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null) continue;
            Map<UUID, TeamAssignment> viewerTeams = nametagTeams.computeIfAbsent(viewer.getUniqueId(), key -> new HashMap<>());
            for (UUID stale : new HashSet<>(viewerTeams.keySet())) {
                if (targets.contains(stale)) continue;
                TeamAssignment old = viewerTeams.remove(stale);
                Team oldTeam = board.getTeam(old.teamName);
                if (oldTeam != null && oldTeam.hasEntry(old.entry)) oldTeam.removeEntry(old.entry);
            }
            for (Player target : players) applyNametag(viewer, board, viewerTeams, target, rendered.get(target.getUniqueId()));
        }
    }

    private void updateNametagTargets(Collection<Player> targets) {
        if (!plugin.registry().tablist().nametagsEnabled() || targets == null || targets.isEmpty()) return;
        Map<UUID, RenderedNametag> rendered = new HashMap<>();
        for (Player target : targets) rendered.put(target.getUniqueId(), renderNametag(target, plugin.getNetworkProfileCache().get(target)));
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null) continue;
            Map<UUID, TeamAssignment> viewerTeams = nametagTeams.computeIfAbsent(viewer.getUniqueId(), key -> new HashMap<>());
            for (Player target : targets) applyNametag(viewer, board, viewerTeams, target, rendered.get(target.getUniqueId()));
        }
    }

    private RenderedNametag renderNametag(Player target, NetworkProfileSnapshot snapshot) {
        int priority = getRankPriority(snapshot);
        String teamName = safeTeamName(String.format(Locale.US, "pit%02d%s", Math.clamp(priority, 0, 99), target.getUniqueId().toString().replace("-", "").substring(0, 9)));
        PitPlayerNameFormatter.ResolvedName resolved = plugin.getPlayerNameFormatter().resolve(target, snapshot);
        int level = plugin.getStatsManager().getLevel(target.getUniqueId());
        int prestige = plugin.getStatsManager().getPrestige(target.getUniqueId());
        return new RenderedNametag(teamName, resolved.teamPrefix(), resolved.teamSuffix(), resolved.teamFieldsSupported(), level, prestige);
    }

    private void applyNametag(Player viewer, Scoreboard board, Map<UUID, TeamAssignment> viewerTeams, Player target, RenderedNametag rendered) {
        UUID targetId = target.getUniqueId();
        TeamAssignment old = viewerTeams.get(targetId);
        if (!rendered.supported) {
            String previousPrefix = "";
            if (old != null) {
                Team oldTeam = board.getTeam(old.teamName);
                if (oldTeam != null) {
                    previousPrefix = oldTeam.getPrefix();
                    if (oldTeam.hasEntry(old.entry)) oldTeam.removeEntry(old.entry);
                }
                viewerTeams.remove(targetId);
            }
            String key = viewer.getUniqueId() + "|" + targetId + "|" + rendered.prefix + "|" + rendered.suffix;
            if (plugin.registry().tablist().debugEnabled() && unsupportedNametagFields.add(key)) {
                plugin.getLogger().warning("Nametag validation rejected target=" + target.getName()
                        + " viewer=" + viewer.getName() + " formatter_level=" + rendered.level + " prestige=" + rendered.prestige
                        + " prefix='" + visibleColors(rendered.prefix) + "' prefix_length=" + rendered.prefix.length()
                        + " previous_prefix='" + visibleColors(previousPrefix) + "' previous_prefix_length=" + previousPrefix.length()
                        + " entry='" + target.getName() + "' entry_length=" + target.getName().length()
                        + " suffix='" + visibleColors(rendered.suffix) + "' suffix_length=" + rendered.suffix.length()
                        + " validation=false rejection=legacy_team_field_limit update_packet_sent=" + (old != null)
                        + " last_sent_cache_changed=" + (old != null) + " fallback=vanilla_username"
                        + " reason=" + lastRefreshReasons.getOrDefault(targetId, "PERIODIC"));
            }
            return;
        }
        Team team = board.getTeam(rendered.teamName);
        if (team == null) team = board.registerNewTeam(rendered.teamName);
        boolean memberBefore = team.hasEntry(target.getName());
        String prefixBefore = team.getPrefix();
        String suffixBefore = team.getSuffix();
        try {
            if (!prefixBefore.equals(rendered.prefix)) team.setPrefix(rendered.prefix);
            if (!suffixBefore.equals(rendered.suffix)) team.setSuffix(rendered.suffix);
            if (team.getNameTagVisibility() != NameTagVisibility.ALWAYS) team.setNameTagVisibility(NameTagVisibility.ALWAYS);
            if (!memberBefore) team.addEntry(target.getName());
        } catch (Throwable failure) {
            try {
                if (!team.getPrefix().equals(prefixBefore)) team.setPrefix(prefixBefore);
                if (!team.getSuffix().equals(suffixBefore)) team.setSuffix(suffixBefore);
                if (!memberBefore && team.hasEntry(target.getName())) team.removeEntry(target.getName());
            } catch (Throwable ignored) { }
            if (plugin.registry().tablist().debugEnabled()) plugin.getLogger().warning("Nametag update failed target="
                    + target.getName() + " viewer=" + viewer.getName() + " formatter_level=" + rendered.level + " prestige=" + rendered.prestige
                    + " prefix='" + visibleColors(rendered.prefix) + "' prefix_length=" + rendered.prefix.length()
                    + " previous_prefix='" + visibleColors(prefixBefore) + "' previous_prefix_length=" + prefixBefore.length()
                    + " validation=true exception=" + failure.getClass().getSimpleName() + ": " + failure.getMessage()
                    + " retained_team=" + (old == null ? "none" : old.teamName)
                    + " update_packet_sent=false last_sent_cache_changed=false"
                    + " reason=" + lastRefreshReasons.getOrDefault(targetId, "PERIODIC"));
            return;
        }
        if (old != null && (!old.teamName.equals(rendered.teamName) || !old.entry.equals(target.getName()))) {
            Team oldTeam = board.getTeam(old.teamName);
            if (oldTeam != null && oldTeam.hasEntry(old.entry)) oldTeam.removeEntry(old.entry);
        }
        boolean cachedChanged = old == null || old.level != rendered.level || old.prestige != rendered.prestige
                || !old.prefix.equals(rendered.prefix) || !old.suffix.equals(rendered.suffix)
                || !old.teamName.equals(rendered.teamName) || !old.entry.equals(target.getName());
        viewerTeams.put(targetId, new TeamAssignment(rendered.teamName, target.getName(), rendered.prefix, rendered.suffix, rendered.level, rendered.prestige));
        if (plugin.registry().tablist().debugEnabled()) plugin.getLogger().info("Nametag applied target=" + target.getName()
                + " viewer=" + viewer.getName() + " formatter_level=" + rendered.level + " prestige=" + rendered.prestige
                + " prefix='" + visibleColors(rendered.prefix) + "' prefix_length=" + rendered.prefix.length()
                + " previous_prefix='" + visibleColors(prefixBefore) + "' previous_prefix_length=" + prefixBefore.length()
                + " entry='" + target.getName() + "' entry_length=" + target.getName().length()
                + " suffix='" + visibleColors(rendered.suffix) + "' suffix_length=" + rendered.suffix.length()
                + " member_before=" + memberBefore + " member_after=" + team.hasEntry(target.getName())
                + " validation=true update_packet_sent=" + (!prefixBefore.equals(rendered.prefix) || !suffixBefore.equals(rendered.suffix) || !memberBefore)
                + " last_sent_cache_changed=" + cachedChanged
                + " reason=" + lastRefreshReasons.getOrDefault(targetId, "PERIODIC"));
    }

    private void restoreVanillaNametags() {
        for (Map.Entry<UUID, Map<UUID, TeamAssignment>> viewerEntry : nametagTeams.entrySet()) {
            Player viewer = Bukkit.getPlayer(viewerEntry.getKey());
            if (viewer == null || viewer.getScoreboard() == null) continue;
            Scoreboard board = viewer.getScoreboard();
            for (TeamAssignment assignment : viewerEntry.getValue().values()) {
                Team team = board.getTeam(assignment.teamName);
                if (team != null && team.hasEntry(assignment.entry)) team.setNameTagVisibility(NameTagVisibility.ALWAYS);
            }
        }
    }

    private String join(List<String> lines, RenderContext context) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) output.append('\n');
            output.append(apply(lines.get(i), context));
        }
        return output.toString();
    }

    private String apply(String input, RenderContext context) {
        String out = input == null ? "" : input;
        out = context.replace(out, "%player%", context.player::getName);
        out = context.replace(out, "%formatted_player_name%", () -> plugin.getPitCommandManager() == null
                ? context.player.getName() : plugin.getPitCommandManager().decoratedName(context.player, true));
        out = context.replace(out, "%supporter_star%", () -> plugin.getPitCommandManager() == null
                ? "" : plugin.getPitCommandManager().supporterStar(context.player, true));
        out = context.replace(out, "%luckperms_prefix%", context.snapshot::getPrefix);
        out = context.replace(out, "%abelix_prefix%", context.snapshot::getPrefix);
        out = context.replace(out, "%abelix_rankcolor%", context.snapshot::getRankColor);
        out = context.replace(out, "%abelix_rank_color%", context.snapshot::getRankColor);
        out = context.replace(out, "%abelix_rankcolor_player%", () -> context.snapshot.getRankColor() + context.player.getName());
        out = context.replace(out, "%fakepixel_prefix%", context.snapshot::getPrefix);
        out = context.replace(out, "%permission_prefix%", context.snapshot::getPermissionPrefix);
        out = context.replace(out, "%group_prefix%", context.snapshot::getPermissionPrefix);
        out = context.replace(out, "%map%", () -> plugin.getArenaManager().getDisplayName(plugin.getArenaManager().getCurrentArena()));
        out = context.replace(out, "%kills%", () -> String.valueOf(plugin.getStatsManager().getKills(context.uuid, Period.LIFETIME)));
        out = context.replace(out, "%deaths%", () -> String.valueOf(plugin.getStatsManager().getDeaths(context.uuid, Period.LIFETIME)));
        out = context.replace(out, "%kd%", () -> String.format(Locale.US, "%.2f", plugin.getStatsManager().getKd(context.uuid, Period.LIFETIME)));
        out = context.replace(out, "%killstreak%", () -> String.valueOf(plugin.getStatsManager().getKillStreak(context.uuid, Period.LIFETIME)));
        out = context.replace(out, "%level%", () -> String.valueOf(plugin.getStatsManager().getLevel(context.uuid)));
        out = context.replace(out, "%level_formatted%", () -> plugin.getStatsManager().getFormattedLevel(context.uuid, StatsManager.LevelRenderContext.TAB_LIST));
        out = context.replace(out, "%level_progress%", () -> plugin.getStatsManager().getLevelProgress(context.uuid));
        out = context.replace(out, "%coins%", () -> plugin.getBuildScoreboardManager().formattedNumber(plugin.getPitGoldService().balance(context.uuid)));
        out = context.replace(out, "%thepit_coins%", () -> plugin.getBuildScoreboardManager().formattedNumber(plugin.getPitGoldService().balance(context.uuid)));
        out = context.replace(out, "%pit_gold%", () -> plugin.getBuildScoreboardManager().formattedNumber(plugin.getPitGoldService().balance(context.uuid)));
        out = context.replace(out, "%thepit_pit_gold%", () -> plugin.getBuildScoreboardManager().formattedNumber(plugin.getPitGoldService().balance(context.uuid)));
        out = context.replace(out, "%abelix_gold%", () -> String.valueOf(context.snapshot.getGold()));
        out = context.replace(out, "%fakepixel_gold%", () -> String.valueOf(context.snapshot.getGold()));
        out = context.replace(out, "%thepit_abelix_gold%", () -> String.valueOf(context.snapshot.getGold()));
        out = context.replace(out, "%thepit_fakepixel_gold%", () -> String.valueOf(context.snapshot.getGold()));
        out = context.replace(out, "%prestige%", () -> String.valueOf(plugin.getStatsManager().getPrestige(context.uuid)));
        out = context.replace(out, "%prestige_icon%", () -> plugin.getStatsManager().getPrestigeIcon(context.uuid));
        out = context.replace(out, "%multiplier%", () -> String.valueOf(plugin.getStatsManager().getMultiplier(context.uuid)));
        return CC.color(plugin.applyPlaceholders(context.player, out));
    }

    private int getRankPriority(NetworkProfileSnapshot snapshot) {
        if (plugin.registry().abelixRanks().getBoolean("abelix.use-rank-priority", true) && snapshot.getPriority() >= 0) {
            return Math.max(0, 99 - Math.min(99, snapshot.getPriority()));
        }
        String group = snapshot.getGroup().toLowerCase(Locale.ENGLISH);
        List<String> order = plugin.registry().tablist().rankPriority();
        for (int i = 0; i < order.size(); i++) {
            String entry = order.get(i);
            if (entry != null && entry.toLowerCase(Locale.ENGLISH).equals(group)) return i;
        }
        return 99;
    }

    private void sendHeaderFooter(Player player, String header, String footer) {
        try {
            PacketAccess access = packetAccess();
            if (access != null) access.send(player, header, footer);
        } catch (Throwable error) {
            if (!packetAccessFailed) plugin.getLogger().warning("Unable to send tablist header/footer: " + error.getMessage());
            packetAccessFailed = true;
        }
    }

    private PacketAccess packetAccess() throws Exception {
        if (packetAccess != null) return packetAccess;
        if (packetAccessFailed) return null;
        packetAccess = new PacketAccess();
        return packetAccess;
    }

    private void updatePlayerInfoEntry(Player target, String displayName) {
        boolean externallyChanged = target.getPlayerListName() != null && !target.getPlayerListName().equals(displayName);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Map<UUID, String> entries = tabEntryNames.computeIfAbsent(viewer.getUniqueId(), ignored -> new HashMap<>());
            String previous = entries.get(target.getUniqueId());
            if (displayName.equals(previous) && !externallyChanged) continue;
            try {
                if (playerInfoAccess == null) playerInfoAccess = new PlayerInfoAccess();
                playerInfoAccess.send(viewer, target, displayName);
                entries.put(target.getUniqueId(), displayName);
                lastTransports.put(target.getUniqueId(), "v1_8_R3 UPDATE_DISPLAY_NAME");
                lastFallbacks.put(target.getUniqueId(), "none");
            } catch (Throwable failure) {
                lastTransports.put(target.getUniqueId(), "Bukkit Player#setPlayerListName");
                lastFallbacks.put(target.getUniqueId(), failure.getClass().getSimpleName() + ": " + failure.getMessage());
                try {
                    target.setPlayerListName(displayName);
                    entries.put(target.getUniqueId(), displayName);
                } catch (Throwable apiFailure) {
                    if (plugin.registry().tablist().debugEnabled()) plugin.getLogger().warning("Tab display update failed target="
                            + target.getName() + " viewer=" + viewer.getName() + " native=" + failure.getMessage()
                            + " bukkit=" + apiFailure.getMessage());
                }
            }
        }
    }

    public void refreshBounty(Player target) {
        if (target == null) return;
        lastRefreshReasons.put(target.getUniqueId(), "BOUNTY_CHANGED");
        formattingGeneration++;
        pendingBountyRefreshes.add(target.getUniqueId());
        if (bountyRefreshQueued) return;
        bountyRefreshQueued = true;
        Bukkit.getScheduler().runTask(plugin, () -> {
            bountyRefreshQueued = false;
            Set<UUID> refreshes = new HashSet<>(pendingBountyRefreshes);
            pendingBountyRefreshes.clear();
            List<Player> players = new ArrayList<>();
            for (UUID uuid : refreshes) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    update(player);
                    players.add(player);
                }
            }
            updateNametagTargets(players);
        });
    }

    public void markRefresh(Player target, String reason) {
        if (target == null) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> markRefresh(target, reason));
            return;
        }
        lastRefreshReasons.put(target.getUniqueId(), reason == null ? "UNSPECIFIED" : reason);
        formattingGeneration++;
        update(target);
        updateNametagTargets(java.util.Collections.singletonList(target));
    }

    public void nameDebug(CommandSender sender, Player target) {
        NetworkProfileSnapshot snapshot = plugin.getNetworkProfileCache().get(target);
        PitPlayerNameFormatter.ResolvedName value = plugin.getPlayerNameFormatter().resolve(target, snapshot);
        sender.sendMessage(CC.color("&eName diagnostics for &f" + target.getName()));
        sender.sendMessage(CC.color("&7Abelix profile loaded: &f" + plugin.getNetworkProfileCache().isCached(target.getUniqueId())));
        sender.sendMessage(CC.color("&7The Pit profile loaded: &f" + plugin.getStatsManager().isProfileLoaded(target.getUniqueId())));
        sender.sendMessage(CC.color("&7Raw level: &f" + plugin.getStatsManager().getLevel(target.getUniqueId()) + " &7prestige: &f" + plugin.getStatsManager().getPrestige(target.getUniqueId())));
        sender.sendMessage(CC.color("&7%level_formatted%: &r" + value.levelFormatted()));
        sender.sendMessage(CC.color("&7Raw Abelix prefix: &r" + value.abelixPrefix()));
        sender.sendMessage(CC.color("&7Pit level: &r" + value.levelFormatted()));
        sender.sendMessage(CC.color("&7Player entry: &f" + value.playerName()));
        sender.sendMessage(CC.color("&7Username color: &r" + visibleColors(value.usernameColor())));
        sender.sendMessage(CC.color("&7Supporter badges: &r" + value.supporterStar()));
        sender.sendMessage(CC.color("&7Raw bounty: &f" + plugin.getStatsManager().getBounty(target.getUniqueId())));
        sender.sendMessage(CC.color("&7%bounty_formatted%: &r" + value.bountyFormatted()));
        String template = plugin.registry().tablist().template(me.outerspace.thepit.registry.TablistRegistry.TAB_PLAYER_NAME);
        sender.sendMessage(CC.color("&7Template lookup key: &f" + me.outerspace.thepit.registry.TablistRegistry.TAB_PLAYER_NAME));
        sender.sendMessage(ChatColor.GRAY + "Raw tab template: " + ChatColor.WHITE + template);
        sender.sendMessage(CC.color("&7Final colored value: &r" + value.tabListDisplay()));
        sender.sendMessage(CC.color("&7Visible length: &f" + visibleLength(value.tabListDisplay()) + " &7encoded length: &f" + value.tabListDisplay().length()));
        sender.sendMessage(CC.color("&7Transport: &f" + lastTransports.getOrDefault(target.getUniqueId(), "not sent")));
        sender.sendMessage(CC.color("&7Fallback: &f" + lastFallbacks.getOrDefault(target.getUniqueId(), "none")));
        sender.sendMessage(CC.color("&7Team entry: &f" + value.playerName() + " &8(" + value.playerName().length() + ")"));
        sender.sendMessage(CC.color("&7Team prefix: &r" + visibleColors(value.teamPrefix()) + " &8(" + value.teamPrefix().length() + "/16)"));
        sender.sendMessage(CC.color("&7Team suffix: &r" + visibleColors(value.teamSuffix()) + " &8(" + value.teamSuffix().length() + "/16)"));
        sender.sendMessage(CC.color("&7Vanilla fields supported: &f" + value.teamFieldsSupported()));
        sender.sendMessage(CC.color("&7Last reason: &f" + lastRefreshReasons.getOrDefault(target.getUniqueId(), "PERIODIC") + " &7generation: &f" + formattingGeneration));
    }

    private void debugResolved(Player player, PitPlayerNameFormatter.ResolvedName value) {
        if (!plugin.registry().tablist().debugEnabled()) return;
        String reason = lastRefreshReasons.getOrDefault(player.getUniqueId(), "PERIODIC");
        plugin.getLogger().info("Tablist name uuid=" + player.getUniqueId()
                + " level_formatted='" + value.levelFormatted() + "' abelix_prefix='" + value.abelixPrefix()
                + "' player='" + value.playerName() + "' username_color='" + visibleColors(value.usernameColor())
                + "' supporter_star='" + value.supporterStar()
                + "' bounty_formatted='" + value.bountyFormatted() + "' tab_display='" + value.tabListDisplay()
                + "' team_prefix='" + visibleColors(value.teamPrefix()) + "' prefix_length=" + value.teamPrefix().length()
                + " team_entry='" + value.playerName() + "' team_suffix='" + visibleColors(value.teamSuffix())
                + "' suffix_length=" + value.teamSuffix().length() + " fields_supported=" + value.teamFieldsSupported() + " reason=" + reason);
    }

    private String visibleColors(String value) {
        return (value == null ? "" : value).replace(ChatColor.COLOR_CHAR, '&');
    }

    private int visibleLength(String value) {
        String stripped = ChatColor.stripColor(value == null ? "" : value);
        return stripped == null ? 0 : stripped.length();
    }

    private String safeTeamName(String text) {
        if (text == null || text.isEmpty()) return "bffa";
        return text.length() > 16 ? text.substring(0, 16) : text;
    }

    private String toJson(String text) {
        return "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"}";
    }

    private final class TablistUpdateTask implements Runnable {
        @Override
        public void run() {
            runCycle();
        }
    }

    private final class PacketAccess {
        private final Method deserialize;
        private final Constructor<?> constructor;
        private final Constructor<?> emptyConstructor;
        private final Field headerField;
        private final Field footerField;
        private final Method getHandle;
        private final Field connectionField;
        private final Method sendPacket;

        private PacketAccess() throws Exception {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> serializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Class<?> componentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerListHeaderFooter");
            Class<?> packetBase = Class.forName("net.minecraft.server." + version + ".Packet");
            deserialize = serializerClass.getMethod("a", String.class);
            Constructor<?> componentConstructor = null;
            try { componentConstructor = packetClass.getConstructor(componentClass); } catch (NoSuchMethodException ignored) { }
            constructor = componentConstructor;
            emptyConstructor = constructor == null ? packetClass.getDeclaredConstructor() : null;
            if (emptyConstructor != null) emptyConstructor.setAccessible(true);
            headerField = packetClass.getDeclaredField("a");
            footerField = packetClass.getDeclaredField("b");
            headerField.setAccessible(true);
            footerField.setAccessible(true);
            Player sample = Bukkit.getOnlinePlayers().iterator().next();
            getHandle = sample.getClass().getMethod("getHandle");
            Object handle = getHandle.invoke(sample);
            connectionField = handle.getClass().getField("playerConnection");
            Object connection = connectionField.get(handle);
            sendPacket = connection.getClass().getMethod("sendPacket", packetBase);
        }

        private void send(Player player, String header, String footer) throws Exception {
            Object headerComponent = deserialize.invoke(null, toJson(header));
            Object footerComponent = deserialize.invoke(null, toJson(footer));
            Object packet = constructor == null ? emptyConstructor.newInstance() : constructor.newInstance(headerComponent);
            headerField.set(packet, headerComponent);
            footerField.set(packet, footerComponent);
            Object handle = getHandle.invoke(player);
            Object connection = connectionField.get(handle);
            sendPacket.invoke(connection, packet);
        }
    }

    private final class PlayerInfoAccess {
        private final Object updateDisplayName;
        private final Constructor<?> packetConstructor;
        private final Class<?> entityPlayerClass;
        private final Method componentDeserialize;
        private final Method getHandle;
        private final Field listName;
        private final Field playerConnection;
        private final Method sendPacket;

        private PlayerInfoAccess() throws Exception {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            entityPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
            Class<?> actionClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutPlayerInfo");
            Class<?> packetBase = Class.forName("net.minecraft.server." + version + ".Packet");
            Class<?> componentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> serializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            updateDisplayName = Enum.valueOf((Class<? extends Enum>) actionClass.asSubclass(Enum.class), "UPDATE_DISPLAY_NAME");
            packetConstructor = packetClass.getConstructor(actionClass, Array.newInstance(entityPlayerClass, 0).getClass());
            componentDeserialize = serializerClass.getMethod("a", String.class);
            Player sample = Bukkit.getOnlinePlayers().iterator().next();
            getHandle = sample.getClass().getMethod("getHandle");
            Object handle = getHandle.invoke(sample);
            listName = entityPlayerClass.getField("listName");
            playerConnection = handle.getClass().getField("playerConnection");
            Object connection = playerConnection.get(handle);
            sendPacket = connection.getClass().getMethod("sendPacket", packetBase);
        }

        private void send(Player viewer, Player target, String displayName) throws Exception {
            Object targetHandle = getHandle.invoke(target);
            Object component = componentDeserialize.invoke(null, toJson(displayName));
            listName.set(targetHandle, component);
            Object players = Array.newInstance(entityPlayerClass, 1);
            Array.set(players, 0, targetHandle);
            Object packet = packetConstructor.newInstance(updateDisplayName, players);
            Object viewerHandle = getHandle.invoke(viewer);
            Object connection = playerConnection.get(viewerHandle);
            sendPacket.invoke(connection, packet);
        }
    }

    private static final class TabState {
        private final String header;
        private final String footer;
        private final String name;

        private TabState(String header, String footer, String name) {
            this.header = header;
            this.footer = footer;
            this.name = name;
        }
    }

    private static final class RenderedNametag {
        private final String teamName;
        private final String prefix;
        private final String suffix;
        private final boolean supported;
        private final int level;
        private final int prestige;

        private RenderedNametag(String teamName, String prefix, String suffix, boolean supported, int level, int prestige) {
            this.teamName = teamName;
            this.prefix = prefix;
            this.suffix = suffix;
            this.supported = supported;
            this.level = level;
            this.prestige = prestige;
        }
    }

    private static final class TeamAssignment {
        private final String teamName;
        private final String entry;
        private final String prefix;
        private final String suffix;
        private final int level;
        private final int prestige;

        private TeamAssignment(String teamName, String entry, String prefix, String suffix, int level, int prestige) {
            this.teamName = teamName;
            this.entry = entry;
            this.prefix = prefix;
            this.suffix = suffix;
            this.level = level;
            this.prestige = prestige;
        }
    }

    private static final class RenderContext {
        private final Player player;
        private final UUID uuid;
        private final NetworkProfileSnapshot snapshot;
        private final Map<String, String> values = new HashMap<>();

        private RenderContext(Player player, NetworkProfileSnapshot snapshot) {
            this.player = player;
            this.uuid = player.getUniqueId();
            this.snapshot = snapshot;
        }

        private String replace(String input, String placeholder, Supplier<String> supplier) {
            if (!input.contains(placeholder)) return input;
            String value = values.get(placeholder);
            if (value == null) {
                value = supplier.get();
                if (value == null) value = "";
                values.put(placeholder, value);
            }
            return input.replace(placeholder, value);
        }
    }
}
