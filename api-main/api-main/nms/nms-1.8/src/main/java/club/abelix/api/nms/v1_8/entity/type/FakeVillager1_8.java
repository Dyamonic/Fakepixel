package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.villager.FakeEntityVillager;
import club.abelix.api.nms.entity.type.villager.VillagerProfession;
import club.abelix.api.nms.packet.PacketLibraryApi;
import com.comphenix.protocol.events.PacketContainer;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.Collection;

public class FakeVillager1_8 extends FakeLivingEntity1_8 implements FakeEntityVillager {

    @Getter
    private VillagerProfession profession;

    public FakeVillager1_8(NmsApi api, Location location) {
        super(api, EntityType.VILLAGER, location);
    }

    @Override
    protected void addSpawnEntityPackets(@NonNull PacketLibraryApi packetApi, Collection<PacketContainer> packets) {
        super.addSpawnEntityPackets(packetApi, packets);

        packets.add(packetApi.getEntityLookPacket(getEntityId(), getLocation()));
        packets.add(packetApi.getHeadRotationPacket(getEntityId(), getLocation().getYaw())); //
    }

    public void setProfession(VillagerProfession profession) {
        if (!profession.isSupported(nmsApi)) {
            return;
        }

        this.profession = profession;

        getDataWatcher().setObject(16, profession.ordinal());
        broadcastDataWatcherPacket();
    }


}
