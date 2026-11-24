package net.fakepixel.housing.scoreboard;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HousingScoreboard implements Listener {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<UUID, List<String>> lastLines = new HashMap<>();
    private final Map<UUID, String> lastTitles = new HashMap<>();
    private final Map<UUID, Long> lastUpdates = new HashMap<>();
    private final Map<UUID, String> lastContexts = new HashMap<>();
    private int taskId = -1;
    private final ScoreboardProvider lobbyProvider;
    private final ScoreboardProvider houseProvider = new HouseScoreboard();
    private final ScoreboardProvider guestProvider = new GuestScoreboard();

    public HousingScoreboard(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
        this.lobbyProvider = new LobbyScoreboard(plugin);
    }

    public void start() {
        if (!plugin.getStaticConfig().getBoolean("scoreboards.enabled", true)) return;
        Bukkit.getPluginManager().registerEvents(this,plugin);
        if(plugin.getCurrentServer()!=null&&plugin.getCurrentServer().isLobby())Bukkit.getPluginManager().registerEvents((LobbyScoreboard)lobbyProvider,plugin);
        taskId = new BukkitRunnable() {
            @Override public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) update(player);
            }
        }.runTaskTimer(plugin, 20L, 40L).getTaskId();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        lastLines.clear();
        lastTitles.clear();
        lastUpdates.clear();
        lastContexts.clear();
    }

    public void update(Player player) {
        if (!plugin.getStaticConfig().getBoolean("scoreboards.enabled", true)) { clear(player); return; }
        if (plugin.getCurrentServer()!=null && plugin.getCurrentServer().isInstance() && plugin.getClusterService()!=null && plugin.getClusterService().isLoadingHouse(player)) { clear(player); return; }
        if (plugin.getCurrentServer()!=null && plugin.getCurrentServer().isLobby()) {
            if (!plugin.getStaticConfig().getBoolean("scoreboards.lobby", true)) { clear(player); return; }
            ScoreboardView view=lobbyProvider.create(player,null);
            lastContexts.put(player.getUniqueId(),"lobby:housing");
            set(player,view.getTitle(),lobbyLines(player));
            return;
        }
        String world = player.getWorld().getName();
        House current = houses.getHouseByWorld(world);
        if (current != null) {
            String context = "house:" + current.getWorldName();
            boolean contextChanged = !context.equals(lastContexts.get(player.getUniqueId()));
            boolean owner = current.getOwner().equals(player.getUniqueId());
            boolean contextEnabled = plugin.getStaticConfig().getBoolean(owner ? "scoreboards.owner-house" : "scoreboards.guest-house", true);
            if (!contextEnabled || !current.isScoreboardEnabled()) {
                clear(player);
                lastContexts.put(player.getUniqueId(), context);
                return;
            }
            long now = System.currentTimeMillis();
            long interval = Math.max(10, current.getScoreboardUpdateTicks()) * 50L;
            Long last = lastUpdates.get(player.getUniqueId());
            if (!contextChanged && last != null && now - last < interval) return;
            lastUpdates.put(player.getUniqueId(), now);
            lastContexts.put(player.getUniqueId(), context);
            ScoreboardView view=(owner?houseProvider:guestProvider).create(player,current);
            set(player,current.isScoreboardPlaceholdersEnabled()?placeholders(player,current,view.getTitle()):view.getTitle(),current.isScoreboardPlaceholdersEnabled()?resolvedLines(player,current,view.getLines()):view.getLines());
            return;
        }
        clear(player);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event){if(plugin.getCurrentServer()!=null&&plugin.getCurrentServer().isLobby())return;Bukkit.getScheduler().runTaskLater(plugin,()->{if(event.getPlayer().isOnline())update(event.getPlayer());},10L);}
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent event){if(plugin.getCurrentServer()!=null&&plugin.getCurrentServer().isLobby())return;reapply(event.getPlayer());}
    @EventHandler public void onTeleport(PlayerTeleportEvent event){if(plugin.getCurrentServer()!=null&&plugin.getCurrentServer().isLobby())return;reapply(event.getPlayer());}
    @EventHandler public void onQuit(PlayerQuitEvent event){clear(event.getPlayer());}
    private void reapply(Player player){Bukkit.getScheduler().runTaskLater(plugin,()->{if(player.isOnline())update(player);},1L);}

    public void showHouseScoreboard(Player player, House house) {
        if (player == null || house == null || !player.isOnline()) return;
        if (!player.getWorld().getName().equals(house.getWorldName())) return;
        lastUpdates.remove(player.getUniqueId());
        lastContexts.remove(player.getUniqueId());
        update(player);
    }

    public void refreshHouse(House house) {
        if (house == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(house.getWorldName())) {
                lastUpdates.remove(player.getUniqueId());
                update(player);
            }
        }
    }

    private List<String> lobbyLines(Player player) {
        return ((LobbyScoreboard)lobbyProvider).lobbyLines(player);
    }

    private List<String> ownerLines(House house) {
        List<String> lines = new ArrayList<>();
        lines.add("&7" + new SimpleDateFormat("M/d/yy h:mm a").format(new Date()));
        lines.add("&1");
        lines.add("&fHouse name:");
        lines.add("&a" + shorten(house.getName()));
        lines.add("&2");
        lines.add("&fVisiting Rules:");
        lines.add(("public".equals(house.getVisitingRule()) ? "&a" : "&c") + house.getVisitingRuleDisplay());
        lines.add("&3");
        lines.add("&fGuests: &a" + houses.getGuests(house));
        lines.add("&4");
        lines.add("&fCookies: &a" + house.getCookies());
        lines.add("&5");
        lines.add("&e" + plugin.getStaticConfig().getString("settings.server-address", "mc.fakepixel.me"));
        return lines;
    }

    private List<String> guestLines(House house) {
        List<String> lines = new ArrayList<>();
        lines.add("&7" + new SimpleDateFormat("M/d/yy h:mm a").format(new Date()));
        lines.add("&1");
        lines.add("&fHouse name");
        lines.add("&a" + shorten(house.getName()));
        lines.add("&2");
        lines.add("&fHouse Owner");
        lines.add("&e" + shorten(house.getOwnerName()));
        lines.add("&3");
        lines.add("&fGuests: &a" + houses.getGuests(house));
        lines.add("&4");
        lines.add("&fCookies: &a" + house.getCookies());
        lines.add("&5");
        lines.add("&e" + plugin.getStaticConfig().getString("settings.server-address", "mc.fakepixel.me"));
        return lines;
    }

    private List<String> houseLines(Player player, House house) {
        if (house == null) return new ArrayList<>();
        List<String> savedLines = new ArrayList<>(house.getScoreboardLines());
        if (!house.isScoreboardPlaceholdersEnabled()) return savedLines;
        List<String> lines = new ArrayList<>();
        for (String line : savedLines) lines.add(placeholders(player, house, line));
        return lines;
    }
    private List<String> resolvedLines(Player player,House house,List<String> raw){List<String> lines=new ArrayList<>();for(String line:raw)lines.add(placeholders(player,house,line));return lines;}

    private String placeholders(Player player, House house, String raw) {
        String out = raw == null ? "" : raw;
        out = out.replace("%date%", new SimpleDateFormat("M/d/yy h:mm a").format(new Date()));
        out = out.replace("%player%", player.getName());
        out = out.replace("%house_name%", house.getName());
        out = out.replace("%housing_rules%", housingRules(house));
        out = out.replace("%house_owner%", house.getOwnerName());
        out = out.replace("%owner%", house.getOwnerName());
        out = out.replace("%guests%", String.valueOf(houses.getGuests(house)));
        out = out.replace("%cookies%", String.valueOf(house.getCookies()));
        out = out.replace("%weekly_cookies%", String.valueOf(house.getWeeklyCookies()));
        out = out.replace("%housing_group%", house.groupSuffix(house.getGroup(player.getUniqueId())));
        out = out.replace("%housing_mode%", plugin.houseModeName(house));
        out = out.replace("%theme%", house.getTheme());
        out = out.replace("%plot_size%", house.getPlotSize() + "x" + house.getPlotSize());
        out = out.replace("%parkour_time%", parkourTime(player, house));
        out = out.replace("%parkour_best%", parkourBest(player, house));
        try { Class<?> papi=Class.forName("me.clip.placeholderapi.PlaceholderAPI"); Object value=papi.getMethod("setPlaceholders",org.bukkit.OfflinePlayer.class,String.class).invoke(null,player,out); if(value instanceof String)out=(String)value; } catch(Throwable ignored) {}
        return out.replaceAll("%[^%]+%", "None");
    }

    private String housingRules(House house) {
        String rule = house == null ? "public" : house.getVisitingRule();
        if ("private".equals(rule)) return "&cPrivate";
        if ("friends".equals(rule)) return "&eFriends Only";
        if ("guild".equals(rule)) return "&bGuild Only";
        if ("party".equals(rule)) return "&dParty Only";
        return "&aPublic";
    }

    private String parkourTime(Player player, House house) {
        if (plugin.getParkourService() == null || !plugin.getParkourService().isRunning(player, house)) return "--:--.---";
        return "Running";
    }

    private String parkourBest(Player player, House house) {
        Long best = house.getParkourBestTime(player.getUniqueId());
        return best == null || best <= 0L ? "--:--.---" : formatTime(best);
    }

    private String formatTime(long millis) {
        long minutes = millis / 60000L;
        long seconds = (millis % 60000L) / 1000L;
        long milliseconds = millis % 1000L;
        return (minutes < 10 ? "0" : "") + minutes + ":"
                + (seconds < 10 ? "0" : "") + seconds + "."
                + (milliseconds < 100 ? "0" : "") + (milliseconds < 10 ? "0" : "") + milliseconds;
    }

    private void set(Player player, String title, List<String> lines) {
        List<String> rendered = render(lines);
        String renderedTitle = plugin.color(title == null || title.trim().isEmpty() ? "&b&lHOUSING" : title);
        if (renderedTitle.length() > 32) renderedTitle = renderedTitle.substring(0, 32);
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            if (plugin.getNametagService() != null) plugin.getNametagService().apply(board);
            player.setScoreboard(board);
        }

        Objective obj = board.getObjective("housing");
        if (rendered.equals(lastLines.get(player.getUniqueId()))
                && renderedTitle.equals(lastTitles.get(player.getUniqueId()))
                && obj != null && obj.getDisplaySlot() == DisplaySlot.SIDEBAR) return;

        if (obj == null) {
            obj = board.registerNewObjective("housing", "dummy");
        }
        if (obj.getDisplaySlot() != DisplaySlot.SIDEBAR) {
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }
        obj.setDisplayName(renderedTitle);

        List<String> previous = lastLines.get(player.getUniqueId());
        if (previous != null) {
            for (String oldLine : previous) {
                if (!rendered.contains(oldLine)) board.resetScores(oldLine);
            }
        }

        int score = rendered.size();
        for (String line : rendered) obj.getScore(line).setScore(score--);

        if (plugin.getNametagService() != null) plugin.getNametagService().apply(board);
        lastLines.put(player.getUniqueId(), rendered);
        lastTitles.put(player.getUniqueId(), renderedTitle);
    }

    private List<String> render(List<String> lines) {
        List<String> rendered = new ArrayList<>();
        int blank = 0;
        Map<String, Integer> seen = new HashMap<>();
        for (String raw : lines) {
            String line = plugin.color(raw);
            if (ChatColor.stripColor(line).trim().isEmpty()) line = ChatColor.values()[blank++].toString();
            int duplicate = seen.getOrDefault(line, 0);
            seen.put(line, duplicate + 1);
            if (duplicate > 0) line = unique(line, duplicate);
            if (line.length() > 40) line = line.substring(0, 40);
            rendered.add(line);
            if (rendered.size() >= 15) break;
        }
        return rendered;
    }

    private String unique(String line, int duplicate) {
        ChatColor[] colors = ChatColor.values();
        String suffix = colors[duplicate % colors.length].toString() + colors[(duplicate / colors.length) % colors.length].toString();
        return line.length() + suffix.length() > 40 ? line.substring(0, 40 - suffix.length()) + suffix : line + suffix;
    }

    private void clear(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("housing");
        if (obj != null) obj.unregister();
        lastLines.remove(player.getUniqueId());
        lastTitles.remove(player.getUniqueId());
        lastUpdates.remove(player.getUniqueId());
        lastContexts.remove(player.getUniqueId());
    }

    private String shorten(String text) {
        if (text == null) return "";
        return text.length() <= 28 ? text : text.substring(0, 28);
    }
}
