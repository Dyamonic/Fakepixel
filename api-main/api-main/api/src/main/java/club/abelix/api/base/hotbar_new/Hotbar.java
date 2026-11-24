package club.abelix.api.base.hotbar_new;

import club.abelix.api.base.gamer.Gamer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.Optional;

public interface Hotbar {

    @NotNull String getId();

    @NotNull
    @UnmodifiableView
    List<@NotNull HotbarIcon> getIcons();

    @NotNull
    Optional<@NotNull HotbarIcon> getIcon(@Range(from = 0, to = 8) int slot);

    void apply(@NotNull Gamer gamer);

}
