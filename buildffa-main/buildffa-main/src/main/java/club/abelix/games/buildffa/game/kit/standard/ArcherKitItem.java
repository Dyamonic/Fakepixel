package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ArcherKitItem extends BaseKit {
    public ArcherKitItem() {
        super(10, "kit_archer", new KitCategory("archer_category"), "Archer", Material.BOW, 255000);

        addItem(ItemUtil.getBuilder(Material.GOLD_SWORD)
                .addEnchantment(Enchantment.DAMAGE_ALL, 2)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(ItemUtil.getBuilder(Material.BOW)
                .addEnchantment(Enchantment.KNOCKBACK, 1)
                .build(), 1);

        addItem(ItemUtil.getBuilder(Material.STICK)
                .addEnchantment(Enchantment.KNOCKBACK, 1)
                .build(), 2);

        addItem(new ItemStack(Material.SANDSTONE, 64), 3);
        addItem(new ItemStack(Material.SANDSTONE, 64), 4);
        addItem(new ItemStack(Material.SANDSTONE, 64), 5);

        addItem(new ItemStack(Material.WEB, 4), 6);
        addItem(new ItemStack(Material.LADDER, 4), 7);
        addItem(new ItemStack(Material.ENDER_PEARL, 1), 8);
        addItem(new ItemStack(Material.ARROW, 32), 9);

        setArmor(new ItemStack[]{
                ItemUtil.getBuilder(Material.LEATHER_BOOTS).setColor(Color.WHITE).build(),
                ItemUtil.getBuilder(Material.LEATHER_LEGGINGS).setColor(Color.WHITE).build(),
                new ItemStack(Material.IRON_CHESTPLATE),
                ItemUtil.getBuilder(Material.LEATHER_HELMET).setColor(Color.WHITE).build(),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
