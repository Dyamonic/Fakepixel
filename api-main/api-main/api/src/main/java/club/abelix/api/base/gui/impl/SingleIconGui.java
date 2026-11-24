package club.abelix.api.base.gui.impl;

import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SingleIconGui extends Gui {

    public SingleIconGui(@NonNull Player player, String title, ItemStack icon, ClickAction action) {
        super(player, title, 3);

        add(icon, toSlot(4, 1), action);
    }

}
