package club.abelix.core.app.api.command;

import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.language.Language;
import club.abelix.network.common.logging.Logging;
import org.jetbrains.annotations.NotNull;

public class ConsoleCommandSender implements CommandSender {

    public static final ConsoleCommandSender INSTANCE = new ConsoleCommandSender("Console");

    private final String name;

    private ConsoleCommandSender(String name) {
        this.name = name;
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
        Logging.IMP.info(message);
    }

    @Override
    public void sendRawMessage(String message) {
        Logging.IMP.info(message);
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

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return true;
    }
}
