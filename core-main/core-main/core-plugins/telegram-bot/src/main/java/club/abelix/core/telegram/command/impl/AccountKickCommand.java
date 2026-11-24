package club.abelix.core.telegram.command.impl;

import club.abelix.core.app.data.gamer.Gamer;
import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;

import java.util.List;

public class AccountKickCommand extends BotCommand {

    public AccountKickCommand() {
        super("кик", "кикнуть", "kick");

        setNoobCommand(true);
        setOnlyPrivateMessages(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        List<String> linkedAccounts = botUser.getLinkedAccounts();

        if (linkedAccounts.size() == 1) {
            args = new String[]{linkedAccounts.get(0)};
        }

        val language = botUser.getLanguage();

        if (args.length == 0) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_kick_usage",
                    "%emoji_1%", "⚠", "%emoji_2%", "⭐"));

            return;
        }

        String playerName = args[0].replace("<", "").replace(">", "");

        if (!botUser.hasLinkedAccount(playerName)) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_no_account",
                    "%emoji_1%", "\uD83D\uDEAB", "%name%", playerName));
            return;
        }

        Gamer player = Gamer.getGamer(playerName);

        if (player == null || player.getServer() == null) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_account_offline",
                    "%emoji_1%", "\uD83D\uDEAB"));
            return;
        }

        player.kick(language.getMessage("core_telegram_command_kick_reason"));

        botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_kick_message",
                "%emoji_1%", "✅", "%name%", playerName));
    }
}
