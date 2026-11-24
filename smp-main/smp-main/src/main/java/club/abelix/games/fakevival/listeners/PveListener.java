package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.PveManager;
import club.abelix.games.fakevival.managers.SpawnProtectionManager;
import club.abelix.games.fakevival.managers.ZoneManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

public final class PveListener implements Listener {
    private final JavaPlugin plugin;
    private final PveManager pveManager;
    private final SpawnProtectionManager spawnProtectionManager;
    private final ZoneManager zoneManager;

    public PveListener(JavaPlugin plugin, PveManager pveManager, SpawnProtectionManager spawnProtectionManager, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.pveManager = pveManager;
        this.spawnProtectionManager = spawnProtectionManager;
        this.zoneManager = zoneManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = getAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;

        if (!pveManager.canDamagePlayer(attacker, victim, event.getCause(), attacker.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
            attacker.sendMessage(CC.color(pveManager.isAllied(attacker, victim) ? "&cYou cannot attack allied guild members." : "&cYou cannot damage players here."));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pveManager.clearCombatTag(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onValidPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = getAttacker(event);
        if (attacker == null || attacker.equals(victim)) return;
        if (!pveManager.canDamagePlayer(attacker, victim, event.getCause(), attacker.getInventory().getItemInMainHand())) return;
        pveManager.tag(attacker);
        pveManager.tag(victim);
    }

    private boolean isSpawnPvpBlocked(Player attacker, Player victim) {
        if (zoneManager.isPvpBlocked(attacker.getLocation()) || zoneManager.isPvpBlocked(victim.getLocation())) return true;
        if (zoneManager.allowsSpawnProtectionPvpOutsideZones()) return false;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("pve.disable-pvp-in-spawn", true)) return false;
        return spawnProtectionManager.isInside(attacker.getLocation()) || spawnProtectionManager.isInside(victim.getLocation());
    }

    private boolean isWorldPvpEnabled(World world) {
        if (world == null) return false;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("pve.use-enabled-worlds", true)) return true;
        List<String> worlds = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("pve.enabled-worlds");
        if (worlds.isEmpty()) return world.getName().equalsIgnoreCase("world");
        for (String name : worlds) {
            if (world.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private Player getAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) return player;
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }
}
