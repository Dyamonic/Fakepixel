package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.api.command.CoreExecutionSession;

@WithAlias("dumpgroups")
@NamedCommand("dumpgroup")
public final class AdminDumpGroupsCommand {

    @Executor
    @Tabcomplete("[group]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.dumpgroup")
    public static void execute(CoreExecutionSession session) {
        session.sendLocalizedMessage("admin_dump_group_start");
//        RankSqlHandler.INSTANCE.getPlayersByRank(group, true).whenCompleteAsync((tIntList, throwable) -> {
//            if (throwable != null) {
//                session.sendLocalizedMessage("admin_dump_group_failed");
//                return;
//            }
//
//            StringBuilder resultString = new StringBuilder("Abelix | Group Dump System");
//            resultString.append("\n\n==================");
//            resultString.append("\nGroup: ").append(group.getName());
//            resultString.append("\nDate & Time: ").append(LocalDateTime.now());
//            resultString.append("\n==================\n");
//            resultString.append("\nUsers:");
//
//            tIntList.forEach(playerId -> {
//                Gamer gamer = Gamer.getOfflineGamer(playerId, Gamer::loadPlayerName);
//
//                resultString.append('\n');
//                if (gamer == null) {
//                    resultString.append(String.format("Unknown gamer (%d)", playerId));
//                } else {
//                    resultString.append(gamer.getName());
//                }
//
//                return true;
//            });
//
//            try {
//                String resultUrl = HastebinUtil.getUrl(resultString.toString());
//
//                session.sendLocalizedMessage("admin_dump_group_success",
//                        "%group%", group.getDisplayName(),
//                        "%url%", resultUrl);
//            } catch (IOException e) {
//                session.sendLocalizedMessage("admin_dump_group_failed");
//            }
//        });
    }
}
