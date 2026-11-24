package club.abelix.api.server.base.shop.category.exchanger;

import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.head.HeadTexture;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.base.shop.ShopCategory;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ExchangerCategory extends ShopCategory {

    public ExchangerCategory(@NonNull Player player, @NonNull ItemStack item) {
        super(player, "exchanger_description_gui_title", item);
        draw();
    }

    @Override
    public void drawCategory() {
        if (CoreUtil.isHub()) {
            add(ItemUtil.getSkullBuilder(HeadTexture.SOMETHING.getTexture())
                    .setName(Lang.of("item_only_for_modes_name"))
                    .setLore(Lang.of("item_only_for_modes_lore"))

                    .build(), 13);
            return;
        }

        draw(1, 20);
        draw(5, 21);
        draw(10, 22);
        draw(20, 23);
        draw(50, 24);
    }

    public void draw(int count, int slot) {
        val canExchange = gamer.hasGold(count);
        val result = Exchanger.getResult(count);

        add(ItemUtil.getBuilder(new ItemStack(Material.GOLD_BLOCK))
                .setAmount(count)
                .setName(language.getMessage("exchange_gold_to_coins_item_name", "%color%", canExchange ? 'a' : 'c', "%golds%", count, "%coins%", result))
                .setLore(language.getMessageList("exchange_gold_to_coins_item_lore", "%golds%", count, "%coins%", result))
                .addLore(Lang.of(canExchange ? "shop_buy_mysteryboxes_button_can_buy" : "shop_buy_mysteryboxes_button_dont_have_money"))
                .build(), slot, (event) -> {
            if (canExchange) Exchanger.convertGoldToCoins(gamer, count);
            else gamer.sendLocalizedMessage("purchase_unavailable_cost_message");
        });
    }
}
