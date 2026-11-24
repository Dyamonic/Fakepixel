package club.abelix.network.packet.play.server;

import club.abelix.network.common.locale.UserLocale;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayServerProcessor;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SChangePlayerNickResponse extends Packet.Response<PlayServerProcessor> {

    private ChangeNickStatus status;

    private int nickId;
    private String name;

    public SChangePlayerNickResponse(@NonNull ChangeNickStatus status) {
        this(status, 0, null);
    }

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        status = PacketUtils.readEnum(buf, ChangeNickStatus.class);

        if (status == ChangeNickStatus.SUCCESS) {
            nickId = PacketUtils.readVarInt(buf);
            name = PacketUtils.readString(buf);
        }
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeEnum(buf, status);

        if (status == ChangeNickStatus.SUCCESS) {
            PacketUtils.writeVarInt(buf, nickId);
            PacketUtils.writeString(buf, name);
        }
    }

    public enum ChangeNickStatus {

        USER_NOT_FOUND,
        ALREADY_USED,
        ALREADY_REGISTERED,
        SUCCESS;

        public String getErrorText(@NonNull UserLocale language) {
            return this == SUCCESS ? null : language.getMessage("nick_error_" + name());
        }

    }

}
