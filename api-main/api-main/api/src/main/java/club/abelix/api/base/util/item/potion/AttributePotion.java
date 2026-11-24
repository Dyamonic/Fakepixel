package club.abelix.api.base.util.item.potion;

import club.abelix.api.FalconAPI;
import club.abelix.api.nms.nbt.tag.CompoundTag;
import club.abelix.commons.language.Lang;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionType;

/**
 * @author Unidentified Person
 */
public class AttributePotion {

    static {
        new AttributePotionListener(FalconAPI.getInstance());
    }

    private final ItemStack item;

    public AttributePotion(PotionType type, String attributeName, double attributeModifier, int duration) {
        ItemStack item = new ItemStack(Material.POTION, 1, (short) type.getDamageValue());

        CompoundTag tag = FalconAPI.getNmsApi().getNbtApi().getItemTag(item);

        CompoundTag attribute = tag.newCompound("attribute-potion");
        attribute.setShort("duration", duration);
        attribute.setByte("effect", type.getEffectType().getId());
        attribute.setString("name", attributeName);
        attribute.setDouble("mod", attributeModifier);

        this.item = item = FalconAPI.getNmsApi().getNbtApi().setItemTag(item, tag);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Lang.of(FalconAPI.getNmsApi().getPlayerApi().getEffectLocaleName(type.getEffectType())).compile());

        item.setItemMeta(meta);
    }

    public ItemStack getItem() {
        return item.clone();
    }

}
