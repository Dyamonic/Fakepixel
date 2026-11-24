package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.event.GameDeathEvent;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author milansky
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class EnderPearlListener extends YListener {
    Map<Player, Collection<Projectile>> projectileMap = new HashMap<>();

    public EnderPearlListener(@NonNull BuildFFAGame game) {
        super(game.getPlugin());
    }

    @EventHandler
    public void onDeath(final @NotNull GameDeathEvent event) {
        val victim = event.getVictim();
        val projectiles = projectileMap.get(victim);

        if (projectiles != null) projectiles.forEach(Entity::remove);
    }

    @EventHandler
    public void onProjectileLaunch(final @NotNull ProjectileLaunchEvent event) {
        val projectile = event.getEntity();

        if (!(projectile.getShooter() instanceof Player player)) return;

        projectileMap.computeIfAbsent(player, __ -> new HashSet<>()).add(projectile);
    }

    @EventHandler
    public void onQuit(final @NotNull PlayerQuitEvent event) {
        projectileMap.remove(event.getPlayer());
    }
}
