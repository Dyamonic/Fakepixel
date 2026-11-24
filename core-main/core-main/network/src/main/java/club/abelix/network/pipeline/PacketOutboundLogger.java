package club.abelix.network.pipeline;

import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public final class PacketOutboundLogger extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Packet) {
            Logging.IMP.debug("[{}] Outbound packet: {}", ctx.channel().remoteAddress(), msg);
        }

        super.write(ctx, msg, promise);
    }
}
