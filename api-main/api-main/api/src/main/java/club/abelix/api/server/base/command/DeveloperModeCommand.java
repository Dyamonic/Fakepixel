package club.abelix.api.server.base.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>administratorModeCommand creation on 14.01.2023 at 18:15</p>
 */

@WithAlias("devmod")
@WithAlias("devmode")
@NamedCommand("administratormode")
public class DeveloperModeCommand {

    private static String DEV_MODE_PROPERTY = "Dev/Mode";

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.devmode")
    public static void execute(BukkitExecutionSession session) {
        Gamer gamer = session.getGamer();
        int currentStatus = gamer.getProperty(DEV_MODE_PROPERTY);

        switch (currentStatus) {
            case 1 -> {
                gamer.setProperty(DEV_MODE_PROPERTY, 0);
                gamer.getPlayer().setOp(false);
                gamer.sendLocalizedMessage("dev_mode_disable");
            }
            case 0 -> {
                gamer.setProperty(DEV_MODE_PROPERTY, 1);
                gamer.getPlayer().setOp(true);
                gamer.sendLocalizedMessage("dev_mode_enable");
            }
        }

    }
}
