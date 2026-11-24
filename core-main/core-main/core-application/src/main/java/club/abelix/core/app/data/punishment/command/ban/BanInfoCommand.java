package club.abelix.core.app.data.punishment.command.ban;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.punishment.meta.BanMeta;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.punishment.PunishmentManager;

import java.util.List;

@NamedCommand("baninfo")
public final class BanInfoCommand {

    @Dependency
    private PunishmentManager manager;

    @Executor
    @Tabcomplete("[playerName]")
    @WithPermission("command.baninfo")
    @WithCondition("disableConsole")
    @WithCondition("minArgs: 1")
    public void execute(CoreExecutionSession session) throws CommandTranslationException {
        boolean full = session.getGamer().hasPermission("command.baninfo");

        String target = session.getArgument(0);
        boolean isIp = Patterns.isIPV4(target);

        if (isIp && !full) {
            throw new CommandTranslationException("ban_cant_ip");
        }

        List<BanMeta> bans;

        if (isIp) {
            bans = manager.getActiveAddressBans(target);
        } else {
            bans = manager.getActiveNameBans(target);
        }

        if (bans.isEmpty()) {
            throw new CommandTranslationException("unban_not_banned");
        }

        Language language = session.getLanguage();

        session.sendLocalizedMessage(
                "baninfo_" + (isIp ? "ip" : "name") + "_result",
                "%target%", target
        );

        for (BanMeta ban : bans) {
            session.sendMessage(manager.getBanJsonText(ban, language, session.getGamer().isHelper(), full));
        }
    }
}
