package club.abelix.api.base.box.reward.standard;

import club.abelix.api.base.box.reward.AbstractBoxReward;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.EquivalentType;
import club.abelix.api.server.lobby.mysterybox.content.equivalent.GoldType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.language.Language;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TitleReward extends AbstractBoxReward {

    private final static Map<Rarity, Integer> DUPLICATE = new HashMap<>() {{
        put(Rarity.COMMON, 19);
        put(Rarity.RARE, 39);
        put(Rarity.EPIC, 69);
        put(Rarity.LEGENDARY, 85);
    }};

    Suffix title;

    public TitleReward(
            Rarity rarity,
            Suffix title,
            int chance
    ) {
        super(
                rarity,
                chance
        );

        this.title = title;
    }

    @Override
    public @NotNull String getName(@NotNull Language language) {
        return title.getDisplaySuffix();
    }

    @Override
    public @UnmodifiableView @NotNull List<@NotNull String> getDescription(@NotNull Language language) {
        return List.of();
    }

    @Override
    public boolean apply(@NotNull Gamer gamer) {
        if (gamer.isSuffixPurchased(title)) {
            return false;
        }

        gamer.addPurchasedSuffix(title.getId());

        return true;
    }

    @Override
    public @NotNull ItemStack getRewardIcon(@NotNull Language language) {
        return new ItemStack(Material.NAME_TAG);
    }

    @Override
    public @NotNull EquivalentType getEquivalentType() {
        return new GoldType();
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getEquivalent(@NotNull Gamer gamer) {
        return DUPLICATE.get(title.getRarity());
    }
}
