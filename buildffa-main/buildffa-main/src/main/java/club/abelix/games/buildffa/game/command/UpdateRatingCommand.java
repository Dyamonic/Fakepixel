package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import lombok.val;

@NamedCommand("updaterating")
public class UpdateRatingCommand {

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("buildffa.updaterating")
    public static void execute(BukkitExecutionSession session) throws CommandException {
        val gamer = session.getGamer();

        int rating = session.getIntArgument(0, "rating");

        gamer.addStat("rating", rating);
    }
}
