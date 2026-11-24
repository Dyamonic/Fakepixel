package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TankKitItem extends BaseKit {
    public TankKitItem() {
        super(11, "kit_tank", new KitCategory("tank_category"), "Tank", Material.IRON_CHESTPLATE, 190000);

        addItem(ItemUtil.getBuilder(Material.WOOD_SWORD)
                .addEnchantment(Enchantment.DAMAGE_ALL, 1)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(new ItemStack(Material.SANDSTONE, 64), 1);
        addItem(new ItemStack(Material.SANDSTONE, 64), 2);
        addItem(new ItemStack(Material.SANDSTONE, 64), 3);

        setArmor(new ItemStack[]{
                ItemUtil.getBuilder(Material.CHAINMAIL_BOOTS).addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3).build(),
                ItemUtil.getBuilder(Material.IRON_LEGGINGS).addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3).build(),
                ItemUtil.getBuilder(Material.IRON_CHESTPLATE).addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3).build(),
                ItemUtil.getBuilder(Material.CHAINMAIL_HELMET).addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3).build(),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
