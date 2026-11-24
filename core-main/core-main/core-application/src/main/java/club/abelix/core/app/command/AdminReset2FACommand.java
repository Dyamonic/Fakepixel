package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;

@WithHelp
@NamedCommand("adminreset2fa")
@WithAlias("a2fareset")
public final class AdminReset2FACommand {

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.adminreset2fa")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer targetGamer = session.getOfflineGamer(0, offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadLanguage();
            offline.updateDisplayName();
        });

        AuthSqlHandler.INSTANCE.setTelegramId(targetGamer.getPlayerId(), 0);

        session.sendLocalizedMessage("admin_user_2fa_reset_finish",
                "%target%", targetGamer.getDisplayName());

        ServerManager.IMP.broadcastStaff("command.staffchat", "2fa_remove_broadcast",
                "%target%", targetGamer.getName(),
                "%sender%", session.getGamer().getDisplayName()
        );
    }
}