package club.abelix.api.server.game.enchant;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.listener.YListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by saharNooby on 23:19 08.11.2017
 */
public class CustomEnchantmentsListener extends YListener {

    // Нужно регистрировать в onGameStart/onGameLoad
    // Не зарегал -- обосрался!!!)))

    @SuppressWarnings("unchecked")
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();

        if (damager instanceof Projectile) {
            List<MetadataValue> metadata = damager.getMetadata("HitCallbacks");
            if (metadata != null && !metadata.isEmpty()) {
                for (HitCallback callback : ((List<HitCallback>) metadata.get(0))) {
                    callback.onHit((Projectile) damager, null);
                }
            }
        } else if (damager instanceof Player) {
            Player player = (Player) damager;

            ItemStack item = player.getItemInHand();

            List<CustomEnchantment> list = CustomEnchantmentRegistry.getEnchantments(item);
            for (CustomEnchantment enchantment : list) {
                enchantment.onDamage(e, player, item, e.getEntity());
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) e.getEntity();

        List<CustomEnchantment> list = CustomEnchantmentRegistry.getEnchantments(e.getBow());
        if (list.isEmpty()) {
            return;
        }

        List<HitCallback> callbacks = new ArrayList<>();

        for (CustomEnchantment enchantment : list) {
            HitCallback callback = enchantment.onShoot(e, player, e.getBow());
            if (callback != null) {
                callbacks.add(callback);
            }
        }

        if (!callbacks.isEmpty()) {
            e.getProjectile().setMetadata("HitCallbacks", new FixedMetadataValue(FalconAPI.getInstance(), callbacks));
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        List<MetadataValue> metadata = e.getEntity().getMetadata("HitCallbacks");
        if (metadata != null && !metadata.isEmpty()) {
            for (HitCallback callback : ((List<HitCallback>) metadata.get(0))) {
                callback.onHit(e.getEntity(), null);
            }
        }
    }

}
