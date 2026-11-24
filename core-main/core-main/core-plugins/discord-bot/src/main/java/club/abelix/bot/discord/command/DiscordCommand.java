package club.abelix.bot.discord.command;

import club.abelix.bot.discord.DiscordBot;
import club.abelix.bot.discord.handler.BotSqlHandler;
import club.abelix.bot.discord.user.DiscordBotUser;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.commons.util.password.PasswordHashing;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NamedCommand("discord")
@WithHelp
@RequiredArgsConstructor
public class DiscordCommand {

    private final DiscordBot bot;

    @SubcommandExecutor("link")
    public void link(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        if (bot.isAwaitingConfirmation(gamer)) {
            throw new CommandTranslationException("discord_already_awaiting");
        }

        DiscordBotUser user = BotSqlHandler.INSTANCE.getByPlayerId(gamer.getPlayerId());

        if (user != null) {
            throw new CommandTranslationException("discord_already_linked");
        }

        String code = PasswordHashing.generateRandomPassword(6);

        session.sendLocalizedMessage("discord_link_action", "%code%", code);

        bot.addAwaitingConfirmation(gamer, code);
    }

    @SubcommandExecutor("unlink")
    public void unlink(CoreExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() == 0) {
            unlinkSelf(session);
            return;
        }

        unlinkByDiscordId(session);
    }

    private void unlinkSelf(CoreExecutionSession session) throws CommandTranslationException {
        Gamer gamer = session.getGamer();

        DiscordBotUser user = BotSqlHandler.INSTANCE.getByPlayerId(gamer.getPlayerId());

        if (user == null) {
            throw new CommandTranslationException("discord_not_linked");
        }

        // отвязываем аккаунт от дискорда
        BotSqlHandler.INSTANCE.unlinkDiscordAccount(user);

        // убираем все роли не нужные
        bot.discordUserSync(user.getDiscordId(), null);

        session.sendLocalizedMessage("discord_unlink_success");
    }

    private void unlinkByDiscordId(CoreExecutionSession session) throws CommandTranslationException {
        if (!session.getSender().hasPermission("command.discord.unlink.other")) {
            throw new CommandTranslationException("no_permission",
                    "%permission%", "command.discord.unlink.other");
        }

        final long discordId;

        try {
            discordId = Long.parseLong(session.getArgument(0));
        } catch (NumberFormatException e) {
            throw new CommandTranslationException("command_int_required",
                    "%arg%", session.getArgument(0));
        }

        DiscordBotUser user = BotSqlHandler.INSTANCE.getByDiscordId(discordId);

        if (user == null) {
            session.sendMessage(ChatColor.RED + "Discord account " + discordId + " is not linked.");
            return;
        }

        Gamer linkedPlayer = user.getPlayer();

        BotSqlHandler.INSTANCE.unlinkDiscordAccount(user);

        bot.discordUserSync(discordId, null);

        if (linkedPlayer.isOnline()) {
            linkedPlayer.sendLocalizedMessage("discord_unlink_success");
        }

        log.info("Staff {} unlinked Discord account {} from {}",
                session.getDisplayName(),
                discordId,
                linkedPlayer.getName());

        session.sendMessage(ChatColor.GREEN + "Discord account " + discordId
                + " has been unlinked from " + linkedPlayer.getName() + ".");
    }

}
