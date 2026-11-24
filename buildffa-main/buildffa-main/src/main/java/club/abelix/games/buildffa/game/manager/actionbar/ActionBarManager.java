package club.abelix.games.buildffa.game.manager.actionbar;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.vote.VoteTask;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import static club.abelix.api.base.util.chat.MessageSender.sendActionBar;

public class ActionBarManager {

    private final BuildFFAGame game;
    private final VoteTask task;

    public ActionBarManager(final @NotNull BuildFFAGame game, VoteTask task) {
        this.game = game;
        this.task = task;
    }

    public void startUpdatingActionBar() {
        new ActionBarTask(game.getPlugin()).timer(0L, 20L);
    }

    private void updateActionBar(Gamer gamer) {
        if (game.getCpsCheckManager() != null && game.getCpsCheckManager().isMonitoring(gamer.getPlayer())) return;
        val language = gamer.getLanguage();
        val actionBarMessage = language.getMessage("action_bar_name",
                "%killStreak%", BuildFFAGamer.get(gamer).getCurrentKillStreak(),
                "%arenaTime%", task.getSeconds(),
                "%kitName%", game.getSelectedKit(gamer).getName());

        sendActionBar(gamer.getPlayer(), actionBarMessage);
    }

    private class ActionBarTask extends AbstractTask {

        public ActionBarTask(final AbstractPlugin plugin) {
            super(plugin);
        }

        @Override
        protected void run() {
            game.getQuestManager().checkRotationReset();
            Gamer.getGamers().forEach(ActionBarManager.this::updateActionBar);
        }
    }
}
