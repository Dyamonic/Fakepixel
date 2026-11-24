package club.abelix.api.base.util.item;

import com.google.common.collect.ImmutableList;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

@UtilityClass
public class PotionUtil {

    private final ImmutableList<PotionEffectType> POSITIVE_POTION_EFFECTS = new ImmutableList.Builder<PotionEffectType>()
            .add(PotionEffectType.ABSORPTION)
            .add(PotionEffectType.DAMAGE_RESISTANCE)
            .add(PotionEffectType.FAST_DIGGING)
            .add(PotionEffectType.FIRE_RESISTANCE)
            .add(PotionEffectType.HEAL)
            .add(PotionEffectType.HEALTH_BOOST)
            .add(PotionEffectType.INCREASE_DAMAGE)
            .add(PotionEffectType.JUMP)
            .add(PotionEffectType.NIGHT_VISION)
            .add(PotionEffectType.REGENERATION)
            .add(PotionEffectType.SATURATION)
            .add(PotionEffectType.SPEED)
            .build();

    private final ImmutableList<PotionEffectType> NEGATIVE_POTION_EFFECTS = new ImmutableList.Builder<PotionEffectType>()
            .add(PotionEffectType.BLINDNESS)
            .add(PotionEffectType.CONFUSION)
            .add(PotionEffectType.HARM)
            .add(PotionEffectType.HUNGER)
            .add(PotionEffectType.POISON)
            .add(PotionEffectType.SLOW)
            .add(PotionEffectType.SLOW_DIGGING)
            .add(PotionEffectType.WEAKNESS)
            .add(PotionEffectType.WITHER)
            .build();

    public boolean isPositive(PotionEffectType potionEffectType) {
        return POSITIVE_POTION_EFFECTS.contains(potionEffectType);
    }

    public boolean isNegative(PotionEffectType potionEffectType) {
        return NEGATIVE_POTION_EFFECTS.contains(potionEffectType);
    }

    public boolean isNeutral(PotionEffectType potionEffectType) {
        return !isPositive(potionEffectType) && !isNegative(potionEffectType);
    }

    public List<PotionEffectType> getPositivePotionEffects() {
        return POSITIVE_POTION_EFFECTS;
    }

    public List<PotionEffectType> getNegativePotionEffects() {
        return NEGATIVE_POTION_EFFECTS;
    }

    public PotionEffect getPotionEffect(Player player, PotionEffectType potionEffectType) {
        return player.getActivePotionEffects().stream()
                .filter(potionEffect -> potionEffect.getType() == potionEffectType)
                .findFirst()
                .orElse(null);
    }

}
