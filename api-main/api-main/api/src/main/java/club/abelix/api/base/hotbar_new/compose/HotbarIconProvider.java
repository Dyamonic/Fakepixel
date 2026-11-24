package club.abelix.api.base.hotbar_new.compose;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.HotbarIcon;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public interface HotbarIconProvider {

    int priority();

    boolean appliesTo(@NotNull Gamer gamer);

    @NotNull Stream<@NotNull HotbarIcon> provide(@NotNull Gamer gamer);

    @NotNull ConflictPolicy policy();
}
