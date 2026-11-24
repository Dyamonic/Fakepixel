package club.abelix.api.server.game.loot;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.loot.category.LootCategory;
import club.abelix.api.server.game.loot.event.ChestFillEvent;
import club.abelix.api.server.game.loot.group.LootGroup;
import club.abelix.core.bukkit.HeartBeat;
import com.google.common.collect.Sets;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Unidentified Person
 */
public class LootManager extends GameListener<Game> {

    private List<LootCategory> categories;

    private Map<Location, LootGroup> byLocation;

    public LootManager(Game game, String name) {
        super(game);

        File lootFile = new File(name + "_loot.yml");
        Configuration loot = YamlConfiguration.loadConfiguration(lootFile);

        ConfigurationSection sCategories = loot.getConfigurationSection("Categories");

        categories = sCategories.getKeys(false).stream()
                .map(sCategories::getConfigurationSection)
                .map(LootCategory::new)
                .collect(Collectors.toList());

        // Координаты и т.д
        ConfigurationSection gameLoot = game.getConfig();
        ConfigurationSection sLocations = gameLoot.getConfigurationSection("Locations");

        byLocation = new HashMap<>();

        for (String sLocationKey : sLocations.getKeys(false)) {
            ConfigurationSection sLocation = sLocations.getConfigurationSection(sLocationKey);
            Set<String> keys = sLocation.getKeys(false);

            for (String key : keys) {
                Location[] locations = game.loadLocations(sLocation.getStringList(key));

                LootGroup group = new LootGroup(
                        sLocationKey, Sets.newHashSet(locations), new ArrayList<>()
                );

                for (Location loc : group.getLocations()) {
                    byLocation.put(loc, group);
                }
            }
        }
    }

    public void addCustomChest(Location location, String category) {
        byLocation.put(location, new LootGroup(category, Collections.singleton(location), new ArrayList<>()));
    }

    public void fillGroup(LootGroup group) {
        group.fillGroup(game, categories);
    }

    public void clear() {
        categories = null;
        byLocation = null;
    }

    public void refillChests() {
        byLocation.values().stream().distinct()
                .forEach(LootGroup::reset);

        // Закрываем все открытые сундуки
        for (Gamer gamer : game.getAlivePlayers()) {
            Player player = gamer.getPlayer();
            Inventory openInventory = player.getOpenInventory().getTopInventory();

            if (openInventory == null) {
                continue;
            }

            InventoryHolder holder = openInventory.getHolder();

            if (!(holder instanceof Chest)) {
                continue;
            }

            Chest chest = (Chest) holder;

            if (byLocation.containsKey(chest.getBlock().getLocation())) {
                player.closeInventory();
            }
        }
    }

    public void fillChest(@NonNull Chest chest, @NonNull ChestFillEvent.Reason reason) {
        Block block = chest.getBlock();
        Location location = block.getLocation();

        if (!byLocation.containsKey(location)) {
            return;
        }

        LootGroup group;

        switch (reason) {
            default:
            case OPEN:
                group = byLocation.get(location);
                break;
            case BREAK:
            case EXPLODE:
                group = byLocation.remove(location);
                group.addDestroyedLocation(location);
                break;
        }

        if (!group.addFilledLocation(location)) {
            return;
        }

        if (group.hasNoItems()) {
            fillGroup(group);
        }

        group.fillChest(chest);

        HeartBeat.callEvent(new ChestFillEvent(chest, reason));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChestOpen(InventoryOpenEvent e) {
        checkGame(e, () -> {
            Inventory inventory = e.getInventory();
            InventoryHolder holder = inventory.getHolder();

            if (!(holder instanceof Chest)) {
                return;
            }

            Player player = (Player) e.getPlayer();
            Gamer gamer = Gamer.getGamer(player);

            if (gamer == null || gamer.isSpectator()) {
                return;
            }

            fillChest((Chest) holder, ChestFillEvent.Reason.OPEN);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        checkGame(e, () -> {
            Block block = e.getBlock();
            BlockState state = block.getState();

            if (!(state instanceof Chest)) {
                return;
            }

            Player player = e.getPlayer();
            Gamer gamer = Gamer.getGamer(player);

            if (gamer == null || gamer.isSpectator()) {
                return;
            }

            fillChest((Chest) state, ChestFillEvent.Reason.BREAK);
        });
    }

    public void onExplode(List<Block> blockList) {
        for (Block block : blockList) {
            BlockState state = block.getState();

            if (!(state instanceof Chest)) {
                continue;
            }

            fillChest((Chest) state, ChestFillEvent.Reason.EXPLODE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(BlockExplodeEvent e) {
        checkGame(e, () -> onExplode(e.blockList()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        checkGame(e, () -> onExplode(e.blockList()));
    }

}
