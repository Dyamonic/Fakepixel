package club.abelix.api.base.box.standard;

import club.abelix.api.base.box.AbstractBox;
import club.abelix.api.base.box.reward.BoxReward;
import club.abelix.api.base.box.type.BoxType;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.data.currency.GameCurrency;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankBox extends AbstractBox {

    private static final Map<Rarity, String> ICONS = new HashMap<>() {{
        put(Rarity.COMMON, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTFkYWVlM2Y0NTNhMzhlMzVkOGNhYjgzNTNjNWQyOGNjZTFiNjc1ZmY5NGI5MmU1YzRhZjJkNjQ1MjAzMWEwIn19fQ==");
        put(Rarity.RARE, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGNjNzk5NTRkMzUwYTk4YzcyMTcyNTY4MzFmNjVjNjJhNDI4MDc0YjZlNGFlOWVlZGU3YTQ0ZjlkZTRhNyJ9fX0=");
        put(Rarity.EPIC, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjQzNmJlOTI4ODdkYzQwY2RmMzcyYmIzYzM4MTFiYWY1YzM2ZmYxMzExNjFiODJmMDY3NTY0MTM2YmJhYiJ9fX0=");
        put(Rarity.LEGENDARY, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDhjMWUxYzYyZGM2OTVlYjkwZmExOTJkYTZhY2E0OWFiNGY5ZGZmYjZhZGI1ZDI2MjllYmZjOWIyNzg4ZmEyIn19fQ==");
    }};

    public RankBox(
            final BoxType boxType,
            final int goldCost,
            final Rarity rarity,
            final List<? super BoxReward> rewards
    ) {
        super(
                "ranks_box",
                boxType,
                goldCost,
                0,
                rarity,
                null,
                rewards
        );
    }

    @Override
    public boolean isCraftable() {
        return true;
    }

    @Override
    public boolean isUpgradable() {
        return true;
    }

    @Override
    public @NotNull ItemStack getIcon(@NotNull Gamer gamer) {
        return ItemUtil.getSkull(ICONS.get(getRarity()));
    }

    @Override
    public boolean isPurchasable(@NotNull GameCurrency currency) {
        return currency != GameCurrency.COINS;
    }
}
