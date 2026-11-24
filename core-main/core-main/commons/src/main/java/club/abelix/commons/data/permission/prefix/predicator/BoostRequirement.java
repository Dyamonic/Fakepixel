package club.abelix.commons.data.permission.prefix.predicator;

import club.abelix.commons.gamer.IGamer;
import lombok.AllArgsConstructor;

/**
 * @author pro0xy
 * <p>Created on 09.09.2023 at 17:16</p>
 */

@AllArgsConstructor
public class BoostRequirement implements IRankPrefixModifierPredicator {
    private int boostCount;

    @Override
    public boolean matchesRequirement(IGamer gamer) {
        return gamer.getProperty("DISCORD-BOOSTED") >= boostCount;
    }

    @Override
    public int getRequirement() {
        return boostCount;
    }
}
