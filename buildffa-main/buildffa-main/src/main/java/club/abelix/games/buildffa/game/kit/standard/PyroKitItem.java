package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PyroKitItem extends BaseKit {
    public PyroKitItem() {
        super(
                4,
                "kit_pyro",
                new KitCategory("pyro_category"),
                "Pyro",
                Material.FLINT_AND_STEEL,
                125000,
                "buildffa.kit.free.pyro"
        );

        addItem(ItemUtil.getBuilder(Material.GOLD_SWORD)
                .addEnchantment(Enchantment.FIRE_ASPECT, 2)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(new ItemStack(Material.FLINT_AND_STEEL, 1), 1);

        addItem(new ItemStack(Material.SANDSTONE, 64), 2);
        addItem(new ItemStack(Material.SANDSTONE, 64), 3);
        addItem(new ItemStack(Material.SANDSTONE, 64), 4);

        addItem(new ItemStack(Material.ENDER_PEARL, 1), 5);

        setArmor(new ItemStack[]{
                ItemUtil.getBuilder(Material.LEATHER_BOOTS).setColor(org.bukkit.Color.YELLOW).build(),
                ItemUtil.getBuilder(Material.LEATHER_LEGGINGS).setColor(org.bukkit.Color.YELLOW).build(),
                new ItemStack(Material.IRON_CHESTPLATE),
                ItemUtil.getBuilder(Material.LEATHER_HELMET).setColor(org.bukkit.Color.YELLOW).build(),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
