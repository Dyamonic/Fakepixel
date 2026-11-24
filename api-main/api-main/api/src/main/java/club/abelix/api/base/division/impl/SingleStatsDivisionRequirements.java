package club.abelix.api.base.division.impl;

import club.abelix.api.base.division.DivisionRequirements;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.stat.StatsMode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>WinDivisionRequirements creation on 14.03.2024 at 16:15</p>
 */

@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class SingleStatsDivisionRequirements implements DivisionRequirements {
    String field;
    int required;

    @Override
    public boolean check(@NonNull Gamer gamer, @NonNull StatsMode statsMode) {
        val gamerValue = gamer.getLifetimeStat(statsMode.getField(this.field));
        return gamerValue >= this.required;
    }

    @Override
    public List<String> getLore(@NonNull Gamer gamer, @NonNull final StatsMode statsMode) {
        return List.of(
                gamer.getLanguage().getMessage("division_requirement_" + this.field + "_lore",
                        "%required%", String.valueOf(this.required),
                        "%current%", gamer.getLifetimeStat(statsMode.getField(this.field))
                ));
    }
}
