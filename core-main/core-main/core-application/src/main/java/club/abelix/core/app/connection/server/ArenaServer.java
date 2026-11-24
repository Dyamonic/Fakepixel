package club.abelix.core.app.connection.server;

import club.abelix.core.app.connection.RemoteClientConnection;
import club.abelix.core.app.connection.server.reservation.ArenaReservation;
import club.abelix.core.app.data.gamer.Gamer;
import club.abelix.network.common.enums.GameState;
import club.abelix.network.packet.play.server.SDisableGameStatistic;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.ServerMode;
import club.abelix.network.type.mode.ServerType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
@EqualsAndHashCode(callSuper = true, exclude = {"map", "state", "reservations", "privateGame"})
public class ArenaServer extends MiniServer {

    protected final Mini mini;
    protected GameMap map;

    protected volatile GameState state = GameState.STAND_BY;

    protected List<ArenaReservation> reservations;

    protected volatile boolean privateGame;

    public void setMap(GameMap map) {
        withLock(() -> this.map = map);
    }

    public void setPrivateGame(boolean privateGame) {
        withLock(() -> this.privateGame = privateGame);
    }

    public ArenaServer(MasterServer master, RemoteClientConnection connection,
                       ServerType type, Mini mini, GameMap map) {
        super(master, connection, type);

        this.mini = mini;
        this.map = map;

        this.reservations = Collections.synchronizedList(new ArrayList<>());
        this.privateGame = false;
    }

    @Override
    public ServerMode getMode() {
        return mini;
    }

    public String getFullModeName() {
        return mini.getDisplayName();
    }

    public String getMapName() {
        return map.getDisplayName();
    }

    public void setState(GameState state) {
        withLock(() -> this.state = state);
    }

    public void disableStatistic(List<Gamer> gamers) {
        disableStatistic(
                gamers.stream().mapToInt(Gamer::getPlayerId).toArray()
        );
    }

    public void disableStatistic(Gamer... gamers) {
        disableStatistic(
                Arrays.stream(gamers).mapToInt(Gamer::getPlayerId).toArray()
        );
    }

    public void disableStatistic(int[] players) {
        getMasterServer().getConnection().sendPacket(
                new SDisableGameStatistic(getName(), players)
        );
    }

    public void newReservation(@NonNull ArenaReservation reservation) {
        withLock(() -> reservations.add(reservation));
    }

    public void newReservation(int slots) {
        val reservation = new ArenaReservation(LocalDateTime.now());
        withLock(() -> {
            for (int i = 0; i < slots; i++) {
                reservations.add(reservation);
            }
        });
    }

    public void unReservation(int slot) {
        withLock(() -> {
            actualizeReservation();

            for (int i = 0; i < slot; i++) {
                if (this.reservations.isEmpty()) {
                    break;
                }

                this.reservations.remove(0);
            }
        });
    }

    public int getReservations() {
        getLock().lock();
        try {
            return _getReservations();
        } finally {
            getLock().unlock();
        }
    }

    private int _getReservations() {
        actualizeReservation();
        int reservationSlots = 0;

        for (val reservation : this.reservations) {
            if (reservation.isExpired()) {
                continue;
            }

            reservationSlots += 1;
        }

        return reservationSlots;
    }

    @Override
    public boolean canJoin() {
        return getMasterServer().canJoin();
    }

    @Override
    public int getSpace() {
        getLock().lock();
        try {
            if (privateGame) {
                return 0;
            }

            int space = super.getSpace();
            int afterReservation = space - _getReservations();

            return Math.max(afterReservation, 0);
        } finally {
            getLock().unlock();
        }
    }

    private void actualizeReservation() {
        this.reservations.removeIf(ArenaReservation::isExpired);
    }
}
