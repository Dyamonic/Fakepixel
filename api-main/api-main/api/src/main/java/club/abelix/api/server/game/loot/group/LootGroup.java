package club.abelix.api.server.game.loot.group;

import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.loot.category.LootCategory;
import club.abelix.api.server.game.loot.category.LootCategoryItem;
import club.abelix.api.server.game.loot.category.luck.LootCategoryLuck;
import club.abelix.commons.util.random.RandomUtil;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Unidentified Person
 */
public class LootGroup {

    private final String location;

    private final Set<Location> locations;
    private final Set<Location> filledLocations;
    private final Set<Location> destroyedLocations;

    private final List<ItemStack> items;

    public LootGroup(String location, Set<Location> locations, List<ItemStack> items) {
        this.location = location;

        this.locations = locations;

        this.filledLocations = new HashSet<>();
        this.destroyedLocations = new HashSet<>();

        this.items = items;
    }

    public boolean hasNoItems() {
        return items.isEmpty();
    }

    public void reset() {
        items.clear();
        locations.removeAll(destroyedLocations);
        destroyedLocations.clear();
        filledLocations.clear();
    }

    public void addDestroyedLocation(Location location) {
        destroyedLocations.add(location);
    }

    public boolean addFilledLocation(Location location) {
        return filledLocations.add(location);
    }

    public float getFillPercentage() {
        return 1F / (locations.size() - filledLocations.size() + 1);
    }

    public void fillGroup(Game game, List<LootCategory> categories) {
        for (LootCategory category : categories) {
            fillGroup(game, category);
        }

        Collections.shuffle(items);
    }

    public void fillGroup(Game game, LootCategory category) {
        LootCategoryLuck luck = category.getLuck(location);

        if (luck == null) {
            return;
        }

        if (!category.test(game)) {
            return;
        }

        int itemCount = luck.getCount();

        if (itemCount == 0) {
            return;
        }

        List<LootCategoryItem> items = category.getItems().stream()
                .filter(item -> item.test(game))
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            return;
        }

        for (int i = 0; i < itemCount; i++) {
            LootCategoryItem categoryItem = RandomUtil.weightedRandom(items, LootCategoryItem::getChance);
            this.items.add(categoryItem.getItemStack().clone());
        }
    }

    public void fillChest(Chest chest) {
        float percentage = getFillPercentage();
        Iterator<ItemStack> iter = items.iterator();

        Inventory inventory = chest.getBlockInventory();
        ItemStack[] contents = new ItemStack[inventory.getSize()];

        Iterator<Integer> slots = IntStream.range(0, contents.length)
                .boxed().collect(RandomUtil.toShuffledList())
                .iterator();

        int count = (int) Math.ceil(this.items.size() * percentage);

        for (int i = 0; i < count; i++) {
            ItemStack item = iter.next();
            contents[slots.next()] = item;
            iter.remove();
        }

        inventory.setContents(contents);
    }

    public Set<Location> getLocations() {
        return locations;
    }

    public List<ItemStack> getItems() {
        return items;
    }

}
