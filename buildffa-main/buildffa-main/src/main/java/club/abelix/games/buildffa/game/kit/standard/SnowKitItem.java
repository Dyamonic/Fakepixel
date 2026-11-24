package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SnowKitItem extends BaseKit {
    public SnowKitItem() {
        super(3, "kit_snow", new KitCategory("snow_category"), "Snow", Material.SNOW_BALL, 190000);

        addItem(ItemUtil.getBuilder(Material.GOLD_SWORD)
                .addEnchantment(Enchantment.DAMAGE_ALL, 2)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(ItemUtil.getBuilder(Material.STICK)
                .addEnchantment(Enchantment.KNOCKBACK, 2)
                .build(), 1);

        addItem(new ItemStack(Material.SANDSTONE, 64), 2);
        addItem(new ItemStack(Material.SANDSTONE, 64), 3);
        addItem(new ItemStack(Material.SANDSTONE, 64), 4);

        addItem(new ItemStack(Material.SNOW_BALL, 64), 5);
        addItem(new ItemStack(Material.SNOW_BALL, 64), 6);
        addItem(new ItemStack(Material.SNOW_BALL, 64), 7);

        addItem(new ItemStack(Material.ENDER_PEARL, 2), 8);

        setArmor(new ItemStack[]{
                new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.IRON_LEGGINGS),
                ItemUtil.getBuilder(Material.LEATHER_CHESTPLATE).setColor(org.bukkit.Color.WHITE).build(),
                ItemUtil.getBuilder(Material.LEATHER_HELMET).setColor(org.bukkit.Color.WHITE).build(),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
