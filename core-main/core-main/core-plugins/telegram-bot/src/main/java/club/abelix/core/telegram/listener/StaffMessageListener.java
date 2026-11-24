package club.abelix.core.telegram.listener;

import club.abelix.commons.language.Language;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.Async;
import club.abelix.core.app.api.event.EventPriority;
import club.abelix.core.app.api.plugin.Listener;
import club.abelix.core.app.event.gamer.StaffMessageEvent;
import club.abelix.core.telegram.ImportantTelegramChats;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.bans.TelegramBanManager;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.command.sender.TelegramConsoleCommandSender;
import club.abelix.core.telegram.event.MessageEvent;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class StaffMessageListener implements Listener {

    private final TelegramBotPlugin plugin;

    @Subscribe(priority = EventPriority.HIGHEST)
    public void onBotStaffMessage(final @NonNull MessageEvent event) {
        val message = event.getMessage();
        val chat = message.chat();

        val from = message.from();

        if (from.isBot()) {
            return;
        }

        var messageText = message.text();

        if (messageText == null || !messageText.startsWith("/") || messageText.startsWith("/start")) {
            return;
        }

        if (BotCommand.isUserAdmin(event.getUserId()) && chat.type() == Chat.Type.Private) {
            plugin.getCore().executeCommand(TelegramConsoleCommandSender.create(chat.id()), message.text());
            return;
        }

        val botUser = BotUserSqlHandler.INSTANCE.getBotUser(event.getUserId());

        if (botUser == BotUser.EMPTY_BOT_USER) {
            return;
        }

        val language = botUser.getLanguage();
        val activePunishment = TelegramBanManager.INSTANCE.getActiveBan(event.getUserId());

        if (activePunishment != null) {
            plugin.replyMessage(message, TelegramBanManager.INSTANCE.getBanMessage(language == null ?
                    Language.defaultLanguage() : language, activePunishment));
            return;
        }

        if (botUser.getLinkedAccounts().isEmpty()) {
            plugin.replyMessage(message, "\uD83D\uDEAB У вас не привязан аккаунт");
            return;
        }

        if (!botUser.hasPrimaryAccount()) {
            plugin.replyMessage(message, "\uD83D\uDEAB Укажите свой основной аккаунт через !основа");
            return;
        }

        val gamer = botUser.getPlayer();

        if (gamer == null) {
            return;
        }

        if (!(gamer.hasPermission("telegram.private.chat") && chat.type() == Chat.Type.Private)
                && !ImportantTelegramChats.canExecuteCommands(message)) {
            return;
        }

        if (gamer.getProperty("Banned") == 1) {
            plugin.replyMessage(message, "\uD83D\uDEAB Ты забанен");
            return;
        }

        messageText = messageText.substring(1).trim();

        val commandArray = messageText.split(" ");
        val commandArgs = Arrays.copyOfRange(commandArray, 1, commandArray.length);

        val cmd = plugin.getProxiedCommand(commandArray[0].toLowerCase());

        if (cmd == null) {
            plugin.replyMessage(message, "\uD83D\uDEAB Команда не найдена или не разрешена в боте");
            return;
        }

        Async.run(() -> cmd.dispatchCommand(message, chat.id(), from.id(), commandArgs, plugin));
    }

    private void handleStaffMessageEvent(final @NonNull StaffMessageEvent event) {
        val formattedMessage = ChatColor.stripColor(event.getContext().format(Language.RUSSIAN));

        if (formattedMessage.contains("Watchdog")) {
            return;
        }

        val staffLogs = ImportantTelegramChats.STAFF_LOGS;

        val sendMessage = new SendMessage(staffLogs.getChatId(), formattedMessage)
                .messageThreadId(staffLogs.getThreadId());

        plugin.getBot().execute(sendMessage);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onServerStaffMessage(final @NonNull StaffMessageEvent event) {
        handleStaffMessageEvent(event);
    }

}
