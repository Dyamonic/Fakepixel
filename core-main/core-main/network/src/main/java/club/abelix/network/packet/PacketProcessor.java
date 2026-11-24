package club.abelix.network.packet;

import lombok.NonNull;

/**
 * @author WhileInside
 */
public interface PacketProcessor {

    /**
     * Process error
     */
    default void process(@NonNull Throwable throwable) throws Exception {
        throwable.printStackTrace();

        // for implementation
    }

    /**
     * Process packet
     */
    default void process(ChannelProcessorContext ctx) throws Exception {
        ctx.callProcess(this);
    }

    /**
     * Process connect
     */
    default void active() throws Exception {
        // for implementation
    }

    /**
     * Process disconnect
     */
    default void inactive() throws Exception {
        // for implementation
    }


}
