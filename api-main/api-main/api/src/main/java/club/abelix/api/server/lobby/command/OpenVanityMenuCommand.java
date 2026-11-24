package club.abelix.api.server.lobby.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("openvanitymenu")
@IgnoreLogs
public class OpenVanityMenuCommand {
    @Executor
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        gamer.getPlayer().performCommand("gmenu main");
    }

}
