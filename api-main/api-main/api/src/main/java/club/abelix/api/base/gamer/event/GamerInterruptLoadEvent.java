package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GamerInterruptLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Gamer gamer;

    public GamerInterruptLoadEvent(Gamer gamer) {
        super(false);

        this.gamer = gamer;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Gamer getGamer() {
        return gamer;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
