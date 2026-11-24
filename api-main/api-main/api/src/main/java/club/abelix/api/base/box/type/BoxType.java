package club.abelix.api.base.box.type;

import club.abelix.commons.language.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public interface BoxType {

    @Range(from = 0, to = Integer.MAX_VALUE)
    int getId();

    @NotNull
    String getName(@NotNull Language language);

}
