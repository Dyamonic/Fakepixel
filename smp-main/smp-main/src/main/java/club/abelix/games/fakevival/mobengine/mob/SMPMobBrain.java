package club.abelix.games.fakevival.mobengine.mob;

import org.bukkit.entity.Player;

public final class SMPMobBrain {
    private static final int TARGET_SCAN_INTERVAL_TICKS = 8;

    private final SMPMobActivationRange activationRange;
    private final SMPMobPathfinder pathfinder;
    private final SMPMobCombatController combatController;
    private final SMPMobVirtualizer virtualizer;

    public SMPMobBrain(
            SMPMobActivationRange activationRange,
            SMPMobPathfinder pathfinder,
            SMPMobCombatController combatController,
            SMPMobVirtualizer virtualizer
    ) {
        this.activationRange = activationRange;
        this.pathfinder = pathfinder;
        this.combatController = combatController;
        this.virtualizer = virtualizer;
    }

    public BrainResult tick(SMPMob mob, long tick, boolean canRunFullAi, boolean canPathfind) {
        return tick(mob, activationRange.nearestActivation(mob), tick, canRunFullAi, canPathfind);
    }

    public BrainResult tick(SMPMob mob, SMPMobActivationRange.Activation activation, long tick, boolean canRunFullAi, boolean canPathfind) {
        SMPMob.State requestedState = activation.state();
        if (mob.isCombatLocked(System.currentTimeMillis()) && activation.player() != null) {
            requestedState = SMPMob.State.FULL_COMBAT;
        }

        if (requestedState == SMPMob.State.FULL_COMBAT && !canRunFullAi) {
            requestedState = SMPMob.State.MEDIUM_AI;
        }

        mob.setState(requestedState);
        if (mob.getRenderTier() == SMPMob.RenderTier.FULL) mob.updateNearbyBossBarViewers();
        else mob.hideBossBar();

        Player target = activation.player();
        switch (requestedState) {
            case SLEEPING -> {
                virtualizer.sleep(mob);
                return new BrainResult(false, false);
            }
            case VIRTUALIZED -> {
                virtualizer.virtualize(mob);
                return new BrainResult(false, false);
            }
            case SLOW_AI -> {
                virtualizer.materialize(mob);
                if (mob.shouldUpdateNameplate(tick, SMPMobEngineSettings.FAR_NAMETAG_UPDATE_INTERVAL_TICKS)) {
                    mob.updateNameplate(false, tick);
                }
                return new BrainResult(false, false);
            }
            case MEDIUM_AI -> {
                virtualizer.materialize(mob);
                if (target != null && tick - mob.getLastTargetScanTick() >= TARGET_SCAN_INTERVAL_TICKS) {
                    combatController.acquireTarget(mob, target);
                    mob.setLastTargetScanTick(tick);
                }
                if (mob.shouldUpdateNameplate(tick, SMPMobEngineSettings.FAR_NAMETAG_UPDATE_INTERVAL_TICKS)) {
                    mob.updateNameplate(false, tick);
                }
                return new BrainResult(false, false);
            }
            case FULL_COMBAT -> {
                virtualizer.materialize(mob);
                if (target == null) {
                    return new BrainResult(true, false);
                }
                combatController.acquireTarget(mob, target);
                combatController.attackIfReady(mob, target, tick);
                boolean pathfindUsed = canPathfind && pathfinder.moveToward(mob, target, tick);
                return new BrainResult(true, pathfindUsed);
            }
            default -> throw new IllegalStateException("Unhandled SMP mob state: " + requestedState);
        }
    }

    public record BrainResult(boolean fullAiUsed, boolean pathfindUsed) {
    }
}
