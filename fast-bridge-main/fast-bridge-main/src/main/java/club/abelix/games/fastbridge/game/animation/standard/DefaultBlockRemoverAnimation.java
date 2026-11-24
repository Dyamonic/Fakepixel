package club.abelix.games.fastbridge.game.animation.standard;

import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.commons.utils.TickTaskExecutor;
import club.abelix.games.fastbridge.game.animation.AbstractAnimatedBlockRemover;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DefaultBlockRemoverAnimation extends AbstractAnimatedBlockRemover {

    @Override
    public void remove(@NotNull List<Block> blocks, @NotNull FastBridgeGamer gamer) {
        final int totalTicks = 20;
        final int blocksPerTick = Math.max(1, (int) Math.ceil((double) blocks.size() / totalTicks));
        final Player player = gamer.getGamer().getPlayer();
        TickTaskExecutor.getInstance().addTask(new Runnable() {
            int ticksPassed = 0;

            @Override
            public void run() {
                if (blocks.isEmpty() || ticksPassed >= totalTicks) {
                    return;
                }

                for (int i = 0; i < blocksPerTick; i++) {
                    if (blocks.isEmpty()) break;

                    Block block = blocks.remove(0);
                    removeBlock(block, player);
                }

                ticksPassed++;

                if (!blocks.isEmpty() && ticksPassed < totalTicks) {
                    TickTaskExecutor.getInstance().addTask(this);
                } else {
                    blocks.forEach(block -> removeBlock(block, player));
                }
            }
        });
    }
}
