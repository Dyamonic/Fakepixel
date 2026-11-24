package club.abelix.core.app.data.stream.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.stream.StreamManager;
import club.abelix.core.app.data.stream.detail.AbstractStreamDetails;
import club.abelix.core.app.data.stream.platform.StreamPlatform;
import com.google.common.base.Joiner;
import lombok.val;

import java.util.List;
import java.util.stream.Collectors;

@NamedCommand("stream")
public final class StreamCommand {

    @Executor
    @WithCondition("disableConsole")
    @WithCondition("disableStaff")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        if (!session.getSender().hasPermission("command.stream")) {
            throw new CommandTranslationException("stream_only_youtube");
        }

        StreamManager streamManager = StreamManager.INSTANCE;

        if (streamManager.hasStreamCooldown(session.getGamer())) {
            throw new CommandTranslationException("stream_cooldown");
        }

        AbstractStreamDetails streamDetails = streamManager.getActiveStream(session.getGamer());
        boolean hasStream = streamDetails != null;

        if (session.getArgumentCount() == 0) {
            if (hasStream) {
                session.sendLocalizedMessage("stream_already_started");
            } else {
                session.sendCommandUsage("stream");
            }
            return;
        }

        if (!hasStream) {
            for (StreamPlatform<?> streamPlatform : streamManager.getAvailableStreamPlatforms()) {
                streamDetails = streamPlatform.parseStreamUrl(session.getArgument(0));
                if (streamDetails != null) {
                    break;
                }
            }

            if (streamDetails == null) {
                List<String> platformNames = streamManager.getAvailableStreamPlatforms()
                        .stream().map(StreamPlatform::getDisplayName).collect(Collectors.toList());

                throw new CommandTranslationException("stream_unknown_platform",
                        "%platforms%", Joiner.on("§f, ").join(platformNames));
            }

            if (streamManager.getActiveStream(streamDetails.getIdentity()) != null) {
                throw new CommandTranslationException("stream_similar_found");
            }

            session.sendLocalizedMessage("stream_created");
            streamManager.addPlayerStream(session.getGamer(), streamDetails);
        }
    }
    @SubcommandExecutor("remove")
    public static void remove(CoreExecutionSession session) throws CommandTranslationException {
        if (!session.getSender().hasPermission("command.stream")) {
            throw new CommandTranslationException("stream_only_youtube");
        }

        val streamManager = StreamManager.INSTANCE;
        val gamer = session.getGamer();

        if (streamManager.hasStream(gamer)) {
            session.sendLocalizedMessage("stream_removed");
            streamManager.removePlayerStream(gamer);
            streamManager.addPlayerCooldown(gamer);
            return;
        }

        if (!session.getSender().hasPermission("staff.stream.remove")) {
            throw new CommandTranslationException("stream_only_staff_command");
        }

        if (session.getArgumentCount() != 1) {
            throw new CommandTranslationException("stream_only_staff_command_exception");
        }

        val deletedStreamer = session.getGamer(0);

        streamManager.removePlayerStream(deletedStreamer);
        streamManager.addPlayerCooldown(deletedStreamer);
        deletedStreamer.sendLocalizedMessage("stream_removed_by_staff");
        gamer.sendLocalizedMessage(
                "stream_removed_by_staff_command",
                "%streamer_nickname%", deletedStreamer.getName()
        );

    }
}
