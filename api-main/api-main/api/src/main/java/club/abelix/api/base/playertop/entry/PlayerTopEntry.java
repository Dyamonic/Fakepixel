package club.abelix.api.base.playertop.entry;

import club.abelix.api.base.gamer.Gamer;

public class PlayerTopEntry {

    private final Gamer gamer;
    private final int value;

    public PlayerTopEntry(Gamer gamer, int value) {
        this.gamer = gamer;
        this.value = value;
    }

    public Gamer getGamer() {
        return gamer;
    }

    public int getValue() {
        return value;
    }
}
