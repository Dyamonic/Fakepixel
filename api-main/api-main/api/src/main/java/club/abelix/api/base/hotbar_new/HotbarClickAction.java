package club.abelix.api.base.hotbar_new;

import club.abelix.api.base.gamer.Gamer;
import org.jetbrains.annotations.NotNull;

public record HotbarClickAction(
        Gamer gamer,
        ActionType type
) {

    @Override
    public @NotNull String toString() {
        return "HotbarClickAction{" +
                "gamer=" + gamer.getName() +
                ", type=" + type +
                '}';
    }
}
