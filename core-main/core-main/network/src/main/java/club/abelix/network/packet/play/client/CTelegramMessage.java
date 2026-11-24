package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import io.netty.buffer.ByteBuf;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class CTelegramMessage extends Packet<PlayClientProcessor> {

    private int playerId;
    private String message;

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NonNull ByteBuf buf) throws Exception {
        this.playerId = PacketUtils.readVarInt(buf);
        this.message = PacketUtils.readString(buf);
    }

    @Override
    public void write0(@NonNull ByteBuf buf) throws Exception {
        PacketUtils.writeVarInt(buf, this.playerId);
        PacketUtils.writeString(buf, this.message);
    }
}
