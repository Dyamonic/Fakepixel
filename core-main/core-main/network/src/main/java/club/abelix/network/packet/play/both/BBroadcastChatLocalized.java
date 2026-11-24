package club.abelix.network.packet.play.both;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayProcessor;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>BBroadcastChatLocalized creation on 03.03.2023 at 02:23</p>
 */

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Data
public class BBroadcastChatLocalized extends Packet<PlayProcessor> {

    private int focusLanguageId; // Если указано, то игроки только с этим языком получать сообщение
    private String key;
    private String[] objects;

    public BBroadcastChatLocalized(@NonNull String key, String... objects) {
        this.key = key;
        this.objects = objects;
        this.focusLanguageId = 0;
    }

    public BBroadcastChatLocalized(int focusLanguageId, @NonNull String key, String... objects) {
        this.key = key;
        this.objects = objects;
        this.focusLanguageId = focusLanguageId;
    }

    @Override
    public void process(@NonNull PlayProcessor processor, @NonNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayProcessor;
    }

    @Override
    public void read0(@NonNull ByteBuf buf) throws Exception {
        focusLanguageId = buf.readInt();
        key = PacketUtils.readString(buf);

        if (buf.readableBytes() > 0) {
            objects = PacketUtils.readArray(buf, String[]::new, () -> PacketUtils.readString(buf));
        }
    }

    @Override
    public void write0(@NonNull ByteBuf buf) throws Exception {
        buf.writeInt(focusLanguageId);
        PacketUtils.writeString(buf, key);

        if (objects != null) {
            PacketUtils.writeArray(buf, objects, s -> PacketUtils.writeString(buf, s));
        }
    }

}