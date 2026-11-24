package club.abelix.core.app.data.punishment.command.ban;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.io.IOException;

@NamedCommand("ban")
public final class BanCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @Tabcomplete("[playerName] [any: Hacking|Insult Staff|DDOS Threads|Extreme Negative Behaviour|Advertising IP|Website Advertising|Scamming|Phishing|Exploiting|Teaming/Cross Teaming|Team Griefing|Inapporate Skin/Cape|Inapporate Build]")
    @WithPermission("command.ban")
    @WithCondition("minArgs: 2")
    public void execute(CoreExecutionSession session) throws CommandTranslationException, IOException {
        CommandSender admin = session.getSender();
        String target = session.getArgument(0);
        String reason = session.getArgumentString(1);

        manager.processBan(
                admin, target,
                reason, null
        );
    }

}
