package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.object.FakeEntityObject;
import club.abelix.api.nms.packet.PacketLibraryApi;
import club.abelix.api.nms.v1_8.entity.FakeEntityImpl1_8;
import com.comphenix.protocol.events.PacketContainer;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.Collection;

public abstract class FakeObject1_8 extends FakeEntityImpl1_8 implements FakeEntityObject {

    @Getter
    private final Vector velocity = new Vector();

    @Getter
    private final int data;

    public FakeObject1_8(NmsApi nmsApi, EntityType entityType, Location location, int data) {
        super(nmsApi, entityType, location);

        this.data = data;
    }

    @Override
    protected void addSpawnEntityPackets(PacketLibraryApi packetApi, Collection<PacketContainer> packets) {
        packets.add(nmsApi.getPacketLibraryApi()
                .getSpawnEntityPacket(getEntityId(), getSpawnTypeId(), data, getLocation(), velocity));
    }
}
