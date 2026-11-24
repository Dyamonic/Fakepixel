package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.BuildFFAGame;

@NamedCommand("buildmode")
public final class BuildModeCommand {
    private static BuildFFAGame game;

    public static void setGame(BuildFFAGame buildFFAGame) {
        game = buildFFAGame;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission(BuildFFAGame.BUILD_MODE_PERMISSION)
    public static void execute(BukkitExecutionSession session) {
        if (game != null) game.executeBuildMode(session.getGamer(), session.getArguments());
    }
}
