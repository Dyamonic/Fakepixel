package club.abelix.api.server.lobby.impl.lobby.quest;

import club.abelix.api.base.command.BukkitExecutionSession;
import club.abelix.api.server.lobby.impl.lobby.GameLobbyServer;
import club.abelix.commons.command.IgnoreLogs;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.data.quest.Quest;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.RequiredArgsConstructor;

import java.util.List;

@WithAlias("квесты")
@WithAlias("квест")
@NamedCommand("quests")
@RequiredArgsConstructor
@IgnoreLogs
public class QuestsCommand {

    private final GameLobbyServer lobbyServer;

    @Executor
    @WithCondition("disableConsole")
    public void execute(BukkitExecutionSession session) throws CommandTranslationException {
        List<Quest> quests = lobbyServer.getQuests();

        if (quests == null || quests.isEmpty()) {
            throw new CommandTranslationException("quests_not_found");
        }

        DiBukkitPlatform.withBeanOrElse(
                QuestGuiFactory.class,
                f -> f.createQuestsGui(session.getGamer(), quests, null),
                () -> new QuestsGui(session.getPlayer(), quests)
        );
    }

    @SubcommandExecutor("reload")
    @WithPermission("quests.reload")
    public void executeReload(BukkitExecutionSession session) {
        lobbyServer.loadQuests();
        session.sendMessage("Quests reloaded");
    }
}
