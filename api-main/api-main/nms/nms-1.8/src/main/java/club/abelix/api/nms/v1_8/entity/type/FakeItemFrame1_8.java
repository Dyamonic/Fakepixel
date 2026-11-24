package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import club.abelix.api.nms.entity.type.object.FakeEntityItemFrame;
import club.abelix.api.nms.entity.type.object.FakeMap;
import lombok.Getter;
import lombok.NonNull;
import net.minecraft.server.v1_8_R3.PacketPlayOutMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FakeItemFrame1_8 extends FakeObject1_8 implements FakeEntityItemFrame {

    @Getter
    private final FakeMap<PacketPlayOutMap> fakeMap;

    public FakeItemFrame1_8(NmsApi api, @NonNull Location location, @NonNull BlockFace face) {
        super(api, EntityType.ITEM_FRAME,
                FakeEntityItem.frameLocation1_8(face, location.clone()), FakeEntityItem.faceToDirection(face));

        int numberOfItemFrame = 1;

        for (FakeEntity fakeEntity : nmsApi.getFakeEntityApi().getTrackedEntities()) {
            if (fakeEntity instanceof FakeEntityItemFrame) {
                numberOfItemFrame++;
            }
        }

        this.fakeMap = new FakeMap1_8(FakeMap.getStartMapIdValue() + numberOfItemFrame);
    }

    @Override
    public void setItemStack(@NotNull ItemStack itemStack) {
        getDataWatcher().setObject(8, itemStack);
    }

    @Override
    public boolean addViewer(@NotNull Player player) {
        boolean add = super.addViewer(player);

        if (add) {
            nmsApi.getPacketLibraryApi().sendPacket(player, fakeMap.getPacket());
        }

        return add;
    }

    @Override
    public boolean addReceiver(@NotNull Player player) {
        boolean add = super.addReceiver(player);

        if (add) {
            nmsApi.getPacketLibraryApi().sendPacket(player, fakeMap.getPacket());
        }

        return add;
    }

    @Override
    public void update() {
        fakeMap.update();

        for (Player receiver : getReceivers()) {
            nmsApi.getPacketLibraryApi().sendPacket(receiver, fakeMap.getPacket());
        }

        setItemStack(new ItemStack(Material.MAP, 1, (short) fakeMap.getId()));
    }

    @Override
    public int getSpawnTypeId() {
        return 71;
    }
}
