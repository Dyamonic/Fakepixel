package club.abelix.core.velocity.listener.player;

import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.core.velocity.util.ServerUtil;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.client.CPlayerConnected;
import club.abelix.network.packet.play.client.CServerConnectRequest;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.mode.Unknown;
import club.abelix.network.util.ModeUtil;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import net.kyori.adventure.text.Component;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>PlayerServerListener creation on 02.03.2023 at 02:09</p>
 */

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerServerListener {

    StarshipVelocity starship;
    VelocityMetricsStorage metricsStorage;


    @Subscribe
    public EventTask onServerConnect(ServerPreConnectEvent event) {
        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);

        if (gamer == null || !gamer.isLoggedIn()) {
            val serverInfo = ServerUtil.getRandomServer(Special.LOGIN);

            if (serverInfo != null) {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(serverInfo));
            } else {
                player.disconnect(Component.text(gamer.getLanguage().getMessage("proxy_auth_server_unavailable")));
            }

            return null;
        }

        val target = event.getOriginalServer();
        val targetName = target.getServerInfo().getName();

        val mode = ModeUtil.getServerModeByName(targetName);

        if (gamer.getFirstServer() != null) { // игрок вошёл и ему установлен новый сервер
            val firstServer = gamer.getFirstServer();

            if (!firstServer.equals(target)) {
                event.setResult(ServerPreConnectEvent.ServerResult.allowed(firstServer));
            }

            gamer.setFirstServer(null);
            return null;
        }

        if (mode.isSpecial() || !Connection.isConnected()) {
            return null;
        }

        // Игрок ещё не подключён к кору
        if (!gamer.isConnectedToCore()) {
            return null;
        }

        CServerConnectRequest request = new CServerConnectRequest(gamer.getPlayerId(), targetName);

        val future = Connection.awaitPacket(request).whenComplete((response, cause) -> {
            if (cause != null) {
                Logging.IMP.error("Failed to connect to server {} for player {}",
                        targetName, gamer.getName(), cause);
                metricsStorage.incrementServerConnectErrors();
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                return;
            }

            if (response.isCancelled()) {
                metricsStorage.incrementServerConnectErrors();
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
            } else {
                val newServer = starship.getProxy().getServer(response.getNewServer()).orElse(null);

                if (newServer == null) {
                    Logging.IMP.info("Не удалось найти сервер {} для игрока {}", response.getNewServer(), gamer.getName());
                    metricsStorage.incrementServerConnectErrors();
                    event.setResult(ServerPreConnectEvent.ServerResult.denied());
                    return;
                }

                event.setResult(ServerPreConnectEvent.ServerResult.allowed(newServer));
            }
        });

        return EventTask.resumeWhenComplete(future);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        val server = event.getServer();
        val serverName = server.getServerInfo().getName();

        val mode = ModeUtil.getServerModeByName(serverName);

        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);

        if (gamer != null) {
            if (mode instanceof Unknown || !mode.isSpecial()) {
                gamer.changeLastServer(serverName);
            }

            if (gamer.isLoggedIn() && gamer.isConnectedToCore()) {
                Connection.sendPacket(new CPlayerConnected(gamer.getPlayerId(), serverName));
                Logging.IMP.info("[{}] connected to {}", gamer.getName(), serverName);
            }
        }
    }
}
