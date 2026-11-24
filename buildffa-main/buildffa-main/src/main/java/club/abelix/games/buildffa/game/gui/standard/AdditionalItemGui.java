package club.abelix.games.buildffa.game.gui.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.games.buildffa.game.manager.additional.AdditionalItemManager;
import club.abelix.games.buildffa.game.purchase.Purchase;
import club.abelix.games.buildffa.game.utility.ConvertUtility;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;

public class AdditionalItemGui extends Gui {
    private final Language language;
    private final AdditionalItemManager itemManager;

    public AdditionalItemGui(Gui parent, Player player) {
        super(player, Lang.of("buildffa_additional_item_shop_gui_title"), 6);

        setParent(parent);

        this.language = Gamer.getGamer(player).getLanguage();
        this.itemManager = AdditionalItemManager.getInstance();

        draw();
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        addItem(Material.ENDER_PEARL, 1, 20, "additional_ender_pearl", 2250);
        addItem(Material.GOLDEN_APPLE, 1, 21, "additional_golden_apple", 1500);
        addItem(Material.WEB, 1, 23, "additional_cobweb", 750);
        addItem(Material.ARROW, 1, 24, "additional_arrow", 1050);

        addPotion(PotionEffectType.SPEED, 60, 1, 39, "additional_speed_potion", 1800);
        addPotion(PotionEffectType.JUMP, 60, 1, 40, "additional_jump_potion", 1800);
        addPotion(PotionEffectType.HEAL, 1, 1, 41, "additional_healing_potion", 1500);
    }

    private void addItem(Material material, int amount, int slot, String nameKey, int price) {
        String name = language.getMessage(nameKey);
        String lore = language.getMessage("additional_item_price", "%price%", price, "%golds%", ConvertUtility.toGold(price));

        ItemStack item = ItemUtil.getBuilder(material)
                .setAmount(amount)
                .setName(name)
                .setLore(lore)
                .build();

        add(item, slot, event -> handleItemPurchase((Player) event.getWhoClicked(), item, price));
    }

    private void addPotion(PotionEffectType effectType, int duration, int amplifier, int slot, String nameKey, int price) {
        ItemStack potion = new ItemStack(Material.POTION, 1, (short) 16384);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        if (meta != null) {
            String name = language.getMessage(nameKey);
            String lore = language.getMessage("additional_item_price", "%price%", price, "%golds%", ConvertUtility.toGold(price));

            meta.addCustomEffect(new PotionEffect(effectType, duration * 20, amplifier), true);
            meta.setDisplayName(name);
            meta.setLore(Collections.singletonList(lore));

            short damageValue = 16384;
            if (effectType == PotionEffectType.SPEED) {
                damageValue += 8194;
            } else if (effectType == PotionEffectType.JUMP) {
                damageValue += 8235;
            } else if (effectType == PotionEffectType.HEAL) {
                damageValue += 8229;
            }

            potion.setDurability(damageValue);
            potion.setItemMeta(meta);
        }

        add(potion, slot, event -> handleItemPurchase((Player) event.getWhoClicked(), potion, price));
    }

    private void handleItemPurchase(Player player, ItemStack item, int price) {
        val purchase = Purchase.builder()
                .onSuccess(() -> itemManager.addItem(player, item.clone()))
                .coinPrice(price)
                .player(player)
                .build();

        purchase.execute();
    }
}
