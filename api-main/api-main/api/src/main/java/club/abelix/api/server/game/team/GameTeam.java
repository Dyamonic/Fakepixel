package club.abelix.api.server.game.team;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.manager.TeamManager;
import club.abelix.api.server.game.team.color.TeamColor;
import club.abelix.commons.util.array.ObjectLinkedBuf;
import club.abelix.commons.util.collection.CollectionUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GameTeam {

    @Getter
    protected final int index;

    @Getter
    protected final char character;

    // Команда игроков

    protected final Game game;
    private final Set<Gamer> players = CollectionUtil.createConcurrentWeakSet();
    private final Set<Gamer> alive = CollectionUtil.createConcurrentWeakSet();
    private boolean valid = true;

    public GameTeam(Game game) {
        this.game = game;
        this.index = game.getTeamManager().nextIndex();
        this.character = (char) ('A' + index);
    }

    public void add(@NonNull Gamer gamer) {
        TeamManager teamManager = game.getTeamManager();

        checkValid(teamManager);

        if (teamManager.getTeam(gamer) != null) {
            throw new IllegalArgumentException(gamer + " already has assigned team");
        }

        teamManager.setTeam(gamer, this);

        this.players.add(gamer);

        if (this.players.size() == 1) {
            // добавляем только непустые тимы
            teamManager.addTeam(this);

            game.log.info("[{}] Add team", this);
        }

        addAlivePlayer(gamer);
    }

    public void addUnsafe(@NonNull Gamer gamer) {
        TeamManager teamManager = game.getTeamManager();

        teamManager.setTeam(gamer, this);

        this.players.add(gamer);

        if (this.players.size() == 1) {
            // добавляем только непустые тимы
            teamManager.addTeam(this);

            game.log.info("[{}] Add team", this);
        }

        addAlivePlayer(gamer);
    }

    public void remove(@NonNull Gamer gamer) {
        TeamManager teamManager = game.getTeamManager();
//        checkValid(teamManager);

        players.remove(gamer);
        alive.remove(gamer);

        teamManager.setTeam(gamer, null);
    }

    private void checkValid(@NonNull TeamManager teamManager) {
        if (teamManager.isInitialized()) {
            throw new IllegalStateException("GameTeam creation is already finished. Failed to add gamer to the team.");
        }

        if (!this.valid) {
            throw new IllegalStateException("GameTeam was unregistered");
        }
    }

    public Set<Gamer> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    public boolean contains(@NonNull Player player) {
        return game.getTeamManager().getTeam(player) == this;
    }

    public boolean contains(@NonNull Gamer gamer) {
        return game.getTeamManager().getTeam(gamer) == this;
    }

    public int size() {
        return players.size();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    /**
     * Вызывается, когда TeamManager ресетится
     */
    public void unregister() {
        this.valid = false;
    }

    public void addAlivePlayer(@NonNull Player player) {
        Gamer gamer = Gamer.getGamer(player);

        addAlivePlayer(gamer);
    }

    public void addAlivePlayer(@NonNull Gamer gamer) {
        if (alive.add(gamer)) {
            game.log.info("[{}] Add player: {}", this, gamer.getName());
        }
    }

    public void removePlayer(@NonNull Player player) {
        Gamer gamer = Gamer.getGamer(player);

        removePlayer(gamer);
    }

    public void removePlayer(@NonNull Gamer gamer) {
        if (alive.remove(gamer)) {
            game.log.info("[{}] Remove player: {}", this, gamer.getName());
        }
    }

    public Gamer firstAlive() {
        return alive.isEmpty() ? null : alive.iterator().next();
    }

    public int aliveCount() {
        return alive.size();
    }

    public boolean isAlive() {
        return !alive.isEmpty();
    }

    public boolean isAlive(@NonNull Gamer gamer) {
        return alive.contains(gamer);
    }

    public boolean hasTeamMates() {
        return this.alive.size() > 1;
    }

    public boolean hasTeamMates(@NonNull Gamer gamer) {
        return alive.contains(gamer) && alive.size() > 1;
    }

    public void broadcast(@NonNull String message) {
        getAliveGamers().forEach(gamer -> gamer.sendMessage(message));
    }

    public void broadcastLocalized(String key, Function<Gamer, ObjectLinkedBuf> objectSupplier) {
        getAliveGamers().forEach(gamer -> gamer.sendLocalizedMessage(key, objectSupplier.apply(gamer).toArray()));
    }

    public void broadcastLocalized(String key, Object... objects) {
        getAliveGamers().forEach(gamer -> gamer.sendLocalizedMessage(key, objects));
    }

    public Set<Gamer> getAliveGamers() {
        return Collections.unmodifiableSet(alive);
    }

    public List<Player> getAlivePlayers() {
        return getAliveGamers().stream().map(Gamer::getPlayer).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void rejoin(@NonNull Gamer gamer) {
        if (players.remove(gamer)) {
            players.add(gamer);

            game.log.info("[{}] Replace gamer reference: {}", this, gamer.getName());
        }
    }

    public TeamColor getColor() {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + players;
    }

}