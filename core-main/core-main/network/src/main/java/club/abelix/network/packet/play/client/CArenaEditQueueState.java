package club.abelix.network.packet.play.client;

import club.abelix.network.common.enums.ArenaQueueState;
import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.type.mode.Mini;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>CArenaEditQueueState creation on 13.09.2022 at 12:06</p>
 */

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CArenaEditQueueState extends Packet<PlayClientProcessor> {
    private String arenaName;
    private Mini mini;

    private ArenaQueueState state;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayClientProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) {
        arenaName = PacketUtils.readString(buf);
        mini = PacketUtils.readEnum(buf, Mini.class, true);
        state = PacketUtils.readEnum(buf, ArenaQueueState.class, true);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) {
        PacketUtils.writeString(buf, arenaName);
        PacketUtils.writeEnum(buf, mini, true);
        PacketUtils.writeEnum(buf, state, true);
    }

    @Override
    public void process(@NotNull PlayClientProcessor processor, @NotNull ChannelProcessorContext ctx) {
        processor.process(this, ctx);
    }
}
