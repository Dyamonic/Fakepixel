package club.abelix.core.app.data.party;

import club.abelix.commons.util.collection.CollectionUtil;
import club.abelix.core.app.Async;
import club.abelix.core.app.connection.server.AbstractServer;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.core.app.data.party.manager.PartyManager;
import club.abelix.network.packet.Packet;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class Party {

    private static final AtomicInteger PARTY_ID_COUNTER = new AtomicInteger(0);

    protected final int id;
    protected final TIntSet kicked = new TIntHashSet();
    protected final Set<Gamer> gamers = new HashSet<>();
    protected final Map<Integer, OfflineMember> offlineMembers = new HashMap<>();
    protected final Set<Gamer> invites = CollectionUtil.createConcurrentSet();
    @Setter
    protected boolean valid;
    @Setter
    protected boolean publicStatus;
    @Setter
    protected boolean privateGame;
    @Setter
    protected int publicSlots;
    @Setter
    protected Gamer leader;

    public Party(@NonNull Gamer leader) {
        this.id = PARTY_ID_COUNTER.incrementAndGet();

        this.leader = leader;

        synchronized (gamers) {
            gamers.add(leader);
        }

        PartyManager.INSTANCE.registerParty(this);
    }

    public Collection<Gamer> getGamers() {
        synchronized (gamers) {
            return Collections.unmodifiableSet(new HashSet<>(gamers));
        }
    }

    public Collection<OfflineMember> getOfflineMembers() {
        synchronized (offlineMembers) {
            return Collections.unmodifiableSet(new HashSet<>(offlineMembers.values()));
        }
    }

    public int[] getPlayerIds() {
        synchronized (gamers) {
            return gamers.stream()
                    .mapToInt(Gamer::getPlayerId)
                    .toArray();
        }
    }

    public void addMember(@NonNull Gamer gamer) {
        synchronized (gamers) {
            gamers.add(gamer);
        }

        kicked.remove(gamer.getPlayerId());
    }

    public void removeMember(@NonNull Gamer gamer) {
        synchronized (gamers) {
            gamers.remove(gamer);
        }
    }

    public void kickMember(@NonNull Gamer gamer) {
        kicked.add(gamer.getPlayerId());

        removeMember(gamer);
    }

    public boolean isKickedBefore(@NonNull Gamer gamer) {
        return kicked.contains(gamer.getPlayerId());
    }

    public OfflineMember makeOffline(@NonNull Gamer gamer) {
        OfflineMember member = new OfflineMember(gamer, this);

        synchronized (offlineMembers) {
            offlineMembers.put(gamer.getPlayerId(), member);
        }

        removeMember(gamer);

        return member;
    }

    public void removeOffline(@NonNull Gamer gamer) {
        synchronized (offlineMembers) {
            OfflineMember member = offlineMembers.remove(gamer.getPlayerId());

            if (member != null) {
                member.cancel();
            }
        }
    }

    public boolean isMembersLimit() {
        int onlineMembers = gamers.size();
        int offlinePlayers = offlineMembers.size();

        if (leader == null) {
            return false;
        }

        int partyLimit;

        if (publicStatus) {
            partyLimit = publicSlots;
        } else {
            partyLimit = PartyManager.INSTANCE.getPartyLimit(leader.getRanks());
        }

        // онлайн и оффлайн игроки, -1 лидер
        return (onlineMembers + offlinePlayers) - 1 >= partyLimit;
    }

    public void rejoin(@NonNull Gamer gamer) {
        removeOffline(gamer);
        addMember(gamer);

        // replace gamer instance
        if (isLeader(gamer)) {
            this.leader = gamer;
        }
    }

    public int getSize() {
        synchronized (gamers) {
            return gamers.size() + offlineMembers.size();
        }
    }

    public boolean isLeader(@NonNull Gamer gamer) {
        return gamer.equals(leader);
    }

    public boolean contains(@NonNull Gamer gamer) {
        return getGamers().contains(gamer) || offlineMembers.containsKey(gamer.getPlayerId());
    }

    public Set<Gamer> getInvites() {
        return Collections.unmodifiableSet(this.invites);
    }

    public void invite(@NonNull Gamer gamer) {
        invites.add(gamer);
    }

    public void uninvite(@NonNull Gamer gamer) {
        invites.remove(gamer);
    }

    protected Set<AbstractServer> getServers() {
        Set<AbstractServer> servers = new HashSet<>();

        for (Gamer gamer : gamers) {
            AbstractServer server = gamer.getServer();

            if (server == null) continue;

            servers.add(server);
        }

        return servers;
    }

    public void sendToServers(@NonNull Packet<?> packet) {
        getServers().forEach(server -> server.sendPacket(packet));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Party party)) return false;

        return id == party.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public static class OfflineMember {

        @Getter
        private final Gamer gamer;
        private final Party party;

        private ScheduledFuture<?> kickTask;

        public OfflineMember(@NonNull Gamer gamer, @NonNull Party party) {
            this.gamer = gamer;
            this.party = party;
        }

        public void runTask() {
            kickTask = Async.runLater(() -> {
                for (Gamer member : party.getGamers()) {
                    member.sendLocalizedMessage("party_member_kicked_too_long_offline",
                            "%name%", gamer.getDisplayName());
                }

                PartyManager.INSTANCE.removeMember(party, gamer, false);
            }, 5, TimeUnit.MINUTES);
        }

        public void cancel() {
            if (kickTask != null) {
                kickTask.cancel(true);
                kickTask = null;
            }
        }

    }
}
