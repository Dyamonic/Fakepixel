package club.abelix.core.telegram.command.impl;

import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.BotCommand;
import club.abelix.core.telegram.user.BotUser;
import club.abelix.network.packet.play.server.STelegramAuth;
import com.pengrad.telegrambot.model.Message;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static club.abelix.core.telegram.listener.PacketListener.CONFIRMATION_CACHE;

public class TwoFactorAuthCommand extends BotCommand {

    private final boolean allowLogin;

    public TwoFactorAuthCommand(@NonNull String commandName, boolean allowLogin) {
        super(commandName);

        this.allowLogin = allowLogin;

        setNoobCommand(false);
        setOnlyPrivateMessages(true);
    }

    @Override
    protected void execute(BotUser botUser, @Nullable Message message, long chatId, @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        if (args.length == 0) {
            return;
        }

        val playerName = args[0];
        if (!botUser.hasLinkedAccount(playerName)) {
            return;
        }

        CONFIRMATION_CACHE.cleanUp();

        val language = botUser.getLanguage();
        val user = CONFIRMATION_CACHE.getIfPresent(playerName.toLowerCase());

        if (user == null) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_login_request_none",
                    "%emoji_1%", "\uD83D\uDEAB"));
            return;
        }

        CONFIRMATION_CACHE.invalidate(playerName.toLowerCase());

        val gamer = Gamer.getGamer(playerName);

        if (gamer != null) {
            return;
        }

        val connectableName = user.getProxyName();
        val connectable = ServerManager.IMP.getProxy(connectableName);

        if (connectable == null) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_login_request_error",
                    "%emoji_1%", "\uD83D\uDEAB"));
            return;
        }

        connectable.sendPacket(new STelegramAuth(user, allowLogin));

        if (allowLogin) {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_login_request_approved",
                    "%emoji_1%", "✅"));
        } else {
            botPlugin.printMessage(chatId, language.getMessage("core_telegram_login_request_declined",
                    "%emoji_1%", "❌"));
        }

    }
}
