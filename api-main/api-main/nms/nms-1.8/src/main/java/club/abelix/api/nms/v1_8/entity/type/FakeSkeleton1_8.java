package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.skeleton.FakeEntitySkeleton;
import club.abelix.api.nms.packet.PacketLibraryApi;
import com.comphenix.protocol.events.PacketContainer;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;

import java.util.Collection;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>FakeSkeleton1_8 creation on 11.05.2024 at 14:23</p>
 */

public class FakeSkeleton1_8 extends FakeLivingEntity1_8 implements FakeEntitySkeleton {

    public FakeSkeleton1_8(NmsApi api, Location location) {
        super(api, EntityType.SKELETON, location);
    }

    @Override
    protected void addSpawnEntityPackets(@NonNull PacketLibraryApi packetApi, Collection<PacketContainer> packets) {
        super.addSpawnEntityPackets(packetApi, packets);

        packets.add(packetApi.getEntityLookPacket(getEntityId(), getLocation()));
        packets.add(packetApi.getHeadRotationPacket(getEntityId(), getLocation().getYaw()));
    }

    @Override
    public void setType(Skeleton.SkeletonType type) {
        getDataWatcher().setObject(13, (byte) type.ordinal());
        broadcastDataWatcherPacket();
    }

}
