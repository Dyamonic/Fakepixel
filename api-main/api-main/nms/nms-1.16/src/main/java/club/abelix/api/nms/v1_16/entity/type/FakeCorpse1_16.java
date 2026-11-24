package club.abelix.api.nms.v1_16.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.player.FakeEntityCorpse;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.exception.SkinException;
import com.comphenix.protocol.events.PacketContainer;
import lombok.NonNull;
import org.bukkit.Location;

import java.util.Collection;

public class FakeCorpse1_16 extends FakePlayer1_16 implements FakeEntityCorpse {

    public FakeCorpse1_16(@NonNull NmsApi nmsApi, @NonNull Skin skin, @NonNull Location location) {
        super(nmsApi, FakeEntityPlayer.generateRandomName(), skin, location);
    }

    public FakeCorpse1_16(NmsApi nmsApi, String skin, Location location) throws SkinException {
        super(nmsApi, FakeEntityPlayer.generateRandomName(), skin, location);
    }

    @Override
    protected synchronized void addSpawnEntityPackets(PacketLibraryApi packetApi, Collection<PacketContainer> packets) {
        super.addSpawnEntityPackets(packetApi, packets);

        packets.add(packetApi.getEntityTeleportPacket(getEntityId(), getLocation()));
    }

}
