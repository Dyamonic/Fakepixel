package club.abelix.games.buildffa.game.task;

import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Map;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockTracker implements Runnable {
    @Getter(AccessLevel.NONE)
    final Map<Block, TrackedBlock> blocks = Maps.newConcurrentMap();
    volatile double decayMultiplier = 1.0D;

    public void track(Block block) {
        blocks.put(block, new TrackedBlock(System.currentTimeMillis(), block.getType(), block.getData()));
    }

    public boolean isTracked(Block block) {
        return blocks.containsKey(block);
    }

    public void untrack(Block block) {
        blocks.remove(block);
    }

    public SuspendedBlock suspend(Block block) {
        requireMainThread();
        TrackedBlock tracked = blocks.remove(block);
        return tracked != null && tracked.matches(block) ? new SuspendedBlock(tracked, System.currentTimeMillis()) : null;
    }

    public void resume(Block block, SuspendedBlock suspended) {
        requireMainThread();
        if (suspended == null || !suspended.tracked.matches(block)) return;
        TrackedBlock resumed = new TrackedBlock(suspended.tracked.placedAt + Math.max(0L, System.currentTimeMillis() - suspended.suspendedAt),
                suspended.tracked.type, suspended.tracked.data);
        resumed.warned = suspended.tracked.warned;
        blocks.putIfAbsent(block, resumed);
    }

    private void requireMainThread() {
        if (!org.bukkit.Bukkit.isPrimaryThread()) throw new IllegalStateException("Block decay suspension must run on the server thread");
    }

    public static final class SuspendedBlock {
        private final TrackedBlock tracked;
        private final long suspendedAt;

        private SuspendedBlock(TrackedBlock tracked, long suspendedAt) {
            this.tracked = tracked;
            this.suspendedAt = suspendedAt;
        }
    }

    public void setDecayMultiplier(double decayMultiplier) {
        this.decayMultiplier = Math.max(0.1D, decayMultiplier);
    }

    public void clearWorld(org.bukkit.World world) {
        for (Map.Entry<Block, TrackedBlock> entry : blocks.entrySet()) {
            Block block = entry.getKey();
            TrackedBlock tracked = entry.getValue();
            if (!block.getWorld().equals(world) || !blocks.remove(block, tracked)) continue;
            if (world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4) && tracked.matches(block))
                block.setType(Material.AIR, false);
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long warningDelay = (long) (5000 * decayMultiplier);
        long removalDelay = (long) (8000 * decayMultiplier);

        for (Map.Entry<Block, TrackedBlock> entry : blocks.entrySet()) {
            Block block = entry.getKey();
            TrackedBlock trackedBlock = entry.getValue();

            if (org.bukkit.Bukkit.getWorld(block.getWorld().getUID()) != block.getWorld()) {
                blocks.remove(block, trackedBlock);
                continue;
            }

            if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) continue;

            long elapsed = now - trackedBlock.placedAt;

            if (elapsed >= removalDelay) {
                if (blocks.remove(block, trackedBlock) && trackedBlock.matches(block)) {
                    block.setType(Material.AIR, false);
                }
                continue;
            }

            if (elapsed >= warningDelay && trackedBlock.type != Material.WEB && !trackedBlock.warned) {
                if (trackedBlock.matches(block) && blocks.get(block) == trackedBlock) {
                    trackedBlock.warned = block.setTypeIdAndData(Material.STAINED_CLAY.getId(), (byte) 14, false);
                } else {
                    blocks.remove(block, trackedBlock);
                }
            }
        }
    }

    @FieldDefaults(level = AccessLevel.PRIVATE)
    static final class TrackedBlock {
        final long placedAt;
        final Material type;
        final byte data;
        boolean warned;

        TrackedBlock(long placedAt, Material type, byte data) {
            this.placedAt = placedAt;
            this.type = type;
            this.data = data;
        }

        boolean matches(Block block) {
            if (warned) return block.getType() == Material.STAINED_CLAY && block.getData() == 14;
            return !(block.getType() != type || block.getData() != data);
        }
    }
}
