package club.abelix.api.server.base.giftban.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.server.base.giftban.GiftBanService;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@NamedCommand("giftban")
@AllArgsConstructor
public final class GiftBanCommand {

    private final GiftBanService giftBanService;

    @SubcommandExecutor("confirm")
    public void handle(BukkitExecutionSession session) {
        giftBanService.process(
                UUID.fromString(session.getArgument(0)),
                session.getGamer()
        );
    }

    @Executor
    @WithPermission("command.giftban")
    public void execute(final @NotNull BukkitExecutionSession session) throws CommandTranslationException {
        val sender = session.getGamer();
        val target = session.getGamer(0);
        val reason = session.getArgumentString(1);

        giftBanService.create(
                sender,
                target,
                reason
        );

        sender.sendMessage("Gift ban book sent to " + target.getDisplayName() + "!");
    }
}
