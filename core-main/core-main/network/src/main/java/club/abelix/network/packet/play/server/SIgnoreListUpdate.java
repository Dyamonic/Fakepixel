package club.abelix.network.packet.play.server;

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
import org.jetbrains.annotations.NotNull;

/**
 * смешнее пакета вы ТОЧНО не найдете :D:D
 *
 * @author tomskiy
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SIgnoreListUpdate extends Packet<PlayServerProcessor> {

    private int processorGamerId;
    private int ignoredGamerId;

    private IgnoreListAction action;

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
        processorGamerId = buf.readInt();
        ignoredGamerId = buf.readInt();
        action = PacketUtils.readEnum(buf, IgnoreListAction.class);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        buf.writeInt(processorGamerId);
        buf.writeInt(ignoredGamerId);
        PacketUtils.writeEnum(buf, action);
    }

    public enum IgnoreListAction {
        ADD, REMOVE
    }
}
