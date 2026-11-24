package club.abelix.api.server.base.command;

import club.abelix.api.base.api.CoreAPI;
import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("testcoreapi")
public class TestCoreAPI {

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.test.coreapi")
    public static void execute(BukkitExecutionSession session) {
        Gamer player = session.getGamer();
        String server = session.getArgument(0);
        String command = session.getArgument(1);

        CoreAPI.dispatchCommand(server, command);

        player.sendMessage("Command " + command + " dispatched on server: " + server);

    }

}
