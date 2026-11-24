package club.abelix.api.server.base.shop.category.booster;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.multiplier.booster.BoosterIcons;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.base.shop.ShopCategory;
import club.abelix.commons.data.booster.BoosterType;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.CommonWords;
import gnu.trove.list.array.TIntArrayList;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>BoosterCategory creation on 09.03.2024 at 02:39</p>
 */

// todo: Как-то надо переписать этот пиздец, но сейчас нету времени
public class BoosterCategory extends ShopCategory {
    public BoosterCategory(@NonNull Player player, @NonNull ItemStack item) {
        super(player, "donate_description_gui_title", item);
        draw();
    }

    @Override
    public void drawCategory() {
        val slots = new TIntArrayList();
        val boosters = BoosterType.values();
        slots.add(new int[] {10, 12, 20, 14, 16, 24});


        for (val booster : boosters) {
            int slot = slots.removeAt(0);
            drawBooster(booster, slot);
        }

    }

    private void drawBooster(BoosterType type, int slot) {
        Language language = getLanguage();

        int amount = getGamer().getProperty(type.name());

        ItemStack item = ItemUtil.getBuilder(BoosterIcons.getIcon(type))
                .setName(translated("booster_gui_name",
                        "%booster%", type.getDisplayName(language))
                )
                .addLore(translatedLore("booster_available_count_lore",
                        "%amount%", amount,
                        "%format%", language.getEnding(amount, "booster"))
                )
                .build();

        item.setAmount(Math.min(Math.max(amount, 1), 64));

        add(processIcon(type, amount, item), slot, getAction(type));
    }

    protected ItemStack processIcon(@NonNull BoosterType type, int amount, @NonNull ItemStack icon) {
        int gold = getGamer().getGold();

        ItemUtil.Builder itemBuilder = ItemUtil.getBuilder(icon);

        itemBuilder.addLore(translatedLore("booster_price_lore",
                "%cost%", CommonWords.GOLD.get(getLanguage()).withNumber(type.getCostPlazma()),
                "%balance%", CommonWords.PLAZMA.get(getLanguage()).withNumber(gold))
        );

        if (gold < type.getCostPlazma()) {
            itemBuilder = itemBuilder.addLore(translatedLore("purchase_unavailable_cost_lore"));
        } else {
            itemBuilder = itemBuilder.addLore(translatedLore("booster_click_to_purchase"));
        }

        return itemBuilder.build();
    }


    protected ClickAction getAction(@NonNull BoosterType type) {
        return e -> {
            Gamer gamer = getGamer();

            int gold = gamer.getGold();

            if (gold < type.getCostPlazma()) {
                gamer.sendLocalizedMessage("purchase_unavailable_cost_message");
                return;
            }

            gamer.takeGold(type.getCostPlazma());
            gamer.addProperty(type.name(), 1);

            gamer.sendLocalizedMessage("booster_purchase", "%name%", type.getDisplayName(gamer.getLanguage()));

            draw();
        };
    }
}
