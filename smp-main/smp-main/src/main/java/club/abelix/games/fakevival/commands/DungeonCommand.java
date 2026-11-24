package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.DungeonManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.menus.DungeonMenuService;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public final class DungeonCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final DungeonManager dungeonManager;
    private final DungeonMenuService dungeonMenus;

    public DungeonCommand(JavaPlugin plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.dungeonMenus = plugin instanceof Game game ? new DungeonMenuService(game) : null;
    }

    public DungeonMenuService menus() { return dungeonMenus; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = label.toLowerCase();
        if (cmd.equals("dungeon") || cmd.equals("dungeons")) return handleDungeon(sender, args);
        if (plugin instanceof Game game && !game.getNetworkService().canRunDungeonInstances()) {
            sender.sendMessage(CC.color("&cDungeon setup commands can only be used on dungeon servers."));
            return true;
        }
        if (!hasDungeonAdmin(sender)) {
            sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));
            return true;
        }
        if (cmd.equals("createdungeon") && args.length >= 1) {
            String worldName = dungeonManager.createDungeon(args[0]);
            boolean swmCreated = createSwmWorld(sender, worldName);
            sender.sendMessage(CC.color("&aCreated dungeon &f" + args[0] + " &awith world &f" + worldName + "&a."));
            if (swmCreated) sender.sendMessage(CC.color("&aRan SWP create command for &f" + worldName + "&a."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.color("&cOnly players can set dungeon locations."));
            return true;
        }
        if (cmd.equals("setdungeonportal") && args.length >= 1) {
            dungeonManager.setPortal(args[0], player.getLocation());
            sender.sendMessage(CC.color("&aSet dungeon portal for &f" + args[0] + "&a."));
            return true;
        }
        if (cmd.equals("setdungeonspawn") && args.length >= 1) {
            dungeonManager.setSpawn(args[0], player.getLocation());
            sender.sendMessage(CC.color("&aSet dungeon spawn for &f" + args[0] + "&a."));
            return true;
        }
        if (cmd.equals("setmobspawnlocation") && args.length >= 2) {
            var template = dungeonManager.dungeonMobTemplate(args[1]);
            if (template == null) {
                plugin.getLogger().warning("[SMP] Unknown dungeon mob alias '" + args[1] + "' for dungeon '"
                        + DungeonManager.canonicalDungeonId(args[0]) + "'. Use /dungeon mob aliases.");
                sender.sendMessage(CC.color("&cUnknown dungeon mob type: " + args[1]));
                sender.sendMessage(CC.color("&7Use &e/dungeon mob aliases &7to see valid options."));
                return true;
            }
            dungeonManager.setMobSpawn(args[0], args[1], player.getLocation());
            sender.sendMessage(CC.color("&aSet &c" + template.displayName() + " &7(" + template.entityType().name()
                    + ") &aspawn for dungeon &f" + args[0] + "&a."));
            return true;
        }
        if (cmd.equals("addmobspawnlocation")) {
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cUsage: /addmobspawnlocation <dungeonName> <mobName> <level>"));
                return true;
            }
            if (!isInt(args[2])) {
                sender.sendMessage(CC.color("&cLevel must be a number."));
                return true;
            }
            var template = dungeonManager.dungeonMobTemplate(args[1]);
            if (template == null) {
                plugin.getLogger().warning("[SMP] Unknown dungeon mob alias '" + args[1] + "' for dungeon '"
                        + DungeonManager.canonicalDungeonId(args[0]) + "'. Use /dungeon mob aliases.");
                sender.sendMessage(CC.color("&cUnknown dungeon mob type: " + args[1]));
                sender.sendMessage(CC.color("&7Use &e/dungeon mob aliases &7to see valid options."));
                return true;
            }
            int level = Math.max(1, Integer.parseInt(args[2]));
            String key = dungeonManager.addMobSpawn(args[0], args[1], level, template.displayName(), player.getLocation());
            sender.sendMessage(CC.color("&aAdded dungeon mob spawn &e" + key + " &afor &c" + template.displayName()
                    + " &7(" + template.entityType().name() + ") &aat level &e" + level + "&a."));
            return true;
        }
        if (cmd.equals("setbosslocation")) {
            if(args.length<3){sender.sendMessage(CC.color("&cUsage: /setbosslocation <dungeonname> <mobId> <level>"));return true;}
            String dungeon=DungeonManager.canonicalDungeonId(args[0]),boss=args[1].toLowerCase(java.util.Locale.ROOT);
            if(dungeonManager.getDungeon(dungeon)==null){sender.sendMessage(CC.color("&cUnknown dungeon: &e"+dungeon+"&c."));return true;}
            if(!dungeonManager.bossTypeExists(boss)){sender.sendMessage(CC.color("&cUnknown boss type: &e"+boss+"&c. Use &e/dungeon bosstype &cto view boss types."));return true;}
            if(!dungeonManager.bossBelongsToDungeon(boss,dungeon)){sender.sendMessage(CC.color("&cBoss &e"+boss+" &cbelongs to a different dungeon."));return true;}
            if(!isInt(args[2])||Integer.parseInt(args[2])<=0){sender.sendMessage(CC.color("&cBoss level must be a positive number."));return true;}
            int level=Integer.parseInt(args[2]);dungeonManager.setBossLocation(dungeon,boss,level,player.getLocation());
            sender.sendMessage(CC.color("&aSet boss location for dungeon &e"+dungeon+"&a."));sender.sendMessage(CC.color("&7Boss: &c"+boss));sender.sendMessage(CC.color("&7Level: &e"+level));
            return true;
        }
        if (cmd.equals("setdungeonarea") && args.length >= 2 && (args[1].equalsIgnoreCase("pos1") || args[1].equalsIgnoreCase("pos2"))) {
            dungeonManager.setAreaPos(args[0], args[1], player.getLocation());
            sender.sendMessage(CC.color("&aSet dungeon area &f" + args[1].toLowerCase() + " &afor &f" + args[0] + "&a."));
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /" + label + " <dungeon>"));
        return true;
    }

    private boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private String joinArgs(String[] args, int from, int toExclusive) {
        return String.join(" ", java.util.Arrays.copyOfRange(args, from, toExclusive));
    }

    private boolean handleDungeon(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player && dungeonMenus != null) {
                dungeonMenus.openSelection(player, 0);
                return true;
            }
            sendDungeonHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            sendDungeonHelp(sender,args.length>1&&isInt(args[1])?Integer.parseInt(args[1]):1);
            return true;
        }
        if (args[0].equalsIgnoreCase("menu")) {
            if (sender instanceof Player player && dungeonMenus != null) dungeonMenus.openSelection(player, 0);
            else sender.sendMessage(CC.color("&cOnly players can open dungeon menus."));
            return true;
        }
        if (args[0].equalsIgnoreCase("lootchest")) {
            if (!(plugin instanceof Game game)) return true;
            return game.getDungeonLootChestManager().handleCommand(sender, args);
        }
        if (args[0].equalsIgnoreCase("trap")) {
            if (!(plugin instanceof Game game)) return true;
            return game.getDungeonTrapManager().handleCommand(sender, args);
        }
        if(args[0].equalsIgnoreCase("spectate")){
            if(!(sender instanceof Player player)){sender.sendMessage(CC.color("&cOnly players can spectate dungeons."));return true;}
            if(args.length>1&&args[1].equalsIgnoreCase("leave")){((Game)plugin).getDungeonSpectateService().leave(player);return true;}
            if(!player.hasPermission("fakevival.dungeon.spectate")){player.sendMessage(CC.color("&cYou do not have permission to spectate dungeons."));return true;}
            ((Game)plugin).getDungeonSpectateService().openRuns(player,0);return true;
        }
        if(args[0].equalsIgnoreCase("mob")){
            if(!hasDungeonAdmin(sender)){sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));return true;}
            if(args.length>1&&args[1].equalsIgnoreCase("aliases")){
                sender.sendMessage(CC.color("&6Dungeon mob aliases:"));
                sender.sendMessage(CC.color("&e"+String.join(", ",dungeonManager.dungeonMobAliases())));
                return true;
            }
            if(args.length>1&&args[1].equalsIgnoreCase("validate")){
                sendDungeonMobValidation(sender,dungeonManager.validateDungeonMobs(args.length>2?args[2]:null,true));
                return true;
            }
            sender.sendMessage(CC.color("&cUsage: /dungeon mob <aliases|validate [dungeon]>"));
            return true;
        }
        if(args[0].equalsIgnoreCase("boss")&&args.length>1&&args[1].equalsIgnoreCase("validate")){
            if(!hasDungeonAdmin(sender)){sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));return true;}
            sendDungeonBossValidation(sender,dungeonManager.validateDefaultDungeonBosses());
            sendDungeonHealthValidation(sender);
            return true;
        }
        if(args[0].equalsIgnoreCase("validate")&&args.length==1){
            if(!hasDungeonAdmin(sender)){sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));return true;}
            sendDungeonMobValidation(sender,dungeonManager.validateDungeonMobs(null,true));
            sendDungeonBossValidation(sender,dungeonManager.validateDefaultDungeonBosses());
            sendDungeonHealthValidation(sender);
            return true;
        }
        if(args[0].equalsIgnoreCase("bosstype")||args[0].equalsIgnoreCase("bosstypes")||args[0].equalsIgnoreCase("bosses")){if(!hasDungeonAdmin(sender)){sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));return true;}sendMobTypes(sender,dungeonManager.bossTypes(),args.length>1&&isInt(args[1])?Integer.parseInt(args[1]):1,true);return true;}
        if(args[0].equalsIgnoreCase("mobtype")||args[0].equalsIgnoreCase("mobtypes")||args[0].equalsIgnoreCase("mobs")){if(!hasDungeonAdmin(sender)){sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));return true;}sendMobTypes(sender,dungeonManager.mobTypes(),args.length>1&&isInt(args[1])?Integer.parseInt(args[1]):1,false);return true;}
        if(args[0].equalsIgnoreCase("runs")){if(!hasDungeonAdmin(sender)){sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));return true;}((Game)plugin).getDungeonSpectateService().sendRunList(sender);return true;}
        if(args[0].equalsIgnoreCase("runinfo")&&args.length>1){if(!hasDungeonAdmin(sender))return true;DungeonManager.RunView run=dungeonManager.runView(args[1]);sender.sendMessage(CC.color(run==null?"&cThat dungeon run is no longer active.":"&e"+run.runId()+" &7dungeon=&d"+run.dungeonId()+" &7server=&b"+run.serverId()+" &7world=&f"+run.instanceWorld()+" &7state=&a"+run.state()+" &7participants=&e"+run.participants().size()));return true;}
        if(args[0].equalsIgnoreCase("forceclose")&&args.length>1){if(!hasDungeonAdmin(sender))return true;sender.sendMessage(CC.color(dungeonManager.forceCloseRun(args[1])?"&aDungeon run force-closed.":"&cThat dungeon run is no longer active."));return true;}
        if(args[0].equalsIgnoreCase("teleport")&&args.length>1){if(!hasDungeonAdmin(sender)||!(sender instanceof Player player))return true;((Game)plugin).getDungeonSpectateService().openPlayers(player,args[1]);return true;}
        if (args.length >= 2 && args[args.length - 1].equalsIgnoreCase("menu")) {
            String requestedId = joinArgs(args, 0, args.length - 1);
            if (!(sender instanceof Player player) || dungeonMenus == null) sender.sendMessage(CC.color("&cOnly players can open dungeon menus."));
            else if (!dungeonMenus.openDetails(player, requestedId)) sender.sendMessage(CC.color("&cUnknown dungeon &e" + DungeonManager.canonicalDungeonId(requestedId) + "&c."));
            return true;
        }
        if (args.length >= 2 && args[args.length - 1].equalsIgnoreCase("mobsmanager")) {
            String requestedId = joinArgs(args, 0, args.length - 1);
            if (!hasDungeonAdmin(sender)) { sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command.")); return true; }
            if (!(plugin instanceof Game game) || !game.getNetworkService().isDungeon()) { sender.sendMessage(CC.color("&cDungeon setup commands can only be used on dungeon servers.")); return true; }
            if (!(sender instanceof Player player) || dungeonMenus == null) sender.sendMessage(CC.color("&cOnly players can open this menu."));
            else if (!dungeonMenus.openMobs(player, requestedId, 0)) sender.sendMessage(CC.color("&cUnknown dungeon &e" + DungeonManager.canonicalDungeonId(requestedId) + "&c."));
            return true;
        }
        if (plugin instanceof Game game && !game.getNetworkService().isDungeon()) {
            sender.sendMessage(CC.color("&cDungeon setup commands can only be used on dungeon servers."));
            return true;
        }
        String trailingAction = args.length >= 2 ? args[args.length - 1].toLowerCase() : "";
        if (args.length >= 2 && (trailingAction.equals("info") || trailingAction.equals("enable") || trailingAction.equals("disable"))) {
            if (!hasDungeonAdmin(sender)) {
                sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));
                return true;
            }
            String canonicalId = DungeonManager.canonicalDungeonId(joinArgs(args, 0, args.length - 1));
            DungeonManager.DungeonDefinition definition = dungeonManager.getDungeon(canonicalId);
            if (definition == null) {
                sender.sendMessage(CC.color("&cUnknown dungeon &e" + canonicalId + "&c."));
                return true;
            }
            if (trailingAction.equals("enable") || trailingAction.equals("disable")) {
                boolean enabled = trailingAction.equals("enable");
                dungeonManager.setEnabled(canonicalId, enabled);
                sender.sendMessage(CC.color(enabled
                        ? "&aDungeon &e" + canonicalId + " &ais now enabled."
                        : "&cDungeon &e" + canonicalId + " &cis now disabled."));
                return true;
            }
            sendDungeonInfo(sender, definition);
            return true;
        }
        if (args[0].equalsIgnoreCase("setup")) {
            if (sender instanceof Player player) openSetupHelp(player); else sendConsoleSetupHelp(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(CC.color("&aDungeons: &f" + dungeonManager.listDungeons()));
            return true;
        }
        if (args[0].equalsIgnoreCase("debugmobs")) {
            if (!sender.hasPermission("fakevival.dungeon.admin")) {
                sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));
                return true;
            }
            dungeonManager.debugMobLines().forEach(line -> sender.sendMessage(CC.color(line)));
            if (plugin instanceof club.abelix.games.fakevival.Game game) {
                game.getZoneManager().debugDungeonMobLines().forEach(line -> sender.sendMessage(CC.color(line)));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("join") && args.length >= 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cOnly players can join dungeons."));
                return true;
            }
            dungeonManager.enterFromMenu(player, args[1]);
            return true;
        }
        if (args[0].equalsIgnoreCase("clearallportals")) {
            if (!hasDungeonAdmin(sender)) {
                sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));
                return true;
            }
            int cleared = dungeonManager.clearAllPortals();
            if (cleared <= 0) {
                sender.sendMessage(CC.color("&eThere are no dungeon portals to clear."));
            } else {
                sender.sendMessage(CC.color("&aCleared " + cleared + " dungeon portals."));
            }
            return true;
        }
        if (!hasDungeonAdmin(sender)) {
            sender.sendMessage(CC.color("&cYou need ADMIN or Higher to use this command."));
            return true;
        }
        if (args[0].equalsIgnoreCase("setrequirement") && args.length >= 4) {
            if (dungeonMenus != null && dungeonMenus.setRequirement(args[1], args[2], args[3])) sender.sendMessage(CC.color("&aUpdated dungeon requirement."));
            else sender.sendMessage(CC.color("&cUsage: /dungeon setrequirement <dungeon> <player|combat|key|permission> <value>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            dungeonManager.refreshDungeonCache();
            plugin.getLogger().info("[Dungeon] Dungeon setup reloaded from the current DataManager registry.");
            sender.sendMessage(CC.color("&aReloaded dungeon setup from the database/registry."));
            return true;
        }
        if (args[0].equalsIgnoreCase("validate") && args.length >= 2) {
            String id = DungeonManager.canonicalDungeonId(joinArgs(args, 1, args.length));
            sendDungeonMobValidation(sender,dungeonManager.validateDungeonMobs(id,true));
            sendDungeonBossValidation(sender,dungeonManager.validateDefaultDungeonBosses());
            sendDungeonHealthValidation(sender);
            dungeonManager.validateDungeonTemplate(id).whenComplete((result, throwable) ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (throwable != null) {
                            sender.sendMessage(CC.color("&cCould not validate dungeon &e" + id + "&c: " + rootMessage(throwable)));
                            return;
                        }
                        sender.sendMessage(CC.color("&aDungeon " + id + " validation:"));
                        sender.sendMessage(CC.color("&7Template World: " + (!result.templateExists() ? "&cMISSING"
                                : result.worldLoadable() ? "&aFOUND / LOADABLE" : "&cFOUND / LOAD FAILED")));
                        sender.sendMessage(CC.color("&7Dungeon Spawn: " + (result.spawnExists() ? "&aFOUND" : "&cMISSING")));
                        sender.sendMessage(CC.color("&7Mob Spawns: &e" + result.mobSpawns()));
                        sender.sendMessage(CC.color("&7Boss Spawns: &e" + result.bossSpawns()));
                        sender.sendMessage(CC.color("&7Ready: " + (result.ready() ? "&aYES" : "&cNO")));
                    }));
            return true;
        }
        if (args[0].equalsIgnoreCase("enable") && args.length >= 2) {
            dungeonManager.setEnabled(args[1], true);
            sender.sendMessage(CC.color("&aDungeon &e" + DungeonManager.canonicalDungeonId(args[1]) + " &ais now enabled."));
            return true;
        }
        if (args[0].equalsIgnoreCase("disable") && args.length >= 2) {
            dungeonManager.setEnabled(args[1], false);
            sender.sendMessage(CC.color("&cDungeon &e" + DungeonManager.canonicalDungeonId(args[1]) + " &cis now disabled."));
            return true;
        }
        if (args[0].equalsIgnoreCase("save") && args.length >= 2) {
            String id = DungeonManager.canonicalDungeonId(args[1]);
            boolean force = args.length >= 3 && args[2].equalsIgnoreCase("--force");
            sender.sendMessage(CC.color("&7Saving dungeon template and setup to MariaDB..."));
            String saverUuid = sender instanceof Player player ? player.getUniqueId().toString() : null;
            dungeonManager.saveDungeonTemplate(id, force, saverUuid, sender.getName()).whenComplete((message, throwable) ->
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (throwable != null) {
                            plugin.getLogger().warning("[SMP] Dungeon save failed for " + id + ": " + rootMessage(throwable));
                            sender.sendMessage(CC.color("&cCould not save dungeon &e" + id + "&c. Check console."));
                        } else sender.sendMessage(CC.color("&aSaved dungeon &e" + id + " &aworld and setup to the database."));
                    }));
            return true;
        }
        if (args[0].equalsIgnoreCase("setspawnportal")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cOnly players can set the spawn dungeon portal."));
                return true;
            }
            dungeonManager.setSpawnPortal(player.getLocation());
            sender.sendMessage(CC.color("&aSet the rotating spawn dungeon portal location."));
            return true;
        }
        if (args[0].equalsIgnoreCase("rotateportal")) {
            dungeonManager.rotateSpawnPortal(true);
            sender.sendMessage(CC.color("&aRotated the spawn dungeon portal."));
            return true;
        }
        if (args[0].equalsIgnoreCase("portalwand")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cOnly players can get the portal wand."));
                return true;
            }
            ItemStack wand = new ItemStack(Material.BLAZE_ROD);
            ItemMeta meta = wand.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(CC.color("&dDungeon Portal Wand"));
                meta.setLore(java.util.List.of(CC.color("&7Left Click: set pos1"), CC.color("&7Right Click: set pos2")));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "dungeon_portal_wand"), PersistentDataType.BYTE, (byte) 1);
                wand.setItemMeta(meta);
            }
            player.getInventory().addItem(wand);
            sender.sendMessage(CC.color("&aGiven dungeon portal wand."));
            return true;
        }
        if (args[0].equalsIgnoreCase("portalinfo")) {
            sender.sendMessage(CC.color("&a" + dungeonManager.getSpawnPortalInfo()));
            return true;
        }
        if ((args[0].equalsIgnoreCase("spawnportal") || args[0].equalsIgnoreCase("openrandom")) && args.length >= 2) {
            dungeonManager.spawnRandomPortal(args[1]);
            sender.sendMessage(CC.color("&aSpawned a random dungeon portal for &f" + args[1] + "&a."));
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /dungeon join <name>, /dungeon enable <name>, /dungeon setspawnportal, /dungeon rotateportal, /dungeon clearallportals"));
        return true;
    }

    private void sendMobTypes(CommandSender sender,List<DungeonManager.MobTypeView> types,int requested,boolean bosses){int per=10,pages=Math.max(1,(types.size()+per-1)/per),page=Math.clamp(requested,1,pages);sender.sendMessage(CC.color("&6Dungeon "+(bosses?"Boss":"Mob")+" Types &7(Page "+page+"/"+pages+")"));for(int i=(page-1)*per;i<Math.min(types.size(),page*per);i++){DungeonManager.MobTypeView type=types.get(i);sender.sendMessage(CC.color("&e"+type.id()+" &7- "+(bosses?"&c":"&f")+type.displayName()+" &7"+(bosses?"Lv."+type.defaultLevel():"Role: &b"+type.role())));}if(types.isEmpty())sender.sendMessage(CC.color("&7No registered "+(bosses?"boss":"mob")+" types were found."));}

    private void sendDungeonMobValidation(CommandSender sender, DungeonManager.DungeonMobValidation result) {
        sender.sendMessage(CC.color("&7Dungeon mob validation: checked=&e" + result.checked() + "&7, repaired=&e"
                + result.repaired() + "&7, invalid=&e" + result.invalid() + "&7, dirty names=&e" + result.dirtyNames() + "&7."));
        sender.sendMessage(CC.color("&7EntityTypes valid=" + yes(result.entityTypesValid()) + "&7, aliases valid="
                + yes(result.aliasesValid()) + "&7, singular names=" + yes(result.displayNamesClean())
                + "&7, MMO/PDC health=" + yes(result.pdcHealthSource()) + "&7, exact red nametag="
                + yes(result.exactNametagFormat()) + "&7, YAML created=&afalse&7."));
        if (!result.unknownAliases().isEmpty()) sender.sendMessage(CC.color("&cUnknown aliases: " + String.join(", ", result.unknownAliases())));
    }

    private void sendDungeonBossValidation(CommandSender sender, DungeonManager.DungeonBossValidation result) {
        sender.sendMessage(CC.color("&6Default dungeon boss validation:"));
        sender.sendMessage(CC.color("&7Pirate registered=" + yes(result.pirateRegistered()) + "&7, dungeon=" + yes(result.pirateDungeon())
                + "&7, Sacred registered=" + yes(result.sacredRegistered()) + "&7, dungeon=" + yes(result.sacredDungeon())
                + "&7, existing command usable=" + yes(result.commandUsable()) + "&7."));
        sender.sendMessage(CC.color("&7EntityTypes=" + yes(result.entityTypesValid()) + "&7, nametags=" + yes(result.nametagsValid())
                + "&7, MMO/PDC health=" + yes(result.pdcHealthSource()) + "&7, bossbars=" + yes(result.bossbarsValid()) + "&7."));
        sender.sendMessage(CC.color("&7Abilities=" + yes(result.abilitiesRegistered()) + "&7, cooldowns=" + yes(result.cooldownsValid())
                + "&7, minions=" + yes(result.minionsValid()) + "&7, missing drops skipped=" + yes(result.missingDropsSkipped()) + "&7."));
        sender.sendMessage(CC.color("&7Duplicate blocked=" + yes(result.duplicateSpawnBlocked()) + "&7, reset=" + yes(result.resetValid())
                + "&7, minions clear=" + yes(result.minionsClear()) + "&7, YAML created=" + (result.noYamlCreated() ? "&afalse" : "&ctrue")
                + "&7, added comments=" + (result.noAddedCodeComments() ? "&afalse" : "&ctrue") + "&7."));
        if (!result.missingSpawns().isEmpty()) sender.sendMessage(CC.color("&cMissing boss spawns: " + String.join(", ", result.missingSpawns())));
    }

    private void sendDungeonHealthValidation(CommandSender sender) {
        DungeonManager.BossHealthValidation result = dungeonManager.validateBossHealthScaling();
        sender.sendMessage(CC.color("&7Normal Lv.15 HP=&e" + (long) result.normalLevel15Health()
                + "&7, dungeon boss Lv.15 HP=&e" + (long) result.dungeonBossLevel15Health()
                + "&7, all dungeon bosses >=8x=" + yes(result.allBossesMeetMinimum())
                + "&7, PDC/MMO health=" + yes(result.pdcHealthSource())
                + "&7, PDC nametag/bossbar=" + yes(result.pdcNametagAndBossbar())
                + "&7, Bukkit safe cap=" + yes(result.safeBukkitHealth()) + "&7."));
    }

    private String yes(boolean value) { return value ? "&atrue" : "&cfalse"; }

    private boolean hasDungeonAdmin(CommandSender sender) {
        return sender.hasPermission("fakevival.admin.dungeon") || sender.hasPermission("fakevival.dungeon.admin");
    }

    private void sendDungeonInfo(CommandSender sender, DungeonManager.DungeonDefinition definition) {
        String id = definition.id();
        sender.sendMessage(CC.color("&6&lDungeon Info"));
        sender.sendMessage(CC.color("&7canonicalId: &e" + id));
        sender.sendMessage(CC.color("&7enabled: " + (dungeonManager.isEnabled(id) ? "&atrue" : "&cfalse")));
        sender.sendMessage(CC.color("&7source: &e" + dungeonManager.dungeonSource(id)));
        sender.sendMessage(CC.color("&7template/world: &e" + dungeonManager.getWorldName(id)));
        sender.sendMessage(CC.color("&7mob count: &e" + dungeonManager.configuredMobCount(id)));
        sender.sendMessage(CC.color("&7boss count: &e" + dungeonManager.configuredBossCount(id)));
        sender.sendMessage(CC.color("&7required player level: &e" + dungeonManager.requiredPlayerLevel(id)));
        sender.sendMessage(CC.color("&7required combat level: &e" + dungeonManager.requiredCombatLevel(id)));
        sender.sendMessage(CC.color("&7required key: &e" + (dungeonManager.requiredKey(id).isBlank() ? "None" : dungeonManager.requiredKey(id))));
        if (sender instanceof Player player) {
            var access = dungeonManager.getAccessResult(player, id);
            sender.sendMessage(CC.color("&7current player requirements: " + (access.requirementsMet() ? "&aMET" : "&cMISSING")));
            if (!access.reasons().isEmpty()) sender.sendMessage(CC.color("&7missing: &c" + String.join(", ", access.reasons())));
        }
    }

    private void sendDungeonHelp(CommandSender sender){sendDungeonHelp(sender,1);}
    private void sendDungeonHelp(CommandSender sender,int requested) {
        List<String> lines=new ArrayList<>(List.of(
                "&e/dungeon help [page] &7- Show dungeon commands.","&e/dungeon menu &7- Open dungeon selection.",
                "&e/dungeon join <dungeon> &7- Join a dungeon.","&e/dungeon leave &7- Leave your dungeon.",
                "&e/dungeon rejoin &7- Rejoin your active run.","&e/dungeon runs &7- View your dungeon runs."));
        if(sender.hasPermission("fakevival.dungeon.spectate"))lines.addAll(List.of("&e/dungeon spectate &7- Spectate an active run.","&e/dungeon spectate leave &7- Stop spectating."));
        if(hasDungeonAdmin(sender))lines.addAll(List.of(
                "&e/dungeon runinfo <runId> &7- Inspect a run.","&e/dungeon forceclose <runId> &7- Close a run.","&e/dungeon teleport <runId> &7- Open run players.",
                "&e/dungeon bosstype [page] &7- List boss IDs.","&e/dungeon boss validate &7- Validate default dungeon bosses.","&e/dungeon mobtype [page] &7- List mob IDs.","&e/dungeon mob <aliases|validate> &7- Resolve or validate mob aliases.","&e/dungeon validate <dungeon> &7- Validate setup.",
                "&e/dungeon lootchest <add|remove|list|tp|setloottable|setfacing|rotate|respawn|validate> &7- Manage loot chests.",
                "&e/dungeon trap <add|remove|list|tp|settype|setdamage|setcooldown|enable|disable|rearm|validate> &7- Manage traps.",
                "&e/dungeon save <dungeon> &7- Save setup and template.","&e/setdungeonspawn <dungeon> &7- Set player spawn.",
                "&e/setbosslocation <dungeon> <mobId> <level> &7- Set boss spawn.","&e/addmobspawnlocation <dungeon> <mobId> <level> &7- Add mob spawn.",
                "&e/dungeon clearallportals &7- Clear dungeon portals."));
        int per=7,pages=Math.max(1,(lines.size()+per-1)/per),page=Math.clamp(requested,1,pages);
        sender.sendMessage(CC.color("&6Dungeon Commands &7(Page "+page+"/"+pages+")"));
        for(int i=(page-1)*per;i<Math.min(lines.size(),page*per);i++)sender.sendMessage(CC.color(lines.get(i)));
    }

    private void openSetupHelp(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("dungeon-help"), 45, CC.color("&5Dungeon Setup Help"));
        inventory.setItem(4, item(Material.ENDER_EYE, "&5Dungeon Setup Guide", List.of(
                "&7Use this as the order for creating",
                "&7a working dungeon and portal.",
                "",
                "&7Creating a dungeon also runs:",
                "&f" + club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.swm.create-command", "swp create %world% file").replace("%world%", "<world>"),
                "",
                "&eTip: &7Stand where you want each",
                "&7location before running set commands."
        )));
        inventory.setItem(10, item(Material.WRITABLE_BOOK, "&e1. Create The Dungeon", List.of(
                "&7Creates the saved dungeon entry",
                "&7and runs the SWP world create command.",
                "",
                "&f/createdungeon <name>",
                "",
                "&8Example: /createdungeon crypt",
                "&8Runs: swp create crypt file"
        )));
        inventory.setItem(12, item(Material.LODESTONE, "&e2. Set Player Spawn", List.of(
                "&7Where players appear after joining.",
                "",
                "&f/setdungeonspawn <name>",
                "",
                "&8Stand inside the arena first."
        )));
        inventory.setItem(14, item(Material.IRON_BARS, "&e3. Protect The Area", List.of(
                "&7Sets the dungeon region corners.",
                "&7Blocks normal building inside it.",
                "",
                "&f/setdungeonarea <name> pos1",
                "&f/setdungeonarea <name> pos2"
        )));
        inventory.setItem(16, item(Material.ENDER_PEARL, "&e4. Set Entry Portal", List.of(
                "&7Static portal for this dungeon.",
                "",
                "&f/setdungeonportal <name>",
                "",
                "&7Optional rotating spawn portal:",
                "&f/dungeon setspawnportal"
        )));
        inventory.setItem(20, item(Material.ZOMBIE_HEAD, "&c5. Add Mob Spawns", List.of(
                "&7Adds regular dungeon mob spawn points.",
                "",
                "&f/setmobspawnlocation <name> <spawnId>",
                "",
                "&8Example: /setmobspawnlocation crypt skeleton_1"
        )));
        inventory.setItem(22, item(Material.DRAGON_HEAD, "&c6. Add Boss Spawns", List.of(
                "&7Sets boss spawn points for the dungeon.",
                "",
                "&f/setbosslocation <name> <bossId> <level>",
                "",
                "&8/setbosslocation piratedungeon",
                "&8pirate_captain_blackwake 75",
                "&8/setbosslocation sacreddungeon",
                "&8sacred_guardian_aurelius 90"
        )));
        inventory.setItem(24, item(Material.BLAZE_ROD, "&d7. Spawn Portal Tools", List.of(
                "&7Use a wand to resize the rotating",
                "&7spawn portal region.",
                "",
                "&f/dungeon portalwand",
                "&f/dungeon portalinfo",
                "&f/dungeon rotateportal"
        )));
        inventory.setItem(28, item(Material.LIME_DYE, "&a8. Enable And Test", List.of(
                "&7Enable the dungeon, then enter it.",
                "",
                "&f/dungeon enable <name>",
                "&f/dungeon join <name>",
                "",
                "&8Use /dungeon list to check names."
        )));
        inventory.setItem(29, item(Material.WRITABLE_BOOK, "&aSave Arena World", List.of(
                "&7Saves the dungeon arena world.",
                "&7Use this after building or editing",
                "&7the arena.",
                "",
                "&f/dungeon save <name>",
                "",
                "&8Default: Bukkit world save"
        )));
        inventory.setItem(30, item(Material.GRASS_BLOCK, "&aZone Dungeon Mobs", List.of(
                "&7For zone-based dungeon mobs, use:",
                "",
                "&f/zone create <name> dungeon",
                "&f/zone addspawn <name> <mob> [level]",
                "&f/zone addspawnboss <name> <mob> [level] [boss]"
        )));
        inventory.setItem(32, item(Material.REDSTONE_TORCH, "&cCommon Checks", List.of(
                "&7If mobs do not spawn:",
                "&f- dungeon/world must be loaded",
                "&f- dungeon must be enabled",
                "&f- spawn world difficulty must allow mobs",
                "&f- region corners must be set correctly"
        )));
        inventory.setItem(40, item(Material.BARRIER, "&cClose", List.of("&7Close this help menu.")));
        player.openInventory(inventory);
    }

    private void sendConsoleSetupHelp(CommandSender sender) {
        sender.sendMessage(CC.color("&5Dungeon Setup:"));
        sender.sendMessage(CC.color("&e1. &f/createdungeon <name>"));
        sender.sendMessage(CC.color("&e2. &f/setdungeonspawn <name>"));
        sender.sendMessage(CC.color("&e3. &f/setdungeonarea <name> pos1 &7and &fpos2"));
        sender.sendMessage(CC.color("&e4. &f/setdungeonportal <name>"));
        sender.sendMessage(CC.color("&e5. &f/setmobspawnlocation <name> <spawnId>"));
        sender.sendMessage(CC.color("&e6. &f/setbosslocation <name> <bossId> <level>"));
        sender.sendMessage(CC.color("&8/setbosslocation piratedungeon pirate_captain_blackwake 75"));
        sender.sendMessage(CC.color("&8/setbosslocation sacreddungeon sacred_guardian_aurelius 90"));
        sender.sendMessage(CC.color("&e7. &f/dungeon enable <name> &7then &f/dungeon join <name>"));
        sender.sendMessage(CC.color("&e8. &f/dungeon save <name> &7to save the arena world"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String label = command.getName().toLowerCase(java.util.Locale.ROOT);
        if (label.equals("setbosslocation")) {
            if (args.length == 1) return matching(args[0], dungeonManager.dungeonDefinitions().stream().map(DungeonManager.DungeonDefinition::id).toList());
            if (args.length == 2) return matching(args[1], dungeonManager.bossTypes().stream()
                    .map(DungeonManager.MobTypeView::id).filter(id -> dungeonManager.bossBelongsToDungeon(id, args[0])).toList());
            if (args.length == 3) return matching(args[2], List.of(String.valueOf(dungeonManager.defaultBossLevel(args[1]))));
            return List.of();
        }
        if (!label.equals("dungeon") && !label.equals("dungeons")) return List.of();
        if (args.length == 1) return matching(args[0], List.of("help", "menu", "join", "leave", "rejoin", "list", "boss", "bosstype", "mob", "mobtype", "validate", "runs", "runinfo", "forceclose", "save"));
        if (args.length == 2 && args[0].equalsIgnoreCase("boss")) return matching(args[1], List.of("validate"));
        if (args.length == 2 && args[0].equalsIgnoreCase("mob")) return matching(args[1], List.of("aliases", "validate"));
        if (args.length == 2 && Set.of("join", "validate", "save").contains(args[0].toLowerCase(java.util.Locale.ROOT)))
            return matching(args[1], dungeonManager.dungeonDefinitions().stream().map(DungeonManager.DungeonDefinition::id).toList());
        return List.of();
    }

    private List<String> matching(String prefix, List<String> values) {
        String normalized = prefix == null ? "" : prefix.toLowerCase(java.util.Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(java.util.Locale.ROOT).startsWith(normalized)).toList();
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        if (material == null || material.isAir() || !material.isItem()) material = Material.PAPER;
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(lore.stream().map(CC::color).toList());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean createSwmWorld(CommandSender sender, String worldName) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.swm.create-on-dungeon-create", true)) return false;
        String command = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.swm.create-command", "swp create %world% file")
                .replace("%world%", worldName)
                .replace("%name%", worldName)
                .replace("%storage%", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.swm.storage", "file"));
        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (!dispatched) sender.sendMessage(CC.color("&eDungeon was saved, but the SWP create command failed: &f/" + command));
        return dispatched;
    }

    private boolean saveDungeonWorld(CommandSender sender, String dungeonName, String worldName) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("dungeons.swm.save-enabled", true)) {
            sender.sendMessage(CC.color("&cDungeon arena saving is disabled in config."));
            return false;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(CC.color("&cDungeon world &f" + worldName + " &cis not loaded, so Bukkit cannot save it."));
            return false;
        }
        world.save();
        runExtraSaveCommands(dungeonName, worldName);
        return true;
    }

    private void runExtraSaveCommands(String dungeonName, String worldName) {
        for (String configured : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("dungeons.swm.save-extra-commands")) {
            if (configured == null || configured.isBlank()) continue;
            String command = configured
                    .replace("%world%", worldName)
                    .replace("%name%", dungeonName)
                    .replace("%storage%", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("dungeons.swm.storage", "file"));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
