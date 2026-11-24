package club.abelix.api.base.item.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.predicate.GameItemPredicate;
import club.abelix.api.base.item.predicate.PredicateType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by saharNooby on 18:21 18.10.2017
 */
public class PredicateMatcher {

    // Представляет результат проверки всех матчеров вещи определённого типа

    @Getter
    private final boolean allMatch;
    @Getter
    private final boolean matches;
    @Getter
    private final List<GameItemPredicate> failed = new ArrayList<>();

    public PredicateMatcher(Gamer gamer, GameItem item, PredicateType type, boolean allMatch) {
        List<GameItemPredicate> predicates = item.predicates(type);

        this.allMatch = allMatch;

        if (predicates.isEmpty()) {
            // в большинстве случаев список будет пустой
            this.matches = !allMatch;
            return;
        }

        for (GameItemPredicate p : predicates) {
            if (!p.match(gamer)) {
                this.failed.add(p);
            }
        }

        if (allMatch) {
            this.matches = this.failed.isEmpty();
        } else {
            this.matches = this.failed.size() < predicates.size() || predicates.isEmpty();
        }
    }

}
