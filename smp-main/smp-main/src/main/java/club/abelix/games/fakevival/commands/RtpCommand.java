package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.Game;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RtpCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Set<Material> badBlocks = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final AtomicBoolean fallbackWarningLogged = new AtomicBoolean(false);

    public RtpCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        badBlocks.add(Material.LAVA);
        badBlocks.add(Material.WATER);
        badBlocks.add(Material.FIRE);
        badBlocks.add(Material.CACTUS);
        badBlocks.add(Material.MAGMA_BLOCK);
        badBlocks.add(Material.POWDER_SNOW);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("fakevival.command.rtp")) {
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.no-permission", "&cYou do not have permission.")));
            return true;
        }
        if (plugin instanceof Game game) {
            if (game.getNetworkService().isDungeon()) { player.sendMessage(CC.color("&cYou cannot use random teleport inside a dungeon.")); return true; }
            if (game.getNetworkService().isHub()) { game.getNetworkService().routeRtp(player); return true; }
            if (!game.getNetworkService().isOverworld()) { player.sendMessage(CC.color("&cRandom teleport is only available on overworld servers.")); return true; }
        }
        if (!player.hasPermission("fakevival.command.rtp.cooldown.bypass") && isCoolingDown(player)) return true;
        if (plugin instanceof Game game) {
            game.getNetworkService().findRtpLocation().thenAccept(saved -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                if (saved.isPresent()) teleport(player, saved.get());
                else {
                    if (fallbackWarningLogged.compareAndSet(false, true)) plugin.getLogger().warning("No RTP location set for "
                            + game.getNetworkService().getCurrentServerId() + "; using existing RTP fallback.");
                    teleport(player, null);
                }
            }));
            return true;
        }
        teleport(player, null);
        return true;
    }

    private void teleport(Player player, club.abelix.games.fakevival.network.SMPNetworkRepository.RtpLocation saved) {
        String spawnWorld = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("rtp.spawn-world", "spawn");
        String worldName = saved == null ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("rtp.world", player.getWorld().getName()) : saved.worldName();
        if (saved == null && player.getWorld().getName().equalsIgnoreCase(spawnWorld)) worldName = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("rtp.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (saved == null && !isAllowedWorld(world)) world = Bukkit.getWorld("world");
        if (!isAllowedWorld(world)) { player.sendMessage(CC.color("&cCould not find an overworld RTP world.")); return; }
        double centerX = saved == null ? 0.0D : saved.x();
        double centerZ = saved == null ? 0.0D : saved.z();
        int min = saved == null ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("rtp.min-radius", 500) : Math.max(0, saved.minRadius());
        int max = saved == null ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("rtp.max-radius", 5000) : saved.maxRadius();
        Location safe = findSafe(world, centerX, centerZ, min, Math.max(min + 1, max));
        if (safe == null) {
            player.sendMessage(CC.color("&cCould not find a safe RTP location. Try again."));
            return;
        }
        player.teleport(safe);
        player.setGameMode(GameMode.SURVIVAL);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.rtp-teleported", "&aTeleported to a random safe location.")));
    }

    private boolean isCoolingDown(Player player) {
        long cooldownMillis = Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("rtp.cooldown-seconds", 60L)) * 1000L;
        if (cooldownMillis == 0L) return false;
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = cooldownMillis - (System.currentTimeMillis() - last);
        if (remaining <= 0L) return false;
        long seconds = Math.max(1L, (remaining + 999L) / 1000L);
        player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.rtp-cooldown", "&cYou can use RTP again in %seconds%s.").replace("%seconds%", String.valueOf(seconds))));
        return true;
    }

    private Location findSafe(World world, double centerX, double centerZ, int min, int max) {
        int attempts = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("rtp.attempts", 80));
        for (int i = 0; i < attempts; i++) {
            int distance = min + random.nextInt(max - min + 1);
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int x = (int) Math.floor(centerX + Math.cos(angle) * distance);
            int z = (int) Math.floor(centerZ + Math.sin(angle) * distance);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) world.getChunkAt(x >> 4, z >> 4).load(true);
            int y = world.getHighestBlockYAt(x, z);
            Location location = new Location(world, x + 0.5, y + 1.0, z + 0.5);
            if (isSafe(location)) return location;
        }
        return null;
    }

    private boolean isAllowedWorld(World world) {
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) return false;
        String name = world.getName().replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
        return !name.equals("hub") && !name.equals("lobby") && !name.startsWith("dungeon_")
                && !name.startsWith("dun_") && !name.startsWith("smpdun_instance_");
    }

    private boolean isSafe(Location location) {
        Block feet = location.getBlock();
        Block head = location.clone().add(0, 1, 0).getBlock();
        Block ground = location.clone().subtract(0, 1, 0).getBlock();
        return feet.getType().isAir() && head.getType().isAir() && ground.getType().isSolid() && !badBlocks.contains(ground.getType());
    }
}
