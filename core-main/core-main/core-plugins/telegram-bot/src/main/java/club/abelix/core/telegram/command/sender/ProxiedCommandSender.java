package club.abelix.core.telegram.command.sender;

import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.language.Language;
import club.abelix.core.app.api.command.CommandSender;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public abstract class ProxiedCommandSender implements CommandSender {

    CommandSender parent;

    @Override
    public UserRanks getRanks() {
        return parent.getRanks();
    }

    @Override
    public boolean isAdmin() {
        return parent.isAdmin();
    }

    @Override
    public boolean isDeveloper() {
        return parent.isDeveloper();
    }

    @Override
    public boolean isDonater() {
        return parent.isDonater();
    }

    @Override
    public boolean isHelper() {
        return parent.isHelper();
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return parent.hasPermission(permission);
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public String getDisplayName() {
        return parent.getDisplayName();
    }

    @Override
    public Language getLanguage() {
        return parent.getLanguage();
    }

    @Override
    public boolean isConsole() {
        return parent.isConsole();
    }

    @Override
    public boolean isOnline() {
        return parent.isOnline();
    }

    @Override
    public void sendMessage(final String message) {
        parent.sendMessage(message);
    }

    @Override
    public void sendRawMessage(final String message) {
        parent.sendRawMessage(message);
    }
}
