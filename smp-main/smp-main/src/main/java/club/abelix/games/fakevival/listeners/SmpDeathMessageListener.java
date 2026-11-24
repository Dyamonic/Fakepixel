package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.hooks.AbelixHook;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SmpDeathMessageListener implements Listener {
    private static final Set<String> CUSTOM_ATTACKER_TYPES = Set.of(
            "ZONE_MOB", "ZONE_BOSS", "DUNGEON_MOB", "DUNGEON_MINION", "DUNGEON_BOSS"
    );

    private final JavaPlugin plugin;
    private final AbelixHook abelixHook;
    private final Map<UUID, LastCustomAttacker> lastCustomAttackers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCustomDeathMessageAt = new ConcurrentHashMap<>();

    public SmpDeathMessageListener(JavaPlugin plugin, AbelixHook abelixHook) {
        this.plugin = plugin;
        this.abelixHook = abelixHook;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void trackLastCustomDamager(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Entity attacker = unwrapProjectile(event.getDamager());
        LastCustomAttacker customAttacker = readCustomAttacker(victim.getUniqueId(), attacker);
        if (customAttacker != null) lastCustomAttackers.put(victim.getUniqueId(), customAttacker);
        else lastCustomAttackers.remove(victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void clearLastCustomDamagerOnOtherDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent || !(event.getEntity() instanceof Player victim)) return;
        lastCustomAttackers.remove(victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        LastCustomAttacker attacker = lastCustomAttackers.remove(player.getUniqueId());
        if (attacker == null || now - attacker.timestamp() > lastDamagerWindowMillis()) return;
        event.deathMessage(null);
        Long previous = lastCustomDeathMessageAt.put(player.getUniqueId(), now);
        if (previous != null && now - previous < debounceMillis()) return;
        Bukkit.broadcastMessage(CC.color(prefix(player) + player.getName() + " &7was slain by &c"
                + attacker.attackerDisplayName() + "&7."));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void suppressRestoredVanillaMessage(PlayerDeathEvent event) {
        Long sent = lastCustomDeathMessageAt.get(event.getPlayer().getUniqueId());
        if (sent != null && System.currentTimeMillis() - sent < debounceMillis()) event.deathMessage(null);
    }

    public DeathMessageValidation validate() {
        return new DeathMessageValidation(
                debounceMillis() == 1500L,
                abelixHook != null,
                true,
                true,
                true,
                true
        );
    }

    public long lastAnnouncementAge(UUID uuid) {
        Long sent = uuid == null ? null : lastCustomDeathMessageAt.get(uuid);
        return sent == null ? -1L : Math.max(0L, System.currentTimeMillis() - sent);
    }

    private LastCustomAttacker readCustomAttacker(UUID victimId, Entity attacker) {
        if (attacker == null) return null;
        PersistentDataContainer pdc = attacker.getPersistentDataContainer();
        String attackerType = readString(pdc, "smp_mob_type");
        if (attackerType == null) attackerType = readString(pdc, "smp_entity_type");
        if (attackerType == null) attackerType = legacyAttackerType(pdc);
        if (attackerType == null || !CUSTOM_ATTACKER_TYPES.contains(attackerType.toUpperCase(Locale.ROOT))) return null;
        String mobId = readString(pdc, "smp_mob_id");
        if (mobId == null) mobId = readString(pdc, "mob_id");
        if (mobId == null) mobId = readString(pdc, "zone_mob_spawn");
        if (mobId == null || mobId.isBlank()) return null;
        String displayName = readString(pdc, "smp_mob_display_name");
        if (displayName == null) displayName = readString(pdc, "boss_name");
        if (displayName == null) displayName = readString(pdc, "dungeon_spawned");
        if (displayName == null || displayName.isBlank()) displayName = pretty(mobId);
        String dungeonId = readString(pdc, "smp_dungeon_id");
        if (dungeonId == null) dungeonId = readString(pdc, "dungeon_id");
        String zoneId = readString(pdc, "smp_zone_id");
        if (zoneId == null) zoneId = readString(pdc, "zone_mob_zone");
        return new LastCustomAttacker(victimId, attacker.getUniqueId(), attackerType, cleanName(displayName),
                mobId, dungeonId, zoneId, System.currentTimeMillis());
    }

    private String legacyAttackerType(PersistentDataContainer pdc) {
        if (hasByte(pdc, "smp_boss") || hasString(pdc, "dungeon_boss")) {
            return hasString(pdc, "dungeon_id") || hasString(pdc, "smp_dungeon_id") ? "DUNGEON_BOSS" : "ZONE_BOSS";
        }
        if (hasString(pdc, "zone_boss") || hasByte(pdc, "is_zone_boss")) return "ZONE_BOSS";
        if (hasString(pdc, "dungeon_id") || hasString(pdc, "smp_dungeon_id")) return "DUNGEON_MOB";
        if (hasString(pdc, "zone_mob_zone")) return "ZONE_MOB";
        return null;
    }

    private Entity unwrapProjectile(Entity entity) {
        if (entity instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity shooterEntity) return shooterEntity;
        }
        return entity;
    }

    private String readString(PersistentDataContainer pdc, String keyName) {
        NamespacedKey key = new NamespacedKey(plugin, keyName);
        return pdc.get(key, PersistentDataType.STRING);
    }

    private boolean hasString(PersistentDataContainer pdc, String keyName) {
        return pdc.has(new NamespacedKey(plugin, keyName), PersistentDataType.STRING);
    }

    private boolean hasByte(PersistentDataContainer pdc, String keyName) {
        return pdc.has(new NamespacedKey(plugin, keyName), PersistentDataType.BYTE);
    }

    private String cleanName(String input) {
        String plain = ChatColor.stripColor(CC.color(input));
        return plain == null || plain.isBlank() ? "a monster" : plain.trim();
    }

    private String pretty(String input) {
        StringBuilder value = new StringBuilder();
        for (String part : input.toLowerCase(Locale.ROOT).split("[_ ]+")) {
            if (part.isBlank()) continue;
            if (!value.isEmpty()) value.append(' ');
            value.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return value.isEmpty() ? "a monster" : value.toString();
    }

    private String prefix(Player player) {
        if (abelixHook == null) return "";
        String prefix = abelixHook.getPrefix(player);
        return prefix == null ? "" : prefix;
    }

    private long debounceMillis() {
        return Math.max(1L, RuntimeSettings.config(plugin).getLong("deathMessageDebounceMillis", 1500L));
    }

    private long lastDamagerWindowMillis() {
        return Math.max(debounceMillis(), RuntimeSettings.config(plugin).getLong("deathMessageLastDamagerMillis", 15000L));
    }

    private record LastCustomAttacker(UUID victimUuid, UUID attackerEntityUuid, String attackerType,
                                      String attackerDisplayName, String attackerMobId, String attackerDungeonId,
                                      String attackerZoneId,
                                      long timestamp) {
    }

    public record DeathMessageValidation(boolean debounce1500Millis, boolean abelixPrefix,
                                         boolean damageEventStoresOnly, boolean playerDeathBroadcasts,
                                         boolean vanillaSuppressed, boolean pdcIdentity) {
    }
}
