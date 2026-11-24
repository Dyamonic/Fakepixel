package club.abelix.api.nms.entity;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.entity.type.FakeEntity;
import club.abelix.api.nms.entity.type.blaze.FakeEntityBlaze;
import club.abelix.api.nms.entity.type.sheep.FakeEntitySheep;
import club.abelix.api.nms.entity.type.wolf.FakeEntityWolf;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import lombok.NonNull;
import org.bukkit.Location;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FakeEntityApiImpl implements FakeEntityApi {

    protected final NmsApi nmsApi;

    private final Map<Integer, FakeEntity> idToEntityMap = new ConcurrentHashMap<>();

    public FakeEntityApiImpl(NmsApi nmsApi) {
        this.nmsApi = nmsApi;
    }

    @Override
    public synchronized Collection<FakeEntity> getTrackedEntities() {
        return idToEntityMap.values();
    }

    @Override
    public synchronized void registerEntity(@NonNull FakeEntity fakeEntity) {
        idToEntityMap.put(fakeEntity.getEntityId(), fakeEntity);
    }

    @Override
    public synchronized void removeEntity(@NonNull FakeEntity fakeEntity) {
        idToEntityMap.remove(fakeEntity.getEntityId());
    }

    @Override
    public FakeEntityBlaze createBlaze(@NonNull Location location) {
        return null;
    }

    @Override
    public FakeEntitySheep createSheep(@NonNull Location location) {
        return null;
    }

    @Override
    public FakeEntityWolf createWolf(@NonNull Location location) {
        return null;
    }

    @Override
    public synchronized void removeEntities() {
        new HashSet<>(idToEntityMap.values()).forEach(FakeEntity::remove);
    }

    @Override
    public synchronized FakeEntity getFakeEntity(int fakeEntityId) {
        return idToEntityMap.get(fakeEntityId);
    }

    @Override
    public WrappedDataWatcher cloneDataWatcher(@NonNull WrappedDataWatcher original) {
        WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        Iterator<WrappedWatchableObject> objectIterator = original.iterator();

        boolean newVersion = MinecraftReflection.watcherObjectExists();

        while (objectIterator.hasNext()) {
            WrappedWatchableObject object = objectIterator.next();

            if (newVersion) {
                dataWatcher.setObject(object.getWatcherObject(), object);
            } else {
                dataWatcher.setObject(object.getIndex(), object);
            }
        }

        return dataWatcher;
    }
}
