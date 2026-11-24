package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * Смешной пакет для отправки сообщения в стаффчат
 *
 * @author tomskiy
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CBroadcastStaff extends Packet<PlayClientProcessor> {

    /**
     * Кому видно сообщение {@link GroupWrapper}
     * Отправляем байт, а не энам, ибо оПтИмИзАцИя От вАйЛа
     */
    private byte group;

    /**
     * Сообщение
     */
    private String message;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    protected void read0(@NotNull ByteBuf buf) throws Exception {
        group = buf.readByte();
        message = PacketUtils.readString(buf);
    }

    @Override
    protected void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeByte(group);
        PacketUtils.writeString(buf, message);
    }

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    /**
     * Смешно.
     */
    @Getter
    @RequiredArgsConstructor
    public enum GroupWrapper {

        /**
         * Будет видно саппортерам
         */
        SUPPORTER((byte) 1),

        /**
         * Будет видно хелперам
         */
        HELPER((byte) 2);

        /**
         * оПтИмИзАцИя
         */
        private final byte value;

    }
}
