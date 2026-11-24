package club.abelix.network.packet.handshake.processor;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.handshake.packet.HRequest;

/**
 * Обрабатывает пакеты ОТ клиента
 *
 * @author WhileInside
 */
public interface HandshakeClientProcessor extends HandshakeProcessor {

    default void process(HRequest packet, ChannelProcessorContext ctx) {
        // override me
    }

}
