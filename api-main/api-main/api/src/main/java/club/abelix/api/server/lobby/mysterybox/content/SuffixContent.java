package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.impl.SuffixReward;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import java.util.HashMap;
import java.util.Map;

@Getter
public class SuffixContent extends SuffixReward implements CaseContent {

    private final static Map<Rarity, Integer> DUPLICATE = new HashMap<>() {{
        put(Rarity.COMMON, 19);
        put(Rarity.RARE, 39);
        put(Rarity.EPIC, 69);
        put(Rarity.LEGENDARY, 85);
    }};

    private final Rarity rarity;

    public SuffixContent(@NonNull Rarity rarity, @NonNull Suffix suffix) {
        super(suffix);

        this.rarity = rarity;
    }

    @Override
    public int getDuplicateEquivalent(@NonNull Gamer gamer) {
        val suffixRarity = suffix.getRarity();

        return DUPLICATE.getOrDefault(suffixRarity, 30);
    }

    @Override
    public String getRewardName(@NonNull Language language) {
        return language.getMessage(
                "case_content_suffix",
                "%rarity_color%", rarity.getColor(),
                "%suffix%", suffix.getDisplaySuffix()
        );
    }

    @Override
    public EquivalentType getEquivalentType() {
        return GoldType.INSTANCE;
    }

}
