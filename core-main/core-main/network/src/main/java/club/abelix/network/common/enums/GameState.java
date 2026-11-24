package club.abelix.network.common.enums;

public enum GameState {

    STAND_BY, STARTING, GAME, ENDING;

    public static GameState getById(int id) {
        if (id < 0 || id >= values().length) {
            throw new IllegalStateException("Invalid id");
        }

        return values()[id];
    }

}
