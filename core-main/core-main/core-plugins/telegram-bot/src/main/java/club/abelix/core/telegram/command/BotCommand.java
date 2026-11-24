package club.abelix.core.telegram.command;

import club.abelix.commons.language.Language;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.bans.TelegramBanManager;
import club.abelix.core.telegram.event.BotCommandEvent;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;
import club.abelix.network.common.logging.Logging;
import com.google.common.base.Preconditions;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Getter
public abstract class BotCommand {

    protected static final Language LANG = Language.defaultLanguage();

    private static final TLongList ADMINISTRATOR_IDS = new TLongArrayList() {{
        add(875732183L);    // @KaiJQ
        add(330911097L);    // @EgorSkorikov
    }};

    protected static final TelegramBanManager BAN_MANAGER = TelegramBanManager.INSTANCE;

    private final String[] aliases;

    public BotCommand(String... aliases) {
        Preconditions.checkArgument(aliases.length > 0,
                "You must specify command name as a first argument in constructor");

        this.aliases = aliases;
    }

    /**
     * Будет ли команда доступна только тем, у кого
     * установлен основной аккаунт в боте
     */
    @Setter
    private boolean shouldLinkAccount = false;

    /**
     * Доступна ли команда для использования только в ЛС
     * основной группы
     */
    @Setter
    private boolean onlyPrivateMessages = false;

    /**
     * Команда доступна только в беседах
     */
    @Setter
    private boolean onlyChats = false;

    /**
     * Команда для нубов или нет? Команды для нубов будут
     * выполняться также с написанием без "!"
     */
    @Setter
    private boolean noobCommand = false;

    /**
     * Доступна ли команда всем игрокам или только особому
     * кругу людей в боте
     */
    @Setter
    private boolean onlyAdmins = false;

    /**
     * Минимальная группа с которой будет доступна команда
     */
    @Setter
    private String permission;

    public void dispatchCommand(@Nullable Message originalMessage, long chatId, long userId,
                                @NonNull String[] args, @NonNull TelegramBotPlugin botPlugin) {
        val botUser = BotUserSqlHandler.INSTANCE.getBotUser(userId);
        val language = botUser.getLanguage();

        val activePunishment = BAN_MANAGER.getActiveBan(userId);

        if (activePunishment != null) {
            botPlugin.printMessage(chatId, BAN_MANAGER.getBanMessage(language == null ?
                    Language.defaultLanguage() : language, activePunishment));
            return;
        }

        if (!aliases[0].equals("setuserlanguage") && language == null) {
            val message = "⭐ Выберите язык / Choose a language";

            val russian = new InlineKeyboardButton("\uD83C\uDDF7\uD83C\uDDFA Русский")
                    .callbackData("!setuserlanguage ru auto");

            val english = new InlineKeyboardButton("\uD83C\uDDEC\uD83C\uDDE7 English")
                    .callbackData("!setuserlanguage en auto");

            val keyboard = new InlineKeyboardMarkup(russian, english);

            botPlugin.printMessage(new SendMessage(chatId, message).replyMarkup(keyboard));
            return;
        }

        if (!botUser.hasPrimaryAccount() && shouldLinkAccount) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_condition_should_link_account",
                    "%emoji_1%", "\uD83D\uDEAB", "%emoji_2%", "\uD83D\uDCDA"));
            return;
        }

        if (!isUserAdmin(botUser.getTelegramId()) && onlyAdmins) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_condition_not_admin",
                    "%emoji_1%", "\uD83D\uDEAB"));
            return;
        }

        if (permission != null) {
            val player = botUser.getPlayer(Gamer::loadAuthData);

            if (player != null && !player.getRanks().hasPermission(permission)) {
                botPlugin.printMessage(chatId, language.getMessage("core_telegram_condition_not_admin",
                        "%emoji_1%", "\uD83D\uDEAB"));
                return;
            }
        }

        if (!Objects.equals(chatId, userId) && onlyPrivateMessages) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_condition_only_private_messages",
                    "%emoji_1%", "\uD83D\uDEAB"));
            return;
        }

        if (Objects.equals(chatId, userId) && onlyChats) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_condition_only_chats",
                    "%emoji_1%", "\uD83D\uDEAB"));
            return;
        }

        val event = new BotCommandEvent(botUser, this);
        botPlugin.getCore().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        try {
            execute(botUser, originalMessage, chatId, args, botPlugin);
        } catch (Throwable t) {
            Logging.IMP.stacktrace(t, "Error occurred whilst executing telegram command");

            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_exception",
                    "%emoji_1%", "\uD83D\uDEAB"));
        }
    }

    public static boolean isUserAdmin(long userId) {
        return ADMINISTRATOR_IDS.contains(userId);
    }

    protected abstract void execute(final BotUser botUser, final @Nullable Message message,
                                    final long chatId, @NonNull String[] args, @NonNull TelegramBotPlugin botPlugin);

    protected static String trimForNoobs(@NonNull String commandLine) {
        return commandLine.replace("<", "").replace(">", "");
    }

}
