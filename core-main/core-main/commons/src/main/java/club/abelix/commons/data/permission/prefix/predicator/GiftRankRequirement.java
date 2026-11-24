package club.abelix.commons.data.permission.prefix.predicator;

import club.abelix.commons.gamer.IGamer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GiftRankRequirement implements IRankPrefixModifierPredicator {
    private int count;

    @Override
    public boolean matchesRequirement(IGamer gamer) {
        return gamer.getProperty("Gifted") >= count;
    }

    @Override
    public int getRequirement() {
        return count;
    }
}
