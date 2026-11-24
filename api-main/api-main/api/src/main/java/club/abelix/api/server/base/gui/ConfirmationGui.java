package club.abelix.api.server.base.gui;

import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ConfirmationGui extends Gui {


    public static final int[] BORDER_SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 18, 27, 36, 45, 17, 26, 35};

    private static final ItemStack YES_ITEM = ItemUtil.getBuilder(Material.STAINED_GLASS)
            .setDurability(13)
            .setName(Lang.of("confirmation_gui_yes"))
            .setLore(Lang.of("confirmation_gui_yes_lore"))
            .build();

    private static final ItemStack NO_ITEM = ItemUtil.getBuilder(Material.STAINED_GLASS)
            .setDurability(14)
            .setName(Lang.of("confirmation_gui_no"))
            .setLore(Lang.of("confirmation_gui_no_lore"))
            .build();

    private final ClickAction confirmAction;

    public ConfirmationGui(@NonNull Player player,
                           @NonNull Lang lang,
                           @NonNull ClickAction confirmAction,
                           Gui previousGui) {

        super(player, lang, 5);

        this.confirmAction = confirmAction;


        if (previousGui != null) {
            generateBackItem(toSlot(4, 4), clickEvent -> previousGui.reopen());
        }

        draw();
    }

    public ConfirmationGui(@NonNull Player player,
                           @NonNull Lang lang,
                           @NonNull ClickAction confirmAction) {

        this(player, lang, confirmAction, null);
    }

    public ConfirmationGui(@NonNull Player player, @NonNull ClickAction confirmAction) {
        this(player, Lang.of("confirmation_gui_title"), confirmAction, null);
    }

    public ConfirmationGui(@NonNull Player player, @NonNull ClickAction confirmAction, Gui previousGui) {
        this(player, Lang.of("confirmation_gui_title"), confirmAction, previousGui);
    }

    @Override
    public void draw() {
        add(YES_ITEM, toSlot(2, 2), clickEvent -> {
            confirmAction.perform(clickEvent);
            closeOrOpenPrevious();
        });
        add(NO_ITEM, toSlot(6, 2), clickEvent -> {
            gamer.playSound(YSound.ITEM_BREAK);
            closeOrOpenPrevious();
        });
    }

    private void closeOrOpenPrevious() {
        Gui parent = getParent();

        if (parent != null) {
            parent.reopen();
        } else {
            player.closeInventory();
        }
    }

}
