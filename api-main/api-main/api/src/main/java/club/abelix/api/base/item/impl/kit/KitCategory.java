package club.abelix.api.base.item.impl.kit;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.GameItemCategory;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.commons.util.word.Noun;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class KitCategory extends GameItemCategory {

    public KitCategory(@NonNull String id) {
        this(id, new ItemStack(Material.CHEST));
    }

    public KitCategory(@NonNull String id, @NonNull ItemStack icon) {
        super(id, CommonWords.KIT, icon, true);
    }

    public KitCategory(@NonNull String id, @NonNull ItemStack icon, @NonNull Noun commonWord) {
        super(id, commonWord, icon, true);
    }

    @Override
    public boolean isPreviewAvailable() {
        return true;
    }

    @Override
    public void preview(@NonNull GameItem item, @NonNull Gamer gamer) {
        val player = gamer.getPlayer();

        Gui current = Gui.getOpened(player);

        new KitSettingsGui(gamer, (Kit) item, current);
    }
}
