package club.abelix.api.server.lobby.gift.gui;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.lobby.gift.GiftBonuses;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;

public class BonusesGui extends Gui {

    public BonusesGui(@NonNull Player player) {
        super(player, "lang:bonuses_gui_title;", 5);

        PagedZone<GiftBonuses.Bonus> zone = new PagedZone<>(this) {
            @Override
            protected Collection<GiftBonuses.Bonus> getItems() {
                return GiftBonuses.getBonuses();
            }

            @Override
            protected void drawItem(GiftBonuses.Bonus bonus, int slot) {
                val localizationKey = "gift_bonus_%d_%d".formatted(bonus.exactAmount(), bonus.rankId());

                add(ItemUtil.getBuilder(Material.PAPER)
                                .setName(translated(localizationKey + "_name"))
                                .setLore(translatedLore(localizationKey + "_lore"))
                                .build(),
                        slot);
            }
        };

        zone.addSlotRect(1, 1, 7, 4);
        zone.draw();
    }
}
