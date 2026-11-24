package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AssassinKitItem extends BaseKit {
    public AssassinKitItem() {
        super(9, "kit_assassin", new KitCategory("assassin_category"), "Assassin", Material.IRON_SWORD, 255000);

        addItem(ItemUtil.getBuilder(Material.IRON_SWORD)
                .addEnchantment(Enchantment.DAMAGE_ALL, 2)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(new ItemStack(Material.SANDSTONE, 64), 2);
        addItem(new ItemStack(Material.SANDSTONE, 64), 3);
        addItem(new ItemStack(Material.SANDSTONE, 64), 4);

        addItem(new ItemStack(Material.ENDER_PEARL, 1), 5);
        addItem(new ItemStack(Material.GOLDEN_APPLE, 2), 1);
        addItem(new ItemStack(Material.WEB, 6), 5);

        setArmor(new ItemStack[]{
                ItemUtil.getBuilder(Material.LEATHER_BOOTS).setColor(org.bukkit.Color.GRAY).build(),
                ItemUtil.getBuilder(Material.LEATHER_LEGGINGS).setColor(org.bukkit.Color.GRAY).build(),
                new ItemStack(Material.IRON_CHESTPLATE),
                ItemUtil.getBuilder(Material.LEATHER_HELMET).setColor(org.bukkit.Color.GRAY).build(),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
