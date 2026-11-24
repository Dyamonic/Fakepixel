package club.abelix.api.server.base.language;

import club.abelix.api.server.base.language.event.LocalizableObjectCreateEvent;
import club.abelix.api.server.base.language.event.LocalizableObjectRemoveEvent;
import club.abelix.api.server.base.language.listener.LocalizableObjectListener;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Временное (или нет) решение для отправки разных энтити для каждого языка игрокам
 */
public class LocalizableObjectManager {

    // --------------------------------------------------------------

    public static final LocalizableObjectManager IMP = new LocalizableObjectManager();

    // --------------------------------------------------------------
    private final List<LocalizableObject> objects = new ArrayList<>();

    public LocalizableObjectManager() {
        new LocalizableObjectListener();
    }

    public void addLocalizableObject(@NonNull LocalizableObject object) {
        // вызываем эвент создания
        HeartBeat.callEvent(new LocalizableObjectCreateEvent(object));

        // добавляем в лист
        objects.add(object);
    }

    public List<LocalizableObject> getObjects() {
        return objects;
    }

    public void removeObject(@NonNull LocalizableObject object) {
        // вызываем эвент удаления
        HeartBeat.callEvent(new LocalizableObjectRemoveEvent(object));

        // вызываем метод подчистки, чтобы там удалить какие-то свои фичи
        object.remove();

        // удаляем из листа объектов
        objects.remove(object);
    }

}
