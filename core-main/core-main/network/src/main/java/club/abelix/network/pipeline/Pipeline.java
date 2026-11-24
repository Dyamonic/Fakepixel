package club.abelix.network.pipeline;

import club.abelix.network.channel.AbstractChannel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import w.util.Env;

@Slf4j
public class Pipeline {

    public static final String FRAMER = "packet-framer";
    public static final String ENCODER = "packet-encoder";
    public static final String DECODER = "packet-decoder";
    public static final String HANDLER = "packet-handler";
    public static final String IN_LOG = "packet-inbound-logger";
    public static final String OUT_LOG = "packet-outbound-logger";

    public static void initPipeline(AbstractChannel channel, SocketChannel socket) {
        ChannelPipeline pipeline = socket.pipeline();

        pipeline.addLast(FRAMER, new PacketFramer());


        pipeline.addLast(ENCODER, new Encoder(channel.getOutboundPacketDirection()));
        pipeline.addLast(DECODER, new Decoder(channel.getInboundPacketDirection()));


        if (Boolean.getBoolean(Env.getString("CORE_PACKET_LOGGING", "false"))) {
            pipeline.addLast(IN_LOG, new PacketInboundLogger());
            pipeline.addLast(OUT_LOG, new PacketOutboundLogger());
        }

        pipeline.addLast(HANDLER, new PipelineHandler(channel.newPacketProcessor(socket)));
    }
}
