package club.abelix.core.velocity.auth;

import club.abelix.commons.data.action.ActionManager;
import club.abelix.commons.data.action.ActionType;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.auth.command.*;
import club.abelix.core.velocity.auth.listener.AuthTwoFactorListener;
import club.abelix.core.velocity.auth.task.AuthTaskUnit;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.core.velocity.metrics.VelocityMetricsStorage;
import club.abelix.core.velocity.util.ServerUtil;
import club.abelix.network.packet.play.both.BLicenseStatus;
import club.abelix.network.packet.play.client.CProxyPlayerLoginRequest;
import club.abelix.network.packet.play.client.CTelegramAuth;
import club.abelix.network.packet.play.server.SProxyPlayerLoginResponse;
import club.abelix.network.type.mode.Lobby;
import club.abelix.network.type.telegram.SecurityUser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.text.Component;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class AuthManager {

    private final StarshipVelocity starship;
    private final VelocityMetricsStorage metricsStorage;

    /**
     * Для проверки лицензии игрока
     */
    private final Cache<String, String> licenseCheckCache = Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.MINUTES)
            .build();

    private final Set<AuthTaskUnit> authTasksSet = Collections.synchronizedSet(new HashSet<>());

    public static CompletableFuture<RegisteredServer> getServerToConnect(Gamer gamer) {
        var proxyServer = StarshipVelocity.getInstance().getProxy();
//        ServerMode serverMode = Lobby.HUB;
//        String lastServer = null;
//
//        Session session = gamer.getLastSession();
//        if (session != null) {
//            SessionServer sessionServer = session.getLastServer();
//            if (sessionServer != null) {
//                lastServer = sessionServer.getName();
//            }
//        }
//
//
//        if (lastServer != null && (!lastServer.startsWith("login") || !lastServer.startsWith("limbo"))) {
//            Master master = Master.getByPrefix(lastServer.split("-")[0]);
//            if (master == null) {
//                var serverInfo = proxyServer.getServer(lastServer).orElse(null);
//                Uncommon uncommonServer = Uncommon.getByName(lastServer.split("-")[0]);
//                if (uncommonServer == null && serverInfo != null) {
//                    return Connection.awaitPacket(new CServerConnectRequest(gamer.getPlayerId(), lastServer))
//                            .thenApply(response -> proxyServer.getServer(response.getNewServer()).orElse(null))
//                            .exceptionally(response -> ServerUtil.getRandomServer(Lobby.HUB))
//                            .exceptionally(response -> ServerUtil.getRandomLimbo());
//                }
//            } else {
//                serverMode = master.getLobby();
//            }
//        }
//
//        final ServerMode fServerMode = serverMode;

        return ServerUtil.getBestServer(gamer, Lobby.HUB, false).thenCompose(response -> {
            if (response.isEmpty()) {
                return CompletableFuture.completedFuture(ServerUtil.getRandomLimbo());
            }

            return CompletableFuture.completedFuture(proxyServer.getServer(response.orElse(null)).orElse(null));
        });
    }

    public void initialize() {
        starship.registerCommand(new LoginCommand(starship));
        starship.registerCommand(new RegisterCommand(starship));
        starship.registerCommand(new LogoutCommand());
        starship.registerCommand(new ChangePassCommand());

        starship.registerCommand(new LicenseCommand(this));
       // starship.registerCommand(new TelegramCommand());

        val eventManager = starship.getProxy().getEventManager();

        eventManager.register(starship, new AuthTwoFactorListener(starship));

        starship.getProxy().getScheduler().buildTask(starship, () -> {
            for (val gamer : Gamer.getGamers()) {
                if (gamer.getPlayer() == null || gamer.isLoggedIn()) {
                    continue;
                }

                val language = gamer.getLanguage();

                if (gamer.isTooLongLogin()) {
                    log.info("Kicking gamer {} for too long authentication", gamer.getName());

                    if (gamer.getPlayer() != null) {
                        gamer.getPlayer().disconnect(
                                Component.text(language.getMessage("auth_idle_too_long")));
                    } else {
                        Gamer.removeGamer(gamer);
                    }
                    continue;
                }

                if (!gamer.isRegistered()) {
                    gamer.sendLocalizedMessage("auth_idle_registration");
                    gamer.sendTitle(
                            language.getMessage("auth_idle_register_title"),
                            language.getMessage("auth_idle_register_subtitle"));
                } else if (gamer.isTwoFactorRequested()) {
                    gamer.sendLocalizedMessage("auth_idle_telegram");
                    gamer.sendTitle(
                            language.getMessage("auth_idle_telegram_title"),
                            language.getMessage("auth_idle_telegram_subtitle"));
                } else {
                    gamer.sendLocalizedMessage("auth_idle_login");
                    gamer.sendTitle(
                            language.getMessage("auth_idle_login_title"),
                            language.getMessage("auth_idle_login_subtitle"));
                }
            }
        }).repeat(1, TimeUnit.SECONDS).schedule();

        starship.getProxy().getScheduler().buildTask(starship, () -> {
            Set<AuthTaskUnit> completedTasks = new HashSet<>();

            for (val task : authTasksSet) {
                if (task.getSeconds() == 0) {
                    try {
                        task.getRunnable().run();
                    } catch (Exception any) {
                        log.error("Error while executing auth task {}: {}",
                                task.getTaskIdentification(), any.getMessage());
                    } finally {
                        completedTasks.add(task);
                    }
                } else if (task.getSeconds() < 0) {
                    completedTasks.add(task);
                    continue;
                }
                task.setSeconds(task.getSeconds() - 1);
            }

            for (val completedTask : completedTasks) {
                authTasksSet.remove(completedTask);
            }
        }).repeat(1, TimeUnit.SECONDS).schedule();
    }

    public void addTask(AuthTaskUnit authTaskUnit) {
        authTasksSet.remove(authTaskUnit);

        authTasksSet.add(authTaskUnit);
    }

    public void addTask(String username, Runnable runnable, int seconds) {

        addTask(new AuthTaskUnit(
                username,
                runnable, seconds));
    }

    public void afterAuthenticationStage(final @NonNull Gamer gamer) {
        Player player = gamer.getPlayer();

        afterLoginStage(gamer, gamer.getPlayer()).thenApply(response -> {
            ActionManager.save(player.getUsername(), gamer.getAddress().getHostAddress(), ActionType.LOGIN);

            RegisteredServer firstServer = gamer.getFirstServer();

            if (firstServer == null) {
                firstServer = ServerUtil.getRandomLimbo();
            }

            if (firstServer == null) {
                player.disconnect(Component.text("Limbo server offline!"));
                return null;
            }

            val firstName = firstServer.getServerInfo().getName();

            player.createConnectionRequest(firstServer).connect().whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("[First] Error while connecting gamer({}) to server {}: {}",
                            player.getUsername(), firstName, throwable.getMessage());
                    return;
                }

                log.info("Gamer({}) connected to server {} successfully",
                        player.getUsername(), firstName);
            });

            return response;
        });
    }

    public void afterRegisterStage(final @NonNull Gamer gamer) {
        Player player = gamer.getPlayer();

        afterLoginStage(gamer, gamer.getPlayer()).thenApply(response -> {
            ActionManager.save(player.getUsername(), gamer.getAddress().getHostAddress(), ActionType.LOGIN);

            log.info("Kicking gamer {} after successful registration", gamer.getName());

            gamer.getPlayer().disconnect(
                    Component.text(gamer.getLanguage().getMessage("auth_registration_success_kick"))
            );

            return response;
        });
    }


    public CompletableFuture<SProxyPlayerLoginResponse> afterLoginStage(
            @NonNull Gamer gamer, @NonNull Player connection) {

        String name = gamer.getName();
        InetAddress address = connection.getRemoteAddress().getAddress();

        log.info("Starting AfterLoginStage for player {}. Getting server to connect...", name);

        return getServerToConnect(gamer).whenComplete((server, x) -> {
            if (x != null) {
                log.error("Error while getting server to connect for gamer {}: {}",
                        gamer.getName(), x.getMessage());

                metricsStorage.incrementAuthFailures();
                connection.disconnect(Component.text(
                        "§cNo servers found to connect to"
                ));

                return;
            }

            if (server != null) {
                log.info("Found server {} to connect player {}", server.getServerInfo().getName(), name);
            } else {
                metricsStorage.incrementAuthFailures();
            }
            gamer.setFirstServer(server);
        }).thenCompose(info -> {
            if (info == null) {
                log.error("No server found to connect for gamer {}", gamer.getName());
                return CompletableFuture.completedFuture(null);
            }

            CProxyPlayerLoginRequest login = new CProxyPlayerLoginRequest(
                    name, address, gamer.getDomain(), info.getServerInfo().getName(),
                    gamer.getProtocol(), CProxyPlayerLoginRequest.FINISH
            );

            log.info("Sending CProxyPlayerLoginRequest (FINISH) for player {} to Core. Target server: {}", name, info.getServerInfo().getName());

            return Connection.awaitPacket(login).whenComplete((response, cause) -> {
                if (cause != null) {
                    log.error("Error while AfterLoginStage (FINISH) for gamer {}: {}",
                            gamer.getName(), cause);

                    metricsStorage.incrementAuthFailures();
                    connection.disconnect(Component.text(
                            "§cNo data loaded AfterLoginStage\n§cCode: " + cause
                    ));

                    return;
                }

                if (response.isCancelled()) {
                    log.warn("Core cancelled FINISH login for player {}. Reason: {}",
                            gamer.getName(), response.getMessage());

                    metricsStorage.incrementAuthFailures();
                    connection.disconnect(Component.text(response.getMessage()));
                    return;
                }

                log.info("Core accepted FINISH login for player {}. Setting connectedToCore=true.", name);
                gamer.setConnectedToCore(true);
            });
        });
    }

    public boolean isCheckingLicense(@NonNull final String playerName) {
        licenseCheckCache.cleanUp();

        return licenseCheckCache.getIfPresent(playerName.toLowerCase()) != null;
    }

    public void setCheckingLicenseStatus(@NonNull String playerName, boolean status) {
        _setCheckingLicenseStatus(playerName, status);
        Connection.sendPacket(new BLicenseStatus(playerName, status));
    }

    public void _setCheckingLicenseStatus(@NonNull String playerName, boolean status) {
        if (status) {
            licenseCheckCache.cleanUp();
            licenseCheckCache.put(playerName.toLowerCase(), "license-checking");

            log.info("License checking enabled for " + playerName);
            log.info("-- After next login with license account he will be set as premium user in db --");
        } else {
            licenseCheckCache.invalidate(playerName.toLowerCase());
        }
    }

    public void send2faPacket(@NonNull Gamer gamer) {
        if (!Connection.isConnected()) {
            gamer.getPlayer().disconnect(Component.text("§cConnection to the authentication service is lost, please rejoin..."));
            return;
        }

        val playerId = gamer.getPlayerId();
        val address = gamer.getAddress();
        val playerName = gamer.getName();

        val telegramId = gamer.getData().getTelegramId();
        val languageId = gamer.getLanguage().getId();

        val proxyName = StarshipVelocity.getInstance().getProxyName();

        Connection.sendPacket(new CTelegramAuth(
                new SecurityUser(playerId, address, telegramId, languageId, playerName, proxyName)
        ));

        gamer.setTwoFactorRequested(true);
    }

}
