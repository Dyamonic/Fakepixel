package club.abelix.api.server.game.loot.predicate.value;

import club.abelix.api.server.game.Game;

/**
 * @author Unidentified Person
 */
public class LootPredicateIntegerValue implements LootPredicateValue {

    private final int value;

    public LootPredicateIntegerValue(int value) {
        this.value = value;
    }

    @Override
    public int getValue(Game game) {
        return value;
    }

}
