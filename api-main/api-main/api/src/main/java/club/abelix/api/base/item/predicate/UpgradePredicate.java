package club.abelix.api.base.item.predicate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.GameItemStorage;
import club.abelix.commons.language.Language;
import com.google.common.base.Preconditions;

import java.util.Collections;
import java.util.List;

/**
 * Created by saharNooby on 22:10 27.10.2017
 */
public class UpgradePredicate implements GameItemPredicate {

    private final GameItem prev;

    public UpgradePredicate(int previousId) {
        this.prev = GameItemStorage.INSTANCE.getById(previousId);

        Preconditions.checkNotNull(prev, "Previous item not found");
    }

    @Override
    public boolean match(Gamer gamer) {
        return prev.has(gamer);
    }

    @Override
    public List<String> getRequirementLore(GameItem item, Gamer gamer) {
        Language language = gamer.getLanguage();

        return Collections.singletonList(language.getMessage("predicate_upgrade",
                "%upgrade%", prev.getDisplayName(language)));
    }

}
