package club.abelix.commons.data.quest;

import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.util.random.RandomUtil;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class QuestGenerator {

    private static final String[] REQUIRED_DATA_VALUES = {
            "quest_bounds", "quest_step", "quest_coins", "quest_experience"
    };

    private static final int FIELDS_TO_SELECT = 3;

    public static List<Quest> generateForAllModes(int day) {
        List<Quest> result = new ArrayList<>();

        for (StatsMode mode : StatsManager.INSTANCE.getModes()) {
            Quest quest = generateForMode(day, mode);

            if (quest != null) {
                result.add(quest);
            }
        }

        return result;
    }

    private static Quest generateForMode(int day, @NonNull StatsMode mode) {
        List<StatsField> validFields = new ArrayList<>();

        for (StatsField field : mode.getFields()) {
            if (!field.hasDescription()) {
                continue;
            }

            boolean allowed = true;

            for (String value : REQUIRED_DATA_VALUES) {
                if (field.getDataValue(value) == null) {
                    allowed = false;
                    break;
                }
            }

            if (allowed) {
                validFields.add(field);
            }
        }

        int fields = Math.min(validFields.size(), FIELDS_TO_SELECT);

        if (fields == 0) {
            return null;
        }

        List<QuestRequirement> requirements = new ArrayList<>();

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < fields; i++) {
            StatsField field = RandomUtil.removeRandomElement(validFields);

            // Такое вряд ли будет, но всё возможно
            if (field == null) {
                continue;
            }

            try {
                int coinsPerBound = Integer.parseInt(field.getDataValue("quest_coins"));
                int expPerBound = Integer.parseInt(field.getDataValue("quest_experience"));

                String[] split = field.getDataValue("quest_bounds").split("/");

                int min = Integer.parseInt(split[0]);
                int max = Integer.parseInt(split[1]);

                if (min == max) {
                    requirements.add(new QuestRequirement(
                            field, min, min * expPerBound, min * coinsPerBound
                    ));
                    continue;
                }

                String stepStr = field.getDataValue("quest_step");

                int step = stepStr != null ? Integer.parseInt(stepStr) : 1;

                if (step < 1) {
                    step = 1;
                }

                int steps = (max + 1 - min) / step;

                if (steps < 1) {
                    requirements.add(new QuestRequirement(
                            field, min, min * expPerBound, min * coinsPerBound
                    ));
                    continue;
                }

                int value = min + step * random.nextInt(steps);

                requirements.add(new QuestRequirement(
                        field, value, value * expPerBound, value * coinsPerBound
                ));
            } catch (Exception e) {
                Logging.IMP.warn("Invalid quest connection in field " + mode.getTable() + "/" + field.getName());
                Logging.IMP.warn("Field connection is " + field.getData());

                e.printStackTrace();
            }
        }

        if (requirements.size() < fields) {
            return null;
        }

        return new Quest(day, mode, requirements);
    }

}
