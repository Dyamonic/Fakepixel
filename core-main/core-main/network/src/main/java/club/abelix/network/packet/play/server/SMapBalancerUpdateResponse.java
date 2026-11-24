package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.type.map.GameMap;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>SMapBalancerUpdateResponse creation on 26.12.2023 at 00:37</p>
 */

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SMapBalancerUpdateResponse extends Packet.Response<PlayServerProcessor> {

    private GameMap newMap;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        newMap = new GameMap().deserialize(buf);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        newMap.serialize(buf);
    }

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

}
