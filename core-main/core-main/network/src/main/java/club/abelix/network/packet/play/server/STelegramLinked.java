package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class STelegramLinked extends Packet.Response<PlayServerProcessor> {

    private int playerId;
    private long telegramId;

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    public void read0(@NonNull ByteBuf buf) throws Exception {
        this.playerId = PacketUtils.readVarInt(buf);
        this.telegramId = buf.readLong();
    }

    @Override
    public void write0(@NonNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarInt(buf, this.playerId);
        buf.writeLong(this.telegramId);
    }
}
