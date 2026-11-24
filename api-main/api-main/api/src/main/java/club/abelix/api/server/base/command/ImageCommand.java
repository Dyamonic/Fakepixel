package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.util.imagemap.ImageMap;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandMessageException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import org.bukkit.Material;
import org.bukkit.block.Block;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;

@WithAlias("img")
@NamedCommand("image")
public class ImageCommand {

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.image")
    @WithCondition("minArgs: 3")
    public static void executor(BukkitExecutionSession session) throws CommandMessageException {
        Block block = session.getPlayer().getTargetBlock((HashSet<Material>) null, 200);

        if (block == null || block.getType() == Material.AIR) {
            throw new CommandTranslationException("image_cant_get_block");
        }

        BufferedImage image;

        try {
            image = ImageIO.read(new File(session.getArgument(0)));
        } catch (IOException e) {
            throw new CommandTranslationException("file_not_found");
        }

        new ImageMap(block.getLocation(), LocationUtil.getFacingDirection(session.getPlayer().getLocation().getYaw(), true).getOppositeFace(), image);

        session.sendLocalizedMessage("image_spawned");
    }
}
