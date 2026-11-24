package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class StorageManager implements Listener, CommandExecutor {
    public static final int MAX_PAGES = 9;
    private static final long LOCK_MS = 120_000L;
    private final JavaPlugin plugin;
    private final DataManager data;
    private final Map<UUID,Integer> unlocked = new ConcurrentHashMap<>();
    private final Map<UUID,Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID,Integer> cancelledClicks = new ConcurrentHashMap<>();

    public StorageManager(JavaPlugin plugin, DataManager data) { this.plugin=plugin; this.data=data; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)){sender.sendMessage("Players only.");return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("debug")){sendDebug(player);return true;}
        if(args.length>0&&args[0].equalsIgnoreCase("admin")){handleAdmin(player,args);return true;}
        openStorage(player);return true;
    }

    public void openStorage(Player player){
        if(!playerDataReady(player)){player.sendMessage(CC.color("&cYour player data is still syncing. Please wait a moment."));return;}
        loadUnlocked(player.getUniqueId()).thenAccept(count->sync(()->{if(player.isOnline())openMain(player,count);}));
    }

    private void openMain(Player player,int count){
        Inventory inv=Bukkit.createInventory(new MenuHolder("storage-main"),54,CC.color("&8Storage"));
        int[] slots={10,11,12,13,14,15,16,19,20};
        for(int page=1;page<=MAX_PAGES;page++){
            boolean open=page<=count;
            List<String> lore=open?List.of("&7Store items in this personal page.","","&eClick to open!"):
                    List.of("&7Unlock this page from the Gold Store.","","&eClick to open Gold Store!");
            inv.setItem(slots[page-1],button(open?Material.ENDER_CHEST:Material.GRAY_STAINED_GLASS_PANE,
                    open?"&aEnder Chest Page "+page:"&cLocked Storage Page",lore,"page:"+page));
        }
        inv.setItem(45,button(Material.GRAY_DYE,"&7Previous Page",List.of("&cYou are on the first page."),null));
        inv.setItem(49,button(Material.BARRIER,"&cClose",List.of("&7Close storage."),"close"));
        inv.setItem(53,button(Material.GRAY_DYE,"&7Next Page",List.of("&cAll storage pages are shown."),null));
        fill(inv);player.openInventory(inv);
    }

    private void openPage(Player player,int page){
        if(!playerDataReady(player)){player.sendMessage(CC.color("&cYour player data is still syncing. Please wait a moment."));return;}
        if(page<1||page>cachedUnlockedPages(player)){locked(player);return;}
        if(sessions.containsKey(player.getUniqueId())){player.sendMessage(CC.color("&cYour storage is already open."));return;}
        acquire(player.getUniqueId(),page).thenCompose(acquired->{
            if(!acquired)return CompletableFuture.failedFuture(new IllegalStateException("Storage is already open on another server."));
            return loadPage(player.getUniqueId(),page);
        }).whenComplete((contents,error)->sync(()->{
            if(!player.isOnline()){release(player.getUniqueId());return;}
            if(error!=null){release(player.getUniqueId());player.sendMessage(CC.color("&cCould not load your storage. Try again in a moment."));return;}
            Inventory inventory=Bukkit.createInventory(new MenuHolder("storage-page:"+page),54,CC.color("&8Ender Chest ("+page+")"));
            inventory.setContents(clone(contents));sessions.put(player.getUniqueId(),new Session(page,inventory));player.openInventory(inventory);
        }));
    }

    private boolean playerDataReady(Player player){return !(plugin instanceof Game game)||!game.getPlayerDataSyncManager().isEnabled()||game.getPlayerDataSyncManager().isPlayerDataLoaded(player.getUniqueId());}

    @EventHandler public void click(InventoryClickEvent event){
        if(!(event.getInventory().getHolder() instanceof MenuHolder(String type)))return;
        if(type.equals("storage-main")){
            event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player)||event.getClickedInventory()!=event.getView().getTopInventory())return;
            String action=action(event.getCurrentItem());if(action==null)return;if(action.equals("close")){player.closeInventory();return;}
            if(action.startsWith("page:"))try{openPage(player,Integer.parseInt(action.substring(5)));}catch(NumberFormatException ignored){}
            return;
        }
        if(!type.startsWith("storage-page:"))return;
        if(!(event.getWhoClicked() instanceof Player))return;
        if(event.getRawSlot()<event.getView().getTopInventory().getSize()&&blocked(event.getCursor())){
            event.setCancelled(true);
            cancelledClicks.merge(event.getWhoClicked().getUniqueId(),1,Integer::sum);
        }
    }

    @EventHandler public void drag(InventoryDragEvent event){
        if(!(event.getInventory().getHolder() instanceof MenuHolder(String type))||!type.startsWith("storage-page:"))return;
        if(blocked(event.getOldCursor())&&event.getRawSlots().stream().anyMatch(slot->slot<event.getView().getTopInventory().getSize())){
            event.setCancelled(true);
            cancelledClicks.merge(event.getWhoClicked().getUniqueId(),1,Integer::sum);
        }
    }
    @EventHandler public void close(InventoryCloseEvent event){if(event.getInventory().getHolder() instanceof MenuHolder(String type)&&type.startsWith("storage-page:")&&event.getPlayer() instanceof Player player)saveSession(player.getUniqueId());}
    @EventHandler public void join(PlayerJoinEvent event){loadUnlocked(event.getPlayer().getUniqueId());}
    @EventHandler public void quit(PlayerQuitEvent event){saveSession(event.getPlayer().getUniqueId());unlocked.remove(event.getPlayer().getUniqueId());}

    public int cachedUnlockedPages(Player player){return Math.clamp(unlocked.getOrDefault(player.getUniqueId(),1),1,MAX_PAGES);}
    public int nextPageCost(Player player){return costForPage(Math.min(MAX_PAGES,cachedUnlockedPages(player)+1));}
    public static int costForPage(int page){return switch(page){case 2->250;case 3->500;case 4->750;case 5->1000;case 6->1500;case 7->2000;case 8->2500;case 9->3000;default->0;};}

    public CompletableFuture<Boolean> unlockNextPage(Player player){
        UUID uuid=player.getUniqueId();int current=cachedUnlockedPages(player);if(current>=MAX_PAGES)return CompletableFuture.completedFuture(false);
        int next=current+1;unlocked.put(uuid,next);
        return async(()->{try(Connection c=data.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO smp_player_storage_unlocks(uuid,unlocked_pages,updated_at) VALUES(?,?,?) ON DUPLICATE KEY UPDATE unlocked_pages=GREATEST(unlocked_pages,VALUES(unlocked_pages)),updated_at=VALUES(updated_at)")){s.setString(1,uuid.toString());s.setInt(2,next);s.setLong(3,System.currentTimeMillis());s.executeUpdate();return true;}catch(SQLException e){unlocked.put(uuid,current);plugin.getLogger().warning("Could not unlock storage page: "+e.getMessage());return false;}});
    }

    public CompletableFuture<Void> saveOpenPage(Player player){return saveSession(player.getUniqueId());}
    public CompletableFuture<Void> shutdown(){return CompletableFuture.allOf(new ArrayList<>(sessions.keySet()).stream().map(this::saveSession).toArray(CompletableFuture[]::new));}

    /** Must be called on the server thread so an open page can be cloned safely. */
    public CompletableFuture<List<ItemStack>> snapshotContents(UUID uuid) {
        Session session = sessions.get(uuid);
        int openPage = session == null ? -1 : session.page;
        ItemStack[] openContents = session == null ? null : clone(session.inventory.getContents());
        return async(() -> {
            Map<Integer, ItemStack[]> pages = new TreeMap<>();
            try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "SELECT page,contents_blob FROM smp_player_storage_pages WHERE uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) pages.put(result.getInt("page"), deserialize(result.getBytes("contents_blob")));
                }
            }
            if (openContents != null) pages.put(openPage, openContents);
            List<ItemStack> contents = new ArrayList<>();
            for (ItemStack[] page : pages.values()) Collections.addAll(contents, page);
            return contents;
        });
    }

    private CompletableFuture<Void> saveSession(UUID uuid){Session session=sessions.remove(uuid);if(session==null)return CompletableFuture.completedFuture(null);ItemStack[] contents=clone(session.inventory.getContents());return async(()->{try(Connection c=data.getConnection();PreparedStatement s=c.prepareStatement("INSERT INTO smp_player_storage_pages(uuid,page,contents_blob,updated_at) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE contents_blob=VALUES(contents_blob),updated_at=VALUES(updated_at)")){s.setString(1,uuid.toString());s.setInt(2,session.page);s.setBytes(3,serialize(contents));s.setLong(4,System.currentTimeMillis());s.executeUpdate();}finally{releaseNow(uuid);}return null;});}

    private CompletableFuture<ItemStack[]> loadPage(UUID uuid,int page){return async(()->{try(Connection c=data.getConnection();PreparedStatement s=c.prepareStatement("SELECT contents_blob FROM smp_player_storage_pages WHERE uuid=? AND page=?")){s.setString(1,uuid.toString());s.setInt(2,page);try(ResultSet r=s.executeQuery()){return r.next()?deserialize(r.getBytes(1)):new ItemStack[54];}}});}
    private CompletableFuture<Integer> loadUnlocked(UUID uuid){Integer cached=unlocked.get(uuid);if(cached!=null)return CompletableFuture.completedFuture(cached);return async(()->{int value=1;try(Connection c=data.getConnection();PreparedStatement s=c.prepareStatement("SELECT unlocked_pages FROM smp_player_storage_unlocks WHERE uuid=?")){s.setString(1,uuid.toString());try(ResultSet r=s.executeQuery()){if(r.next())value=Math.clamp(r.getInt(1),1,MAX_PAGES);}}unlocked.put(uuid,value);return value;}).exceptionally(error->{unlocked.put(uuid,1);return 1;});}

    private CompletableFuture<Boolean> acquire(UUID uuid,int page){return async(()->{long now=System.currentTimeMillis();try(Connection c=data.getConnection()){try(PreparedStatement d=c.prepareStatement("DELETE FROM smp_player_storage_sessions WHERE uuid=? AND expires_at<?")){d.setString(1,uuid.toString());d.setLong(2,now);d.executeUpdate();}try(PreparedStatement s=c.prepareStatement("INSERT IGNORE INTO smp_player_storage_sessions(uuid,page,server_id,opened_at,expires_at) VALUES(?,?,?,?,?)")){s.setString(1,uuid.toString());s.setInt(2,page);s.setString(3,serverId());s.setLong(4,now);s.setLong(5,now+LOCK_MS);return s.executeUpdate()==1;}}});}
    private void release(UUID uuid){async(()->{releaseNow(uuid);return null;});}
    private void releaseNow(UUID uuid)throws SQLException{if(!data.isMySqlEnabled())return;try(Connection c=data.getConnection();PreparedStatement s=c.prepareStatement("DELETE FROM smp_player_storage_sessions WHERE uuid=? AND server_id=?")){s.setString(1,uuid.toString());s.setString(2,serverId());s.executeUpdate();}}

    private void locked(Player player){player.sendMessage(CC.color("&cThis storage page is locked. Unlock more storage in the Gold Store."));player.performCommand("store upgrades");}
    private void sendDebug(Player player){
        if(!player.hasPermission("fakevival.storage.admin")){player.sendMessage(CC.color("&cNo permission."));return;}
        Session session=sessions.get(player.getUniqueId());
        player.sendMessage(CC.color("&8[Storage Debug]"));
        player.sendMessage("uuid="+player.getUniqueId());
        player.sendMessage("unlockedPages="+cachedUnlockedPages(player));
        player.sendMessage("source=goldstore/account_upgrades/storage_unlocks");
        player.sendMessage("openPage="+(session==null?"none":session.page));
        player.sendMessage("canEdit="+(session!=null&&session.page<=cachedUnlockedPages(player)));
        player.sendMessage("Storage page: "+(session==null?"none":session.page));
        player.sendMessage("Unlocked: "+(session!=null&&session.page<=cachedUnlockedPages(player)));
        player.sendMessage("Inventory type: "+(session==null?"STORAGE_MAIN_MENU":"STORAGE_PAGE"));
        player.sendMessage("Cancelled clicks: "+cancelledClicks.getOrDefault(player.getUniqueId(),0));
        player.sendMessage("Saved slots: 54");
    }
    public void sendDebug(CommandSender viewer,Player target){
        Session session=sessions.get(target.getUniqueId());
        viewer.sendMessage(CC.color("&8[Storage Debug] &7"+target.getName()));
        viewer.sendMessage("Storage page: "+(session==null?"none":session.page));
        viewer.sendMessage("Unlocked: "+(session!=null&&session.page<=cachedUnlockedPages(target)));
        viewer.sendMessage("Inventory type: "+(session==null?"STORAGE_MAIN_MENU":"STORAGE_PAGE"));
        viewer.sendMessage("Cancelled clicks: "+cancelledClicks.getOrDefault(target.getUniqueId(),0));
        viewer.sendMessage("Saved slots: 54");
    }
    private boolean blocked(ItemStack item){if(item==null||item.getType().isAir()||!item.hasItemMeta())return false;PersistentDataContainer pdc=item.getItemMeta().getPersistentDataContainer();for(var key:pdc.getKeys()){String name=key.getKey().toLowerCase(Locale.ROOT);if(name.contains("menu_action")||name.contains("menu_item"))return true;}String text=(item.getItemMeta().hasDisplayName()?org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName()):"").toLowerCase(Locale.ROOT);return text.contains("admin item")||text.contains("test item")||(!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("furniture.enabled",false)&&pdc.getKeys().stream().anyMatch(k->k.getKey().contains("furniture")));}
    private String serverId(){if(plugin instanceof Game game&&game.getNetworkService()!=null)return game.getNetworkService().getCurrentServerId();return System.getenv().getOrDefault("SMP_SERVER_ID","unknown");}
    private void handleAdmin(Player player,String[] args){if(!player.hasPermission("fakevival.devcontrols.admin")){player.sendMessage(CC.color("&cNo permission."));return;}String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"debug";switch(action){case "give"->unlockNextPage(player).thenAccept(ok->sync(()->player.sendMessage(CC.color(ok?"&aGranted one storage page.":"&cStorage is already maxed or could not be updated."))));case "reset"->{unlocked.put(player.getUniqueId(),1);async(()->{try(Connection c=data.getConnection();PreparedStatement s=c.prepareStatement("REPLACE INTO smp_player_storage_unlocks(uuid,unlocked_pages,updated_at) VALUES(?,1,?)")){s.setString(1,player.getUniqueId().toString());s.setLong(2,System.currentTimeMillis());s.executeUpdate();}return null;});player.sendMessage(CC.color("&aStorage unlocks reset to page 1."));}case "save"-> {saveOpenPage(player);player.sendMessage(CC.color("&aStorage save queued."));}case "reload"->{unlocked.remove(player.getUniqueId());loadUnlocked(player.getUniqueId()).thenAccept(v->sync(()->player.sendMessage(CC.color("&aReloaded storage data: &e"+v+" pages&a."))));}case "store"->player.performCommand("store upgrades");default->player.sendMessage(CC.color("&eStorage pages: &a"+cachedUnlockedPages(player)+"&7/&a"+MAX_PAGES+"&e, session: &a"+sessions.containsKey(player.getUniqueId())));}}

    private ItemStack button(Material material,String name,List<String> lore,String action){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setDisplayName(CC.color(name));meta.setLore(lore.stream().map(CC::color).toList());if(action!=null)meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin,"storage_action"),org.bukkit.persistence.PersistentDataType.STRING,action);item.setItemMeta(meta);return item;}
    private String action(ItemStack item){return item==null||!item.hasItemMeta()?null:item.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin,"storage_action"),org.bukkit.persistence.PersistentDataType.STRING);}
    private void fill(Inventory inv){ItemStack pane=button(Material.BLACK_STAINED_GLASS_PANE," ",List.of(),null);for(int i=0;i<inv.getSize();i++)if(inv.getItem(i)==null)inv.setItem(i,pane);}
    private ItemStack[] clone(ItemStack[] source){ItemStack[] copy=new ItemStack[source.length];for(int i=0;i<source.length;i++)copy[i]=source[i]==null?null:source[i].clone();return copy;}
    private byte[] serialize(ItemStack[] contents)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(BukkitObjectOutputStream out=new BukkitObjectOutputStream(bytes)){out.writeInt(contents.length);for(ItemStack item:contents)out.writeObject(item);}return bytes.toByteArray();}
    private ItemStack[] deserialize(byte[] bytes)throws IOException,ClassNotFoundException{try(BukkitObjectInputStream in=new BukkitObjectInputStream(new ByteArrayInputStream(bytes))){int length=in.readInt();if(length<0||length>54)throw new IOException("Invalid storage size");ItemStack[] result=new ItemStack[54];for(int i=0;i<length;i++)result[i]=(ItemStack)in.readObject();return result;}}
    private <T> CompletableFuture<T> async(SqlSupplier<T> supplier){return CompletableFuture.supplyAsync(()->{try{return supplier.get();}catch(Exception error){throw new java.util.concurrent.CompletionException(error);}});}
    private void sync(Runnable task){if(Bukkit.isPrimaryThread())task.run();else Bukkit.getScheduler().runTask(plugin,task);}
    @FunctionalInterface private interface SqlSupplier<T>{T get()throws Exception;}
    private record Session(int page,Inventory inventory){}
}
