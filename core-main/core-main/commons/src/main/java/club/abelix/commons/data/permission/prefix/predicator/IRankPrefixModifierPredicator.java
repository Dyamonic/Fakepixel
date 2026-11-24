package club.abelix.commons.data.permission.prefix.predicator;

import club.abelix.commons.gamer.IGamer;

/**
 * @author pro0xy
 * <p>Created on 09.09.2023 at 17:06</p>
 */

public interface IRankPrefixModifierPredicator {
    boolean matchesRequirement(IGamer gamer);

    int getRequirement();
}
