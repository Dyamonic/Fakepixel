package club.abelix.api.server.game.enchant;

import lombok.NonNull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Created by saharNooby on 23:16 08.11.2017
 */
public class EffectEnchantment extends CustomEnchantment {

    private final PotionEffect effect;

    public EffectEnchantment(@NonNull PotionEffect effect) {
        this.effect = effect;
    }

    public EffectEnchantment(@NonNull PotionEffectType type, int duration, int amplifier) {
        this(new PotionEffect(type, duration, amplifier));
    }

    @Override
    public void onDamage(EntityDamageByEntityEvent e, Player damager, ItemStack item, Entity target) {
        if (target instanceof LivingEntity) {
            ((LivingEntity) target).addPotionEffect(this.effect);
        }
    }

    @Override
    public HitCallback onShoot(EntityShootBowEvent e, Player player, ItemStack bow) {
        return (projectile, entity) -> {
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).addPotionEffect(this.effect);
            }
        };
    }

}
