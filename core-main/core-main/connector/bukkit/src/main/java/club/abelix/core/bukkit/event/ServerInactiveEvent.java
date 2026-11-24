package club.abelix.core.bukkit.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ServerInactiveEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    public ServerInactiveEvent() {
        super(true);
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
