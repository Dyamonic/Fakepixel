package club.abelix.network.packet;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.Getter;

public class PacketMapper {

    private final PacketFactory[] idFactoryMap
            = new PacketFactory[2096];

    // юзается для отладки в хуйне с эксцепшеном отправки
    // не регнутых пакетов
    @Getter
    private final TObjectIntMap<Class<?>> classIdMap
            = new TObjectIntHashMap<>(2096, 0.5F, -1);

    public void registerPacket(int id, Class<?> cls, PacketFactory factory) {
        idFactoryMap[id] = factory;
        classIdMap.put(cls, id);
    }

    public Packet<?> newPacket(int id) {
        if (id >= idFactoryMap.length) return null;

        PacketFactory supplier = idFactoryMap[id];
        if (supplier == null) return null;

        return supplier.newInstance();
    }

    public int getPacketId(Class<?> cls) {
        return classIdMap.get(cls);
    }

}
