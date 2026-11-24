package club.abelix.api.base.multiplier.prepared;

import club.abelix.api.base.multiplier.Multiplier;
import club.abelix.commons.data.booster.MultiplierType;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.PermissionUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class DonateExpMultiplier extends Multiplier {

    UserRanks ranks;

    public DonateExpMultiplier(@NonNull UserRanks ranks) {
        super(MultiplierType.EXPERIENCE, PermissionUtil.extractDoubleLimit(ranks, "donate.exp.multiplier"));

        this.ranks = ranks;
    }

    @Override
    public String getDisplayName(Language language) {
        return language.getMessage("donate_experience_multiplier_name",
                "%group%", ranks.findPriorityRank().rank().getDisplayName(language), "%multiplier%", (1.0 + value));
    }

}
