package club.abelix.api.nms.v1_12.entity.type;

import club.abelix.api.nms.entity.type.object.AbstractFakeMap;
import lombok.Getter;
import net.minecraft.server.v1_12_R1.PacketPlayOutMap;

import java.util.Collections;

public class FakeMap1_12 extends AbstractFakeMap<PacketPlayOutMap> {

    public FakeMap1_12(int id, int zoom) {
        super(id, zoom);
    }

    public FakeMap1_12(int id) {
        super(id);
    }

    @Getter
    private PacketPlayOutMap packet;

    @Override
    public void updatePacket() {
        packet = new PacketPlayOutMap(
                id, (byte) zoom, false, Collections.emptyList(),
                data, 0, 0, 128, 128
        );
    }

}
