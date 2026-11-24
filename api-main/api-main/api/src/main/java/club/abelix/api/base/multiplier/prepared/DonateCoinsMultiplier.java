package club.abelix.api.base.multiplier.prepared;

import club.abelix.api.base.multiplier.Multiplier;
import club.abelix.commons.data.booster.MultiplierType;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.data.rank.Rank;
import club.abelix.commons.data.rank.UserRanks;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.PermissionUtil;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class DonateCoinsMultiplier extends Multiplier {

    private final Rank rank;

    public DonateCoinsMultiplier(Rank rank) {
        super(MultiplierType.EXPERIENCE, PermissionUtil.extractDoubleValue(rank, "donate.coins", "multiplier"));

        this.rank = rank;
    }

    @Override
    public String getDisplayName(Language language) {
        return language.getMessage("donate_coin_multiplier_name",
                "%group%", rank.getDisplayName(language), "%multiplier%", (1.0 + value));
    }
}
