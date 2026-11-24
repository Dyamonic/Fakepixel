package club.abelix.network.common.enums;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>ArenaQueueState creation on 13.09.2022 at 11:50</p>
 */

public enum ArenaQueueState {
    START_WAIT, // Ожидает игроков
    STOP_WAIT; // Перестать ожидать игроков

    public static ArenaQueueState getById(int id) {
        if (id < 0 || id >= values().length) {
            throw new IllegalStateException("Invalid id");
        }

        return values()[id];
    }
}
