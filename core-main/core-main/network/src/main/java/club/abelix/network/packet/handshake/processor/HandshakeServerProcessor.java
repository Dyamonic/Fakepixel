package club.abelix.network.packet.handshake.processor;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.handshake.packet.HResponse;

/**
 * Обрабатывает пакеты ОТ сервера
 *
 * @author WhileInside
 */
public interface HandshakeServerProcessor extends HandshakeProcessor {

    default void process(HResponse packet, ChannelProcessorContext ctx) {
        // override me
    }

}
