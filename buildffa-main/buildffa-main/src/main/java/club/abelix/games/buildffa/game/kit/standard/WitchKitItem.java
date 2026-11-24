package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WitchKitItem extends BaseKit {
    public WitchKitItem() {
        super(1, "kit_witch", new KitCategory("witch_category"), "Witch", Material.POTION, 125000);

        addItem(ItemUtil.getBuilder(Material.GOLD_SWORD)
                .addEnchantment(Enchantment.DAMAGE_ALL, 2)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(ItemUtil.getBuilder(Material.STICK)
                .addEnchantment(Enchantment.KNOCKBACK, 1)
                .build(), 1);

        addItem(new ItemStack(Material.SANDSTONE, 64), 2);
        addItem(new ItemStack(Material.SANDSTONE, 64), 3);
        addItem(new ItemStack(Material.SANDSTONE, 64), 4);

        addItem(new ItemStack(Material.WEB, 4), 5);
        addItem(new ItemStack(Material.ENDER_PEARL, 2), 6);
        addItem(new ItemStack(Material.POTION, 2, (short) 8197), 7); // Мгновенное лечение II
        addItem(new ItemStack(Material.POTION, 2, (short) 8226), 8); // Скорость II (1 минута)
        addItem(new ItemStack(Material.POTION, 2, (short) 8261), 9); // Прыгучесть II (1 минута)
        addItem(new ItemStack(Material.POTION, 1, (short) 16396), 10); // Моментальный урон II

        setArmor(new ItemStack[]{
                ItemUtil.getBuilder(Material.LEATHER_BOOTS).setColor(org.bukkit.Color.GREEN).build(),
                ItemUtil.getBuilder(Material.LEATHER_LEGGINGS).setColor(org.bukkit.Color.GREEN).build(),
                new ItemStack(Material.IRON_CHESTPLATE),
                ItemUtil.getBuilder(Material.LEATHER_HELMET).setColor(org.bukkit.Color.GREEN).build(),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
