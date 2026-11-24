package club.abelix.api.base.privategame;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;

@NamedCommand("privategame")
@WithAlias("privategames")
@WithAlias("pg")
public class PrivateGameCommand {

    @Executor
    @WithPermission("game.private")
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) throws CommandTranslationException {
        new PrivateGameGui(session.getGamer());
    }
}
