package club.abelix.core.app.data.punishment.command.ban;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.util.List;
import java.util.stream.Collectors;

@NamedCommand("banbyinfo")
public final class BanByInfoCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("command.banbyinfo")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        if (session.getArgumentCount() == 0)
            throw new CommandTranslationException("banbyinfo_unknown_usage");

        String banBy = session.getArgument(0);

        List<BanMeta> bans = manager.getBanMetas().stream()
                .filter(banMeta -> banMeta.getAdminName().equalsIgnoreCase(banBy))
                .collect(Collectors.toList());

        if (bans.isEmpty()) {
            throw new CommandTranslationException("banbyinfo_empty_result", "%ban_by%", banBy);
        }

        boolean full = session.getGamer().hasPermission("command.banbyinfo");

        session.sendLocalizedMessage("banbyinfo_result", "%ban_by%", banBy);

        for (BanMeta ban : bans) {
            session.sendMessage(manager.getBanJsonText(ban, session.getLanguage(), true, full));
        }
    }
}
