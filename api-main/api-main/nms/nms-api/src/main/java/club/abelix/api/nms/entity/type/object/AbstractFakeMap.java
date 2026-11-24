package club.abelix.api.nms.entity.type.object;

import lombok.Getter;
import org.bukkit.map.MapPalette;

import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public abstract class AbstractFakeMap<T> implements FakeMap<T> {

    protected int id;
    protected int zoom;
    protected final byte[] data = new byte[128 * 128];
    private final AtomicBoolean updated = new AtomicBoolean();

    public AbstractFakeMap(int id, int zoom) {
        this.id = id;
        this.zoom = zoom;
    }

    public AbstractFakeMap(int id) {
        this(id, 0);
    }

    public AbstractFakeMap() {
        this(ThreadLocalRandom.current().nextInt(5000, Short.MAX_VALUE));
    }

    public boolean isUpdated() {
        return updated.get();
    }

    public void setPixel(int x, int y, int color) {
        byte pixel = MapPalette.matchColor(new Color(color, true));

        int index = y * 128 + x;

        if (data[index] != pixel) {
            updated.set(true);
            data[index] = pixel;
        }
    }

    public void update() {
        if (!updated.getAndSet(false)) {
            return;
        }

        updatePacket();
    }

    public abstract T getPacket();

    protected abstract void updatePacket();
}
