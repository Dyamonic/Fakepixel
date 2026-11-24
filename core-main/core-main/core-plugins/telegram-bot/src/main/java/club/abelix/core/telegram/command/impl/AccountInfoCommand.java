package club.abelix.core.telegram.command.impl;

import club.abelix.commons.util.color.ChatColor;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.app.data.gamer.Gamer;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;

import java.util.ArrayList;

public class AccountInfoCommand extends BotCommand {

    /**
     * Аккаунтов в одной строке в клавиатуре
     */
    private static final int ACCOUNTS_PER_KEYBOARD_LINE = 2;

    public AccountInfoCommand() {
        super("инфо", "информация", "инфа", "аккаунты", "info", "accounts");

        setNoobCommand(true);
        setOnlyPrivateMessages(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        val language = botUser.getLanguage();
        if (botUser.getLinkedAccounts().isEmpty()) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_info_usage",
                    "%emoji_1%", "⚠"));

            return;
        }

        if (args.length > 0) {
            String playerName = trimForNoobs(args[0]);

            if (!botUser.hasLinkedAccount(playerName)) {
                botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_no_account",
                        "%emoji_1%", "\uD83D\uDEAB", "%name%", playerName));
                return;
            }

            val player = Gamer.getGamer(playerName);
            val offline = player == null || player.getServer() == null;

            String response;

            if (offline) {
                response = language.getMessage("core_telegram_command_account_offline",
                        "%emoji_1%", "\uD83D\uDEAB");
            } else {
                response = language.getMessage("core_telegram_command_info_account_info",
                        "%emoji_1%", "\uD83D\uDD25", "%name%", player.getName(),
                        "%server%", player.getServer().getName(), "%address%", player.getAddress().getHostAddress(),
                        "%time%", Duration.ruWordsSec(player.getPlayingSeconds()), "%emoji_2%", "❗");
            }

            val restoreButton = new InlineKeyboardButton(language.getMessage("core_telegram_command_info_recovery_button"));
            restoreButton.callbackData("!восстановить " + playerName);

            val unbindButton = new InlineKeyboardButton(language.getMessage("core_telegram_command_info_unlink_button"));
            unbindButton.callbackData("!отвязать " + playerName);

            val sendMessage = new SendMessage(chatId, response);

            if (!offline) {
                val kickButton = new InlineKeyboardButton(language.getMessage("core_telegram_command_info_kick_button"));
                kickButton.callbackData("!кик " + playerName);

                sendMessage.replyMarkup(new InlineKeyboardMarkup(kickButton, restoreButton, unbindButton));
            } else {
                sendMessage.replyMarkup(new InlineKeyboardMarkup(restoreButton, unbindButton));
            }

            botPlugin.printMessage(sendMessage);

            return;
        }

        val response = new StringBuilder(language.getMessage("core_telegram_command_info_accounts_list",
                "%emoji_1%", "\uD83D\uDCDA"));

        int accounts = 0;

        val buttons = new ArrayList<InlineKeyboardButton>();
        for (val playerName : botUser.getLinkedAccounts()) {
            if (++accounts > ACCOUNTS_PER_KEYBOARD_LINE) {
                accounts = 0;
            }

            Gamer offlinePlayer = Gamer.getOfflineGamer(playerName, offline -> {});

            if (offlinePlayer == null) {
                continue;
            }

            response.append("\n").append("* ").append(offlinePlayer.getName());

            val button = new InlineKeyboardButton("\uD83D\uDC64 " + offlinePlayer.getName());
            button.callbackData("!инфо " + offlinePlayer.getName());

            buttons.add(button);
        }

        if (botUser.hasPrimaryAccount()) {
            val offlinePlayer = Gamer.getOfflineGamer(botUser.getPrimaryAccountName());

            if (offlinePlayer != null) {
                response.append("\n\n").append(language.getMessage("core_telegram_command_info_main_account",
                                "%emoji_1%", "\uD83D\uDC7E")).append(offlinePlayer.getName());
            }
        }

        response.append("\n\n").append(language.getMessage("core_telegram_command_info_additions",
                "%emoji_1%", "⭐"));

        val sendMessage = new SendMessage(chatId, ChatColor.stripColor(response.toString()));

        if (!buttons.isEmpty()) {
            val markup = new InlineKeyboardMarkup();

            for (int i = 0; i < buttons.size(); i += ACCOUNTS_PER_KEYBOARD_LINE) {
                val subList = buttons.subList(i, Math.min(i + ACCOUNTS_PER_KEYBOARD_LINE,
                        buttons.size()));
                markup.addRow(subList.toArray(new InlineKeyboardButton[0]));
            }

            sendMessage.replyMarkup(markup);
        }

        botPlugin.printMessage(sendMessage);
    }

}
