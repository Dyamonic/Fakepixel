package club.abelix.games.fakevival.mobengine.mob;

import org.bukkit.entity.Mob;

public final class SMPMobVirtualizer {
    public void sleep(SMPMob mob) {
        mob.getEntity().setAI(false);
        if (mob.getEntity() instanceof Mob vanillaMob) vanillaMob.setAware(false);
        mob.getEntity().setVelocity(mob.getEntity().getVelocity().multiply(0.0));
        mob.getEntity().setCollidable(false);
        mob.hideBossBar();
    }

    public void virtualize(SMPMob mob) {
        mob.getEntity().setAI(false);
        if (mob.getEntity() instanceof Mob vanillaMob) vanillaMob.setAware(false);
        mob.getEntity().setVelocity(mob.getEntity().getVelocity().multiply(0.0));
        mob.getEntity().setCollidable(false);
        mob.hideBossBar();
    }

    public void materialize(SMPMob mob) {
        mob.getEntity().setAI(true);
        if (mob.getEntity() instanceof Mob vanillaMob) vanillaMob.setAware(true);
        mob.getEntity().setCollidable(true);
    }
}
