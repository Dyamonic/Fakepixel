package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.CollectionManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import club.abelix.games.fakevival.registries.VanillaBazaarRegistry;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class CollectionListener implements Listener {
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final CollectionManager collectionManager;
    private final CustomItemManager customItemManager;
    private final NamespacedKey countedKey,sourceKey,blockedKey,itemTypeKey,playerDroppedKey,dropOwnerKey,dropTimeKey;

    public CollectionListener(org.bukkit.plugin.java.JavaPlugin plugin, CollectionManager collectionManager, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.collectionManager = collectionManager;
        this.customItemManager = customItemManager;
        countedKey=new NamespacedKey(plugin,"smp_collection_counted");sourceKey=new NamespacedKey(plugin,"smp_collection_source");blockedKey=new NamespacedKey(plugin,"blocked_from_collections");itemTypeKey=new NamespacedKey(plugin,"custom_item_type");
        playerDroppedKey=new NamespacedKey(plugin,"smp_player_dropped");dropOwnerKey=new NamespacedKey(plugin,"smp_drop_owner");dropTimeKey=new NamespacedKey(plugin,"smp_drop_time");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(!eligible(event.getPlayer()))return;
        collectionManager.markPlayerPlaced(event.getBlockPlaced().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if(!eligible(event.getPlayer()))return;
        if (collectionManager.consumePlayerPlaced(event.getBlock().getLocation())) return;
        if (event.getBlock().getBlockData() instanceof Ageable ageable && ageable.getAge() < ageable.getMaximumAge()) return;
        Material source = collectionSource(event.getBlock().getType());
        collectionManager.addProgress(event.getPlayer(), source, 1);
        markSpawnedDrops(event.getBlock().getLocation(),collectionManager.getPrimaryCollectionId(source),"BLOCK_BREAK");
        rollCollectionDrops(event.getPlayer(), activityFor(source, event.getBlock().getBlockData() instanceof Ageable), source.name(),
                item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        org.bukkit.entity.Player killer = event.getEntity().getKiller();
        if (killer == null||!eligible(killer)) return;
        for (ItemStack drop : event.getDrops()) {
            String customId=customItemManager.getItemId(drop);if(customId==null)collectionManager.addProgress(killer, drop.getType(), Math.max(1, drop.getAmount()));else collectionManager.addCustomItemProgress(killer,customId,Math.max(1,drop.getAmount()));
        }
        markSpawnedDrops(event.getEntity().getLocation(),java.util.Set.of(),"MOB_KILL");
        rollCollectionDrops(killer, "combat", event.getEntityType().name(), event.getDrops()::add);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if(!eligible(event.getPlayer()))return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item item)) return;
        ItemStack caught = item.getItemStack();
        String caughtCustomId=customItemManager.getItemId(caught);if(caughtCustomId==null)collectionManager.addProgress(event.getPlayer(), caught.getType(), Math.max(1, caught.getAmount()));else collectionManager.addCustomItemProgress(event.getPlayer(),caughtCustomId,Math.max(1,caught.getAmount()));
        markCounted(item,"FISHING");
        rollCollectionDrops(event.getPlayer(), "fishing", caught.getType().name(), drop ->
                event.getPlayer().getInventory().addItem(drop).values().forEach(leftover ->
                        event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), leftover)));
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onPickup(EntityPickupItemEvent event){
        // Inventory acquisition is never a collection source. Legitimate
        // progress is awarded at break/kill/fish/craft time above.
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void onPlayerDrop(PlayerDropItemEvent event){
        Item entity=event.getItemDrop();
        entity.getPersistentDataContainer().set(playerDroppedKey,PersistentDataType.BYTE,(byte)1);
        entity.getPersistentDataContainer().set(dropOwnerKey,PersistentDataType.STRING,event.getPlayer().getUniqueId().toString());
        entity.getPersistentDataContainer().set(dropTimeKey,PersistentDataType.LONG,System.currentTimeMillis());
        markCounted(entity,"PLAYER_DROP_PICKUP");
    }

    private void markSpawnedDrops(org.bukkit.Location location,String collection,String source){markSpawnedDrops(location,java.util.Set.of(collection),source);}
    private void markSpawnedDrops(org.bukkit.Location location,java.util.Set<String>collections,String source){org.bukkit.Bukkit.getScheduler().runTask(plugin,()->{if(location.getWorld()==null)return;for(Item item:location.getWorld().getNearbyEntitiesByType(Item.class,location,2.5D)){if(collections.isEmpty()||collections.contains(collectionManager.getPrimaryCollectionId(item.getItemStack().getType())))markCounted(item,source);}});}
    private void markCounted(Item item,String source){item.getPersistentDataContainer().set(countedKey,PersistentDataType.BYTE,(byte)1);item.getPersistentDataContainer().set(sourceKey,PersistentDataType.STRING,source);}
    private boolean validCollectionItem(ItemStack item){if(item==null||item.getType().isAir()||!item.hasItemMeta()&&VanillaBazaarRegistry.byMaterial(item.getType()).isEmpty())return false;if(item.hasItemMeta()){var pdc=item.getItemMeta().getPersistentDataContainer();if(Byte.valueOf((byte)1).equals(pdc.get(blockedKey,PersistentDataType.BYTE)))return false;String id=customItemManager.getItemId(item);if(id!=null){String type=pdc.getOrDefault(itemTypeKey,PersistentDataType.STRING,"").toUpperCase(Locale.ROOT);if(type.contains("SPECIAL")||type.contains("KEY")||type.contains("COSMETIC")||type.contains("FURNITURE")||type.contains("PREMIUM")||id.equalsIgnoreCase("developer_notebook"))return false;return true;}}return VanillaBazaarRegistry.byMaterial(item.getType()).isPresent();}

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        if(!eligible(player))return;
        ItemStack result = event.getRecipe().getResult();
        String customItemId = customItemManager.getItemId(result);
        if (customItemId == null || collectionManager.isCustomItemUnlocked(player, customItemId)) return;

        CollectionManager.UnlockRequirement requirement = collectionManager.getUnlockRequirement(customItemId);
        event.setCancelled(true);
        String collectionName = requirement == null ? "Collection" : collectionManager.getDisplayName(requirement.collectionId());
        int needed = requirement == null ? 1 : requirement.required();
        player.sendMessage(CC.color("&cYou have not unlocked this recipe. &7Collect &e" + needed + " &7in &f" + collectionName + "&7."));
    }

    private void rollCollectionDrops(org.bukkit.entity.Player player, String activity, String source, Consumer<ItemStack> drop) {
        ConfigurationSection items = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("custom-items.items");
        if (items == null || activity == null || activity.isBlank()) return;
        for (String id : items.getKeys(false)) {
            String path = id + ".collection.drop";
            if (!activity.equalsIgnoreCase(items.getString(path + ".activity", ""))) continue;
            if (items.getStringList(path + ".sources").stream().noneMatch(value -> value.equalsIgnoreCase(source))) continue;
            double chance = Math.clamp(items.getDouble(path + ".chance", 0.0D), 0.0D, 100.0D);
            if (ThreadLocalRandom.current().nextDouble(100.0D) >= chance || !customItemManager.itemExists(id)) continue;
            ItemStack item = customItemManager.createItem(id, 1);
            drop.accept(item);
            collectionManager.addCustomItemProgress(player, id, item.getAmount());
        }
    }

    private String activityFor(Material material, boolean ageable) {
        if (ageable || switch (material) {
            case PUMPKIN, MELON, SUGAR_CANE, CACTUS, BAMBOO, BROWN_MUSHROOM, RED_MUSHROOM -> true;
            default -> false;
        }) return "farming";
        String name = material.name().toLowerCase(Locale.ROOT);
        if (name.endsWith("_log") || name.endsWith("_wood") || name.endsWith("_stem") || name.endsWith("_hyphae")
                || name.endsWith("_leaves")) return "foraging";
        return "mining";
    }
    private Material collectionSource(Material block){return switch(block){case CARROTS->Material.CARROT;case POTATOES->Material.POTATO;case BEETROOTS->Material.BEETROOT;case COCOA->Material.COCOA_BEANS;case SWEET_BERRY_BUSH->Material.SWEET_BERRIES;case CAVE_VINES,CAVE_VINES_PLANT->Material.GLOW_BERRIES;default->block;};}
    private boolean eligible(org.bukkit.entity.Player player){return player!=null&&player.getGameMode()!=org.bukkit.GameMode.CREATIVE&&player.getGameMode()!=org.bukkit.GameMode.SPECTATOR;}
}
