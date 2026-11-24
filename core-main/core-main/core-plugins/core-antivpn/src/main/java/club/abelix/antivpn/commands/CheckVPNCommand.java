package club.abelix.antivpn.commands;

import club.abelix.antivpn.manager.AntiVPNManager;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@NamedCommand("checkvpn")
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@RequiredArgsConstructor
public final class CheckVPNCommand {

    AntiVPNManager antiVPNManager;

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.checkvpn")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer target = session.getGamer(0);

        String ip = target.getAddress().getHostAddress();
        antiVPNManager.check(ip).whenComplete((result, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                session.sendLocalizedMessage("internal_error");
                return;
            }

            session.sendLocalizedMessage("antivpn_check_result", "%ip%", ip,
                    "%verdict%", result == null ? "§cНеизвестно" : result ? "§cИСПОЛЬЗУЕТСЯ" : "§aНЕ используется"
            );
        });
    }

}
