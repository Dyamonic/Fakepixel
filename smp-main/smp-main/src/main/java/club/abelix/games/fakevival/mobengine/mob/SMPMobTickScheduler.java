package club.abelix.games.fakevival.mobengine.mob;

import club.abelix.games.fakevival.mobengine.profile.SMPMobPerformanceProfiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SMPMobTickScheduler {
    private static final int MAX_FULL_AI_MOBS_PER_TICK = 120;
    private static final int MAX_PATHFINDS_PER_TICK = 30;
    private static final long MAX_AI_TIME_NANOS = 8_000_000L;
    private static final int ROTATION_BATCH_SIZE = 160;

    private final SMPMobManager manager;
    private final SMPMobBrain brain;
    private final SMPMobPerformanceProfiler profiler;
    private final SMPMobActivationRange activationRange = new SMPMobActivationRange();
    private long tickCounter;
    private int cursor;

    public SMPMobTickScheduler(SMPMobManager manager, SMPMobBrain brain, SMPMobPerformanceProfiler profiler) {
        this.manager = manager;
        this.brain = brain;
        this.profiler = profiler;
    }

    public void tick() {
        long start = System.nanoTime();
        tickCounter++;
        manager.clearInvalid();
        manager.processSpawnQueue();
        manager.engine().clientLoadController().beginTick(tickCounter);

        int fullAiCount = 0;
        int pathfindCount = 0;
        int ticked = 0;

        Collection<SMPMob> snapshot = manager.getMobsSnapshot();
        List<SMPMob> mobs = new ArrayList<>(snapshot);
        int total = mobs.size();
        if (total == 0) {
            profiler.recordTick(0, 0, 0, 0, ROTATION_BATCH_SIZE, System.nanoTime() - start);
            return;
        }

        int limit = Math.min(total, ROTATION_BATCH_SIZE);
        for (int i = 0; i < limit; i++) {
            if (System.nanoTime() - start > MAX_AI_TIME_NANOS) {
                break;
            }

            SMPMob mob = mobs.get((cursor + i) % total);
            SMPMobActivationRange.Activation activation = activationRange.nearestActivation(mob);
            activation = manager.engine().clientLoadController().applyVisibility(mob, activation, tickCounter);
            SMPMobBrain.BrainResult result = brain.tick(
                    mob,
                    activation,
                    tickCounter,
                    fullAiCount < MAX_FULL_AI_MOBS_PER_TICK,
                    pathfindCount < MAX_PATHFINDS_PER_TICK
            );
            if (result.fullAiUsed()) {
                fullAiCount++;
            }
            if (result.pathfindUsed()) {
                pathfindCount++;
            }
            ticked++;
        }

        cursor = (cursor + Math.max(1, ticked)) % total;
        manager.engine().clientLoadController().finishTick(manager.spawnQueueSize());
        profiler.recordTick(total, ticked, fullAiCount, pathfindCount, ROTATION_BATCH_SIZE, System.nanoTime() - start);
    }
}
