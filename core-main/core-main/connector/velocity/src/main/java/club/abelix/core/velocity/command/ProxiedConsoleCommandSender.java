package club.abelix.core.velocity.command;

import club.abelix.commons.language.Language;
import com.velocitypowered.api.command.CommandSource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ProxiedConsoleCommandSender extends ProxiedCommandSender {

    @Getter
    private final CommandSource commandSender;

    @Override
    public void sendRawMessage(String message) {
        commandSender.sendMessage(Component.text(message));
    }

    @Override
    public String getDisplayName() {
        return "Console";
    }

    @Override
    public Language getLanguage() {
        return Language.defaultLanguage();
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
    public boolean hasPermission(@NotNull String permission) {
        return commandSender.hasPermission(permission);
    }
}
