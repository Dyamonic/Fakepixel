package club.abelix.core.velocity.gamer;

import club.abelix.commons.data.auth.AuthSqlHandler;
import club.abelix.commons.data.auth.BaseAuthPlayerData;
import club.abelix.commons.data.friend.FriendSqlHandler;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.data.permission.prefix.defaults.RankPrefixModifier;
import club.abelix.commons.data.prefix.Prefix;
import club.abelix.commons.data.prefix.PrefixSqlHandler;
import club.abelix.commons.data.property.PropertySqlHandler;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.data.session.Session;
import club.abelix.commons.data.session.SessionServer;
import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.data.skin.SkinsSqlHandler;
import club.abelix.commons.data.skin.exception.SkinException;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.language.Language;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.map.CachingIndexMap;
import club.abelix.commons.util.map.IndexMap;
import club.abelix.commons.util.password.PasswordHashing;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.command.ProxiedCommandSender;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.skin.SkinsManager;
import club.abelix.core.velocity.util.LanguageUtil;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.both.BRemoveProperty;
import club.abelix.network.packet.play.both.BSetProperty;
import club.abelix.network.packet.play.client.CPlayerConnected;
import club.abelix.network.packet.play.client.CProxyPlayerDisconnect;
import club.abelix.network.packet.play.client.CSendPlayerFailed;
import club.abelix.network.packet.play.client.CServerConnectRequest;
import club.abelix.network.session.PlayerSession;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import gnu.trove.TCollections;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Getter
public class Gamer extends ProxiedCommandSender implements IGamer {

    private static final IndexMap<Gamer> GAMERS = IndexMap.<Gamer>newMap()
            .registerIndex(String.class, Gamer::getLowerName)
            .registerIntIndex(Gamer::getPlayerId);
    private static final CachingIndexMap<Gamer> OFFLINE_GAMERS = IndexMap.<Gamer>newCachingMap(30, TimeUnit.MINUTES)
            .registerIndex(String.class, Gamer::getLowerName)
            .registerIntIndex(Gamer::getPlayerId);

    private final BaseAuthPlayerData data;
    private final String domain;
    private final int protocol;
    private final String name;
    private final long loginTimestamp = System.currentTimeMillis();

    @Getter
    private final AtomicLong lastExecution = new AtomicLong(System.currentTimeMillis());

    private final long createdAt = System.currentTimeMillis();
    private volatile int playerId;
    private volatile Player player;
    private volatile TIntSet friendIds;
    private volatile String displayName;

    @Setter
    private volatile Prefix prefix;

    private volatile TObjectIntMap<String> properties;
    private volatile boolean loggedIn;

    @Setter
    private volatile RegisteredServer firstServer;

    @Setter
    private volatile boolean firstServerConnected;

    @Setter
    private volatile boolean connectedToCore;

    @Setter
    private volatile boolean twoFactorRequested;

    @Setter
    private volatile InetAddress address;

    @Setter
    private Session currentSession;

    @Setter
    private SessionServer currentServer;

    @Getter(AccessLevel.NONE)
    private int passwordErrors;

    @Setter
    @Getter
    private UserRanks ranks;

    @Getter
    @Setter
    private PlayerSession redisSession;

    public Gamer(@NonNull String name, int protocol, String domain) {
        this.name = name;
        this.data = new BaseAuthPlayerData(name);

        this.protocol = protocol;
        this.domain = domain;
    }

    public static Collection<Gamer> getGamers() {
        return new HashSet<>(GAMERS.values());
    }

    public static Collection<Gamer> getNonVanishedGamers() {
        return getGamers().stream()
                .filter(gamer -> !gamer.isVanished())
                .toList();
    }

    public static Gamer getGamer(Player player) {
        return getGamer(player.getUsername());
    }

    public static Optional<Gamer> getGamerOptional(Player player) {
        return Optional.ofNullable(getGamer(player.getUsername()));
    }

    public static Gamer getGamer(String name) {
        return GAMERS.get(String.class, name.toLowerCase());
    }

    public static Optional<Gamer> getGamerOptional(String name) {
        return Optional.ofNullable(getGamer(name));
    }

    public static Gamer getGamer(int playerID) {
        return GAMERS.get(playerID);
    }

    public static Optional<Gamer> getGamerOptional(int playerID) {
        return Optional.ofNullable(getGamer(playerID));
    }

    public static Gamer getOrCreate(String name, Consumer<Gamer> gamerConsumer) {
        Gamer gamer = getGamer(name);

        if (gamer != null) {
            return gamer;
        }

        //иначе загружаем ID по нику и загружаем оффлайн-игрока
        //даже если игрок с таким ID уже есть (оффлайн игрок есть),
        //это не проблема - у networksqlhandler свой кэш
        return getOrCreate(NetworkSqlHandler.INSTANCE.getPlayerId(name), gamerConsumer);
    }

    public static Gamer getOrCreate(int playerId, Consumer<Gamer> gamerConsumer) {
        //игрока с таким id нет
        if (playerId < 0) {
            return null;
        }

        Gamer gamer = getGamer(playerId);

        if (gamer != null) {
            return gamer;
        }

        OFFLINE_GAMERS.cleanUp();

        //получаем оффлайн-игрока по этому id
        gamer = getOfflineGamer(playerId);

        //если этого игрока нет - загружаем нового геймера и добавляем его в список оффлайн
        //при этом берем его ник из БД еще раз (или из кэша), чтобы он был в норм регистре
        if (gamer == null) {
            String playerName = NetworkSqlHandler.INSTANCE.getPlayerName(playerId, true);

            if (playerName == null) {
                return null;
            }

            gamer = new Gamer(playerName, -1, null);

            OFFLINE_GAMERS.store(gamer);
        }

        gamerConsumer.accept(gamer);

        return gamer;
    }

    public static Gamer getOfflineGamer(@NonNull String name) {
        return OFFLINE_GAMERS.get(String.class, name.toLowerCase());
    }

    public static Gamer getOfflineGamer(int playerId) {
        return OFFLINE_GAMERS.get(playerId);
    }

    public static void addGamer(Gamer gamer) {
        OFFLINE_GAMERS.remove(gamer);
        GAMERS.store(gamer);
    }

    public static void removeGamer(Gamer gamer) {
        GAMERS.remove(gamer);
    }

    public void changeLastServer(String serverName) {
        LocalDateTime timestamp = LocalDateTime.now();

        if (currentServer != null) {
            currentServer.setEndTimestamp(timestamp);

            if (currentSession != null) {
                SessionSqlHandler.INSTANCE.addSessionServer(currentSession.getSessionId(), currentServer);
            }
        }

        if (serverName != null) {
            currentServer = new SessionServer(serverName, timestamp, timestamp);
        }
    }

    public void sendTo(@NotNull RegisteredServer target,
                       @Nullable Runnable successConnect,
                       @Nullable BiConsumer<ConnectionRequestBuilder.Status, Optional<Component>> failConnect) {

        String targetServerName = target.getServerInfo().getName();

        ServerConnection currentlyConnect = player.getCurrentServer().orElse(null);
        SessionServer currentlyConnectSession = currentServer;

        String currentConnectServerName; // Имя текущего сервера с учётом арен
        if (currentlyConnectSession != null) {
            currentConnectServerName = currentlyConnectSession.getName();
        } else if (currentlyConnect != null) {
            currentConnectServerName = currentlyConnect.getServerInfo().getName();
        } else {
            currentConnectServerName = null;
        }

        // Проверяем может игрок уже подключен к этому серверу
        if (currentlyConnectSession != null &&
                currentConnectServerName.equalsIgnoreCase(target.getServerInfo().getName())) {
            if (failConnect != null)
                failConnect.accept(ConnectionRequestBuilder.Status.ALREADY_CONNECTED, Optional.empty());
            return;
        }

        // обрабатываем сабсервера
        if (currentlyConnect != null
                && Objects.equals(currentlyConnect.getServerInfo().getAddress(), target.getServerInfo().getAddress())) {
            Connection.awaitPacket(new CServerConnectRequest(playerId, targetServerName)) // отправляем пакет о запросе подключения
                    .whenCompleteAsync((response, cause) -> {
                        if (cause != null) {
                            Logging.IMP.warn("[{}] Subserver request failed!. Details:", name);
                            cause.printStackTrace();
                            if (failConnect != null)
                                failConnect.accept(ConnectionRequestBuilder.Status.SERVER_DISCONNECTED,
                                        Optional.empty());
                            return;
                        }

                        if (response.isCancelled()) { // Кор запретил подключатсья
                            if (failConnect != null)
                                failConnect.accept(ConnectionRequestBuilder.Status.CONNECTION_CANCELLED,
                                        Optional.empty());
                            return;
                        }

                        Logging.IMP.info("[{}] Redirected to subserver [{} -> {}]",
                                name, currentConnectServerName, targetServerName);

                        Connection.sendPacket(new CPlayerConnected(playerId, targetServerName)); // Отправляем кору, что всё ок, подключаемся
                        changeLastServer(targetServerName);

                        if (successConnect != null) successConnect.run();
                    });
            return;
        }

        player.createConnectionRequest(target).connect().whenCompleteAsync((result, throwable) -> {
            if (throwable != null) {
                Logging.IMP.error("[{}] Failed send server {}. Details:", name, targetServerName);
                throwable.printStackTrace();
                if (failConnect != null)
                    failConnect.accept(ConnectionRequestBuilder.Status.CONNECTION_CANCELLED, Optional.empty());
            }

            switch (result.getStatus()) {
                case SUCCESS, CONNECTION_IN_PROGRESS -> {
                    if (successConnect != null) successConnect.run();
                }
                case ALREADY_CONNECTED, CONNECTION_CANCELLED, SERVER_DISCONNECTED -> {
                    if (failConnect != null) failConnect.accept(result.getStatus(), result.getReasonComponent());
                }
            }
        });
    }

    public void sendFailedConnect(String reason) {
        Connection.sendPacket(new CSendPlayerFailed(playerId));
        sendLocalizedMessage("gamer_server_failed_connection", "%reason%", reason);
    }

    public void setSkin(SkinsManager skins, String owner) throws SkinException {
        if (!Patterns.isMojangNick(owner)) {
            return;
        }

        Skin skin = SkinsApi.getCachedSkin(owner);

        SkinsSqlHandler.INSTANCE.setCustomSkin(playerId, owner);
        SkinsSqlHandler.INSTANCE.addSkinHistory(playerId, owner, LocalDateTime.now());
        SkinsSqlHandler.INSTANCE.trimSkinHistory(playerId, 7);

        skins.setSkin(player, skin);
    }

    public void resetSkin(SkinsManager skins) {
        SkinsSqlHandler.INSTANCE.removeCustomSkin(playerId);

        if (isOnline()) {
            try {
                Skin skin = SkinsApi.getCachedSkin(name);

                skins.setSkin(player, skin);
            } catch (SkinException e) {
                skins.setSkin(player, SkinsApi.DEFAULT_SKIN);
                sendLocalizedMessage("skin_reset_failed", "%message%", getLanguage().getMessage(e.getMessage()));
            }
        }
    }

    public void register(@NonNull String pass, Runnable afterRegistration) {
        GAMERS.remove(this);

        data.setLanguage(LanguageUtil.getLanguage(StarshipVelocity.getInstance().getGeoLocationManager(), player));
        data.setPassword(PasswordHashing.getHash(pass));
        data.setRegIp(address.getHostAddress());
        data.setRegDate(System.currentTimeMillis());

        Component error = Component.text("Ошибка при обновлении игровых данных!");
        Consumer<Throwable> exceptionConsumer = (exception) -> {
            Logging.IMP.error("Registration internal error:");
            exception.printStackTrace();
            player.disconnect(error);
        };

        AuthSqlHandler.INSTANCE.createAuth(data).whenComplete((id, authThrowable) -> {
            if (authThrowable != null) {
                Logging.IMP.error("Failed user registration!");
                authThrowable.printStackTrace();
                player.disconnect(error);
                return;
            }

            data.setPlayerId(id);

            playerId = id;

            GAMERS.store(this);

            login();
            afterRegistration.run();

        }).exceptionally(throwable -> {
            exceptionConsumer.accept(throwable);
            return null;
        });
    }

    public boolean checkPassword(@NonNull String password) {
        return PasswordHashing.checkPassword(password, data.getPassword());
    }

    public void sessionLogin() {
        this.loggedIn = true;
    }

    public void login() {
        this.loggedIn = true;

        try {
            currentSession = SessionSqlHandler.INSTANCE.addSession(playerId, LocalDateTime.now(), domain, address)
                    .join();
        } catch (Exception exception) {
            Logging.IMP.error("Failed open gamer {} session", this.getName());
            exception.printStackTrace();

            Language language = this.getLanguage();

            player.disconnect(
                    Component.text(language.getMessage("auth_failed_open_session"))
            );
        }
    }

    public void saveCurrentSession() {
        if (currentSession != null) {
            SessionSqlHandler.INSTANCE.changeSessionEndTime(currentSession.getSessionId(), LocalDateTime.now());
        }
    }

    public boolean incPasswordErrors() {
        return ++this.passwordErrors >= 5;
    }

    public boolean isTooLongLogin() {
        return !this.loggedIn && System.currentTimeMillis() - this.createdAt >= TimeUnit.MINUTES.toMillis(2);
    }

    public void sendCommandUsageMessage(@NonNull String usageKey) {
        sendLocalizedMessage("command_usage", "%usage%", getLanguage().getMessage(usageKey));
    }

    public CompletableFuture<Boolean> load() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                loadAuthData();
                loadLanguage();
                loadProperties();
                loadRanks();
                loadGroupAndPrefix();

                return true;
            } catch (Exception any) {
                Logging.IMP.error("Failed load gamer. Details:");
                any.printStackTrace();

                return false;
            }
        });
    }

    public void loadAuthData() {
        data.load();

        playerId = data.getPlayerId();
    }

    public void loadRanks() {
        val loadedRanks = StarshipVelocity.getInstance().getUserRanksService().findAllUserRanks(getPlayerId());

        if (loadedRanks.isEmpty()) {
            getPlayer().disconnect(Component.text("Unable to load your ranks, please contact support!"));
            return;
        }

        if (loadedRanks.getFirst().rank().getId() == -999) {
            sendLocalizedMessage("core_error_unable_to_load_ranks");
        }

        ranks = new UserRanks();
        ranks.addAllRanks(loadedRanks);

        log.info("Loaded {} ranks for player {}", loadedRanks.size(), getName());
    }

    public void setPlayer(@NonNull Player player) {
        this.player = player;
    }

    public boolean isOnline() {
        return player != null && StarshipVelocity.getInstance().getProxy().getPlayer(name).isPresent();
    }

    public CommandSource getCommandSender() {
        return player;
    }

    @Override
    public void sendLocalizedMessage(@NonNull String text, Object... o) {
        sendMessage(getLanguage().getMessage(text, o));
    }

    @Override
    public void sendMessage(final String text) {
        Component msg;

        if ((text.startsWith("{") && text.endsWith("}"))
                || (text.startsWith("[") && text.endsWith("]"))) {
            msg = GsonComponentSerializer.gson().deserialize(text);
        } else {
            msg = Component.text(text);
        }

        sendMessage(msg);
    }

    public void sendMessage(final Component component) {
        if (player != null) {
            player.sendMessage(component);
        }
    }

    public void sendTitle(@NonNull final String title, @NonNull final String subtitle) {
        sendTitle(title, subtitle, 0, 1, 1);
    }

    public void sendTitle(@NonNull String title, @NonNull String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player != null) {
            player.showTitle(
                    Title.title(
                            Component.text(title),
                            Component.text(subtitle),
                            Title.Times.times(Duration.ofSeconds(fadeIn), Duration.ofSeconds(stay), Duration.ofSeconds(fadeOut)))
            );
        }

    }

    public void sendLocalizedTitle(int fadeIn, int stay, int fadeOut, @NonNull String titleKey, @NonNull String subtitleKey, Object... objects) {
        sendTitle(getLanguage().getMessage(titleKey, objects), getLanguage().getMessage(subtitleKey, objects), fadeIn, stay, fadeOut);
    }

    public void sendLocalizedTitle(@NonNull String titleKey, @NonNull String subtitleKey, Object... objects) {
        sendTitle(getLanguage().getMessage(titleKey, objects), getLanguage().getMessage(subtitleKey, objects));
    }

    @Override
    public void dispatchCommand(String command) {
        if (player != null) {
            StarshipVelocity.getInstance().getProxy().getCommandManager().executeAsync(player, command);
        }
    }

    @Override
    public Language getLanguage() {
        Language language = data.getLanguage();

        return language == null ? Language.defaultLanguage() : language;
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    public void loadPlayerId() {
        if (hasPlayerId() || playerId < 0) {
            return;
        }

        playerId = NetworkSqlHandler.INSTANCE.getPlayerId(name, false);
    }

    public boolean hasPlayerId() {
        return playerId > 0;
    }

    public void loadLanguage() {
        loadPlayerId();

        if (playerId < 0 || data.getLanguage() != null) {
            return;
        }

        data.setLanguage(NetworkSqlHandler.INSTANCE.getPlayerLanguage(playerId));
    }

    public void changeLanguage(@NonNull Language language) {
        changeLanguage(language, true);
    }

    public void changeLanguage(@NonNull Language language, boolean updateOnCore) {
        data.setLanguage(language);

        if (updateOnCore) {
            Connection.updateLanguage(playerId, language);
        }
    }

    public int getVkId() {
        return data.getVkId();
    }

    public boolean has2FA() {
        return data.has2FA();
    }

    public void loadGroupAndPrefix() {
        if (playerId < 0) {
            return;
        }

        loadPrefix();
        updateDisplayName();
    }

    public void loadPrefix() {
        if (playerId < 0 || prefix != null) {
            return;
        }

        prefix = PrefixSqlHandler.INSTANCE.getPrefix(playerId).join();

        if (prefix != null && prefix.isExpired()) {
            prefix = null;

            PrefixSqlHandler.INSTANCE.removePrefix(playerId);
        }
    }

    public void updateDisplayName() {
        String prefixText = ranks.findPriorityRank().rank().getPrefix();
        String customPrefixText = prefix == null ? null : prefix.getText();

        if (customPrefixText != null && !customPrefixText.startsWith("§")) {
            String prefixColor = prefixText.substring(0, 2);
            prefixText = prefixColor + customPrefixText;
        }

        if (Project.CURRENT == Project.FAKEPIXEL) {
            this.displayName = getRankPrefixModifier().modify(prefixText + name);
            ;
        } else {
            this.displayName = prefixText + name;
        }
    }

    public RankPrefixModifier getRankPrefixModifier() {
        if (getProperty("selected-plus-color") >= RankPrefixModifier.VALUES.length) {
            return RankPrefixModifier.VALUES[0];
        }

        return RankPrefixModifier.VALUES[getProperty("selected-plus-color")];
    }

    public void disconnect() {
        removeGamer(this);

        // Игрок может выйти, когда кор стопнут (во время реконнекта например)
        if (Connection.isConnected()) {
            if (playerId < 0) {
                log.error("[Disconnect] gamer {} failed remove from core! (playerId < 0)", player.getUsername());
                return;
            }

            Connection.sendPacket(new CProxyPlayerDisconnect(player.getUsername(), playerId));
            log.info("[Disconnect] gamer {}!", displayName);
            return;
        }

        log.info("[Disconnect] gamer {} disconnected while core not connected!", displayName);
    }

    public boolean isVanished() {
        return getProperty("Vanished") == 1;
    }

    public void loadProperties() {
        loadPlayerId();

        //данные уже загружены
        //онлайн игроки проверку игнорят - данные возможно придется обновлять
        if (!isOnline() && properties != null && !properties.isEmpty()) {
            return;
        }

        properties = PropertySqlHandler.INSTANCE.loadProperties(playerId).join();
    }

    public TObjectIntMap<String> getProperties() {
        return TCollections.unmodifiableMap(properties);
    }

    public boolean hasProperty(@NonNull String property) {
        return properties != null && properties.containsKey(property.toLowerCase());
    }

    @Override
    public int getProperty(@NonNull String property) {
        return properties == null ? 0 : properties.get(property.toLowerCase());
    }

    @Override
    public void addProperty(@NonNull String property, int value) {
        if (properties == null) {
            return;
        }

        this.updateProperty(property, properties.get(property.toLowerCase()) + value);
    }

    @Override
    public void setProperty(@NonNull String property, int value) {
        if (updateProperty(property, value)) {
            Connection.sendPacket(new BSetProperty(playerId, property, value));
        }
    }

    public boolean updateProperty(@NonNull String property, int value) {
        if (properties == null) {
            return false;
        }

        properties.put(property.toLowerCase(), value);
        return true;
    }

    public void dropProperty(@NonNull String property) {
        if (properties != null) {
            properties.remove(property.toLowerCase());
        }
    }

    public void removeProperty(@NonNull String property) {
        dropProperty(property);

        if (hasPlayerId()) {
            Connection.sendPacket(new BRemoveProperty(playerId, property));
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return ranks.hasPermission(permission);
    }

    public int getExperience() {
        return getProperty("Experience");
    }

    public int getLevel() {
        return Leveling.getLevel(getExperience());
    }

    public boolean isSettingSet(@NonNull Setting setting) {
        return (getProperty("Settings") & setting.getMask()) != 0;
    }

    public boolean isSettingSet(@NonNull Setting setting, int settingsValue) {
        return (settingsValue & setting.getMask()) != 0;
    }

    public void setSettingState(@NonNull Setting setting, boolean state) {
        int settings = getProperty("Settings");

        if (state) {
            setProperty("Settings", settings | setting.getMask());
        } else {
            setProperty("Settings", settings & ~setting.getMask());
        }
    }

    public void loadFriends() {
        loadPlayerId();

        friendIds = FriendSqlHandler.INSTANCE.getFriends(playerId).join();
    }

    public void addFriend(int friendId) {
        if (friendIds == null) {
            return;
        }

        friendIds.add(friendId);
    }

    public void removeFriend(int friendId) {
        if (friendIds == null) {
            return;
        }

        friendIds.remove(friendId);
    }

    public boolean isFriend(int friendId) {
        return friendIds != null && friendIds.contains(friendId);
    }

    public TIntSet getFriendIds() {
        if (friendIds == null) {
            return new TIntHashSet();
        }

        return TCollections.unmodifiableSet(friendIds);
    }

    public String getLowerName() {
        return name.toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Gamer)) {
            return false;
        }

        Gamer gamer = (Gamer) o;

        return gamer.playerId == playerId;
    }

    @Override
    public int hashCode() {
        return playerId;
    }

    public String getPassword() {
        return data.getPassword();
    }

    public void setPassword(@NonNull String pass) {
        data.setPassword(PasswordHashing.getHash(pass));

        AuthSqlHandler.INSTANCE.setPassword(name, data.getPassword());
    }

    public long getRegDate() {
        return data.getRegDate();
    }

    public String getRegIp() {
        return data.getRegIp();
    }

    public boolean isPremiumAuth() {
        return data.isPremiumAuth();
    }

    public void setPremiumAuth(boolean premiumAuth) {
        data.setPremiumAuth(premiumAuth);
    }

    public Session getLastSession() {
        return data.getLastSession();
    }

    public boolean hasPlayedBefore() {
        return data.hasPlayedBefore();
    }

    public boolean isRegistered() {
        return data.isRegistered();
    }

    public boolean isSessionExpired() {
        return data.isSessionExpired();
    }
}