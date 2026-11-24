package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.manager.progression.QuestManager;

@NamedCommand("questdebug")
public class QuestDebugCommand {
    private static QuestManager questManager;

    public static void setQuestManager(QuestManager manager) {
        questManager = manager;
    }

    @Executor
    @WithCondition("disableConsole")
    @WithPermission("buildffa.questdebug")
    public static void execute(BukkitExecutionSession session) {
        if (questManager == null) return;
        String[] arguments = session.getArguments();
        if (arguments.length > 0 && "retry".equalsIgnoreCase(arguments[0])) {
            questManager.retryInitialization(session.getGamer().getPlayer());
            return;
        }
        questManager.showDiagnostic(
                session.getGamer().getPlayer(),
                arguments.length == 0 ? null : arguments[0]
        );
    }
}
