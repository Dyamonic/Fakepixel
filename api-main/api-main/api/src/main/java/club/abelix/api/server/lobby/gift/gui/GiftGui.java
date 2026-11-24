package club.abelix.api.server.lobby.gift.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.Heads;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class GiftGui extends Gui {

    Gamer targetGamer;

    public GiftGui(@NonNull Gamer gamer, @NonNull Gamer targetGamer) {
        super(gamer, Lang.of("gift_gui_title"), 4);

        this.targetGamer = targetGamer;

        draw();
    }

    @Override
    public void draw() {
        final Language language = getLanguage();

        add(ItemUtil.getSkullBuilder(Heads.Celebrations_BluePresent.getValue())
                .setName(language.getMessage("gift_gui_gift_rank_name"))
                .setLore(language.getMessageList("gift_gui_gift_rank_lore", "%player%", targetGamer.getPlayerName()))
                .build(), 13, (event) -> new RankGiftGui(gamer, targetGamer));

        generateCloseItem(31);
    }

}
