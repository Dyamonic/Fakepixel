package club.abelix.api.server.game.queue;

import club.abelix.api.server.game.Game;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.network.common.enums.ArenaQueueState;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.play.client.CArenaEditQueueState;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>GameQueue creation on 18.08.2022 at 01:22</p>
 *
 * <p>Система управления очередью
 * Когда активна - арена ждет новых игроков на сервере
 * Когда не активна - арена больше не ждет новых игроков
 * </p>
 */

@RequiredArgsConstructor
public class GameQueue {

    private final Game game;

    @Getter
    private GameQueueState state = GameQueueState.INACTIVE;

    /**
     * Изменить состояние очереди
     *
     * @param state состояние очереди
     */
    public void editState(@NonNull GameQueueState state) {
        switch (state) {
            case ACTIVE -> activate();
            case INACTIVE -> deactivate();
        }
    }

    /**
     * Перевести очередь в состояние активности,
     * те арена будет ждать новых игроков
     */
    public void activate() {
        if (state == GameQueueState.ACTIVE) {
            Logging.IMP.warn("[{}] Queue: Fail edit state to ACTIVE", game.getName());
            return;
        }

        state = GameQueueState.ACTIVE;

        HeartBeat.runLaterSync(() -> {
            if (Connection.isActive()) {
                Connection.sendPacket(new CArenaEditQueueState(game.getName(), game.getMini(), ArenaQueueState.START_WAIT));
            }
        }, 20L * 5);
    }

    /**
     * Перевести очередь в состояние не активности,
     * те арена больше не будет принимать новых игроков
     */
    public void deactivate() {
        if (Connection.isActive() && state != GameQueueState.INACTIVE) {
            Connection.sendPacket(new CArenaEditQueueState(game.getName(), game.getMini(), ArenaQueueState.STOP_WAIT));
            state = GameQueueState.INACTIVE;

            Logging.IMP.info("[{}] Queue: Edit state to INACTIVE. Stop waiting players!", game.getName());
            return;
        }

        Logging.IMP.warn("[{}] Queue: Fail edit state to INACTIVE", game.getName());
    }

    /**
     * Используется при реконнекте
     */
    public void synchronization() {
        if (state == null) return;

        switch (state) {
            case ACTIVE -> HeartBeat.runLaterSync(() -> Connection.sendPacket(
                    new CArenaEditQueueState(game.getName(), game.getMini(), ArenaQueueState.START_WAIT)
            ), 20L * 5);
            case INACTIVE -> Connection.sendPacket(
                    new CArenaEditQueueState(game.getName(), game.getMini(), ArenaQueueState.STOP_WAIT)
            );
        }
    }

}
