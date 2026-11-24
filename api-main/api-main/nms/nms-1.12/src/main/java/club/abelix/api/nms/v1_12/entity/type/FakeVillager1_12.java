package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.villager.FakeEntityVillager;
import club.abelix.api.nms.entity.type.villager.VillagerProfession;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class FakeVillager1_12 extends FakeLivingEntity1_12 implements FakeEntityVillager {

    // ----------------------------------------------------------------------

    private static final WrappedDataWatcher.WrappedDataWatcherObject VILLAGER_PROFESSION_OBJECT
            = new WrappedDataWatcher.WrappedDataWatcherObject(13, INT_SERIALIZER);

    // ----------------------------------------------------------------------

    @Getter
    private VillagerProfession profession;

    public FakeVillager1_12(NmsApi api, Location location) {
        super(api, EntityType.VILLAGER, location);
    }

    public synchronized void setProfession(VillagerProfession profession) {
        this.profession = profession;

        getDataWatcher().setObject(VILLAGER_PROFESSION_OBJECT, profession.ordinal());
        broadcastDataWatcherPacket();
    }

}
