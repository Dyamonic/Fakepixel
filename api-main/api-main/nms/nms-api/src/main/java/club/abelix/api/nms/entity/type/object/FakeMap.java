package club.abelix.api.nms.entity.type.object;

import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.network.type.mode.ServerType;

public interface FakeMap<T> {

    static int getStartMapIdValue() {
        StarshipBukkit core = StarshipBukkit.getInstance();
        ServerType type = core.getServerType();

        if (type.isLobby()) {
            return type.getLobby().ordinal() * 1000 + ((type.getServerNumber() - 1) * 100);
        }

        return 0;
    }

    int getId();

    int getZoom();

    byte[] getData();

    T getPacket();

    void setPixel(int x, int y, int color);

    boolean isUpdated();

    void update();

}
