package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.BuildFFAGame;

@NamedCommand("freeze")
public final class FreezeCommand {
    private static BuildFFAGame game;
    public static void setGame(BuildFFAGame value) { game = value; }
    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) {
        if (game != null) FreezeCommandSupport.execute(game, session.getGamer(), session.getArguments(), true);
    }
}