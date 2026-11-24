package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LumberjackKitItem extends BaseKit {
    public LumberjackKitItem() {
        super(5, "kit_lumberjack", new KitCategory("lumberjack_category"), "LumberJack", Material.IRON_AXE, 255000);

        addItem(ItemUtil.getBuilder(Material.IRON_AXE)
                .addEnchantment(Enchantment.DAMAGE_ALL, 2)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(new ItemStack(Material.GOLDEN_APPLE, 2), 1);

        addItem(new ItemStack(Material.SANDSTONE, 64), 2);
        addItem(new ItemStack(Material.SANDSTONE, 64), 3);
        addItem(new ItemStack(Material.SANDSTONE, 64), 4);

        addItem(new ItemStack(Material.ENDER_PEARL, 2), 5);
        addItem(new ItemStack(Material.WEB, 4), 6);

        setArmor(new ItemStack[]{
                ItemUtil.getBuilder(Material.LEATHER_BOOTS).setColor(org.bukkit.Color.RED).build(),
                ItemUtil.getBuilder(Material.LEATHER_LEGGINGS).setColor(org.bukkit.Color.RED).build(),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                new ItemStack(Material.CHAINMAIL_HELMET),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
