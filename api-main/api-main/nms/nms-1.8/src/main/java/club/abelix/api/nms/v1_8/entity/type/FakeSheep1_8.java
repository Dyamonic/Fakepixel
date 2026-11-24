package club.abelix.api.nms.v1_8.entity.type;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.sheep.FakeEntitySheep;
import lombok.NonNull;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>FakeSheep1_8 creation on 04.09.2022 at 23:04</p>
 */

public class FakeSheep1_8 extends FakeLivingEntity1_8 implements FakeEntitySheep {
    private byte woolData;

    public FakeSheep1_8(@NonNull NmsApi nmsApi, @NonNull Location location) {
        super(nmsApi, EntityType.SHEEP, location);

        getDataWatcher().setObject(16, (byte) 0);
    }

    @Override
    public void setColor(@NonNull DyeColor color) {
        int newColorId = color.getWoolData() & 0xF;
        int oldColorId = woolData & 0xF;

        if (newColorId == oldColorId) {
            return;
        }

        this.woolData = (byte) ((woolData & 0x10) | newColorId);
        updateWoolData();
    }

    @Override
    public void setSheared(boolean sheared) {
        boolean oldSheared = (woolData & 0x10) == 0x10;

        if (sheared == oldSheared) {
            return;
        }

        if (sheared) {
            this.woolData |= 0x10;
        } else {
            this.woolData &= ~0x10;
        }

        updateWoolData();
    }

    private void updateWoolData() {
        getDataWatcher().setObject(16, woolData);
        broadcastDataWatcherPacket();
    }
}
