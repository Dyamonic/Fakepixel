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

public class TitlesBox extends AbstractBox {

    private static final Map<Rarity, String> ICONS = new HashMap<>() {{
        put(Rarity.COMMON, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2YxOWJkZmRmYWEzOTk0OWRhMDY2N2Y0YTg5ZjM2NDI2YmE4NjkyYWVmNTFmYzYwNTFhOTgwMjMyN2RhNyJ9fX0=");
        put(Rarity.RARE, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTlkZDc3MTVlNmRjNjQyZTUyZDVjMDJjNjI0YmZhMWE1NmYwNWNkYWJiZTljYTc4OWY1MGVjNjExODk5ZiJ9fX0=");
        put(Rarity.EPIC, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzY3N2U2NWRmMjk5OWQwMzE5ZmRiY2JhM2MwOTJmMTYwYjk5YjRiNDY3OTgzYWY4MWZjZmExZWI0NWQzOWEzIn19fQ==");
        put(Rarity.LEGENDARY, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzNiZGJhZWRkN2Q2NDQ0ZTc5YWE4MjIyZjg5ODEyNDAyMDRjYzNjYzFjOTY1NTExODY4NzYxOGRiOGNlYyJ9fX0=");
    }};

    public TitlesBox(
            int goldCost,
            Rarity rarity,
            List<? super BoxReward> rewards
    ) {
        super(
                "titles_box",
                StandardBoxType.TITLES,
                goldCost,
                0,
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
