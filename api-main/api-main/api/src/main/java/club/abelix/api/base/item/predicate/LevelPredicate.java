package club.abelix.api.base.item.predicate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.CommonWords;

import java.util.Collections;
import java.util.List;

/**
 * Created by saharNooby on 23:11 17.10.2017
 */
public class LevelPredicate implements GameItemPredicate {

    // Проверка на наличие уровня у игрока

    private final int level;

    public LevelPredicate(int level) {
        this.level = level;
    }

    @Override
    public boolean match(Gamer gamer) {
        return gamer.getLevel() >= this.level;
    }

    @Override
    public List<String> getRequirementLore(GameItem item, Gamer gamer) {
        Language language = gamer.getLanguage();

        return Collections.singletonList(
                language.getMessage("predicate_level_requirement",
                        "%level%", CommonWords.LEVEL.get(language).withNumber(this.level),
                        "%required%", gamer.getLevel()
                )
        );
    }


}
