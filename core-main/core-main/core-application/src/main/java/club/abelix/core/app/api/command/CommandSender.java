package club.abelix.core.app.api.command;

import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.core.app.CoreApplication;
import org.jetbrains.annotations.NotNull;

public interface CommandSender extends ICommandSender {

    @Override
    default void dispatchCommand(@NotNull String command) {
        CoreApplication.getInstance().executeCommand(this, command);
    }

    default void sendCommandUsageMessage(@NotNull String usageKey) {
        sendLocalizedMessage("command_usage", "%usage%", getLanguage().getMessage(usageKey));
    }

    UserRanks getRanks();

    boolean isAdmin();

    boolean isHelper();

    boolean isDeveloper();

    boolean isDonater();

}
