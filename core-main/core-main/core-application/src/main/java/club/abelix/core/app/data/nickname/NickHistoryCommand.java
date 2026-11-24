package club.abelix.core.app.data.nickname;

import club.abelix.commons.command.Async;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.data.nickname.FakePlayerNickname;
import club.abelix.commons.data.nickname.NickSqlHandler;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.data.gamer.Gamer;

@NamedCommand("nickhistory")
@WithAlias("nickinfo")
public class NickHistoryCommand {

    @Executor
    @WithCondition("minArgs: 1")
    @WithPermission("command.nickhistory")
    @Async
    public static void executor(CoreExecutionSession session) {
        String nick = session.getArgument(0);

        NickSqlHandler.INSTANCE.getNicknameHistory(nick).thenAccept(entries -> {
            if (entries.isEmpty()) {
                session.sendLocalizedMessage("command_nickhistory_empty");
                return;
            }

            session.sendLocalizedMessage("command_nickhistory_title",
                    "%nick%", nick, "%size%", entries.size());

            Language language = session.getLanguage();

            for (FakePlayerNickname.HistoryEntry entry : entries) {
                FakePlayerNickname fakeNick = entry.getNickname();

                Gamer owner = Gamer.getOfflineGamer(fakeNick.getPlayerId(), offline -> {
                    offline.loadPlayerId();
                    offline.loadRanks();
                    offline.loadPrefix();
                });

                if (owner == null) {
                    continue;
                }

                String status;

                if (entry.isActive()) {
                    status = language.getMessage("command_nickhistory_entry_active");
                } else {
                    status = language.getMessage("command_nickhistory_entry_finished",
                            "%end_date%", TimeUtil.getDateTimeString(language, entry.getEndDate()));
                }

                session.sendLocalizedMessage("command_nickhistory_entry",
                        "%owner%", owner.getDisplayName(),
                        "%rank%", fakeNick.getGroup().getDisplayName(session.getLanguage()),
                        "%issue_date%", TimeUtil.getDateTimeString(language, entry.getIssueDate()),
                        "%status%", status
                );
            }
        });
    }

}
