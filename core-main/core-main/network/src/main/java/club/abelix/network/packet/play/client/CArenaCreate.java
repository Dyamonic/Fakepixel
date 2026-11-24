package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Mini;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CArenaCreate extends Packet<PlayClientProcessor> {

    private Mini mini;

    private String serverName;
    private GameMap map;
    private int slots;

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        mini = PacketUtils.readEnum(buf, Mini.class, true);
        serverName = PacketUtils.readString(buf);
        slots = PacketUtils.readVarInt(buf);
        map = new GameMap().deserialize(buf);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeEnum(buf, mini, true);
        PacketUtils.writeString(buf, serverName);
        PacketUtils.writeVarInt(buf, slots);
        map.serialize(buf);
    }

}
