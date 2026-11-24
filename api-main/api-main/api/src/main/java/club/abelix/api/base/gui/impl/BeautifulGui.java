package club.abelix.api.base.gui.impl;

import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.slots.BeautifulSlotsProvider;
import club.abelix.api.base.gui.slots.BeautifulSlotsProviderType;
import lombok.NonNull;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class BeautifulGui extends Gui {

    private final BeautifulSlotsProvider slotsProvider;

    public BeautifulGui(@NonNull Player gamer, @NonNull String lang, BeautifulSlotsProviderType providerType, int itemsCount) {
        super(gamer, lang, providerType.getInstance().getRawCountForItems(itemsCount));
        this.slotsProvider = providerType.getInstance();
    }

    public abstract List<ItemStack> getContentToDraw();

    @Override
    public void draw() {
        val contentToDraw = getContentToDraw();
        for (int i = 0; i < contentToDraw.size(); i++) {
            val slotForItem = slotsProvider.getSlotForItem(i, contentToDraw.size());
            add(contentToDraw.get(i), slotForItem);
        }
    }
}
