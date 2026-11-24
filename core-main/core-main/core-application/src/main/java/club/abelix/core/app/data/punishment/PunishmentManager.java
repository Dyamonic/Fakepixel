package club.abelix.core.app.data.punishment;

import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.commons.data.property.PropertySqlHandler;
import club.abelix.commons.data.punishment.BansSqlHandler;
import club.abelix.commons.data.punishment.MutesSqlHandler;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.commons.data.punishment.meta.PunishmentMeta;
import club.abelix.commons.data.stat.staff.StaffStatsSqlHandler;
import club.abelix.commons.language.Language;
import club.abelix.commons.language.LanguageContext;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.IpAddressUtil;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.commons.util.json.JSON;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.commons.util.string.StringUtil;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.app.api.command.ConsoleCommandSender;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.punishment.event.PlayerBanDisconnectEvent;
import club.abelix.core.app.data.punishment.event.PlayerBanEvent;
import club.abelix.core.app.data.punishment.event.PlayerUnbanEvent;
import club.abelix.core.app.data.punishment.exception.PunishmentException;
import club.abelix.core.app.data.stats.StatsWipeManager;
import club.abelix.network.packet.play.server.SMuteChangeMuteState;
import club.abelix.network.packet.play.server.SUnmute;
import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.net.InetAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class PunishmentManager {

    // ----------------------------------------------------------------------

    @Dependency
    public static final PunishmentManager INSTANCE = new PunishmentManager();

    // ----------------------------------------------------------------------
    private final Cache<String, List<BanMeta>> recentlyRemovedBans = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    private final AtomicBoolean amnestyInProgress = new AtomicBoolean(false);
    /**
     * Данные обо всех банах
     */
    private List<BanMeta> banMetas;
    /**
     * Данные обо всех мутах
     */
    private List<MuteMeta> muteMetas;

    /**
     * Загрузка всех наказаний
     */
    public CompletableFuture<?> loadPunishments() {
        return CompletableFuture.allOf(
                MutesSqlHandler.INSTANCE.loadMuteList()
                        .whenComplete((result, cause) -> {
                            if (cause != null) {
                                log.info("An error occurred while loading mutes", cause);
                                return;
                            }

                            log.info("Totally loaded {} mutes", result.size());

                            muteMetas = result;
                        }),
                BansSqlHandler.INSTANCE.loadBanList()
                        .whenComplete((result, cause) -> {
                            if (cause != null) {
                                log.info("An error occurred while loading bans", cause);
                                return;
                            }

                            log.info("Totally loaded {} bans", result.size());

                            banMetas = result;
                        })
        );
    }

    /**
     * Получить список всех банов
     *
     * @return - список банов
     */
    public List<BanMeta> getBanMetas() {
        return Collections.unmodifiableList(banMetas);
    }

    /**
     * Получить список всех мутов
     *
     * @return - список банов
     */
    public List<MuteMeta> getMuteMetas() {
        return Collections.unmodifiableList(muteMetas);
    }

    /**
     * Получить активные баны по нику
     *
     * @param playerName - ник игрока
     * @return - активные баны
     */
    public List<BanMeta> getActiveNameBans(@NonNull String playerName) {
        if (banMetas == null) {
            return List.of();
        }

        return banMetas.stream()
                .filter(banMeta -> banMeta.containsName(playerName))
                .filter(banMeta -> !banMeta.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * Получить активные баны по IP
     *
     * @param address - IP игрока
     * @return - активные баны
     */
    public List<BanMeta> getActiveAddressBans(@NonNull String address) {
        if (banMetas == null) {
            return List.of();
        }

        return banMetas.stream()
                .filter(banMeta -> banMeta.containsAddress(address))
                .filter(banMeta -> !banMeta.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * Получить историю банов игрока или ip адреса
     *
     * @param target - ip или ник игрока
     * @return - история блокировок игрока
     */
    public List<BanMeta> getBanHistory(@NonNull String target) {
        return banMetas.stream()
                .filter(banMeta -> banMeta.containsAddress(target) || banMeta.containsName(target))
                .collect(Collectors.toList());
    }

    /**
     * Получить историю мутов игрока по его id
     *
     * @param playerId - id игрока
     * @return - история мутов игрока
     */
    public List<MuteMeta> getMuteHistory(int playerId) {
        return muteMetas.stream()
                .filter(muteMeta -> muteMeta.getPlayerId() == playerId)
                .collect(Collectors.toList());
    }

    public MuteMeta getActiveIdMute(int playerId) {
        return muteMetas.stream()
                .filter(meta -> meta.getPlayerId() == playerId)
                .filter(meta -> !meta.isExpired())
                .findFirst().orElse(null);
    }

    /**
     * Найти информацию о бане по указанному ID мута
     *
     * @param muteId - ID мута
     * @return - информация о муте
     */
    public Optional<MuteMeta> findMuteMeta(int muteId) {
        return muteMetas.stream()
                .filter(meta -> meta.getId() == muteId)
                .findFirst();
    }

    /**
     * Получить все баны начиная с указанного периода
     *
     * @param startDate - время начала периода
     * @return - баны за указанный период
     */
    public List<BanMeta> getBanMetas(long startDate) {
        return banMetas.stream()
                .filter(banMeta -> banMeta.getIssueDate() >= startDate)
                .sorted((ban1, ban2) -> (int) (ban1.getIssueDate() - ban2.getIssueDate()))
                .collect(Collectors.toList());
    }

    /**
     * Найти информацию о бане по указанному ID бана
     *
     * @param banId - ID бана
     * @return - информация о бане
     */
    public Optional<BanMeta> findBanMeta(int banId) {
        return banMetas.stream()
                .filter(banMeta -> banMeta.getId() == banId)
                .findAny();
    }

    /**
     * Добавить бан по нику к текущему бану
     *
     * @param banMeta    - информация о бане
     * @param playerName - ник игрока
     */
    public void addNameBan(@NonNull BanMeta banMeta, @NonNull String playerName) {
        if (banMeta.isExpired()) {
            throw new IllegalStateException("Can not add name ban to expired banmeta");
        }

        //добавляем ник в лист
        banMeta.addNames(Collections.singleton(playerName));
        //заносим инфу в БД
        BansSqlHandler.INSTANCE.addNameBan(banMeta.getId(), playerName);
    }

    /**
     * Добавить бан по нику к текущему бану
     *
     * @param banMeta - информация о бане
     * @param address - ip игрока
     */
    public void addIpBan(@NonNull BanMeta banMeta, @NonNull String address) {
        if (banMeta.isExpired()) {
            throw new IllegalStateException("Can not add ip ban to expired banmeta");
        }

        //добавляем ip в лист
        banMeta.addAddresses(Collections.singleton(address));
        //заносим инфу в БД
        BansSqlHandler.INSTANCE.addIpBan(banMeta.getId(), address);
    }

    /**
     * Добавить информацию о бане
     *
     * @param admin  - ник администратора
     * @param time   - время выдачи бана
     * @param expire - срок действия бана
     * @param reason - причина бана
     * @return - информация о бане в виде меты
     */
    private BanMeta addBanMeta(@NonNull String admin, long time, long expire, @NonNull String reason) {
        BanMeta banMeta = BansSqlHandler.INSTANCE.addBanMeta(admin, time, expire, reason)
                .join();

        banMetas.add(banMeta);

        return banMeta;
    }

    /**
     * Добавить информацию о муте
     *
     * @param playerId  - id игрока получившего мут
     * @param admin     - ник администратора
     * @param issueDate - время выдачи мута
     * @param time      - срок действия мута
     * @param reason    - причина мута
     * @return - информация о муте в виде меты
     */
    private MuteMeta addMuteMeta(int playerId, @NonNull String admin, long issueDate, long time, @NonNull String reason) {
        MuteMeta muteMeta = MutesSqlHandler.INSTANCE.addMuteMeta(playerId, admin, issueDate, time, reason)
                .join();

        muteMetas.add(muteMeta);

        return muteMeta;
    }

    /**
     * Завершить срок действия бана досрочно
     *
     * @param banMeta - информация о бане
     * @param admin   - администратор, разбанивший игрока
     */
    public void finishBanMeta(@NonNull BanMeta banMeta, @NonNull String admin) {
        long expirationDate = System.currentTimeMillis();

        //удаляем из кеша старую банмету (потому что она иммутабельна)
        banMetas.remove(banMeta);

        //создаем новую банмету
        BanMeta newBanMeta = new BanMeta(banMeta.getId(), banMeta.getAdminName(), admin, banMeta.getIssueDate(), expirationDate, banMeta.getUntranslatedReason());

        //добавляем в новую банмету все баны старой
        newBanMeta.addAddresses(banMeta.getAddresses());
        newBanMeta.addNames(banMeta.getNames());

        //и добавляем в общий список
        banMetas.add(newBanMeta);

        //изменяем данные в БД
        BansSqlHandler.INSTANCE.finishBanMeta(banMeta.getId(), expirationDate, admin);
    }

    /**
     * Полность удалить бан из базы
     *
     * @param meta - блокировка
     */
    public void removeBanMeta(@NonNull BanMeta meta) {
        // удаляем бан из кеша
        banMetas.remove(meta);

        // удаляем информацию из БД
        BansSqlHandler.INSTANCE.removeBanMeta(meta.getId());
    }

    /**
     * Полность удалить мут из базы
     *
     * @param meta - блокировка чата
     */
    public void removeMuteMeta(@NonNull MuteMeta meta) {
        // удаляем мут из кеша
        muteMetas.remove(meta);

        // удаляем информацию из БД
        MutesSqlHandler.INSTANCE.removeMuteMeta(meta.getId());
    }

    /**
     * Завершить срок действия мута досрочно
     *
     * @param muteMeta - информация о муте
     * @param admin    - администратор, размутивший игрока
     */
    public void finishMuteMeta(@NonNull MuteMeta muteMeta, @NonNull String admin) {
        long expire = System.currentTimeMillis();

        // удаляем из кеша старую мутмету (потому что она иммутабельна)
        muteMetas.remove(muteMeta);

        // создаем новую мутмету
        MuteMeta newMuteMeta = new MuteMeta(
                muteMeta.getId(), muteMeta.getPlayerId(), muteMeta.getAdminName(),
                admin, muteMeta.getIssueDate(), expire, muteMeta.getUntranslatedReason()
        );

        // заносим информацию о новой мете
        muteMetas.add(newMuteMeta);

        // завершаем этот мут в БД
        MutesSqlHandler.INSTANCE.finishMuteMeta(muteMeta.getId(), expire, admin);

        Gamer gamer = Gamer.getGamer(muteMeta.getPlayerId());

        // если игрок онлайн, надо отправить пакет о завершении срока действия мута
        if (gamer != null) {
            gamer.sendToProxy(new SUnmute(gamer.getPlayerId()));
        }
    }

    /**
     * Разбанить игрока
     *
     * @param initiator - ник администратора
     * @param target    - ник забаненного игрока (или ip)
     */
    public void processUnban(@NonNull String initiator, @NonNull String target) throws PunishmentException {
        CommandSender consoleSender = ConsoleCommandSender.INSTANCE;

        CommandSender sender = initiator.equalsIgnoreCase(consoleSender.getName()) ? consoleSender : null;

        if (sender == null) {
            sender = Gamer.getOfflineGamer(initiator, offline -> {
                offline.loadPlayerId();
                offline.loadRanks();
                offline.loadPrefix();
            });
        }

        processUnban(sender, target);
    }

    /**
     * Разбанить игрока
     *
     * @param sender - администратор
     * @param target - ник забаненного игрока (или ip)
     */
    public void processUnban(@NonNull CommandSender sender, @NonNull String target) throws PunishmentException {
        boolean isIp = Patterns.isIPV4(target);

        List<BanMeta> bans;

        if (isIp) {
            bans = getActiveAddressBans(target);
        } else {
            bans = getActiveNameBans(target);
        }

        if (bans.isEmpty()) {
            throw new PunishmentException("unban_not_banned");
        }

        List<BanMeta> toRemove;

        Gamer gamer = sender instanceof Gamer ? ((Gamer) sender) : null;

        boolean staff = !sender.hasPermission("core.staff");

        if (staff) {
            toRemove = bans.stream().filter(banMeta -> banMeta.getAdminName().equalsIgnoreCase(sender.getName())).collect(Collectors.toList());

            if (toRemove.isEmpty()) {
                throw new PunishmentException("unban_cant");
            }
        } else {
            toRemove = bans;
        }

        recentlyRemovedBans.put(target, toRemove);

        if (!sender.isOnline()) {
            sender.sendLocalizedMessage("unban_result",
                    "%target%", target
            );
        }

        for (BanMeta meta : toRemove) {
            finishBanMeta(meta, sender.getName());

            ServerManager.IMP.broadcastStaff(
                    "command.staffchat",
                    new LanguageContext(
                            isIp ? "unban_ip" : "unban_name",
                            language -> new ObjectLinkedBuf(
                                    "%name%", (gamer == null ? sender.getName() : gamer.getDisplayName()),
                                    "%count%", toRemove.size(),
                                    "%label%", language.getEnding(toRemove.size(), "ban"),
                                    "%target%", target
                            )
                    )
            );

            StatsWipeManager.INSTANCE.handleUserUnbanned(meta, sender);
        }

        if (!isIp) {
            val event = new PlayerUnbanEvent(target);

            CoreApplication.getInstance()
                    .getPluginManager()
                    .callEvent(event);
        }

        if (toRemove.size() < bans.size()) {
            int left = toRemove.size() - bans.size();

            Language language = sender.getLanguage();

            sender.sendLocalizedMessage("unban_more_" + (isIp ? "ip" : "name") + "_bans",
                    "%size_format%", CommonWords.BAN.get(language).withNumber(toRemove.size()),
                    "%left_format%", CommonWords.BAN.get(language).withNumber(left));
        }
    }

    /**
     * Снять мут с игрока
     *
     * @param initiator  - ник администратора
     * @param targetName - ник игрока в муте
     */
    public void processUnmute(@NonNull String initiator, @NonNull String targetName) throws PunishmentException {
        processUnmute(getCommandSender(initiator), targetName);
    }

    /**
     * Снять мут с игрока
     *
     * @param sender     - администратор
     * @param targetName - ник игрока в муте
     */
    public void processUnmute(@NonNull CommandSender sender, @NonNull String targetName) throws PunishmentException {
        if (!Patterns.isValidNick(targetName)) {
            throw new PunishmentException("player_incorrect_name");
        }

        Gamer gamer = sender instanceof Gamer ? ((Gamer) sender) : null;

        Gamer target = Gamer.getNickedGamer(targetName);

        // игрок оффлайн
        if (target == null) {
            target = Gamer.getOfflineGamer(targetName, Gamer::load);
        }

        if (target == null) {
            throw new PunishmentException("player_not_found");
        }

        MuteMeta muteMeta = getActiveIdMute(target.getPlayerId());

        if (muteMeta == null) {
            throw new PunishmentException("unmute_not_muted");
        }

        if (gamer != null
                && !sender.hasPermission("command.unmute.bypass")
                && !muteMeta.getAdminName().equalsIgnoreCase(gamer.getName())) {

            throw new PunishmentException("unmute_no_permission");
        }

        finishMuteMeta(muteMeta, sender.getName());

        if (!sender.isOnline()) {
            sender.sendLocalizedMessage("unmute_result",
                    "%target%", targetName
            );
        }

        ServerManager.IMP.broadcastStaff("command.staffchat", "unmute",
                "%name%", sender.getDisplayName(),
                "%target%", target.getDisplayName());
    }

    /**
     * Выдать мут игроку
     *
     * @param initiator  - ник администратора
     * @param targetName - игрок, которому выдается мут
     * @param reason     - причина мута
     * @param timeString - время мута
     */
    public MuteMeta processMute(@NonNull String initiator, @NonNull String targetName,
                                @NonNull String reason, @NonNull String timeString) throws PunishmentException {

        return processMute(getCommandSender(initiator), targetName, reason, timeString);
    }

    /**
     * Выдать мут игроку
     *
     * @param initiator  - ник администратора
     * @param targetName - игрок, которому выдается мут
     * @param reason     - причина мута
     * @param time       - время мута в мс.
     */
    public MuteMeta processMute(@NonNull String initiator, @NonNull String targetName,
                                @NonNull String reason, long time) throws PunishmentException {

        return processMute(getCommandSender(initiator), targetName, reason, time);
    }

    /**
     * Выдать мут игроку
     *
     * @param sender     - администратор
     * @param targetName - игрок, которому выдается мут
     * @param reason     - причина мута
     * @param timeString - время мута
     */
    public MuteMeta processMute(@NonNull CommandSender sender, @NonNull String targetName,
                                @NonNull String reason, @NonNull String timeString) throws PunishmentException {

        long time;

        try {
            time = Patterns.parseTime(timeString, TimeUnit.MILLISECONDS);
        } catch (IllegalArgumentException e) {
            throw new PunishmentException("mute_wrong_time");
        }

        if (sender instanceof Gamer && !sender.getRanks().hasPermission("mute.unlimited.time")) {
            val maxTime = Project.CURRENT == Project.FAKEPIXEL ? 30 : 14;

            if (time > TimeUnit.DAYS.toMillis(maxTime)) {
                throw new PunishmentException("mute_too_long_time");
            }
        }

        return processMute(sender, targetName, reason, time);
    }

    /**
     * Выдать мут игроку
     *
     * @param sender     - администратор
     * @param targetName - игрок, которому выдается мут
     * @param reason     - причина мута
     * @param time       - время мута в мс.
     */
    public MuteMeta processMute(@NonNull CommandSender sender, @NonNull String targetName,
                                @NonNull String reason, long time) throws PunishmentException {

        if (!Patterns.isValidNick(targetName)) {
            throw new PunishmentException("player_incorrect_name");
        }

        Gamer target = Gamer.getNickedGamer(targetName);

        // игрок оффлайн
        if (target == null) {
            target = Gamer.getOfflineGamer(targetName, Gamer::load);
        }

        if (target == null) {
            throw new PunishmentException("player_not_found");
        }

        target.updateDisplayName();

        // добавляем к сроку наказания муты за последние 14 дней
        // каждый мут мут дает на 50% больше времени
        List<MuteMeta> mutes = getMuteHistory(target.getPlayerId()).stream()
                .sorted(Comparator.comparing(muteMeta -> muteMeta.getIssueDate()
                        >= System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14)))
                .toList();

        MuteMeta activeMute = getActiveIdMute(target.getPlayerId());

        if (activeMute != null) {
            throw new PunishmentException("mute_already_given");
        }

        long currentTime = System.currentTimeMillis();

        val initiatorStaffGroup = sender.getRanks().findPriorityRank();
        val targetStaffGroup = target.getRanks().findPriorityRank();

        if (!sender.isDeveloper() && targetStaffGroup != null && targetStaffGroup.rank().getPriority() > initiatorStaffGroup.rank().getPriority()) {
            throw new PunishmentException("mute_cant");
        }

        MuteMeta muteMeta = addMuteMeta(target.getPlayerId(), sender.getName(), currentTime, time + currentTime, reason);

        Gamer finalTarget = target;

        if (!sender.isOnline()) {
            sender.sendLocalizedMessage("mute_result",
                    "%target%", targetName,
                    "%time%", Duration.durationWordsMs(sender.getLanguage(), time),
                    "%reason%", reason
            );
        }

        ServerManager.IMP.broadcastStaff(
                "command.staffchat",
                new LanguageContext(
                        "mute_broadcast",
                        language -> new ObjectLinkedBuf(
                                "%name%", sender.getDisplayName(),
                                "%target%", finalTarget.getDisplayName(),
                                "%reason%", muteMeta.getReason(language),
                                "%time%", Duration.durationWordsMs(language, time)
                        )
                )
        );

        if (sender instanceof Gamer gamer) StaffStatsSqlHandler.INSTANCE.updateStaffStats(gamer.getPlayerId(), 0, 1);

        if (target.isOnline()) {
            //отправляем пакет на банжу игрока
            sendBungeeMutePacket(target, muteMeta);

            Language language = target.getLanguage();

            target.sendLocalizedMessage("muted_message_format",
                    "%time_left%", muteMeta.getExpirationString(language),
                    "%meta_id%", muteMeta.getId(),
                    "%reason%", muteMeta.getReason(language));

            target.sendLocalizedMessage("muted_read_rules");
        }

        return muteMeta;
    }

    /**
     * Отправить пакет на прокси-сервер игрока с мутом
     *
     * @param gamer    - игрок
     * @param muteMeta - информация о муте
     */
    public void sendBungeeMutePacket(@NonNull Gamer gamer, @NonNull MuteMeta muteMeta) {
        gamer.sendToProxy(new SMuteChangeMuteState(muteMeta.getId(), gamer.getPlayerId(), muteMeta.getAdminName(),
                muteMeta.getUntranslatedReason(), muteMeta.getIssueDate(), muteMeta.getExpirationDate()));
    }

    /**
     * Заблокировать игрока
     *
     * @param initiator - ник администратора
     * @param target    - забаненный игрок или ip
     * @param reason    - причина бана
     * @param time      - время бана
     */
    public BanMeta processBan(@NonNull String initiator, @NonNull String target, @NonNull String reason, long time) throws PunishmentException {
        return processBan(getCommandSender(initiator), target, reason, time);
    }

    /**
     * Заблокировать игрока
     *
     * @param initiator  - ник администратора
     * @param target     - забаненный игрок или ip
     * @param reason     - причина бана
     * @param timeString - время бана
     */
    public BanMeta processBan(@NonNull String initiator, @NonNull String target, @NonNull String reason, String timeString) throws PunishmentException {
        return processBan(getCommandSender(initiator), target, reason, timeString);
    }

    /**
     * Заблокировать игрока
     *
     * @param sender     - администратор
     * @param target     - забаненный игрок или ip
     * @param reason     - причина бана
     * @param timeString - строка времени бана
     */
    public BanMeta processBan(@NonNull CommandSender sender, @NonNull String target, @NonNull String reason, String timeString) throws PunishmentException {
        long time = PunishmentMeta.PERMANENT_TIME;

        if (timeString != null) {
            try {
                time = Patterns.parseTime(timeString, TimeUnit.MILLISECONDS);
            } catch (IllegalArgumentException e) {
                throw new PunishmentException("ban_wrong_time");
            }
        }

        return processBan(sender, target, reason, time);
    }

    /**
     * Заблокировать игрока
     *
     * @param sender - администратор
     * @param target - забаненный игрок или ip
     * @param reason - причина бана
     * @param time   - время бана
     */
    public BanMeta processBan(@NonNull CommandSender sender, @NonNull String target, @NonNull String reason, long time) throws PunishmentException {
        Gamer nickedGamer = Gamer.getNickedGamer(target);
        String originalTargetName = nickedGamer == null ? target : nickedGamer.getName();

        boolean isIp = Patterns.isIPV4(originalTargetName);

        if (sender instanceof Gamer && !sender.getRanks().hasPermission("ban.unlimited.time")) {
            if (time > TimeUnit.DAYS.toMillis(14)) {
                throw new PunishmentException("ban_too_long_time");
            } else if (time == PunishmentMeta.PERMANENT_TIME) {
                throw new PunishmentException("ban_permanent_unavailable");
            } else if (isIp) {
                throw new PunishmentException("ban_ip_unavailable");
            }
        }

        if (!isIp && !Patterns.isValidNick(target)) {
            throw new PunishmentException("ban_wrong_name");
        }

        List<BanMeta> bans;

        if (isIp) {
            bans = getActiveAddressBans(target);
        } else {
            bans = getActiveNameBans(target);
        }

        if (!bans.isEmpty()) {
            throw new PunishmentException("ban_already_given");
        }

        long currentTime = System.currentTimeMillis();
        long expireTime = time;

        if (time != PunishmentMeta.PERMANENT_TIME) {
            expireTime += currentTime;
        }

        Set<Gamer> toKick = new HashSet<>();

        List<String> nicks = new ArrayList<>();
        List<String> ips = new ArrayList<>();

        val initiatorStaffGroup = sender.getRanks().findPriorityRank();

        //если это бан по ip
        if (isIp) {
            try {
                InetAddress address = InetAddress.getByName(target);

                //берем всех игроков с этого ip, и добавляем в список на бан
                for (Gamer gamer : Gamer.getGamersByIp(address)) {
                    nicks.add(gamer.getName());
                }

                //и естественно добавляем этот ip в список на бан
                ips.add(target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else { //это бан по нику
            nicks.add(target);
        }

        // Отправляем ивенты о бане
        for (String banNick : nicks) {
            Gamer gamer = Gamer.getOfflineGamer(banNick, Gamer::loadPlayerId);
            CoreApplication.getInstance().getPluginManager().callEvent(new PlayerBanEvent(gamer, time, time == PunishmentMeta.PERMANENT_TIME));
        }

        //а теперь ЛЕВЕЛ ХАРД, проверяем ранг всех игроков из списка и сравниваем
        //с рангом игрока. Если он >= рангу игрока, удаляем из списка
        //ну делаем это только если инициатор бана - игрок
        if (!sender.isDeveloper()) {
            //для начала пройдемся по IP адресам, здесь мы можем прочекать только
            //ip онлайн игроков, к сожалению
            ips.removeIf(ip -> {
                try {
                    for (Gamer gamer : Gamer.getGamersByIp(InetAddress.getByName(ip))) {
                        val targetStaffGroup = gamer.getRanks().findPriorityRank();

                        if (targetStaffGroup != null && targetStaffGroup.rank().getPriority() > initiatorStaffGroup.rank().getPriority()) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return false;
            });

            //а теперь проверяем все ники, вот тут задача сложнее - надо еще оффлайн игроков чекать
            nicks.removeIf(playerName -> {
                Gamer gamer = Gamer.getOfflineGamer(playerName, offline -> {
                    offline.loadPlayerId();
                    offline.loadRanks();
                    offline.loadPrefix();
                });

                if (gamer == null) {
                    return false;
                }

                val targetStaffGroup = gamer.getRanks().findPriorityRank();

                return targetStaffGroup != null && !initiatorStaffGroup.rank().hasPermission("ban.group." + targetStaffGroup.rank().getName().toLowerCase());
            });
        }

        //проверяем размер списка игроков на бан, если он пустой - шлем нахуй
        if (nicks.size() + ips.size() == 0) {
            throw new PunishmentException("ban_empty_operation_list_empty");
        }

        //добавляем в БД новую банмету и получаем ее
        BanMeta banMeta = addBanMeta(sender.getName(), currentTime, expireTime, reason);

        //добавляем баны по нику
        for (String nick : nicks) {
            Gamer gamer = Gamer.getNickedGamer(nick);

            if (gamer != null) {
                toKick.add(gamer);
            }

            addNameBan(banMeta, nick);
        }

        recentlyRemovedBans.cleanUp();

        List<BanMeta> recentlyRemoved = this.recentlyRemovedBans.getIfPresent(target);

        // если игрока перебанили (т.е разбанили и тут же забанили), то бан снятый
        // можно удалить из истории совсем, он ведь ложный
        if (recentlyRemoved != null) {
            int autoDeleted = 0;

            for (BanMeta removed : recentlyRemoved) {
                if (StringUtil.hasEqualStart(removed.getUntranslatedReason(),
                        banMeta.getUntranslatedReason(), 4)) {

                    removeBanMeta(removed);

                    autoDeleted++;
                }
            }

            if (autoDeleted > 0) {
                ServerManager.IMP.broadcastStaff(
                        "command.staffchat",
                        "ban_auto_remove",
                        "%count%", autoDeleted
                );
            }
        }

        //добавляем баны по ip
        for (String ip : ips) {
            addIpBan(banMeta, ip);
        }

        //количество игроков на кик > 0, кикаем их
        if (!toKick.isEmpty()) {
            Set<AbstractServer> ignored = new HashSet<>();

            for (Gamer gamer : toKick) {
                Language language = gamer.getLanguage();

                //нужен, чтобы после бана редиректить на 2b2t
                PlayerBanDisconnectEvent event = new PlayerBanDisconnectEvent(gamer, banMeta);
                CoreApplication.getInstance().getPluginManager().callEvent(event);

                boolean disconnect = event.isDisconnect();

                String disconnectReason = getPlayerDisconnectReason(language, banMeta, time);

                if (disconnect) {
                    gamer.kick(disconnectReason);
                } else {
                    gamer.sendMessage(disconnectReason);
                }

                AbstractServer server = gamer.getServer();

                if (server == null || server.isSpecial()) {
                    continue;
                }

                if (!ignored.add(server)) {
                    continue;
                }

                boolean announceBan = false;

                if (sender instanceof Gamer senderGamer) {

                    FakePlayerNickname nickname = senderGamer.getFakeNickname();

                    announceBan = !senderGamer.isVanished()
                            && (nickname == null || nickname.getGroup().hasPermission("core.staff"));
                }

                if (announceBan) {
                    for (Gamer another : server.getGamers()) {
                        if (another == gamer) {
                            continue;
                        }

                        another.sendLocalizedMessage("player_removed_from_server");
                    }
                }
            }
        }

        //для сообщения инициатору бана уберем указанный по дефолту ip или ник
        if (isIp) {
            ips.remove(target);
        } else {
            nicks.remove(target);
        }

        StatsWipeManager.INSTANCE.handleUserBanned(banMeta);

        for (int playerId : banMeta.getBannedIds()) {
            PropertySqlHandler.INSTANCE.setPropertyValue(playerId, "Banned", 1);
        }

        if (!sender.isOnline()) {
            sender.sendLocalizedMessage("ban_result",
                    "%target%", target,
                    "%reason%", reason,
                    "%time%", Duration.durationWordsMs(sender.getLanguage(), time)
            );
        }

        ServerManager.IMP.broadcastStaff(
                "command.staffchat",
                new LanguageContext(
                        language -> {
                            String banTimeString = Duration.durationWordsMs(language, time);

                            //дальше строим сообщение которое надо отправить инициатору бана
                            String message = language.getMessage(
                                    isIp ? "ban_ip_broadcast" : "ban_name_broadcast",
                                    "%admin%", sender.getDisplayName(),
                                    "%target%", target
                            );

                            String languageEnumeration = language.getMessage("ban_enumeration_delimiter");

                            if (!ips.isEmpty()) {
                                message += language.getMessage("ban_with_ip", "%ips%", Joiner.on(languageEnumeration).join(ips.stream().map(IpAddressUtil::hideLastPart).toList()));
                            }

                            if (!nicks.isEmpty()) {
                                if (nicks.size() == 1) {
                                    message += language.getMessage("ban_with_name", "%nick%", nicks.get(0));
                                } else {
                                    message += language.getMessage("ban_with_names", "%nicks%", Joiner.on(languageEnumeration).join(nicks));
                                }
                            }

                            if (!banMeta.isPermanent()) {
                                message += language.getMessage("ban_with_time", "%time%", banTimeString);
                            }

                            message += language.getMessage("ban_with_reason", "%reason%", banMeta.getReason(language));

                            return message;
                        }
                )
        );

        if (sender instanceof Gamer gamer) StaffStatsSqlHandler.INSTANCE.updateStaffStats(gamer.getPlayerId(), 0, 1);

        return banMeta;
    }

    /**
     * Кикнуть игрока
     *
     * @param initiator - ник администратора
     * @param name      - игрок или ip
     * @param reason    - причина
     */
    public void processKick(@NonNull String initiator, @NonNull String name, @NonNull String reason) throws PunishmentException {
        processKick(getCommandSender(initiator), name, reason);
    }

    /**
     * Кикнуть игрока
     *
     * @param sender - администратор
     * @param name   - игрок или ip
     * @param reason - причина
     */
    public void processKick(@NonNull CommandSender sender, @NonNull String name, @NonNull String reason) throws PunishmentException {
        boolean isIp = Patterns.isIPV4(name);

        val senderStaffRank = sender.getRanks().findPriorityRank();

        List<Gamer> toKick = new ArrayList<>();

        if (isIp) {
            try {
                InetAddress address = InetAddress.getByName(name);

                toKick.addAll(Gamer.getGamersByIp(address));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Gamer target = Gamer.getNickedGamer(name);

            if (target != null) {
                toKick.add(target);
            }
        }

        if (!sender.isDeveloper()) {
            toKick.removeIf(target -> {
                val targetStaffRank = target.getRanks().findPriorityRank();

                return targetStaffRank != null && targetStaffRank.rank().getPriority() > senderStaffRank.rank().getPriority();
            });
        }

        if (toKick.isEmpty()) {
            throw new PunishmentException("kick_empty");
        }

        for (Gamer target : toKick) {
            target.localizedKick("kick_disconnect_reason",
                    "%initiator%", sender.getDisplayName(),
                    "%reason%", reason);
        }

        if (!sender.isOnline()) {
            sender.sendLocalizedMessage("kick_result",
                    "%target%", name,
                    "%reason%", reason
            );
        }

        String key = toKick.size() < 2 ? "kick_single" : "kick_many";

        ServerManager.IMP.broadcastStaff(
                "command.staffchat",
                new LanguageContext(
                        language -> {
                            String languageEnumeration = language.getMessage("ban_enumeration_delimiter");

                            String targetNames = toKick.stream().map(Gamer::getDisplayName)
                                    .collect(Collectors.joining(languageEnumeration));

                            return language.getMessage(key,
                                    "%name%", sender.getDisplayName(), "%target%", targetNames, "%reason%", reason);
                        }
                )
        );

        if (sender instanceof Gamer gamer) StaffStatsSqlHandler.INSTANCE.updateStaffStats(gamer.getPlayerId(), 0, 1);
    }

    public CommandSender getCommandSender(@NonNull String senderName) {
        CommandSender consoleSender = ConsoleCommandSender.INSTANCE;

        CommandSender sender = senderName.equalsIgnoreCase(consoleSender.getName()) ? consoleSender : null;

        if (sender == null) {
            sender = Gamer.getOfflineGamer(senderName, offline -> {
                offline.loadPlayerId();
                offline.loadRanks();
                offline.loadPrefix();
            });
        }

        return sender;
    }

    public String getBanJsonText(@NonNull BanMeta ban, @NonNull Language language, boolean showAdmin, boolean full) {
        return String.format(JSON.HOVER_TEXT_LAYOUT, getBanText(ban, language), getBanHoverText(ban, language, showAdmin, full));
    }

    public String getBanHastebinText(@NonNull BanMeta ban, @NonNull Language language, boolean showAdmin, boolean full) {
        return getBanText(ban, language) + "\n" +
                getBanHoverText(ban, language, showAdmin, full);
    }

    public String getBanText(@NonNull BanMeta ban, @NonNull Language language) {
        return language.getMessage("punishment_row", "%id%", ban.getId(), "%reason%", ban.getReason(language));
    }

    public String getBanHoverText(@NonNull BanMeta ban, @NonNull Language language, boolean showAdmin, boolean full) {
        String languageEnumeration = language.getMessage("ban_enumeration_delimiter");

        String hover = language.getMessage("ban_hover_time", "%admin%", ban.getAdminName(), "%time%", Duration.ruWordsMs(System.currentTimeMillis() - ban.getIssueDate()));

        if (showAdmin) {
            hover += "\n" + language.getMessage("ban_hover_admin", "%admin%", ban.getAdminName());
        }

        List<String> banNames = ban.getNames();

        if (!banNames.isEmpty()) {
            hover += "\n" + language.getMessage("ban_hover_names",
                    "%names%", Joiner.on(languageEnumeration).join(banNames));
        }

        if (full && !ban.getAddresses().isEmpty()) {
            hover += "\n" + language.getMessage("ban_hover_ips",
                    "%ips%", Joiner.on(languageEnumeration).join(ban.getAddresses()));
        }

        if (ban.isExpired()) {
            hover += "\n" + language.getMessage("ban_hover_expired",
                    "%time%", Duration.durationWordsMs(language, System.currentTimeMillis() - ban.getExpirationDate()));

            if (ban.getRemovedPunishmentName() != null) {
                hover += "\n" + language.getMessage("ban_hover_unbanned_by",
                        "%admin%", ban.getRemovedPunishmentName());
            }
        } else {
            if (ban.isPermanent()) {
                hover += "\n" + language.getMessage("ban_hover_never_expired");
            } else {
                hover += "\n" + language.getMessage("ban_hover_expiring",
                        "%time%", ban.getExpirationString(language));
            }
        }

        return hover;
    }

    public String getMuteJsonText(@NonNull MuteMeta mute, @NonNull Language language, boolean full) {
        return String.format(JSON.HOVER_TEXT_LAYOUT, getMuteText(mute, language), getMuteHoverText(mute, language, full));
    }

    public String getMuteHastebinText(@NonNull MuteMeta mute, @NonNull Language language, boolean full) {
        return getMuteText(mute, language) + "\n" +
                getMuteHoverText(mute, language, full);
    }

    private String getMuteText(@NonNull MuteMeta mute, @NonNull Language language) {
        return language.getMessage("punishment_row", "%id%", mute.getId(), "%reason%", mute.getReason(language));
    }

    public String getMuteHoverText(@NonNull MuteMeta mute, @NonNull Language language, boolean full) {
        Gamer mutedGamer = Gamer.getOfflineGamer(mute.getPlayerId(), offline -> {
            offline.loadPlayerId();
            offline.loadRanks();
            offline.loadPrefix();
        });

        String hover = language.getMessage("mute_hover_time", "%time%", Duration.ruWordsMs(System.currentTimeMillis() - mute.getIssueDate()));

        if (full) {
            hover += "\n" + language.getMessage("mute_hover_admin", "%admin%", mute.getAdminName());
        }

        if (mutedGamer != null) {
            hover += "\n" + language.getMessage("mute_hover_muted", "%name%", mutedGamer.getName());
        }

        if (mute.isExpired()) {
            hover += "\n" + language.getMessage("mute_hover_expired");

            if (mute.getRemovedPunishmentName() != null) {
                hover += "\n" + language.getMessage("mute_hover_unmuted_by", "%unmutedBy%", mute.getRemovedPunishmentName());
            }
        } else {
            hover += "\n" + language.getMessage("mute_hover_expiring", "%time%", mute.getExpirationString(language));
        }

        return hover;
    }


    public BanMeta getActiveBan(@NonNull String name, @NonNull InetAddress ip) {
        List<BanMeta> nameBans = getActiveNameBans(name);
        List<BanMeta> ipBans = getActiveAddressBans(ip.getHostAddress());

        List<BanMeta> allBans = new ArrayList<>(nameBans.size() + ipBans.size());
        allBans.addAll(ipBans);
        allBans.addAll(nameBans);

        if (allBans.isEmpty()) { //Gamer has no punishments
            return null;
        }

        if (allBans.size() > 1) { //берем бан которому до истечения осталось больше всего
            allBans.sort((ban1, ban2) -> (int) (ban1.getIssueDate() - ban2.getIssueDate()));
        }

        return allBans.get(0);
    }

    public String getPlayerDisconnectReason(@NonNull Language language, @NonNull BanMeta ban) {
        return getPlayerDisconnectReason(language, ban, ban.getExpirationDate() - System.currentTimeMillis());
    }

    public String getPlayerDisconnectReason(@NonNull Language language, @NonNull BanMeta ban, long time) {
        String expirationString;

        if (time < 0) {
            expirationString = language.getMessage("ban_disconnect_permanent");
        } else {
            expirationString = language.getMessage("ban_disconnect_temporary_expiration",
                    "%expires%", ban.getExpirationString(language));
        }

        return language.getMessage("ban_disconnect_reason",
                "%id%", ban.getId(),
                "%reason%", ban.getReason(language),
                "%expires%", expirationString
        );
    }

    public CompletableFuture<Integer> amnesty(Instant since) {
        long sinceMs = since.toEpochMilli();

        if (!amnestyInProgress.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Amnesty already in progress"));
        }

        return CompletableFuture.supplyAsync(() -> {
                    AtomicInteger count = new AtomicInteger();
                    List<BanMeta> metas = getBanMetas(sinceMs);
                    metas.forEach(meta -> {
                        if (meta.getRemovedPunishmentName() != null) return;

                        meta.getNames().forEach(name -> {
                            try {
                                processUnban(ConsoleCommandSender.INSTANCE, name);
                                count.getAndIncrement();
                            } catch (PunishmentException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        log.info("Proceed ban meta {}", meta.getNames());
                    });
                    return count.get();
                })
                .whenComplete((res, ex) -> amnestyInProgress.set(false));
    }

    public int getBansCount() {
        return this.banMetas.size();
    }

    public int getMuteCount() {
        return this.muteMetas.size();
    }
}
