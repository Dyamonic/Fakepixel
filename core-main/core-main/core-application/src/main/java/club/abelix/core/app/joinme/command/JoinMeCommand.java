package club.abelix.core.app.joinme.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.cooldown.WithCooldown;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.joinme.service.JoinMeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@NamedCommand("joinme")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class JoinMeCommand {

    JoinMeService service;

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.joinme")
    @WithCooldown(300_000) // 5 mins
    public void execute(CoreExecutionSession session) {
        val gamer = session.getGamer();

        if (service.hasActiveJoinMe(gamer)) {
            session.sendLocalizedMessage("joinme_command_already_active");
            return;
        }

        service.startJoinMe(gamer, gamer.getServer());
    }


    @SubcommandExecutor("join")
    public void executeJoin(CoreExecutionSession session) throws CommandTranslationException {
        val target = session.getGamer(0);

        if (!service.hasActiveJoinMe(target)) {
            session.sendLocalizedMessage("joinme_no_active", "%target%", target.getDisplayName());
            return;
        }

        service.join(session.getGamer(), target);
    }
}
