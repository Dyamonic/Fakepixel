package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UHCKitItem extends BaseKit {
    public UHCKitItem() {
        super(2, "kit_uhc", new KitCategory("uhc_category"), "UHC", Material.GOLDEN_APPLE, 190000);

        addItem(ItemUtil.getBuilder(Material.GOLD_SWORD)
                .addEnchantment(Enchantment.DAMAGE_ALL, 2)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(ItemUtil.getBuilder(Material.FISHING_ROD)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 1);

        addItem(new ItemStack(Material.BOW), 2);

        addItem(new ItemStack(Material.SANDSTONE, 64), 3);
        addItem(new ItemStack(Material.SANDSTONE, 64), 4);
        addItem(new ItemStack(Material.SANDSTONE, 64), 5);

        addItem(new ItemStack(Material.ENDER_PEARL, 2), 6);
        addItem(new ItemStack(Material.GOLDEN_APPLE, 1), 7);
        addItem(new ItemStack(Material.WEB, 4), 8);
        addItem(new ItemStack(Material.ARROW, 8), 9);

        setArmor(new ItemStack[]{
                new ItemStack(Material.CHAINMAIL_BOOTS),
                new ItemStack(Material.CHAINMAIL_LEGGINGS),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE),
                new ItemStack(Material.CHAINMAIL_HELMET),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
