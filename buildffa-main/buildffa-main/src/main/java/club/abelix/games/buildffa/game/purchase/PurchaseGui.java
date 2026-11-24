package club.abelix.games.buildffa.game.purchase;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.network.type.mode.Uncommon;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public final class PurchaseGui extends Gui {
    private final Purchase purchase;

    public PurchaseGui(final @NonNull Player player, final @NotNull Purchase purchase) {
        super(player, Lang.of("buildffa_purchase_gui_title"), 4);

        this.purchase = purchase;

        draw();
    }

    @Override
    public void draw() {
        drawDynamicBorders();

        val coinsPrice = purchase.getCoinPrice();
        val goldPrice = purchase.getResolvedGoldPrice();

        val goldItem = ItemUtil.getBuilder(Material.GOLD_INGOT)
                .setName(Lang.of("buildffa_purchase_gui_gold_name"))
                .setLore(Lang.of("buildffa_purchase_gui_gold_lore").setAll("%golds%", goldPrice))
                .build();

        val coinsItem = ItemUtil.getBuilder(Material.EMERALD)
                .setName(Lang.of("buildffa_purchase_gui_coins_name"))
                .setLore(Lang.of("buildffa_purchase_gui_coins_lore").setAll("%coins%", coinsPrice))
                .build();

        add(goldItem, toSlot(2, 2), event -> {
            if (!purchase.isPurchaseAllowed()) {
                player.closeInventory();
                return;
            }

            if (gamer.getGold() < goldPrice) {
                gamer.sendLocalizedMessage("not_enough_golds_message");
                purchase.executeFailure();

                player.closeInventory();
                return;
            }

            gamer.takeGold(goldPrice);
            purchase.executeSuccess();

            player.closeInventory();
            gamer.sendLocalizedMessage("buildffa_purchase_golds_success", "%gold_bought%", goldPrice);
        });

        add(coinsItem, toSlot(6, 2), event -> {
            if (!purchase.isPurchaseAllowed()) {
                player.closeInventory();
                return;
            }

            if (gamer.getCoins(Uncommon.BUILDFFA) < coinsPrice) {
                gamer.sendLocalizedMessage("not_enough_coins_message");
                purchase.executeFailure();

                player.closeInventory();
                return;
            }

            gamer.takeCoins(Uncommon.BUILDFFA, coinsPrice);
            purchase.executeSuccess();

            player.closeInventory();
            gamer.sendLocalizedMessage("buildffa_purchase_coins_success", "%coin_bought%", coinsPrice);
        });
    }
}
