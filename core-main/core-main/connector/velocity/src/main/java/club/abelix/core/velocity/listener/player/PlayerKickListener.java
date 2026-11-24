package club.abelix.core.velocity.listener.player;

import club.abelix.core.velocity.metrics.KickByReasonMetricsStorage;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import club.abelix.core.velocity.util.ServerUtil;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Special;
import club.abelix.network.type.mode.Uncommon;
import club.abelix.network.util.ModeUtil;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import io.sentry.protocol.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerKickListener {

    KickByReasonMetricsStorage storage;
    VelocityMetricsStorage velocityMetricsStorage;

    @Subscribe(priority = 30_000)
    public void onServerKickEvent(KickedFromServerEvent event) {
        val from = event.getServer();
        val playerName = event.getPlayer().getUsername();
        val selectedServer = findCandidate(event);

        if (selectedServer.isEmpty()) {
            velocityMetricsStorage.incrementRedirectFailures();
            event.setResult(
                    KickedFromServerEvent.DisconnectPlayer.create(
                            Component.text("An error occurred while redirecting you from " + from.getServerInfo().getName())
                    )
            );

            log.error("Player {} kicked from server due to no candidate found for redirect!", playerName);

            captureRedirectFailure(event, null);

            return;
        }

        val server = selectedServer.get();

        event.setResult(
                KickedFromServerEvent.RedirectPlayer.create(
                        server,
                        Component.text("You have been redirected to " + server.getServerInfo().getName()).color(NamedTextColor.DARK_GRAY)
                )
        );

        log.info(
                "Player {} redirected from {} to {}",
                playerName,
                from.getServerInfo().getName(),
                server.getServerInfo().getName()
        );
    }

    private Optional<RegisteredServer> findCandidate(
            final KickedFromServerEvent event
    ) {
        val player = event.getPlayer();
        val optionalReason = event.getServerKickReason();
        val kickedFrom = event.getServer();
        val playerName = player.getUsername();

        if (optionalReason.isEmpty()) {
            val selectedServer = ServerUtil.getRandomServer(Special.LIMBO);

            log.warn(
                    "Player {} got kicked from {} without reason and sent to {}",
                    playerName,
                    kickedFrom.getServerInfo().getName(),
                    selectedServer.getServerInfo().getName()
            );

            storage.incrementReasonCount("NO_REASON");
            captureRedirectFailure(event, "NO_REASON");

            return Optional.of(selectedServer);
        }

        val parsedReason = PlainTextComponentSerializer
                .plainText()
                .serialize(optionalReason.get());

        storage.incrementReasonCount(parsedReason);

        log.info(
                "Player {} kicked from {} with reason {}",
                playerName,
                kickedFrom.getServerInfo().getName(),
                parsedReason
        );

        val serverName = kickedFrom.getServerInfo().getName().split("-")[0];
        val kickedFromMode = ModeUtil.getServerMode(serverName);
        val optionalSpecial = Special.findByServerName(serverName);

        if (optionalSpecial.isPresent()) {
            val special = optionalSpecial.get();

            if (special == Special.LIMBO) {
                log.error(
                        "Player {} kicked from limbo with reason {}",
                        playerName,
                        parsedReason
                );

                return Optional.empty();
            }

            return Optional.of(ServerUtil.getRandomLimbo());
        }

        val masterOptional = Optional.ofNullable(Master.getByPrefix(serverName));

        if (masterOptional.isPresent()) {
            val master = masterOptional.get();
            val suitableLobbyType = master.getLobby();
            val lobbyServer = ServerUtil.getRandomServer(suitableLobbyType);

            if (lobbyServer == null) {
                val hub = ServerUtil.getRandomServer(Lobby.HUB);

                if (hub == null) {
                    return Optional.ofNullable(ServerUtil.getRandomLimbo());
                }

                return Optional.ofNullable(ServerUtil.getRandomServer(Lobby.HUB));
            }

            return Optional.of(lobbyServer);
        }

        val lobbyOptional = Optional.ofNullable(Lobby.getByName(serverName));

        if (lobbyOptional.isPresent()) {
            val lobby = lobbyOptional.get();

            if (lobby == Lobby.HUB) {
                val newServer = ServerUtil.getRandomServer(lobby, Collections.singleton(kickedFrom));

                if (newServer == null) {
                    log.info("No servers available by {} type, redirecting to LIMBO", lobby);

                    return Optional.ofNullable(ServerUtil.getRandomLimbo());
                }

                return Optional.of(newServer);
            }

            val newServer = ServerUtil.getRandomServer(lobby, Collections.singleton(kickedFrom));

            if (newServer == null) {
                log.info("No servers available by {} type, redirecting to HUB", lobby);
                return Optional.ofNullable(ServerUtil.getRandomServer(Lobby.HUB));
            }

            return Optional.of(newServer);
        }

        if (kickedFromMode instanceof Uncommon) {
            if (kickedFromMode == Uncommon.SKYBLOCK) {
                val newServer = ServerUtil.getRandomServer(Uncommon.SKYBLOCK, Collections.singleton(kickedFrom));

                if (newServer != null) {
                    return Optional.of(newServer);
                }
            }

            val newServer = ServerUtil.getRandomServer(Lobby.HUB, Collections.singleton(kickedFrom));

            if (newServer == null) {
                return Optional.of(ServerUtil.getRandomServer(Special.LIMBO, Collections.singleton(kickedFrom)));
            }

            return Optional.of(newServer);
        }

        captureRedirectFailure(event, parsedReason);
        return Optional.empty();
    }

    private void captureRedirectFailure(
            @NotNull final KickedFromServerEvent event,
            @Nullable final String parsedReasonOrNull
    ) {
        Sentry.withScope(scope -> {
            scope.setTag("component", "velocity-kick-redirect");
            scope.setTag("from_server", event.getServer().getServerInfo().getName());

            val user = new User();
            val player = event.getPlayer();

            user.setUsername(player.getUsername());
            user.setEmail("N/A");
            user.setId("N/A");
            user.setIpAddress(player.getRemoteAddress().getAddress().getHostAddress());

            scope.setUser(user);
            scope.setExtra("reason", parsedReasonOrNull);
            scope.setExtra("player_uuid", String.valueOf(event.getPlayer().getUniqueId()));
            scope.setExtra("server_mode_prefix", event.getServer().getServerInfo().getName().split("-")[0]);

            val msg = new Message();
            msg.setMessage("Redirect candidate not found after server kick");

            val sentryEvent = new SentryEvent();
            sentryEvent.setLevel(SentryLevel.ERROR);
            sentryEvent.setMessage(msg);

            Sentry.captureEvent(sentryEvent);
        });
    }
}
