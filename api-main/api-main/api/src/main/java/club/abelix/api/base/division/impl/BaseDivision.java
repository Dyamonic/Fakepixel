package club.abelix.api.base.division.impl;

import club.abelix.api.base.division.Division;
import club.abelix.api.base.division.DivisionRequirements;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.data.suffix.color.SuffixColor;
import club.abelix.network.type.mode.Mini;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>Division creation on 10.03.2024 at 01:52</p>
 */

@Getter
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class BaseDivision implements Division {
    int identifier;
    int priority;

    String displayText;
    String localization_key;
    SuffixColor color;

    Mini mini;
    List<DivisionRequirements> requirementsList;

    @Override
    public String getDisplayText(@NonNull Gamer gamer) {
        return this.displayText;
    }

    @Override
    public SuffixColor getColor(@NonNull Gamer gamer) {
        return this.color;
    }

    @Override
    public String getName(@NonNull Gamer gamer) {
        return gamer.getLanguage().getMessage("division_" + this.localization_key + "_name");
    }

    @Override
    public List<String> getLore(@NonNull Gamer gamer) {
        val language = gamer.getLanguage();

        List<String> baseLore
                = new ArrayList<>(language.getMessageList("division_" + this.localization_key + "_lore"));

        StatsMode statsMode;
        if (mini != null) {
            statsMode = StatsManager.INSTANCE.getMode(mini);
        } else {
            statsMode = StatsManager.INSTANCE.getMode(CoreUtil.getMaster());
        }

        val isAvailable = check(gamer, statsMode);

        baseLore.add("");
        if (isAvailable) {
            baseLore.add(language.getMessage("division_available"));
        } else {
            baseLore.add(language.getMessage("division_not_available"));

            for (val requirement : requirementsList) {
                baseLore.addAll(requirement.getLore(gamer, statsMode));
            }
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
