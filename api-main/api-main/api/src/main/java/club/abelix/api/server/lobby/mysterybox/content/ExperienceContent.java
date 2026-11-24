package club.abelix.api.server.lobby.mysterybox.content;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.reward.impl.ExperienceReward;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;

import java.util.concurrent.ThreadLocalRandom;

public class ExperienceContent extends ExperienceReward implements CaseContent {

    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    @Getter
    private final Rarity rarity;

    public ExperienceContent(@NonNull Rarity rarity, int exp) {
        super(exp);

        this.rarity = rarity;
    }

    @Override
    public int getDuplicateEquivalent(@NonNull Gamer gamer) {
        int minBounds = rarity.ordinal() * 2;
        int maxBounds = rarity.ordinal() * 4;

        return random.nextInt(minBounds, maxBounds);
    }

    @Override
    public String getRewardName(@NonNull Language language) {
        return language.getMessage(
                "case_content_experience",
                "%rarity_color%", rarity.getColor(),
                "%amount%", experience
        );
    }

    @Override
    public EquivalentType getEquivalentType() {
        return GoldType.INSTANCE;
    }

}
