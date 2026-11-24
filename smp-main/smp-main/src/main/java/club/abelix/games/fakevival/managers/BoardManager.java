package club.abelix.games.fakevival.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import club.abelix.games.fakevival.util.ScoreboardTeamUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BoardManager {
    private static final String OBJECTIVE_NAME = "fakevival";
    private static final String OWNER_TEAM = "fv_board";
    private static final int MAX_LINES = 15;

    private final JavaPlugin plugin;
    private final TablistManager tablistManager;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask task;

    public BoardManager(JavaPlugin plugin, TablistManager tablistManager) {
        this.plugin = plugin;
        this.tablistManager = tablistManager;
    }

    public void start() {
        stop();
        long interval = Math.max(20L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("scoreboard.update-interval-ticks", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("tablist.update-ticks", 40L)));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tablistManager.apply(player);
                update(player);
            }
        }, 20L, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        boards.clear();
    }

    public void update(Player player) {
        if (Bukkit.getScoreboardManager() == null) return;
        Scoreboard current = player.getScoreboard();
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null || !isManaged(board)) {
            board = isManaged(current) ? current : Bukkit.getScoreboardManager().getNewScoreboard();
            boards.put(player.getUniqueId(), board);
        }
        ensureManaged(board);

        if (current == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            ensureManaged(board);
            boards.put(player.getUniqueId(), board);
        }

        applyNametags(player, board);

        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("scoreboard.enabled", true)) {
            Objective objective = board.getObjective(OBJECTIVE_NAME);
            if (objective == null) objective = board.registerNewObjective(OBJECTIVE_NAME, "dummy");
            if (objective.getDisplaySlot() != DisplaySlot.SIDEBAR) objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            String title = tablistManager.parse(player, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("scoreboard.title", "&b&lFakevival"));
            if (!objective.getDisplayName().equals(title)) objective.setDisplayName(title);

            List<String> lines = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("scoreboard.lines");
            int lineCount = Math.min(lines.size(), MAX_LINES);
            int score = lineCount;
            int index = 0;
            for (String line : lines) {
                if (index >= MAX_LINES) break;
                addScoreLine(board, objective, tablistManager.parse(player, line), score--, index++);
            }
            clearUnusedLines(board, objective, index);
        } else {
            Objective objective = board.getObjective(OBJECTIVE_NAME);
            if (objective != null) objective.unregister();
        }

        if (player.getScoreboard() != board) player.setScoreboard(board);
    }

    public void remove(Player player) {
        if (player != null) boards.remove(player.getUniqueId());
    }

    private boolean isManaged(Scoreboard board) {
        return board != null && board.getTeam(OWNER_TEAM) != null;
    }

    private void ensureManaged(Scoreboard board) {
        ScoreboardTeamUtil.getOrCreate(board, OWNER_TEAM);
    }

    private void applyNametags(Player viewer, Scoreboard board) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("nametag.enabled", true)) return;
        String format = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("nametag.format", "%abelix_prefix%%player%");
        int index = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            String teamName = "nt" + index++;
            Team team = ScoreboardTeamUtil.getOrCreate(board, teamName);
            if (team == null) continue;
            for (String entry : List.copyOf(team.getEntries())) {
                if (!entry.equals(target.getName())) team.removeEntry(entry);
            }
            team.addEntry(target.getName());
            String parsed = tablistManager.parse(target, format);
            String name = target.getName();
            int nameIndex = parsed.indexOf(name);
            if (nameIndex >= 0) {
                String prefix = removeTrailingReset(parsed.substring(0, nameIndex));
                setPrefix(team, prefix);
                ChatColor nameColor = lastColor(prefix);
                if (nameColor != null) team.setColor(nameColor);
                setSuffix(team, parsed.substring(Math.min(parsed.length(), nameIndex + name.length())));
            } else {
                setPrefix(team, parsed);
                setSuffix(team, "");
            }
        }
        clearUnusedNametagTeams(board, index);
    }

    private String removeTrailingReset(String text) {
        if (text == null) return "";
        String reset = ChatColor.RESET.toString();
        while (text.endsWith(reset)) text = text.substring(0, text.length() - reset.length());
        return text;
    }

    private ChatColor lastColor(String text) {
        String colors = ChatColor.getLastColors(text);
        for (int i = colors.length() - 1; i >= 0; i--) {
            ChatColor color = ChatColor.getByChar(colors.charAt(i));
            if (color != null && color.isColor()) return color;
        }
        return null;
    }

    private void addScoreLine(Scoreboard board, Objective objective, String line, int score, int index) {
        String entry = uniqueBlank(index);
        Team team = ScoreboardTeamUtil.getOrCreate(board, "line" + index);
        if (team == null) return;
        if (!team.getEntries().contains(entry)) team.addEntry(entry);
        setPrefix(team, line.substring(0, Math.min(64, line.length())));
        setSuffix(team, line.length() > 64 ? line.substring(64, Math.min(128, line.length())) : "");
        if (objective.getScore(entry).getScore() != score) objective.getScore(entry).setScore(score);
    }

    private void clearUnusedLines(Scoreboard board, Objective objective, int usedLines) {
        for (int index = usedLines; index < MAX_LINES; index++) {
            String entry = uniqueBlank(index);
            board.resetScores(entry);
            Team team = board.getTeam("line" + index);
            if (team != null) {
                team.removeEntry(entry);
                team.unregister();
            }
        }
    }

    private void clearUnusedNametagTeams(Scoreboard board, int usedTeams) {
        for (int index = usedTeams; index < 80; index++) {
            Team team = board.getTeam("nt" + index);
            if (team == null) break;
            team.unregister();
        }
    }

    private void setPrefix(Team team, String prefix) {
        prefix = prefix == null ? "" : prefix;
        if (!team.getPrefix().equals(prefix)) team.setPrefix(prefix);
    }

    private void setSuffix(Team team, String suffix) {
        suffix = suffix == null ? "" : suffix;
        if (!team.getSuffix().equals(suffix)) team.setSuffix(suffix);
    }

    private String uniqueBlank(int index) {
        String colors = ChatColor.values()[index % ChatColor.values().length].toString();
        return colors + ChatColor.RESET;
    }
}
