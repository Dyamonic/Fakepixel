package club.abelix.games.fakevival.mobengine.mob;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

public final class SMPMobCombatController {
    private static final long ATTACK_INTERVAL_TICKS = 20L;
    private static final double ATTACK_RANGE_SQUARED = 2.8 * 2.8;

    public void acquireTarget(SMPMob mob, Player target) {
        if (mob.getEntity() instanceof Mob vanillaMob) {
            vanillaMob.setTarget(target);
        }
    }

    public void attackIfReady(SMPMob mob, Player target, long tick) {
        if (tick - mob.getLastAttackTick() < ATTACK_INTERVAL_TICKS) {
            return;
        }
        if (!target.getWorld().equals(mob.getEntity().getWorld())) {
            return;
        }
        if (target.getLocation().distanceSquared(mob.getLocation()) > ATTACK_RANGE_SQUARED) {
            return;
        }

        target.damage(mob.getAttackDamage(), mob.getEntity());
        mob.markCombat(System.currentTimeMillis());
        mob.setLastAttackTick(tick);
    }
}
