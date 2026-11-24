package club.abelix.api.server.game.queue;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>GameQueueState creation on 18.08.2022 at 01:31</p>
 */

public enum GameQueueState {
    ACTIVE, // Ждем новых игроков
    INACTIVE // Больше не принимаем игроков
}
