package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.entity.type.object.AbstractFakeMap;
import lombok.Getter;
import net.minecraft.server.v1_8_R3.PacketPlayOutMap;

import java.util.Collections;

@Getter
public class FakeMap1_8 extends AbstractFakeMap<PacketPlayOutMap> {

    private PacketPlayOutMap packet;

    public FakeMap1_8(int id, int zoom) {
        super(id, zoom);
    }

    public FakeMap1_8(int id) {
        super(id);
    }

    public FakeMap1_8() {
        super();
    }

    public void updatePacket() {
        packet = new PacketPlayOutMap(
                id,
                (byte) zoom,
                Collections.emptyList(),
                data,
                0, 0, 128, 128
        );
    }
}