package club.abelix.games.fakevival.abilities;

import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.mmo.MMOStatType;

import java.util.List;
import java.util.Set;

public interface CustomItemAbility {
    String id();

    String displayName();

    List<String> description();

    AbilityTrigger trigger();

    int manaCost();

    default Set<MMOItemType> allowedItemTypes() { return Set.of(); }

    default int cooldownSeconds() { return 0; }

    default double baseDamage() { return 0.0D; }

    default MMOStatType scalingStat() { return null; }

    default double radius() { return 0.0D; }

    default double range() { return 0.0D; }

    default int maxTargets() { return 1; }

    boolean execute(AbilityContext context);
}
