package club.abelix.network.pipeline;

import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;

public final class PacketInboundLogger extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        if (msg instanceof Packet) {

            Logging.IMP.debug("[{}] Inbound packet: {}", ctx.channel().remoteAddress(), msg);
        }

        super.channelRead(ctx, msg);
    }
}
