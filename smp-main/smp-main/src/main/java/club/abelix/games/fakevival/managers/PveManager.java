package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PveManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final SpawnProtectionManager spawnProtectionManager;
    private final ZoneManager zoneManager;
    private final Map<UUID, Long> combatTags = new HashMap<>();
    private GuildManager guildManager;

    public PveManager(JavaPlugin plugin, DataManager dataManager, SpawnProtectionManager spawnProtectionManager, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.spawnProtectionManager = spawnProtectionManager;
        this.zoneManager = zoneManager;
    }

    public void setGuildManager(GuildManager guildManager) { this.guildManager = guildManager; }
    public boolean isAllied(Player one, Player two) { return guildManager != null && guildManager.areAllied(one, two); }
    public boolean isGuildDamageBlocked(Player one, Player two) { return guildManager != null && guildManager.isGuildDamageBlocked(one, two); }

    public boolean canDamagePlayer(Player attacker, Player victim, EntityDamageEvent.DamageCause source, ItemStack item) {
        if (attacker == null || victim == null || attacker.equals(victim)) return false;
        if (!attacker.isOnline() || !victim.isOnline() || attacker.isDead() || victim.isDead()) return false;
        if (attacker.getGameMode() == GameMode.SPECTATOR || victim.getGameMode() == GameMode.SPECTATOR) return false;
        if (attacker.hasMetadata("vanished") || attacker.hasMetadata("vanish")
                || victim.hasMetadata("vanished") || victim.hasMetadata("vanish")) return false;
        if (isGuildDamageBlocked(attacker, victim)) return false;
        if (!attacker.getWorld().equals(victim.getWorld())) return false;
        if (!isWorldPvpEnabled(attacker.getWorld())) return false;
        if (zoneManager.isPvpBlocked(attacker.getLocation()) || zoneManager.isPvpBlocked(victim.getLocation())) return false;
        if (!zoneManager.allowsSpawnProtectionPvpOutsideZones()
                && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("pve.disable-pvp-in-spawn", true)
                && (spawnProtectionManager.isInside(attacker.getLocation()) || spawnProtectionManager.isInside(victim.getLocation()))) return false;
        return !isPve(attacker) && !isPve(victim);
    }

    private boolean isWorldPvpEnabled(World world) {
        if (world == null || !world.getPVP()) return false;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("pve.use-enabled-worlds", true)) return true;
        java.util.List<String> worlds = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("pve.enabled-worlds");
        if (worlds.isEmpty()) return world.getName().equalsIgnoreCase("world");
        return worlds.stream().anyMatch(name -> world.getName().equalsIgnoreCase(name));
    }

    public boolean isPve(Player player) {
        if (player == null) return false;
        return dataManager.getData().getBoolean(path(player.getUniqueId()), club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("pve.default-enabled", false));
    }

    public boolean togglePve(Player player) {
        boolean enabled = !isPve(player);
        dataManager.getData().set(path(player.getUniqueId()), enabled);
        dataManager.save();
        return enabled;
    }

    public void setPve(Player player, boolean enabled) {
        dataManager.getData().set(path(player.getUniqueId()), enabled);
        dataManager.save();
    }

    public void tag(Player player) {
        if (player == null) return;
        combatTags.put(player.getUniqueId(), System.currentTimeMillis() + getCombatTagMillis());
    }

    public void clearCombatTag(Player player) {
        if (player != null) combatTags.remove(player.getUniqueId());
    }

    public boolean isCombatTagged(Player player) {
        return getCombatTagRemainingMillis(player) > 0L;
    }

    public long getCombatTagRemainingMillis(Player player) {
        if (player == null) return 0L;
        Long expiresAt = combatTags.get(player.getUniqueId());
        if (expiresAt == null) return 0L;
        long remaining = expiresAt - System.currentTimeMillis();
        if (remaining <= 0L) {
            combatTags.remove(player.getUniqueId());
            return 0L;
        }
        return remaining;
    }

    public int getCombatTagRemainingSeconds(Player player) {
        long remaining = getCombatTagRemainingMillis(player);
        return (int) Math.max(1L, (remaining + 999L) / 1000L);
    }

    public long getCombatTagMillis() {
        return Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("pve.combat-tag-seconds", 5L)) * 1000L;
    }

    private String path(UUID uuid) {
        return "pve.players." + uuid + ".enabled";
    }
}
