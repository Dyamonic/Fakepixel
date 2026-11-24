package club.abelix.api.server.base.duels.gui;

import club.abelix.api.base.gamer.Gamer;
import org.jetbrains.annotations.NotNull;

public interface DuelsGuiFactory {
    void openDuelsRequestGui(@NotNull Gamer sender, @NotNull Gamer receiver);
}
