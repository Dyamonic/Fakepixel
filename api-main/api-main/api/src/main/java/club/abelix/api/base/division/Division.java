package club.abelix.api.base.division;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.data.suffix.color.SuffixColor;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;

import java.util.List;

public interface Division {

    int getIdentifier();

    Mini getMini();

    int getPriority();

    String getDisplayText(@NonNull Gamer gamer);

    SuffixColor getColor(@NonNull Gamer gamer);

    String getName(@NonNull Gamer gamer);

    List<String> getLore(@NonNull Gamer gamer);

    List<DivisionRequirements> getRequirementsList();

    boolean check(@NonNull Gamer gamer, @NonNull StatsMode statsMode);
}
