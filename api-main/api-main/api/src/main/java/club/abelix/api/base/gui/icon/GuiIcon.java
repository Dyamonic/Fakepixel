package club.abelix.api.base.gui.icon;

import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import lombok.*;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class GuiIcon {

    private final ItemStack item;
    private final ClickAction action;

    @Setter
    private int slot;

    public void draw(@NonNull Gui gui) {
        gui.add(item, slot, action);
    }

}

