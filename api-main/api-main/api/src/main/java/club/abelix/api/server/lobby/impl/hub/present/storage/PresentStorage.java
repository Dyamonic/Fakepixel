package club.abelix.api.server.lobby.impl.hub.present.storage;

import club.abelix.api.server.lobby.impl.hub.present.Present;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;

public interface PresentStorage {

    void load();

    @Nullable Present findPresentByLocation(@NotNull Location location);

    @UnmodifiableView
    @NotNull Collection<@NotNull Present> findAll();

    int count();
}
