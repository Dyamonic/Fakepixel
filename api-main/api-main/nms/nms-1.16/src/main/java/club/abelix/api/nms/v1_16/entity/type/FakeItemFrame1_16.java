package club.abelix.api.nms.v1_16.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.entity.type.object.FakeEntityItem;
import club.abelix.api.nms.entity.type.object.FakeEntityItemFrame;
import club.abelix.api.nms.entity.type.object.FakeMap;
import club.abelix.api.nms.packet.PacketLibraryApi;
import lombok.Getter;
import net.minecraft.server.v1_16_R3.PacketPlayOutMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class FakeItemFrame1_16 extends FakeItem1_16 implements FakeEntityItemFrame {

    @Getter
    private FakeMap<PacketPlayOutMap> fakeMap;

    public FakeItemFrame1_16(NmsApi nmsApi, Location location, BlockFace face) {
        super(nmsApi, EntityType.ITEM_FRAME,
                FakeEntityItem.frameLocation1_12AndUpper(face, location.clone()), FakeEntityItem.faceToDirection(face));

        int numberOfItemFrame = 1;

        for (FakeEntity fakeEntity : nmsApi.getFakeEntityApi().getTrackedEntities()) {
            if (fakeEntity instanceof FakeEntityItemFrame) {
                numberOfItemFrame++;
            }
        }

        this.fakeMap = new FakeMap1_16(FakeMap.getStartMapIdValue() + numberOfItemFrame);
    }

    @Override
    public boolean addReceivers(@NotNull Collection<? extends Player> players) {
        boolean add = super.addReceivers(players);

        if (add) {
            PacketLibraryApi packetApi = nmsApi.getPacketLibraryApi();
            players.forEach(player -> packetApi.sendPacket(player, fakeMap.getPacket()));
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
