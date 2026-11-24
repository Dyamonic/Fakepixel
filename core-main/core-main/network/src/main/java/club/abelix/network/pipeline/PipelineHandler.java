package club.abelix.network.pipeline;

import club.abelix.network.common.logging.Logging;
import club.abelix.network.packet.BossProcessor;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class PipelineHandler extends SimpleChannelInboundHandler<Packet<?>> {

    private final BossProcessor processor;

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) throws Exception {
        processor.callActive();
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        processor.callInactive();
    }

    @Override
    public void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull Packet packet) {
        ChannelProcessorContext context = new ChannelProcessorContext(packet, processor, ctx);

        try {
            if (processor == null) {
                throw new RuntimeException("Boss processor is null! " + packet);
            }

            processor.callProcessor(context);
        } catch (Exception any) {
            Logging.IMP.error("Error occurred whilst processing packet: " + packet);
            any.printStackTrace();
        } finally {
            context.post();
        }
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable cause) throws Exception {
        processor.callException(cause);
    }

}