package club.abelix.api.base.box.standard;

import club.abelix.api.base.box.AbstractBox;
import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.box.type.StandardBoxType;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperienceBox extends AbstractBox {

    private static final Map<Rarity, String> ICONS = new HashMap<>() {{
        put(Rarity.COMMON, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzVlNGNiNGZjMTRiYTY5ZDQ5YjJkZDJiMDQxOGQ2NTYyZGZmZGViZDBlYmYwMTY1YzQ0MTZhMDczZTYyZGRmOCJ9fX0=");
        put(Rarity.RARE, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzFmN2NkZmVhMmQyMWNkNWY2ZWJiZjQ4NDgxNzYxYzZjYmRmMzZkMDBmZTY0MDgzNjg2ZTlhZWFhM2YxZjIxNyJ9fX0=");
        put(Rarity.EPIC, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Y2NmY3ZjAzMTI1Y2Y1NDczMzY5NmYzNjMyZjBkOWU2NDcwYmFhYjg0OTg0N2VhNWVhMmQ3OTE1NmFkMGY0MCJ9fX0=");
        put(Rarity.LEGENDARY, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODQ0NDk4YTBmZTI3ODk1NmUzZDA0MTM1ZWY0YjEzNDNkMDU0OGE3ZTIwOGM2MWIxZmI2ZjNiNGRiYzI0MGRhOCJ9fX0=");
    }};

    public ExperienceBox(
            int goldCost,
            int coinsCost,
            Rarity rarity,
            List<? super BoxReward> rewards
    ) {
        super(
                "experience_box",
                StandardBoxType.EXPERIENCE,
                goldCost,
                coinsCost,
                rarity,
                null,
                rewards
        );
    }

    @Override
    public boolean isCraftable() {
        return false;
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isPurchasable(@NotNull GameCurrency currency) {
        return currency != GameCurrency.COINS;
    }

    @Override
    public @NotNull ItemStack getIcon(@NotNull Gamer gamer) {
        return ItemUtil.getSkull(ICONS.get(getRarity()));
    }
}
