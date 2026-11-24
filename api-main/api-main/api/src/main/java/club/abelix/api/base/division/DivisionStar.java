package club.abelix.api.base.division;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;

import java.util.List;

public interface DivisionStar {
    int getIdentifier();

    Mini getMini();

    String getStar();

    boolean isPurchased(@NonNull Gamer gamer);

    void processPurchase(@NonNull Gamer gamer);

    int getPrice();

    String getDisplayName(@NonNull Gamer gamer);

    List<String> getLore(@NonNull Gamer gamer);

    List<DivisionRequirements> getRequirementsList();

    boolean check(@NonNull Gamer gamer, @NonNull StatsMode statsMode);
}
