package club.abelix.commons.gamer;

import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public interface ICommandSender {

    String getName();

    void sendMessage(String message);

    void sendRawMessage(String message);

    String getDisplayName();

    default void sendLocalizedMessage(@NonNull String key, Object... objects) {
        sendMessage(getLanguage().getMessage(key, objects));
    }

    void dispatchCommand(String command);

    Language getLanguage();

    boolean isConsole();

    boolean isOnline();

    boolean hasPermission(@NotNull String permission);

}
