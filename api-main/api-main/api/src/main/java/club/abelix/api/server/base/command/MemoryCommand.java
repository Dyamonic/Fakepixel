package club.abelix.api.server.base.command;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.time.Duration;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@WithAlias("mem")
@WithAlias("tps")
@NamedCommand("memory")
public class MemoryCommand {

    private static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory() / 1024L / 1024L;
    }

    private static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory() / 1024L / 1024L;
    }

    private static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory() / 1024L / 1024L;
    }

    private static void appendChunk(World world, StringBuilder message) {
        Chunk[] chunks = world.getLoadedChunks();
        message.append(" §7Chunks: ").append(chunks.length).append("\n");

        List<BlockState> tiles = new ArrayList<>();

        Stream.of(chunks)
                .map(Chunk::getTileEntities)
                .forEach(states -> Collections.addAll(tiles, states));

        message.append(" §7Tile entity (").append(tiles.size()).append("):\n");

        if (!tiles.isEmpty()) {
            TObjectIntMap<Material> map = new TObjectIntHashMap<>();
            for (BlockState state : tiles) {
                Material type = state.getType();

                map.put(type, map.get(type) + 1);
            }

            map.forEachEntry((key, value) -> {
                message.append("  ").append(key.name()).append(": ").append(value).append("\n");

                return true;
            });
        }
    }

    private static void appendEntity(World world, StringBuilder message) {
        List<Entity> entities = world.getEntities();
        message.append(" §7Entity (").append(entities.size()).append("):\n");
        if (!entities.isEmpty()) {
            TObjectIntMap<EntityType> map = new TObjectIntHashMap<>();

            for (Entity entity : entities) {
                EntityType type = entity.getType();

                map.put(type, map.get(type) + 1);
            }

            map.forEachEntry((key, value) -> {
                message.append("  ").append(key.name()).append(": ").append(value).append("\n");

                return true;
            });
        }
    }

    private static int getUptime() {
        return (int) (System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime());
    }

    public static String format(double tps) {
        return (tps > 18.0D ? ChatColor.GREEN : (tps > 16.0D ? ChatColor.YELLOW : ChatColor.RED)).toString() + (tps > 20.0D ? "*" : "") + Math.min((double) Math.round(tps * 100.0D) / 100.0D, 20.0D);
    }


    @Executor
    @WithPermission("command.tps")
    public static void executor(BukkitExecutionSession session) {
        StringBuilder message = new StringBuilder();

        Bukkit.getWorlds().forEach(world -> {
            message.append("§e").append(world.getName()).append(":\n");
            message.append(" §7Players: ").append(world.getPlayers().size()).append("\n");
            appendChunk(world, message);
            appendEntity(world, message);
        });

        message.append("§eTPS from last 1m, 5m, 15m: ");

        double[] tps = FalconAPI.getNmsApi().getTPS();

        message.append(format(tps[0])).append(", ");
        message.append(format(tps[1])).append(", ");
        message.append(format(tps[2])).append("\n");

        message.append("§eMax memory: §7").append(getMaxMemory()).append("MB\n");
        message.append("§eTotal memory: §7").append(getTotalMemory()).append("MB\n");
        message.append("§eFree memory: §7").append(getFreeMemory()).append("MB\n");
        message.append("§eUptime: §7").append(Duration.ruWordsMs(getUptime()));

        session.sendMessage(message.toString());
    }
}
