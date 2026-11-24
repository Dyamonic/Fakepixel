package club.abelix.api.base.item.impl.kit;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.actionitem.ActionItem;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.Applicable;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.util.item.MaterialUtil;
import club.abelix.api.base.util.nms.locale.LocaleUtil;
import club.abelix.commons.language.Language;
import com.google.common.collect.Iterables;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Created by saharNooby on 19:57 18.10.2017
 */
public abstract class Kit extends GameItem implements Applicable {

    // Кит - набор вещей, который покупается за монеты режима.
    // От этого класса должны наследоваться киты конкретных миниигр

    protected final List<ItemStack> items = new ArrayList<>();

    protected Kit(int id, @NonNull String localizationKey, @NonNull KitCategory category) {
        super(id, localizationKey, category);
    }

    public Kit add(@NonNull ItemStack item) {
        this.items.add(item);

        return this;
    }

    public Kit add(ItemStack... items) {
        this.items.addAll(Arrays.asList(items));

        return this;
    }

    public Kit add(Material... types) {
        for (Material type : types) {
            this.items.add(new ItemStack(type));
        }

        return this;
    }

    public Kit addBook(Object... enchants) {
        return add(Material.ENCHANTED_BOOK).enchant(enchants);
    }

    public Kit addPotion(PotionType type, boolean splash, boolean extended, boolean upgraded) {
        return addPotion(type, splash, extended, upgraded, 1);
    }

    public Kit addPotion(PotionType type, boolean splash, boolean extended, boolean upgraded, int amount) {
        add(FalconAPI.getNmsApi().getPlayerApi().getPotion(type, splash, extended, upgraded, amount));

        return this;
    }

    public Kit generateDescription() {
        for (Language language : Language.VALUES) {
            generateDescription(language);
        }

        return this;
    }

    /**
     * Генерирует описание кита исходя из списка вещей. Имя каждой вещи -- имя в русской локализации Minecraft.
     */
    public void generateDescription(Language language) {
        for (ItemStack item : this.items) {
            if (item.getType() == Material.POTION) {
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                Collection<PotionEffect> effects = meta.getCustomEffects();

                boolean printEffects = false;

                String line = ChatColor.GRAY + LocaleUtil.translate(language, item);

                if (meta.hasDisplayName()) {
                    line += "§7: " + item.getItemMeta().getDisplayName();
                } else if (effects.size() == 1) {
                    line += "§7: " + potion(language, Iterables.getOnlyElement(effects));
                } else {
                    printEffects = true;
                }

                if (!printEffects) {
                    line += " x" + item.getAmount();
                }

                descr(language, line);

                if (printEffects) {
                    List<String> lines = new ArrayList<>(effects.size());

                    for (PotionEffect effect : effects) {
                        lines.add(" §8" + potion(language, effect));
                    }

                    Collections.sort(lines);

                    descr(language, lines);
                }
            } else if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                Map<Enchantment, Integer> enchantments;

                if (meta instanceof EnchantmentStorageMeta) {
                    enchantments = ((EnchantmentStorageMeta) meta).getStoredEnchants();
                } else {
                    enchantments = meta.getEnchants();
                }

                List<String> lines = new ArrayList<>(enchantments.size());

                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    lines.add("  §7∙ " + enchantment(language, entry.getKey(), entry.getValue()));
                }

                Collections.sort(lines);

                descr(language, ChatColor.GRAY + (meta.hasDisplayName()
                        ? meta.getDisplayName()
                        : LocaleUtil.translateMultiple(language, item, "x%d")));
                descr(language, lines);
            } else {
                descr(language, ChatColor.GRAY + LocaleUtil.translateMultiple(language, item, "x%d"));
            }
        }

    }

    private String enchantment(Language language, Enchantment enchantment, int level) {
        return LocaleUtil.translateWithLevel(language, enchantment, level, "%s %s");
    }

    private String potion(Language language, PotionEffect effect) {
        return effect.getDuration() > 0
                ? LocaleUtil.translateWithAmplifierAndDuration(language, effect.getType(), effect.getAmplifier() + 1, effect.getDuration())
                : LocaleUtil.translateWithAmplifier(language, effect.getType(), effect.getAmplifier() + 1);
    }

    // enchant last line
    public Kit enchant(Object... enchants) {
        if (enchants.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid array " + Arrays.toString(enchants));
        }

        if (items.isEmpty()) {
            throw new IllegalStateException("No line were added");
        }

        ItemStack item = items.get(items.size() - 1);

        ItemMeta meta = item.hasItemMeta() ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());

        for (int i = 0; i < enchants.length; i += 2) {
            try {
                Enchantment ench = (Enchantment) enchants[i];
                Integer level = (Integer) enchants[i + 1];

                if (meta instanceof EnchantmentStorageMeta) {
                    ((EnchantmentStorageMeta) meta).addStoredEnchant(ench, level, true);
                } else {
                    meta.addEnchant(ench, level, true);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid array " + Arrays.toString(enchants), e);
            }
        }

        item.setItemMeta(meta);

        return this;
    }

    public List<ItemStack> getItems(Player player) {
        return new ArrayList<>(items);
    }

    @Override
    public void apply(@NotNull Gamer gamer) {
        Player player = gamer.getPlayer();

        PlayerInventory inventory = player.getInventory();

        List<ItemStack> items = getItems(player);
        Collections.reverse(items);

        for (ItemStack stack : getItems(player)) {
            Material material = stack.getType();

            boolean applyHead = material.name().contains("SKULL") && !ActionItem.hasActionItem(stack);

            if (applyHead || MaterialUtil.isHelmet(material)) {
                inventory.setHelmet(stack);
            } else if (MaterialUtil.isChestplate(material)) {
                inventory.setChestplate(stack);
            } else if (MaterialUtil.isLeggings(material)) {
                inventory.setLeggings(stack);
            } else if (MaterialUtil.isBoots(material)) {
                inventory.setBoots(stack);
            } else {
                inventory.addItem(stack);
            }
        }
    }
}
