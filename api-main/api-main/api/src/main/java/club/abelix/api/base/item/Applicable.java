package club.abelix.api.base.item;

import club.abelix.api.base.gamer.Gamer;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public interface Applicable {

    void apply(@NotNull Gamer gamer);

}
