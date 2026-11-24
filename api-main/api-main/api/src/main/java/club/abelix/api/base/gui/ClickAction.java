package club.abelix.api.base.gui;

import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface ClickAction {

    /**
     * Действие при клике на предмет в инвентаре
     */
    void perform(InventoryClickEvent clickEvent);
}

