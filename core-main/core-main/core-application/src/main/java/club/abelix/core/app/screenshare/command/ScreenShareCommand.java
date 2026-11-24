package club.abelix.core.app.screenshare.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.screenshare.service.ScreenShareService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@NamedCommand("screenshare")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public final class ScreenShareCommand {

    ScreenShareService service;

    @WithAlias("begin")
    @WithPermission("command.screenshare")
    @SubcommandExecutor("start")
    public void start(CoreExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() < 1) {
            throw new CommandTranslationException("screenshare_start_usage");
        }
        val target = session.getGamer(0);

        session.sendMessage("Preparing session...");

        service.start(
                session.getGamer(),
                target
        );
    }

    @WithAlias("end")
    @WithAlias("done")
    @WithAlias("finish")
    @WithPermission("command.screenshare")
    @SubcommandExecutor("remove")
    public void remove(CoreExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() == 0) {
            service.finishByAdmin(session.getGamer());
        } else {
            val target = session.getGamer(0);
            service.finishByAdmin(target);
        }
    }

}
