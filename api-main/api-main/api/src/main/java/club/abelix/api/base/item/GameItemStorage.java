package club.abelix.api.base.item;

import club.abelix.api.base.gamer.Gamer;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.*;

public class GameItemStorage {

    public static final GameItemStorage INSTANCE = new GameItemStorage();

    // Сюда нужно регистрировать все вещи (киты, абилки, перки) плагину миниигры

    private final Map<GameItemCategory, List<GameItem>> storage = new LinkedHashMap<>();
    private final TIntObjectMap<GameItem> byId = new TIntObjectHashMap<>();

    public void register(@NonNull GameItem item) {
        List<GameItem> items = storage.computeIfAbsent(item.getCategory(), k -> new ArrayList<>());

        if (!items.contains(item)) {
            items.add(item);
        }

        byId.put(item.getId(), item);
    }

    public GameItem getById(int id) {
        return byId.get(id);
    }

    public Set<GameItemCategory> getCategories() {
        return Collections.unmodifiableSet(storage.keySet());
    }

    public List<GameItem> getItems(@NonNull GameItemCategory cat) {
        List<GameItem> gameItemList = storage.getOrDefault(cat, new ArrayList<>());

        //удаляем предметы, которые являются продолжением других предметов
        gameItemList.removeIf(gameItem -> gameItem.isUpgradeable() && gameItem.getPrev() != null);

        return Collections.unmodifiableList(gameItemList);
    }

    public <T extends GameItem> void selectItem(@NonNull Gamer gamer, @NonNull T item) {
        GameItemCategory cat = item.getCategory();

        if (!cat.isSelectable()) {
            return;
        }

        gamer.setProperty("SELECTED/" + cat.getId(), item.getId());
    }

    @Deprecated
    public GameItem getSelected(@NonNull GameItemCategory category, @NonNull Player player) {
        return getSelected(category, Gamer.getGamer(player));
    }

    public GameItem getSelected(@NonNull GameItemCategory cat, @NonNull Gamer gamer) {
        if (!cat.isSelectable()) {
            throw new IllegalArgumentException(cat + " does not require selection");
        }

        GameItem defaultItem = cat.getDefaultItem();
        int selectedItemId = gamer.getProperty("SELECTED/" + cat.getId());

        if (selectedItemId <= 0) {
            return defaultItem;
        }

        GameItem gameItem = byId.get(selectedItemId);

        if (gameItem == null) {
            return defaultItem;
        }

        //если игроку недоступен этот предмет, то очищаем его выбор
        if (!gameItem.has(gamer)) {
            clearSelection(cat, gamer);

            return defaultItem;
        }

        return gameItem;
    }

    @Deprecated
    public void clearSelection(@NonNull GameItemCategory category, @NonNull Player player) {
        clearSelection(category, Gamer.getGamer(player));
    }

    public void clearSelection(@NonNull GameItemCategory cat, @NonNull Gamer gamer) {
        if (!cat.isSelectable()) {
            throw new IllegalArgumentException(cat + " does not require selection");
        }

        gamer.setProperty("SELECTED/" + cat.getId(), 0);
    }

    @Deprecated
    public void applyItems(@NonNull GameItemCategory cat, @NonNull Player player) {
        applyItems(cat, Gamer.getGamer(player));
    }

    public void applyItems(@NonNull GameItemCategory cat, @NonNull Gamer gamer) {
        if (cat.isSelectable()) {
            // быстрое получение выбранной вещи, без цикла
            GameItem item = getSelected(cat, gamer);

            if (item instanceof Applicable) {
                ((Applicable) item).apply(gamer);
            }

            return;
        }

        List<GameItem> items = getItems(cat);

        for (GameItem item : items) {
            if (!item.has(gamer)) {
                continue;
            }

            if (item.isUpgradeable()) {
                item = item.getLastPurchased(gamer);
            }

            if (item instanceof Applicable) {
                ((Applicable) item).apply(gamer);
            }
        }
    }

}
