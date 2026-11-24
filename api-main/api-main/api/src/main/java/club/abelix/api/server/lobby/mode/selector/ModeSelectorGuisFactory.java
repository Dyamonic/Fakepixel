package club.abelix.api.server.lobby.mode.selector;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.commons.language.Lang;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.Uncommon;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ModeSelectorGuisFactory {

    void openLobbySelector(@NotNull Gamer gamer, @NotNull String title, @Nullable Gui parent);

    default void openLobbySelector(@NotNull Gamer gamer, @NotNull Lang title, @Nullable Gui parent) {
        openLobbySelector(gamer, title.compile(), parent);
    }

    void openModeActionGui(@NonNull Mini mode, @NonNull Gamer gamer, @NonNull String title, @Nullable Gui parent);

    default void openModeActionGui(@NonNull Mini mode, @NonNull Gamer gamer, @NonNull Lang title, @Nullable Gui parent) {
        openModeActionGui(mode, gamer, title.compile(), parent);
    }

    void openSeverSelectorGui(@NotNull Gamer gamer, @Nullable Gui parent);

    void openUncommonSelector(@NonNull Gamer gamer, @NonNull Uncommon uncommon, @Nullable Gui parent);
}
