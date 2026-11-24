package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.reward.impl.KeyReward;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;

public class KeyContent extends KeyReward implements CaseContent {

    @Getter
    private final Rarity rarity;

    public KeyContent(@NonNull Rarity rarity, @NonNull CaseType type, int amount) {
        super(type, amount);

        this.rarity = rarity;
    }

    public KeyContent(@NonNull Rarity rarity, @NonNull CaseType type) {
        super(type);

        this.rarity = rarity;
    }

    @Override
    public String getRewardName(@NonNull Language language) {
        return language.getMessage("case_content_keys",
                "%rarity_color%", rarity.getColor(),
                "%name%", type.getDisplayName(language),
                "%amount%", amount);
    }

}
