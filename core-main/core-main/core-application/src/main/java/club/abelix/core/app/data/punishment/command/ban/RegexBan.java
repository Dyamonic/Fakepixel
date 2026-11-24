package club.abelix.core.app.data.punishment.command.ban;


import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.Async;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;
import club.abelix.core.app.data.punishment.exception.PunishmentException;
import club.abelix.network.common.logging.Logging;
import lombok.val;

@NamedCommand("regexban")
public class RegexBan {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.regexban")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        String pattern = session.getArgument(0);
        Logging.IMP.info("Start regex ban: " + pattern);
        session.sendMessage("Start regex ban: " + pattern);

        Async.run(() -> {
            for (Gamer gamer : Gamer.getGamers()) {
                val gamerName = gamer.getName();

                if (!gamerName.matches(pattern)) {
                    Logging.IMP.error("[RegexBan] User skip: " + gamerName);
                    continue;
                }
                Logging.IMP.error("[RegexBan] Start ban processing: " + gamerName);

                try {
                    manager.processBan(
                            session.getSender(), gamerName, "love <3", null
                    );
                    manager.processBan(
                            session.getSender(), gamer.getAddress().getHostAddress(), "love <3", "2d"
                    );
                } catch (PunishmentException e) {
                    session.sendMessage("Failed ban: " + e);
                }

            }
        });

    }
}
