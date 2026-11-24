package club.abelix.api.base.box.animation;

import club.abelix.commons.language.Language;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public interface BoxOpenAnimation extends Listener {

    @NotNull
    String getName(@NotNull Language language);

    void start(@NotNull Location startLocation);

    void finish();

    void onSelect(@NotNull Location location);

    void cancel();

    void onInteractEvent(@NotNull Location location);

    void revealRewards();
}
