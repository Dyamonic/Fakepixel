package club.abelix.core.app.connection.server;

import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.packet.Packet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(exclude = {"connection", "gamers", "failedPingAttempts", "lock"})
@RequiredArgsConstructor
public abstract class AbstractConnectable {

    protected final RemoteClientConnection connection;

    protected final String name;
    protected final Set<Gamer> gamers = ConcurrentHashMap.newKeySet();

    private final AtomicInteger failedPingAttempts = new AtomicInteger(0);
    @Getter
    private final ReentrantLock lock = new ReentrantLock();

    public void close() {
        connection.close();
    }

    public Set<Gamer> getGamers() {
        return getGamers(true);
    }

    public Set<Gamer> getGamers(final boolean filter) {
        return !filter
                ? gamers
                : Collections.unmodifiableSet(gamers.stream().filter(gamer -> !gamer.isVanished()).collect(Collectors.toSet()));
    }

    public int getOnline() {
        return getOnline(false);
    }

    public int getOnline(boolean filter) {
        return filter ? (int) gamers.stream().filter(gamer -> !gamer.isVanished()).count() : gamers.size();
    }

    public int[] getPlayerIds() {
        int[] playerIds = new int[gamers.size()];

        int n = 0;

        for (Gamer gamer : gamers) {
            if (gamer.isVanished()) {
                continue;
            }

            playerIds[n++] = gamer.getPlayerId();
        }

        return playerIds.length == n + 1 ? playerIds : Arrays.copyOf(playerIds, n);
    }

    public abstract boolean addPlayer(@NonNull Gamer gamer);

    public abstract boolean removePlayer(@NonNull Gamer gamer);

    public void recordPingFailure() {
        failedPingAttempts.incrementAndGet();
    }

    public boolean isUnhealthy() {
        return failedPingAttempts.get() > 5;
    }

    public void sendPacket(@NonNull Packet<?> packet) {
        connection.sendPacket(packet);
    }

    public void withLock(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

}
