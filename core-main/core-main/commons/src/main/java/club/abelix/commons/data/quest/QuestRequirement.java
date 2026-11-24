package club.abelix.commons.data.quest;

import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class QuestRequirement {

    private final StatsField field;
    private final int value;
    private final int experience;
    private final int coins;

    public String getQuestDescription(Language language) {
        String key = field.getDescription();

        if (language.isMessageTranslated(key)) {
            return language.getMessage(key + "_quest");
        }

        return key;
    }

}
