package club.abelix.api.server.game.loot.predicate.value;

import club.abelix.api.server.game.Game;

/**
 * @author Unidentified Person
 */
public class LootPredicateMetadataValue implements LootPredicateValue {

    private final String value;

    public LootPredicateMetadataValue(String value) {
        this.value = value;
    }

    @Override
    public int getValue(Game game) {
        return game.getMetadata().get(value);
    }

}
