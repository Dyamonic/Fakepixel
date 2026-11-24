package club.abelix.games.fakevival.mobengine.mob;

import club.abelix.games.fakevival.mobengine.profile.SMPMobPerformanceProfiler;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SMPMobClientLoadController {
    private final Plugin plugin;
    private final SMPMobPerformanceProfiler profiler;
    private final Map<UUID, Integer> visibleNormalByPlayer = new HashMap<>();
    private final Map<UUID, Integer> visibleImportantByPlayer = new HashMap<>();
    private final Map<UUID, Integer> hitParticleBudget = new HashMap<>();
    private int visibleTotal;
    private int sleeping;
    private int fullRender;
    private int reducedRender;
    private int hidden;
    private int nameplateUpdates;
    private int particlesSent;
    private int secondTick;

    public SMPMobClientLoadController(Plugin plugin, SMPMobPerformanceProfiler profiler) {
        this.plugin = plugin;
        this.profiler = profiler;
    }

    public void beginTick(long tick) {
        visibleNormalByPlayer.clear();
        visibleImportantByPlayer.clear();
        visibleTotal = 0;
        sleeping = 0;
        fullRender = 0;
        reducedRender = 0;
        hidden = 0;
        nameplateUpdates = 0;
        if (++secondTick >= 20) {
            secondTick = 0;
            hitParticleBudget.clear();
            particlesSent = 0;
        }
    }

    public SMPMobActivationRange.Activation applyVisibility(SMPMob mob, SMPMobActivationRange.Activation activation, long tick) {
        LivingEntity entity = mob.getEntity();
        boolean anyVisible = false;
        boolean anyNametag = false;
        boolean important = mob.isImportant();
        boolean combatLocked = mob.isCombatLocked(System.currentTimeMillis());
        double bestDistance = activation.distanceSquared();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid() || !player.getWorld().equals(entity.getWorld())) {
                player.hideEntity(plugin, entity);
                continue;
            }
            double distanceSquared = player.getLocation().distanceSquared(entity.getLocation());
            boolean inHardRange = distanceSquared <= square(SMPMobEngineSettings.HIDDEN_VISIBILITY_RANGE);
            boolean underDensity = reserveVisibleSlot(player, important, distanceSquared);
            boolean show = inHardRange && (underDensity || (combatLocked && player.equals(activation.player())));
            if (show) {
                player.showEntity(plugin, entity);
                anyVisible = true;
                visibleTotal++;
                if (distanceSquared <= square(SMPMobEngineSettings.NAMETAG_RANGE)) anyNametag = true;
            } else {
                player.hideEntity(plugin, entity);
            }
        }

        entity.setCustomNameVisible(anyNametag);
        if (!anyVisible && !combatLocked) {
            mob.setRenderTier(SMPMob.RenderTier.HIDDEN);
            hidden++;
            sleeping++;
            return new SMPMobActivationRange.Activation(activation.player(), bestDistance, SMPMob.State.SLEEPING);
        }

        SMPMob.RenderTier tier = bestDistance <= square(SMPMobEngineSettings.FULL_VISIBILITY_RANGE) || important || combatLocked
                ? SMPMob.RenderTier.FULL : SMPMob.RenderTier.BASIC;
        mob.setRenderTier(tier);
        if (tier == SMPMob.RenderTier.FULL) fullRender++;
        else reducedRender++;

        int nameInterval = tier == SMPMob.RenderTier.FULL
                ? SMPMobEngineSettings.NAMETAG_UPDATE_INTERVAL_TICKS
                : SMPMobEngineSettings.FAR_NAMETAG_UPDATE_INTERVAL_TICKS;
        if (anyNametag && mob.shouldUpdateNameplate(tick, nameInterval) && mob.updateNameplate(false, tick)) {
            nameplateUpdates++;
        }

        applyRenderTier(entity, tier);
        return activation;
    }

    public boolean tryHitParticles(Player player, int cost) {
        if (player == null || cost <= 0) return false;
        int budget = SMPMobEngineSettings.HIT_PARTICLES_PER_PLAYER_PER_SECOND;
        UUID uuid = player.getUniqueId();
        int used = hitParticleBudget.getOrDefault(uuid, 0);
        if (used + cost > budget) return false;
        hitParticleBudget.put(uuid, used + cost);
        particlesSent += cost;
        return true;
    }

    public void finishTick(int spawnQueueSize) {
        Map<UUID, Integer> visibleByPlayer = new HashMap<>(visibleNormalByPlayer);
        visibleImportantByPlayer.forEach((uuid, count) -> visibleByPlayer.merge(uuid, count, Integer::sum));
        profiler.recordClientLoad(visibleTotal, nameplateUpdates, particlesSent, sleeping, fullRender,
                reducedRender, hidden, spawnQueueSize, visibleByPlayer);
    }

    private boolean reserveVisibleSlot(Player player, boolean important, double distanceSquared) {
        if (distanceSquared > square(SMPMobEngineSettings.HIDDEN_VISIBILITY_RANGE)) return false;
        UUID uuid = player.getUniqueId();
        Map<UUID, Integer> map = important ? visibleImportantByPlayer : visibleNormalByPlayer;
        int max = important ? SMPMobEngineSettings.MAX_VISIBLE_IMPORTANT_MOBS_PER_PLAYER
                : SMPMobEngineSettings.MAX_VISIBLE_NORMAL_MOBS_PER_PLAYER;
        int current = map.getOrDefault(uuid, 0);
        if (current >= max) return distanceSquared <= square(8.0D);
        map.put(uuid, current + 1);
        return true;
    }

    private void applyRenderTier(LivingEntity entity, SMPMob.RenderTier tier) {
        entity.setGlowing(false);
        entity.setVisualFire(false);
        entity.setArrowsInBody(0);
        entity.setBeeStingersInBody(0);
        if (tier != SMPMob.RenderTier.BASIC) return;
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;
        clearEquipment(equipment.getHelmet(), item -> equipment.setHelmet(item));
        clearEquipment(equipment.getChestplate(), item -> equipment.setChestplate(item));
        clearEquipment(equipment.getLeggings(), item -> equipment.setLeggings(item));
        clearEquipment(equipment.getBoots(), item -> equipment.setBoots(item));
        clearEquipment(equipment.getItemInMainHand(), item -> equipment.setItemInMainHand(item));
        clearEquipment(equipment.getItemInOffHand(), item -> equipment.setItemInOffHand(item));
    }

    private void clearEquipment(ItemStack current, java.util.function.Consumer<ItemStack> setter) {
        if (current != null && !current.getType().isAir()) setter.accept(null);
    }

    private double square(double value) {
        return value * value;
    }
}
