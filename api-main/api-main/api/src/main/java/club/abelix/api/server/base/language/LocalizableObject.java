package club.abelix.api.server.base.language;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.packet.IVisibleObject;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class LocalizableObject {

    // ----------------------------------------------------------

    protected final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

    // ----------------------------------------------------------
    /**
     * Объекты для каждого языка
     */
    private final Map<Language, List<IVisibleObject>> objectsPerLanguage = new HashMap<>();

    public void create(@NonNull Location location) {
        for (Language language : Language.values()) {
            createEntities(language, location);
        }

        createUpdaterTask();
    }

    public abstract void createEntities(@NonNull Language language, @NonNull Location location);

    public void createUpdaterTask() {

    }

    public void remove() {

    }

    public void send(@NonNull Gamer gamer) {
        send(gamer.getLanguage(), gamer.getPlayer());
    }

    public void remove(@NonNull Gamer gamer) {
        remove(gamer.getLanguage(), gamer.getPlayer());
    }

    private void send(@NonNull Language language, @NonNull Player player) {
        for (IVisibleObject object : getRegisteredObjects(language)) {
            object.addReceiver(player);
        }
    }

    public void remove(@NonNull Language language, @NonNull Player player) {
        for (IVisibleObject object : getRegisteredObjects(language)) {
            object.removeReceiver(player);
        }
    }

    public void registerObject(@NonNull Language language, @NonNull IVisibleObject object) {
        getRegisteredObjects(language).add(object);
    }

    public IVisibleObject getRegisteredObjectByIndex(@NonNull Language language, int index) {
        List<IVisibleObject> objects = getRegisteredObjects(language);

        return index >= 0 && index < objects.size() ? objects.get(index) : null;
    }

    public List<IVisibleObject> getRegisteredObjects(@NonNull Language language) {
        return objectsPerLanguage.computeIfAbsent(language, k -> new ArrayList<>());
    }

}
