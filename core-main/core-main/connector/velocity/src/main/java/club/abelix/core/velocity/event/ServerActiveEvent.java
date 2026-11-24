package club.abelix.core.velocity.event;

import club.abelix.network.packet.BossProcessor;
import com.velocitypowered.api.event.annotation.AwaitingEvent;

@AwaitingEvent
public record ServerActiveEvent(BossProcessor processor) {
}
