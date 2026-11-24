package club.abelix.games.fastbridge.game.animation.standard;

import club.abelix.core.bukkit.HeartBeat;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.game.animation.AbstractAnimatedBlockRemover;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class FallingBlockRemoverAnimation extends AbstractAnimatedBlockRemover {

    private final Random random = ThreadLocalRandom.current();

    @Override
    public void remove(@NotNull List<Block> blocks, @NotNull FastBridgeGamer gamer) {
        val transformedBlocks = new ArrayList<FallingBlock>();

        for (val block : blocks) {
            val location = block.getLocation();
            val material = block.getType();

            block.setType(Material.AIR);

            val fallingBlock = location.getWorld().spawnFallingBlock(location, material, block.getData());

            fallingBlock.setDropItem(false);
            fallingBlock.setVelocity(new Vector(0, -0.3, 0));

            val xOffset = (random.nextDouble() - 0.5) * 0.2;
            val zOffset = (random.nextDouble() - 0.5) * 0.2;
            fallingBlock.setVelocity(new Vector(xOffset, -0.3, zOffset));

            val blockBelow = fallingBlock.getLocation().clone().add(0, -1, 0).getBlock();

            if (blockBelow.getType() != Material.AIR) {
                fallingBlock.remove();
                return;
            }

            transformedBlocks.add(fallingBlock);
        }

        HeartBeat.runLaterSync(() -> transformedBlocks.forEach(Entity::remove), 30);
    }
}
