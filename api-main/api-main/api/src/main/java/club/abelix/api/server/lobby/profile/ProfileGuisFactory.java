package club.abelix.api.server.lobby.profile;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.network.type.mode.Master;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ProfileGuisFactory {

    void openProfileGui(@NotNull Gamer gamer);

    void openPlayerProfileGui(@NotNull Gamer gamer, @NotNull Gamer targetGamer);

    void openStatsGui(@NotNull Gamer gamer, @NotNull Gamer target, @Nullable Gui parent);

    void openSettingsGui(@NotNull Gamer gamer, @Nullable Gui parent);

    void openDetailedStatsGui(@NotNull Gamer gamer, @NotNull Master master, @Nullable Gui parent);

    void openLevelRewardsGui(@NotNull Gamer gamer, @Nullable Gui parent);
}
