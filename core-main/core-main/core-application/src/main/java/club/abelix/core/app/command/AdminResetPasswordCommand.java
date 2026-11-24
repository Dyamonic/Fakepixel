package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.action.ActionManager;
import club.abelix.commons.data.action.ActionType;
import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.util.password.PasswordHashing;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>AdminResetPasswordCommand creation on 20.11.2022 at 16:51</p>
 */

@WithHelp
@NamedCommand("adminresetpassword")
@WithAlias("apreset")
public final class AdminResetPasswordCommand {

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.adminresetpassword")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer targetGamer = session.getOfflineGamer(0, offline -> {
            offline.loadRanks();
            offline.loadPrefix();
            offline.loadLanguage();
        });

        targetGamer.localizedKick("admin_user_password_reset_kick");

        String newPassword = PasswordHashing.generateRandomPassword(6);
        AuthSqlHandler.INSTANCE.setPassword(targetGamer.getName(), PasswordHashing.getHash(newPassword));

        ActionManager.save(targetGamer.getName(), ActionType.CHANGE_PASS);
        session.sendLocalizedMessage("admin_user_password_reset_finish",
                "%target%", targetGamer.getDisplayName(), "%newpassword%", newPassword);

        ServerManager.IMP.broadcastStaff("command.staffchat", "password_reset_staff",
                "%target%", targetGamer.getName(),
                "%sender%", session.getGamer().getDisplayName());
    }
}
