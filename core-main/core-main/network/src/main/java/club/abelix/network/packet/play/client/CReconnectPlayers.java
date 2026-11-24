package club.abelix.network.packet.play.client;

import club.abelix.network.packet.ChannelProcessorContext;
import club.abelix.network.packet.Packet;
import club.abelix.network.packet.PacketProcessor;
import club.abelix.network.packet.PacketUtils;
import club.abelix.network.packet.play.processor.PlayClientProcessor;
import club.abelix.network.packet.play.server.SFailedReconnectPlayers;
import club.abelix.network.type.gamer.ReconnectingGamer;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * <br>Когда кор отключается и включается заново, банджа отправляет этот пакет
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CReconnectPlayers extends Packet.Request<PlayClientProcessor, SFailedReconnectPlayers> {

    private Set<ReconnectingGamer> gamers;

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
        gamers = PacketUtils.readSerializableCollection(buf, HashSet::new, ReconnectingGamer::new);
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeSerializableCollection(buf, gamers);
    }

    @Override
    public Class<SFailedReconnectPlayers> getResponse() {
        return SFailedReconnectPlayers.class;
    }
}
