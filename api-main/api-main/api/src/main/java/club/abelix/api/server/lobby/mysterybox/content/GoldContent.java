package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.reward.impl.GoldReward;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class GoldContent extends GoldReward implements CaseContent {
    private final Rarity rarity;

    public GoldContent(@NonNull Rarity rarity, int amount) {
        super(amount, rarity.ordinal());

        this.rarity = rarity;
    }

    @Override
    public EquivalentType getEquivalentType() {
        return GoldType.INSTANCE;
    }
}
