package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.commons.data.auth.BaseAuthPlayerData;
import club.abelix.commons.data.session.Session;
import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.commons.util.IpAddressUtil;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.util.List;

@WithHelp
@WithAlias("fetchevasions")
@NamedCommand("fetchevadingplayers")
public final class FetchEvadingPlayersCommand {

    private static boolean processOnlineStatus(final List<String> accounts) {

        boolean hasBanned = false;
        for (int i = 0; i < accounts.size(); i++) {
            String name = accounts.get(i);

            Gamer gamer = Gamer.getOfflineGamer(name, Gamer::loadLastSession);

            if (gamer == null) {
                accounts.set(i, "§c" + name);
                continue;
            }

            boolean banned;

            if (gamer.getLastSession() != null) {
                banned = PunishmentManager.INSTANCE.getActiveBan(name, gamer.getLastSession().getAddress()) != null;
            } else {
                banned = PunishmentManager.INSTANCE.getActiveNameBans(name).size() > 0;
            }

            if (banned) {
                hasBanned = true;

                accounts.set(i, (banned ? "§4[B] " : "") + (gamer.isOnline() ? "§a" : "§c") + name);
            }
        }
        return hasBanned;
    }

    @WithPermission("command.fetchevasions")
    @SubcommandExecutor("registered")
    public void regip(CoreExecutionSession session) {
        Gamer.getGamers().forEach(target -> {
            BaseAuthPlayerData authData = new BaseAuthPlayerData(target.getName());
            authData.load();

            String regIp = authData.getRegIp();

            List<String> regIpAccounts = AuthSqlHandler.INSTANCE.findAccounts(regIp)
                    .join();

            boolean banned = processOnlineStatus(regIpAccounts);

            if (!banned) {
                return;
            }

            session.sendLocalizedMessage("core_fetchevading_found",
                    "%target%", target.getDisplayName(),
                    "%accs%", String.join("§f, ", regIpAccounts), "%ip%", IpAddressUtil.hideLastPart(regIp));
        });
    }

    @WithPermission("command.fetchevasions.last")
    @SubcommandExecutor("last")
    public void lastip(CoreExecutionSession session) {

        Gamer.getGamers().forEach(target -> {
            BaseAuthPlayerData authData = new BaseAuthPlayerData(target.getName());
            authData.load();

            Session lastSession = authData.getLastSession();

            if (lastSession != null) {
                String lastIp = lastSession.getAddress().getHostAddress();

                List<String> lastIpAccounts = SessionSqlHandler.INSTANCE.findAccounts(lastIp)
                        .join();

                boolean banned = processOnlineStatus(lastIpAccounts);

                if (!banned) {
                    return;
                }

                session.sendLocalizedMessage("core_fetchevading_found",
                        "%target%", target.getDisplayName(),
                        "%accs%", String.join("§f, ", lastIpAccounts), "%ip%", IpAddressUtil.hideLastPart(lastIp));
            }
        });
    }
}
