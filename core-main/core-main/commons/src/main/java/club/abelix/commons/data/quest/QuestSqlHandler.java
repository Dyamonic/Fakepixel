package club.abelix.commons.data.quest;

import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.mysql.modern.BufferedRow;
import club.abelix.commons.mysql.modern.Database;
import club.abelix.commons.mysql.modern.DatabaseCredentials;
import club.abelix.commons.mysql.modern.impl.MySQL;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class QuestSqlHandler {

    private static final String HAS_QUESTS_FOR_DAY
            = "SELECT COUNT(*) AS `count` FROM `Quests` WHERE `day` = ?;";
    private static final String GET_QUEST
            = "SELECT `field`, `value`, `experience`, `coins` FROM `Quests` WHERE `mode` = ? AND `day` = ?;";
    private static final String ADD_QUEST
            = "INSERT IGNORE INTO `Quests` (`mode`, `day`, `field`, `value`, `experience`, `coins`) VALUES (?, ?, ?, ?, ?, ?);";
    public static QuestSqlHandler INSTANCE = new QuestSqlHandler();
    private final Database database = new MySQL(DatabaseCredentials.MAIN.withDatabase("playerdata"));

    public CompletableFuture<Boolean> hasQuestsForDay(int day) {
        if (day < 1) {
            throw new IllegalArgumentException("Invalid day " + day);
        }

        return database.executeQuery(HAS_QUESTS_FOR_DAY, day)
                .thenApply(rows -> !rows.isEmpty() && rows.getFirst().getRequiredInt("count") > 0);
    }

    public CompletableFuture<Quest> getQuest(@NonNull StatsMode mode, int day) {
        if (day < 1) {
            throw new IllegalArgumentException("Invalid day " + day);
        }

        return database.executeQuery(GET_QUEST, mode.getTable(), day)
                .thenApply(rows -> {
                    if (rows.isEmpty()) {
                        return null;
                    }

                    List<QuestRequirement> requirements = new ArrayList<>();

                    for (BufferedRow row : rows) {
                        StatsField field = mode.getField(row.getRequiredString("field"));

                        if (field != null) {
                            requirements.add(new QuestRequirement(field,
                                    row.getRequiredInt("value"),
                                    row.getRequiredInt("experience"),
                                    row.getRequiredInt("coins")
                            ));
                        }
                    }
                    return requirements.isEmpty() ? null : new Quest(day, mode, requirements);
                });
    }

    public void addQuest(@NonNull Quest quest) {
        for (QuestRequirement requirement : quest.getRequirements()) {
            database.executeVoidUpdate(ADD_QUEST,
                    quest.getMode().getTable(), quest.getDay(),
                    requirement.getField().getName(), requirement.getValue(),
                    requirement.getExperience(), requirement.getCoins());
        }
    }

}
