package club.abelix.network.packet.play.client;

import club.abelix.network.common.NetworkPlayerLoad;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SNetworkPlayerLoadResponse;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>CNetworkPlayerLoadRequest creation on 18.09.2022 at 00:46</p>
 */

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CNetworkPlayerLoadRequest extends Packet.Request<PlayClientProcessor, SNetworkPlayerLoadResponse> {

    private int[] playerIds;
    private int mask;

    public CNetworkPlayerLoadRequest(int playerId, int mask) {
        this(new int[]{playerId}, mask);
    }

    public boolean isLoadParty() {
        return (mask & NetworkPlayerLoad.LOAD_PARTY) == NetworkPlayerLoad.LOAD_PARTY;
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        playerIds = PacketUtils.readVarIntArray(buf);
        mask = buf.readUnsignedByte();
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarIntArray(buf, playerIds);
        buf.writeByte(mask);
    }

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public Class<SNetworkPlayerLoadResponse> getResponse() {
        return SNetworkPlayerLoadResponse.class;
    }
}
