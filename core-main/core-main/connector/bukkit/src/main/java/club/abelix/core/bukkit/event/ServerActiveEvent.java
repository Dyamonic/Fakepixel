package club.abelix.core.bukkit.event;

import club.abelix.network.packet.BossProcessor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public final class ServerActiveEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final BossProcessor processor;

    public ServerActiveEvent(BossProcessor processor) {
        super(true);

        this.processor = processor;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlerList;
    }

    public BossProcessor getProcessor() {
        return processor;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
