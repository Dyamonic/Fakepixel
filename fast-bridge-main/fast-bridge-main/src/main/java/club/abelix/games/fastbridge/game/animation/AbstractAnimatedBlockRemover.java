package club.abelix.games.fastbridge.game.animation;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public abstract class AbstractAnimatedBlockRemover implements AnimatedBlockRemover {

    protected void removeBlock(final Block block, final Player player) {
        block.setType(Material.AIR);
        player.sendBlockChange(block.getLocation(), Material.AIR, (byte) 0);
    }

}
