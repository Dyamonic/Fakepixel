package club.abelix.core.telegram.command;

import club.abelix.core.telegram.TelegramBotPlugin;
import club.abelix.core.telegram.command.sender.TelegramCommandSender;
import club.abelix.core.telegram.user.BotUser;
import com.pengrad.telegrambot.model.Message;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public final class ProxiedBotCommand extends BotCommand {

    public static @NotNull ProxiedBotCommand create(
            final @NonNull String coreCommandName,
            final @NonNull String @NotNull ... aliases
    ) {
        val command = new ProxiedBotCommand(coreCommandName, aliases);
        command.setShouldLinkAccount(true);

        return command;
    }

    String commandName;

    private ProxiedBotCommand(final String coreCommandName, final String... aliases) {
        super(aliases);

        this.commandName = coreCommandName;
    }

    @Override
    protected void execute(BotUser botUser,
                           @Nullable Message message,
                           long chatId,
                           @NotNull @NonNull String[] args,
                           @NonNull TelegramBotPlugin botPlugin) {
        if (message == null) {
            return;
        }

        val player = botUser.getPlayer();

        if (player == null) {
            return;
        }

        val sender = TelegramCommandSender.create(player, message);

        val command = botPlugin.getCore()
                .getCommand(commandName);

        if (command != null) {
            command.execute(player, sender, args);
        }
    }
}
