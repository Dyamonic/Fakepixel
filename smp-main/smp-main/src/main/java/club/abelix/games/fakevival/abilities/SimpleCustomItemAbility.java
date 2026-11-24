package club.abelix.games.fakevival.abilities;

import java.util.List;

public record SimpleCustomItemAbility(
        String id,
        String displayName,
        List<String> description,
        AbilityTrigger trigger,
        int manaCost,
        AbilityExecutor executor
) implements CustomItemAbility {
    @Override
    public boolean execute(AbilityContext context) {
        return executor.execute(context);
    }

    @FunctionalInterface
    public interface AbilityExecutor {
        boolean execute(AbilityContext context);
    }
}
