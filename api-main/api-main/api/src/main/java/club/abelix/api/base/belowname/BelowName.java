package club.abelix.api.base.belowname;

import club.abelix.api.FalconAPI;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.scoreboard.objective.BoardObjective;
import club.abelix.api.nms.scoreboard.objective.ObjectiveActionMode;
import club.abelix.api.nms.scoreboard.objective.ObjectiveDisplayType;
import club.abelix.api.nms.scoreboard.objective.ObjectiveType;
import club.abelix.api.nms.scoreboard.score.ScoreActionMode;
import club.abelix.commons.util.collection.CollectionUtil;
import com.comphenix.protocol.events.PacketContainer;
import gnu.trove.TCollections;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BelowName {

    private final PacketLibraryApi packetApi = FalconAPI.getNmsApi().getPacketLibraryApi();

    @Getter
    private final BoardObjective objective;

    private final ObjectiveDisplayType displayType;

    private final Set<Player> receivers = CollectionUtil.createConcurrentSet();

    private final TObjectIntMap<Player> players = TCollections.synchronizedMap(
            new TObjectIntHashMap<>(10, 0.5F, -1)
    );

    public BelowName(String displayName) {
        this(displayName, ObjectiveType.INTEGER, ObjectiveDisplayType.BELOW_NAME);
    }

    public BelowName(String displayName, ObjectiveDisplayType type) {
        this(displayName, ObjectiveType.INTEGER, type);
    }

    public BelowName(String name, String displayName, ObjectiveType type, ObjectiveDisplayType displayType) {
        objective = new BoardObjective(name, displayName, type);

        this.displayType = displayType;

        BelowNameManager.IMP.addBelowName(this);
    }

    public BelowName(String displayName, ObjectiveType type, ObjectiveDisplayType displayType) {
        this("BelowNameBoard", displayName, type, displayType);
    }

    public Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(players.keySet());
    }

    public void broadcast(PacketContainer container) {
        broadcast(receivers, container);
    }

    public void broadcast(Set<Player> players, PacketContainer packet) {
        for (Player player : players) {
            packetApi.sendPacket(player, packet);
        }
    }

    public void remove() {
        players.clear();
        broadcast(packetApi.getScoreboardObjectivePacket(objective, ObjectiveActionMode.REMOVE));

        BelowNameManager.IMP.removeBelowName(this);
    }

    public String getDisplayName() {
        return objective.getDisplayName();
    }

    public void setDisplayName(String displayName) {
        objective.setDisplayName(displayName);

        broadcast(packetApi.getScoreboardObjectivePacket(objective, ObjectiveActionMode.UPDATE));
    }

    public void addReceiver(Player player) {
        addReceivers(Collections.singleton(player));
    }

    public void addReceivers(Collection<Player> toAdd) {
        Set<Player> sent = new HashSet<>();

        for (Player player : toAdd) {
            if (receivers.add(player)) {
                sent.add(player);

                BelowName old = BelowNameManager.IMP.setBelowName(player, this);

                if (old != null) {
                    old.removeReceiver(player);
                }
            }
        }

        broadcast(sent, packetApi.getScoreboardObjectivePacket(objective, ObjectiveActionMode.CREATE));
        broadcast(sent, packetApi.getScoreboardDisplayObjectivePacket(objective, displayType));

        players.forEachEntry((key, value) -> {
            sendChangeScore(key.getName(), value, sent);

            return true;
        });
    }

    public void removeReceiver(Player player) {
        removeReceivers(Collections.singleton(player));
    }

    public void removeReceivers(Collection<Player> toRemove) {
        Set<Player> sent = new HashSet<>();

        for (Player player : toRemove) {
            if (receivers.remove(player)) {
                BelowName belowName = BelowNameManager.IMP.getBelowName(player);

                if (belowName == this) {
                    BelowNameManager.IMP.setBelowName(player, null);
                }

                sent.add(player);
            }
        }

        broadcast(sent, packetApi.getScoreboardObjectivePacket(objective, ObjectiveActionMode.REMOVE));
    }

    public void setScore(@NonNull Player player, int score) {
        players.put(player, score);

        sendChangeScore(player.getName(), score, receivers);
    }

    public void removeScore(@NonNull Player player) {
        if (players.remove(player) != -1) {
            sendRemoveLine(player.getName(), receivers);
        }
    }

    private void sendChangeScore(String entry, int score, Set<Player> toSend) {
        broadcast(toSend, packetApi.getScoreboardScorePacket(objective, entry, ScoreActionMode.CHANGE, score));
    }

    private void sendRemoveLine(String entry, Set<Player> toSend) {
        broadcast(toSend, packetApi.getScoreboardScorePacket(objective, entry, ScoreActionMode.REMOVE, 0));
    }
}