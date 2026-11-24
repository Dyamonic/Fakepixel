package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.STelegramAuth;
import club.abelix.network.type.telegram.SecurityUser;
import io.netty.buffer.ByteBuf;
import lombok.*;
import org.jetbrains.annotations.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class CTelegramAuth extends Packet.Request<PlayClientProcessor, STelegramAuth> {

    private SecurityUser securityUser;

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
        this.securityUser = PacketUtils.readSerializableArray(buf, SecurityUser[]::new, SecurityUser::new)[0];
    }

    @Override
    public void write0(@NonNull ByteBuf buf) throws Exception {
        PacketUtils.writeSerializableArray(buf, new SecurityUser[]{this.securityUser});
    }

    @Override
    public Class<STelegramAuth> getResponse() {
        return STelegramAuth.class;
    }
}
