package club.abelix.api.base.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @deprecated Используйте YEvent из CoreBukkit
 */
@Deprecated
public abstract class YEvent extends Event {

    private static HandlerList handlers = new HandlerList();

    public YEvent(boolean async) {
        super(async);
    }

    public YEvent() {
        super();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
