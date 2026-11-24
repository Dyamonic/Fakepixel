package club.abelix.core.votifer.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.vote.VotePlatform;
import club.abelix.commons.data.vote.VoteSqlHandler;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.RequiredArgsConstructor;
import club.abelix.core.votifer.manager.VoteManager;

@NamedCommand("corevotifier")
@RequiredArgsConstructor
public final class VotifierCommand {

    private final VoteManager voteManager;

    @Executor
    @WithPermission("command.corevotifier")
    public void execute(CoreExecutionSession session) throws CommandException {
        if (session.getArgumentCount() == 0) {
            session.sendLocalizedMessage("configuration_reloaded");

            voteManager.reloadConfiguration();
        } else if (session.getArgumentCount() == 2) {
            VotePlatform platform = VoteSqlHandler.INSTANCE.getVotePlatform(
                    session.getIntArgument(1, "platform id")
            );

            if (platform != null) {
                voteManager.processVote(platform, session.getArgument(0));
            }
        }
    }
}
