package club.abelix.api.base.command;

import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ProxiedConsoleCommandSender extends ProxiedCommandSender {

    @Getter
    private final CommandSender commandSender;

    @Override
    public String getDisplayName() {
        return commandSender.getName();
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
    public boolean hasPermission(@NotNull String s) {
        return commandSender.hasPermission(s);
    }
}
