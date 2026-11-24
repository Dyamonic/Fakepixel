package club.abelix.api.server.lobby.impl.hub.present.storage;

import club.abelix.api.server.lobby.impl.hub.present.Present;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class AbstractPresentStorage implements PresentStorage {

    Map<Location, Present> presentsByLocation = new HashMap<>();

    @Override
    public @NotNull Present findPresentByLocation(final @NonNull Location location) {
        return presentsByLocation.get(location);
    }

    @Override
    public @UnmodifiableView @NotNull Collection<@NotNull Present> findAll() {
        return Collections.unmodifiableCollection(presentsByLocation.values());
    }

    @Override
    public int count() {
        return presentsByLocation.size();
    }

    protected void addPresent(final Location location, final Present present) {
        presentsByLocation.put(location, present);
        log.info("Added present {} at {}", present.id(), location);
    }
}
