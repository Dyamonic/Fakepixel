package club.abelix.core.velocity.listener.player;

import club.abelix.commons.data.action.ActionManager;
import club.abelix.commons.data.action.ActionType;
import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.commons.version.GameVersion;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.auth.bans.AuthBanManager;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.event.GamerLoginEvent;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.core.velocity.util.Constants;
import club.abelix.core.velocity.util.HostnameUtils;
import club.abelix.core.velocity.util.LanguageUtil;
import club.abelix.network.packet.play.client.CProxyPlayerLoginRequest;
import club.abelix.network.session.PlayerRedisSessionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.text.Component;

import java.net.InetAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>PlayerConnectionListener creation on 02.03.2023 at 02:09</p>
 */

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerConnectionListener {

    private static final String[] BLOCKED_NICKNAME = new String[]{
            "admin", "administrator", "moderator",
            "moder", "mod", "helper", "hlp",
            "hlpr", "hlper", "hlp", "hper", "hplr",
            "core", "console", "system", "server",
            "owner", "fakepixel", "bareapi_", "api"
    };


    StarshipVelocity starship;
    PlayerRedisSessionService sessionService;
    Map<String, Gamer> loadedData = new ConcurrentHashMap<>();

    /**
     * Кэш, используемый при асинхронной загрузке данных об игроке Именно там в AuthPlayerData будет ставится online
     * mode status
     * <p>
     * Больше 30 секунд хранить чет тут нам не нужно, потому что либо игрока за это время кикнет, либо он уже загрузится
     * Либо сервер просто не работает...
     * <p>
     */
    private final Cache<String, Boolean> premiumStatusCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .initialCapacity(100)
            .build();

    @Subscribe(priority = 100)
    public EventTask onPreLogin(PreLoginEvent event) {
        Language language = LanguageUtil.getLanguage(starship.getGeoLocationManager(), event.getConnection());

        for (String blockedNickname : BLOCKED_NICKNAME) {
            if (event.getUsername().equalsIgnoreCase(blockedNickname)) {
                log.info("User {} tried to connect with blocked nickname", event.getUsername());
                event.setResult(PreLoginEvent.PreLoginComponentResult
                        .denied(Component.text(language.getMessage("proxy_blocked_nick"))));
                return null;
            }
        }

        if (!starship.getJoinable()) {
            log.warn("User {} tried to connect while server is not joinable", event.getUsername());

            event.setResult(PreLoginEvent.PreLoginComponentResult
                    .denied(Component.text(language.getMessage("proxy_try_later"))));
            return null;
        }

        if (!Connection.isConnected()) {
            log.warn("User {} tried to connect while core is not connected", event.getUsername());

            event.setResult(PreLoginEvent.PreLoginComponentResult
                    .denied(Component.text(language.getMessage("core_not_connected"))));

            return null;
        }

        val connection = event.getConnection();
        val playerName = event.getUsername();

        log.info("Raw username: [{}], length={}", playerName, playerName.length());
        playerName.codePoints().forEach(cp ->
                log.info("username cp=U+{}", Integer.toHexString(cp).toUpperCase())
        );

        if (!Patterns.validateNickName(playerName)) {
            log.info("User {} tried to connect with incorrect nickname", event.getUsername());

            event.setResult(PreLoginEvent.PreLoginComponentResult
                    .denied(Component.text(language.getMessage("proxy_incorrect_nick"))));
            return null;
        }

        if (sessionService.isSessionOpened(playerName)) {
            log.warn("User {} tried to connect, but session is already opened in Redis. Denying login.", playerName);

            event.setResult(PreLoginEvent.PreLoginComponentResult
                    .denied(Component.text(language.getMessage("proxy_already_connected"))));
            return null;
        }

        val playerAddress = connection.getRemoteAddress().getHostString();

        if (!AuthBanManager.isBanExpired(playerAddress)) {
            log.info("User {} tried to connect from banned IP {}", event.getUsername(), playerAddress);

            event.setResult(PreLoginEvent.PreLoginComponentResult
                    .denied(Component.text(language.getMessage("proxy_ip_block"))));
            return null;
        }

        if (connection.getProtocolVersion().getProtocol() < GameVersion.MINIMUM_VERSION.getVersion()) {
            log.warn("User {} tried to connect with old version {}", event.getUsername(),
                    connection.getProtocolVersion().getName());

            event.setResult(PreLoginEvent.PreLoginComponentResult
                    .denied(Component.text(language.getMessage("proxy_client_old_version", "%minimal_version%", GameVersion.MINIMUM_VERSION.getName()))));
            return null;
        }

        if (connection.getProtocolVersion().getProtocol() > GameVersion.MAXIMUM_VERSION.getVersion()) {
            log.warn("User {} tried to connect with unsupported version {}", event.getUsername(),
                    connection.getProtocolVersion().getName());

            event.setResult(PreLoginEvent.PreLoginComponentResult
                    .denied(Component.text(language.getMessage("proxy_old_version", "%max_version%", GameVersion.MAXIMUM_VERSION.getMostRecentSupportedVersionName()))));
            return null;
        }

        if (!Patterns.validateNickName(playerName)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult
                    .denied(Component.text(language.getMessage("proxy_incorrect_nick"))));

            return null;
        }

        if (starship.getAuthManager().isCheckingLicense(playerName)) {
            log.info("Forced licence for {}", playerName);

            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            return null;
        }

        premiumStatusCache.cleanUp();

        val prem = premiumStatusCache.getIfPresent(playerName.toLowerCase());

        if (prem != null) {
            event.setResult(prem ? PreLoginEvent.PreLoginComponentResult.forceOnlineMode()
                    : PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
            return null;
        }

        val future = AuthSqlHandler.INSTANCE.isOnlineMode(playerName).thenAccept(premium -> {
            event.setResult(premium ? PreLoginEvent.PreLoginComponentResult.forceOnlineMode()
                    : PreLoginEvent.PreLoginComponentResult.forceOfflineMode());

            premiumStatusCache.put(playerName.toLowerCase(), premium);
        });

        return EventTask.resumeWhenComplete(future);
    }

    // Здесь отправляется пакет на кор о том, что игрок зашёл на сервер
    // (его не добавляет в кор)
    @Subscribe
    public EventTask onLogin(LoginEvent event) {
        if (!event.getResult().isAllowed()) {
            return null;
        }

        val connection = event.getPlayer();

        val name = connection.getUsername();
        val address = connection.getRemoteAddress().getAddress();

        try {
            log.info("Loading data for player {}", name);

            return gamerLoad(event, name, address);
        } catch (ExecutionException | InterruptedException | TimeoutException exception) {
            log.error("Failed to load data for player {}", name, exception);

            val language = LanguageUtil.getLanguage(starship.getGeoLocationManager(), event.getPlayer());

            event.setResult(
                    ResultedEvent.ComponentResult.denied(
                            Component.text(language.getMessage("gamer_failed_data_load"))
                    )
            );
        }

        return null;
    }

    @Subscribe(priority = Short.MIN_VALUE)
    public void onPostLogin(PostLoginEvent e) {
        val player = e.getPlayer();
        val gamer = loadedData.remove(player.getUsername().toLowerCase());

        if (gamer == null) {
            log.warn("Gamer data for {} not found in loadedData during PostLoginEvent. Disconnecting player.", player.getUsername());
            player.disconnect(Component.text(Language.defaultLanguage().getMessage("gamer_failed_data_load")));
            return;
        }

        if (!player.isActive()) {
            log.warn("User {} disconnected before completing PostLoginEvent. Cleaning up gamer data.", player.getUsername());

            gamer.disconnect();
            return;
        }

        log.info("Gamer {} successfully passed PostLoginEvent, adding to online gamers and starting Redis session.", player.getUsername());
        Gamer.addGamer(gamer);

        sessionService.start(
                player.getUsername(),
                Optional.ofNullable(starship.getProxyName()).orElse("N/A")
        ).ifPresentOrElse(
                session -> {
                    log.info("Redis session started for player {} on proxy {}", player.getUsername(), starship.getProxyName());
                    gamer.setRedisSession(session);
                },
                () -> {
                    log.error("Failed to start Redis session for player {}: session already exists!", player.getUsername());
                    player.disconnect(Component.text("Failed to open session: session already exists in Redis."));
                });
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent e) {
        val name = e.getPlayer().getUsername();
        log.info("Player {} disconnected from proxy! Login status: {}", name, e.getLoginStatus().name());

        loadedData.remove(name.toLowerCase());

        if (e.getLoginStatus() == DisconnectEvent.LoginStatus.CONFLICTING_LOGIN) {
            log.info("Player {} disconnected due to CONFLICTING_LOGIN. Not closing Redis session to prevent kicking the NEW session.", name);
            return;
        }

        val gamer = Gamer.getGamer(name);
        if (gamer == null) {
            log.error("No Gamer object found for {} during disconnect. Possibly didn't finish login.", name);
            return;
        }

        log.info("Closing Redis session for {} and saving data.", name);
        if (!sessionService.closeSession(name)) {
            log.warn("Failed to close player {} session in Redis (might already be closed or expired)", name);
        }

        gamer.changeLastServer(null);
        gamer.saveCurrentSession();

        gamer.disconnect();
    }

    @Subscribe
    public EventTask onGamerLogin(GamerLoginEvent event) {
        val player = event.getGamer().getPlayer();
        val gamer = event.getGamer();
        var name = player.getUsername();

        if (!player.isActive()) {
            log.warn("User {} disconnected before completing login", player.getUsername());
            return null;
        }

        // После загрузки данных ещё есть вероятность, что логин евент
        // отменится и игрок не попадёт на сервер. Потому нельзя сразу
        // кидать данные в AuthBanManager, сначала нужно убедиться, что игрок
        // точно зашёл (то есть вызвался PostLoginEvent)
        this.loadedData.put(player.getUsername().toLowerCase(), gamer);

        starship.getAuthManager().addTask(player.getUsername().toLowerCase(), () -> {
            if (loadedData.remove(player.getUsername().toLowerCase(), gamer)) {
                log.warn("User not connected to server {}, disconnecting...", player.getUsername());
                gamer.disconnect();
            }
        }, 10);

        name = name.toLowerCase();

        val authManager = starship.getAuthManager();
        var premiumAuth = authManager.isCheckingLicense(name);

        authManager.setCheckingLicenseStatus(name, false);

        if (premiumAuth) {
            gamer.setPremiumAuth(true);

            AuthSqlHandler.INSTANCE.setPremiumStatus(gamer.getPlayerId(), true);
        } else {
            premiumAuth = gamer.isPremiumAuth();
        }

        val session = gamer.getLastSession();
        val address = gamer.getAddress();

        val need2faConfirmation = StarshipVelocity.getInstance().isTwoFactorEnabled() &&
                (gamer.has2FA() && gamer.isSettingSet(Setting.TFA_CONFIRMATION_EVERY_LOGIN));

        if (premiumAuth || (session != null && (address.equals(session.getAddress()) && !gamer.isSessionExpired()))) {
            if (!premiumAuth && need2faConfirmation) {
                authManager.send2faPacket(gamer);
                return null;
            }

            gamer.sessionLogin();
            gamer.login();

            ActionManager.save(gamer.getName(), gamer.getAddress().getHostAddress(), premiumAuth ? ActionType.PREMIUM_LOGIN : ActionType.SESSION_LOGIN);
            log.info("User {} logged in using {}", gamer.getName(),
                    premiumAuth ? "premium authentication" : "session authentication");
        }

        if (gamer.getPlayerId() < 0) {
            gamer.changeLanguage(LanguageUtil.getLanguage(starship.getGeoLocationManager(), player), false);
        } else if (gamer.isLoggedIn()) {
            val error = Component.text(gamer.getLanguage().getMessage("proxy_auth_error"));
            val future = authManager.afterLoginStage(gamer, player).whenComplete((sProxyPlayerLoginResponse, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to load gamer {} after login", player.getUsername(), throwable);
                    event.setResult(ResultedEvent.ComponentResult.denied(error));
                }
            });

            return EventTask.resumeWhenComplete(future);
        }

        return null;
    }

    private EventTask gamerLoad(@NonNull LoginEvent event, @NonNull String name, @NonNull InetAddress address) throws ExecutionException, InterruptedException, TimeoutException {
        val player = event.getPlayer();
        val loginRequest = new CProxyPlayerLoginRequest(
                name, address, null, null, -1, CProxyPlayerLoginRequest.VERIFY
        );

        val language = LanguageUtil.getLanguage(starship.getGeoLocationManager(), player);

        val failedComponent = Component.text(language.getMessage("proxy_failed_load_data"));

        if (!player.isActive()) {
            log.info("User {} disconnected before data load", player.getUsername());

            event.setResult(ResultedEvent.ComponentResult.denied(failedComponent));
            return null;
        }

        var domain = HostnameUtils.cleanVhost(event.getPlayer().getVirtualHost().orElse(null));

        if (domain == null || domain.length() > 32) {
            domain = "unknown.agerapvp.club";
        }

        if (!Constants.UNKNOWN_HOST && !domain.contains("agerapvp.club")) {
            log.warn("User {} tried to connect through another domain: {}", player.getUsername(), domain);

            event.setResult(ResultedEvent.ComponentResult.denied(Component.text(language.getMessage("auth_another_domain"))));
            return null;
        }

        val gamer = new Gamer(name, player.getProtocolVersion().getProtocol(), domain);

        gamer.setPlayer(player);
        gamer.setAddress(player.getRemoteAddress().getAddress());

        log.info("User {}/{}  joined from {} domain and started loading process", name, address.getHostAddress(), domain);

        val loginFuture = Connection.awaitPacket(loginRequest).whenCompleteAsync((response, cause) -> {
            if (cause != null || response.isCancelled()) {
                log.warn("Core cancelled loading process for player {}", name);
                log.error("Cancel reason:", cause);

                val component = response.getMessage() != null
                        ? Component.text(response.getMessage())
                        : Component.text("Core cancelled loading process");

                val username = event.getPlayer().getUsername();

                loadedData.remove(username.toLowerCase());
                event.setResult(ResultedEvent.ComponentResult.denied(component));
            }
        });

        val gamerFuture = gamer.load().thenApplyAsync((loadedStatus) -> {
            if (!loadedStatus) {
                throw new RuntimeException(String.format("Failed gamer %s load!", player.getUsername()));
            }

            return starship.getProxy().getEventManager().fire(new GamerLoginEvent(gamer)).join();
        }).whenComplete((gamerLoginEvent, throwable) -> {
            if (!player.isActive()) {
                log.warn("User {} disconnected during data load", player.getUsername());

                event.setResult(ResultedEvent.ComponentResult.denied(failedComponent));
                gamer.disconnect();
                return;
            }

            if (throwable != null) {
                log.error("Error during gamer {} load", player.getUsername(), throwable);

                event.setResult(ResultedEvent.ComponentResult.denied(failedComponent));
                gamer.disconnect();
                return;
            }

            if (gamerLoginEvent.getResult().isAllowed()) {
                log.info("User {} successfully loaded", player.getUsername());

                return;
            }

            Component reason;

            event.setResult(ResultedEvent.ComponentResult.denied(failedComponent));

            if (gamerLoginEvent.getResult().getReasonComponent().isEmpty()) {
                reason = Component.text("§сAn unknown error has occurred, please contact the administration");
            } else {
                reason = gamerLoginEvent.getResult().getReasonComponent().get();
            }

            event.setResult(ResultedEvent.ComponentResult.denied(reason));

            log.warn("User {} failed to load: {}", player.getUsername(), reason);
        });

        return EventTask.resumeWhenComplete(CompletableFuture.allOf(loginFuture, gamerFuture));
    }
}
