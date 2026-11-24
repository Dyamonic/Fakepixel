package club.abelix.api.nms.v1_16.entity.type;

import club.abelix.api.nms.entity.type.object.AbstractFakeMap;
import lombok.Getter;
import net.minecraft.server.v1_16_R3.PacketPlayOutMap;

import java.util.Collections;

public class FakeMap1_16 extends AbstractFakeMap<PacketPlayOutMap> {

    @Getter
    private PacketPlayOutMap packet;

    public FakeMap1_16(int id, int zoom) {
        super(id, zoom);
    }

    public FakeMap1_16(int id) {
        super(id);
    }

    public FakeMap1_16() {
        super();
    }

    @Override
    public void updatePacket() {
        packet = new PacketPlayOutMap(
                id,
                (byte) zoom,
                false,
                false,
                Collections.emptyList(),
                data,
                0, 0, 128, 128
        );
    }

}
