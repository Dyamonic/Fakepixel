package net.fakepixel.housing.parkour;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.hook.AbelixHook;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ParkourService implements Listener, CommandExecutor, TabCompleter {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<UUID, ParkourRun> runs = new HashMap<>();
    private final Map<UUID, String> lastPad = new HashMap<>();

    public ParkourService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateTimers, 2L, 2L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (sameBlock(event.getFrom(), event.getTo())) return;
        Player player = event.getPlayer();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) return;
        String pad = padAt(house, event.getTo());
        if (pad == null) {
            lastPad.remove(player.getUniqueId());
            return;
        }
        String marker = house.getWorldName() + ":" + locationKey(event.getTo()) + ":" + pad;
        if (marker.equals(lastPad.get(player.getUniqueId()))) return;
        lastPad.put(player.getUniqueId(), marker);
        if ("start".equals(pad)) start(player, house);
        else if ("checkpoint".equals(pad)) checkpoint(player, house, event.getTo());
        else if ("finish".equals(pad)) finish(player, house);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        endRun(event.getPlayer(), false);
        lastPad.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        ParkourRun run = runs.get(event.getPlayer().getUniqueId());
        if (run == null) return;
        House house = houses.getHouseByWorld(event.getPlayer().getWorld().getName());
        if (house == null || !run.worldName.equals(house.getWorldName())) endRun(event.getPlayer(), true);
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!runs.containsKey(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        event.getPlayer().setFlying(false);
        event.getPlayer().setAllowFlight(false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player player = (Player) sender;
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) {
            player.sendMessage(plugin.color("&cYou are not inside a player house."));
            return true;
        }
        String sub = args.length == 0 ? "leaderboard" : args[0].toLowerCase();
        if ("reset".equals(sub) || "checkpoint".equals(sub)) {
            reset(player, house);
            return true;
        }
        if ("leave".equals(sub)) {
            if (!isRunning(player, house)) {
                player.sendMessage(plugin.color("&cYou are not currently running parkour."));
                return true;
            }
            endRun(player, true);
            player.sendMessage(plugin.color("&cYou left the parkour."));
            return true;
        }
        if ("leaderboard".equals(sub) || "lb".equals(sub)) {
            leaderboard(player, house);
            return true;
        }
        player.sendMessage(plugin.color("&cUsage: /parkour <checkpoint|reset|leave|leaderboard>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return new ArrayList<>();
        List<String> values = Arrays.asList("checkpoint", "reset", "leave", "leaderboard");
        String prefix = args[0].toLowerCase();
        List<String> out = new ArrayList<>();
        for (String value : values) if (value.startsWith(prefix)) out.add(value);
        return out;
    }

    public void start(Player player, House house) {
        ParkourRun previous = runs.get(player.getUniqueId());
        GameMode previousMode = previous == null ? player.getGameMode() : previous.previousMode;
        boolean previousAllowFlight = previous == null ? player.getAllowFlight() : previous.previousAllowFlight;
        boolean previousFlying = previous == null ? player.isFlying() : previous.previousFlying;
        ParkourRun run = new ParkourRun(house.getWorldName(), System.currentTimeMillis(), previousMode, previousAllowFlight, previousFlying);
        runs.put(player.getUniqueId(), run);
        player.setFlying(false);
        player.setAllowFlight(false);
        if (player.getGameMode() != GameMode.ADVENTURE) player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage(plugin.color("&aParkour started!"));
        runParkourFunction(player, house, "parkour_start");
    }

    private void checkpoint(Player player, House house, Location location) {
        ParkourRun run = runs.get(player.getUniqueId());
        if (run == null || !run.worldName.equals(house.getWorldName())) {
            player.sendMessage(plugin.color("&cStep on the Parkour Start pad first."));
            return;
        }
        run.checkpoint = blockString(location);
        player.sendMessage(plugin.color("&eCheckpoint reached!"));
        runParkourFunction(player, house, "parkour_checkpoint");
    }

    public void checkpointAction(Player player, House house, Location location) {
        checkpoint(player, house, location);
    }

    private void finish(Player player, House house) {
        ParkourRun run = runs.get(player.getUniqueId());
        if (run == null || !run.worldName.equals(house.getWorldName())) {
            player.sendMessage(plugin.color("&cStep on the Parkour Start pad first."));
            return;
        }
        long time = Math.max(1L, System.currentTimeMillis() - run.startedAt);
        boolean personalBest = house.setParkourBestTime(player.getUniqueId(), player.getName(), time);
        houses.saveAll();
        if (personalBest) refreshLeaderboard(house);
        endRun(player, true);
        player.sendMessage(plugin.color("&aParkour finished in &e" + formatTime(time) + "&a!"));
        if (personalBest) player.sendMessage(plugin.color("&6New personal best!"));
        runParkourFunction(player, house, "parkour_finish");
    }

    public void reset(Player player, House house) {
        ParkourRun run = runs.get(player.getUniqueId());
        if (run == null || !run.worldName.equals(house.getWorldName())) {
            player.sendMessage(plugin.color("&cYou are not currently running parkour."));
            return;
        }
        String target = run == null ? null : run.checkpoint;
        if (target == null) target = house.getParkourStart();
        Location location = parse(house, target);
        if (location == null) {
            player.sendMessage(plugin.color("&cThis house does not have a Parkour Start pad."));
            return;
        }
        player.teleport(location.add(0.5D, 0.2D, 0.5D));
        player.sendMessage(plugin.color("&aTeleported to your parkour checkpoint."));
        runParkourFunction(player, house, "parkour_reset");
    }

    public boolean isRunning(Player player, House house) {
        ParkourRun run = runs.get(player.getUniqueId());
        return run != null && house != null && run.worldName.equals(house.getWorldName());
    }

    public Location deathRespawnLocation(Player player, House house) {
        if (player == null || house == null) return null;
        ParkourRun run = runs.get(player.getUniqueId());
        if (run == null || !run.worldName.equals(house.getWorldName())) return null;
        String target = run.checkpoint == null ? house.getParkourStart() : run.checkpoint;
        Location location = parse(house, target);
        if (location != null) return location.add(0.5D, 0.2D, 0.5D);
        return houses.spawnLocation(house, Bukkit.getWorld(house.getWorldName()), true);
    }

    private void runParkourFunction(Player player, House house, String functionId) {
        if (plugin.getFunctionService() != null && house != null && house.getFunction(functionId) != null) {
            plugin.getFunctionService().runFunction(player, house, functionId);
        }
    }

    private void endRun(Player player, boolean restore) {
        ParkourRun run = runs.remove(player.getUniqueId());
        if (run == null) return;
        if (restore && player.isOnline()) restorePlayerState(player, run);
    }

    private void restorePlayerState(Player player, ParkourRun run) {
        if (run.previousMode != null && player.getGameMode() == GameMode.ADVENTURE) {
            player.setGameMode(run.previousMode);
        }
        if (run.previousAllowFlight) {
            player.setAllowFlight(true);
            player.setFlying(run.previousFlying);
        } else {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        houses.ensureHouseMenuItem(player);
    }

    private void updateTimers() {
        if (runs.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, ParkourRun> entry : new ArrayList<>(runs.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) continue;
            ParkourRun run = entry.getValue();
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null || !run.worldName.equals(house.getWorldName())) {
                endRun(player, true);
                continue;
            }
            if (player.getAllowFlight() || player.isFlying()) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            sendActionBar(player, "&aParkour Time: &e" + formatTime(Math.max(1L, now - run.startedAt)));
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Class<?> chatComponent = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Class<?> packetSuper = Class.forName("net.minecraft.server." + version + ".Packet");
            Object component = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + jsonEscape(plugin.color(message)) + "\"}");
            Object packet = packetClass.getConstructor(chatComponent, byte.class).newInstance(component, (byte) 2);
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = handle.getClass().getField("playerConnection").get(handle);
            connection.getClass().getMethod("sendPacket", packetSuper).invoke(connection, packet);
        } catch (Throwable ignored) {
        }
    }

    private String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void leaderboard(Player player, House house) {
        List<String> lines = leaderboardLines(house, 10);
        if (lines.size() <= 1) {
            player.sendMessage(plugin.color("&e&lParkour Leaderboard"));
            player.sendMessage(plugin.color("&7No completions yet."));
            return;
        }
        for (String line : lines) player.sendMessage(plugin.color(line));
    }

    public List<String> leaderboardLines(House house, int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("&b&lParkour Leaderboard");
        if (house == null) return lines;
        List<Map.Entry<UUID, Long>> entries = new ArrayList<>(house.getParkourBestTimes().entrySet());
        entries.sort(Comparator.comparingLong(Map.Entry::getValue));
        if (entries.isEmpty()) {
            lines.add("&7No times recorded yet.");
            return lines;
        }
        int rank = 1;
        int max = Math.max(1, limit);
        for (Map.Entry<UUID, Long> entry : entries) {
            if (rank > max) break;
            lines.add(rankColor(rank) + rank + ". " + leaderboardName(house, entry.getKey()) + " &7- &a" + formatTime(entry.getValue()));
            rank++;
        }
        return lines;
    }

    public void placeLeaderboard(Player player, House house, Location location) {
        if (player == null || house == null || location == null || plugin.getHologramService() == null) return;
        removeLeaderboard(house, false);
        House.Hologram hologram = house.addHologram(location, leaderboardLines(house, 10));
        if (hologram == null) return;
        hologram.setIdentity("PARKOUR_LEADERBOARD", "leaderboard");
        house.setParkourLeaderboardHologramId(hologram.getId());
        plugin.getHologramService().spawnHologram(house, hologram, location.getWorld());
        houses.saveAll();
        player.sendMessage(plugin.color("&aPlaced the &eParkour Leaderboard&a."));
    }

    public void refreshLeaderboard(House house) {
        if (house == null || plugin.getHologramService() == null) return;
        String hologramId = house.getParkourLeaderboardHologramId();
        if (hologramId == null || hologramId.trim().isEmpty()) return;
        House.Hologram hologram = house.getHologram(hologramId);
        if (hologram == null) {
            house.clearParkourLeaderboardHologramId();
            houses.saveAll();
            return;
        }
        hologram.setLines(leaderboardLines(house, 10));
        World world = Bukkit.getWorld(house.getWorldName());
        if (world != null) plugin.getHologramService().spawnHologram(house, hologram, world);
        houses.saveAll();
    }

    public void removeLeaderboard(House house, boolean save) {
        if (house == null) return;
        String hologramId = house.getParkourLeaderboardHologramId();
        if (hologramId != null && !hologramId.trim().isEmpty()) {
            if (plugin.getHologramService() != null) plugin.getHologramService().deleteHologram(house, hologramId);
            else house.removeHologram(hologramId);
        }
        house.clearParkourLeaderboardHologramId();
        if (save) houses.saveAll();
    }

    private String leaderboardName(House house, UUID uuid) {
        String savedName = house.getParkourBestName(uuid);
        if (savedName == null || savedName.trim().isEmpty()) {
            savedName = Bukkit.getOfflinePlayer(uuid).getName();
        }
        if (savedName == null || savedName.trim().isEmpty()) {
            savedName = uuid.toString().substring(0, 8);
        }
        AbelixHook abelix = plugin.getAbelixHook();
        if (abelix == null || !abelix.isEnabled()) return "&f" + savedName;
        String prefix = abelix.getPrefix(uuid, savedName);
        String nameColor = abelix.getNameColor(uuid, savedName);
        StringBuilder display = new StringBuilder();
        if (prefix != null && !prefix.trim().isEmpty()) display.append(prefix).append(" ");
        display.append(nameColor == null || nameColor.trim().isEmpty() ? "&f" : nameColor).append(savedName);
        return display.toString();
    }

    private String rankColor(int rank) {
        if (rank == 1) return "&6";
        if (rank == 2) return "&e";
        if (rank == 3) return "&c";
        return "&7";
    }

    private String padAt(House house, Location location) {
        String type = house.parkourPadTypeAt(location);
        if (type != null) return type;
        Location below = location.clone().subtract(0, 1, 0);
        return house.parkourPadTypeAt(below);
    }

    private Location parse(House house, String value) {
        if (house == null || value == null || value.trim().isEmpty()) return null;
        World world = Bukkit.getWorld(house.getWorldName());
        if (world == null) return null;
        String[] split = value.split(",");
        if (split.length != 3) return null;
        try {
            return new Location(world, Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return false;
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    private String locationKey(Location location) {
        String at = blockString(location);
        String below = blockString(location.clone().subtract(0, 1, 0));
        return at + "|" + below;
    }

    private String blockString(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private String formatTime(long millis) {
        long minutes = millis / 60000L;
        long seconds = (millis % 60000L) / 1000L;
        long milliseconds = millis % 1000L;
        return (minutes < 10 ? "0" : "") + minutes + ":"
                + (seconds < 10 ? "0" : "") + seconds + "."
                + (milliseconds < 100 ? "0" : "") + (milliseconds < 10 ? "0" : "") + milliseconds;
    }

    private static class ParkourRun {
        private final String worldName;
        private final long startedAt;
        private final GameMode previousMode;
        private final boolean previousAllowFlight;
        private final boolean previousFlying;
        private String checkpoint;

        private ParkourRun(String worldName, long startedAt, GameMode previousMode, boolean previousAllowFlight, boolean previousFlying) {
            this.worldName = worldName;
            this.startedAt = startedAt;
            this.previousMode = previousMode;
            this.previousAllowFlight = previousAllowFlight;
            this.previousFlying = previousFlying;
        }
    }
}
