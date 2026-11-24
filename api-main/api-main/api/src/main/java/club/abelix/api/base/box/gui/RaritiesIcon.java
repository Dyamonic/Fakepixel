package club.abelix.api.base.box.gui;

import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.cases.rarity.Rarity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class RaritiesIcon {

    private static final EnumMap<Rarity, ItemStack> ICONS = new EnumMap<>(Rarity.class) {{
        put(
                Rarity.NONE,
                ItemUtil.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmQ0M2Y5ZGI2MWJlZTk0MTUyZTE2M2UxZjMxNjgwODlmYTk4YmNmNmRhNzg5ZmRhNGM5NmIzZjY5ZTNiZWQwYyJ9fX0=")
        );

        put(
                Rarity.COMMON,
                ItemUtil.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODhiMDIxNDE4OWI1YWY4ZTczNWM5ZDIyZWJjYmY2ZTI5N2E2NTBlODdlZTUzYTkyNzBmZDcyNGM3YjMyOGZmYSJ9fX0=")
        );

        put(
                Rarity.RARE,
                ItemUtil.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODAwOTcwY2RlY2ZhMGJjNTM2NTllYTI4Zjk5NDg3MDE1NWYwZDRlMjUyNmE1MTMzOTI0NTU5MzllNTBjYzhhYiJ9fX0=")
        );

        put(
                Rarity.EPIC,
                ItemUtil.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmMwMWIxMjg3ODNjNDU5MGZjNWUyODI5ZjhjZTFmOWY5ZDI0YWEwMjU2MWY5MmZmZGI3NDRhMDc3NWZiMDRkZSJ9fX0=")
        );

        put(
                Rarity.LEGENDARY,
                ItemUtil.getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODRlZWQzMGM4OWZhODA2YmM2MjRiYmU1NjY1YzIxMzBiZDkyZWYxODJjOTRiMjIzMDE0MDhiZjk3NGVkZmE0ZiJ9fX0=")
        );
    }};

    public static ItemStack find(Rarity rarity) {
        return ICONS.get(rarity);
    }
}
