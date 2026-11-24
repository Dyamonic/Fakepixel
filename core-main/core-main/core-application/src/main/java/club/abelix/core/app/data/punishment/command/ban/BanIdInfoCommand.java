package club.abelix.core.app.data.punishment.command.ban;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandException;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

@NamedCommand("banidinfo")
public final class BanIdInfoCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @WithPermission("command.banidinfo")
    @WithCondition("minArgs: 1")
    public void execute(CoreExecutionSession session) throws CommandException {
        int id = session.getIntArgument(0, "Ban Id");

        BanMeta banMeta = manager.findBanMeta(id)
                .orElseThrow(() -> new CommandTranslationException("banidinfo_empty_result"));

        boolean full = session.getGamer().hasPermission("command.baninfo");

        session.sendLocalizedMessage("banidinfo_result", "%id%", id);
        session.sendMessage(manager.getBanJsonText(banMeta, session.getLanguage(), true, full));
    }
}
