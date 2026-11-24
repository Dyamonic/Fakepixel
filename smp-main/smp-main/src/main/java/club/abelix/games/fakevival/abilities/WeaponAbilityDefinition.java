package club.abelix.games.fakevival.abilities;

import club.abelix.games.fakevival.mmo.MMOItemType;
import club.abelix.games.fakevival.mmo.MMOStatType;

import java.util.List;
import java.util.Set;

public record WeaponAbilityDefinition(
        String id,
        String displayName,
        List<String> description,
        Set<MMOItemType> allowedItemTypes,
        AbilityTrigger trigger,
        int manaCost,
        int cooldownSeconds,
        double baseDamage,
        MMOStatType scalingStat,
        double radius,
        double range,
        int maxTargets
) implements CustomItemAbility {
    public WeaponAbilityDefinition {
        description = description == null ? List.of() : List.copyOf(description);
        allowedItemTypes = allowedItemTypes == null ? Set.of() : Set.copyOf(allowedItemTypes);
        manaCost = Math.max(0, manaCost);
        cooldownSeconds = Math.max(0, cooldownSeconds);
        radius = Math.max(0.0D, radius);
        range = Math.max(0.0D, range);
        maxTargets = Math.clamp(maxTargets, 1, 8);
    }

    @Override
    public boolean execute(AbilityContext context) {
        return WeaponAbilityExecutor.execute(this, context);
    }
}
