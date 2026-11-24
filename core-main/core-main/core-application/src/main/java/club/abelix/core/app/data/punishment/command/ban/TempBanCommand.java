package club.abelix.core.app.data.punishment.command.ban;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
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

@WithAlias("tban")
@NamedCommand("tempban")
public final class TempBanCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @Tabcomplete("[playerName] [any: 1d|3d|5d|7d] [any: Hacking|Insult Staff|DDOS Threads|Extreme Negative Behaviour|Advertising IP|Website Advertising|Scamming|Phishing|Exploiting|Teaming/Cross Teaming|Team Griefing|Inapporate Skin/Cape|Inapporate Build]")
    @WithCondition("minArgs: 3")
    @WithPermission("command.tempban")
    public void execute(CoreExecutionSession session) throws CommandTranslationException, IOException {

        CommandSender admin = session.getSender();
        String target = session.getArgument(0);
        String time = session.getArgument(1);
        String reason = session.getArgumentString(2);

        manager.processBan(
                admin, target,
                reason, time
        );
    }

}
