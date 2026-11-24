package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ChunkPregeneratorManager {
    private static final String ROOT = "pregeneration.tasks";
    private static final int DEFAULT_MAX_RADIUS = 10_000;
    private static final int ABSOLUTE_MAX_RADIUS = 100_000;
    private static final int BASE_CHUNKS_PER_TICK = 2;
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private int tickTask = -1;
    private long ticks;

    public ChunkPregeneratorManager(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        load();
    }

    public void start() {
        if (tickTask != -1) return;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L).getTaskId();
    }

    public void stop() {
        if (tickTask != -1) Bukkit.getScheduler().cancelTask(tickTask);
        tickTask = -1;
        tasks.values().stream().filter(task -> task.status == Status.RUNNING).forEach(task -> task.status = Status.PAUSED);
        saveNow();
    }

    public boolean handle(CommandSender sender, String[] args, int offset) {
        if (!sender.hasPermission("fakevival.pregenerate.admin")) {
            sender.sendMessage(CC.color("&cYou do not have permission to use this command."));
            return true;
        }
        if (args.length <= offset) return usage(sender);
        String action = args[offset].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            if (tasks.isEmpty()) sender.sendMessage(CC.color("&7No pregeneration tasks are saved."));
            else tasks.values().forEach(task -> sender.sendMessage(CC.color(statusLine(task))));
            return true;
        }
        if (args.length <= offset + 1) return usage(sender);
        String worldName = args[offset + 1];
        if (action.equals("start")) return startCommand(sender, args, offset, worldName);
        Task task = tasks.get(key(worldName));
        if (task == null) {
            sender.sendMessage(CC.color("&cNo pregeneration task exists for that world."));
            return true;
        }
        switch (action) {
            case "pause" -> {
                if (task.status == Status.RUNNING) task.status = Status.PAUSED;
                save();
                sender.sendMessage(CC.color("&ePaused pregeneration for &f" + task.worldName + "&e."));
            }
            case "continue", "resume" -> {
                if (Bukkit.getWorld(task.worldName) == null) {
                    sender.sendMessage(CC.color("&cThat world is not loaded."));
                    return true;
                }
                if (task.status == Status.COMPLETE || task.status == Status.CANCELLED) {
                    sender.sendMessage(CC.color("&cOnly a paused pregeneration task can be continued."));
                    return true;
                }
                task.status = Status.RUNNING;
                task.lastSampleAt = System.currentTimeMillis();
                task.lastSampleProcessed = task.processed;
                save();
                sender.sendMessage(CC.color("&aContinued pregeneration for &f" + task.worldName + "&a."));
            }
            case "cancel" -> {
                task.status = Status.CANCELLED;
                task.updatedAt = System.currentTimeMillis();
                save();
                sender.sendMessage(CC.color("&cCancelled pregeneration for &f" + task.worldName + "&c. Generated chunks were retained."));
            }
            case "status" -> sender.sendMessage(CC.color(statusLine(task)));
            default -> { return usage(sender); }
        }
        return true;
    }

    private boolean startCommand(CommandSender sender, String[] args, int offset, String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(CC.color("&cThat world does not exist or is not loaded."));
            return true;
        }
        int remaining = args.length - (offset + 2);
        if (remaining != 1 && remaining != 3) return usage(sender);
        int centerX = world.getSpawnLocation().getBlockX();
        int centerZ = world.getSpawnLocation().getBlockZ();
        int radius;
        try {
            if (remaining == 1) radius = Integer.parseInt(args[offset + 2]);
            else {
                centerX = Integer.parseInt(args[offset + 2]);
                centerZ = Integer.parseInt(args[offset + 3]);
                radius = Integer.parseInt(args[offset + 4]);
            }
        } catch (NumberFormatException exception) {
            sender.sendMessage(CC.color("&cCenter coordinates and radius must be whole numbers."));
            return true;
        }
        int max = sender.hasPermission("fakevival.pregenerate.unlimited") ? ABSOLUTE_MAX_RADIUS : DEFAULT_MAX_RADIUS;
        if (radius < 16 || radius > max) {
            sender.sendMessage(CC.color("&cRadius must be between 16 and " + max + " blocks."));
            return true;
        }
        Task existing = tasks.get(key(worldName));
        if (existing != null && (existing.status == Status.RUNNING || existing.status == Status.PAUSED)) {
            sender.sendMessage(CC.color("&cThat world already has an active or paused pregeneration task. Cancel it first."));
            return true;
        }
        int chunkRadius = Math.max(1, (int) Math.ceil(radius / 16.0D));
        long width = 2L * chunkRadius + 1L;
        long total = width * width;
        Task task = new Task(world.getName(), centerX, centerZ, Math.floorDiv(centerX, 16), Math.floorDiv(centerZ, 16),
                radius, chunkRadius, total, sender instanceof Player player ? player.getUniqueId() : null, sender.getName());
        tasks.put(key(worldName), task);
        save();
        if (radius >= 5_000) sender.sendMessage(CC.color("&eWarning: this is a large pregeneration job and will perform substantial disk I/O."));
        sender.sendMessage(CC.color("&aStarted pregeneration for &f" + world.getName() + " &awith &e" + total + " &achunks."));
        if (!Bukkit.getOnlinePlayers().isEmpty()) sender.sendMessage(CC.color("&ePregeneration is running while players are online. TPS throttling is enabled."));
        return true;
    }

    private void tick() {
        ticks++;
        double tps = currentTps();
        int budget = tps < 17.0D ? 1 : tps < 18.5D ? 1 : BASE_CHUNKS_PER_TICK;
        if (!Bukkit.getOnlinePlayers().isEmpty()) budget = 1;
        for (Task task : new ArrayList<>(tasks.values())) {
            if (task.status != Status.RUNNING) continue;
            World world = Bukkit.getWorld(task.worldName);
            if (world == null) {
                task.status = Status.PAUSED;
                continue;
            }
            for (int count = 0; count < budget && task.status == Status.RUNNING; count++) processOne(world, task);
            if (ticks % 100L == 0L) sampleAndReport(task);
        }
        if (ticks % 600L == 0L) save();
    }

    private void processOne(World world, Task task) {
        if (task.processed >= task.total) {
            complete(task);
            return;
        }
        int chunkX = task.centerChunkX + task.relativeX;
        int chunkZ = task.centerChunkZ + task.relativeZ;
        boolean alreadyLoaded = world.isChunkLoaded(chunkX, chunkZ);
        try {
            world.getChunkAt(chunkX, chunkZ, true);
            task.processed++;
            task.updatedAt = System.currentTimeMillis();
            advance(task);
            if (!alreadyLoaded && canUnload(world, chunkX, chunkZ)) world.unloadChunkRequest(chunkX, chunkZ);
            if (task.processed >= task.total) complete(task);
        } catch (Throwable throwable) {
            task.status = Status.PAUSED;
            plugin.getLogger().warning("Paused pregeneration for '" + task.worldName + "' after chunk " + chunkX + "," + chunkZ + ": " + throwable.getMessage());
            save();
        }
    }

    private void advance(Task task) {
        int x = task.relativeX;
        int z = task.relativeZ;
        if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
            int oldDx = task.directionX;
            task.directionX = -task.directionZ;
            task.directionZ = oldDx;
        }
        task.relativeX += task.directionX;
        task.relativeZ += task.directionZ;
    }

    private boolean canUnload(World world, int chunkX, int chunkZ) {
        if (world.isChunkForceLoaded(chunkX, chunkZ)) return false;
        Chunk spawn = world.getSpawnLocation().getChunk();
        if (Math.abs(spawn.getX() - chunkX) <= 2 && Math.abs(spawn.getZ() - chunkZ) <= 2) return false;
        for (Player player : world.getPlayers()) {
            if (Math.abs(player.getLocation().getChunk().getX() - chunkX) <= 2
                    && Math.abs(player.getLocation().getChunk().getZ() - chunkZ) <= 2) return false;
        }
        return true;
    }

    private void complete(Task task) {
        task.status = Status.COMPLETE;
        task.processed = task.total;
        task.updatedAt = System.currentTimeMillis();
        save();
        broadcast("&aPregeneration complete for &f" + task.worldName + "&a: &e" + task.total + " &achunks processed.");
    }

    private void sampleAndReport(Task task) {
        long now = System.currentTimeMillis();
        long elapsed = Math.max(1L, now - task.lastSampleAt);
        task.chunksPerSecond = (task.processed - task.lastSampleProcessed) * 1000.0D / elapsed;
        task.lastSampleAt = now;
        task.lastSampleProcessed = task.processed;
        broadcast(statusLine(task));
    }

    private void broadcast(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("fakevival.pregenerate.admin")) player.sendMessage(CC.color(message));
        }
    }

    private String statusLine(Task task) {
        double percent = task.total <= 0 ? 100.0D : Math.clamp(task.processed * 100.0D / task.total, 0.0D, 100.0D);
        long remaining = Math.max(0L, task.total - task.processed);
        long etaMillis = task.chunksPerSecond <= 0.01D ? -1L : (long) (remaining / task.chunksPerSecond * 1000.0D);
        return "&aPregen &eWorld: " + task.worldName + " &7| &f" + task.status + " &7| &b"
                + String.format(Locale.US, "%.1f", percent) + "% &7| &e" + task.processed + "/" + task.total
                + " chunks &7| &a" + String.format(Locale.US, "%.1f", task.chunksPerSecond) + " cps &7| ETA &f"
                + (etaMillis < 0 ? "--" : formatDuration(etaMillis)) + " &7| Elapsed &f" + formatDuration(System.currentTimeMillis() - task.startedAt)
                + " &7| Center &f" + task.centerX + "," + task.centerZ + " &7| Radius &f" + task.radiusBlocks;
    }

    private String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainder = seconds % 60L;
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + remainder + "s";
        return remainder + "s";
    }

    private double currentTps() {
        try { return plugin.getServer().getTPS()[0]; }
        catch (Throwable ignored) { return 20.0D; }
    }

    private void load() {
        ConfigurationSection section = dataManager.getData().getConfigurationSection(ROOT);
        if (section == null) return;
        for (String storageKey : section.getKeys(false)) {
            String path = ROOT + "." + storageKey;
            String world = dataManager.getData().getString(path + ".world", storageKey);
            UUID startedBy = parseUuid(dataManager.getData().getString(path + ".started-by-uuid", ""));
            Task task = new Task(world,
                    dataManager.getData().getInt(path + ".center-x"), dataManager.getData().getInt(path + ".center-z"),
                    dataManager.getData().getInt(path + ".center-chunk-x"), dataManager.getData().getInt(path + ".center-chunk-z"),
                    dataManager.getData().getInt(path + ".radius-blocks"), dataManager.getData().getInt(path + ".chunk-radius"),
                    dataManager.getData().getLong(path + ".total"), startedBy, dataManager.getData().getString(path + ".started-by", "unknown"));
            task.processed = Math.max(0L, dataManager.getData().getLong(path + ".processed"));
            task.relativeX = dataManager.getData().getInt(path + ".relative-x");
            task.relativeZ = dataManager.getData().getInt(path + ".relative-z");
            task.directionX = dataManager.getData().getInt(path + ".direction-x", 0);
            task.directionZ = dataManager.getData().getInt(path + ".direction-z", -1);
            task.startedAt = dataManager.getData().getLong(path + ".started-at", System.currentTimeMillis());
            task.updatedAt = dataManager.getData().getLong(path + ".updated-at", task.startedAt);
            try { task.status = Status.valueOf(dataManager.getData().getString(path + ".status", "PAUSED")); }
            catch (IllegalArgumentException ignored) { task.status = Status.PAUSED; }
            if (task.status == Status.RUNNING) task.status = Status.PAUSED;
            tasks.put(key(world), task);
        }
        save();
    }

    private void save() {
        writeSnapshot();
        dataManager.save();
    }

    private void saveNow() {
        writeSnapshot();
        dataManager.saveNow();
    }

    private void writeSnapshot() {
        dataManager.getData().set(ROOT, null);
        for (Task task : tasks.values()) {
            String path = ROOT + "." + storageKey(task.worldName);
            dataManager.getData().set(path + ".world", task.worldName);
            dataManager.getData().set(path + ".shape", "SQUARE");
            dataManager.getData().set(path + ".center-x", task.centerX);
            dataManager.getData().set(path + ".center-z", task.centerZ);
            dataManager.getData().set(path + ".center-chunk-x", task.centerChunkX);
            dataManager.getData().set(path + ".center-chunk-z", task.centerChunkZ);
            dataManager.getData().set(path + ".radius-blocks", task.radiusBlocks);
            dataManager.getData().set(path + ".chunk-radius", task.chunkRadius);
            dataManager.getData().set(path + ".total", task.total);
            dataManager.getData().set(path + ".processed", task.processed);
            dataManager.getData().set(path + ".relative-x", task.relativeX);
            dataManager.getData().set(path + ".relative-z", task.relativeZ);
            dataManager.getData().set(path + ".direction-x", task.directionX);
            dataManager.getData().set(path + ".direction-z", task.directionZ);
            dataManager.getData().set(path + ".status", task.status.name());
            dataManager.getData().set(path + ".started-at", task.startedAt);
            dataManager.getData().set(path + ".updated-at", task.updatedAt);
            dataManager.getData().set(path + ".started-by", task.startedByName);
            dataManager.getData().set(path + ".started-by-uuid", task.startedByUuid == null ? "" : task.startedByUuid.toString());
        }
    }

    private boolean usage(CommandSender sender) {
        sender.sendMessage(CC.color("&cUsage: /smp pregenerate <start <world> [centerX centerZ] <radius>|pause <world>|continue <world>|cancel <world>|status <world>|list>"));
        return true;
    }

    private String key(String world) { return world == null ? "" : world.toLowerCase(Locale.ROOT); }
    private String storageKey(String world) { return key(world).replace('.', '_').replace(' ', '_'); }
    private UUID parseUuid(String value) {
        try { return value == null || value.isBlank() ? null : UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private enum Status { RUNNING, PAUSED, CANCELLED, COMPLETE }

    private static final class Task {
        private final String worldName;
        private final int centerX;
        private final int centerZ;
        private final int centerChunkX;
        private final int centerChunkZ;
        private final int radiusBlocks;
        private final int chunkRadius;
        private final long total;
        private final UUID startedByUuid;
        private final String startedByName;
        private long processed;
        private int relativeX;
        private int relativeZ;
        private int directionX;
        private int directionZ = -1;
        private Status status = Status.RUNNING;
        private long startedAt = System.currentTimeMillis();
        private long updatedAt = startedAt;
        private long lastSampleAt = startedAt;
        private long lastSampleProcessed;
        private double chunksPerSecond;

        private Task(String worldName, int centerX, int centerZ, int centerChunkX, int centerChunkZ,
                     int radiusBlocks, int chunkRadius, long total, UUID startedByUuid, String startedByName) {
            this.worldName = worldName;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.centerChunkX = centerChunkX;
            this.centerChunkZ = centerChunkZ;
            this.radiusBlocks = radiusBlocks;
            this.chunkRadius = chunkRadius;
            this.total = total;
            this.startedByUuid = startedByUuid;
            this.startedByName = startedByName;
        }
    }
}
