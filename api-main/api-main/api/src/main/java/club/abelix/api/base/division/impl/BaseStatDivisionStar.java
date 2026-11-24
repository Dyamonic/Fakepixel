package club.abelix.api.base.division.impl;

import club.abelix.api.base.division.DivisionManager;
import club.abelix.api.base.division.DivisionRequirements;
import club.abelix.api.base.division.DivisionStar;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.network.type.mode.Mini;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>BaseDivisionStar creation on 19.03.2024 at 18:06</p>
 */

@Getter
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class BaseStatDivisionStar implements DivisionStar {

    int identifier;
    String star;
    String localizationKey;
    Mini mini;
    List<DivisionRequirements> requirementsList;


    @Override
    public boolean isPurchased(@NonNull Gamer gamer) {
        return true;
    }

    @Override
    public void processPurchase(@NonNull Gamer gamer) {
    }

    @Override
    public int getPrice() {
        return 0;
    }

    @Override
    public String getDisplayName(@NonNull Gamer gamer) {
        return gamer.getLanguage().getMessage("division_star_" + this.localizationKey + "_name");
    }

    @Override
    public List<String> getLore(@NonNull Gamer gamer) {
        val language = gamer.getLanguage();

        List<String> baseLore =
                new ArrayList<>(language.getMessageList("division_" + this.localizationKey + "_lore", "%display%", DivisionManager.IMP.getDisplayText(gamer)));

        StatsMode statsMode;
        if (mini != null) {
            statsMode = StatsManager.INSTANCE.getMode(mini);
        } else {
            statsMode = StatsManager.INSTANCE.getMode(CoreUtil.getMaster());
        }

        val isAvailable = check(gamer, statsMode);

        baseLore.add("");
        if (isAvailable) {
            baseLore.add(language.getMessage("division_star_available",
                    "%icon%", this.getStar()));
        } else {

            for (val requirement : requirementsList) {
                baseLore.addAll(requirement.getLore(gamer, statsMode));
            }

            baseLore.add(language.getMessage("division_star_not_available", "%price%",
                    this.getPrice(), "%icon%", this.getStar()));
        }

        return baseLore;
    }


    @Override
    public boolean check(@NonNull Gamer gamer, @NonNull StatsMode statsMode) {
        if (requirementsList.isEmpty()) {
            return true;
        }

        for (val requirement : requirementsList) {
            if (!requirement.check(gamer, statsMode)) {
                return false;
            }
        }

        return true;
    }
}
