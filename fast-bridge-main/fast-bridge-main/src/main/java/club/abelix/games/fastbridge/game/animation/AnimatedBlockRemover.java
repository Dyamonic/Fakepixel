package club.abelix.games.fastbridge.game.animation;

import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface AnimatedBlockRemover {

    void remove(@NotNull List<Block> blocks, @NotNull FastBridgeGamer gamer);

}
