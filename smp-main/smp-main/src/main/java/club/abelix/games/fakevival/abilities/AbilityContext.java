package club.abelix.games.fakevival.abilities;

import club.abelix.games.fakevival.Game;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public record AbilityContext(
        Game plugin,
        Player player,
        ItemStack item,
        String itemId,
        AbilityTrigger trigger,
        LivingEntity target,
        Location location,
        double damage,
        EntityDamageEvent damageEvent,
        Projectile projectile
) {
}
