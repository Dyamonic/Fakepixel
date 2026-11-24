package club.abelix.core.app.data.punishment.command.ban;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;

@NamedCommand("massban")
public class MassBanCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @WithPermission("command.massban")
    @WithCondition("minArgs: 1")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        for (String arg : session.getArguments()) {
            if (!Patterns.isIPV4(arg)) {
                Gamer gamer = Gamer.getGamer(arg);

                if (gamer == null) {
                    continue;
                }

                arg = gamer.getAddress().getHostAddress();
            }

            manager.processBan(
                    session.getSender(), arg, "love <3", null
            );
        }
    }

}
