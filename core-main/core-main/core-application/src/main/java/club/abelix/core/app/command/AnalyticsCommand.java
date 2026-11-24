package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.version.GameVersion;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@WithHelp
@WithAlias("astats")
@NamedCommand("analytics")
public final class AnalyticsCommand {

    @Tabcomplete("[playerName]")
    @WithPermission("command.analytics.domains")
    @SubcommandExecutor("domains")
    public void domains(CoreExecutionSession session) {
        Collection<Gamer> gamers;
        String scope;

        if (session.getArgumentCount() > 0) {
            scope = session.getArgument(0);
            gamers = new ArrayList<>();

            for (AbstractServer server : ServerManager.IMP.findServers(scope)) {
                gamers.addAll(server.getGamers());
            }
        } else {
            scope = "global";
            gamers = Gamer.getGamers();
        }

        printDomainsAnalytics(session, gamers, scope);
    }

    @Tabcomplete("[playerName]")
    @WithPermission("command.analytics.network")
    @WithAlias("versions")
    @SubcommandExecutor("network")
    public void protocol(CoreExecutionSession session) {
        Collection<Gamer> gamers;
        String scope;

        if (session.getArgumentCount() > 0) {
            scope = session.getArgument(0);
            gamers = new ArrayList<>();

            for (AbstractServer server : ServerManager.IMP.findServers(scope)) {
                gamers.addAll(server.getGamers());
            }
        } else {
            scope = "global";
            gamers = Gamer.getGamers();
        }

        printProtocolAnalytics(session, gamers, scope);
    }

    private void printProtocolAnalytics(CoreExecutionSession session, Collection<Gamer> gamers, String scope) {
        session.sendMessage("§dCore §8:: §fСтатистика версий текущего онлайна: §7(%s)", scope);

        for (Map.Entry<Integer, Integer> bestProtocol : calcProtocols(gamers)) {
            session.sendMessage(" §d| §fНомер протокола §7[%s]§f: §d%s", GameVersion.getByProtocolNumber(bestProtocol.getKey()), bestProtocol.getValue());
        }
    }

    private List<Map.Entry<Integer, Integer>> calcProtocols(Collection<Gamer> gamers) {
        Map<Integer, Integer> map = new HashMap<>();

        for (Gamer gamer : gamers) {
            map.compute(gamer.getProtocol(), (key, value) -> value == null ? 1 : value + 1);
        }

        return map.entrySet().stream()
                .sorted((o1, o2) -> -Integer.compare(o1.getValue(), o2.getValue()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private void printDomainsAnalytics(CoreExecutionSession session, Collection<Gamer> gamers, String scope) {
        session.sendMessage("§dCore §8:: §fСтатистика доменов текущего онлайна: §7(%s)", scope);

        for (Map.Entry<String, Integer> domain : calcDomains(gamers)) {
            session.sendMessage(" §d| §fДомен §7[%s]§f: §d%s", domain.getKey(), domain.getValue());
        }
    }

    private List<Map.Entry<String, Integer>> calcDomains(Collection<Gamer> gamers) {
        Map<String, Integer> map = new HashMap<>();

        for (Gamer gamer : gamers) {
            map.compute(gamer.getDomain(), (key, value) -> value == null ? 1 : value + 1);
        }

        return map.entrySet().stream()
                .sorted((o1, o2) -> -Integer.compare(o1.getValue(), o2.getValue()))
                .limit(10)
                .collect(Collectors.toList());
    }

}
