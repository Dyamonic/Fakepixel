package club.abelix.core.app.data.action;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.action.ActionManager;
import club.abelix.commons.data.action.ActionSqlHandler;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.app.api.command.CoreExecutionSession;
import lombok.val;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@WithAlias("authactions")
@WithAlias("auths")
@NamedCommand("actions")
public final class ActionsCommand {

    @WithCondition("minArgs: 1")
    @WithPermission("command.authactions")
    @Executor
    public static void execute(final CoreExecutionSession session) throws CommandTranslationException {
        val target = session.getArgument(0);
        val isIp = Patterns.isIPV4(target);

        long time = TimeUnit.DAYS.toSeconds(3);

        if (session.getArgumentCount() > 1) {
            try {
                time = Patterns.parseTime(session.getArgument(1), TimeUnit.SECONDS);
            } catch (IllegalArgumentException e) {
                session.sendLocalizedMessage("core_invalid_time_format");
            }
        }

        val from = LocalDateTime.now()
                .minusSeconds(time);

        val future = isIp
                ? ActionSqlHandler.INSTANCE.getIpActions(target, from)
                : ActionSqlHandler.INSTANCE.getNameActions(target, from);

        final long duration = time;

        future.thenAccept(actions -> {
            if (actions.isEmpty()) {
                session.sendLocalizedMessage("core_actions_empty_result");
                return;
            }

            val language = session.getLanguage();

            val builder = new StringBuilder();

            builder.append(session.getLanguage().getMessage(
                    "core_actions_" + (isIp ? "ip" : "name") + "_result",
                    "%target%", target,
                    "%duration%", Duration.durationWordsSec(language, duration)
            ));

            for (val action : actions) {
                builder.append("\n");
                builder.append(ActionManager.getActionText(language, action, isIp));
            }

            session.sendMessage(builder.toString());
        });
    }
}
