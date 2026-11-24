package club.abelix.api.server.lobby.exclusive.newyear;


import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import lombok.val;

@NamedCommand("snow")
@IgnoreLogs
public class SnowCommand {

    private static final String PROPERTY_NAME = "Exclusive/Snow";

    private static boolean isSnowDisabled(final Gamer gamer) {
        return gamer.getProperty(PROPERTY_NAME) == 1;
    }

    @Executor
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        val gamer = session.getGamer();

        if (isSnowDisabled(gamer)) {
            gamer.setProperty(PROPERTY_NAME, 0);
            gamer.sendLocalizedMessage("snow_enabled");
        } else {
            gamer.setProperty(PROPERTY_NAME, 1);
            gamer.sendLocalizedMessage("snow_disabled");
        }

    }
}
