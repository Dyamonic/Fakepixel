package club.abelix.api.base.item;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.item.gui.CategoryListGui;
import club.abelix.api.base.item.gui.GameItemGui;
import club.abelix.api.base.item.gui.GameItemPurchaseConfirmGui;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.Noun;
import club.abelix.network.common.logging.Logging;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Created by saharNooby on 23:03 17.10.2017
 */
@Getter
public class GameItemCategory implements GameMenuCategory {
    /**
     * Строковый id категории
     */
    private final String id;

    /**
     * Существительное, обозначающее вещь, которая находится в этой категории. Пример: "кит"
     */
    private final Noun word;

    /**
     * Иконка
     */
    private final ItemStack icon;

    /**
     * true, если вещи в категории можно выбирать
     */
    private final boolean isSelectable;

    /**
     * Вещь по умолчанию. Будет использоваться в getSelected если не
     * выбрана никакая больше вещь
     */
    private GameItem defaultItem;

    /**
     * Хранилище предметов, как правило этот параметр не требуется указывать
     */
    private final GameItemStorage storage;

    /**
     * Гуишка подтверждения покупки предмета
     */
    private final Class<? extends GameItemPurchaseConfirmGui> confirmGui;

    public GameItemCategory(@NonNull String id, @NonNull Noun word, @NonNull ItemStack icon, boolean isSelectable, @Nullable GameItemStorage storage, @Nullable Class<? extends GameItemPurchaseConfirmGui> confirmationGui) {
        this.id = id;
        this.word = word;
        this.icon = icon;
        this.isSelectable = isSelectable;
        this.storage = storage != null ? storage : GameItemStorage.INSTANCE;
        this.confirmGui = confirmationGui != null ? confirmationGui : GameItemPurchaseConfirmGui.class;
    }

    public GameItemCategory(@NonNull String id, @NonNull Noun word, @NonNull ItemStack icon, boolean isSelectable) {
        this(id, word, icon, isSelectable, null, null);
    }

    public String getDisplayName(Language language) {
        return this.word.get(language).plural().firstUp().toString();
    }

    public List<String> getDescription(Language language) {
        String key = "category_" + id + "_description";

        if (!language.isMessageTranslated(key)) {
            return Collections.emptyList();
        }

        return language.getMessageList(key);
    }

    @Override
    public void openGui(Player player, CategoryListGui from) {
        new GameItemGui(player, this, from, storage, confirmGui);
    }

    public void openGui(Player player, Gui from) {
        new GameItemGui(player, this, from, storage, confirmGui);
    }

    public GameItemCategory defaultItem(@NonNull GameItem defaultItem) {
        this.defaultItem = defaultItem;
        return this;
    }

    public boolean isDefault(@NonNull GameItem item) {
        return defaultItem != null && defaultItem.getId() == item.getId();
    }

    public boolean isPreviewAvailable() {
        return false;
    }

    public void preview(@NonNull GameItem item, @NonNull Gamer gamer) {
        // no actions by default
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "/" + this.id;
    }

}
