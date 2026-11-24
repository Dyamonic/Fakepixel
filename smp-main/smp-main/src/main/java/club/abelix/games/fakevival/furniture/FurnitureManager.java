package club.abelix.games.fakevival.furniture;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.managers.*;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import com.google.gson.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class FurnitureManager implements Listener {
    private static final int ITEMS_PER_PAGE = 21;
    private static final int[] SHOP_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
    private final JavaPlugin plugin;
    private final DataManager data;
    private final FurnitureRegistry registry;
    private final EconomyManager economy;
    private final SpawnProtectionManager spawn;
    private final ZoneManager zones;
    private final NamespacedKey itemId;
    private final NamespacedKey itemType;
    private final NamespacedKey furnitureId;
    private final NamespacedKey modelId;
    private final NamespacedKey packId;
    private final NamespacedKey category;
    private final NamespacedKey furnitureMarker;
    private final NamespacedKey groupId;
    private final NamespacedKey owner;
    private final NamespacedKey protectedSpawn;
    private final NamespacedKey menuAction;
    private final NamespacedKey seatMarker;
    private final SmpModelEngine modelEngine = new SmpModelEngine();
    private final SmpModelPlacementService placementService;
    private final Set<String> validPlacedGroups=ConcurrentHashMap.newKeySet();private volatile boolean placedGroupsLoaded;
    private final Deque<SpawnJob> spawnQueue=new ArrayDeque<>();
    private final Set<UUID> queuedGroups=new HashSet<>();
    private final Map<String,Integer> queuedGroupsByChunk=new HashMap<>(),queuedPartsByChunk=new HashMap<>();
    private final Map<String,List<Placed>> placedByChunk=new HashMap<>();
    private final Map<String,Placed> placedByGroup=new HashMap<>();
    private final Map<String,Collision> collisionsByLocation=new HashMap<>();
    private final Map<String,List<Collision>> collisionsByGroup=new HashMap<>();
    private final Map<UUID,SeatSession> activeSeats=new HashMap<>();

    public FurnitureManager(JavaPlugin plugin, DataManager data, FurnitureRegistry registry, EconomyManager economy,
                            SpawnProtectionManager spawn, ZoneManager zones) {
        this.plugin=plugin; this.data=data; this.registry=registry; this.economy=economy; this.spawn=spawn; this.zones=zones;
        itemId=key("custom_item_id"); itemType=key("custom_item_type"); furnitureId=key("furniture_id"); modelId=key("model_id"); packId=key("furniture_pack_id"); category=key("furniture_category");
        furnitureMarker=key("smp_furniture"); groupId=key("furniture_group_id"); owner=key("furniture_owner_uuid");
        NamespacedKey placedAt = key("placed_at");
        protectedSpawn=key("protected_spawn"); menuAction=key("furniture_menu_action");seatMarker=key("smp_furniture_seat");placementService=new SmpModelPlacementService(plugin);
    }

    private NamespacedKey key(String value) { return new NamespacedKey(plugin, value); }
    private String currentServerId(){if(plugin instanceof Game game&&game.getNetworkService()!=null){String id=game.getNetworkService().getCurrentServerId();if(id!=null&&!id.isBlank())return id.toLowerCase(Locale.ROOT);}return"unknown";}

    public boolean isEnabled(){return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("furniture.enabled",false);}
    public boolean isShopEnabled(){return isEnabled()&&club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("furniture.shop.enabled",false);}
    private boolean overrideEnabled(Player player){return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("furniture.admin-override-enabled",false)&&player!=null&&player.hasPermission("fakevival.furniture.admin");}
    public void start() { if(!isEnabled()){removeLoadedVisuals();return;} Bukkit.getScheduler().runTaskTimer(plugin,this::processSpawnQueue,1L,1L);reloadPlaced(); }
    private void removeLoadedVisuals(){for(World world:Bukkit.getWorlds())for(Entity entity:world.getEntities())if(isFurniture(entity)||entity.getPersistentDataContainer().has(seatMarker,PersistentDataType.BYTE))entity.remove();spawnQueue.clear();queuedGroups.clear();queuedGroupsByChunk.clear();queuedPartsByChunk.clear();}
    public void stop(){for(UUID player:new ArrayList<>(activeSeats.keySet()))leaveSeat(player);for(World world:Bukkit.getWorlds())for(Entity entity:world.getEntities())if(entity.getPersistentDataContainer().has(seatMarker,PersistentDataType.BYTE))entity.remove();}

    public ItemStack createItem(FurnitureDefinition definition, int amount) {
        ItemStack item = new ItemStack(definition.previewMaterial(), Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color("&e" + definition.displayName()));
        int parts=modelEngine.parse(definition).parts().size();
        meta.setLore(List.of(CC.color("&7Place this furniture as decoration."),CC.color("&7Type: &eVanilla Model Furniture"),CC.color("&7Parts: &e"+parts),"",CC.color("&eRight Click to place!")));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemId, PersistentDataType.STRING, definition.id()); pdc.set(itemType, PersistentDataType.STRING, "FURNITURE"); pdc.set(furnitureId, PersistentDataType.STRING, definition.id());pdc.set(modelId,PersistentDataType.STRING,definition.id());
        pdc.set(packId, PersistentDataType.STRING, definition.packId()); pdc.set(category, PersistentDataType.STRING, definition.category().name());
        item.setItemMeta(meta); return item;
    }

    public void openShop(Player player, FurnitureDefinition.Category selected) {
        if(!isShopEnabled()){player.sendMessage(CC.color("&cFurniture Shop is currently disabled."));return;}
        openShop(player, 0, ShopCategory.ALL);
    }

    private void openShop(Player player, int requestedPage, ShopCategory filter) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("furniture-shop"), 54, CC.color("&8Furniture Shop"));
        List<FurnitureDefinition> items = shopEntries(filter);
        int maxPage = Math.max(0, (items.size() - 1) / ITEMS_PER_PAGE);
        int page = Math.clamp(requestedPage, 0, maxPage);

        inventory.setItem(4, menuItem(Material.BOOKSHELF, "&aCategory: &e"+filter.displayName,
                List.of("&7Click to cycle furniture categories.", "", "&eClick to change!"), "filter:"+filter.next().name()));

        if (items.isEmpty()) {
            inventory.setItem(22, menuItem(Material.BARRIER, "&cNo Furniture Available",
                    List.of("&7No furniture models are registered yet."), null));
        } else {
            int start = page * ITEMS_PER_PAGE;
            for (int index=0; index<ITEMS_PER_PAGE && start+index<items.size(); index++) {
                FurnitureDefinition definition=items.get(start+index);
                inventory.setItem(SHOP_SLOTS[index], shopPreview(definition, page, filter));
            }
        }

        if (page > 0) inventory.setItem(45, menuItem(Material.ARROW, "&aPrevious Page",
                List.of("&7Go to page &e"+page+"&7.", "", "&eClick to view!"), "page:"+(page-1)+":"+filter.name()));
        else inventory.setItem(45, menuItem(Material.GRAY_DYE, "&7Previous Page", List.of("&cYou are on the first page."), null));
        inventory.setItem(48, menuItem(Material.PAPER, "&ePage "+(page+1)+"/"+(maxPage+1),
                List.of("&7Furniture: &e"+items.size()), null));
        inventory.setItem(49, menuItem(Material.BARRIER, "&cClose", List.of(), "close"));
        if (page < maxPage) inventory.setItem(53, menuItem(Material.ARROW, "&aNext Page",
                List.of("&7Go to page &e"+(page+2)+"&7.", "", "&eClick to view!"), "page:"+(page+1)+":"+filter.name()));
        else inventory.setItem(53, menuItem(Material.GRAY_DYE, "&7Next Page", List.of("&cYou are on the last page."), null));
        player.openInventory(inventory);
    }

    private List<FurnitureDefinition> shopEntries(ShopCategory filter) {
        Map<String,FurnitureDefinition> unique=new HashMap<>();
        for(FurnitureDefinition definition:registry.all()) if(filter.matches(definition.category())) unique.putIfAbsent(definition.id().toLowerCase(Locale.ROOT),definition);
        return unique.values().stream().sorted(Comparator.comparing((FurnitureDefinition value)->value.category().ordinal())
                .thenComparingDouble(FurnitureDefinition::price).thenComparing(FurnitureDefinition::displayName,String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private ItemStack shopPreview(FurnitureDefinition definition,int page,ShopCategory filter) {
        ItemStack icon=createItem(definition,1).clone();
        ItemMeta meta=icon.getItemMeta();
        int parts=modelEngine.parse(definition).parts().size();
        meta.setLore(color(List.of("&7Place this furniture as decoration.","&7Type: &eFurniture",
                "&7Category: &e"+definition.category().displayName(),"&7Parts: &e"+parts,"",
                "&7Price: &6"+economy.format(definition.price())+" Coins","","&eClick to buy!")));
        meta.getPersistentDataContainer().set(menuAction,PersistentDataType.STRING,"buy:"+definition.id()+":"+page+":"+filter.name());
        icon.setItemMeta(meta);return icon;
    }

    private ItemStack menuItem(Material material,String name,List<String> lore,String action) {ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setDisplayName(CC.color(name));meta.setLore(color(lore));if(action!=null)meta.getPersistentDataContainer().set(menuAction,PersistentDataType.STRING,action);item.setItemMeta(meta);return item;}
    private List<String> color(List<String> lore){return lore.stream().map(CC::color).toList();}

    public int removeHere(Player player, double radius) {
        Set<String> groups=new HashSet<>();for(Entity entity:player.getWorld().getNearbyEntities(player.getLocation(),Math.min(32,radius),Math.min(32,radius),Math.min(32,radius))){if(!isFurniture(entity)||protectedFurniture(entity)&&!spawn.isBuildMode(player))continue;String group=get(entity,groupId);groups.add(group==null?entity.getUniqueId().toString():group);}for(String group:groups){removeGroup(player.getWorld(),group);try{delete(UUID.fromString(group));}catch(IllegalArgumentException ignored){}}return groups.size();
    }

    public int cleanupCurrentServerWorld(Player player){World world=player.getWorld();Set<String> groups=new HashSet<>();for(Placed row:new ArrayList<>(placedByGroup.values()))if(row.world.equalsIgnoreCase(world.getName()))groups.add(row.entityUuid);for(Entity entity:world.getEntities())if(isFurniture(entity)){String group=get(entity,groupId);groups.add(group==null?entity.getUniqueId().toString():group);}for(Collision collision:new ArrayList<>(collisionsByLocation.values()))if(collision.world.equalsIgnoreCase(world.getName()))groups.add(collision.groupId);for(String group:groups){removeGroup(world,group);try{delete(UUID.fromString(group));}catch(IllegalArgumentException ignored){}}return groups.size();}

    public void pickupTarget(Player player) {
        org.bukkit.util.RayTraceResult entityHit=player.getWorld().rayTraceEntities(
                player.getEyeLocation(),player.getEyeLocation().getDirection(),5.0D,0.75D,this::isFurniture);
        if(entityHit!=null&&entityHit.getHitEntity()!=null){tryRemove(player,entityHit.getHitEntity());return;}
        Block target=player.getTargetBlockExact(5,org.bukkit.FluidCollisionMode.NEVER);
        Collision collision=target==null?null:collisionsByLocation.get(blockKey(target.getLocation()));
        if(collision!=null){
            for(Entity entity:player.getWorld().getNearbyEntities(target.getLocation().add(.5,.5,.5),3,3,3,this::isFurniture)){
                if(collision.groupId.equals(get(entity,groupId))){tryRemove(player,entity);return;}
            }
        }
        player.sendMessage(CC.color("&cNo furniture found in front of you."));
    }

    public List<String> info(Player player) {
        org.bukkit.util.RayTraceResult hit=player.getWorld().rayTraceEntities(player.getEyeLocation(),player.getEyeLocation().getDirection(),8,0.5,this::isFurniture);
        if(hit==null||hit.getHitEntity()==null)return List.of("&cLook at furniture within 8 blocks.");Entity entity=hit.getHitEntity();return List.of("&bFurniture: &f"+get(entity,furnitureId),"&7Pack: &f"+get(entity,packId),"&7Owner: &f"+get(entity,owner),"&7Spawn protected: &f"+protectedFurniture(entity),"&7Entity: &f"+entity.getUniqueId());
    }

    public boolean testPlace(Player player, FurnitureDefinition definition) {
        Location location=player.getLocation().getBlock().getLocation().add(.5,0,.5); boolean protectedArea=spawn.isInside(location);
        if(!canPlaceFurniture(player,location))return false;SmpFurnitureModel model=modelEngine.parse(definition);if(model.parts().size()>SmpModelPlacementService.MAX_MODEL_PARTS||!validModelPlacement(player,location,model))return false;
        float yaw=FurnitureRotation.cardinal(player.getFacing());UUID requested=UUID.randomUUID();List<Collision> collision=placeCollisionBlocks(player,definition,model,location,yaw,requested,protectedArea);if(collision==null)return false;
        boolean queued=enqueueSpawn(model,location,yaw,player.getUniqueId(),protectedArea,requested,group->{if(group.isPresent()){persist(group.get().id(),definition,location,yaw,player.getUniqueId(),protectedArea);persistCollisions(collision);}else restoreCollisions(requested.toString(),false);});if(!queued)restoreCollisions(requested.toString(),false);return queued;
    }
    public boolean preview(Player player,SmpFurnitureModel model){Location location=player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2)).getBlock().getLocation().add(.5,0,.5);if(!canPlaceFurniture(player,location)||model.parts().size()>SmpModelPlacementService.MAX_MODEL_PARTS||!validModelPlacement(player,location,model))return false;return enqueueSpawn(model,location,FurnitureRotation.cardinal(player.getFacing()),player.getUniqueId(),false,null,group->group.ifPresent(placed->Bukkit.getScheduler().runTaskLater(plugin,()->placed.entities().forEach(Entity::remove),20L*20L)));}

    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equals("furniture-shop")) return;
        event.setCancelled(true); if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory()!=event.getView().getTopInventory()) return;
        if(!isShopEnabled()){player.closeInventory();player.sendMessage(CC.color("&cFurniture Shop is currently disabled."));return;}
        ItemStack clicked=event.getCurrentItem(); if(clicked==null||!clicked.hasItemMeta())return;
        String action=clicked.getItemMeta().getPersistentDataContainer().get(menuAction,PersistentDataType.STRING); if(action==null)return;
        if(action.equals("close")){player.closeInventory();return;}
        if(action.startsWith("filter:")){openShop(player,0,ShopCategory.parse(action.substring(7)));return;}
        if(action.startsWith("page:")){String[] values=action.split(":",3);if(values.length==3)try{openShop(player,Integer.parseInt(values[1]),ShopCategory.parse(values[2]));}catch(NumberFormatException ignored){}return;}
        if(!action.startsWith("buy:"))return; String[] values=action.split(":",4);if(values.length<4)return;FurnitureDefinition definition=registry.get(values[1]).orElse(null); if(definition==null)return;
        int page;try{page=Integer.parseInt(values[2]);}catch(NumberFormatException ignored){page=0;}ShopCategory filter=ShopCategory.parse(values[3]);
        if(!definition.permission().isBlank()&&!player.hasPermission(definition.permission())){player.sendMessage(CC.color("&cYou do not have permission to buy this furniture."));return;}
        ItemStack purchase=createItem(definition,1); if(!hasInventorySpace(player,purchase)){player.sendMessage(CC.color("&cYou need more inventory space."));return;}
        if(!economy.takeCoins(player.getUniqueId(),definition.price(),"furniture shop "+definition.id())){player.sendMessage(CC.color("&cYou do not have enough coins."));return;}
        Map<Integer,ItemStack> remaining=player.getInventory().addItem(purchase);if(!remaining.isEmpty()){economy.giveCoins(player.getUniqueId(),definition.price(),"furniture shop refund "+definition.id());player.sendMessage(CC.color("&cYou need more inventory space."));return;}
        player.sendMessage(CC.color("&aPurchased &e"+definition.displayName()+" &afor &6"+economy.format(definition.price())+" coins&a.")); openShop(player,page,filter);
    }

    @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent event){if(event.getInventory().getHolder() instanceof MenuHolder(String type)&&type.equals("furniture-shop"))event.setCancelled(true);}

    private boolean hasInventorySpace(Player player,ItemStack item){for(ItemStack current:player.getInventory().getStorageContents())if(current==null||current.getType().isAir()||current.isSimilar(item)&&current.getAmount()<current.getMaxStackSize())return true;return false;}

    private enum ShopCategory {
        ALL("All"),TABLES("Tables"),CHAIRS("Chairs"),BEDS("Beds"),LAMPS("Lamps"),PLANTS("Plants"),DECORATIONS("Decorations"),STORAGE("Storage"),OUTDOOR("Outdoor"),MISC("Misc");
        private final String displayName;ShopCategory(String displayName){this.displayName=displayName;}
        private ShopCategory next(){ShopCategory[] values=values();return values[(ordinal()+1)%values.length];}
        private boolean matches(FurnitureDefinition.Category category){return switch(this){case ALL->true;case TABLES->category==FurnitureDefinition.Category.TABLES;case CHAIRS->category==FurnitureDefinition.Category.CHAIRS;case BEDS->category==FurnitureDefinition.Category.BEDS;case LAMPS->category==FurnitureDefinition.Category.LAMPS;case PLANTS->category==FurnitureDefinition.Category.PLANTS;case DECORATIONS->category==FurnitureDefinition.Category.DECORATIONS;case STORAGE->category==FurnitureDefinition.Category.SHELVES;case OUTDOOR->category==FurnitureDefinition.Category.OUTDOOR;case MISC->category==FurnitureDefinition.Category.MISC||category==FurnitureDefinition.Category.FURNITURE||category==FurnitureDefinition.Category.FOOD_DRINKS;};}
        private static ShopCategory parse(String value){try{return valueOf(value.toUpperCase(Locale.ROOT));}catch(Exception ignored){return ALL;}}
    }

    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true) public void place(PlayerInteractEvent event) {
        if(event.getAction()!=Action.RIGHT_CLICK_BLOCK||event.getHand()!=EquipmentSlot.HAND)return;
        if(event.getClickedBlock()!=null){Collision owned=collisionsByLocation.get(blockKey(event.getClickedBlock().getLocation()));if(owned!=null){event.setCancelled(true);trySit(event.getPlayer(),owned.groupId);return;}}
        ItemStack item=event.getItem(); String id=get(item,furnitureId); if(id==null)return; event.setCancelled(true);
        if(!isEnabled()&&!overrideEnabled(event.getPlayer())){event.getPlayer().sendMessage(CC.color("&cFurniture is currently disabled."));return;}
        Player player=event.getPlayer(); FurnitureDefinition definition=registry.get(id).orElse(null); if(definition==null||!definition.placeable()){player.sendMessage(CC.color("&cThat furniture is not currently available."));return;}
        assert event.getClickedBlock() != null;
        Location location=event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(.5,0,.5);
        if(!isFurnitureItem(item)||!canPlaceFurniture(player,location))return;
        SmpFurnitureModel model=modelEngine.parse(definition);if(model.parts().size()>SmpModelPlacementService.MAX_MODEL_PARTS){player.sendMessage(CC.color("&cThat furniture model has too many parts."));return;}if(!validModelPlacement(player,location,model)){player.sendMessage(CC.color("&cThat model collides with blocks or nearby furniture."));return;}
        boolean inSpawn=spawn.isInside(location);float yaw=FurnitureRotation.cardinal(player.getFacing());UUID requested=UUID.randomUUID();List<Collision> collision=placeCollisionBlocks(player,definition,model,location,yaw,requested,inSpawn);
        if(collision==null){player.sendMessage(CC.color("&cThere is not enough space to place this furniture."));return;}
        boolean accepted=enqueueSpawn(model,location,yaw,player.getUniqueId(),inSpawn,requested,group->{if(group.isPresent()){persist(group.get().id(),definition,location,yaw,player.getUniqueId(),inSpawn);persistCollisions(collision);}else{restoreCollisions(requested.toString(),false);if(player.isOnline())player.getInventory().addItem(createItem(definition,1));}});
        if(!accepted){restoreCollisions(requested.toString(),false);player.sendMessage(CC.color("&cCould not place furniture. This chunk reached its furniture limit."));return;}
        if(player.getGameMode()==GameMode.SURVIVAL)item.setAmount(item.getAmount()-1);
    }

    public boolean canPlaceFurniture(Player player,Location location){if(!isEnabled()&&!overrideEnabled(player)){player.sendMessage(CC.color("&cFurniture is currently disabled."));return false;}PlacementDenial denial=serverPlacementDenial(player,location);if(denial!=null){player.sendMessage(CC.color(denial.message));return false;}if(!normalPlacementAllowed(player,location)||!withinFurnitureGroupLimits(location)){player.sendMessage(CC.color("&cYou cannot place furniture there."));return false;}return true;}
    private PlacementDenial serverPlacementDenial(Player player,Location location){if(player==null||location==null||location.getWorld()==null)return PlacementDenial.LOCATION;String world=location.getWorld().getName().toLowerCase(Locale.ROOT);boolean blockedWorld=Set.of("hub","lobby","dungeon","dungeons","dungeon_lobby").contains(world);boolean allowedServer=false;if(plugin instanceof Game game&&game.getNetworkService()!=null){String serverId=game.getNetworkService().getCurrentServerId();serverId=serverId==null?"":serverId.toLowerCase(Locale.ROOT);allowedServer=!game.getNetworkService().isHub()&&!game.getNetworkService().isDungeon()&&serverId.startsWith("smpworld-");}if(allowedServer&&!blockedWorld)return null;if(player.hasPermission("fakevival.furniture.place.bypassserver")){if(!spawn.isBuildMode(player))return PlacementDenial.BUILD_MODE;return null;}return PlacementDenial.SERVER;}
    private boolean isFurnitureItem(ItemStack item){if(item==null||!item.hasItemMeta())return false;PersistentDataContainer pdc=item.getItemMeta().getPersistentDataContainer();String id=pdc.get(furnitureId,PersistentDataType.STRING),type=pdc.get(itemType,PersistentDataType.STRING),customId=pdc.get(itemId,PersistentDataType.STRING);return id!=null&&customId!=null&&"FURNITURE".equalsIgnoreCase(type)&&id.equalsIgnoreCase(customId)&&registry.get(id).isPresent();}
    private boolean withinFurnitureGroupLimits(Location location){if(location.getWorld()==null||!location.isChunkLoaded())return false;Set<String> groups=new HashSet<>();for(Entity entity:location.getChunk().getEntities()){String id=get(entity,groupId);if(id!=null&&isFurniture(entity))groups.add(id);}return groups.size()+queuedGroupsByChunk.getOrDefault(chunkKey(location),0)<SmpModelPlacementService.MAX_GROUPS_PER_CHUNK&&groupsNear(location,128)<100;}
    private boolean normalPlacementAllowed(Player player,Location location){ if(location.getWorld()==null||!location.getBlock().isPassable()||!location.clone().add(0,1,0).getBlock().isPassable()||!location.clone().subtract(0,1,0).getBlock().getType().isSolid())return false;
        if(spawn.isInside(location)&&!spawn.canBypass(player))return false; return zones.canPlace(player,location.getBlock())||player.hasPermission("fakevival.admin.zone.bypass"); }
    private boolean validModelPlacement(Player player,Location origin,SmpFurnitureModel model){if(!normalPlacementAllowed(player,origin))return false;double radius=Math.max(.25,model.hitboxWidth()/2);int minX=(int)Math.floor(origin.getX()-radius+.001),maxX=(int)Math.floor(origin.getX()+radius-.001),minZ=(int)Math.floor(origin.getZ()-radius+.001),maxZ=(int)Math.floor(origin.getZ()+radius-.001);for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)for(int y=0;y<Math.ceil(model.hitboxHeight());y++)if(!origin.getWorld().getBlockAt(x,origin.getBlockY()+y,z).isPassable())return false;return origin.getWorld().getNearbyEntities(origin,radius,model.hitboxHeight(),radius,this::isFurniture).isEmpty();}
    private enum PlacementDenial{SERVER("&cYou can only place furniture on SMPWorld survival servers."),BUILD_MODE("&cEnable buildmode to place furniture here."),LOCATION("&cYou cannot place furniture there.");private final String message;PlacementDenial(String message){this.message=message;}}

    @EventHandler(priority=EventPriority.HIGHEST) public void damage(EntityDamageByEntityEvent event){if(!isFurniture(event.getEntity()))return;event.setCancelled(true);Player player=attackingPlayer(event.getDamager());if(player!=null)tryRemove(player,event.getEntity());}
    @EventHandler(priority=EventPriority.HIGHEST) public void damageAny(EntityDamageEvent event){if(isFurniture(event.getEntity()))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void manipulate(PlayerArmorStandManipulateEvent event){if(isFurniture(event.getRightClicked())){event.setCancelled(true);denyIfProtected(event.getPlayer(),event.getRightClicked());}}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractAtEntityEvent event){if(!isFurniture(event.getRightClicked()))return;event.setCancelled(true);if(event.getPlayer().isSneaking())tryRemove(event.getPlayer(),event.getRightClicked());else if(!trySit(event.getPlayer(),get(event.getRightClicked(),groupId)))denyIfProtected(event.getPlayer(),event.getRightClicked());}
    @EventHandler(priority=EventPriority.HIGHEST) public void breakCollision(BlockBreakEvent event){Collision collision=collisionsByLocation.get(blockKey(event.getBlock().getLocation()));if(collision==null)return;event.setCancelled(true);if(!event.getPlayer().hasPermission("fakevival.furniture.admin")||!spawn.isBuildMode(event.getPlayer())){event.getPlayer().sendMessage(CC.color("&cYou cannot break furniture collision blocks."));return;}Placed placed=placedByGroup.get(collision.groupId);if(placed!=null){removeGroup(event.getBlock().getWorld(),collision.groupId);try{delete(UUID.fromString(collision.groupId));}catch(IllegalArgumentException ignored){}}}
    @EventHandler(priority=EventPriority.HIGHEST) public void explode(BlockExplodeEvent event){event.blockList().removeIf(block->collisionsByLocation.containsKey(blockKey(block.getLocation())));}
    @EventHandler(priority=EventPriority.HIGHEST) public void explode(EntityExplodeEvent event){event.blockList().removeIf(block->collisionsByLocation.containsKey(blockKey(block.getLocation())));}
    @EventHandler public void sneak(PlayerToggleSneakEvent event){if(event.isSneaking())leaveSeat(event.getPlayer().getUniqueId());}
    @EventHandler public void quit(PlayerQuitEvent event){leaveSeat(event.getPlayer().getUniqueId());}
    @EventHandler public void death(PlayerDeathEvent event){leaveSeat(event.getEntity().getUniqueId());}
    @EventHandler public void worldChange(PlayerChangedWorldEvent event){leaveSeat(event.getPlayer().getUniqueId());}
    @EventHandler public void dismount(EntityDismountEvent event){if(event.getEntity() instanceof Player player&&event.getDismounted().getPersistentDataContainer().has(seatMarker,PersistentDataType.BYTE))Bukkit.getScheduler().runTask(plugin,()->leaveSeat(player.getUniqueId()));}
    private void denyIfProtected(Player p,Entity e){if(protectedFurniture(e)&&!spawn.canBypass(p))p.sendMessage(CC.color("&cYou cannot break furniture in spawn."));}
    private void tryRemove(Player player,Entity part){boolean protectedArea=protectedFurniture(part);if(protectedArea&&!spawn.isBuildMode(player)){player.sendMessage(CC.color("&cEnable &e/buildmode &cto edit furniture in this protected area."));return;}if(protectedArea&&!spawn.canBypass(player)){player.sendMessage(CC.color("&cYou cannot break furniture in spawn."));return;}String ownerId=get(part,owner);FurnitureDefinition definition=registry.get(get(part,furnitureId)).orElse(null);boolean owns=player.getUniqueId().toString().equals(ownerId);if(!owns&&!player.hasPermission("fakevival.furniture.admin")){player.sendMessage(CC.color("&cYou do not own this furniture."));return;}if(definition!=null&&!definition.playerBreakable()&&!player.hasPermission("fakevival.furniture.admin"))return;String group=get(part,groupId);if(group==null)group=part.getUniqueId().toString();removeGroup(part.getWorld(),group);try{delete(UUID.fromString(group));}catch(IllegalArgumentException ignored){}if(definition!=null){player.getInventory().addItem(createItem(definition,1));player.sendMessage(CC.color("&aPicked up &e"+definition.displayName()+"&a."));}}
    private Player attackingPlayer(Entity e){if(e instanceof Player p)return p;if(e instanceof Projectile p&&p.getShooter() instanceof Player player)return player;return null;}
    private boolean isFurniture(Entity e){return e!=null&&e.getPersistentDataContainer().has(furnitureMarker,PersistentDataType.BYTE);}
    private void removeGroup(World world,String id){leaveGroupSeats(id);for(Entity entity:world.getEntities())if(id.equals(get(entity,groupId)))entity.remove();restoreCollisions(id,true);}
    private boolean protectedFurniture(Entity e){Byte b=e.getPersistentDataContainer().get(protectedSpawn,PersistentDataType.BYTE);return isFurniture(e)&&b!=null&&b==1;}
    private String get(ItemStack i,NamespacedKey k){return i!=null&&i.hasItemMeta()?i.getItemMeta().getPersistentDataContainer().get(k,PersistentDataType.STRING):null;}
    private String get(Entity e,NamespacedKey k){return e.getPersistentDataContainer().get(k,PersistentDataType.STRING);}
    private static boolean bool(JsonObject o,String k,boolean d){try{return o.has(k)?o.get(k).getAsBoolean():d;}catch(Exception ignored){return d;}}

    private List<Collision> placeCollisionBlocks(Player player,FurnitureDefinition definition,SmpFurnitureModel model,Location origin,float yaw,UUID group,boolean protectedArea){List<Collision> result=new ArrayList<>();Set<String> locations=new HashSet<>();for(FurnitureCollisionBlock offset:model.collisionBlocks()){Location at=rotate(origin,offset.x(),offset.y(),offset.z(),yaw).getBlock().getLocation();String key=blockKey(at);Block block=at.getBlock();if(!locations.add(key)||collisionsByLocation.containsKey(key)||!replaceable(block.getType())||!zones.canPlace(player,block)&&!player.hasPermission("fakevival.admin.zone.bypass")||spawn.isInside(at)&&!spawn.canBypass(player)){rollbackCollisionList(result);return null;}Collision collision=new Collision(group.toString(),definition.id(),at.getWorld().getName(),at.getBlockX(),at.getBlockY(),at.getBlockZ(),block.getType().name(),block.getBlockData().getAsString(),player.getUniqueId().toString(),protectedArea);result.add(collision);}for(Collision collision:result){Block block=collision.location().getBlock();block.setType(Material.BARRIER,false);collisionsByLocation.put(blockKey(block.getLocation()),collision);collisionsByGroup.computeIfAbsent(collision.groupId,ignored->new ArrayList<>()).add(collision);}return result;}
    private boolean replaceable(Material material){return material.isAir()||material==Material.SHORT_GRASS||material==Material.TALL_GRASS||material==Material.FERN||material==Material.LARGE_FERN||material==Material.DEAD_BUSH||Tag.FLOWERS.isTagged(material);}
    private Location rotate(Location origin,double x,double y,double z,float yaw){org.bukkit.util.Vector rotated=FurnitureRotation.rotate(new org.bukkit.util.Vector(x,y,z),FurnitureRotation.snap(yaw));return origin.clone().add(Math.round(rotated.getX()),rotated.getY(),Math.round(rotated.getZ()));}
    private Location rotateExact(Location origin,double x,double y,double z,float yaw){return origin.clone().add(FurnitureRotation.rotate(new org.bukkit.util.Vector(x,y,z),FurnitureRotation.snap(yaw)));}
    private void rollbackCollisionList(List<Collision> collisions){for(Collision collision:collisions){collisionsByLocation.remove(collision.key());List<Collision> group=collisionsByGroup.get(collision.groupId);if(group!=null)group.remove(collision);restoreBlock(collision);}}
    private void restoreCollisions(String groupId,boolean deleteRows){List<Collision> collisions=new ArrayList<>(collisionsByGroup.getOrDefault(groupId,List.of()));for(Collision collision:collisions){collisionsByLocation.remove(collision.key());restoreBlock(collision);}collisionsByGroup.remove(groupId);if(deleteRows&&data.isMySqlEnabled())Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try(Connection connection=data.getConnection();PreparedStatement statement=connection.prepareStatement("DELETE FROM smp_furniture_collision_blocks WHERE group_id=?")){statement.setString(1,groupId);statement.executeUpdate();}catch(SQLException exception){plugin.getLogger().warning("Could not delete furniture collision blocks: "+exception.getMessage());}});}
    private void restoreBlock(Collision collision){World world=Bukkit.getWorld(collision.world);if(world==null)return;Block block=world.getBlockAt(collision.x,collision.y,collision.z);if(block.getType()!=Material.BARRIER)return;try{block.setBlockData(Bukkit.createBlockData(collision.originalData),false);}catch(Exception ignored){Material material=Material.matchMaterial(collision.originalType);block.setType(material==null?Material.AIR:material,false);}}
    private void persistCollisions(List<Collision> collisions){if(!data.isMySqlEnabled()||collisions.isEmpty())return;List<Collision> copy=List.copyOf(collisions);String serverId=currentServerId();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try(Connection connection=data.getConnection();PreparedStatement statement=connection.prepareStatement("REPLACE INTO smp_furniture_collision_blocks(group_id,furniture_id,world,x,y,z,original_type,original_data,owner_uuid,protected_block,server_id) VALUES(?,?,?,?,?,?,?,?,?,?,?)")){for(Collision collision:copy){statement.setString(1,collision.groupId);statement.setString(2,collision.furnitureId);statement.setString(3,collision.world);statement.setInt(4,collision.x);statement.setInt(5,collision.y);statement.setInt(6,collision.z);statement.setString(7,collision.originalType);statement.setString(8,collision.originalData);statement.setString(9,collision.owner);statement.setBoolean(10,collision.protectedBlock);statement.setString(11,serverId);statement.addBatch();}statement.executeBatch();}catch(SQLException exception){plugin.getLogger().warning("Could not save furniture collision blocks: "+exception.getMessage());}});}
    private String blockKey(Location location){return location.getWorld().getName()+":"+location.getBlockX()+":"+location.getBlockY()+":"+location.getBlockZ();}
    private void ensureLegacyCollisions(Placed placed){FurnitureDefinition definition=registry.get(placed.furnitureId).orElse(null);World world=Bukkit.getWorld(placed.world);if(definition==null||world==null)return;SmpFurnitureModel model=modelEngine.parse(definition);List<Collision> created=new ArrayList<>();Location origin=new Location(world,placed.x,placed.y,placed.z);for(FurnitureCollisionBlock offset:model.collisionBlocks()){Location at=rotate(origin,offset.x(),offset.y(),offset.z(),placed.yaw).getBlock().getLocation();Block block=at.getBlock();if(!replaceable(block.getType())||collisionsByLocation.containsKey(blockKey(at)))continue;Collision collision=new Collision(placed.entityUuid,placed.furnitureId,placed.world,at.getBlockX(),at.getBlockY(),at.getBlockZ(),block.getType().name(),block.getBlockData().getAsString(),placed.owner,placed.protectedArea);block.setType(Material.BARRIER,false);collisionsByLocation.put(collision.key(),collision);collisionsByGroup.computeIfAbsent(collision.groupId,ignored->new ArrayList<>()).add(collision);created.add(collision);}persistCollisions(created);}

    private boolean trySit(Player player,String groupId){if(groupId==null)return false;if(activeSeats.containsKey(player.getUniqueId())){leaveSeat(player.getUniqueId());return true;}Placed placed=placedByGroup.get(groupId);if(placed==null)return false;FurnitureDefinition definition=registry.get(placed.furnitureId).orElse(null);if(definition==null)return false;SmpFurnitureModel model=modelEngine.parse(definition);List<FurnitureSitPoint> points=!model.bedPoints().isEmpty()?model.bedPoints():model.sitPoints();if(!model.interactable()||points.isEmpty())return false;Set<Integer> occupied=new HashSet<>();for(SeatSession session:activeSeats.values())if(session.groupId.equals(groupId))occupied.add(session.index);int index=-1;for(int i=0;i<points.size();i++)if(!occupied.contains(i)){index=i;break;}if(index<0){player.sendMessage(CC.color("&cThat seat is occupied."));return true;}FurnitureSitPoint point=points.get(index);World world=Bukkit.getWorld(placed.world);if(world==null)return false;int placementYaw=FurnitureRotation.snap(placed.yaw);Location origin=new Location(world,placed.x,placed.y,placed.z);Location at=rotateExact(origin,point.x(),point.y(),point.z(),placementYaw);int seatIndex=index;float finalYaw=FurnitureRotation.normalize(placementYaw+point.yaw());ArmorStand seat=world.spawn(at,ArmorStand.class,stand->{stand.setInvisible(true);stand.setGravity(false);stand.setMarker(false);stand.setSmall(true);stand.setBasePlate(false);stand.setArms(false);stand.setInvulnerable(true);stand.setSilent(true);stand.setCollidable(false);stand.setPersistent(false);stand.getPersistentDataContainer().set(seatMarker,PersistentDataType.BYTE,(byte)1);stand.getPersistentDataContainer().set(groupId(),PersistentDataType.STRING,groupId);stand.getPersistentDataContainer().set(key("seat_index"),PersistentDataType.INTEGER,seatIndex);stand.getPersistentDataContainer().set(key("seat_owner_uuid"),PersistentDataType.STRING,player.getUniqueId().toString());});seat.setRotation(FurnitureRotation.toBukkitYaw(finalYaw),0);seat.addPassenger(player);player.setRotation(FurnitureRotation.toBukkitYaw(finalYaw),0);activeSeats.put(player.getUniqueId(),new SeatSession(groupId,index,seat.getUniqueId()));return true;}
    private NamespacedKey groupId(){return groupId;}
    private void leaveSeat(UUID playerId){SeatSession session=activeSeats.remove(playerId);if(session==null)return;Player player=Bukkit.getPlayer(playerId);Entity seat=Bukkit.getEntity(session.seatEntity);if(seat!=null){seat.eject();seat.remove();}if(player!=null&&player.getVehicle()!=null&&player.getVehicle().getUniqueId().equals(session.seatEntity))player.leaveVehicle();}
    private void leaveGroupSeats(String group){for(Map.Entry<UUID,SeatSession> entry:new ArrayList<>(activeSeats.entrySet()))if(entry.getValue().groupId.equals(group))leaveSeat(entry.getKey());}

    private void persist(UUID entityId,FurnitureDefinition f,Location l,float yaw,UUID ownerId,boolean protectedArea){validPlacedGroups.add(entityId.toString());Location saved=l.clone();String facing=facingName(FurnitureRotation.snap(yaw));Placed row=new Placed(entityId.toString(),f.id(),saved.getWorld().getName(),saved.getX(),saved.getY(),saved.getZ(),FurnitureRotation.snap(yaw),facing,ownerId.toString(),protectedArea);placedByGroup.put(row.entityUuid,row);placedByChunk.computeIfAbsent(chunkKey(saved),ignored->new ArrayList<>()).removeIf(value->value.entityUuid.equals(entityId.toString()));placedByChunk.get(chunkKey(saved)).add(row);if(!data.isMySqlEnabled())return;String serverId=currentServerId();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try(Connection connection=data.getConnection();PreparedStatement s=connection.prepareStatement("REPLACE INTO smp_placed_furniture(entity_uuid,furniture_id,pack_id,world,x,y,z,yaw,pitch,owner_uuid,placed_at,protected_spawn,facing,server_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)")){s.setString(1,entityId.toString());s.setString(2,f.id());s.setString(3,f.packId());s.setString(4,saved.getWorld().getName());s.setDouble(5,saved.getX());s.setDouble(6,saved.getY());s.setDouble(7,saved.getZ());s.setFloat(8,FurnitureRotation.snap(yaw));s.setFloat(9,saved.getPitch());s.setString(10,ownerId.toString());s.setLong(11,System.currentTimeMillis());s.setBoolean(12,protectedArea);s.setString(13,facing);s.setString(14,serverId);s.executeUpdate();}catch(SQLException e){plugin.getLogger().warning("Could not save furniture: "+e.getMessage());}});}
    private void delete(UUID uuid){String id=uuid.toString();validPlacedGroups.remove(id);placedByGroup.remove(id);placedByChunk.values().forEach(rows->rows.removeIf(row->row.entityUuid.equals(id)));if(!data.isMySqlEnabled())return;Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try(Connection connection=data.getConnection();PreparedStatement s=connection.prepareStatement("DELETE FROM smp_placed_furniture WHERE entity_uuid=?")){s.setString(1,id);s.executeUpdate();}catch(SQLException ignored){}});}

    private void reloadPlaced(){if(!data.isMySqlEnabled())return;String serverId=currentServerId();Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{List<Placed> rows=new ArrayList<>();List<Collision> collisionRows=new ArrayList<>();try(Connection connection=data.getConnection();PreparedStatement s=connection.prepareStatement("SELECT * FROM smp_placed_furniture WHERE server_id=? OR server_id=''")){s.setString(1,serverId);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Placed(r.getString("entity_uuid"),r.getString("furniture_id"),r.getString("world"),r.getDouble("x"),r.getDouble("y"),r.getDouble("z"),FurnitureRotation.snap(r.getFloat("yaw")),r.getString("facing"),r.getString("owner_uuid"),r.getBoolean("protected_spawn")));}try(PreparedStatement cs=connection.prepareStatement("SELECT * FROM smp_furniture_collision_blocks WHERE server_id=? OR server_id=''")){cs.setString(1,serverId);try(ResultSet cr=cs.executeQuery()){while(cr.next())collisionRows.add(new Collision(cr.getString("group_id"),cr.getString("furniture_id"),cr.getString("world"),cr.getInt("x"),cr.getInt("y"),cr.getInt("z"),cr.getString("original_type"),cr.getString("original_data"),cr.getString("owner_uuid"),cr.getBoolean("protected_block")));}}}catch(SQLException e){plugin.getLogger().warning("Could not load placed furniture: "+e.getMessage());return;}Bukkit.getScheduler().runTask(plugin,()->{indexPlaced(rows);indexCollisions(collisionRows);cleanupOrphans(rows);for(World world:Bukkit.getWorlds())for(Chunk chunk:world.getLoadedChunks())queueChunk(chunk);});});}
    private void cleanupOrphans(List<Placed> rows){validPlacedGroups.clear();for(Placed row:rows)validPlacedGroups.add(row.entityUuid);placedGroupsLoaded=true;for(World world:Bukkit.getWorlds())for(Entity entity:world.getEntities()){if(entity.getPersistentDataContainer().has(seatMarker,PersistentDataType.BYTE)){entity.remove();continue;}String group=get(entity,groupId);if(group!=null&&isFurniture(entity)&&!validPlacedGroups.contains(group))entity.remove();}}
    @EventHandler public void loadChunk(ChunkLoadEvent event){if(!isEnabled()){for(Entity entity:event.getChunk().getEntities())if(isFurniture(entity)||entity.getPersistentDataContainer().has(seatMarker,PersistentDataType.BYTE))entity.remove();return;}if(!placedGroupsLoaded)return;for(Entity entity:event.getChunk().getEntities()){if(entity.getPersistentDataContainer().has(seatMarker,PersistentDataType.BYTE)){entity.remove();continue;}String group=get(entity,groupId);if(group!=null&&isFurniture(entity)&&!validPlacedGroups.contains(group))entity.remove();}verifyChunkCollisions(event.getChunk());Bukkit.getScheduler().runTaskLater(plugin,()->queueChunk(event.getChunk()),2L);}
    @EventHandler public void unloadChunk(ChunkUnloadEvent event){String key=chunkKey(event.getChunk());Iterator<SpawnJob> iterator=spawnQueue.iterator();while(iterator.hasNext()){SpawnJob job=iterator.next();if(chunkKey(job.session.origin()).equals(key)){iterator.remove();placementService.abort(job.session);release(job);job.callback.accept(Optional.empty());}}}

    private void indexPlaced(List<Placed> rows){placedByChunk.clear();placedByGroup.clear();for(Placed row:rows){placedByGroup.put(row.entityUuid,row);placedByChunk.computeIfAbsent(chunkKey(row.world,(int)Math.floor(row.x)>>4,(int)Math.floor(row.z)>>4),ignored->new ArrayList<>()).add(row);}}
    private void indexCollisions(List<Collision> rows){collisionsByLocation.clear();collisionsByGroup.clear();for(Collision collision:rows){collisionsByLocation.put(collision.key(),collision);collisionsByGroup.computeIfAbsent(collision.groupId,ignored->new ArrayList<>()).add(collision);World world=Bukkit.getWorld(collision.world);if(world!=null&&world.isChunkLoaded(collision.x>>4,collision.z>>4)){Block block=world.getBlockAt(collision.x,collision.y,collision.z);if(block.getType()==Material.BARRIER||replaceable(block.getType()))block.setType(Material.BARRIER,false);}}for(Placed placed:placedByGroup.values())if(!collisionsByGroup.containsKey(placed.entityUuid))ensureLegacyCollisions(placed);}
    private void verifyChunkCollisions(Chunk chunk){for(Collision collision:collisionsByLocation.values())if(collision.world.equals(chunk.getWorld().getName())&&(collision.x>>4)==chunk.getX()&&(collision.z>>4)==chunk.getZ()){Block block=chunk.getWorld().getBlockAt(collision.x,collision.y,collision.z);if(block.getType()==Material.BARRIER||replaceable(block.getType()))block.setType(Material.BARRIER,false);}}
    private void queueChunk(Chunk chunk){if(!isEnabled()||!chunk.isLoaded()||!hasNearbyPlayer(chunk))return;for(Placed row:placedByChunk.getOrDefault(chunkKey(chunk),List.of())){FurnitureDefinition definition=registry.get(row.furnitureId).orElse(null);if(definition==null)continue;try{UUID group=UUID.fromString(row.entityUuid),ownerId=UUID.fromString(row.owner);if(groupExists(chunk,group)||queuedGroups.contains(group))continue;SmpFurnitureModel model=modelEngine.parse(definition);if(model.parts().size()>SmpModelPlacementService.MAX_MODEL_PARTS)continue;enqueueSpawn(model,new Location(chunk.getWorld(),row.x,row.y,row.z),FurnitureRotation.snap(row.yaw),ownerId,row.protectedArea,group,ignored->{});}catch(IllegalArgumentException ignored){}}}
    private boolean hasNearbyPlayer(Chunk chunk){double x=(chunk.getX()<<4)+8,z=(chunk.getZ()<<4)+8,radius=Math.max(64,(Bukkit.getViewDistance()+2)*16);for(Player player:chunk.getWorld().getPlayers())if(player.getLocation().distanceSquared(new Location(chunk.getWorld(),x,player.getY(),z))<=radius*radius)return true;return false;}
    private boolean groupExists(Chunk chunk,UUID group){String id=group.toString();return Arrays.stream(chunk.getEntities()).anyMatch(entity->id.equals(get(entity,groupId)));}

    private boolean enqueueSpawn(SmpFurnitureModel model,Location origin,float yaw,UUID ownerId,boolean protectedArea,UUID requestedGroup,Consumer<Optional<SmpModelPlacementService.PlacedGroup>> callback){
        if(!isEnabled()||origin.getWorld()==null||!origin.isChunkLoaded()||model.parts().size()>SmpModelPlacementService.MAX_MODEL_PARTS)return false;
        String chunk=chunkKey(origin);int parts=visibleParts(model),existingParts=0;Set<String> existingGroups=new HashSet<>();for(Entity entity:origin.getChunk().getEntities()){String id=get(entity,groupId);if(id!=null&&isFurniture(entity)){existingGroups.add(id);existingParts++;}}
        if(existingGroups.size()+queuedGroupsByChunk.getOrDefault(chunk,0)>=SmpModelPlacementService.MAX_GROUPS_PER_CHUNK||existingParts+queuedPartsByChunk.getOrDefault(chunk,0)+parts>SmpModelPlacementService.MAX_PARTS_PER_CHUNK||groupsNear(origin,128)>=100)return false;
        Optional<SmpModelPlacementService.SpawnSession> session=placementService.begin(model,origin,yaw,ownerId,protectedArea,requestedGroup);if(session.isEmpty())return false;
        SpawnJob job=new SpawnJob(session.get(),parts,callback);spawnQueue.addLast(job);queuedGroups.add(session.get().group());queuedGroupsByChunk.merge(chunk,1,Integer::sum);queuedPartsByChunk.merge(chunk,parts,Integer::sum);return true;
    }
    private void processSpawnQueue(){int groups=0,budget=20;while(groups<2&&budget>0&&!spawnQueue.isEmpty()){SpawnJob job=spawnQueue.removeFirst();if(!job.session.origin().isChunkLoaded()){placementService.abort(job.session);release(job);job.callback.accept(Optional.empty());groups++;continue;}int used=placementService.spawnNext(job.session,budget);budget-=used;groups++;if(placementService.complete(job.session)){Optional<SmpModelPlacementService.PlacedGroup> result=placementService.failed(job.session)?Optional.empty():Optional.of(placementService.result(job.session));release(job);job.callback.accept(result);}else spawnQueue.addLast(job);}}
    private void release(SpawnJob job){queuedGroups.remove(job.session.group());String chunk=chunkKey(job.session.origin());queuedGroupsByChunk.computeIfPresent(chunk,(key,value)->value<=1?null:value-1);queuedPartsByChunk.computeIfPresent(chunk,(key,value)->value<=job.parts?null:value-job.parts);}
    private int visibleParts(SmpFurnitureModel model){return (int)model.parts().stream().filter(part->part.visible()||part.entityType()==SmpModelPart.EntityType.ARMOR_STAND).count()+(model.hitboxWidth()>0&&model.hitboxHeight()>0?1:0);}
    private int groupsNear(Location origin,double radius){Set<String> groups=new HashSet<>();for(Entity entity:origin.getWorld().getNearbyEntities(origin,radius,radius,radius,this::isFurniture)){String id=get(entity,groupId);if(id!=null)groups.add(id);}return groups.size();}
    private String chunkKey(Location location){return chunkKey(location.getWorld().getName(),location.getBlockX()>>4,location.getBlockZ()>>4);}
    private String chunkKey(Chunk chunk){return chunkKey(chunk.getWorld().getName(),chunk.getX(),chunk.getZ());}
    private String chunkKey(String world,int x,int z){return world+":"+x+":"+z;}
    private String facingName(int yaw){return switch(Math.floorMod(yaw,360)){case 90->"EAST";case 180->"SOUTH";case 270->"WEST";default->"NORTH";};}

    public List<String> debugNear(Player player,double radius){Map<String,List<Entity>> groups=nearbyGroups(player,radius);int armor=0,displays=0,duplicates=0,parts=0;for(Map.Entry<String,List<Entity>> entry:groups.entrySet()){parts+=entry.getValue().size();for(Entity entity:entry.getValue()){if(entity instanceof ArmorStand)armor++;if(entity instanceof Display)displays++;}if(entry.getValue().size()>expectedParts(entry.getValue()))duplicates++;}Set<String> chunkGroups=new HashSet<>();for(Entity entity:player.getChunk().getEntities()){String id=get(entity,groupId);if(id!=null&&isFurniture(entity))chunkGroups.add(id);}return List.of("&eFurniture nearby: &a"+groups.size()+" groups, "+parts+" parts, "+duplicates+" duplicates.","&7Armor stands: &e"+armor+"&7, display entities: &e"+displays+"&7.","&7Furniture groups in this chunk: &e"+chunkGroups.size()+"&7.");}
    public String debugBarriers(Player player,double radius){int count=0;double squared=radius*radius;for(Collision collision:collisionsByLocation.values()){World world=Bukkit.getWorld(collision.world);if(world!=player.getWorld())continue;Location location=new Location(world,collision.x+.5,collision.y+.5,collision.z+.5);if(location.distanceSquared(player.getLocation())>squared)continue;count++;world.spawnParticle(Particle.END_ROD,location,3,.25,.25,.25,0);}return "&eFurniture barriers nearby: &a"+count;}
    public List<String> debugRotation(Player player,String id){FurnitureDefinition definition=registry.get(id).orElse(null);if(definition==null)return List.of("&cThat furniture id is not registered.");SmpFurnitureModel model=modelEngine.parse(definition);int yaw=FurnitureRotation.cardinal(player.getFacing());List<String> lines=new ArrayList<>(List.of("&eFurniture: &a"+definition.id(),"&eOrigin: &a"+player.getLocation().getBlockX()+", "+player.getLocation().getBlockY()+", "+player.getLocation().getBlockZ(),"&eFacing: &a"+player.getFacing()+" &7("+yaw+" degrees)","&eParts: &a"+model.parts().size()+"&e, collisions: &a"+model.collisionBlocks().size()+"&e, sit points: &a"+(model.sitPoints().size()+model.bedPoints().size())));for(int index=0;index<Math.min(5,model.parts().size());index++){SmpModelPart part=model.parts().get(index);org.bukkit.util.Vector local=new org.bukkit.util.Vector(part.x(),part.y(),part.z()),rotated=FurnitureRotation.rotate(local,yaw);lines.add("&7#"+index+" local &f"+vector(local)+" &7-> rotated &a"+vector(rotated));}return lines;}
    private String vector(org.bukkit.util.Vector vector){return String.format(Locale.US,"(%.3f, %.3f, %.3f)",vector.getX(),vector.getY(),vector.getZ());}
    public String cleanupDuplicates(Player player,double radius){Map<String,List<Entity>> groups=nearbyGroups(player,radius);int removed=0;for(Map.Entry<String,List<Entity>> entry:groups.entrySet()){List<Entity> entities=entry.getValue();boolean protectedArea=entities.stream().anyMatch(this::protectedFurniture);if(protectedArea&&!spawn.isBuildMode(player))continue;if(!validPlacedGroups.contains(entry.getKey())){entities.forEach(Entity::remove);removed+=entities.size();continue;}int extra=Math.max(0,entities.size()-expectedParts(entities));for(int index=0;index<extra;index++){entities.get(entities.size()-1-index).remove();removed++;}}return removed==0?"&aNo duplicate or orphaned furniture entities were found.":"&aRemoved &e"+removed+" &aduplicate or orphaned furniture entities.";}
    private Map<String,List<Entity>> nearbyGroups(Player player,double radius){Map<String,List<Entity>> groups=new HashMap<>();for(Entity entity:player.getWorld().getNearbyEntities(player.getLocation(),radius,radius,radius,this::isFurniture)){String id=get(entity,groupId);if(id!=null)groups.computeIfAbsent(id,ignored->new ArrayList<>()).add(entity);}return groups;}
    private int expectedParts(List<Entity> entities){if(entities.isEmpty())return 0;String id=get(entities.getFirst(),furnitureId);return registry.get(id).map(modelEngine::parse).map(this::visibleParts).orElse(0);}
    private record SpawnJob(SmpModelPlacementService.SpawnSession session,int parts,Consumer<Optional<SmpModelPlacementService.PlacedGroup>> callback){}
    private record Placed(String entityUuid,String furnitureId,String world,double x,double y,double z,float yaw,String facing,String owner,boolean protectedArea){}
    private record Collision(String groupId,String furnitureId,String world,int x,int y,int z,String originalType,String originalData,String owner,boolean protectedBlock){String key(){return world+":"+x+":"+y+":"+z;}Location location(){return new Location(Bukkit.getWorld(world),x,y,z);}}
    private record SeatSession(String groupId,int index,UUID seatEntity){}
}
