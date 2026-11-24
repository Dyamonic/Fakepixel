package club.abelix.api.server.game.loot.event;

import club.abelix.core.bukkit.event.YEvent;
import org.bukkit.block.Chest;

public class ChestFillEvent extends YEvent {

    private final Chest chest;
    private final Reason reason;

    public ChestFillEvent(Chest chest, Reason reason) {
        this.chest = chest;
        this.reason = reason;
    }

    public Chest getChest() {
        return chest;
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        OPEN, BREAK, EXPLODE
    }

}
