package club.abelix.games.buildffa.game.manager.additional;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AdditionalItemManager {
    private static final AdditionalItemManager INSTANCE = new AdditionalItemManager();
    private final Map<Player, Map<ItemStack, Integer>> additionalItems = new HashMap<>();

    private AdditionalItemManager() {
    }

    public static AdditionalItemManager getInstance() {
        return INSTANCE;
    }

    public void addItem(Player player, ItemStack item) {
        additionalItems.computeIfAbsent(player, k -> new HashMap<>())
                .merge(item, 1, Integer::sum);
    }

    public Map<ItemStack, Integer> getPlayerItems(Player player) {
        return additionalItems.getOrDefault(player, new HashMap<>());
    }

    public void clearPlayerItems(Player player) {
        additionalItems.remove(player);
    }
}