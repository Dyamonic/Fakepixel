package club.abelix.core.telegram.command.impl;

import club.abelix.commons.data.suffix.SuffixSqlHandler;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.core.telegram.user.sql.AccountSqlHandler;
import club.abelix.core.telegram.user.sql.BotUserSqlHandler;
import club.abelix.network.packet.play.server.STelegramLinked;
import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AccountLinkCommand extends BotCommand {

    private static final String SUSPICIOUS_ACTIVITY_MESSAGE = "[TG-Bot] Id: %d Пытается привязать больше пяти" +
            " аккаунтов, его аккаунты: %s";

    public AccountLinkCommand() {
        super("привязать", "привязка", "привезать", "привизать", "превезать", "превязать", "аккаунт", "link", "start");

        setNoobCommand(true);
        setOnlyPrivateMessages(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        val language = botUser.getLanguage();

        if (args.length < 1) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_link_usage",
                    "%emoji_1%", "⚠", "%emoji_2%", "⭐"));

            return;
        }

        val token = args[0];
        val playerId = botPlugin.getPlayerIdByToken(token);

        if (playerId == 0) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_link_error"));
            return;
        }

        val gamer = Gamer.getGamer(playerId);

        if (gamer == null || !gamer.isOnline()) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_link_error"));
            return;
        }

        val playerName = gamer.getName();

        if (botUser.hasLinkedAccount(playerName)) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_link_already_linked",
                    "%name%", playerName, "%emoji_1%", "\uD83D\uDEAB"));
            return;
        }

        val playerBotUser = BotUserSqlHandler.INSTANCE.getBotUser(playerName);

        if (playerBotUser.hasLinkedAccount(playerName)) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_link_already_linked_to_other",
                    "%name%", playerName, "%emoji_1%", "\uD83D\uDEAB", "%emoji_2%", "\uD83D\uDE12"));
            return;
        }

        val account = AccountSqlHandler.INSTANCE.getAccount(playerName);

        if (!account.registered()) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_link_not_registered",
                    "%emoji_1%", "\uD83D\uDEAB", "%name%", playerName));
            return;
        }

        if (botUser.getLinkedAccounts().size() >= 5) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_link_too_many_accounts",
                    "%emoji_1%", "\uD83D\uDEAB", "%emoji_2%", "\uD83D\uDD25"));
            return;
        }

        BotUserSqlHandler.INSTANCE.linkBotAccount(botUser, playerName);

        botPlugin.printMessage(chatId, language.getMessage("core_telegram_command_link_success",
                "%name%", playerName, "%emoji_1%", "✅",
                "%emoji_2%", "\uD83D\uDD25", "%emoji_3%", "\uD83D\uDC6E\u200D♂"));

        val telegramId = botUser.getTelegramId();

        gamer.setTelegramId(telegramId);

        val server = gamer.getServer();

        if (server != null) {
            server.sendPacket(new STelegramLinked(gamer.getPlayerId(), telegramId));
        }

        botPlugin.removeToken(token);

        val suffix = SuffixSqlHandler.INSTANCE.getSuffixById(115);

        SuffixSqlHandler.INSTANCE.addPlayerSuffix(playerId, suffix);
    }
}
