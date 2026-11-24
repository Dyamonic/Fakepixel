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

public class GoldBox extends AbstractBox {

    private static final Map<Rarity, String> ICONS = new HashMap<>() {{
        put(Rarity.COMMON, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTI2MjlmMjY4MmRjZWUzMGY1ODU1YjFlNTQyN2NjNGJlZTczZDE4YTI3NmZhZmM1MjBkNjkzYjQwY2E4MWIyMiJ9fX0=");
        put(Rarity.RARE, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTFlNTFkY2RjNjg3ZDNhYjdkNTk5ZmI3NGZmMzYyZjkwMGRhZmQyOTRkYTExZDIyNDZmNTcxOTQ0YWQ3MzcxMSJ9fX0=");
        put(Rarity.EPIC, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjJlZTkyOWU2NDEwN2ZmYjVmYjk0NTAwZmM2NDAyZGFmZDE3ZjNiNGIxOGYxZjliMmM4YzAxZWQ5YWRhZGNjYiJ9fX0=");
        put(Rarity.LEGENDARY, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzQwNmU0NTMxOGU5YTRhNmJmZTEzMmYyMDJmZTNjZWFjMTVkMTFlYWVkYmVmMWViMDZhMzc2ZGI0MzMwOTBhOCJ9fX0=");
    }};

    public GoldBox(int goldCost, Rarity rarity, List<? super BoxReward> rewards) {
        super(
                "box_gold",
                StandardBoxType.GOLD,
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
