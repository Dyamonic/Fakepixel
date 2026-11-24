package club.abelix.games.buildffa.game.command;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.execution.Executor;
import club.abelix.games.buildffa.game.manager.progression.QuestManager;

@NamedCommand("quests")
public class QuestsCommand {
    private static QuestManager questManager;

    public static void setQuestManager(QuestManager manager) {
        questManager = manager;
    }

    @Executor
    @WithCondition("disableConsole")
    public static void execute(BukkitExecutionSession session) {
        if (questManager != null) questManager.open(session.getGamer().getPlayer());
    }
}
