package club.abelix.core.bukkit.event;

import club.abelix.network.packet.BossProcessor;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public final class ConnectionEstablishedEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final BossProcessor processor;

    private final boolean reconnect;

    public ConnectionEstablishedEvent(BossProcessor processor) {
        this(processor, false);
    }

    public ConnectionEstablishedEvent(BossProcessor processor, boolean reconnect) {
        super(true);

        this.processor = processor;
        this.reconnect = reconnect;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
