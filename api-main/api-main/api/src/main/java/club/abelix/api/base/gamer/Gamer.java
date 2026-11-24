package club.abelix.api.base.gamer;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.box.entity.BoxDto;
import club.abelix.api.base.box.repo.PlayerBoxesRepository;
import club.abelix.api.base.chat.mode.ChatMode;
import club.abelix.api.base.command.ProxiedCommandSender;
import club.abelix.api.base.event.skin.SkinChangedEvent;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.gamer.event.GamerLanguageChangeEvent;
import club.abelix.api.base.gamer.event.GamerPropertyChangeEvent;
import club.abelix.api.base.gamer.event.GamerSettingToggleEvent;
import club.abelix.api.base.gamer.state.GamerState;
import club.abelix.api.base.level.event.ExperienceChangeEvent;
import club.abelix.api.base.level.event.LevelUpEvent;
import club.abelix.api.base.multiplier.Multiplier;
import club.abelix.api.base.multiplier.booster.Booster;
import club.abelix.api.base.multiplier.booster.BoosterSqlHandler;
import club.abelix.api.base.multiplier.prepared.DonateCoinsMultiplier;
import club.abelix.api.base.multiplier.prepared.DonateExpMultiplier;
import club.abelix.api.base.multiplier.prepared.LevelMultiplier;
import club.abelix.api.base.party.Party;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.vanish.VanishListener;
import club.abelix.api.nms.sound.SoundType;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.manager.GameManager;
import club.abelix.api.server.game.manager.GameStatsManager;
import club.abelix.api.server.game.manager.TeamManager;
import club.abelix.api.server.game.stats.GameStatsCache;
import club.abelix.api.server.lobby.guild.event.GamerGuildTagUpdate;
import club.abelix.commons.data.auth.BaseAuthPlayerData;
import club.abelix.commons.data.booster.MultiplierType;
import club.abelix.commons.data.currency.GameCurrency;
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
import club.abelix.commons.data.purchase.PurchaseSqlHandler;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.UserRank;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.data.session.Session;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.data.setting.media.SocialMediaSqlHandler;
import club.abelix.commons.data.setting.media.SocialMediaType;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.SkinsApi;
import club.abelix.commons.data.skin.exception.SkinException;
import club.abelix.commons.data.stat.StatisticPeriod;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.data.stat.StatsSqlHandler;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.commons.data.suffix.color.SuffixColor;
import club.abelix.commons.data.vote.PlayerVoteData;
import club.abelix.commons.data.vote.VoteSqlHandler;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.language.Language;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.json.JSON;
import club.abelix.commons.util.map.IndexMap;
import club.abelix.commons.util.object.FractionAccumulator;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.commons.version.GameVersion;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.common.enums.GamerQueueJoinResult;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.both.BChat;
import club.abelix.network.packet.play.both.BRemoveProperty;
import club.abelix.network.packet.play.both.BSetProperty;
import club.abelix.network.packet.play.client.CGamerLeaveQueue;
import club.abelix.network.packet.play.client.CGamerQueueJoinRequest;
import club.abelix.network.packet.play.client.CGetGuildInfo;
import club.abelix.network.packet.play.client.CPlayerCommand;
import club.abelix.network.packet.play.server.SGiveSkyblockGems;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.Uncommon;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import gnu.trove.TCollections;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Игрок
 */
@Slf4j
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class Gamer extends ProxiedCommandSender implements IGamer {

    public static final Consumer<Gamer> DEFAULT_LOADER = gamer -> {
        gamer.loadPlayerId();
        gamer.loadProperties();
        gamer.loadRanks();
        gamer.loadFakeName();
        gamer.loadPrefixAndUpdateName();
        gamer.loadSuffix();
        gamer.loadJoinMessages();
        gamer.loadLanguage();
        gamer.loadPurchases();
        gamer.loadVoteData();
        gamer.loadFriends();
        gamer.loadIgnoreList();
        gamer.loadAuthData();
        gamer.loadSkull();
        gamer.loadStats();
        gamer.loadSocialMediaLinks();
        gamer.loadExternalPermissions();
        gamer.loadBooster();
        gamer.loadBoxes();
    };

    @Getter
    private static Consumer<Gamer> gamerLoader = DEFAULT_LOADER;

    private static final IndexMap<Gamer> GAMERS
            = IndexMap.<Gamer>newMap()
            .registerIndex(String.class, Gamer::getLowerName)
            .registerIndex(UUID.class, gamer -> gamer.getPlayer().getUniqueId())
            .registerIntIndex(Gamer::getPlayerId);

    private static final Cache<Integer, Gamer> OFFLINE_GAMERS
            = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .build();

    private final Map<StatsMode, TIntObjectMap<TObjectIntMap<StatsField>>> totalStats = new HashMap<>();
    private final FractionAccumulator xpAccumulator = new FractionAccumulator();
    private final FractionAccumulator coinAccumulator = new FractionAccumulator();
    @Getter
    private final long loginTimestamp = System.currentTimeMillis();
    @Getter
    private String name;

    private String displayName;

    @Setter
    @Getter
    private String server;
    @Getter
    private FakePlayerNickname fakeName;

    // Other data
    @Getter
    @EqualsAndHashCode.Include
    private int playerId;

    @Getter
    private Player player;
    @Getter
    private GameVersion version;
    @Getter
    private Language language;
    @Setter
    @Getter
    private Party party;
    @Getter
    private Skin skin;
    private ItemStack skull;
    @Getter
    @Setter
    private Prefix prefix;
    @Getter
    private String prefixText;
    @Getter
    private String realPrefixText;
    @Getter
    private PlayerVoteData playerVoteData;

    /**
     * Текущая игра
     */
    @Getter
    private Game game;
    /**
     * Купленные суффиксы
     */
    private TIntObjectMap<Suffix> purchasedSuffixes;

    /**
     * Купленные сообщения о входе
     */
    private List<JoinMessage> purchasedJoinMessages;
    /**
     * список id друзей
     */
    private TIntSet friendIds;
    /**
     * игнор-лист
     */
    private TIntList ignoreList;
    /**
     * покупки
     */
    private TIntList purchases;
    /**
     * проперти
     */
    private TObjectIntMap<String> properties;
    /**
     * медиа ссылки
     */
    private Map<SocialMediaType, String> socialMediaLinkMap;
    /**
     * Дополнительные bukkit-права
     */
    private List<ExternalPermissionEntry> externalPermissionEntryList;

    /**
     * Активный бустер
     */
    @Setter
    private Booster booster;

    private LocalDateTime lastLoginTime;
    @Getter
    private LocalDateTime lastLoginTimeLoad;
    @Setter
    @Getter
    private GamerState gamerState = GamerState.PLAYER;

    @Getter
    private BaseAuthPlayerData authData;

    @Getter
    @Setter
    private FakeTeam playerTag;

    @Getter
    private Mini currentQueueMini;

    @Getter
    @Setter
    private boolean isInternallyChangingGameMode = false;

    @Getter
    @Setter
    private boolean gameRejoin = false;

    @Getter
    private List<BoxDto> boxes;

    /**
     * Guilds
     */
    @Getter
    private String guildTag;

    @Getter
    @Setter
    private UserRanks ranks;

    public Gamer(@NonNull String name) {
        this.name = name;
    }

    public static Collection<Gamer> getGamers() {
        return new HashSet<>(GAMERS.values());
    }

    public static Collection<Gamer> getNonVanishedGamers() {
        Collection<Gamer> gamers = getGamers();
        gamers.removeIf(Gamer::isVanished);

        return gamers;
    }

    public static Gamer getGamer(@NonNull Player player) {
        return GAMERS.get(UUID.class, player.getUniqueId());
    }

    public static Gamer getGamer(String name) {
        return GAMERS.get(String.class, name.toLowerCase());
    }

    public static Gamer getGamer(int playerID) {
        return GAMERS.get(playerID);
    }

    public static Gamer getOrCreate(String name, Consumer<Gamer> gamerConsumer) {
        //проверяем онлайн игроков по этому нику
        Gamer gamer = getGamer(name);

        if (gamer != null) {
            return gamer;
        }

        //иначе загружаем ID по нику и загружаем оффлайн-игрока
        //даже если игрок с таким ID уже есть (оффлайн игрок есть),
        //это не проблема - у networksqlhandler свой кэш
        return getOrCreate(NetworkSqlHandler.INSTANCE.getPlayerId(name), gamerConsumer);
    }

    /**
     * Загрузить геймера
     *
     * <br>Если геймера с таким ID нет, то вернёт null
     */
    @Nullable
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

            gamer = new Gamer(playerName);
            gamer.playerId = playerId;
            OFFLINE_GAMERS.put(playerId, gamer);
        }

        gamerConsumer.accept(gamer);

        return gamer;
    }

    // Base Method

    // я боюсь удалять этот метод, вдруг что-то обосрётся.
    // если вы кот и у вас 9 жизней, удалите его, пожалуйста))
    @Deprecated
    public static Gamer getOfflineGamer(@NonNull String name) {
        return OFFLINE_GAMERS.asMap().values()
                .stream()
                .filter(gamer -> gamer.name.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    //------------------------------------

    public static Gamer getOfflineGamer(int playerId) {
        return OFFLINE_GAMERS.getIfPresent(playerId);
    }

    public static void addGamer(Gamer gamer) {
        OFFLINE_GAMERS.invalidate(gamer.playerId);
        GAMERS.store(gamer);
    }

    public static Gamer remove(Player player) {
        Gamer gamer = GAMERS.get(UUID.class, player.getUniqueId());

        if (gamer != null) {
            // сохраняем стату (которая не относится к конкретной игре)
            GameStatsManager.DEFAULT.saveAndRemove(gamer);

            GAMERS.remove(gamer);
        }

        return gamer;
    }

    public String getPlayerName() {
        return fakeName == null ? name : fakeName.getNickname().getName();
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime == null ? LocalDateTime.MIN : lastLoginTime;
    }

    public LocalDateTime getLastLoginTimeRaw() {
        return lastLoginTime;
    }

    public int getTimeOnServer() {
        return (int) TimeUnit.SECONDS.convert(System.currentTimeMillis() - loginTimestamp, TimeUnit.MILLISECONDS);
    }

    public int getNetworkPlayedTime() {
        return getProperty(GLOBAL_TIME_PROPERTY);
    }

    public StatsMode getStatsMode() {
        return game == null ? FalconAPI.getStatsMode() : game.getStatsMode();
    }

    /**
     * Установка кастомного загрузчика игрока
     * ВНИМАНИЕ! после выполнения консьюмера данные должны быть загружены!
     * При изменении лоадера убедитесь, что вы загрузили все необходимые данные!
     *
     * @param gamerLoader лоадер игрока
     */
    public static void setGamerLoader(Consumer<Gamer> gamerLoader) {
        Gamer.gamerLoader = gamerLoader;
    }

    public void load() {
        long start = System.currentTimeMillis();

        // Загружаем данные
        gamerLoader.accept(this);

        long processTime = System.currentTimeMillis() - start;
        if (processTime >= 200)
            Logging.IMP.warn("Loading gamer {} from Database >= 200ms. Loaded time {}", this.name, processTime);
    }

    public void setPlayer(@NonNull Player player) {
        this.player = player;
        this.name = player.getName();

        this.player.setCustomName(getPlayerName());
        this.player.setPlayerListName(getPlayerName());
    }

    public void updateVersion() {
        if (player == null) {
            throw new IllegalStateException("Cannot load player version because player is offline.");
        }

        this.version = FalconAPI.getPlayerVersionProvider().getVersion(player);

        if (this.version == null) {
            //самая последняя, потому что старье он по-любому найдет
            //а так хоть если добавим поддержку новой версии и забудем тут,
            //игроку будет разрешать делать что надо на нужной версии
            this.version = GameVersion.MAXIMUM_VERSION;
        }
    }

    @Override
    public boolean isOnline() {
        return player != null && player.isOnline();
    }

    public void resolveGame() {
        if (game != null || isOnline()) {
            return;
        }

        for (Game game : GameManager.INSTANCE.getRegisteredGames()) {
            TeamManager manager = game.getTeamManager();

            if (manager != null && manager.getPlayers().contains(this)) {
                this.game = game;
            }
        }
    }

    @Override
    public CommandSender getCommandSender() {
        return player;
    }

    public String getRealDisplayName() {
        return realPrefixText + name;
    }

    public void sendMessage(@NonNull String text) {
        if (player != null && player.isOnline()) {
            player.sendMessage(text);
        } else {
            sendMessageCore(text);
        }
    }

    public void sendJsonMessage(@NonNull String text) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(ComponentSerializer.parse(text));
        } else {
            sendMessageCore(text);
        }
    }

    public void sendMessageCore(@NonNull String text) {
        Connection.sendPacket(new BChat(playerId, text));
    }

    public void sendActionBar(@NonNull String text) {
        if (player != null) {
            FalconAPI.getNmsApi().getPlayerApi().sendActionBar(player, text);
        }
    }

    public void sendCoreCommand(@NonNull String message) {
        Connection.sendPacket(new CPlayerCommand(getPlayerId(), message));
    }

    // region Skull

    public void sendTitle(@NonNull String title, @NonNull String subtitle) {
        if (player != null) {
            FalconAPI.getNmsApi().getPlayerApi().sendTitle(player, title, subtitle);
        }
    }

    public void sendTitle(@NonNull String title, @NonNull String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player != null) {
            FalconAPI.getNmsApi().getPlayerApi().sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    public void sendLocalizedTitle(int fadeIn, int stay, int fadeOut, @NonNull String titleKey, @NonNull String subtitleKey, Object... objects) {
        sendTitle(language.getMessage(titleKey, objects), language.getMessage(subtitleKey, objects), fadeIn, stay, fadeOut);
    }

    public void sendLocalizedTitle(@NonNull String titleKey, @NonNull String subtitleKey, Object... objects) {
        sendTitle(language.getMessage(titleKey, objects), language.getMessage(subtitleKey, objects));
    }

    public void sendLocalizedActionBar(@NonNull String key, Object... objects) {
        sendActionBar(language.getMessage(key, objects));
    }

    // endregion

    // region player id

    public void sendLocalizedMessage(@NonNull String key, Object... objects) {
        sendMessage(language.getMessage(key, objects));
    }

    public void playSound(@NonNull SoundType sound) {
        playSound(sound.toYSound(), sound.getVolume(), sound.getPitch());
    }

    // endregion

    // region language
    public void playSound(@NonNull SoundType sound, float volume, float pitch) {
        playSound(sound.toYSound(), volume, pitch);
    }

    public void playSound(@NonNull YSound sound) {
        playSound(sound, 1F, 1F);
    }

    public void playSound(@NonNull YSound sound, float volume, float pitch) {
        FalconAPI.getNmsApi().getSoundApi().playSound(player, sound, volume, pitch);
    }

    // endregion

    @Override
    public boolean isConsole() {
        return false;
    }

    public ItemStack getSkull() {
        ItemStack stack = this.skull;

        return stack != null ? stack.clone() : new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
    }

    public void loadSkull() {
        loadPlayerId();

        //данные о скине уже загружены
        //онлайн игроки проверку игнорят - данные возможно придется обновлять
        if (!isOnline() && isSkinLoaded()) {
            return;
        }

        try {
            SkinsApi.getPlayerSkin(playerId,
                    fakeName != null ? fakeName.getNickname().getName() : name
            ).thenAccept(this::setSkin).get(3, TimeUnit.SECONDS);
        } catch (ExecutionException | RuntimeException | InterruptedException | TimeoutException e) {
            this.setSkin(SkinsApi.DEFAULT_SKIN);
            Logging.IMP.error("Failed load gamer skull. Details: ");
            e.printStackTrace();
        }
    }

    public void loadRanks() {
        loadPlayerId();

        if (ranks != null && !ranks.asSet().isEmpty()) return;

        val ranks = StarshipBukkit.getInstance().getUserRanksService().findAllUserRanks(getPlayerId());

        if (ranks.isEmpty()) {
            disconnect("rank_unable_to_load");
            return;
        }

        if (ranks.getFirst().rank().getId() == -999) {
            sendLocalizedMessage("core_error_unable_to_load_ranks");
        }

        this.ranks = new UserRanks();
        this.ranks.addAllRanks(ranks);

        log.info("Loaded {} ranks for player {}", ranks.size(), getName());
    }

    public boolean hasPermission(@NonNull String permission) {
        return ranks.hasPermission(permission);
    }

    public void loadGuild() {
        loadPlayerId();
        loadProperties();

        if (getProperty("IN_GUILD") == 0) {
            return;
        }

        //данные уже загружены
        //онлайн игроки проверку игнорят - данные возможно придется обновлять
        if (!isOnline() && guildTag != null && !guildTag.isEmpty()) {
            return;
        }

        Connection.awaitPacket(new CGetGuildInfo(getPlayerId())).whenComplete((response, cause) -> {
            if (response.getGuildName() == null || response.getGuildTag() == null ||
                    response.getGuildName().contains("N/A") || response.getGuildTag().contains("N/A")) {
                return;
            }

            guildTag = response.getGuildTag();
        });
    }


    public void setSkin(@NonNull Skin skin) {
        this.skin = skin;
        this.skull = ItemUtil.getSkull(skin);
    }

    public void setFakeName(FakePlayerNickname fakeName) {
        this.fakeName = fakeName;

        this.player.setCustomName(fakeName == null ? null : fakeName.getNickname().getName());

        // обновляем дисплей нейм
        updateDisplayName();

        this.player.setDisplayName(getDisplayName()); // Не устанавливаем суффикс чтобы не палиться
        this.player.setPlayerListName(getTabListName() + getPlayerName());

        // обновляем ник игроку
        FalconAPI.getNmsApi().getPlayerApi().changePlayerNick(getPlayer(), getPlayerName());

        // вызываем эвент чтобы обновились теги и прочая залупа
        HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(this));

        if (fakeName != null) {
            try {
                val fakeSkin = SkinsApi.getCachedSkin(fakeName.getNickname().getName());
                setSkin(fakeSkin);
                HeartBeat.callEvent(new SkinChangedEvent(this.player, fakeSkin.getValue(), fakeSkin.getSignature()));
            } catch (SkinException ignore) {
            }
        }
    }

    public void updateGuildTag(String tag) {
        this.guildTag = tag;

        updateDisplayName();

        HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(this));
        HeartBeat.callEvent(new GamerGuildTagUpdate(this));
    }

    public boolean isSkinLoaded() {
        return skin != null;
    }

    public void loadPlayerId() {
        //если id уже загружен или id -1 (т.е игрок не зареган)
        //то ничего делать не нужно
        if (hasPlayerId() || playerId < 0) {
            return;
        }

        //иначе загружаем id из БД
        playerId = NetworkSqlHandler.INSTANCE.getPlayerId(name, false);
    }

    public boolean hasPlayerId() {
        return playerId > 0;
    }

    public void loadLanguage() {
        loadPlayerId();

        if (playerId < 0 || language != null) {
            return;
        }

        language = NetworkSqlHandler.INSTANCE.getPlayerLanguage(playerId);
    }

    public void loadBoxes() {
        loadPlayerId();

        if (boxes != null || playerId < 0) {
            return;
        }

        val boxes = PlayerBoxesRepository.findAllBoxByPlayerId(playerId);

        if (boxes == null || boxes.isEmpty()) {
            this.boxes = new ArrayList<>();
            return;
        }

        this.boxes = new ArrayList<>(boxes);
    }

    public boolean hasBoxes() {
        return !boxes.isEmpty();
    }

    public void takeBox(BoxDto boxDto) {
        PlayerBoxesRepository.takeBox(getPlayerId(), boxDto.getId());
        boxes.remove(boxDto);
    }

    public void clearBoxes() {
        PlayerBoxesRepository.removeAllBoxesByPlayerId(getPlayerId());
        boxes.clear();
    }

    public void changeLanguage(@NonNull Language language) {
        changeLanguage(language, true);
    }

    public void changeLanguage(@NonNull Language language, boolean updateOnCore) {
        //вызываем эвент, чтобы обновить всякую косметическую хуету для игрока
        Bukkit.getPluginManager().callEvent(new GamerLanguageChangeEvent(this, this.language, (this.language = language)));

        if (updateOnCore) {
            //обновляем язык на коре
            Connection.updateLanguage(playerId, language);
        }
    }

    public CompletableFuture<ConnectStatus> connectMode(@NonNull ServerMode mode, boolean awaitResponse) {
        return connectMode(awaitResponse, mode);
    }

    public void connectMode(@NonNull ServerMode... modes) {
        connectMode(false, modes);
    }

    public CompletableFuture<ConnectStatus> connectMode(boolean awaitResponse, @NonNull ServerMode... modes) {
        return CoreUtil.connectMode(this, awaitResponse, modes);
    }

    public void connectMap(@NonNull Mini mini, @NonNull String map) {
        connectMap(mini, map, false);
    }

    public CompletableFuture<ConnectStatus> connectMap(@NonNull Mini mini, @NonNull String map, boolean awaitResponse) {
        return CoreUtil.connectMap(this, mini, map, awaitResponse);
    }

    public void connectExact(@NonNull String prefix) {
        connectExact(prefix, false);
    }

    public CompletableFuture<ConnectStatus> connectExact(@NonNull String prefix, boolean awaitResponse) {
        return CoreUtil.connectExact(this, prefix, awaitResponse);
    }

    public void connectToPlayer(int playerId) {
        CoreUtil.connectToPlayer(this, playerId);
    }

    public void connectToPlayer(@NonNull Gamer gamer) {
        CoreUtil.connectToPlayer(this, gamer.getPlayerId());
    }

    public CompletableFuture<ConnectStatus> connectToPlayer(int playerId, boolean awaitResponse) {
        return CoreUtil.connectToPlayer(this, playerId, awaitResponse);
    }

    public CompletableFuture<ConnectStatus> connectToPlayer(@NonNull Gamer gamer, boolean awaitResponse) {
        return CoreUtil.connectToPlayer(this, gamer.getPlayerId(), awaitResponse);
    }

    public void disconnect(@NonNull String localizationKey, Object... objects) {
        Player player = getPlayer();

        if (player != null) {
            player.kickPlayer(language.getMessage(localizationKey, objects));
        }
    }

    public boolean isRankListLoaded() {
        return ranks != null;
    }

    public void loadPrefixAndUpdateName() {
        loadPrefix();
        updateDisplayName();
    }

    public void loadFakeName() {
        loadPlayerId();

        boolean hasEnoughRights = hasPermission("core.nick");

        if (!hasEnoughRights) {
            fakeName = null;
            return;
        }

        if (fakeName != null) {
            return;
        }

        fakeName = NickSqlHandler.INSTANCE.getPlayerNickname(playerId).join();
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

    public void refreshDisplayName() {
        loadPrefix();
        loadFakeName();
        updateDisplayName();

        HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(this));
    }

    public ChatColor getPrefixColor() {
        String prefix = prefixText.substring(0, 2);

        if (prefix.charAt(0) != '§') {
            return ChatColor.GRAY;
        }

        return ChatColor.getByChar(prefix.charAt(1));
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

    public JoinMessage getActiveJoinMessage() {
        int joinMessageId = getProperty("JoinMessage");

        JoinMessage message = JoinMessageSqlHandler.INSTANCE
                .getJoinMessageById(joinMessageId);

        if (message == null || purchasedJoinMessages == null) {
            return JoinMessage.DEFAULT;
        }

        if (fakeName != null || !purchasedJoinMessages.contains(message) && !ranks.hasPermission("join.message.nicked")) {

            return JoinMessage.DEFAULT;
        }

        return message;
    }

    public List<JoinMessage> getPurchasedJoinMessages() {
        return Collections.unmodifiableList(purchasedJoinMessages);
    }

    // endregion

    public void addPurchasedJoinMessage(@NonNull JoinMessage joinMessage) {
        if (purchasedJoinMessages.contains(joinMessage)) {
            return;
        }

        purchasedJoinMessages.add(joinMessage);

        JoinMessageSqlHandler.INSTANCE.addPlayerJoinMessage(playerId, joinMessage);
    }

    public void changeActiveJoinMessage(JoinMessage message) {
        if (message == null) {
            removeProperty("JoinMessage");
        } else {
            setProperty("JoinMessage", message.getId());
        }
    }

    public void loadSuffix() {
        loadPlayerId();

        if (!isOnline() && purchasedSuffixes != null) {
            return;
        }

        purchasedSuffixes = SuffixSqlHandler.INSTANCE.getPlayerSuffixes(playerId)
                .join();
    }

    public Suffix getActiveSuffix() {
        int activeSuffix = getProperty("Suffix");

        return purchasedSuffixes.get(activeSuffix);
    }

    public List<Suffix> getPurchasedSuffixes() {
        return new ArrayList<>(purchasedSuffixes.valueCollection());
    }

    public boolean isSuffixPurchased(@NonNull Suffix suffix) {
        return purchasedSuffixes.get(suffix.getId()) != null;
    }

    public void addPurchasedSuffix(int suffixId) {
        Suffix suffix = SuffixSqlHandler.INSTANCE.getSuffixById(suffixId);

        if (suffix != null) {
            if (isSuffixPurchased(suffix)) {
                return;
            }

            SuffixSqlHandler.INSTANCE.addPlayerSuffix(playerId, suffix);
            purchasedSuffixes.put(suffixId, suffix);
        }
    }

    public String getSuffixText() {
        Suffix activeSuffix = getActiveSuffix();

        if (activeSuffix == null || fakeName != null) {
            return "";
        }

        return activeSuffix.getDisplaySuffix();
    }

    public Suffix changeSuffixColor(@NonNull Suffix suffix, @NonNull SuffixColor color) {
        int suffixId = suffix.getId();

        suffix = new Suffix(suffixId, suffix.getText(), color, suffix.getCost(), suffix.getRarity());

        //изменяем суффикс в списке купленных
        purchasedSuffixes.put(suffixId, suffix);

        //обновляем данные в БД
        SuffixSqlHandler.INSTANCE.changeSuffixColor(playerId, suffixId, color);

        Suffix activeSuffix = getActiveSuffix();

        //обновляем суффикс игроку если он выбран ща
        if (activeSuffix != null && activeSuffix.getId() == suffixId) {
            changeActiveSuffix(activeSuffix);
        }

        return suffix;
    }

    public void changeActiveSuffix(Suffix suffix) {
        if (suffix == null) {
            removeProperty("Suffix");
        } else {
            setProperty("Suffix", suffix.getId());
        }

        //вызываем эвент, чтобы обновить суффикс игрока в теге
        HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(this));
    }

    public boolean isVanished() {
        return getProperty(VanishListener.VANISH_PROPERTY_NAME) == 1;
    }

    public String toString() {
        return "Gamer[" + name + "]";
    }

    //=======================================
    //               PROPERTIES
    //=======================================
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

    /**
     * Получить проперти игрока на текущем сервере
     *
     * @param name Имя проперти
     * @return Значение проперти
     */
    public int getLocalProperty(String name) {
        return getProperty(getLocalPropertyName(name));
    }
    //=======================================

    /**
     * Добавить значение к существующей проперти игрока на текущем сервере
     *
     * @param name  Имя проперти
     * @param value Значение, которое добавится к существующей проперти
     */
    public void addLocalProperty(String name, int value) {
        addProperty(getLocalPropertyName(name), value);
    }

    /**
     * Установить проперти игрока на текущем сервере
     *
     * @param name  Имя проперти
     * @param value Новое значение
     */
    public void setLocalProperty(String name, int value) {
        setProperty(getLocalPropertyName(name), value);
    }
    //=======================================

    // region Purchases

    /**
     * Проверить, есть ли у игрока проперти на данном сервере
     *
     * @param name Имя проперти
     * @return true, если есть
     */
    public boolean hasLocalProperty(String name) {
        return hasProperty(getLocalPropertyName(name));
    }

    private String getLocalPropertyName(@NonNull String name) {
        return CoreUtil.getServerName() + "/" + name;
    }

    public boolean hasProperty(@NonNull String property) {
        return properties != null && properties.containsKey(property.toLowerCase());
    }

    public int getProperty(@NonNull String property) {
        return properties == null ? 0 : properties.get(property.toLowerCase());
    }

    public void addProperty(@NonNull String property, int value) {
        if (properties == null) {
            return;
        }

        setProperty(property, getProperty(property) + value);
    }

    // endregion

    // region social media

    public void setProperty(@NonNull String property, int value) {
        if (updateProperty(property, value)) {
            Connection.sendPacket(new BSetProperty(playerId, property, value));
        }
    }

    public boolean updateProperty(@NonNull String property, int value) {
        if (properties == null) {
            return false;
        }

        int oldValue = getProperty(property);

        if (value == oldValue) {
            return false;
        }

        GamerPropertyChangeEvent event = new GamerPropertyChangeEvent(this, property, oldValue, value);
        HeartBeat.callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        properties.put(property.toLowerCase(), value);

        return true;
    }

    public boolean dropProperty(@NonNull String property) {
        val propertyName = property.toLowerCase();

        if (properties != null && properties.containsKey(propertyName)) {
            properties.remove(propertyName);
            return true;
        }

        return false;
    }

    public void removeProperty(@NonNull String property) {
        if (dropProperty(property) && hasPlayerId()) {
            Connection.sendPacket(new BRemoveProperty(playerId, property));
        }
    }

    // region Stats

    //=======================================
    //                VOTES
    //=======================================
    public int getVotes() {
        return getProperty("Votes");
    }

    public void takeVotes(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Votes amount must be positive");
        }

        setProperty("Votes", getVotes() - amount);
    }

    public void loadPurchases() {
        loadPlayerId();

        //данные о группе уже загружены
        //онлайн игроки проверку игнорят - данные возможно придется обновлять
        if (!isOnline() && purchases != null) {
            return;
        }

        purchases = PurchaseSqlHandler.INSTANCE.getPurchases(playerId).join();
    }

    public boolean hasPurchase(int id) {
        return purchases.contains(id);
    }

    public TIntList getPurchases() {
        return TCollections.unmodifiableList(purchases);
    }

    public void addPurchase(int id, int cost, @NonNull GameCurrency currency) {
        if (!purchases.add(id)) {
            return;
        }

        if (hasPlayerId()) {
            PurchaseSqlHandler.INSTANCE.addPurchase(playerId, id, cost, currency);
        }
    }

    public void removePurchase(int id) {
        if (!purchases.remove(id)) {
            return;
        }

        if (hasPlayerId()) {
            PurchaseSqlHandler.INSTANCE.removePurchase(playerId, id);
        }
    }

    public boolean isExternalPermissionsLoad() {
        return externalPermissionEntryList != null;
    }

    public void loadExternalPermissions() {
        loadPlayerId();

        if (!isOnline() && (!hasPlayerId() || isExternalPermissionsLoad())) {
            return;
        }

        externalPermissionEntryList = ExternalPermissionsSqlHandler.INSTANCE.loadPlayerInfo(playerId).join();
    }


    public boolean isSocialMediaLoaded() {
        return socialMediaLinkMap != null;
    }

    public void loadSocialMediaLinks() {
        loadPlayerId();

        if (!isOnline() && (!hasPlayerId() || isSocialMediaLoaded())) {
            return;
        }

        socialMediaLinkMap = SocialMediaSqlHandler.INSTANCE.loadSocialMediaLinks(playerId);
    }

    public String getSocialMediaLink(@NonNull SocialMediaType mediaType) {
        return socialMediaLinkMap.get(mediaType);
    }

    public void changeSocialMediaLink(@NonNull SocialMediaType mediaType, String value) {
        if (value == null) {
            socialMediaLinkMap.remove(mediaType);
        } else {
            socialMediaLinkMap.put(mediaType, value);
        }

        SocialMediaSqlHandler.INSTANCE.setSocialMediaLinkValue(playerId, mediaType, value);
    }

    public GameStatsManager getStatsManager() {
        return game == null ? GameStatsManager.DEFAULT : game.getStatsManager();
    }

    public void setGame(Game game) {
        Game previousGame = this.game;

        if (previousGame == game) {
            return;
        }

        this.game = game;

        if (previousGame == null && game != null) {
            GameStatsManager.DEFAULT.transferStatsCache(this, game.getStatsManager());
        }
    }

    public GameStatsCache getStatsCache() {
        val manager = getStatsManager();

        return manager.getOrCreateStatsCache(this);
    }

    /**
     * Сбросить на 0 статистику за все время и за сегодня
     *
     * @param field - поле статистики
     */
    public void resetStat(@NonNull String field) {
        int today = (int) TimeUtil.getDayUTC();

        addStat(field, -getLifetimeStat(field), 0);
        addStat(field, -getStatByDay(field, today), today);
    }

    /**
     * Сбросить на 0 статистику за определенный день
     *
     * @param field - поле статистики
     * @param day   - день за который сбрасывать
     */
    public void resetStat(@NonNull String field, int day) {
        addStat(field, -getStatByDay(field, day), day);
    }

    public void resetModeStat(@NonNull ServerMode mode, @NonNull String field) {
        addModeStat(mode, field, -getLifetimeStat(mode, field));
    }

    /**
     * Сбросить на 0 статистику за все время и за сегодня на определенном режиме
     *
     * @param field - поле статистики
     */
    public void resetStat(@NonNull StatsMode statsMode, @NonNull String field) {
        StatsField statsField = statsMode.getOptionalField(field);

        if (statsField != null) {
            resetStat(statsField);
        }
    }

    /**
     * Сбросить на 0 статистику за определенный день на определенном режиме
     *
     * @param statsMode - режим статистики
     * @param field     - поле статистики
     * @param day       - день за который сбрасывать
     */
    public void resetStat(@NonNull StatsMode statsMode, @NonNull String field, int day) {
        StatsField statsField = statsMode.getOptionalField(field);

        if (statsField != null) {
            resetStat(statsField, day);
        }
    }

    /**
     * Сбросить на 0 статистику за все время и за сегодня
     *
     * @param field - поле статистики
     */
    public void resetStat(@NonNull StatsField field) {
        resetStat(field, 0);
        resetStat(field, (int) TimeUtil.getDayUTC());
    }

    /**
     * Сбросить на 0 статистику за определенный день
     *
     * @param field - поле статистики
     * @param day   - день за который сбрасывать
     */
    public void resetStat(@NonNull StatsField field, int day) {
        addStat(field, -getStatByDay(field, day), day);
    }

    /**
     * Добавить +1 к статистике (по опр. полю) за все время и за сегодня
     *
     * @param field - поле статистики
     */
    public void addStat(@NonNull String field) {
        addStat(field, 1);
    }

    public void addStat(@NonNull Mini mini, @NonNull String field, int value) {
        addModeStat(mini, field, value);
        addModeStat(mini.getMaster(), field, value);
    }

    public void takeStat(@NonNull StatsMode statsMode, @NonNull String field, int delta) {
        StatsField statsField = statsMode.getOptionalField(field);

        if (statsField != null) {
            addStat(statsField, -delta);
        }
    }

    public void addModeStat(@NonNull ServerMode mode, @NonNull String field, int value) {
        StatsMode statsMode = StatsManager.INSTANCE.tryGetMode(mode.getName());

        if (statsMode == null) {
            return;
        }

        StatsField statsField = statsMode.getField(field);

        if (statsField == null) {
            return;
        }

        addStat(statsField, value);
    }

    /**
     * Добавить статистику (по опр. полю) за все время и за сегодня
     *
     * @param field - поле статистики
     * @param delta - добавляемое значение
     */
    public void addStat(@NonNull String field, int delta) {
        if (game != null) {
            addStat(game.getMini(), field, delta);
            return;
        }

        StatsMode mode = getStatsMode();

        if (mode == null) {
            return;
        }

        addStat(mode, field, delta);
    }

    /**
     * Добавить статистику (по опр. полю) за опр. день
     *
     * @param field - поле статистики
     * @param delta - добавляемое значение
     * @param day   - день для которого добавлять
     */
    public void addStat(@NonNull String field, int delta, int day) {
        StatsMode mode = getStatsMode();

        if (mode == null) {
            return;
        }

        addStat(mode, field, delta, day);
    }

    /**
     * Добавить +1 к статистике (по опр. полю) за все время и за сегодня
     *
     * @param field - поле статистики
     */
    public void addStat(@NonNull StatsMode statsMode, @NonNull String field) {
        addStat(statsMode, field, 1);
    }

    /**
     * Добавить статистику (по опр. полю) за все время и за сегодня
     *
     * @param field - поле статистики
     * @param delta - значение для добавления
     */
    public void addStat(@NonNull StatsMode statsMode, @NonNull String field, int delta) {
        StatsField statsField = statsMode.getOptionalField(field);

        if (statsField != null) {
            addStat(statsField, delta);
        }
    }

    /**
     * Добавить статистику (по опр. полю) за все время и за сегодня
     *
     * @param field - поле статистики
     * @param delta - добавляемое значение
     */
    public void addStat(@NonNull StatsMode statsMode, @NonNull String field, int delta, int day) {
        StatsField statsField = statsMode.getOptionalField(field);

        if (statsField != null) {
            addStat(statsField, delta, day);
        }
    }

    /**
     * Добавить +1 к статистике (по опр. полю) за все время и за сегодня
     *
     * @param field - поле статистики
     */
    public void addStat(@NonNull StatsField field) {
        addStat(field, 1);
    }

    /**
     * Добавить статистику (по опр. полю) за все время и за сегодня
     *
     * @param field - поле статистики
     * @param value - добавляемое значение
     */
    public void addStat(@NonNull StatsField field, int value) {
        addStat(field, value, 0);
        addStat(field, value, (int) TimeUtil.getDayUTC());
    }

    /**
     * Добавить статистику (по опр. полю) за определенный день
     *
     * @param field - поле статистики
     * @param value - добавляемое значение
     * @param day   - день за который добавлять
     */
    public void addStat(@NonNull StatsField field, int value, int day) {
        StatsMode statsMode = field.getMode();

        TIntObjectMap<TObjectIntMap<StatsField>> loaded = totalStats.get(statsMode);

        // сохраняем стату, чтобы в /stats игрок видел уже актуальную
        if (loaded != null) {
            TObjectIntMap<StatsField> dailyStats = loaded.get(day);

            if (dailyStats == null) {
                dailyStats = new TObjectIntHashMap<>();
            }

            dailyStats.adjustOrPutValue(field, value, value);

            loaded.put(day, dailyStats);
        }

        resolveGame();

        getStatsCache().addStat(field, value, day);
    }

    /**
     * Получить статистику ЗА ВСЕ ВРЕМЯ по определенному полю на текущем режиме
     * Текущий режим берется из общего серверного или из текущей игры,
     * если такая у игрока есть
     *
     * @param field - поле статистики
     */
    public int getLifetimeStat(@NonNull String field) {
        return getStatByDay(field, 0);
    }

    // base stats ending

    /**
     * Получить статистику ЗА ВСЕ ВРЕМЯ по определенному полю
     *
     * @param field - поле статистики
     */
    public int getLifetimeStat(@NonNull StatsField field) {
        return getStatByDay(field, 0);
    }

    /**
     * Получить статистику ЗА ВСЕ ВРЕМЯ по определенному полю на определенном режиме
     *
     * @param field - поле статистики
     */
    public int getLifetimeStat(@NonNull StatsMode statsMode, @NonNull String field) {
        return getStatByDay(statsMode, field, 0);
    }

    public int getLifetimeStat(@NonNull ServerMode mode, @NonNull String field) {
        return getLifetimeStat(mode, field, 0);
    }

    public int getLifetimeStat(@NonNull ServerMode mode, @NonNull String field, int day) {
        StatsMode statsMode = StatsManager.INSTANCE.tryGetMode(mode.getName());

        return statsMode == null ? 0 : getStatByDay(statsMode, field, day);
    }

    /**
     * Получить статистику ЗА СЕГОДНЯ по определенному полю на текущем режиме
     * Текущий режим берется из общего серверного или из текущей игры,
     * если такая у игрока есть
     *
     * @param field - поле статистики
     */
    public int getTodayStat(@NonNull String field) {
        return getStatByDay(field, (int) TimeUtil.getDayUTC());
    }


    /**
     * Получить статистику ЗА СЕГОДНЯ по определенному полю на определенном режиме
     *
     * @param statsMode - режим статистики
     * @param field     - поле статистики
     */
    public int getTodayStat(@NonNull StatsMode statsMode, @NonNull String field) {
        return getStatByDay(statsMode, field, (int) TimeUtil.getDayUTC());
    }

    /**
     * Получить статистику ЗА СЕГОДНЯ по определенному полю на текущем режиме
     * Текущий режим берется из общего серверного или из текущей игры,
     * если такая у игрока есть
     *
     * @param field - поле статистики
     */
    public int getTodayStat(@NonNull StatsField field) {
        return getStatByDay(field, (int) TimeUtil.getDayUTC());
    }

    // endregion

    /**
     * Получить статистику по определенному полю на текущем режиме за опр. день
     * Текущий режим берется из общего серверного или из текущей игры,
     * если такая у игрока есть
     *
     * @param field - поле статистики
     * @param day   - день по которому получать
     */
    public int getStatByDay(@NonNull String field, int day) {
        StatsMode statsMode = getStatsMode();

        return statsMode == null ? 0 : getStatByDay(statsMode, field, day);
    }

    /**
     * Получить статистику по определенному полю на определенном режиме за опр. день
     *
     * @param statsMode - режим статистики
     * @param field     - поле статистики
     * @param day       - день по которому получать
     */
    public int getStatByDay(@NonNull StatsMode statsMode, @NonNull String field, int day) {
        StatsField statsField = statsMode.getOptionalField(field);

        return statsField == null ? 0 : getStatByDay(statsField, day);
    }

    // region Coins

    /**
     * Получить статистику по определенному полю за опр. день
     *
     * @param field - поле статистики
     * @param day   - день по которому получать
     */
    public int getStatByDay(@NonNull StatsField field, int day) {
        TIntObjectMap<TObjectIntMap<StatsField>> loaded = totalStats.get(field.getMode());

        if (loaded == null) {
            return 0;
        }

        TObjectIntMap<StatsField> byDay = loaded.get(day);

        if (byDay == null) {
            return 0;
        }

        return byDay.get(field);
    }

    /**
     * Получить статистику ОТ определенного дня по определенному полю
     *
     * @param field    - поле статистики
     * @param dayStart - день с которого получать (до текущего)
     */
    public int getStatFromDay(@NonNull StatsField field, int dayStart) {
        TIntList statValues = new TIntArrayList();

        for (int day = dayStart; day <= TimeUtil.getDayUTC(); day++) {
            statValues.add(getStatByDay(field, day));
        }

        int value = 0;

        switch (field.getTemporaryDisplayMode()) {
            case "sum": {
                value = statValues.sum();
                break;
            }
            case "max": {
                value = statValues.max();
                break;
            }
            case "min": {
                value = statValues.min();
                break;
            }
            case "last": {
                value = statValues.isEmpty() ? 0 : statValues.get(statValues.size() - 1);
            }
        }

        return value;
    }

    public boolean isLoaded(StatsMode mode) {
        return totalStats.containsKey(mode);
    }

    public void loadStats() {
        //если это игровой сервер или какой-нибудь скайблок,
        //то грузим только его стату. Если нет, то грузим всю
        //статистику по всем режимам (для /stats итд)
        if (CoreUtil.isMaster()) {
            Master master = CoreUtil.getMaster();

            StatsMode overallStats = StatsManager.INSTANCE.tryGetMode(master);

            if (overallStats != null) {
                loadStatsFor(overallStats);
            }

            for (Mini mini : master.getMinis()) {
                StatsMode miniStats = StatsManager.INSTANCE.tryGetMode(mini);

                if (miniStats != null) {
                    loadStatsFor(miniStats);
                }
            }
        } else if (CoreUtil.isUncommon()) {
            StatsMode statsMode = getStatsMode();

            if (statsMode != null) {
                loadStatsFor(statsMode);
            }
        } else {
            loadAllStats();
        }
    }

    public void loadStatsFor(@NonNull StatsMode mode) {
        if (!hasPlayerId()) {
            return;
        }

        int firstMonthDay = StatisticPeriod.MONTH.getFromDay();
        int weekStartDay = StatisticPeriod.WEEK.getFromDay();

        // неделя может быть раньше чем 1 число месяца
        // поэтому грузим стату от минимальной даты - либо от начала недели, тогда
        // оно захватит и месяц; либо от начала месяца
        int loadFrom = Math.min(firstMonthDay, weekStartDay);

        totalStats.put(mode, StatsSqlHandler.INSTANCE.loadPlayerStats(mode, playerId, loadFrom).join());
    }

    public boolean isAllStatsLoaded() {
        for (StatsMode mode : StatsManager.INSTANCE.getModes()) {
            if (!isLoaded(mode)) {
                return false;
            }
        }

        return true;
    }

    public void loadAllStats() {
        for (StatsMode mode : StatsManager.INSTANCE.getModes()) {
            if (!isLoaded(mode)) {
                loadStatsFor(mode);
            }
        }
    }

    public int getCachedStat(@NonNull String field) {
        return getCachedStat(getStatsMode().getField(field));
    }

    public int getCachedStat(@NonNull StatsField field) {
        return getStatsCache().getCachedStat(field, 0);
    }

    public void loadLastLoginTime() {
        loadPlayerId();

        if (hasPlayerId() && authData.isLoaded()) {
            Session session = authData.getLastSession();

            if (session != null) {
                lastLoginTime = session.getLeftTimestamp();
            }

            lastLoginTimeLoad = LocalDateTime.now();
        }
    }

    public void loadAuthData() {
        loadPlayerId();

        if (!isOnline() && isAuthDataLoaded()) {
            return;
        }

        val authPlayerData = new BaseAuthPlayerData(name);
        authPlayerData.load();

        this.authData = authPlayerData;
    }

    public boolean isAuthDataLoaded() {
        return authData != null && authData.isLoaded();
    }

    public boolean isLastLoginTimeCached() {
        return lastLoginTimeLoad != null && java.time.Duration.between(lastLoginTimeLoad, LocalDateTime.now()).toMinutes() <= 15;
    }

    public void loadVoteData() {
        loadPlayerId();

        //данные о голосованиях игрока
        //онлайн игроки проверку игнорят - данные возможно придется обновлять
        if (!isOnline() && playerVoteData != null) {
            return;
        }

        playerVoteData = VoteSqlHandler.INSTANCE.getPlayerVotes(playerId).join();
    }

    // start region money

    /**
     * Получит игровую валюту по режиму UNCOMMON
     *
     * @param uncommon - режим
     * @return количество игровой валюты
     */
    public int getCoins(@NonNull Uncommon uncommon) {
        return getCoins(uncommon.name());
    }

    /**
     * Получит игровую валюту по режиму UNCOMMON
     *
     * @param master - master режима
     * @return количество игровой валюты
     */
    public int getCoins(@NonNull Master master) {
        return getCoins(master.name());
    }

    /**
     * Получить игровую валюту по текущему мастеру
     *
     * @return количество игровой валюты
     */
    public int getCoins() {
        Master master = CoreUtil.getMaster();
        return master == null ? 0 : getCoins(master);
    }

    public int getCoins(@NotNull final ServerMode mode) {
        return getCoins(mode.getName());
    }

    private int getCoins(@NonNull String mode) {
        return getProperty(mode + "/Coins");
    }

    /**
     * Выдача игровой валюты (коинов)
     *
     * @param coins количество коинов
     * @return количество игровой валюты
     */
    public int addCoins(int coins) {
        return addCoins(coins, null);
    }

    /**
     * Выдача игровой валюты (коинов)
     *
     * @param coins  количество коинов
     * @param reason причина выдачи
     * @return количество игровой валюты
     */
    public int addCoins(int coins, String reason) {
        Master master = CoreUtil.getMaster();
        return master == null ? 0 : addCoins(master, coins, reason);
    }

    /**
     * Выдача игровой валюты (коинов)
     *
     * @param coins    количество коинов
     * @param uncommon режим
     * @return количество игровой валюты
     */
    public int addCoins(@NonNull Uncommon uncommon, int coins) {
        return addCoins(uncommon, coins, null);
    }

    /**
     * Выдача игровой валюты (коинов)
     *
     * @param coins  количество коинов
     * @param master режим
     * @return количество игровой валюты
     */
    public int addCoins(@NonNull Master master, int coins) {
        return addCoins(master, coins, null);
    }

    /**
     * Выдача игровой валюты (коинов)
     *
     * @param coins    количество коинов
     * @param uncommon режим
     * @param reason   причина выдачи
     * @return количество игровой валюты
     */
    public int addCoins(@NonNull Uncommon uncommon, int coins, String reason) {
        return addCoins(uncommon.name(), coins, true, reason);
    }

    /**
     * Выдача игровой валюты (коинов)
     *
     * @param coins  количество коинов
     * @param master режим
     * @param reason причина выдачи
     * @return количество игровой валюты
     */
    public int addCoins(@NonNull Master master, int coins, String reason) {
        return addCoins(master.name(), coins, true, reason);
    }

    /**
     * Выдача игровой валюты (коинов)
     *
     * @param coins    количество коинов
     * @param uncommon режим
     * @param multiply использовать активные бустеры
     * @param reason   причина выдачи
     * @return количество игровой валюты
     */
    public int addCoins(@NonNull Uncommon uncommon, int coins, boolean multiply, String reason) {
        return addCoins(uncommon.name(), coins, multiply, reason);
    }

    /**
     * Выдача игровой валюты (коинов)
     *
     * @param coins    количество коинов
     * @param master   режим
     * @param multiply использовать активные бустеры
     * @param reason   причина выдачи
     * @return количество игровой валюты
     */
    public int addCoins(@NonNull Master master, int coins, boolean multiply, String reason) {
        return addCoins(master.name(), coins, multiply, reason);
    }

    private int addCoins(@NonNull String mode, int initial, boolean multiply, String reasonLanguageKey) {
        resolveGame();

        if (game != null && game.isStatsSavingDisabled()) {
            return 0;
        }

        int coins = initial;

        StringBuilder hover = new StringBuilder();

        //если множитель работает...
        if (multiply) {
            List<Multiplier> multipliers = getMultipliers(MultiplierType.COINS);

            if (!multipliers.isEmpty()) {
                coins = coinAccumulator.add(initial * getMultiplier(multipliers));

                language.getMessageList(
                        "multiplier_add_lore_initial",
                        "%color%", "§6", "%initial_value%", initial
                ).forEach(line -> hover.append(line).append("\n"));

                boolean first = true;

                for (Multiplier multiplier : multipliers) {
                    if (first) {
                        first = false;
                    } else {
                        hover.append("\n");
                    }

                    hover.append(" ").append(multiplier.getDisplayName(language));
                }
            }
        }

        addProperty(mode + "/Coins", coins);

        //сообщение не требуется
        if (reasonLanguageKey == null) {
            return coins;
        }


        String coinsForm = CommonWords.COIN.get(language).fromNumber(coins).toString();
        String msg = " §6+" + Leveling.formatGrouped(coins) + " " + coinsForm + (mode.isEmpty() ? "" : " " + mode);

        if (!reasonLanguageKey.isEmpty()) {
            msg += " (" + (language.isMessageTranslated(reasonLanguageKey) ? language.getMessage(reasonLanguageKey) : reasonLanguageKey) + ")";
        }

        String hoverString = hover.toString();

        if (hoverString.isEmpty()) {
            hoverString = language.getMessage("coin_no_multipliers_used_lore");
        }

        sendJsonMessage(String.format(JSON.HOVER_TEXT_LAYOUT, msg, hoverString));

        return coins;
    }


    /**
     * Проверить доступна ли сумма для снятия
     *
     * @param uncommon режима
     * @param amount   сумма
     * @return True - доступна, False - нет
     */
    public boolean hasCoins(@NonNull Uncommon uncommon, int amount) {
        return hasCoins(uncommon.name(), amount);
    }

    /**
     * Проверить доступна ли сумма для снятия
     *
     * @param master режим
     * @param amount сумма
     * @return True - доступна, False - нет
     */
    public boolean hasCoins(@NonNull Master master, int amount) {
        return hasCoins(master.name(), amount);
    }

    /**
     * Проверить доступна ли сумма для снятия по текущему мастеру
     *
     * @param amount сумма
     * @return True - доступна, False - нет
     */
    public boolean hasCoins(int amount) {
        return hasCoins(CoreUtil.getMaster(), amount);
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
        takeCoins(CoreUtil.getMaster().name(), take);
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
    // end region money

    // External Permission region

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
    // end region

    // Start mystery dust region

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
    // end region mystery dust

    public ChatMode getChatModeUnchecked() {
        return ChatMode.getById(getProperty("ChatMode"));
    }

    public ChatMode getChatMode() {
        ChatMode mode = getChatModeUnchecked();

        if (!mode.check(this)) {
            setChatMode(mode = ChatMode.FALLBACK);
        }

        return mode;
    }

    public void setChatMode(@NonNull ChatMode chatMode) {
        setProperty("ChatMode", chatMode.ordinal());
    }

    public int getExperience() {
        return getProperty("Experience");
    }

    public int getLevel() {
        return Leveling.getLevel(getExperience());
    }

    // endregion

    // region Party

    public void addExperience(int experience) {
        addExperience(experience, null);
    }

    public void addExperience(int experience, String reason) {
        addExperience(experience, true, reason);
    }

    public void addExperience(int initial, boolean multiply, String reasonLanguageKey) {
        resolveGame();

        if (game != null && game.isStatsSavingDisabled()) {
            return;
        }

        int level = getLevel();
        int experience = initial;

        StringBuilder hover = new StringBuilder();

//        //если множитель работает...
//        if (multiply) {
//            List<Multiplier> multipliers = getMultipliers(MultiplierType.EXPERIENCE);
//
//            if (!multipliers.isEmpty()) {
//                experience = xpAccumulator.add(initial * getMultiplier(multipliers));
//
//                language.getMessageList(
//                        "multiplier_add_lore_initial",
//                        "%color%", "§3", "%initial_value%", initial
//                ).forEach(line -> hover.append(line).append("\n"));
//
//                boolean first = true;
//
//                for (Multiplier multiplier : multipliers) {
//                    if (first) {
//                        first = false;
//                    } else {
//                        hover.append("\n");
//                    }
//
//                    hover.append(" ").append(multiplier.getDisplayName(language));
//                }
//            }
//        }

        addProperty("Experience", experience);

        //вызываем событие изменения опыта игрока
        HeartBeat.callEvent(new ExperienceChangeEvent(this));

        //уровень игрока повысился...
        if (level != getLevel()) {
            HeartBeat.callEvent(new LevelUpEvent(this));
        }

        //сообщение не требуется
        if (reasonLanguageKey == null) {
            return;
        }

        String msg = " §3+" + Leveling.formatGrouped(experience) + " XP";

        if (!reasonLanguageKey.isEmpty()) {
            msg += " (" + (language.isMessageTranslated(reasonLanguageKey) ? language.getMessage(reasonLanguageKey) : reasonLanguageKey) + ")";
        }

        String hoverString = hover.toString();

        if (hoverString.isEmpty()) {
            hoverString = language.getMessage("experience_no_multipliers_used_lore");
        }

        sendJsonMessage(String.format(JSON.HOVER_TEXT_LAYOUT, msg, hoverString));
    }

    // region Boosters
    public void loadBooster() {
        loadPlayerId();

        //данные о бустере
        //онлайн игроки проверку игнорят - данные возможно придется обновлять
        if (!isOnline() && booster != null) {
            return;
        }

        this.booster = BoosterSqlHandler.INSTANCE.loadBooster(playerId);
    }

    public Booster getBooster() {
        if (booster == null || booster.isExpired()) {
            return null;
        }

        return booster;
    }

    public void activateBooster(@NonNull Booster activeBooster) {
        this.booster = activeBooster;

        if (hasPlayerId()) {
            BoosterSqlHandler.INSTANCE.setBooster(playerId, activeBooster);
        }
    }

    public boolean hasParty() {
        return party != null;
    }

    public boolean isLeader() {
        return party != null && party.getLeader() == this;
    }

    public boolean isSameParty(Party party) {
        return this.party != null && this.party == party;
    }

    public double getMultiplier(@NonNull MultiplierType type) {
        return getMultiplier(getMultipliers(type));
    }

    public double getMultiplier(@NonNull List<Multiplier> list) {
        double result = 1.0;

        for (Multiplier multiplier : list) {
            result += multiplier.getValue();
        }

        return result;
    }

    public List<Multiplier> getMultipliers(@NonNull MultiplierType type) {
        List<Multiplier> list = new ArrayList<>();

        val rank = ranks.findPriorityRank();

        switch (type) {
            case EXPERIENCE: {
                if (hasPermission("multiplier.exp")) {
                    list.add(new DonateExpMultiplier(getRanks()));
                }

                break;
            }
            case COINS: {
                if (hasPermission("multiplier.coins")) {
                    list.add(new DonateCoinsMultiplier(rank.rank()));
                }

                LevelMultiplier multiplier = new LevelMultiplier(getLevel());

                if (multiplier.getValue() > 0) {
                    list.add(multiplier);
                }
            }
        }

        Booster booster = getBooster();

        if (booster != null && booster.getType() == type) {
            list.add(booster);
        }

        return list;
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

        GamerSettingToggleEvent settingToggleEvent = new GamerSettingToggleEvent(this, setting, state);
        HeartBeat.callEvent(settingToggleEvent);
    }

    public void loadFriends() {
        loadPlayerId();

        if (!isOnline() && friendIds != null) {
            return;
        }

        friendIds = FriendSqlHandler.INSTANCE.getFriends(playerId)
                .join();
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

    // endregion

    public boolean isFriend(Gamer gamer) {
        return isFriend(gamer.getPlayerId());
    }

    public TIntSet getFriendIds() {
        if (friendIds == null) {
            return new TIntHashSet();
        }

        return TCollections.unmodifiableSet(friendIds);
    }

    public int getFriendLimit() {
        return FriendSqlHandler.INSTANCE.getFriendLimit(ranks);
    }

    public List<Gamer> getOnlineFriends() {
        if (friendIds == null) {
            return Collections.emptyList();
        }

        return IntStream.of(friendIds.toArray()).mapToObj(Gamer::getOfflineGamer)
                .filter(gamer -> gamer != null && gamer.getServer() != null && (canSeeVanish() || !gamer.isVanished()))
                .collect(Collectors.toList());
    }

    public boolean canSeeVanish() {
        return isVanished() || hasPermission("core.vanish.seevanished");
    }

    public void loadIgnoreList() {
        if (ignoreList != null) {
            return;
        }

        ignoreList = IgnoreListSqlHandler.INSTANCE.getPlayerIgnoreList(playerId).join();
    }

    /**
     * Добавить игрока в очередь
     *
     * @param mini тип игры
     * @return удалось ли добавить игрока в очередь
     */
    public boolean joinToMiniQueue(@NonNull Mini mini) {
        if (party != null && party.getLeader() != this) {
            this.sendLocalizedMessage(GamerQueueJoinResult.ONLY_PARTY_LEADER.formatToLocaleKey());
            return false;
        }

        AtomicBoolean successJoin = new AtomicBoolean(false);
        Connection.awaitPacket(new CGamerQueueJoinRequest(this.playerId, mini))
                .whenComplete(((response, throwable) -> {
                    if (throwable != null) {
                        Logging.IMP.warn("[QUEUE] Failed add gamer({} to queue type {}. Details: ", this.name, mini.getName());
                        throwable.printStackTrace();
                        successJoin.set(false);
                        this.sendLocalizedMessage(GamerQueueJoinResult.FAIL.formatToLocaleKey());
                        return;
                    }

                    GamerQueueJoinResult responseResult = response.getResult();

                    this.sendLocalizedMessage(responseResult.formatToLocaleKey());

                    currentQueueMini = mini;
                    successJoin.set(true);
                }));

        return successJoin.get();
    }

    /**
     * Удалить игрока из очереди
     *
     * @param mini тип очереди
     */
    public void exitMiniQueue(@NonNull Mini mini) {
        Connection.sendPacket(new CGamerLeaveQueue(mini, playerId));
    }

    /**
     * Удалить игрока из текущей очереди
     */
    public void exitCurrentMiniQueue() {
        if (currentQueueMini == null) {
            return;
        }

        this.exitMiniQueue(currentQueueMini);
    }

    /**
     * Добавить игрока в список игнорируемых
     *
     * @param playerId ID игрока
     */
    public void addIgnoredPlayer(int playerId) {
        ignoreList.add(playerId);
    }

    /**
     * Удалить игрока из списка игнорируемых
     *
     * @param playerId ID игрока
     */
    public void removeIgnoredPlayer(int playerId) {
        ignoreList.remove(playerId);
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
     * Получить числовые ID списка игнорируемых
     *
     * @return коллекция числовых ID друзей
     */
    public TIntList getIgnoreList() {
        return TCollections.unmodifiableList(ignoreList);
    }

    public boolean isSpectator() {
        return gamerState == GamerState.SPECTATOR;
    }

    public String getLowerName() {
        return name.toLowerCase();
    }

    public void updateDisplayName() {
        var rank = ranks.findPriorityRank().rank();

        prefixText = getRankPrefixModifier(rank.getPrefix());
        realPrefixText = prefixText;

        if (fakeName != null) {
            if (Project.CURRENT == Project.FAKEPIXEL) {
                prefixText = getRankPrefixModifier().modify(fakeName.getGroup().getPrefix());
            } else {
                prefixText = getRankPrefixModifier(fakeName.getGroup().getPrefix());
            }
        }

        if (fakeName == null && prefix != null) {
            prefixText = getRankPrefixModifier(prefix.getText());
        }

        if (Project.CURRENT == Project.FAKEPIXEL) {
            this.displayName = getRankPrefixModifier().modify(prefixText + getPlayerName());
        } else {
            this.displayName = prefixText + getPlayerName();
        }
    }

    public RankPrefixModifier getRankPrefixModifier() {
        if (getProperty("selected-plus-color") >= RankPrefixModifier.VALUES.length) {
            return RankPrefixModifier.VALUES[0];
        }

        return RankPrefixModifier.VALUES[getProperty("selected-plus-color")];
    }

    public @Nullable SuffixColor getSelectedPrefixColor() {
        if (!hasProperty("prefix-color")) return null;

        int colorId = getProperty("prefix-color");

        if (colorId <= -1) return null;
        return SuffixColor.getById(colorId);
    }

    public void changePrefixColor(@Nullable SuffixColor color) {
        if (color == null) {
            removeProperty("prefix-color");
            return;
        }

        setProperty("prefix-color", color.getId());
    }

    public String getRankPrefixModifier(@NonNull String prefixText) {
        val canUse = ranks.hasPermission("prefix.bold");

        if (getPriorityGroup().rank().hasPermission("core.staff") && !canUse) {
            return prefixText;
        }

        val color = getSelectedPrefixColor();
        if (color != null && canUse) {
            return "§" + color.getColorCode() + "§l" + club.abelix.commons.util.color.ChatColor.stripColor(prefixText);
        }

        return prefixText;
    }

    public String getTabListName() {
        String tablistName;

        if (Project.CURRENT == Project.FAKEPIXEL) {
            tablistName = getRankPrefixModifier().modify(prefixText);
        } else {
            tablistName = prefixText;
        }

        return isVanished() ? "§8§l⚔ §8§o" : tablistName;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    public String getColorInChat() {
        return getShownGroup().getSuffix();
    }

    public Rank getShownGroup() {
        return fakeName == null ? getPriorityGroup().rank() : fakeName.getGroup();
    }

    public UserRank getPriorityGroup() {
        return getRanks().findPriorityRank();
    }

    public String getDisplayNameAndSuffix() {
        String suffix = getSuffixText();

        return getDisplayName() + (suffix.isEmpty() ? "" : " " + suffix);
    }

    public String getColoredName() {
        return getPrefixColor() + getDisplayName();
    }

    public String getColoredNameAndSuffix() {
        String suffix = getSuffixText();

        return getColoredName() + (suffix.isEmpty() ? "" : " " + suffix);
    }

    public boolean isDevMode() {
        return getProperty("Dev/Mode") != 0;
    }

    public void sendDevLog(String message) {
        if (isDevMode()) {
            sendMessage("§3[DEV] >> §f" + message);
        }
    }

    public void addSkyblockGems(int amount) {
        Connection.sendPacket(new SGiveSkyblockGems(name, amount));
    }

}
