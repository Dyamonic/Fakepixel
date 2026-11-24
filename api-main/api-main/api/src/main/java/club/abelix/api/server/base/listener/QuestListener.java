package club.abelix.api.server.base.listener;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameStatsChangeEvent;
import club.abelix.commons.data.quest.Quest;
import club.abelix.commons.data.quest.QuestRequirement;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;
import org.bukkit.event.EventHandler;

public class QuestListener extends YListener {

    public QuestListener(@NonNull FalconAPI plugin) {
        super(plugin);
    }

    /**
     * Проверяем текущий квест на выполнение его полностью (или его части)
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuestComplete(GameStatsChangeEvent event) {
        Gamer gamer = event.getGamer();
        gamer.resolveGame();

        Game game = gamer.getGame();

        if (game == null) {
            return;
        }

        Quest quest = game.getTodayQuest();

        if (quest == null) {
            return;
        }

        StatsField statsField = event.getStatsField();

        int delta = event.getDelta();

        int newStatValue = event.getTotalStatValue();
        int oldStatValue = newStatValue - delta;

        QuestRequirement currentRequirement = null;

        for (QuestRequirement questRequirement : quest.getRequirements()) {
            if (questRequirement.getField().equals(statsField)) {
                currentRequirement = questRequirement;
                break;
            }
        }

        if (currentRequirement == null) {
            return;
        }

        int requiredValue = currentRequirement.getValue();

        //квест только что выполнили - старое значение статы было МЕНЬШЕ чем необходимое
        if (oldStatValue < requiredValue && newStatValue >= requiredValue) {
            int uncompletedQuests = 0;

            //проверяем количество оставшихся квестов, чтобы если что написать верное сообщение
            for (QuestRequirement questRequirement : quest.getRequirements()) {
                StatsField requirementField = questRequirement.getField();

                if (gamer.getTodayStat(requirementField) < questRequirement.getValue()) {
                    uncompletedQuests++;
                }
            }

            Mini mini = game.getMini();
            String currentModeName = mini.getDisplayName();

            if (uncompletedQuests > 0) {
                gamer.sendLocalizedMessage("daily_quest_progress",
                        "%description%", currentRequirement.getQuestDescription(gamer.getLanguage()),
                        "%mode%", currentModeName,
                        "%uncompleted_quests%", uncompletedQuests
                );
            } else {
                gamer.sendLocalizedMessage("daily_quest_completed",
                        "%mode%", currentModeName);

                gamer.addCoins(mini.getMaster(), quest.getCoinReward(), true, "quest_completed");
                gamer.addExperience(quest.getExperienceReward(), "quest_completed");
            }
        }
    }

}
