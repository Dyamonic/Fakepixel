package club.abelix.games.buildffa.game.kit.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.impl.kit.KitCategory;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FishermanKitItem extends BaseKit {
    public FishermanKitItem() {
        super(
                6,
                "kit_fisherman",
                new KitCategory("fisherman_category"),
                "Fisherman",
                Material.FISHING_ROD,
                125000,
                "buildffa.kit.free.fisherman"
        );

        addItem(ItemUtil.getBuilder(Material.GOLD_SWORD)
                .addEnchantment(Enchantment.DAMAGE_ALL, 2)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 0);

        addItem(ItemUtil.getBuilder(Material.FISHING_ROD)
                .addEnchantment(Enchantment.DURABILITY, 10)
                .build(), 1);

        addItem(ItemUtil.getBuilder(Material.STICK)
                .addEnchantment(Enchantment.KNOCKBACK, 2)
                .build(), 2);

        addItem(new ItemStack(Material.SANDSTONE, 64), 3);
        addItem(new ItemStack(Material.SANDSTONE, 64), 4);
        addItem(new ItemStack(Material.SANDSTONE, 64), 5);

        addItem(new ItemStack(Material.WEB, 4), 6);
        addItem(new ItemStack(Material.LADDER, 4), 7);
        addItem(new ItemStack(Material.ENDER_PEARL, 1), 8);

        setArmor(new ItemStack[]{
                ItemUtil.getBuilder(Material.LEATHER_BOOTS).setColor(org.bukkit.Color.FUCHSIA).build(),
                ItemUtil.getBuilder(Material.LEATHER_LEGGINGS).setColor(org.bukkit.Color.FUCHSIA).build(),
                new ItemStack(Material.IRON_CHESTPLATE),
                ItemUtil.getBuilder(Material.LEATHER_HELMET).setColor(org.bukkit.Color.FUCHSIA).build(),
        });
    }

    @Override
    public void onSelect(Player player) {
        apply(Gamer.getGamer(player));
    }
}
