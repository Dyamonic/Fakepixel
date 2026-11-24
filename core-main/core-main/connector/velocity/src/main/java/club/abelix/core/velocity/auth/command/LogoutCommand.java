package club.abelix.core.velocity.auth.command;

import club.abelix.commons.data.action.ActionManager;
import club.abelix.commons.data.action.ActionType;
import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.core.velocity.gamer.Gamer;
import net.kyori.adventure.text.Component;

public class LogoutCommand extends AuthCommand {

    public LogoutCommand() {
        super("logout", "выход");

        setNeedLogin(true);
        setNeedLegacyMode(true);
        setNeedRegistration(true);
    }

    @Override
    protected void execute(Gamer gamer, String[] args) {
        int sessionId = gamer.getCurrentSession().getSessionId();

        ActionManager.save(gamer.getName(), gamer.getAddress().getHostAddress(), ActionType.LOG_OUT);

        SessionSqlHandler.INSTANCE.changeSessionEndTime(sessionId, null);
        gamer.setCurrentSession(null);

        gamer.getPlayer().disconnect(Component.text(gamer.getLanguage().getMessage("auth_logout_success")));
    }
}
