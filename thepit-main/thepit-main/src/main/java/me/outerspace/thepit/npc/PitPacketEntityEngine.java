package me.outerspace.thepit.npc;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class PitPacketEntityEngine {
    private static final PitPacketEntityEngine SHARED = new PitPacketEntityEngine();
    private final AtomicInteger ids = new AtomicInteger(1500000000);
    private final AtomicLong generations = new AtomicLong();
    private final Set<Integer> activeIds = ConcurrentHashMap.newKeySet();
    private volatile String lastFailure = "none";

    private PitPacketEntityEngine() {}

    public static PitPacketEntityEngine shared() { return SHARED; }

    public RuntimeId allocate() {
        int id;
        do {
            id = ids.incrementAndGet();
            if (id < 1000000000) throw new IllegalStateException("Virtual entity ID space exhausted");
        } while (!activeIds.add(id));
        return new RuntimeId(id, generations.incrementAndGet());
    }

    public void release(int id) { activeIds.remove(id); }
    public int activeIdCount() { return activeIds.size(); }
    public boolean active(int id) { return activeIds.contains(id); }
    public String lastFailure() { return lastFailure; }
    public void failure(Throwable failure) { lastFailure = failure == null ? "none" : failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage()); }
    public void clear() { activeIds.clear(); }

    public record RuntimeId(int entityId, long generation) {}
}
