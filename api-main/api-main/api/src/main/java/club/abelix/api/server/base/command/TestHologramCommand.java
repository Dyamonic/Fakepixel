package club.abelix.api.server.base.command;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@NamedCommand("testhologram")
public class TestHologramCommand {

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.test.hologram")
    public static void execute(final @NotNull BukkitExecutionSession session) {
        Gamer gamer = session.getGamer();
        Player player = gamer.getPlayer();

        Hologram hologram = new Hologram(player.getLocation());

        String message = String.join(" ", Arrays.copyOfRange(session.getArguments(), 0, session.getArguments().length));

        hologram.addTextLine(message);
        hologram.setPublicStatus(true);

        new AbstractTask(FalconAPI.getInstance()) {
            @Override
            protected void run() {
                if (player.isOnline()) {
                    session.sendPrefixedMessage("Hologram removed");
                }

                hologram.remove();
            }
        }.later(5 * 20 * 60);

        session.sendPrefixedMessage("Created test hologram!");
    }

}