package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SMapBalancerUpdateResponse;
import club.abelix.network.type.map.GameMap;
import club.abelix.network.type.mode.Mini;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>CMapBalancerGetBestMap creation on 26.12.2023 at 00:08</p>
 */

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CMapBalancerUpdateMap extends Packet.Request<PlayClientProcessor, SMapBalancerUpdateResponse> {
    // Тк у request пакетов может быть, а может и не быть id, как и у previousMap, карты может быть или нет,
    // приходится добавлять пустую карту, чтобы не ломалась система коллбэков
    private static final GameMap EMPTY_MAP = new GameMap("empty", "empty", 0);

    private Mini mini;
    private GameMap previousMap;

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
        previousMap = new GameMap().deserialize(buf);

        if (previousMap.equals(EMPTY_MAP)) {
            previousMap = null;
        }
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeEnum(buf, mini, true);

        if (previousMap == null) {
            previousMap = EMPTY_MAP;
        }

        previousMap.serialize(buf);
    }

    @Override
    public Class<SMapBalancerUpdateResponse> getResponse() {
        return SMapBalancerUpdateResponse.class;
    }
}