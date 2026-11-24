package club.abelix.core.telegram.command.sender;

import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.app.api.command.CommandSender;
import club.abelix.core.telegram.TelegramBotPlugin;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.val;
import org.jetbrains.annotations.NotNull;

public class TelegramConsoleCommandSender implements CommandSender {

    private final String name;
    private final long chatId;

    public static @NotNull CommandSender create(final long chatId) {
        return new TelegramConsoleCommandSender("Core", chatId);
    }

    private TelegramConsoleCommandSender(String name, long chatId) {
        this.name = name;
        this.chatId = chatId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Language getLanguage() {
        return Language.defaultLanguage();
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public void sendMessage(String message) {
        if (TelegramBotPlugin.getInstance().isPaused()) {
            return;
        }

        val bot = TelegramBotPlugin.getInstance().getBot();
        val sendMessage = new SendMessage(chatId, ChatColor.stripColor(message));

        bot.execute(sendMessage);
    }

    @Override
    public void sendRawMessage(String message) {
        sendMessage(message);
    }

    @Override
    public UserRanks getRanks() {
        return UserRanks.CONSOLE;
    }

    @Override
    public boolean isConsole() {
        return true;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return true;
    }

    @Override
    public boolean isHelper() {
        return true;
    }

    @Override
    public boolean isDeveloper() {
        return true;
    }

    @Override
    public boolean isDonater() {
        return true;
    }
}
