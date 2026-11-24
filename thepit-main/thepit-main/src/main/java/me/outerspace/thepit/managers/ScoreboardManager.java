package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScoreboardManager implements Listener {
    private final ThePitPlugin plugin;
    private final Map<UUID, BoardState> states = new HashMap<>();
    private final Map<UUID, String> megastreaks = new ConcurrentHashMap<>();
    private final Set<String> configurationWarnings = new HashSet<>();
    private volatile boolean majorEventActive;
    private BukkitTask renderTask;
    private SimpleDateFormat dateFormatter;
    private TimeZone dateTimeZone;
    private volatile String cachedDate = "";
    private int cachedCalendarDate = Integer.MIN_VALUE;
    private long nextDateCheck;

    public ScoreboardManager(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        cancelTasks();
        configureDate();
        schedule();
    }

    public void reload() {
        cancelTasks();
        configureDate();
        if (!enabled()) {
            removeManagedBoards();
            return;
        }
        schedule();
        for (Player player : Bukkit.getOnlinePlayers()) render(player);
    }

    public void stop() {
        cancelTasks();
        removeManagedBoards();
        megastreaks.clear();
        majorEventActive = false;
    }

    public void update(Player player) {
        if (!enabled() || player == null || !player.isOnline()) return;
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> update(player));
            return;
        }
        render(player);
    }

    public String neededXp(Player player) {
        if (player == null) return "MAXED";
        UUID uuid = player.getUniqueId();
        int required = plugin.getStatsManager().getRequiredXp(uuid);
        if (required <= 0) return "MAXED";
        return String.valueOf(Math.max(0L, (long) required - plugin.getStatsManager().getLevelXp(uuid)));
    }

    public String status(Player player) {
        if (player == null) return "&aIdling";
        if (majorEventActive) return "&dEvent";
        if (plugin.getPitCommandManager() != null) {
            String event = plugin.getPitCommandManager().carePackageStatus();
            if (!event.isEmpty()) return event;
        }
        String megastreak = megastreaks.get(player.getUniqueId());
        if (megastreak != null && !megastreak.isEmpty()) return megastreak;
        if (plugin.getStatsManager().getBounty(player.getUniqueId()) > 0) return "&6Bountied";
        if(plugin.getCombatLogManager()!=null&&plugin.getCombatLogManager().isTagged(player.getUniqueId()))return "&cFighting";
        return "&aIdling";
    }

    public boolean isCombatTagged(Player player) {
        if (player == null) return false;
        if (plugin.getCombatLogManager() != null) return plugin.getCombatLogManager().isTagged(player.getUniqueId());
        return false;
    }

    public boolean hasMegastreak(UUID uuid) {
        return uuid != null && megastreaks.containsKey(uuid);
    }

    public String effectSummary(Player player) {
        List<String> effects = effectLines(player);
        return effects.isEmpty() ? "" : String.join(", ", effects);
    }

    public String prestigeLine(Player player) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();
        int prestige = plugin.getStatsManager().getPrestige(uuid);
        return prestige <= 0 ? "" : "&fPrestige: " + plugin.getStatsManager().getPrestigeBracketColor(uuid) + plugin.getStatsManager().getPrestigeRoman(uuid);
    }

    public String bountyLine(Player player) {
        if (player == null) return "";
        int bounty = plugin.getStatsManager().getBounty(player.getUniqueId());
        return bounty <= 0 ? "" : "&fBounty: &6" + format(bounty) + "g";
    }

    public String streakLine(Player player) {
        if (player == null) return "";
        int streak = plugin.getStatsManager().getKillStreak(player.getUniqueId(), Period.LIFETIME);
        return streak <= 0 ? "" : "&fStreak: &a" + format(streak);
    }

    public String formattedNumber(long value) {
        return format(value);
    }

    public String formattedGold(UUID uuid) {
        me.outerspace.thepit.model.NetworkProfileSnapshot snapshot = uuid == null || plugin.getNetworkProfileCache() == null ? null : plugin.getNetworkProfileCache().cached(uuid);
        return format(snapshot == null ? 0L : snapshot.getGold());
    }

    public String formattedDate() {
        if (cachedDate.isEmpty()) refreshDateCache(true);
        return cachedDate;
    }

    public void setMajorEventActive(boolean active) {
        majorEventActive = active;
        refreshAll();
    }

    public void setMegastreakStatus(UUID uuid, String megastreak) {
        if (uuid == null) return;
        String status = megastreakStatus(megastreak);
        if (status.isEmpty()) megastreaks.remove(uuid);
        else megastreaks.put(uuid, status);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) update(player);
    }

    public void clearMegastreakStatus(UUID uuid) {
        if (uuid == null) return;
        megastreaks.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) update(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BoardState state = states.remove(uuid);
        if (state != null) cleanupBoard(state.board);
        megastreaks.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        megastreaks.remove(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> update(event.getEntity()));
    }

    private void configureDate() {
        String timezoneName = plugin.registry().scoreboard().getString("scoreboard.date-timezone", "America/Los_Angeles");
        Set<String> timezones = new HashSet<>();
        java.util.Collections.addAll(timezones, TimeZone.getAvailableIDs());
        if (timezoneName == null || !timezones.contains(timezoneName)) {
            dateTimeZone = TimeZone.getDefault();
            warnConfiguration("date-timezone", "Invalid scoreboard date timezone '" + timezoneName + "'. Using server timezone " + dateTimeZone.getID() + ".");
        } else dateTimeZone = TimeZone.getTimeZone(timezoneName);
        String format = plugin.registry().scoreboard().getString("scoreboard.date-format", "MM/dd/yy");
        try {
            dateFormatter = new SimpleDateFormat(format, Locale.US);
        } catch (IllegalArgumentException exception) {
            dateFormatter = new SimpleDateFormat("MM/dd/yy", Locale.US);
            warnConfiguration("date-format", "Invalid scoreboard date format '" + format + "'. Using MM/dd/yy.");
        }
        dateFormatter.setTimeZone(dateTimeZone);
        cachedDate = "";
        cachedCalendarDate = Integer.MIN_VALUE;
        nextDateCheck = 0L;
        refreshDateCache(true);
    }

    private void refreshDateCache(boolean force) {
        if (dateFormatter == null || dateTimeZone == null) return;
        long now = System.currentTimeMillis();
        if (!force && now < nextDateCheck) return;
        nextDateCheck = now + 10000L;
        Calendar calendar = Calendar.getInstance(dateTimeZone);
        int date = calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR);
        if (!force && date == cachedCalendarDate) return;
        cachedCalendarDate = date;
        cachedDate = dateFormatter.format(calendar.getTime());
    }

    private void warnConfiguration(String key, String message) {
        if (configurationWarnings.add(key)) plugin.getLogger().warning(message);
    }

    private void schedule() {
        if (!enabled()) return;
        long updateTicks = Math.max(20L, plugin.registry().scoreboard().getLong("scoreboard.update-ticks", 20L));
        renderTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runRenderCycle, 1L, updateTicks);
    }

    private boolean enabled() {
        return plugin.registry().scoreboard().getBoolean("scoreboard.enabled", true);
    }

    private void cancelTasks() {
        if (renderTask != null) renderTask.cancel();
        renderTask = null;
    }

    private void runRenderCycle() {
        refreshDateCache(false);
        Set<UUID> online = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            online.add(player.getUniqueId());
            render(player);
        }
        Iterator<Map.Entry<UUID, BoardState>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BoardState> entry = iterator.next();
            if (online.contains(entry.getKey())) continue;
            cleanupBoard(entry.getValue().board);
            iterator.remove();
        }
        megastreaks.keySet().retainAll(online);
    }

    private void refreshAll() {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, this::refreshAll);
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) render(player);
    }

    private void render(Player player) {
        if (!enabled() || player == null || !player.isOnline()) return;
        Scoreboard board = player.getScoreboard();
        BoardState previous = states.get(player.getUniqueId());
        boolean create = board == null || board == Bukkit.getScoreboardManager().getMainScoreboard() || board.getObjective("buildffa") == null;
        boolean scoreboardReplaced = create || previous != null && previous.board != board;
        if (create) {
            if (previous != null && previous.board != board) cleanupBoard(previous.board);
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            Objective objective = board.registerNewObjective("buildffa", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            player.setScoreboard(board);
            previous = null;
            if (plugin.getNpcService() != null) plugin.getNpcService().applyHiddenNpcTeams(board);
        } else if (previous != null && previous.board != board) {
            cleanupBoard(previous.board);
            previous = null;
        }

        Objective objective = board.getObjective("buildffa");
        String title = safeLimit(CC.color(plugin.registry().scoreboard().getString("scoreboard.title", "&b&lThe Pit")), 32);
        List<String> rendered = visibleLines(player);
        if (previous == null || !title.equals(previous.title)) objective.setDisplayName(title);
        int oldSize = previous == null ? managedLineCount(board) : previous.lines.size();
        for (int index = 0; index < rendered.size(); index++) {
            String oldLine = previous == null || index >= previous.lines.size() ? null : previous.lines.get(index);
            String newLine = rendered.get(index);
            if (!newLine.equals(oldLine)) {
                setLine(board, objective, index, newLine, 15 - index, oldLine == null);
            }
        }
        for (int index = rendered.size(); index < oldSize; index++) removeLine(board, index);
        states.put(player.getUniqueId(), new BoardState(board, title, rendered));
        if (scoreboardReplaced && plugin.getTablistManager() != null) {
            plugin.getTablistManager().synchronizeViewerScoreboard(player, "SIDEBAR_SCOREBOARD_REPLACED");
        }
    }

    private List<String> visibleLines(Player player) {
        List<VisibleLine> lines = new ArrayList<>();
        List<String> effects = effectLines(player);
        for (String configured : plugin.registry().scoreboard().getStringList("scoreboard.lines")) {
            String token = configured == null ? "" : configured.trim();
            if (token.equals("%effect_lines%")) {
                for (String effect : effects) lines.add(new VisibleLine(effect, LineType.EFFECT));
                continue;
            }
            String resolved = resolveInternal(player, configured);
            if (optional(token) && resolved.trim().isEmpty()) continue;
            LineType type = lineType(token);
            lines.add(new VisibleLine(resolved, type));
        }
        reduceToLimit(lines);
        removeConsecutiveBlanks(lines);
        List<String> rendered = new ArrayList<>();
        for (VisibleLine line : lines) rendered.add(CC.color(line.text));
        return rendered;
    }

    private String resolveInternal(Player player, String line) {
        String out = line == null ? "" : line;
        UUID uuid = player.getUniqueId();
        int prestige = plugin.getStatsManager().getPrestige(uuid);
        String prestigeRoman = plugin.getStatsManager().getPrestigeRoman(uuid);
        String prestigeColor = plugin.getStatsManager().getPrestigeBracketColor(uuid);
        String level = plugin.getStatsManager().getFormattedLevel(uuid, StatsManager.LevelRenderContext.SCOREBOARD);
        String needed = neededXp(player);
        String coins = format(plugin.getStatsManager().getCoins(uuid, Period.LIFETIME));
        int bountyValue = plugin.getStatsManager().getBounty(uuid);
        int streakValue = plugin.getStatsManager().getKillStreak(uuid, Period.LIFETIME);
        out = out.replace("%date%", formattedDate()).replace("%thepit_date%", formattedDate());
        out = out.replace("%prestige_line%", prestigeLine(player));
        out = out.replace("%prestige_level%", level);
        out = out.replace("%prestige_color%", prestigeColor);
        out = out.replace("%prestige_roman%", prestigeRoman).replace("%thepit_prestige_roman%", prestigeRoman);
        out = out.replace("%thepit_prestige%", String.valueOf(prestige));
        out = out.replace("%level_formatted%", level).replace("%thepit_level_formatted%", level);
        out = out.replace("%level%", String.valueOf(plugin.getStatsManager().getLevel(uuid))).replace("%thepit_level%", String.valueOf(plugin.getStatsManager().getLevel(uuid)));
        out = out.replace("%needed_xp%", needed).replace("%thepit_needed_xp%", needed);
        out = out.replace("%thepit_abelix_gold%", formattedGold(uuid));
        out = out.replace("%coins%", coins).replace("%thepit_coins%", coins)
                .replace("%pit_gold%", coins).replace("%thepit_pit_gold%", coins);
        out = out.replace("%status%", status(player)).replace("%thepit_status%", status(player));
        out = out.replace("%bounty_line%", bountyLine(player));
        out = out.replace("%bounty%", format(bountyValue)).replace("%thepit_bounty%", format(bountyValue));
        out = out.replace("%streak_line%", streakLine(player));
        out = out.replace("%killstreak%", format(streakValue)).replace("%thepit_killstreak%", format(streakValue));
        out = out.replace("%effect_lines%", "");
        return out;
    }

    private List<String> effectLines(Player player) {
        if (player == null) return new ArrayList<>();
        boolean effectsEnabled=plugin.registry().scoreboard().getBoolean("scoreboard.effects.enabled", true);
        List<String> allowlist = plugin.registry().scoreboard().getStringList("scoreboard.effects.allowlist");
        Map<String, Integer> priorities = new LinkedHashMap<>();
        for (int index = 0; index < allowlist.size(); index++) priorities.put(allowlist.get(index).toUpperCase(Locale.ENGLISH), index);
        List<EffectLine> active = new ArrayList<>();
        if (plugin.getCombatLogManager() != null) {
            String combat = plugin.getCombatLogManager().display(player.getUniqueId());
            if (!combat.isEmpty()) active.add(new EffectLine(combat,(int)plugin.getCombatLogManager().remainingSeconds(player.getUniqueId()),Integer.MIN_VALUE));
        }
        if(!effectsEnabled){List<String> combatOnly=new ArrayList<>();for(EffectLine line:active)combatOnly.add(line.text);return combatOnly;}
        Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
        for (PotionEffect effect : potionEffects) {
            String key = effect.getType().getName().toUpperCase(Locale.ENGLISH);
            if (!priorities.containsKey(key) || effect.getDuration() <= 0) continue;
            int seconds = Math.max(1, (effect.getDuration() + 19) / 20);
            int level = Math.max(1, effect.getAmplifier() + 1);
            String name = plugin.registry().scoreboard().getString("scoreboard.effects.names." + key, readable(key));
            String color = plugin.registry().scoreboard().getString("scoreboard.effects.level-colors." + key, "&f");
            active.add(new EffectLine("&f" + name + " " + color + roman(level) + " &7(" + seconds + "s)", seconds, priorities.get(key)));
        }
        if (plugin.getShopManager() != null) {
            for (String line : plugin.getShopManager().effectLines(player)) active.add(new EffectLine(line, effectSeconds(line), -1));
        }
        if (plugin.getKillstreakManager() != null) {
            for (String line : plugin.getKillstreakManager().effectLines(player)) active.add(new EffectLine(line, effectSeconds(line), -1));
        }
        String sort = plugin.registry().scoreboard().getString("scoreboard.effects.sort", "SHORTEST_REMAINING").toUpperCase(Locale.ENGLISH);
        Comparator<EffectLine> comparator;
        if (sort.equals("LONGEST_REMAINING")) comparator = Comparator.comparingInt(EffectLine::seconds).reversed().thenComparingInt(EffectLine::priority);
        else if (sort.equals("CONFIGURED_PRIORITY") || sort.equals("PRIORITY")) comparator = Comparator.comparingInt(EffectLine::priority).thenComparingInt(EffectLine::seconds);
        else comparator = Comparator.comparingInt(EffectLine::seconds).thenComparingInt(EffectLine::priority);
        active.sort(comparator);
        int topMaximum = Math.max(0, plugin.registry().scoreboard().getInt("scoreboard.maximum-effect-lines", 3));
        int effectMaximum = Math.max(0, plugin.registry().scoreboard().getInt("scoreboard.effects.maximum-lines", topMaximum));
        int maximum = Math.min(topMaximum, effectMaximum);
        if(plugin.getCombatLogManager()!=null&&plugin.getCombatLogManager().isTagged(player.getUniqueId()))maximum=Math.max(1,maximum);
        List<String> result = new ArrayList<>();
        for (EffectLine effect : active) {
            if (result.size() >= maximum) break;
            result.add(effect.text);
        }
        return result;
    }

    private int effectSeconds(String line) {
        if (line == null) return Integer.MAX_VALUE;
        int open = line.lastIndexOf('(');
        int end = line.lastIndexOf('s');
        if (open < 0 || end <= open) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(org.bukkit.ChatColor.stripColor(CC.color(line.substring(open + 1, end))));
        } catch (NumberFormatException ignored) {
            return Integer.MAX_VALUE;
        }
    }

    private void reduceToLimit(List<VisibleLine> lines) {
        while (lines.size() > 15 && removeLast(lines, LineType.EFFECT)) { }
        while (lines.size() > 15 && removeLast(lines, LineType.BLANK)) { }
        while (lines.size() > 15 && removeLast(lines, LineType.OTHER)) { }
        while (lines.size() > 15 && removeLast(lines, LineType.STREAK)) { }
        while (lines.size() > 15 && removeLast(lines, LineType.BOUNTY)) { }
        while (lines.size() > 15 && removeLast(lines, LineType.PRESTIGE)) { }
        while (lines.size() > 15) lines.remove(lines.size() - 1);
    }

    private boolean removeLast(List<VisibleLine> lines, LineType type) {
        for (int index = lines.size() - 1; index >= 0; index--) {
            if (lines.get(index).type != type) continue;
            lines.remove(index);
            return true;
        }
        return false;
    }

    private void removeConsecutiveBlanks(List<VisibleLine> lines) {
        for (int index = lines.size() - 1; index > 0; index--) {
            if (lines.get(index).text.isEmpty() && lines.get(index - 1).text.isEmpty()) lines.remove(index);
        }
        while (!lines.isEmpty() && lines.get(0).text.isEmpty()) lines.remove(0);
        while (!lines.isEmpty() && lines.get(lines.size() - 1).text.isEmpty()) lines.remove(lines.size() - 1);
    }

    private boolean optional(String token) {
        return token.equals("%prestige_line%") || token.equals("%bounty_line%") || token.equals("%streak_line%") || token.equals("%effect_lines%");
    }

    private LineType lineType(String token) {
        if (token.equals("%streak_line%")) return LineType.STREAK;
        if (token.equals("%bounty_line%")) return LineType.BOUNTY;
        if (token.equals("%prestige_line%")) return LineType.PRESTIGE;
        if (token.isEmpty()) return LineType.BLANK;
        if (token.contains("%date%") || token.contains("%thepit_date%")
                || token.contains("%level_formatted%") || token.contains("%thepit_level_formatted%")
                || token.contains("%needed_xp%") || token.contains("%thepit_needed_xp%")
                || token.contains("%coins%") || token.contains("%thepit_coins%")
                || token.contains("%pit_gold%") || token.contains("%thepit_pit_gold%")
                || token.contains("%status%") || token.contains("%thepit_status%")
                || token.toLowerCase(Locale.ENGLISH).contains("mc.fakepixel.me")) return LineType.REQUIRED;
        return LineType.OTHER;
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (!(damager instanceof Projectile)) return null;
        Object shooter = ((Projectile) damager).getShooter();
        return shooter instanceof Player ? (Player) shooter : null;
    }

    private String megastreakStatus(String value) {
        if (value == null) return "";
        String normalized = value.toLowerCase(Locale.ENGLISH).replace("_", "").replace(" ", "");
        if (normalized.equals("overdrive")) return "&bOverdrive";
        if (normalized.equals("beastmode")) return "&cBeastmode";
        if (normalized.equals("hermit")) return "&7Hermit";
        if (normalized.equals("highlander")) return "&6Highlander";
        if (normalized.equals("magnumopus")) return "&dMagnum Opus";
        if (normalized.equals("tothemoon")) return "&bTo the Moon";
        if (normalized.equals("uberstreak")) return "&5Uberstreak";
        return "";
    }

    private String readable(String value) {
        String[] words = value.toLowerCase(Locale.ENGLISH).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private String roman(int value) {
        int number = Math.max(1, value);
        int[] values = {50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < values.length; index++) {
            while (number >= values[index]) {
                result.append(numerals[index]);
                number -= values[index];
            }
        }
        return result.toString();
    }

    private String format(long value) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0L, value));
    }

    private void setLine(Scoreboard board, Objective objective, int line, String text, int score, boolean updateScore) {
        String teamName = "line" + line;
        Team team = board.getTeam(teamName);
        boolean newTeam = team == null;
        if (newTeam) team = board.registerNewTeam(teamName);
        String entry = entry(line);
        if (!team.hasEntry(entry)) team.addEntry(entry);
        LineParts parts = split(text);
        if (!team.getPrefix().equals(parts.prefix)) team.setPrefix(parts.prefix);
        if (!team.getSuffix().equals(parts.suffix)) team.setSuffix(parts.suffix);
        if (newTeam || updateScore) objective.getScore(entry).setScore(score);
    }

    private LineParts split(String text) {
        String prefix = safeLimit(text == null ? "" : text, 16);
        if (text == null || prefix.length() >= text.length()) return new LineParts(prefix, "");
        String remaining = text.substring(prefix.length());
        String suffix = safeLimit(ChatColor.getLastColors(prefix) + remaining, 16);
        return new LineParts(prefix, suffix);
    }

    private String safeLimit(String text, int maximum) {
        if (text == null || text.length() <= maximum) return text == null ? "" : text;
        int end = maximum;
        if (end > 0 && text.charAt(end - 1) == ChatColor.COLOR_CHAR) end--;
        return text.substring(0, end);
    }

    private int managedLineCount(Scoreboard board) {
        int count = 0;
        while (board.getTeam("line" + count) != null) count++;
        return count;
    }

    private void removeLine(Scoreboard board, int line) {
        Team team = board.getTeam("line" + line);
        if (team == null) return;
        for (String entry : new ArrayList<>(team.getEntries())) board.resetScores(entry);
        team.unregister();
    }

    private String entry(int line) {
        ChatColor[] colors = ChatColor.values();
        return colors[line % colors.length].toString() + ChatColor.RESET;
    }

    private void removeManagedBoards() {
        for (BoardState state : new ArrayList<>(states.values())) cleanupBoard(state.board);
        states.clear();
    }

    private void cleanupBoard(Scoreboard board) {
        if (board == null) return;
        Objective objective = board.getObjective("buildffa");
        if (objective != null) objective.unregister();
        int count = managedLineCount(board);
        for (int index = 0; index < count; index++) removeLine(board, index);
    }

    private record LineParts(String prefix, String suffix) {}
    private record EffectLine(String text, int seconds, int priority) {}
    private record VisibleLine(String text, LineType type) {}
    private enum LineType { REQUIRED, PRESTIGE, BOUNTY, STREAK, EFFECT, BLANK, OTHER }

    private static final class BoardState {
        private final Scoreboard board;
        private final String title;
        private final List<String> lines;

        private BoardState(Scoreboard board, String title, List<String> lines) {
            this.board = board;
            this.title = title;
            this.lines = new ArrayList<>(lines);
        }
    }
}
