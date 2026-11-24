package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.commons.language.Language;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;

@NamedCommand("find")
public final class FindCommand {

    @Executor
    @Tabcomplete("[playerName]")
    @WithCondition("minArgs: 1")
    public static void execute(CoreExecutionSession session) throws CommandTranslationException {
        Gamer target = session.getGamer(0);

        if (!session.isConsole() && target != session.getGamer() && !session.getGamer().getRanks().isStaff()) {
            Gamer gamer = session.getGamer();

            if (!target.isFriend(gamer.getPlayerId())) {
                throw new CommandTranslationException("player_not_friend");
            }
        }

        Language language = session.getLanguage();

        FakePlayerNickname nickname = target.getFakeNickname();

        String displayName = null;

        if (nickname != null) {
            displayName = language.getMessage("core_find_nicked_format",
                    "%real_name%", target.getDisplayName(), "%mask%", nickname.formatDisplayName());
        } else {
            displayName = target.getDisplayName();
        }

        // todo сделать это
//        if (!target.isOnline()) {
//            session.sendLocalizedMessage("core_find_result_offline", "%name%",
//                    displayName, Duration.durationShortMs(language, java.time.Duration.between(target.getLoginDate(), LocalDate.now()).toMillis()));
//            return;
//        }

        AbstractServer server = target.getServer();
        String serverName = server == null ? language.getMessage("server_unknown") : server.getName();
        String proxyName = target.getProxy().getName();

        session.sendLocalizedMessage("core_find_result" + (target.isVanished() ? "_vanished" : ""),
                "%name%", displayName, "%server_name%", serverName, "%proxy_name%", proxyName);
    }

}
