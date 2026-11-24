package club.abelix.api.server.lobby.gift.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.base.gui.ConfirmationGui;
import club.abelix.api.server.lobby.gift.GiftManager;
import club.abelix.api.server.lobby.gift.data.GiftType;
import club.abelix.commons.language.Lang;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.bukkit.Material;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RankGiftGui extends Gui {

    static final String GREEN_PRESENT = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlNDJlMzcyNWMyYjRhZTY5MDA1ODBjNGUyYTZiODMwZjZlY2EwMjExZjdhMzY0MTQzM2ZjNjdmYmM0M2QzZiJ9fX0=";
    static final String BLUE_PRESENT = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDZlMTJmZGFlMWZjZWJhNjg3OWY2NTk3OTYxMzJhN2ZmYTA4Y2Q5MmEyNmNiN2ExMDY3ZDQ5NzAzZDdiMWI0YiJ9fX0=";
    static final String PURPLE_PRESENT = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjBiNDVjYmZiZGZiNmRjNjY5NGRlOTZkOTdkYTM2YWU3YjVmZTNjZDk0YTViMjYyNTA1NTRjNWYwMjJjYTdkMCJ9fX0=";
    static final String RED_PRESENT = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTcyNmQ5ZDA2MzJlNDBiZGE1YmNmNjU4MzliYTJjYzk4YTg3YmQ2MTljNTNhZGYwMDMxMGQ2ZmM3MWYwNDJiNSJ9fX0=";

    @Getter
    Gamer targetGamer;

    public RankGiftGui(@NonNull Gamer gamer, @NonNull Gamer targetGamer) {
        super(gamer, Lang.of("gift_rank_title"), 6);

        this.targetGamer = targetGamer;

        draw();
    }

    @Override
    public void draw() {
        if (!targetGamer.isOnline()) {
            gamer.getPlayer().closeInventory();
            gamer.sendLocalizedMessage("gift_player_not_online");

            return;
        }

        add(ItemUtil.getBuilder(targetGamer.getSkull())
                .setName(targetGamer.getDisplayName())
                .setLore(language.getMessageList("gift_rank_lore"))
                .build(), 13);

        drawRanks();

        generateBackItem(48, event -> openPreviousGui());
        generateCloseItem(49);

        add(ItemUtil.getBuilder(Material.PAPER)
                .setName(translated("gift_rank_gui_rewards_name"))
                .setLore(translatedLore("gift_rank_gui_rewards_lore"))
                .build(), 50, __ -> new BonusesGui(player));

        add(ItemUtil.getBuilder(Material.GOLD_NUGGET)
                .setName(gamer.getLanguage().getMessage("shop_nugget_name", "%gold%", gamer.getGold()))
                .setLore(Lang.of("shop_nugget_lore"))
                .build(), 53);
    }

    public void drawRanks() {
        drawItem(21, GREEN_PRESENT, GiftType.VIP);
        drawItem(22, GREEN_PRESENT, GiftType.VIP_PLUS);
        drawItem(23, BLUE_PRESENT, GiftType.MVP);
        drawItem(30, BLUE_PRESENT, GiftType.MVP_PLUS);

        add(ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability(3)
                .setSkullValue(PURPLE_PRESENT)
                .setName(language.getMessage("gift_rank_sponsor_name"))
                .setLore(language.getMessageList("donate_description_gui_sponsor_lore"))
                .glowing()
                .build(), 31, event -> new SponsorGiftGui(gamer, targetGamer));

        add(ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability(3)
                .setSkullValue(RED_PRESENT)
                .setName(language.getMessage("gift_rank_special_name"))
                .setLore(language.getMessageList("donate_description_gui_special_lore"))
                .glowing()
                .build(), 32, event -> new SpecialGiftGui(gamer, targetGamer));
    }

    public void openPreviousGui() {
        new GiftGui(gamer, targetGamer);
    }

    private void drawItem(int slot, String headValue, GiftType type) {
        String actionLine;

        if (targetGamer.getRanks().hasRank(type.getRankId())) {
            actionLine = language.getMessage("gift_rank_item_already_owned");
        } else if (!gamer.hasGold(type.getPrice())) {
            actionLine = language.getMessage("gift_rank_not_enough_coins");
        } else {
            actionLine = language.getMessage("gift_rank_item_buy");
        }

        add(ItemUtil.getBuilder(Material.SKULL_ITEM)
                .setDurability(3)
                .setSkullValue(headValue)
                .setName(language.getMessage("gift_rank_" + type.name().toLowerCase() + "_name"))
                .setLore(language.getMessageList("donate_description_gui_" + type.name().toLowerCase() + "_lore",
                        "%price%", type.getPrice()))
                .addLore(actionLine)
                .glowing(type.name().contains("PLUS"))
                .build(), slot, event -> {
            if (targetGamer.getRanks().hasRank(type.getRankId())) {
                gamer.getPlayer().closeInventory();
                gamer.sendLocalizedMessage("gift_rank_item_already_owned");
                return;
            }

            if (!gamer.hasGold(type.getPrice())) {
                gamer.getPlayer().closeInventory();
                gamer.sendLocalizedMessage("gift_rank_not_enough_coins");
                return;
            }

            sendGift(type);
        });
    }

    public void sendGift(GiftType type) {
        if (!targetGamer.isOnline()) {
            gamer.getPlayer().closeInventory();
            gamer.sendLocalizedMessage("gift_player_not_online");

            return;
        }

        new ConfirmationGui(player, (event) -> {
            GiftManager.INSTANCE.sendGift(gamer, targetGamer, type);
        });
    }

}
