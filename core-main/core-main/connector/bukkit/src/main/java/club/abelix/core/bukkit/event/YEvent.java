package club.abelix.core.bukkit.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class YEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public YEvent(boolean async) {
        super(async);
    }

    public YEvent() {
        super();
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
