package club.abelix.core.app.command;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.commons.data.auth.BaseAuthPlayerData;
import club.abelix.commons.data.session.Session;
import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.commons.util.IpAddressUtil;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.PunishmentManager;
import lombok.val;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@WithAlias("alts")
@NamedCommand("findaccounts")
public final class FindAccountsCommand {

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.alts")
    @Async
    public static void execute(final CoreExecutionSession session) throws CommandTranslationException {
        val arg = session.getArgument(0);



        Gamer target = session.getOfflineGamer(0, gamer -> {
            gamer.loadPlayerId();
            gamer.loadRanks();
            gamer.loadPrefix();
        });

        BaseAuthPlayerData authData = new BaseAuthPlayerData(target.getName());
        authData.load();

        String regIp = authData.getRegIp();

        List<String> regIpAccounts = AuthSqlHandler.INSTANCE.findAccounts(regIp)
                .join();

        processOnlineStatus(regIpAccounts);

        session.sendLocalizedMessage("core_findaccounts_result",
                "%name%", target.getDisplayName(),
                "%reg_ip_names%", String.join("§f, ", regIpAccounts)
        );

        Session lastSession = authData.getLastSession();

        if (lastSession != null) {
            String lastIp = lastSession.getAddress().getHostAddress();

            List<String> lastIpAccounts = SessionSqlHandler.INSTANCE.findAccounts(lastIp)
                    .join();

            processOnlineStatus(lastIpAccounts);

            session.sendLocalizedMessage("core_findaccounts_result_last",
                    "%last_ip_names%", String.join("§f, ", lastIpAccounts)
            );
        }

        if (target.isOnline()) {
            InetAddress address = target.getAddress();

            List<String> accounts = new ArrayList<>();

            for (Gamer gamer : Gamer.getGamersByIp(address)) {
                accounts.add(gamer.getDisplayName());
            }

            session.sendLocalizedMessage("core_findaccounts_result_online",
                    "%current_ip_names%", String.join("§f, ", accounts)
            );
        }
    }

    private static void processOnlineStatus(final List<String> accounts) {
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

            accounts.set(i, (banned ? "§4[B] " : "") + (gamer.isOnline() ? "§a" : "§c") + name);
        }
    }

}
