package club.abelix.games.buildffa.game.item;

import club.abelix.api.base.actionitem.AbstractActionItem;
import club.abelix.api.base.actionitem.ActionItem;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LimeDyeActionItem extends AbstractActionItem {

    public LimeDyeActionItem(Language language) {
        super(createInitialItem(language));
    }

    private static ItemStack createInitialItem(Language language) {
        return ItemUtil.getBuilder(Material.INK_SACK)
                .setName(language.getMessage("hide_players_display_name"))
                .build();
    }

    public static ItemStack getItemStack(Language language) {
        return new LimeDyeActionItem(language).getActionItem().getItemStack();
    }

    @Override
    public void handle(@NonNull ActionItem actionItem) {
        actionItem.setInteractHandler(event -> {
            event.getPlayer().sendMessage("TODO: Это будет скрывать игроков");
        });
    }


    private void updatePlayerDyeItem(Player player, ItemStack item, boolean hiding, Language language) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = ItemUtil.getBuilder(item).build().getItemMeta();
        }

        if (hiding) {
            meta.setDisplayName(language.getMessage("show_players_display_name"));
        } else {
            meta.setDisplayName(language.getMessage("hide_players_display_name"));
        }

        item.setItemMeta(meta);
        player.getInventory().setItem(player.getInventory().getHeldItemSlot(), item);
    }
}