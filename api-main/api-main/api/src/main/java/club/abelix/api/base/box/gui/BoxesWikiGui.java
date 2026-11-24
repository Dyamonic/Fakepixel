package club.abelix.api.base.box.gui;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BoxesWikiGui extends Gui {

    public BoxesWikiGui(@NonNull Player player) {
        super(player, Lang.of("boxes_wiki_gui"), 3);

        draw();
    }

    @Override
    public void draw() {
        add(ItemUtil.getBuilder(Material.BOOK)
                .setName(translated("boxes_wiki_what_is_this"))
                .setLore(translatedLore("boxes_wiki_what_is_this_lore"))
                .build(), 10
        );

        add(ItemUtil.getBuilder(Material.ENDER_CHEST)
                .setName(translated("boxes_wiki_how_to_get"))
                .setLore(translatedLore("boxes_wiki_how_to_get_lore"))
                .build(), 12
        );

        add(ItemUtil.getBuilder(Material.DIAMOND)
                .setName(translated("boxes_wiki_chances"))
                .setLore(translatedLore("boxes_wiki_chances"))
                .build(), 14
        );

        add(ItemUtil.getBuilder(Material.CHEST)
                .setName(translated("boxes_wiki_how_to_open"))
                .setLore(translatedLore("boxes_wiki_how_to_open_lore"))
                .build(), 16
        );

    }
}
