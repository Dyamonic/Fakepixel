package club.abelix.api.server.game.enchant;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Created by saharNooby on 23:14 08.11.2017
 */
public abstract class CustomEnchantment {

    // Вызывается, когда игрок пиздит кого-то мечом/вещью
    public void onDamage(EntityDamageByEntityEvent e, Player damager, ItemStack item, Entity target) {

    }

    // Вызывается, когда игрок стреляет их лука с этим енчантом
    // HitCallback, если не null, вызовется, когда стрела куда-то попадёт
    public HitCallback onShoot(EntityShootBowEvent e, Player player, ItemStack bow) {
        return null;
    }

}
