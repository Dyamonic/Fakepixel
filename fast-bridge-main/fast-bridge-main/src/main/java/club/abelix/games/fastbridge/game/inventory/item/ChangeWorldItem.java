package club.abelix.games.fastbridge.game.inventory.item;

import club.abelix.api.base.actionitem.AbstractActionItem;
import club.abelix.api.base.actionitem.ActionItem;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.games.fastbridge.game.gui.WorldChangeGui;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ChangeWorldItem extends AbstractActionItem {

    public ChangeWorldItem() {
        super(ItemUtil.getBuilder(Material.COMPASS)
                .setName(Lang.of("fastbridge_menu_item_title"))
                .setLore(Lang.of("fastbridge_menu_item_lore"))
                .build());
    }

    public static ItemStack getItemStack() {
        return new ChangeWorldItem().getActionItem().getItemStack();
    }

    @Override
    public void handle(@NonNull ActionItem actionItem) {
        actionItem.setInteractHandler(event -> {
            event.setCancelled(true);

            new WorldChangeGui(Gamer.getGamer(event.getPlayer())).draw();
        });
    }
}
