package club.abelix.core.app.data.gamer;

import club.abelix.commons.data.auth.BaseAuthPlayerData;
import club.abelix.commons.data.externalpermissions.ExternalPermissionEntry;
import club.abelix.commons.data.externalpermissions.ExternalPermissionsSqlHandler;
import club.abelix.commons.data.friend.FriendSqlHandler;
import club.abelix.commons.data.ignorelist.IgnoreListSqlHandler;
import club.abelix.commons.data.joinmessage.JoinMessage;
import club.abelix.commons.data.joinmessage.JoinMessageSqlHandler;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.data.network.NetworkSqlHandler;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.commons.data.nickname.NickSqlHandler;
import club.abelix.commons.data.permission.prefix.defaults.RankPrefixModifier;
import club.abelix.commons.data.prefix.Prefix;
import club.abelix.commons.data.prefix.PrefixSqlHandler;
import club.abelix.commons.data.property.PropertySqlHandler;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.data.session.Session;
import club.abelix.commons.data.session.SessionSqlHandler;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.commons.data.suffix.color.SuffixColor;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.language.Language;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.core.app.Async;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.app.connection.server.AbstractConnectable;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ArenaServer;
import club.abelix.core.app.connection.server.MiniServer;
import club.abelix.core.app.connection.server.Proxy;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.party.Party;
import club.abelix.core.app.event.gamer.OfflineGamerLoadedEvent;
import club.abelix.core.app.event.gamer.PlayerGameRedirectEvent;
import club.abelix.core.app.event.gamer.PlayerQuitEvent;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.play.both.BChat;
import club.abelix.network.packet.play.both.BProxyKick;
import club.abelix.network.packet.play.both.BRemoveProperty;
import club.abelix.network.packet.play.both.BSetProperty;
import club.abelix.network.packet.play.server.SAddPlayerJoinMessage;
import club.abelix.network.packet.play.server.SAddPlayerSuffix;
import club.abelix.network.packet.play.server.SIgnoreListUpdate;
import club.abelix.network.packet.play.server.SMovePlayerToMini;
import club.abelix.network.packet.play.server.SSendPlayer;
import club.abelix.network.packet.play.server.SSendTitle;
import club.abelix.network.packet.play.server.STablistHeaderFooter;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Uncommon;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gnu.trove.TCollections;
import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TIntSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Getter
public class Gamer implements CommandSender, IGamer {

    private static final Cache<String, Gamer> GAMERS_BY_NAME = Caffeine.newBuilder().build();
    private static final Cache<Integer, Gamer> GAMERS_BY_ID = Caffeine.newBuilder().build();
    private static final Map<InetAddress, Set<Gamer>> GAMERS_BY_IP = new ConcurrentHashMap<>();

    private static final Cache<String, Gamer> OFFLINE_GAMERS_BY_NAME = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();
    private static final Cache<Integer, Gamer> OFFLINE_GAMERS_BY_ID = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    /**
     * Реальный адрес игрока
     */
    private final InetAddress address;
    /**
     * Сервер, на котором он сейчас находится
     */
    private final AtomicReference<String> currentServerName = new AtomicReference<>(null);
    private final AtomicReference<String> currentlyConnectingServerName = new AtomicReference<>(null);
    @Getter
    private final AtomicBoolean waitingConnection = new AtomicBoolean(false);
    /**
     * Прокси, на которой он сейчас находится
     */
    private Proxy proxy;
    /**
     * Дата, когда он зашёл на сервер server
     */
    private long serverTimestamp;
    /**
     * Пати
     */
    @Setter
    private Party party;

    /**
     * Ник игрока. Будем получать еще данные из базы, чтобы
     * правильно регистр в нике был
     */
    @Setter
    private String name;

    /**
     * ID игрока
     */
    @Setter
    private int playerId;

    /**
     * Фейковый ник игрока
     */
    @Getter
    private FakePlayerNickname fakeNickname;

    /**
     * Дисплей-нейм игрока
     */
    @Getter
    private String displayName;

    /**
     * Префикс игрока
     */
    @Setter
    private Prefix prefix;

    /**
     * Язык игрока
     */
    @Setter
    private Language language;

    /**
     * ID друзей игрока
     */
    private TIntSet friends;

    /**
     * Список игнорируемых игроков
     */
    private TIntList ignoreList;

    /**
     * Проперки игрока
     */
    private TObjectIntMap<String> properties;

    /**
     * Суффиксы игрока
     */
    private TIntObjectMap<Suffix> purchasedSuffixes;

    /**
     * Сообщения о входе игрока
     */
    private List<JoinMessage> purchasedJoinMessages;

    /**
     * Дополнительные bukkit-права
     */
    private List<ExternalPermissionEntry> externalPermissionEntryList;

    /**
     * Выбранный скин
     */
    @Setter
    private Skin skin;

    /**
     * Онлайн ли игрок или нет
     */
    private boolean online;

    /**
     * Отображение чата персонала
     */
    @Getter
    @Setter
    private boolean activePersonalChat = true;

    /**
     * Если true, то при ближайшем коннекте к ближайшему серверу вызовется GamerJoinEvent
     */
    @Setter
    private boolean justJoined;

    /**
     * Дата логина
     */
    private LocalDateTime loginDate;

    /**
     * Последняя сессия
     */
    private Session lastSession;

    /**
     * Данные авторизации
     */
    private BaseAuthPlayerData authData;

    @Setter
    private int protocol;

    @Setter
    private String domain;

    @Getter
    @Setter
    private UserRanks ranks;

    public Gamer(@NonNull String name, InetAddress address) {
        this.name = name;
        this.address = address;
        this.online = address != null;
        this.justJoined = true;
        this.authData = new BaseAuthPlayerData(name);

        if (online) {
            loginDate = LocalDateTime.now();
        }
    }

    public static Gamer getGamer(@NonNull String name) {
        return GAMERS_BY_NAME.getIfPresent(name.toLowerCase());
    }

    @Nullable
    public static Gamer getNickedGamer(@NonNull String name) {
        Gamer gamer = GAMERS_BY_NAME.getIfPresent(name.toLowerCase());

        if (gamer == null) {
            for (Gamer target : GAMERS_BY_ID.asMap().values()) {
                if (target.isNicked(name)) {
                    gamer = target;
                    break;
                }
            }
        }

        return gamer;
    }

    public static Gamer getOfflineGamer(@NonNull String name, @NonNull Consumer<Gamer> consumer) {
        Gamer onlineGamer = getGamer(name);

        if (onlineGamer != null) {
            return onlineGamer;
        }

        Gamer offlineGamer = getOfflineGamer(name);

        boolean isNull = offlineGamer == null;

        if (isNull) {
            offlineGamer = new Gamer(name, null);
            offlineGamer.loadPlayerId();

            if (offlineGamer.getPlayerId() < 0) {
                return null;
            }

            // загружаем имя в верном регистре
            offlineGamer.loadPlayerName();

            OFFLINE_GAMERS_BY_NAME.put(offlineGamer.getLowerName(), offlineGamer);
            OFFLINE_GAMERS_BY_ID.put(offlineGamer.getPlayerId(), offlineGamer);
        }

        // выполняем действия пользователя
        consumer.accept(offlineGamer);

        if (isNull) {
            // вызываем эвент загрузки оффлайн игрока
            CoreApplication.getInstance().getPluginManager().callEvent(new OfflineGamerLoadedEvent(offlineGamer));
        }

        return offlineGamer;
    }

    public static Gamer getOfflineGamer(int playerId, @NonNull Consumer<Gamer> consumer) {
        Gamer onlineGamer = getGamer(playerId);

        if (onlineGamer != null) {
            return onlineGamer;
        }

        String name = NetworkSqlHandler.INSTANCE.getPlayerName(playerId);

        if (name == null) {
            return null;
        }

        Gamer offlineGamer = new Gamer(name, null);
        offlineGamer.setPlayerId(playerId);

        OFFLINE_GAMERS_BY_NAME.put(offlineGamer.getLowerName(), offlineGamer);
        OFFLINE_GAMERS_BY_ID.put(offlineGamer.getPlayerId(), offlineGamer);

        consumer.accept(offlineGamer);

        return offlineGamer;
    }

    public static Gamer getOfflineGamer(@NonNull String name) {
        return OFFLINE_GAMERS_BY_NAME.getIfPresent(name.toLowerCase());
    }

    public static Gamer getOfflineGamer(int playerId) {
        return OFFLINE_GAMERS_BY_ID.getIfPresent(playerId);
    }

    public static Gamer getGamer(int playerId) {
        return GAMERS_BY_ID.getIfPresent(playerId);
    }

    public static void kickAll() {
        for (Gamer gamer : new ArrayList<>(GAMERS_BY_ID.asMap().values())) {
            gamer.quit();
        }
    }

    public static void forEach(Consumer<Gamer> gamerConsumer) {
        GAMERS_BY_ID.asMap().values().forEach(gamerConsumer);
    }

    public static Collection<Gamer> getGamers() {
        return Collections.unmodifiableCollection(GAMERS_BY_ID.asMap().values());
    }

    public static List<Gamer> getGamersByIp(@NonNull InetAddress address) {
        Set<Gamer> gamers = GAMERS_BY_IP.get(address);
        return gamers == null ? Collections.emptyList() : new ArrayList<>(gamers);
    }


    public static void addGamer(Gamer gamer) {
        GAMERS_BY_NAME.put(gamer.getLowerName(), gamer);
        GAMERS_BY_ID.put(gamer.getPlayerId(), gamer);
        GAMERS_BY_IP.computeIfAbsent(gamer.getAddress(), k -> ConcurrentHashMap.newKeySet()).add(gamer);
    }

    public static void removeGamer(Gamer gamer) {
        AbstractServer server = gamer.getServer();

        if (server != null) {
            server.removePlayer(gamer);
            gamer.setServer(null);
        }

        if (gamer.getProxy() != null) {
            gamer.getProxy().removePlayer(gamer);
            gamer.setProxy(null);
        }

        OFFLINE_GAMERS_BY_NAME.invalidate(gamer.getLowerName());
        OFFLINE_GAMERS_BY_ID.invalidate(gamer.getPlayerId());

        GAMERS_BY_NAME.invalidate(gamer.getLowerName());
        GAMERS_BY_ID.invalidate(gamer.getPlayerId());

        Set<Gamer> gamers = GAMERS_BY_IP.get(gamer.getAddress());
        if (gamers != null) {
            gamers.remove(gamer);
            if (gamers.isEmpty()) {
                GAMERS_BY_IP.remove(gamer.getAddress(), gamers);
            }
        }
    }

    public static void invalidateOfflinePlayer(Gamer gamer) {
        OFFLINE_GAMERS_BY_NAME.invalidate(gamer.getLowerName());
        OFFLINE_GAMERS_BY_ID.invalidate(gamer.getPlayerId());
    }

    public static int getTotalOnline() {
        return (int) GAMERS_BY_ID.estimatedSize();
    }

    public String getLowerName() {
        return name.toLowerCase();
    }

    public long getPlayingSeconds() {
        return java.time.Duration.between(loginDate, LocalDateTime.now()).getSeconds();
    }

    public String getCurrentServerName() {
        return currentServerName.get();
    }

    public String getCurrentlyConnectingServerName() {
        return currentlyConnectingServerName.get();
    }

    public int getTimeOnCurrentServer() {
        return serverTimestamp == 0 ? 0
                : (int) TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - serverTimestamp);
    }

    public int getGlobalTime() {
        return getProperty(GLOBAL_TIME_PROPERTY);
    }

    public boolean canSeeVanish() {
        return isVanished() || hasPermission("core.vanish.seevanished");
    }

    public boolean checkVanished() {
        if (isVanished()) {
            sendLocalizedMessage("core_vanish_function_disabled");
        }

        return !isVanished();
    }

    public void loadPlayerId() {
        playerId = NetworkSqlHandler.INSTANCE.getPlayerId(name, false);
    }

    public void loadPlayerName() {
        if (playerId < 0) {
            return;
        }

        name = NetworkSqlHandler.INSTANCE.getPlayerName(playerId);
    }

    public void loadFriends() {
        if (friends != null) {
            return;
        }

        friends = TCollections.synchronizedSet(FriendSqlHandler.INSTANCE.getFriends(playerId).join());
    }

    // =================== NICKS ====================

    public @Nullable SuffixColor getSelectedPrefixColor() {
        if (!hasProperty("prefix-color")) return null;
        int colorId = getProperty("prefix-color");

        if (colorId <= -1) return null;
        return SuffixColor.getById(colorId);
    }

    public RankPrefixModifier getRankPrefixModifier() {
        if (getProperty("selected-plus-color") >= RankPrefixModifier.VALUES.length) {
            return RankPrefixModifier.VALUES[0];
        }

        return RankPrefixModifier.VALUES[getProperty("selected-plus-color")];
    }

    public void loadFakeName() {
        loadPlayerId();

        boolean hasEnoughRights = hasPermission("core.nick");

        if (!hasEnoughRights) {
            fakeNickname = null;
            return;
        }

        if (fakeNickname != null) {
            return;
        }

        fakeNickname = NickSqlHandler.INSTANCE.getPlayerNickname(playerId).join();
    }

    public void loadIgnoreList() {
        if (ignoreList != null) {
            return;
        }

        ignoreList = IgnoreListSqlHandler.INSTANCE.getPlayerIgnoreList(playerId).join();
    }

    public boolean isRankListLoaded() {
        return ranks != null;
    }


    public void loadLastSession() {
        List<Session> sessions = SessionSqlHandler.INSTANCE.loadSessions(playerId, 1)
                .join();

        if (!sessions.isEmpty()) {
            lastSession = sessions.get(0);
        }
    }

    public void load() {
        long start = System.currentTimeMillis();
        try {
            loadProperties();
            loadLanguage();
            loadSuffixes();
            loadRanks();
            loadPrefix();
            loadJoinMessages();
            loadFakeName();
            loadFriends();
            loadExternalPermissions();
            loadSkin();

            updateDisplayName();
        } finally {
            GamerPerformanceMetrics.GAMER_LOAD_TIME.addValue(System.currentTimeMillis() - start);
        }
    }

    public void loadRanks() {
        loadPlayerId();

        val ranks = CoreApplication.getInstance().getUserRanksService().findAllUserRanks(getPlayerId());

        if (ranks.isEmpty()) {
            kick("Unable to load your ranks, please contact support!");
            return;
        }

        if (ranks.getFirst().rank().getId() == -999) {
            sendLocalizedMessage("core_error_unable_to_load_ranks");
        }

        this.ranks = new UserRanks();
        this.ranks.addAllRanks(ranks);
    }

    public void loadLanguage() {
        if (playerId < 0 || language != null) {
            return;
        }

        language = NetworkSqlHandler.INSTANCE.getPlayerLanguage(playerId);
    }

    public void loadAuthData() {
        //данные уже загружены
        //онлайн игроки проверку игнорят - данные возможно придется обновлять
        if (playerId < 0 || (isOnline() && authData.isLoaded())) {
            return;
        }

        authData.load();
    }

    public boolean has2FA() {
        return authData.isLoaded() && authData.has2FA();
    }

    public void setTelegramId(long telegramId) {
        authData.setTelegramId(telegramId);
    }

    public void loadPrefix() {
        val canUse = ranks.hasPermission("custom.prefix") || Project.CURRENT == Project.FAKEPIXEL;

        if (playerId < 0 || prefix != null || !canUse) {
            return;
        }

        prefix = PrefixSqlHandler.INSTANCE.getPrefix(playerId).join();

        if (prefix == null || !prefix.isExpired() || canUse) {
            return;
        }

        prefix = null;

        PrefixSqlHandler.INSTANCE.removePrefix(playerId);
    }

    public void loadSuffixes() {
        if (playerId < 0 || purchasedSuffixes != null) {
            return;
        }

        purchasedSuffixes = TCollections.synchronizedMap(
                SuffixSqlHandler.INSTANCE.getPlayerSuffixes(playerId).join()
        );
    }

    public void loadExternalPermissions() {
        if (playerId < 0) {
            return;
        }

        externalPermissionEntryList = ExternalPermissionsSqlHandler.INSTANCE.loadPlayerInfo(playerId).join();
    }

    public void loadSkin() {
        if (playerId < 0) {
            return;
        }

        if (skin != null) return;

        this.skin = SkinsApi.getPlayerSkin(this.playerId, this.name).join();
    }

    public int getExperience() {
        return getProperty("Experience");
    }

    public int getLevel() {
        return Leveling.getLevel(getExperience());
    }

    public void loadJoinMessages() {
        if (playerId < 0 || purchasedJoinMessages != null) {
            return;
        }

        purchasedJoinMessages = JoinMessageSqlHandler.INSTANCE
                .getPlayerJoinMessages(playerId).join();

        if (ranks == null || !ranks.hasPermission("join.message.use")) {
            return;
        }

        for (JoinMessage message : JoinMessageSqlHandler.INSTANCE.getJoinMessages()) {
            if (purchasedJoinMessages.contains(message) || !message.isAvailableForElite()) {
                continue;
            }

            purchasedJoinMessages.add(message);
        }
    }

    public boolean isSuffixPurchased(int suffixId) {
        return purchasedSuffixes != null && purchasedSuffixes.containsKey(suffixId);
    }

    // ==================================

    public boolean hasJoinMessage(@NonNull JoinMessage message) {
        return purchasedJoinMessages != null && purchasedJoinMessages.contains(message);
    }

    public void loadProperties() {
        if (playerId < 0 || properties != null) {
            return;
        }

        properties = TCollections.synchronizedMap(PropertySqlHandler.INSTANCE.loadProperties(playerId).join());
    }

    public String getRankPrefixModifier(@NonNull String prefixText) {
        val canUse = ranks.hasPermission("prefix.bold");

        if (hasPermission("core.staff") && !canUse) {
            return prefixText;
        }

        val color = getSelectedPrefixColor();
        if (color != null && canUse) {
            return "§" + color.getColorCode() + "§l" + club.abelix.commons.util.color.ChatColor.stripColor(prefixText);
        }

        return prefixText;
    }

    public void updateDisplayName() {
        var rank = ranks.findPriorityRank().rank();
        String prefixText;

        if (prefix != null && (ranks.hasPermission("prefix.custom.color") || Project.CURRENT == Project.FAKEPIXEL)) {
            prefixText = getRankPrefixModifier(prefix.getText());
        } else {
            prefixText = getRankPrefixModifier(rank.getPrefix());
        }

        if (Project.CURRENT == Project.FAKEPIXEL) {
            this.displayName = getRankPrefixModifier().modify(prefixText + name);
        } else {
            this.displayName = prefixText + name;
        }
    }

    public boolean isNicked() {
        return fakeNickname != null;
    }

    public boolean isNicked(@NonNull String nick) {
        return isNicked() && fakeNickname.equals(nick);
    }

    public void changePlayerNick(@NonNull FakePlayerNickname fakeNickname) {
        this.fakeNickname = fakeNickname;
    }

    public void resetPlayerNick() {
        // сбрасываем ник в базе
        NickSqlHandler.INSTANCE.clearPlayerNickname(fakeNickname);

        // сбрасываем ник
        this.fakeNickname = null;
    }

    /**
     * Получить Party игрока, если пати не валидное, вернёт null
     */
    public Party getParty() {
        return party == null || !party.isValid() ? null : party;
    }

    // External Permission region

    /**
     * Получить пати игрока
     */
    public Party getRawParty() {
        return party;
    }

    /**
     * Получить числовые ID друзей
     *
     * @return коллекция числовых ID друзей
     */
    public TIntCollection getFriends() {
        return TCollections.unmodifiableSet(friends);
    }

    /**
     * Получить лимит друзей
     *
     * @return лимит друзей
     */
    public int getFriendLimit() {
        return FriendSqlHandler.INSTANCE.getFriendLimit(ranks);
    }

    /**
     * Получить онлайн друзей
     *
     * @return онлайн друзья
     */
    public List<Gamer> getOnlineFriends() {
        if (friends == null) {
            return Collections.emptyList();
        }

        return IntStream.of(friends.toArray()).mapToObj(Gamer::getGamer)
                .filter(gamer -> gamer != null && gamer.getServer() != null && (canSeeVanish() || !gamer.isVanished()))
                .collect(Collectors.toList());
    }

    /**
     * Получить числовые ID списка игнорируемых
     *
     * @return коллекция числовых ID друзей
     */
    public TIntList getIgnoreList() {
        return TCollections.unmodifiableList(ignoreList);
    }

    /**
     * Добавить игрока в список игнорируемых
     *
     * @param playerId ID игрока
     */
    public void addIgnoredPlayer(int playerId) {
        ignoreList.add(playerId);

        IgnoreListSqlHandler.INSTANCE.addPlayerToIgnoreList(this.playerId, playerId);
        sendToServer(new SIgnoreListUpdate(
                this.playerId,
                playerId,
                SIgnoreListUpdate.IgnoreListAction.ADD
        ));
    }

    /**
     * Удалить игрока из списка игнорируемых
     *
     * @param playerId ID игрока
     */
    public void removeIgnoredPlayer(int playerId) {
        ignoreList.remove(playerId);

        IgnoreListSqlHandler.INSTANCE.removePlayerFromIgnoreList(this.playerId, playerId);
        sendToServer(new SIgnoreListUpdate(
                this.playerId,
                playerId,
                SIgnoreListUpdate.IgnoreListAction.REMOVE
        ));
    }

    /**
     * Проверить, есть ли игрок в списке игнорируемых
     *
     * @param playerId ID игрока
     * @return true, если есть в игнор-листе
     */
    public boolean isIgnored(int playerId) {
        return ignoreList != null && ignoreList.contains(playerId);
    }

    /**
     * Добавить игрока в друзья
     *
     * @param playerId ID игрока
     * @return если игрок уже добавлен, вернёт false
     */
    public boolean addFriend(int playerId) {
        return friends != null && friends.add(playerId);
    }

    // end region

    /**
     * Удалить игрока из друзей
     *
     * @param playerId ID игрока
     * @return если игрок не в друзьях, вернёт false
     */
    public boolean removeFriend(int playerId) {
        return friends != null && friends.remove(playerId);
    }

    /**
     * Проверить, является ли игрок другом
     *
     * @param playerId ID игрока
     * @return true, если является другом
     */
    public boolean isFriend(int playerId) {
        return friends != null && friends.contains(playerId);
    }

    /**
     * На банжи устаналваиется этот uuid, лучше его нигде не использовать, но всё же
     *
     * @return UUID
     */
    public UUID getProxyUUID() {
        if (this.playerId > 0) {
            return UUID.nameUUIDFromBytes(("ID_" + playerId).getBytes());
        }
        return null;
    }

    public boolean hasProperty(@NonNull String property) {
        return properties != null && properties.containsKey(property.toLowerCase());
    }

    public int getProperty(@NonNull String key) {
        if (properties == null) {
            return 0;
        }

        return properties.get(key.toLowerCase());
    }
    // end region money

    // Start mystery dust region

    public void updateProperty(@NonNull String key, int value) {
        if (playerId < 0 || properties == null) {
            return;
        }

        properties.put(key.toLowerCase(), value);

        PropertySqlHandler.INSTANCE.setPropertyValue(playerId, key, value);
    }

    public void removeProperty(@NonNull String key) {
        if (playerId < 0 || properties == null) {
            return;
        }

        dropProperty(key);

        BRemoveProperty packet = new BRemoveProperty(playerId, key);
        sendToProxy(packet);
        sendToServer(packet);
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return ranks.hasPermission(permission);
    }

    public UserRank getPriorityGroup() {
        return getRanks().findPriorityRank();
    }

    public void dropProperty(@NonNull String key) {
        if (playerId < 0 || properties == null) {
            return;
        }

        properties.remove(key.toLowerCase());

        PropertySqlHandler.INSTANCE.removeProperty(playerId, key);
    }


    /**
     * Получить права по определенной группе серверов или нескольким
     *
     * @param lobby    доступно в лобби
     * @param mini     доступно на мини серверах
     * @param uncommon доступно на uncommon серверах
     * @param special  дотсупно на special серверах
     * @return список прав
     */
    public ArrayList<ExternalPermissionEntry> getExternalPermissionsEntry(boolean lobby, boolean mini,
                                                                          boolean uncommon, boolean special) {
        ArrayList<ExternalPermissionEntry> result = new ArrayList<>();

        for (ExternalPermissionEntry entry : this.externalPermissionEntryList) {
            if ((lobby && entry.isCanLobby()) || (mini && entry.isCanLobby()) ||
                    (uncommon && entry.isCanUncommon()) || (special && entry.isCanSpecial())) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Получить право
     *
     * @param permissions право
     * @param lobby       доступно в лобби
     * @param mini        доступно на мини серверах
     * @param uncommon    доступно на uncommon серверах
     * @param special     дотсупно на special серверах
     * @return право
     */
    public ExternalPermissionEntry getExternalPermissionEntry(@NonNull String permissions,
                                                              boolean lobby, boolean mini,
                                                              boolean uncommon, boolean special) {
        ArrayList<ExternalPermissionEntry> findPermissions = getExternalPermissionsEntry(lobby, mini, uncommon, special);
        for (ExternalPermissionEntry entry : findPermissions) {
            if (entry.getPermission().equalsIgnoreCase(permissions)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Есть ли у игрока определенное прав
     *
     * @param permissions право
     * @return есть или нет
     */
    public ExternalPermissionEntry getExternalPermissionEntry(@NonNull String permissions) {
        return getExternalPermissionEntry(permissions, true, true, true, true);

    }

    /**
     * Есть ли у игрока определенное прав
     *
     * @param permissions право
     * @param lobby       доступно в лобби
     * @param mini        доступно на мини серверах
     * @param uncommon    доступно на uncommon серверах
     * @param special     дотсупно на special серверах
     * @return есть или нет
     */
    public boolean hasExternalPermission(@NonNull String permissions,
                                         boolean lobby, boolean mini,
                                         boolean uncommon, boolean special) {
        return getExternalPermissionEntry(permissions, lobby, mini, uncommon, special) != null;
    }

    /**
     * Есть ли у игрока определенное прав
     *
     * @param permissionEntry право
     * @return есть или нет
     */
    public boolean hasExternalPermission(@NonNull ExternalPermissionEntry permissionEntry) {
        return getExternalPermissionEntry(permissionEntry.getPermission()) != null;
    }

    /**
     * Есть ли у игрока определенное прав
     *
     * @param permissionEntry право
     * @param lobby           доступно в лобби
     * @param mini            доступно на мини серверах
     * @param uncommon        доступно на uncommon серверах
     * @param special         дотсупно на special серверах
     * @return есть или нет
     */
    public boolean hasExternalPermission(@NonNull ExternalPermissionEntry permissionEntry,
                                         boolean lobby, boolean mini,
                                         boolean uncommon, boolean special) {
        return getExternalPermissionEntry(permissionEntry.getPermission(),
                lobby, mini, uncommon, special) != null;
    }

    /**
     * Добавить игроку право
     *
     * @param permissionEntry право
     */
    public void addExternalPermission(@NonNull ExternalPermissionEntry permissionEntry) {
        ExternalPermissionsSqlHandler.INSTANCE.savePermissionInfo(permissionEntry).whenComplete((entry, exception) -> {
            if (exception != null) {
                Logging.IMP.error("Failed save permission(permission: {}) gamer in gamer {}. Details:", permissionEntry.getPermission(),
                        this.getName());
                return;
            }
            this.externalPermissionEntryList.add(entry);
        });
    }

    /**
     * Добавить игроку право
     *
     * @param permission право
     * @param lobby      доступно в лобби
     * @param mini       доступно на мини серверах
     * @param uncommon   доступно на uncommon серверах
     * @param special    дотсупно на special серверах
     */
    public void addExternalPermission(@NonNull String permission,
                                      boolean lobby, boolean mini,
                                      boolean uncommon, boolean special) {
        addExternalPermission(new ExternalPermissionEntry(-1, this.getPlayerId(), permission,
                lobby, mini, uncommon, special));
    }

    public void removeExternalPermission(@NonNull ExternalPermissionEntry externalPermissionEntry) {
        ExternalPermissionsSqlHandler.INSTANCE.removePermissionEntry(externalPermissionEntry);
        this.externalPermissionEntryList.remove(externalPermissionEntry);
    }

    /**
     * Получить количество донат-валюты
     *
     * @return количество донат валюты на счету
     */
    public int getGold() {
        return getProperty("Donate/Gold");
    }

    /**
     * Проверить есть ли у игрока сейчас указаное количество донат-валюты на счету
     *
     * @param amount сумма для проверки
     * @return True - доступно, False - не доступно
     */
    public boolean hasGold(int amount) {
        return getGold() >= amount;
    }

    /**
     * Выдать донат-валюту
     *
     * @param amount сумма
     */
    public void addGold(int amount) {
        setProperty("Donate/Gold", getGold() + amount);
    }

    /**
     * Снять со счёта
     *
     * @param amount сумма
     */
    public void takeGold(int amount) {
        if (!hasGold(amount)) {
            throw new IllegalArgumentException("The gamer does not have enough gold to perform this operation");
        }

        addGold(-amount);
    }

    /**
     * Получение количества мистической пыли у игрока
     *
     * @return количество мистической пыли
     */
    public int getMysteryDust() {
        return getProperty("Lobby/MysteryDust");
    }

    /**
     * Выдача мистической пыли игроку
     *
     * @param amount количество
     * @return текущий баланс
     */
    public int addMysteryDust(int amount) {
        return addMysteryDust(amount, getMysteryDust(), "");
    }

    /**
     * Выдача мистической пыли игроку
     *
     * @param amount количество
     * @param reason причина выдачи (ключ локализации)
     * @return текущий баланс
     */
    public int addMysteryDust(int amount, @NonNull String reason) {
        return addMysteryDust(amount, getMysteryDust(), reason);
    }

    /**
     * Забрать мистическую пыль у игрока
     *
     * @param amount количество (больше 0)
     * @return текущий баланс
     */
    public int takeMysteryDust(int amount) {
        int balance = getMysteryDust();

        if (balance <= amount || amount < 0) {
            throw new IllegalArgumentException("The gamer does not have enough mystery dust to perform this operation");
        }

        return addMysteryDust(-amount, getMysteryDust(), null);
    }

    private int addMysteryDust(int amount, int inital, String reasonLanguageKey) {

        int mysteryDustFinal = inital + amount;

        setProperty("Lobby/MysteryDust", mysteryDustFinal);

        //сообщение не требуется
        if (reasonLanguageKey == null) {
            return mysteryDustFinal;
        }

        String mysteryDustForm = CommonWords.MYSTERY_DUST.get(language).fromNumber(amount).toString();
        String msg = " §d+" + Leveling.formatGrouped(amount) + " " + mysteryDustForm;

        if (!reasonLanguageKey.isEmpty()) {
            msg += " (" + (language.isMessageTranslated(reasonLanguageKey) ? language.getMessage(reasonLanguageKey) : reasonLanguageKey) + ")";
        }

        sendMessage(msg);

        return mysteryDustFinal;
    }

    public boolean isVanished() {
        return getProperty("Vanished") == 1;
    }

    // endregion

    public boolean setVanished(boolean vanished) {
        boolean oldValue = isVanished();

        // ваниш не изменился
        if (oldValue == vanished) {
            return false;
        }

        // все проверки прошли, обновляем ваниш и отправляем на все сервера
        setProperty("Vanished", vanished ? 1 : 0);

        return true;
    }

    public AbstractServer getServer() {
        return currentServerName.get() == null ? null : ServerManager.IMP.getServer(currentServerName.get());
    }

    public synchronized void setServer(AbstractServer server) {
        String serverName = server == null ? null : server.getName();

        if (Objects.equals(serverName, currentServerName.get())) {
            return;
        }

        updateServer(server);
    }

    public void addSuffix(@NonNull Suffix suffix) {
        SuffixSqlHandler.INSTANCE.addPlayerSuffix(playerId, suffix);

        if (purchasedSuffixes != null) {
            purchasedSuffixes.put(suffix.getId(), suffix);
        }

        sendToServer(new SAddPlayerSuffix(playerId, suffix.getId()));
    }

    public void addJoinMessage(@NonNull JoinMessage message) {
        JoinMessageSqlHandler.INSTANCE.addPlayerJoinMessage(playerId, message);

        if (purchasedJoinMessages != null) {
            purchasedJoinMessages.add(message);
        }

        sendToServer(new SAddPlayerJoinMessage(playerId, message.getId()));
    }

    public boolean isSettingSet(@NonNull Setting setting) {
        return (getProperty("Settings") & setting.getMask()) != 0;
    }

    public void setSettingState(@NonNull Setting setting, boolean state) {
        int settings = getProperty("Settings");

        if (state) {
            setProperty("Settings", settings | setting.getMask());
        } else {
            setProperty("Settings", settings & ~setting.getMask());
        }
    }


    public void addProperty(@NonNull String property, int value) {
        int oldValue = getProperty(property);
        int newValue = oldValue + value;

        updateProperty(property, newValue);
        sendPropertyChange(property, newValue);
    }

    public void setProperty(@NonNull String property, int value) {
        updateProperty(property, value);
        sendPropertyChange(property, value);
    }

    private void sendPropertyChange(String property, int value) {
        Packet<?> packet = new BSetProperty(playerId, property, value);
        sendToServer(packet);
        sendToProxy(packet);
    }

    public void sendTitle(@NonNull String title, @NonNull String subtitle) {
        sendTitle(title, subtitle, 20, 20, 20);
    }

    public void sendTitle(@NonNull String title, @NonNull String subtitle, int fadeIn, int stay, int fadeOut) {
        sendToProxy(new SSendTitle(playerId, title, subtitle, fadeIn, stay, fadeOut));
    }

    public void quit() {
        quit(true);
    }

    public void quit(boolean sendKick) {
        if (sendKick) {
            kick("§cYou have been disconnected from server!");
        }

        this.online = false;

        CoreApplication.getInstance().getPluginManager().callEvent(
                new PlayerQuitEvent(this)
        );

        updateServer(null);
        removeGamer(this);
    }

    public void sendToProxy(@NonNull Packet<?> packet) {
        if (online && this.proxy != null) {
            this.proxy.getConnection().sendPacket(packet);
        }
    }

    public void sendToServer(@NonNull Packet<?> packet) {
        AbstractServer server = getServer();

        if (online && server != null) {
            server.getConnection().sendPacket(packet);
        }
    }

    public void localizedKick(@NonNull String key, Object... objects) {
        kick(language.getMessage(key, objects));
    }

    public void kick(@NonNull String reason) {
        sendToProxy(new BProxyKick(playerId, reason));
    }

    @Override
    public void sendMessage(@NonNull String message) {
        sendToProxy(new BChat(playerId, message));
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    public void setProxy(Proxy proxy) {
        if (this.proxy == proxy) {
            return;
        }

        // удаляем игрока с предыдущей прокси
        if (this.proxy != null) {
            this.proxy.removePlayer(this);
        }

        this.proxy = proxy;

        // добавляем на новую
        if (proxy != null) {
            proxy.addPlayer(this);
        }
    }

    public synchronized void updateServer(AbstractServer server) {
        waitingConnection.set(false);

        int sendGlobalTime = 0;
        AbstractServer oldServer = getServer();
        val serverName = server == null ? null : server.getName();

        // Обновляем информацию в серверах
        if (oldServer != null && oldServer.removePlayer(this)) {
            sendGlobalTime = getTimeOnCurrentServer();
        }

        this.currentServerName.set(serverName);
        if (server != null && server.addPlayer(this)) {
            updateTablist();
        }

        // Обновление времени
        this.serverTimestamp = server == null || server.isSpecial() ? 0 : System.currentTimeMillis();
        if (serverName != null && serverName.equals(currentlyConnectingServerName.get())) {
            currentlyConnectingServerName.set(null); // игрок подключился к серверу
        }

        // обновляем проперти в базе данных
        if (sendGlobalTime != 0) {
            // синхронно и без отправки на другие сервера
            String property = GLOBAL_TIME_PROPERTY;

            int oldValue = getProperty(property);
            int newValue = oldValue + sendGlobalTime;

            properties.put(property.toLowerCase(), newValue);
            PropertySqlHandler.INSTANCE.setPropertyValue(playerId, property, newValue).join();
        }
    }

    public void updateTablist() {
        String header = language.getMessage("tab_header");
        String footer = language.getMessage("tab_footer", "%server_name%", getServer() != null ? getServer().getName() : "unknown");

        Async.runLater(() -> sendToProxy(new STablistHeaderFooter(playerId, header, footer)), 20, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<ConnectStatus> sendTo(@NonNull AbstractServer... servers) {
        return this.sendTo(Arrays.stream(servers).toList());
    }

    public CompletableFuture<ConnectStatus> sendTo(boolean needReservation, @NonNull AbstractServer... servers) {
        return this.sendTo(Arrays.stream(servers).toList(), needReservation);
    }

    public CompletableFuture<ConnectStatus> sendTo(@NonNull List<AbstractServer> servers) {
        return sendTo(servers, true);
    }

    public CompletableFuture<ConnectStatus> sendTo(@NonNull List<AbstractServer> servers, boolean needReservation) {
        // Игрок уже ждет подключения
//        if (this.waitingConnection.get()) {
//            sendLocalizedMessage("gamer_already_waiting_connection");
//            return CompletableFuture.completedFuture(ConnectStatus.GAMER_WAITING_CONNECT);
//        }

        if (this.waitingConnection.get()) {
            sendLocalizedMessage("gamer_already_waiting_connection");
            return CompletableFuture.completedFuture(ConnectStatus.GAMER_WAITING_CONNECT);
        }

        // Сервера ещё не доступен для входа
        List<String> availableServers = servers.stream()
                .filter(AbstractServer::canJoin).map(AbstractConnectable::getName).toList();

        if (availableServers.isEmpty()) {
            sendLocalizedMessage("servers_not_found");
            return CompletableFuture.completedFuture(ConnectStatus.SERVER_NOT_FOUND);
        }

        // Текуший сервер игрока
        val currentServer = getServer();

        if (servers.contains(currentServer)) {
            this.sendLocalizedMessage("server_already_connected");
            return CompletableFuture.completedFuture(ConnectStatus.ALREADY_CONNECTED);
        }

        // Приватные игры

//        for ()

        waitingConnection.set(true);
        Async.runLater(() -> waitingConnection.compareAndSet(true, false), 10, TimeUnit.SECONDS);

        // Todo: переписать этот пиздец
        // Проверка на мини, из-за системы арен мы поддерживает закидывание ток на 1 мини к сожалению
        for (AbstractServer abstractServer : servers) {
            if (abstractServer instanceof MiniServer newServer) {
                if (needReservation && newServer instanceof ArenaServer arenaServer) {
                    arenaServer.newReservation(1);
                }

                if (currentServer instanceof MiniServer oldServer && newServer.getMasterServer() == oldServer.getMasterServer()) {
                    currentlyConnectingServerName.set(newServer.getName());
                    sendToProxy(new SSendPlayer(playerId, 2000, new String[]{newServer.getName()}));
                    sendToServer(new SMovePlayerToMini(playerId, newServer.getName()));
                    return CompletableFuture.completedFuture(ConnectStatus.SUCCESS);
                }

                currentlyConnectingServerName.set(newServer.getName());
                sendToProxy(new SSendPlayer(playerId, 2000, new String[]{newServer.getName()}));
                return CompletableFuture.completedFuture(ConnectStatus.SUCCESS);
            }
        }

        sendToProxy(new SSendPlayer(playerId, 2500, availableServers.toArray(String[]::new)));
        return CompletableFuture.completedFuture(ConnectStatus.SUCCESS);
    }

    public synchronized CompletableFuture<ConnectStatus> sendTo(ServerMode... serverModes) {
        //вызываем эвент, чтобы он нам вернул игроков которых надо тпшить, а следовательно и кол-во места
        PlayerGameRedirectEvent redirectEvent = new PlayerGameRedirectEvent(this);
        CoreApplication.getInstance().getPluginManager().callEvent(redirectEvent);

        Set<Gamer> redirectedGamers = redirectEvent.getAdditionalGamerList();
        redirectedGamers.add(this); // Лидер пати

        //необходимое пространство
        int space = redirectedGamers.size();

        // Если это мини режим, то добавляем в очередь
        for (ServerMode serverMode : serverModes) {
            if (!serverMode.isMini()) {
                continue;
            }

            ArenaServer miniServer;
            miniServer = ServerManager.IMP.getBestBySpace((Mini) serverMode, space);
            if (miniServer == null) {
                this.sendLocalizedMessage("server_not_found");
                return CompletableFuture.completedFuture(ConnectStatus.SERVER_NOT_FOUND);
            }

            miniServer.newReservation(space);


            for (Gamer target : redirectedGamers) {
                target.sendTo(false, miniServer);
            }

            return CompletableFuture.completedFuture(ConnectStatus.SUCCESS);
        }

        // Добавляем все сервера на которые будем пытаться зайти
        List<AbstractServer> tryServers = new ArrayList<>();
        for (ServerMode mode : serverModes) {
            if (mode.isUncommon()) {
                tryServers.addAll(mode == Uncommon.BUILDFFA
                        ? ServerManager.IMP.getFullestServers(mode, space)
                        : ServerManager.IMP.getBestBySpaceServers(mode, space));
            } else {
                tryServers.addAll(ServerManager.IMP.getBestByOnlineServers(mode, false));
            }
        }

        if (tryServers.isEmpty()) {
            return CompletableFuture.completedFuture(ConnectStatus.SERVER_NOT_FOUND);
        }

        return this.sendTo(tryServers);
    }

    public int getCoins(@NonNull Uncommon uncommon) {
        return getCoins(uncommon.name());
    }

    public int getCoins(@NonNull Master master) {
        return getCoins(master.name());
    }

    public int getCoins() {
        Master master = getServer().getMaster();
        return master == null ? 0 : getCoins(master);
    }

    public int getCoins(@NotNull final ServerMode mode) {
        return getCoins(mode.getName());
    }

    public int getCoins(@NonNull String mode) {
        return getProperty(mode + "/Coins");
    }

    private int addCoins(@NonNull String mode, int initial) {
        addProperty(mode + "/Coins", initial);
        return initial;
    }

    public int addCoins(@NonNull Uncommon uncommon, int coins) {
        return addCoins(uncommon, coins, null);
    }

    public int addCoins(@NonNull Master master, int coins) {
        return addCoins(master, coins, null);
    }

    public int addCoins(@NonNull Uncommon uncommon, int coins, String reason) {
        return addCoins(uncommon.name(), coins);
    }

    public int addCoins(@NonNull Master master, int coins, String reason) {
        return addCoins(master.name(), coins);
    }

    public int addCoins(@NonNull Uncommon uncommon, int coins, boolean multiply, String reason) {
        return addCoins(uncommon.name(), coins);
    }

    public int addCoins(@NonNull Master master, int coins, boolean multiply, String reason) {
        return addCoins(master.name(), coins);
    }

    public boolean hasCoins(@NonNull Uncommon uncommon, int amount) {
        return hasCoins(uncommon.name(), amount);
    }

    public boolean hasCoins(@NonNull Master master, int amount) {
        return hasCoins(master.name(), amount);
    }

    public boolean hasCoins(int amount) {
        return hasCoins(Objects.requireNonNull(getServer().getType().getMaster()), amount);
    }

    private boolean hasCoins(@NonNull String mode, int amount) {
        int currentBalance = getProperty(mode + "/Coins");

        return amount <= currentBalance;
    }

    /**
     * Снять коины со счёта по текущему мастеру
     *
     * @param take сумма
     */
    public void takeCoins(int take) {
        takeCoins(Objects.requireNonNull(getServer().getType().getMaster()), take);
    }

    public void takeCoins(ServerMode mode, int take) {
        takeCoins(mode.getName(), take);
    }

    /**
     * Снять коины со счёта
     *
     * @param uncommon режим
     * @param take     сумма
     */
    public void takeCoins(@NonNull Uncommon uncommon, int take) {
        takeCoins(uncommon.name(), take);
    }

    /**
     * Снять коины со счёта
     *
     * @param master режим
     * @param take   сумма
     */
    public void takeCoins(@NonNull Master master, int take) {
        takeCoins(master.name(), take);
    }

    private void takeCoins(String mode, int take) {
        if (take <= 0) {
            throw new IllegalArgumentException("Can not take positive coins amount: " + take);
        }

        addProperty(mode + "/Coins", -take);
    }

    @Override
    public boolean isAdmin() {
        return ranks.hasPermission("core.admin");
    }

    @Override
    public boolean isHelper() {
        return ranks.hasPermission("core.helper");
    }

    @Override
    public boolean isDeveloper() {
        return ranks.hasPermission("core.developer");
    }

    @Override
    public boolean isDonater() {
        return ranks.hasPermission("core.donate");
    }

    @Override
    public String toString() {
        return "Gamer/" + this.name;
    }

    @Override
    public int hashCode() {
        return playerId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        return o instanceof Gamer && playerId == ((Gamer) o).playerId;
    }

}
