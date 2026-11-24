package club.abelix.commons.data.permission.prefix.predicator;

import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.gamer.IGamer;
import lombok.AllArgsConstructor;

/**
 * @author pro0xy
 * <p>Created on 09.09.2023 at 17:15</p>
 */

@AllArgsConstructor
public class LevelRequirement implements IRankPrefixModifierPredicator {
    private int lvl;

    @Override
    public boolean matchesRequirement(IGamer gamer) {
        return Leveling.getLevel(gamer.getProperty("Experience")) >= lvl;
    }

    @Override
    public int getRequirement() {
        return lvl;
    }
}
