package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.*;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public final class SandboxCommand implements CommandExecutor, Listener {
    private static final String PERMISSION = "fakevival.sandbox.admin";
    private final Game game;
    private final SkillManager skills;
    private final JobManager jobs;
    private final EconomyManager economy;
    private final PlayerLevelManager playerLevels;
    private final CustomItemManager customItems;
    private final NamespacedKey actionKey;

    public SandboxCommand(Game game, SkillManager skills, JobManager jobs, EconomyManager economy,
                          PlayerLevelManager playerLevels, CustomItemManager customItems) {
        this.game=game;this.skills=skills;this.jobs=jobs;this.economy=economy;this.playerLevels=playerLevels;this.customItems=customItems;
        this.actionKey=new NamespacedKey(game,"sandbox_action");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player)){sender.sendMessage(CC.color("&cOnly players can open the sandbox menu."));return true;}
        if(!player.hasPermission(PERMISSION)){player.sendMessage(CC.color("&cYou do not have permission to use the sandbox menu."));return true;}
        openMain(player);return true;
    }

    public void openMain(Player player){
        Inventory menu=menu("sandbox:main",54,"&8Sandbox");
        menu.setItem(10,item(Material.CHEST,"&aItem Browser",List.of("&7Open the custom item browser.","","&eClick to open!"),"itembrowser"));
        menu.setItem(12,item(Material.EXPERIENCE_BOTTLE,"&aSkills Modify",List.of("&7Modify your skill levels","&7for testing.","","&eClick to open!"),"skills"));
        menu.setItem(14,item(Material.GOLD_INGOT,"&6Coins Generator",List.of("&7Generate or remove coins","&7from your test account.","","&eClick to open!"),"coins"));
        menu.setItem(20,item(Material.ANVIL,"&aEdit Item",List.of("&7Edit the item in your hand.","","&eClick to open!"),"edititem"));
        menu.setItem(22,item(Material.ENCHANTED_BOOK,"&dEnchantments",List.of("&7Test vanilla and custom","&7enchantments.","","&eClick to open!"),"enchants"));
        menu.setItem(24,item(Material.NETHER_STAR,"&bPlayer Level Modify",List.of("&7Modify your SMP/player level","&7for testing.","","&eClick to open!"),"playerlevel"));
        menu.setItem(30,item(Material.IRON_PICKAXE,"&eJobs Modify",List.of("&7Modify your job levels","&7and job XP for testing.","","&eClick to open!"),"jobs"));
        menu.setItem(49,item(Material.BARRIER,"&cClose",List.of(),"close"));fill(menu);player.openInventory(menu);
    }

    private void openSkills(Player player){
        Inventory menu=menu("sandbox:skills",54,"&8Sandbox - Skills");int[] slots=contentSlots();int index=0;
        for(String skill:game.getStaticGameRegistries().skills().skillIds()){
            if(index>=slots.length)break;menu.setItem(slots[index++],item(skillIcon(skill),"&a"+pretty(skill),List.of("&7Current Level: &e"+skills.getLevel(player,skill),"&7Current XP: &b"+skills.getXp(player,skill),"","&eLeft Click: +1 Level","&eRight Click: -1 Level","&eShift Left Click: +10 Levels","&eShift Right Click: -10 Levels","&eMiddle Click: Reset"),"skill:"+skill));
        }navigation(menu);player.openInventory(menu);
    }

    private void openJobs(Player player){
        Inventory menu=menu("sandbox:jobs",54,"&8Sandbox - Jobs");int[] slots=contentSlots();int index=0;
        for(String job:game.getStaticGameRegistries().jobs().jobIds()){
            if(index>=slots.length)break;menu.setItem(slots[index++],item(jobIcon(job),"&e"+pretty(job),List.of("&7Current Level: &e"+jobs.getLevel(player,job),"&7Current XP: &b"+jobs.getXp(player,job),"","&eLeft Click: +1 Level","&eRight Click: -1 Level","&eShift Left Click: +10 Levels","&eShift Right Click: -10 Levels","&eMiddle Click: Reset"),"job:"+job));
        }navigation(menu);player.openInventory(menu);
    }

    private void openCoins(Player player){
        Inventory menu=menu("sandbox:coins",27,"&8Sandbox - Coins");
        long[] values={100,1000,10000,100000,-100,-1000,-10000};int[] slots={10,11,12,13,15,16,17};
        for(int i=0;i<values.length;i++){long value=values[i];menu.setItem(slots[i],item(value>0?Material.GOLD_INGOT:Material.IRON_INGOT,(value>0?"&a+":"&c")+String.format(Locale.US,"%,d",value)+" Coins",List.of("&7Balance: &e"+economy.format(economy.getCoins(player))),"coin:"+value));}
        menu.setItem(22,item(Material.BARRIER,"&cReset Coins",List.of("&7Set your test balance to zero."),"coin:reset"));menu.setItem(18,item(Material.ARROW,"&cBack",List.of(),"back"));player.openInventory(menu);
    }

    private void openPlayerLevel(Player player){
        Inventory menu=menu("sandbox:playerlevel",54,"&8Sandbox - Player Level");
        menu.setItem(10,item(Material.LIME_DYE,"&a+1 Level",List.of(),"plevel:1"));menu.setItem(11,item(Material.EMERALD,"&a+10 Levels",List.of(),"plevel:10"));menu.setItem(12,item(Material.EMERALD_BLOCK,"&a+100 Levels",List.of(),"plevel:100"));
        menu.setItem(13,item(Material.RED_DYE,"&c-1 Level",List.of(),"plevel:-1"));menu.setItem(14,item(Material.REDSTONE,"&c-10 Levels",List.of(),"plevel:-10"));menu.setItem(15,item(Material.REDSTONE_BLOCK,"&c-100 Levels",List.of(),"plevel:-100"));
        menu.setItem(16,item(Material.NETHER_STAR,"&d+1000 Levels",List.of(),"plevel:1000"));menu.setItem(19,item(Material.COAL_BLOCK,"&4-1000 Levels",List.of(),"plevel:-1000"));
        menu.setItem(28,item(Material.NETHER_STAR,"&dSet Level 1000",List.of("&7Test the first ascended style."),"plevel:set1000"));
        menu.setItem(29,item(Material.DIAMOND,"&dSet Level 2000",List.of("&7Test the second prestige style."),"plevel:set2000"));
        menu.setItem(30,item(Material.AMETHYST_SHARD,"&dSet Level 5000",List.of("&7Test a high prestige style."),"plevel:set5000"));
        menu.setItem(31,item(Material.NETHER_STAR,"&dSet Level 10000",List.of("&7Test the ultimate style."),"plevel:set10000"));
        menu.setItem(37,item(Material.EXPERIENCE_BOTTLE,"&b+1,000 XP",List.of(),"pxp:1000"));menu.setItem(38,item(Material.EXPERIENCE_BOTTLE,"&b+10,000 XP",List.of(),"pxp:10000"));
        menu.setItem(39,item(Material.WRITABLE_BOOK,"&eShow Next Requirements",List.of(),"plreq:show"));menu.setItem(40,item(Material.CHEST,"&aGive Required Items",List.of(),"plreq:items"));
        menu.setItem(41,item(Material.EXPERIENCE_BOTTLE,"&aSet Required Skills",List.of(),"plreq:skills"));menu.setItem(42,item(Material.LIME_CONCRETE,"&cForce Level Up",List.of("&7Ignores requirements."),"plreq:force"));
        menu.setItem(43,item(Material.COMPARATOR,"&eValidate Registry",List.of(),"plreq:validate"));menu.setItem(44,item(Material.BEACON,"&bRecalculate Rewards",List.of(),"plreq:recalc"));
        menu.setItem(49,item(Material.BARRIER,"&cReset Level",List.of("&7Reset level to 1 and XP to 0."),"plevel:reset"));menu.setItem(45,item(Material.ARROW,"&cBack",List.of(),"back"));fill(menu);player.openInventory(menu);
    }

    @EventHandler(priority=EventPriority.HIGHEST) public void onClick(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)||!holder.type().startsWith("sandbox:"))return;
        event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player)||!player.hasPermission(PERMISSION)){event.getWhoClicked().closeInventory();return;}
        if(event.getClickedInventory()!=event.getView().getTopInventory())return;ItemStack clicked=event.getCurrentItem();if(clicked==null||!clicked.hasItemMeta())return;
        String action=clicked.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(action==null)return;
        if(action.equals("close")){player.closeInventory();return;}if(action.equals("back")){openMain(player);return;}
        switch(action){
            case "itembrowser" -> player.performCommand("itembrowser");
            case "skills" -> openSkills(player); case "coins" -> openCoins(player); case "playerlevel" -> openPlayerLevel(player); case "jobs" -> openJobs(player);
            case "edititem" -> {if(!validHeld(player,true))return;player.performCommand("edititem");}
            case "enchants" -> {if(!validHeld(player,false))return;customItems.openSandboxEnchantmentEditor(player,0);}
            default -> {if(action.startsWith("skill:"))modifySkill(player,action.substring(6),event.getClick());else if(action.startsWith("job:"))modifyJob(player,action.substring(4),event.getClick());else if(action.startsWith("coin:"))modifyCoins(player,action.substring(5));else if(action.startsWith("plevel:"))modifyPlayerLevel(player,action.substring(7));else if(action.startsWith("pxp:"))modifyPlayerXp(player,action.substring(4));else if(action.startsWith("plreq:"))handleLevelRequirementAction(player,action.substring(6));}
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST) public void onDrag(InventoryDragEvent event){
        if(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder&&holder.type().startsWith("sandbox:"))event.setCancelled(true);
    }

    private void modifySkill(Player p,String id,ClickType click){int before=skills.getLevel(p,id);int next=adjust(before,click);skills.setLevel(p,id,next);log(p,"skill "+id,skills.getLevel(p,id)-before);refresh(p);openSkills(p);}
    private void modifyJob(Player p,String id,ClickType click){int before=jobs.getLevel(p,id);int next=adjust(before,click);jobs.setLevel(p,id,next);log(p,"job "+id,jobs.getLevel(p,id)-before);refresh(p);openJobs(p);}
    private int adjust(int current,ClickType click){if(click==ClickType.MIDDLE)return 1;if(click==ClickType.SHIFT_LEFT)return current+10;if(click==ClickType.SHIFT_RIGHT)return current-10;if(click==ClickType.RIGHT)return current-1;return current+1;}
    private void modifyCoins(Player p,String value){double before=economy.getCoins(p);if(value.equals("reset"))economy.setCoins(p.getUniqueId(),0,"sandbox reset");else{double change=Double.parseDouble(value);economy.setCoins(p.getUniqueId(),Math.max(0,before+change),"sandbox");}log(p,"coins",economy.getCoins(p)-before);refresh(p);openCoins(p);}
    private void modifyPlayerLevel(Player p,String value){int before=playerLevels.getLevel(p);if(value.equals("reset")){playerLevels.setLevel(p,1);playerLevels.addXp(p,-playerLevels.getXp(p));}else if(value.startsWith("set"))playerLevels.setLevel(p,Integer.parseInt(value.substring(3)));else playerLevels.setLevel(p,before+Integer.parseInt(value));log(p,"player level",playerLevels.getLevel(p)-before);refresh(p);openPlayerLevel(p);}
    private void modifyPlayerXp(Player p,String value){int amount=Integer.parseInt(value);playerLevels.addXp(p,amount);log(p,"player XP",amount);refresh(p);openPlayerLevel(p);}
    private void handleLevelRequirementAction(Player p,String action){switch(action){case"show"->p.sendMessage(CC.color("&eNext requirements: &f"+playerLevels.nextRequirementsSummary(p)));case"items"->playerLevels.giveNextRequiredItems(p);case"skills"->playerLevels.setNextRequiredSkills(p);case"force"->playerLevels.forceLevelUp(p);case"validate"->p.sendMessage(CC.color("&a"+playerLevels.validateRequirementRegistry()));case"recalc"->playerLevels.recalculateLevelStatRewards(p);}refresh(p);openPlayerLevel(p);}
    private boolean validHeld(Player p,boolean custom){ItemStack held=p.getInventory().getItemInMainHand();if(held.getType().isAir()||(custom&&customItems.getItemId(held)==null)){p.sendMessage(CC.color(custom?"&cHold a custom item to edit it.":"&cHold an item to edit enchantments."));return false;}return true;}
    private void refresh(Player p){game.getBoardManager().update(p);game.getTablistManager().apply(p);if(game.getMmoStatService()!=null)game.getMmoStatService().refreshPlayerMMOStats(p);}
    private void log(Player p,String type,double amount){game.getLogger().info(p.getName()+" used sandbox to change "+type+" by "+amount);}

    private Inventory menu(String type,int size,String title){return Bukkit.createInventory(new MenuHolder(type),size,CC.color(title));}
    private ItemStack item(Material material,String name,List<String> lore,String action){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setDisplayName(CC.color(name));meta.setLore(lore.stream().map(CC::color).toList());meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);item.setItemMeta(meta);return item;}
    private void navigation(Inventory menu){menu.setItem(45,item(Material.ARROW,"&cBack",List.of(),"back"));menu.setItem(49,item(Material.BARRIER,"&cClose",List.of(),"close"));fill(menu);}
    private void fill(Inventory menu){ItemStack pane=item(Material.BLACK_STAINED_GLASS_PANE," ",List.of(),"noop");for(int i=0;i<menu.getSize();i++)if(menu.getItem(i)==null)menu.setItem(i,pane);}
    private int[] contentSlots(){return new int[]{10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};}
    private String pretty(String id){String s=id.toLowerCase(Locale.ROOT).replace('_',' ');return Character.toUpperCase(s.charAt(0))+s.substring(1);}
    private Material skillIcon(String id){return switch(id.toLowerCase()){case"combat"->Material.IRON_SWORD;case"mining"->Material.DIAMOND_PICKAXE;case"farming"->Material.WHEAT;case"fishing"->Material.FISHING_ROD;case"foraging","woodcutting"->Material.DIAMOND_AXE;case"enchanting"->Material.ENCHANTING_TABLE;case"alchemy"->Material.BREWING_STAND;default->Material.EXPERIENCE_BOTTLE;};}
    private Material jobIcon(String id){return switch(id.toLowerCase()){case"miner"->Material.IRON_PICKAXE;case"farmer"->Material.IRON_HOE;case"fisher"->Material.FISHING_ROD;case"hunter"->Material.BOW;case"lumberjack"->Material.IRON_AXE;case"builder"->Material.BRICKS;default->Material.WRITABLE_BOOK;};}
}
