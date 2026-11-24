package club.abelix.network.packet.play.server;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import club.abelix.network.type.telegram.SecurityUser;
import io.netty.buffer.ByteBuf;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class STelegramAuth extends Packet.Response<PlayServerProcessor> {

    private SecurityUser securityUser;
    private boolean confirmed;

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
        this.securityUser = PacketUtils.readSerializableArray(buf, SecurityUser[]::new, SecurityUser::new)[0];
        this.confirmed = buf.readBoolean();
    }

    @Override
    public void write0(@NonNull ByteBuf buf) throws Exception {
        PacketUtils.writeSerializableArray(buf, new SecurityUser[]{this.securityUser});
        buf.writeBoolean(confirmed);
    }
}
