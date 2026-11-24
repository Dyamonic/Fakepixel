package club.abelix.core.app.data.quest;

import club.abelix.commons.data.quest.Quest;
import club.abelix.commons.data.quest.QuestGenerator;
import club.abelix.commons.data.quest.QuestSqlHandler;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.app.Async;
import club.abelix.network.common.logging.Logging;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class QuestManager {

    // ------------------------------------------------------------------------------

    public static final QuestManager INSTANCE = new QuestManager();

    // ------------------------------------------------------------------------------

    public void startQuestGenerator() {
        Async.runTimer(() -> {
            long currDay = TimeUtil.getDayUTC();

            for (int trial = 0; trial < 3 && !QuestSqlHandler.INSTANCE.hasQuestsForDay((int) currDay).join(); trial++) {
                Logging.IMP.info("[QuestManager] No quests detected for current day!");

                generateQuests((int) currDay);
            }

        }, 5, 30, TimeUnit.SECONDS);
    }

    private void generateQuests(int day) {
        Logging.IMP.info("[QuestManager] Refreshing StatsManager");

        List<Quest> quests = QuestGenerator.generateForAllModes(day);

        for (Quest quest : quests) {
            QuestSqlHandler.INSTANCE.addQuest(quest);
        }

        Logging.IMP.info("[QuestManager] Generated " + quests.size() + " new quests");
    }

}
