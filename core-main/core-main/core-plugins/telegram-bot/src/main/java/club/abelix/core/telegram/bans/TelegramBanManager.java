package club.abelix.core.telegram.bans;

import club.abelix.commons.data.punishment.meta.PunishmentMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.bans.meta.TelegramBanMeta;
import club.abelix.core.telegram.bans.sql.TelegramBansSqlHandler;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import lombok.val;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class TelegramBanManager {

    // ----------------------------------------------------------------------

    public static final TelegramBanManager INSTANCE = new TelegramBanManager();

    // ----------------------------------------------------------------------

    /**
     * Данные обо всех банах
     */
    private List<TelegramBanMeta> banMetas;

    /**
     * Загрузка всех наказаний
     */
    public CompletableFuture<?> loadPunishments() {
        return TelegramBansSqlHandler.INSTANCE.loadBanList().whenComplete((result, cause) -> {
            if (cause != null) {
                Logging.IMP.stacktrace(cause, "Не удалось загрузить Telegram-баны");

                return;
            }

            Logging.IMP.info("Loaded {} Telegram bot punishments", result.size());

            banMetas = result;
        });
    }

    public TelegramBanMeta getActiveBan(long telegramId) {
        val bans = getActiveBans(telegramId);

        if (bans.isEmpty()) { //has no punishments
            return null;
        }

        if (bans.size() > 1) {
            bans.sort((ban1, ban2) -> (int) (ban1.getIssueDate() - ban2.getIssueDate()));
        }

        return bans.get(0);
    }

    /**
     * Получить активные баны по ID бана
     *
     * @param banId - ID блоировки
     * @return - активные баны
     */
    public List<TelegramBanMeta> getActiveIdBans(int banId) {
        return getCopiedBanMetas().stream()
                .filter(banMeta -> banMeta.getId() == banId && !banMeta.isExpired())
                .collect(Collectors.toList());
    }

    /**
     * Получить активные баны по ID Telegram
     *
     * @param telegram - ID Telegram
     * @return - активные баны
     */
    public List<TelegramBanMeta> getActiveBans(long telegram) {
        return getCopiedBanMetas().stream()
                .filter(banMeta -> banMeta.getTelegramId() == telegram && !banMeta.isExpired())
                .collect(Collectors.toList());
    }

    public void processBan(BotUser botUser, long expire, @NonNull String reason) {
        val telegram = botUser.getTelegramId();
        val time = System.currentTimeMillis();

        var expireTime = expire;

        if (expire != PunishmentMeta.PERMANENT_TIME) {
            expireTime += time;
        }

        val meta = addBanMeta(telegram, time,
                expireTime, reason);

        val plugin = TelegramBotPlugin.getInstance();

        var language = botUser.getLanguage();

        if (language == null) {
            language = Language.defaultLanguage();
        }

        plugin.printMessage(telegram, getBanMessage(language, meta));

        // todo стафф сообщение
    }

    public boolean processUnban(BotUser botUser) {
        val telegram = botUser.getTelegramId();
        val activeBan = getActiveBan(telegram);

        if (activeBan == null) {
            return false;
        }

        removeBanMeta(activeBan);
        return true;
    }

    /**
     * Добавить информацию о бане
     *
     * @param telegram  - айди телеграма
     * @param time      - время выдачи бана
     * @param expire    - срок действия бана
     * @param reason    - причина бана
     * @return - информация о бане в виде меты
     */
    private TelegramBanMeta addBanMeta(long telegram, long time, long expire, @NonNull String reason) {
        val banMeta = TelegramBansSqlHandler.INSTANCE.addBanMeta(telegram, time, expire, reason)
                .join();

        banMetas.add(banMeta);

        return banMeta;
    }

    /**
     * Полность удалить бан из базы
     *
     * @param meta - блокировка
     */
    public void removeBanMeta(@NonNull TelegramBanMeta meta) {
        banMetas.remove(meta);

        TelegramBansSqlHandler.INSTANCE.removeBanMeta(meta.getId());
    }

    public String getBanMessage(@NonNull Language language, @NonNull TelegramBanMeta ban) {
        return getBanMessage(language, ban, ban.getExpirationDate() - System.currentTimeMillis());
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy H:mm");

    public String getBanMessage(@NonNull Language language, @NonNull TelegramBanMeta ban, long time) {
        String timeString;
        String expirationString;

        if (time < 0) {
            timeString = language.getMessage("ban_disconnect_time_permanent");
            expirationString = ChatColor.stripColor(language.getMessage("never"));
        } else {
            timeString = language.getMessage("ban_disconnect_time_temporary");
            expirationString = ban.getExpirationString(language);
        }

        val reasonKey = ban.getUntranslatedReason();
        val reason = language.isMessageTranslated(reasonKey) ? language.getMessage(reasonKey) : reasonKey;

        return language.getMessage("core_telegram_ban_reason",
                "%id%", ban.getId(),
                "%time%", timeString,
                "%expires%", expirationString,
                "%date%", dateFormat.format(new Date(ban.getIssueDate())),
                "%reason%", reason,
                "%emoji_1%", "\uD83D\uDEAB",
                "%emoji_2%", "\uD83D\uDC81\u200D♂\uFE0F",
                "%emoji_3%", "\uD83D\uDCA1"
        );
    }

    /**
     * Получить активные баны
     *
     * @return - активные баны
     */
    public List<TelegramBanMeta> getCopiedBanMetas() {
        return new ArrayList<>(banMetas);
    }
}
