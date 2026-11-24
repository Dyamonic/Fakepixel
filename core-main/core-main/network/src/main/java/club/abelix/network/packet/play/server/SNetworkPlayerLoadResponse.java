package club.abelix.network.packet.play.server;

import club.abelix.network.common.NetworkPlayerLoad;
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>SNetworkPlayerLoadResponse creation on 18.09.2022 at 00:38</p>
 */

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SNetworkPlayerLoadResponse extends Packet.Response<PlayServerProcessor> {

    private List<NetworkPlayerLoad.PlayerHolder> players;

    @Override
    public boolean isProcessor(PacketProcessor processor) {
        return processor instanceof PlayServerProcessor;
    }

    @Override
    public void read0(@NotNull ByteBuf buf) throws Exception {
        players = PacketUtils.readCollection(buf, ArrayList::new, () -> {
            int mask = buf.readUnsignedByte();
            int playerId = PacketUtils.readVarInt(buf);

            boolean online = false;

            String server = null;
            NetworkPlayerLoad.PartyHolder party = null;

            if ((mask & NetworkPlayerLoad.IS_ONLINE) == NetworkPlayerLoad.IS_ONLINE) {
                online = true;
                server = PacketUtils.readString(buf);

                if ((mask & NetworkPlayerLoad.HAS_PARTY) == NetworkPlayerLoad.HAS_PARTY) {
                    party = new NetworkPlayerLoad.PartyHolder(
                            PacketUtils.readVarInt(buf),
                            PacketUtils.readVarInt(buf) == 1,
                            PacketUtils.readVarInt(buf),
                            PacketUtils.readVarIntArray(buf)
                    );
                }
            }

            return new NetworkPlayerLoad.PlayerHolder(online, playerId, party, server);
        });
    }

    @Override
    public void write0(@NotNull ByteBuf buf) throws Exception {
        PacketUtils.writeCollection(buf, players, player -> {
            buf.writeByte((player.isOnline() ? NetworkPlayerLoad.IS_ONLINE : 0) | (player.getParty() != null ? NetworkPlayerLoad.HAS_PARTY : 0));
            PacketUtils.writeVarInt(buf, player.getPlayerId());

            if (player.isOnline()) {
                PacketUtils.writeString(buf, player.getServer());

                NetworkPlayerLoad.PartyHolder party = player.getParty();

                if (party != null) {
                    PacketUtils.writeVarInt(buf, party.getPartyId());
                    PacketUtils.writeVarInt(buf, party.isPrivate() ? 1 : 0);
                    PacketUtils.writeVarInt(buf, party.getLeader());
                    PacketUtils.writeVarIntArray(buf, party.getMembers());
                }
            }
        });
    }

    @Override
    public void process(@NotNull PlayServerProcessor processor, @NotNull ChannelProcessorContext ctx) throws Exception {
        processor.process(this, ctx);
    }
}
