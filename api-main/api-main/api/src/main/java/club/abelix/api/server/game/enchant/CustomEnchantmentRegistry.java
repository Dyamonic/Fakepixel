package club.abelix.api.server.game.enchant;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomEnchantmentRegistry {

    private static final Map<String, CustomEnchantment> ENCHANTMENTS = new HashMap<>();

    static {
        ENCHANTMENTS.put("§9Отравление I", new EffectEnchantment(PotionEffectType.POISON, 5 * 20, 0));
        ENCHANTMENTS.put("§9Замедление I", new EffectEnchantment(PotionEffectType.SLOW, 5 * 20, 0));
        ENCHANTMENTS.put("§9Усталость I", new EffectEnchantment(PotionEffectType.SLOW_DIGGING, 5 * 20, 0));
        ENCHANTMENTS.put("§9Слепота", new EffectEnchantment(PotionEffectType.BLINDNESS, 5 * 20, 0));
        // todo fill custom enchant
    }

    public static List<CustomEnchantment> getEnchantments(ItemStack item) {
        if (item == null) {
            return Collections.emptyList();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return Collections.emptyList();
        }

        List<CustomEnchantment> result = new ArrayList<>();

        for (String line : meta.getLore()) {
            CustomEnchantment ench = ENCHANTMENTS.get(line);

            if (ench != null) {
                result.add(ench);
            }
        }

        return result;
    }

}
