package me.outerspace.thepit.commands;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.model.StatType;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.setup.PitSetupService;
import me.outerspace.thepit.setup.SetupValidationResult;
import me.outerspace.thepit.registry.definition.ArenaDefinition;
import me.outerspace.thepit.registry.definition.StoredLocation;
import me.outerspace.thepit.managers.ArenaManager;
import me.outerspace.thepit.managers.StatsManager;
import me.outerspace.thepit.managers.PlayerInventoryManager;
import me.outerspace.thepit.npc.PitNpcService;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PitCommand implements CommandExecutor, TabCompleter {
    private final ThePitPlugin plugin;
    private final Map<String,Long> replaceConfirmations=new HashMap<>();
    public PitCommand(ThePitPlugin plugin) { this.plugin = plugin; }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String lowerLabel = label.toLowerCase();
        if (lowerLabel.equals("npc")) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = "npc";
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        } else if (lowerLabel.equals("setfeaturehologram") || lowerLabel.equals("removefeaturehologram")) {
            String[] newArgs = new String[args.length + 2];
            newArgs[0] = lowerLabel.equals("setfeaturehologram") ? "setleaderboard" : "removeleaderboard";
            newArgs[1] = "unlockedfeatures";
            System.arraycopy(args, 0, newArgs, 2, args.length);
            args = newArgs;
        } else if (lowerLabel.equals("gmc") || lowerLabel.equals("gms") || lowerLabel.equals("gmsp") || lowerLabel.equals("gma") || lowerLabel.equals("unsafeenchant") || lowerLabel.equals("enchantunsafe") || lowerLabel.equals("spawn") || lowerLabel.equals("respawn") || lowerLabel.equals("oof") || lowerLabel.equals("trade") || lowerLabel.equals("tradelimits") || lowerLabel.equals("offer") || lowerLabel.equals("pickupstash") || lowerLabel.equals("dropconfirm") || lowerLabel.equals("show") || lowerLabel.equals("events") || lowerLabel.equals("event") || lowerLabel.equals("eventsetup") || lowerLabel.equals("carepackageset") || lowerLabel.equals("carepackageremove") || lowerLabel.equals("carepackagelist") || lowerLabel.equals("carepackagetest") || lowerLabel.equals("squad") || lowerLabel.equals("cool") || lowerLabel.equals("credit") || lowerLabel.equals("pitsupporter") || lowerLabel.equals("blue") || lowerLabel.equals("azul") || lowerLabel.equals("enderchest") || lowerLabel.equals("ec") || lowerLabel.equals("renown") || lowerLabel.equals("buildmode") || lowerLabel.equals("quests") || lowerLabel.equals("quest") || lowerLabel.equals("contracts") || lowerLabel.equals("contract") || lowerLabel.equals("contractadmin") || lowerLabel.equals("combatlog") || lowerLabel.equals("pitpurchasedebug") || lowerLabel.equals("stats") || lowerLabel.equals("shop") || lowerLabel.equals("shopadmin") || lowerLabel.equals("gold") || lowerLabel.equals("coins") || lowerLabel.equals("pitchat") || lowerLabel.equals("upgrades") || lowerLabel.equals("killstreaks") || lowerLabel.equals("view") || lowerLabel.equals("achievements") || lowerLabel.equals("achievementadmin") || lowerLabel.equals("setleaderboard") || lowerLabel.equals("removeleaderboard") || lowerLabel.equals("leaderboard")) {
            String[] newArgs = new String[args.length + 1];
            newArgs[0] = lowerLabel;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }
        if (args[0].equalsIgnoreCase("recoverkit")) {
            if (!has(sender, "thepit.admin")) return true;
            if (args.length < 2) {
                sender.sendMessage(CC.color("&c/pit recoverkit <player>"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            PlayerInventoryManager.RecoveryResult result = plugin.getPlayerInventoryManager().recoverStarter(target);
            if (result == PlayerInventoryManager.RecoveryResult.SUCCESS) sender.sendMessage(CC.color("&aRecovered and queued persistence of the starter equipment for &f" + target.getName() + "&a."));
            else if (result == PlayerInventoryManager.RecoveryResult.NOT_ONLINE) sender.sendMessage(CC.color("&cThat player is not online."));
            else if (result == PlayerInventoryManager.RecoveryResult.NOT_READY) sender.sendMessage(CC.color("&cThat player's profile and inventory have not completed loading."));
            else if (result == PlayerInventoryManager.RecoveryResult.NOT_EMPTY) sender.sendMessage(CC.color("&cRecovery refused because that player's gameplay inventory or armor is not empty."));
            else if (result == PlayerInventoryManager.RecoveryResult.ALREADY_RECOVERED) sender.sendMessage(CC.color("&cRecovery was already performed during this join session."));
            else sender.sendMessage(CC.color("&cStarter recovery failed. Review the inventory-persistence debug log."));
            return true;
        }
        if (args[0].equalsIgnoreCase("namedebug")) {
            if (!has(sender, "thepit.admin.namedebug")) return true;
            if (args.length < 2) {
                sender.sendMessage(CC.color("&c/pit namedebug <player>"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) sender.sendMessage(CC.color("&cThat player is not online."));
            else plugin.getTablistManager().nameDebug(sender, target);
            return true;
        }

        if (args[0].equalsIgnoreCase("packetentities")) {
            if (!has(sender, "thepit.admin.packetentities")) return true;
            PitNpcService service = plugin.getNpcService();
            me.outerspace.thepit.npc.PitPacketEntityEngine engine = me.outerspace.thepit.npc.PitPacketEntityEngine.shared();
            String action = args.length < 2 ? "status" : args[1].toLowerCase(Locale.ENGLISH);
            if (action.equals("respawn")) {
                if (args.length < 3) { sender.sendMessage(CC.color("&c/pit packetentities respawn <player>")); return true; }
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) { sender.sendMessage(CC.color("&cThat player is not online.")); return true; }
                service.respawn(target);
                sender.sendMessage(CC.color("&aRespawned nearby packet entities for &f" + target.getName() + "&a."));
                return true;
            }
            sender.sendMessage(CC.color("&6The Pit packet entities"));
            sender.sendMessage(CC.color("&7Initialized: " + (service.packetInitialized() ? "&atrue" : "&cfalse")));
            sender.sendMessage(CC.color("&7Arena instance: &f" + plugin.getArenaManager().getCurrentArena() + ":" + (plugin.getArenaManager().getCurrentWorld() == null ? "unloaded" : plugin.getArenaManager().getCurrentWorld().getUID())));
            sender.sendMessage(CC.color("&7NPC controllers: &f" + service.size()));
            sender.sendMessage(CC.color("&7Hologram lines: &f" + service.hologramCount()));
            sender.sendMessage(CC.color("&7Viewer links: &f" + service.viewerLinkCount()));
            sender.sendMessage(CC.color("&7Allocated virtual IDs: &f" + engine.activeIdCount()));
            sender.sendMessage(CC.color("&7Last packet failure: &f" + engine.lastFailure()));
            return true;
        }

        if(args[0].equalsIgnoreCase("combatlog")){
            if(!has(sender,"thepit.admin.combatlog"))return true;
            if(args.length<3){sender.sendMessage(CC.color("&c/combatlog <status|clear> <player>"));return true;}
            Player target=Bukkit.getPlayerExact(args[2]);if(target==null){sender.sendMessage(CC.color("&cThat player is not online."));return true;}
            if(args[1].equalsIgnoreCase("status")){for(String line:plugin.getCombatLogManager().status(target))sender.sendMessage(line);}
            else if(args[1].equalsIgnoreCase("clear")){plugin.getCombatLogManager().clear(target.getUniqueId());sender.sendMessage(CC.color("&aCleared combat state for &f"+target.getName()+"&a."));}
            else sender.sendMessage(CC.color("&c/combatlog <status|clear> <player>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("admin")) {
            if (!has(sender, "thepit.admin")) return true;
            sendAdminHelp(sender, label);
            return true;
        }

        if (args[0].equalsIgnoreCase("buildmode")) {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
            if (!has(sender, "thepit.admin.break")) return true;
            boolean enabled = plugin.toggleBuildMode(p);
            p.sendMessage(enabled ? "§aBuild mode enabled. You can place/break permanent arena blocks." : "§cBuild mode disabled. Arena protection is back on for you.");
            return true;
        }

        if (args[0].equalsIgnoreCase("ragepit")) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
            if (!has(sender, "thepit.admin.ragepit")) return true;
            Player player = (Player) sender;
            if (args.length < 3) { player.sendMessage("§c/pit ragepit <setshape|pos1|pos2|setcenter|setradius|setheight|preview|clearpreview|validate> <arena> [values]"); return true; }
            String action = args[1].toLowerCase(Locale.ENGLISH);
            String arena = me.outerspace.thepit.managers.PitYamlManager.normalizeArenaId(args[2]);
            if (!plugin.getYamlManager().containsArena(arena)) { player.sendMessage("§cUnknown arena in arenas.yml."); return true; }
            if (action.equals("setshape")) {
                if (args.length < 4) { player.sendMessage("§c/pit ragepit setshape <arena> <circle|ellipse|square|rectangle|polygon>"); return true; }
                plugin.getMinorEventManager().clearRagePitPreview(arena);
                player.sendMessage(plugin.getYamlManager().saveRagePitShape(arena, args[3]) ? "§aSaved Rage Pit shape §f" + args[3].toUpperCase(Locale.ENGLISH) + "§a for §e" + arena + "§a." : "§cUnknown shape or the Rage Pit configuration could not be saved.");
                return true;
            }
            if (action.equals("pos1") || action.equals("pos2")) {
                plugin.getMinorEventManager().clearRagePitPreview(arena);
                int corner = action.equals("pos1") ? 1 : 2;
                player.sendMessage(plugin.getYamlManager().saveRagePitCorner(arena, corner, player.getLocation()) ? "§aSaved Rage Pit corner " + corner + " for §e" + arena + "§a at §f" + player.getLocation().getBlockX() + ", " + player.getLocation().getBlockZ() + "§a." : "§cCould not save that corner. Stand in the arena's configured world.");
                return true;
            }
            if (action.equals("setcenter")) {
                plugin.getMinorEventManager().clearRagePitPreview(arena);
                player.sendMessage(plugin.getYamlManager().saveRagePitCenter(arena, player.getLocation()) ? "§aSaved Rage Pit center for §e" + arena + "§a." : "§cCould not save the center. Stand in that arena's configured world.");
                return true;
            }
            if (action.equals("setradius")) {
                if (args.length < 4) { player.sendMessage("§c/pit ragepit setradius <arena> <radius> [radius-z]"); return true; }
                Integer radiusX = integer(args[3]), radiusZ = args.length >= 5 ? integer(args[4]) : radiusX;
                if (radiusX == null || radiusZ == null || radiusX < 2 || radiusZ < 2) { player.sendMessage("§cRadii must be whole numbers of at least 2."); return true; }
                plugin.getMinorEventManager().clearRagePitPreview(arena);
                player.sendMessage(plugin.getYamlManager().saveRagePitRadius(arena, radiusX, radiusZ) ? "§aSaved Rage Pit radii §f" + radiusX + "§a x §f" + radiusZ + "§a for §e" + arena + "§a." : "§cCould not save Rage Pit radii.");
                return true;
            }
            if (action.equals("setheight")) {
                if (args.length < 5) { player.sendMessage("§c/pit ragepit setheight <arena> <minimum-y> <maximum-y>"); return true; }
                Integer minimum = integer(args[3]), maximum = integer(args[4]);
                if (minimum == null || maximum == null || minimum < 0 || maximum > 255 || minimum > maximum) { player.sendMessage("§cHeight must be within 0-255 and minimum-y must not exceed maximum-y."); return true; }
                plugin.getMinorEventManager().clearRagePitPreview(arena);
                player.sendMessage(plugin.getYamlManager().saveRagePitHeight(arena, minimum, maximum) ? "§aSaved Rage Pit height §f" + minimum + "§a through §f" + maximum + "§a for §e" + arena + "§a." : "§cCould not save Rage Pit height.");
                return true;
            }
            if (action.equals("preview")) { player.sendMessage(plugin.getMinorEventManager().previewRagePit(arena)); return true; }
            if (action.equals("clearpreview")) { player.sendMessage(plugin.getMinorEventManager().clearRagePitPreview(arena)); return true; }
            if (action.equals("validate")) { player.sendMessage(plugin.getMinorEventManager().validateRagePit(arena)); return true; }
            player.sendMessage("§c/pit ragepit <setshape|pos1|pos2|setcenter|setradius|setheight|preview|clearpreview|validate> <arena> [values]");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!has(sender, "thepit.admin.reload")) return true;
            plugin.getMinorEventManager().ragePitReloadCleanup();
            if (!plugin.registry().reload()) {
                sender.sendMessage("§cA The Pit YAML file is invalid. Previous registry values remain active.");
                return true;
            }
            plugin.getStorageManager().connect();
            plugin.getPurchaseRepository().initialize();
            if (!plugin.getSetupService().initialize()) {
                sender.sendMessage("Â§carenas.yml setup data is invalid. Previous runtime values remain active.");
                return true;
            }
            boolean arenasReloaded = plugin.getArenaManager().reloadFromYaml();
            plugin.getQuestManager().reloadOverrides();
            plugin.getPerkManager().reload();
            plugin.getPassiveManager().reload();
            plugin.getStatsMenuManager().reload();
            plugin.getLevelUpFeedbackManager().reload();
            plugin.getFishingManager().reload();
            plugin.getShopManager().reload();
            plugin.getAchievementManager().reload();
            plugin.getContractManager().reload();
            plugin.getPitCommandManager().reload();
            plugin.getKillstreakManager().reload();
            plugin.getDefaultLoadoutManager().reload();
            plugin.getCombatKnockbackCompatibility().reload();
            reloadRuntimeSetup();
            plugin.getAbelixRankPermissionManager().reload();
            sender.sendMessage(arenasReloaded ? "§aThe Pit reloaded." : "§carenas.yml was invalid. The previous arena cache remains active.");
            return true;
        }

        if(args[0].equalsIgnoreCase("setup")){
            if(args.length<2){sender.sendMessage("§c/pit setup <status|validate|export|import>");return true;}
            String action=args[1].toLowerCase(Locale.ENGLISH);
            if(action.equals("status")){
                if(!has(sender,"thepit.admin.setup"))return true;
                PitSetupService service=plugin.getSetupService();
                sender.sendMessage("§6The Pit setup status");
                sender.sendMessage("§7Database: "+(service.repository().databaseAvailable()?"§aConnected":"§7Disabled or unavailable"));
                sender.sendMessage("§7Schema version: §f"+service.schemaVersion());
                sender.sendMessage("§7Source: §f"+service.source());
                sender.sendMessage("§7Arenas: §f"+service.snapshot().arenas.size()+" §7Leaderboards: §f"+service.snapshot().leaderboards.size()+" §7NPCs: §f"+service.snapshot().npcs.size());
                sender.sendMessage("§7Last export: §f"+(service.lastExportAt().isEmpty()?"never":service.lastExportAt()));
                return true;
            }
            if(action.equals("validate")){
                if(!has(sender,"thepit.admin.setup"))return true;
                SetupValidationResult result=plugin.getSetupService().validateCurrent();
                if(result.valid())sender.sendMessage("§aThe Pit arena setup is valid.");
                else{sender.sendMessage("§cThe Pit arena setup has "+result.errors().size()+" error(s):");for(String error:result.errors())sender.sendMessage("§c- "+error);}
                return true;
            }
            if(action.equals("export")){
                if(!has(sender,"thepit.admin.setup.export"))return true;
                try{PitSetupService.ExportResult result=plugin.getSetupService().exportSnapshot();sender.sendMessage("§aExported The Pit setup to §f"+result.path());sender.sendMessage("§7Arenas: §f"+result.arenas()+" §7Leaderboards: §f"+result.leaderboards()+" §7NPCs: §f"+result.npcs());}
                catch(Exception exception){sender.sendMessage(storageUnavailable());}
                return true;
            }
            if(action.equals("import")){
                if(!has(sender,"thepit.admin.setup.import"))return true;
                boolean replace=args.length>=3&&args[2].equalsIgnoreCase("confirm-replace");
                if(replace){
                    if(!has(sender,"thepit.admin.setup.replace"))return true;
                    String key=senderKey(sender);long now=System.currentTimeMillis();
                    if(replaceConfirmations.getOrDefault(key,0L)<now){replaceConfirmations.put(key,now+30000L);sender.sendMessage("§cWarning: this will replace all existing The Pit arena setup. Run the same command again within 30 seconds to confirm.");return true;}
                    replaceConfirmations.remove(key);
                }
                PitSetupService.ImportResult result=plugin.getSetupService().importFile(replace);
                if(!result.success()){sender.sendMessage("§cImport failed. Nothing was changed: "+result.error());return true;}
                reloadRuntimeSetup();sender.sendMessage("§aImported The Pit setup from §f"+result.path());return true;
            }
            sender.sendMessage("§c/pit setup <status|validate|export|import [confirm-replace]>");return true;
        }

        if (args[0].equalsIgnoreCase("rankdebug")) {
            if (!has(sender, "thepit.admin.rankdebug")) return true;
            Player target;
            if (args.length >= 2) target = Bukkit.getPlayer(args[1]);
            else if (sender instanceof Player) target = (Player) sender;
            else {
                sender.sendMessage("§c/pit rankdebug <player>");
                return true;
            }
            plugin.getAbelixRankPermissionManager().sendDebug(sender, target);
            return true;
        }

        if (args[0].equalsIgnoreCase("resetstats") || args[0].equalsIgnoreCase("wipestats") || args[0].equalsIgnoreCase("resetplayer") || args[0].equalsIgnoreCase("wipeplayer")) {
            if (!has(sender, "thepit.admin.reset")) return true;
            if (args.length < 2) { sender.sendMessage("§c/" + label + " resetstats <player>"); return true; }
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            boolean ok = plugin.getStatsManager().resetPlayer(target);
            sender.sendMessage(ok ? "§aWiped The Pit statistics for §f" + args[1] : "§cCould not wipe that player.");
            return true;
        }
        if (args[0].equalsIgnoreCase("setprestige")) {
            if (!has(sender, "thepit.admin.prestige")) return true;
            if (args.length >= 2 && !validPlayerName(args[1])) return true;
            if (args.length < 3) { sender.sendMessage("§c/" + label + " setprestige <player> <amount>"); return true; }
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount = parseInt(args[2], -1);
            if (amount < 0 || amount > plugin.getStatsManager().getMaxPrestige()) { sender.sendMessage("§cPrestige must be between 0 and 50."); return true; }
            boolean ok = plugin.getStatsManager().setPrestige(target, amount);
            sender.sendMessage(ok ? "§aSet §f" + args[1] + "§a's prestige to §e" + amount + "§a." : "§cCould not set prestige.");
            return true;
        }

        if ((args[0].equalsIgnoreCase("perks") || args[0].equalsIgnoreCase("perk") || args[0].equalsIgnoreCase("buffs")) && args.length >= 2
                && Arrays.asList("give", "remove", "equip", "unlockslot", "reset").contains(args[1].toLowerCase(Locale.ENGLISH))) {
            if (!has(sender, "thepit.admin.perks")) return true;
            if (args.length < 3) {
                sender.sendMessage("Â§c/" + label + " perks <give|remove|equip|unlockslot|reset> <player> [slot] [perk]");
                return true;
            }
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            me.outerspace.thepit.perk.PerkManager.Result result;
            String action = args[1].toLowerCase(Locale.ENGLISH);
            if (action.equals("give") || action.equals("remove")) {
                if (args.length < 4) { sender.sendMessage("Â§cSpecify a perk ID."); return true; }
                result = action.equals("give") ? plugin.getPerkManager().grant(target.getUniqueId(), args[3]) : plugin.getPerkManager().remove(target.getUniqueId(), args[3]);
            } else if (action.equals("equip")) {
                if (args.length < 5) { sender.sendMessage("Â§c/" + label + " perks equip <player> <slot> <perk>"); return true; }
                result = plugin.getPerkManager().adminEquip(target.getUniqueId(), parseInt(args[3], -1), args[4]);
            } else if (action.equals("unlockslot")) {
                if (args.length < 4) { sender.sendMessage("Â§c/" + label + " perks unlockslot <player> <slot>"); return true; }
                result = plugin.getPerkManager().adminUnlockSlot(target.getUniqueId(), parseInt(args[3], -1));
            } else result = plugin.getPerkManager().reset(target.getUniqueId());
            sender.sendMessage(result == me.outerspace.thepit.perk.PerkManager.Result.SUCCESS ? "Â§aPerk profile updated for Â§f" + target.getName() + "Â§a." : "Â§cPerk action failed: " + result.name().toLowerCase(Locale.ENGLISH).replace('_', ' '));
            return true;
        }

        if (args[0].equalsIgnoreCase("contractadmin")) {
            if (!has(sender, "thepit.admin.contracts")) return true;
            if (args.length < 2) {
                sender.sendMessage(CC.color("&c/contractadmin <reload|resetdaily|cancel|complete|setprogress|debug>"));
                return true;
            }
            String action = args[1].toLowerCase(Locale.ENGLISH);
            if (action.equals("reload")) {
                sender.sendMessage(plugin.getContractManager().reload() ? CC.color("&aContracts reloaded.") : CC.color("&ccontracts.yml contains invalid definitions."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(CC.color("&c/contractadmin " + action + " <player> [amount]"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null || !plugin.getContractManager().isLoaded(target.getUniqueId())) {
                sender.sendMessage(CC.color("&cThat player's contract profile is not loaded."));
                return true;
            }
            if (action.equals("debug")) {
                plugin.getContractManager().debug(sender, target);
                return true;
            }
            boolean success;
            if (action.equals("resetdaily")) success = plugin.getContractManager().resetDailyAdmin(target);
            else if (action.equals("cancel")) success = plugin.getContractManager().cancelAdmin(target);
            else if (action.equals("complete")) success = plugin.getContractManager().complete(target, true);
            else if (action.equals("setprogress") && args.length >= 4) {
                success = plugin.getContractManager().setProgressAdmin(target, parseLong(args[3], -1L));
            } else {
                sender.sendMessage(CC.color("&cUnknown contract administration action."));
                return true;
            }
            sender.sendMessage(success ? CC.color("&aContract profile updated.") : CC.color("&cThe contract profile could not be updated."));
            return true;
        }

        if (args[0].equalsIgnoreCase("pitpurchasedebug")) {
            if (!has(sender, "thepit.admin.purchasedebug")) return true;
            if (args.length < 2) {
                sender.sendMessage(CC.color("&c/pitpurchasedebug <player>"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(CC.color("&cThat player is not online."));
                return true;
            }
            UUID uuid = target.getUniqueId();
            int cached = plugin.getPitGoldService().balance(uuid);
            boolean statsLoaded = plugin.getStatsManager().isProfileLoaded(uuid);
            boolean contractLoaded = plugin.getContractManager().isLoaded(uuid);
            boolean killstreakLoaded = plugin.getKillstreakManager().isLoaded(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                int database = plugin.getPurchaseRepository().databaseCoins(uuid);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(CC.color("&6Pit purchase diagnostics for &f" + target.getName()));
                    sender.sendMessage(CC.color("&7Profile loaded: &f" + (statsLoaded && contractLoaded && killstreakLoaded)));
                    sender.sendMessage(CC.color("&7Cached Pit Gold: &f" + cached));
                    sender.sendMessage(CC.color("&7Database Pit Gold: &f" + (database < 0 ? "missing/unavailable" : database)));
                    sender.sendMessage(CC.color("&7Contract repository available: &f" + plugin.getPurchaseRepository().contractAvailable()));
                    sender.sendMessage(CC.color("&7Killstreak repository available: &f" + plugin.getPurchaseRepository().killstreakAvailable()));
                    sender.sendMessage(CC.color("&7Contract definitions loaded: &f" + plugin.getContractManager().definitionsLoaded()));
                    sender.sendMessage(CC.color("&7Valid Novice definitions: &f"
                            + plugin.getContractManager().validDefinitionCount(me.outerspace.thepit.managers.ContractManager.Difficulty.NOVICE)));
                    sender.sendMessage(CC.color("&7Valid Big Time definitions: &f"
                            + plugin.getContractManager().validDefinitionCount(me.outerspace.thepit.managers.ContractManager.Difficulty.BIG_TIME)));
                    sender.sendMessage(CC.color("&7Killstreak definitions: &f" + plugin.getKillstreakManager().definitionCount()));
                    sender.sendMessage(CC.color("&7Required database tables present: &f" + plugin.getPurchaseRepository().requiredTablesPresent()));
                    sender.sendMessage(CC.color("&7Pending transaction: &f" + plugin.getPurchaseRepository().pending(uuid)
                            + " &7contractLock=&f" + plugin.getContractManager().purchasePending(uuid)
                            + " &7killstreakLock=&f" + plugin.getKillstreakManager().purchasePending(uuid)));
                    sender.sendMessage(CC.color("&7Last purchase failure code: &f" + plugin.getPurchaseRepository().lastFailure(uuid)));
                });
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("achievementadmin")) {
            if (!has(sender, "thepit.admin.achievements")) return true;
            if (args.length < 2) {
                sender.sendMessage(CC.color("&c/achievementadmin <give|revoke|progress|reset|resetall|reload|debug>"));
                return true;
            }
            String action = args[1].toLowerCase(Locale.ENGLISH);
            if (action.equals("reload")) {
                sender.sendMessage(plugin.getAchievementManager().reload() ? CC.color("&aAchievements reloaded.") : CC.color("&cAchievements contain invalid definitions. Previous definitions remain active."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(CC.color("&c/achievementadmin " + action + " <player> [achievementId] [amount|tier]"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null || !plugin.getAchievementManager().isLoaded(target.getUniqueId())) {
                sender.sendMessage(CC.color("&cThat player's achievement profile is not loaded."));
                return true;
            }
            boolean success;
            if (action.equals("debug")) {
                plugin.getAchievementManager().debug(sender, target);
                return true;
            } else if (action.equals("resetall")) {
                success = plugin.getAchievementManager().resetAll(target);
            } else if (args.length < 4) {
                sender.sendMessage(CC.color("&cAn achievement ID is required."));
                return true;
            } else if (action.equals("give")) {
                success = plugin.getAchievementManager().grant(target, args[3], args.length >= 5 ? parseInt(args[4], 1) : 1);
            } else if (action.equals("revoke")) {
                success = plugin.getAchievementManager().revoke(target, args[3], args.length >= 5 ? parseInt(args[4], 1) : 1);
            } else if (action.equals("progress")) {
                if (args.length < 5) {
                    sender.sendMessage(CC.color("&c/achievementadmin progress <player> <achievementId> <amount>"));
                    return true;
                }
                success = plugin.getAchievementManager().adminProgress(target, args[3], parseLong(args[4], -1L));
            } else if (action.equals("reset")) {
                success = plugin.getAchievementManager().reset(target, args[3]);
            } else {
                sender.sendMessage(CC.color("&cUnknown achievement administration action."));
                return true;
            }
            sender.sendMessage(success ? CC.color("&aAchievement profile updated.") : CC.color("&cThe achievement profile could not be updated."));
            return true;
        }

        if (args[0].equalsIgnoreCase("prestige") && args.length >= 2
                && args[1].equalsIgnoreCase("resetcooldown")) {
            if (!has(sender, "thepit.admin.prestige")) return true;
            if (args.length < 3 || !validPlayerName(args[2])) {
                sender.sendMessage("§c/pit prestige resetcooldown <player>");
                return true;
            }
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            boolean success = plugin.getStatsManager().resetPrestigeCooldown(target);
            sender.sendMessage(success ? "§aPrestige data updated for §f" + args[2] + "§a." : "§cInvalid player or amount.");
            return true;
        }

        if (args[0].equalsIgnoreCase("renown")) {
            if (args.length == 1) {
                if (!(sender instanceof Player player)) { sender.sendMessage("§c/pit renown <give|take|set> <player> <amount>"); return true; }
                plugin.getRenownShopManager().open(player);
                return true;
            }
            if (!has(sender, "thepit.admin.renown")) return true;
            if (args.length < 4 || !validPlayerName(args[2])) {
                sender.sendMessage("§c/pit renown <give|take|set> <player> <amount>");
                return true;
            }
            StatsManager.RenownMode mode;
            try { mode = StatsManager.RenownMode.valueOf(args[1].toUpperCase(Locale.ENGLISH)); }
            catch (IllegalArgumentException exception) { sender.sendMessage("§cUse give, take, or set."); return true; }
            long amount = parseLong(args[3], -1L);
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            boolean success = amount >= 0L && plugin.getStatsManager().changeRenown(target, amount, mode);
            sender.sendMessage(success ? "§aRenown updated for §f" + args[2] + "§a." : "§cRenown update failed.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setlevel") || args[0].equalsIgnoreCase("setxp") || args[0].equalsIgnoreCase("addxp")) {
            if (!has(sender, "thepit.admin.level")) return true;
            if (args.length < 3 || !validPlayerName(args[1])) {
                sender.sendMessage("§c/" + label + " " + args[0].toLowerCase(Locale.ENGLISH) + " <player> <amount>");
                return true;
            }
            org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            long longAmount = parseLong(args[2], -1L);
            int amount = longAmount > Integer.MAX_VALUE ? -1 : (int) longAmount;
            boolean success;
            if (args[0].equalsIgnoreCase("setlevel")) success = plugin.getStatsManager().setLevel(target, amount);
            else if (args[0].equalsIgnoreCase("setxp")) success = plugin.getStatsManager().setXp(target, amount);
            else success = plugin.getStatsManager().addOfflineXp(target, longAmount);
            sender.sendMessage(success ? "§aUpdated progression for §f" + args[1] + "§a." : "§cInvalid player or amount.");
            return true;
        }

        if (args[0].equalsIgnoreCase("maprotation")) {
            if (args.length == 1 || args[1].equalsIgnoreCase("status")) {
                sender.sendMessage(plugin.getMapRotationManager().status());
                return true;
            }
            if (!has(sender, "thepit.admin.maprotation")) return true;
            if (args[1].equalsIgnoreCase("resettime")) {
                sender.sendMessage(plugin.getMapRotationManager().resetTime() ? "§aMap rotation time reset." : "§cCould not persist the rotation time.");
                return true;
            }
            if (args[1].equalsIgnoreCase("next")) {
                plugin.getMapRotationManager().next(success -> sender.sendMessage(success ? "§aAdvanced to the next map." : "§cThe next arena could not be loaded."));
                return true;
            }
            if (args[1].equalsIgnoreCase("set") && args.length >= 3) {
                String mapName = args[2];
                plugin.getMapRotationManager().set(mapName, success -> sender.sendMessage(success ? "§aMap changed to §f" + mapName + "§a." : "§cUnknown map or arena load failed."));
                return true;
            }
            sender.sendMessage("§c/" + label + " maprotation <status|next|set <map>|resettime>");
            return true;
        }

        if (args[0].equalsIgnoreCase("fishingclub")) {
            if (!(sender instanceof Player)) { sender.sendMessage("§cPlayers only."); return true; }
            if (!has(sender, "thepit.fishing")) return true;
            plugin.getFishingManager().openClub((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("fishing")) {
            return plugin.getFishingManager().handleCommand(sender, Arrays.copyOfRange(args, 1, args.length));
        }

        if (args[0].equalsIgnoreCase("shopadmin")) {
            if (!has(sender, "thepit.admin.shop")) return true;
            if (args.length < 2) {
                sender.sendMessage(CC.color("&c/shopadmin <reload|give|unlock|lock|autobuy|debug>"));
                return true;
            }
            if (args[1].equalsIgnoreCase("reload")) {
                if (!plugin.registry().reload()) sender.sendMessage(CC.color("&cshop.yml could not be reloaded."));
                else {
                    plugin.getShopManager().reload();
                    sender.sendMessage(CC.color("&aItem Shop configuration reloaded."));
                }
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cSpecify an online player."));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null || !plugin.getStatsManager().isProfileLoaded(target.getUniqueId())) {
                sender.sendMessage(CC.color("&cThat player's profile is not loaded."));
                return true;
            }
            if (args[1].equalsIgnoreCase("debug")) {
                sender.sendMessage(CC.color("&6Item Shop debug: &f" + plugin.getShopManager().debug(target)));
                return true;
            }
            if (args[1].equalsIgnoreCase("autobuy")) {
                if (args.length < 4 || !args[3].equalsIgnoreCase("on") && !args[3].equalsIgnoreCase("off")) {
                    sender.sendMessage(CC.color("&c/shopadmin autobuy <player> <on|off>"));
                    return true;
                }
                plugin.getShopManager().setAutobuyEnabled(target, args[3].equalsIgnoreCase("on"));
                sender.sendMessage(CC.color("&aUpdated Autobuy for &f" + target.getName() + "&a."));
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(CC.color("&cSpecify an Item Shop item ID."));
                return true;
            }
            String id = args[3];
            boolean success;
            if (args[1].equalsIgnoreCase("give")) success = plugin.getShopManager().adminGive(target, id, args.length >= 5 ? Math.max(1, parseInt(args[4], 1)) : 1);
            else if (args[1].equalsIgnoreCase("unlock")) success = plugin.getShopManager().adminUnlock(target, id);
            else if (args[1].equalsIgnoreCase("lock")) success = plugin.getShopManager().adminLock(target, id);
            else {
                sender.sendMessage(CC.color("&c/shopadmin <reload|give|unlock|lock|autobuy|debug>"));
                return true;
            }
            sender.sendMessage(CC.color(success ? "&aItem Shop action completed." : "&cItem Shop action failed."));
            return true;
        }

        if (args[0].equalsIgnoreCase("gold") || args[0].equalsIgnoreCase("coins")) {
            if (args.length == 1) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§c/gold <give|take|set> <player> <amount>");
                    return true;
                }
                if (!has(sender, "thepit.command.gold")) return true;
                sender.sendMessage(prefix() + "Gold: §6" + formatNumber(plugin.getPitGoldService().balance(player.getUniqueId())));
                return true;
            }
            if (!has(sender, "thepit.admin.gold") && !has(sender, "thepit.admin.coins")) return true;
            if (args.length < 4 || !args[1].equalsIgnoreCase("give") && !args[1].equalsIgnoreCase("take") && !args[1].equalsIgnoreCase("set")) {
                sender.sendMessage("§c/gold <give|take|set> <player> <amount>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            int amount = parseInt(args[3], -1);
            if (target == null || !plugin.getStatsManager().isProfileLoaded(target.getUniqueId())) {
                sender.sendMessage("§cThat player's The Pit profile is not loaded.");
                return true;
            }
            if (amount < 0) {
                sender.sendMessage("§cAmount must be zero or greater.");
                return true;
            }
            boolean changed = true;
            if (args[1].equalsIgnoreCase("give")) plugin.getPitGoldService().add(target, amount);
            else if (args[1].equalsIgnoreCase("take")) changed = plugin.getPitGoldService().take(target, amount);
            else plugin.getPitGoldService().set(target, amount);
            if (!changed) sender.sendMessage("§cThat player does not have enough Gold.");
            else {
                sender.sendMessage("§aUpdated §f" + target.getName() + "§a's Pit Gold to §6" + formatNumber(plugin.getPitGoldService().balance(target.getUniqueId())) + "§a.");
                target.sendMessage(prefix() + "Your Gold balance is now §6" + formatNumber(plugin.getPitGoldService().balance(target.getUniqueId())) + "§7.");
                plugin.getBuildScoreboardManager().update(target);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("leaderboard")) {
            if (!has(sender, "thepit.admin.leaderboard")) return true;
            if (args.length < 2) {
                sender.sendMessage(CC.color("&c/leaderboard <refresh|list|teleport <id>|debug [player]>"));
                return true;
            }
            if (args[1].equalsIgnoreCase("refresh")) {
                plugin.getLeaderboardManager().refreshActivePlayers(true);
                sender.sendMessage(CC.color("&aActive Players leaderboard refresh started."));
                return true;
            }
            if (args[1].equalsIgnoreCase("list")) {
                plugin.getLeaderboardManager().list(sender);
                return true;
            }
            if (args[1].equalsIgnoreCase("teleport")) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(CC.color("&cPlayers only."));
                    return true;
                }
                if (args.length < 3 || !plugin.getLeaderboardManager().teleport(player, args[2])) sender.sendMessage(CC.color("&cUnknown leaderboard ID or its world is not loaded."));
                else sender.sendMessage(CC.color("&aTeleported to &f" + args[2] + "&a."));
                return true;
            }
            if (args[1].equalsIgnoreCase("debug")) {
                Player target = args.length >= 3 ? Bukkit.getPlayer(args[2]) : sender instanceof Player ? (Player) sender : null;
                if (args.length >= 3 && target == null) sender.sendMessage(CC.color("&cThat player is not online."));
                else plugin.getLeaderboardManager().debug(sender, target);
                return true;
            }
            sender.sendMessage(CC.color("&c/leaderboard <refresh|list|teleport <id>|debug [player]>"));
            return true;
        }


        if (args[0].equalsIgnoreCase("event")) {
            if (args.length < 2) sender.sendMessage(CC.color("&c/event <list|status|start <event-id>|stop [event-id]>"));
            else if (args[1].equalsIgnoreCase("list")) for (String line : plugin.getMinorEventManager().eventList()) sender.sendMessage(line);
            else if (args[1].equalsIgnoreCase("status")) sender.sendMessage(CC.color(plugin.getMinorEventManager().activeEventId() == null ? "&7No event is active." : plugin.getMinorEventManager().statusLine()));
            else if (args[1].equalsIgnoreCase("debug") && args.length >= 3) for (String line : plugin.getMinorEventManager().debugEvent(args[2])) sender.sendMessage(line);
            else if (args[1].equalsIgnoreCase("start") && args.length >= 3) sender.sendMessage(plugin.getPitCommandManager().eventCommand(sender, args[1], args[2]));
            else if (args[1].equalsIgnoreCase("stop")) sender.sendMessage(plugin.getPitCommandManager().eventCommand(sender, args[1], args.length >= 3 ? args[2] : ""));
            else sender.sendMessage(CC.color("&c/event <list|status|start <event-id>|stop [event-id]>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("eventsetup")) {
            if (!(sender instanceof Player player)) sender.sendMessage(CC.color("&cPlayers only."));
            else if (args.length < 4) player.sendMessage(CC.color("&c/eventsetup <arena> <event-id> <action> [location-id]"));
            else for (String line : plugin.getMinorEventManager().setupCommand(player, args[1], args[2], args[3], args.length >= 5 ? args[4] : "")) player.sendMessage(line);
            return true;
        }

        if (plugin.getPitCommandManager().handles(args[0])) {
            return plugin.getPitCommandManager().handle(sender, args[0], Arrays.copyOfRange(args, 1, args.length));
        }

        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }

        if (args[0].equalsIgnoreCase("carepackageset")) {
            if (args.length < 3) p.sendMessage(CC.color("&c/carepackageset <arena> <location-id>"));
            else p.sendMessage(plugin.getPitCommandManager().carePackageSet(p, args[1], args[2]));
            return true;
        }
        if (args[0].equalsIgnoreCase("carepackageremove")) {
            if (args.length < 3) p.sendMessage(CC.color("&c/carepackageremove <arena> <location-id>"));
            else p.sendMessage(plugin.getPitCommandManager().carePackageRemove(p, args[1], args[2]));
            return true;
        }
        if (args[0].equalsIgnoreCase("carepackagelist")) {
            if (args.length < 2) p.sendMessage(CC.color("&c/carepackagelist <arena>"));
            else for (String line : plugin.getPitCommandManager().carePackageList(p, args[1])) p.sendMessage(line);
            return true;
        }
        if (args[0].equalsIgnoreCase("carepackagetest")) {
            if (args.length < 2) p.sendMessage(CC.color("&c/carepackagetest <arena> [location-id]"));
            else p.sendMessage(plugin.getPitCommandManager().carePackageTest(p, args[1], args.length >= 3 ? args[2] : null));
            return true;
        }

        if (args[0].equalsIgnoreCase("launchpad")) {
            if (!has(p, "thepit.admin.launchpad")) return true;
            if (args.length < 2) { p.sendMessage("§c/pit launchpad <add [name]|remove <name>|list>"); return true; }
            if (args[1].equalsIgnoreCase("add")) p.sendMessage(plugin.getSlimeLaunchPadManager().addPad(p, args.length >= 3 ? args[2] : null));
            else if (args[1].equalsIgnoreCase("remove")) {
                if (args.length < 3) p.sendMessage("§c/pit launchpad remove <name>");
                else p.sendMessage(plugin.getSlimeLaunchPadManager().removePad(p, args[2]));
            }
            else if (args[1].equalsIgnoreCase("list")) for (String line : plugin.getSlimeLaunchPadManager().listPads()) p.sendMessage(line);
            else p.sendMessage("§c/pit launchpad <add [name]|remove <name>|list>");
            return true;
        }

        if (args[0].equalsIgnoreCase("coinspawn")) {
            if (!has(p, "thepit.admin.coinspawn")) return true;
            if (args.length < 2) { p.sendMessage("§c/pit coinspawn <add <name> [reward] [respawnSeconds]|remove <name>|list [page]|reload|respawn|diagnose>"); return true; }
            if (args[1].equalsIgnoreCase("add")) {
                if (args.length < 3) { p.sendMessage("§c/pit coinspawn add <name> [reward] [respawnSeconds]"); return true; }
                int reward = args.length >= 4 ? parseInt(args[3], -1) : plugin.getCollectibleCoinManager().getDefaultReward();
                int respawn = args.length >= 5 ? parseInt(args[4], -1) : plugin.getCollectibleCoinManager().getDefaultRespawnSeconds();
                if (reward < 0 || respawn < 0) p.sendMessage("§cReward and respawn seconds must be non-negative numbers.");
                else p.sendMessage(plugin.getCollectibleCoinManager().addSpawn(p, args[2], reward, respawn));
            } else if (args[1].equalsIgnoreCase("remove")) {
                if (args.length < 3) p.sendMessage("§c/pit coinspawn remove <name>");
                else p.sendMessage(plugin.getCollectibleCoinManager().removeSpawn(p, args[2]));
            }
            else if (args[1].equalsIgnoreCase("list")) {
                boolean pageOnly = args.length >= 3 && args[2].matches("[0-9]+");
                String arena = args.length >= 3 && !pageOnly ? args[2] : null;
                int page = pageOnly ? parseInt(args[2], 1) : args.length >= 4 ? parseInt(args[3], 1) : 1;
                for (String line : plugin.getCollectibleCoinManager().listSpawns(arena, page)) p.sendMessage(line);
            }
            else if (args[1].equalsIgnoreCase("reload")) p.sendMessage(plugin.getCollectibleCoinManager().reloadActive(args.length >= 3 ? args[2] : null));
            else if (args[1].equalsIgnoreCase("respawn")) {
                plugin.getCollectibleCoinManager().respawnAll();
                p.sendMessage("§aManaged coins safely respawned for the active arena.");
            } else if (args[1].equalsIgnoreCase("diagnose")) {
                for (String line : plugin.getCollectibleCoinManager().diagnose()) p.sendMessage(line);
            } else p.sendMessage("§c/pit coinspawn <add <name> [reward] [respawnSeconds]|remove <name>|list [page]|reload|respawn|diagnose>");
            return true;
        }

        if (args[0].equalsIgnoreCase("npc")) {
            if (!has(p, "thepit.admin.npc")) return true;
            if (args.length < 2) {
                sendNpcUsage(p, label);
                return true;
            }
            String action = args[1].toLowerCase(Locale.ENGLISH);
            if (action.equals("migratefromdatabase")) {
                if (!has(p, "thepit.admin.npcmigrate")) return true;
                if (args.length >= 3 && args[2].equalsIgnoreCase("map")) {
                    if (args.length < 5) p.sendMessage("§c/" + label + " npc migratefromdatabase map <database-world> <arena-id>");
                    else p.sendMessage(plugin.getNpcService().setMigrationWorldMapping(args[3], args[4]) ? "§aMigration mapping saved for this server session." : "§cUnknown arena ID or invalid mapping.");
                } else plugin.getNpcService().migrateFromDatabase(p, args.length >= 3 && args[2].equalsIgnoreCase("--overwrite"));
                return true;
            }
            if (action.equals("create") && args.length >= 5) {
                sendNpcMutation(p,args[3],plugin.getNpcService().create(p,args[2],args[3],args[4]),"created in arenas."+args[2]+".npcs."+args[3]);
                return true;
            }
            if ((action.equals("remove") || action.equals("delete")) && args.length >= 4) {
                sendNpcMutation(p,args[3],plugin.getNpcService().remove(args[2],args[3]),"removed from arenas."+args[2]+".npcs."+args[3]);
                return true;
            }
            if (action.equals("setlocation") && args.length >= 4) {
                sendNpcMutation(p,args[3],plugin.getNpcService().setLocation(p,args[2],args[3]),"saved at arenas."+args[2]+".npcs."+args[3]+".location");
                return true;
            }
            if (action.equals("setskin") && args.length >= 5) {
                sendNpcMutation(p,args[3],plugin.getNpcService().setSkinId(args[2],args[3],args[4]),"updated at arenas."+args[2]+".npcs."+args[3]+".skin-id");
                return true;
            }
            if (action.equals("setaction") && args.length >= 5) {
                sendNpcMutation(p,args[3],plugin.getNpcService().setActionId(args[2],args[3],args[4]),"updated at arenas."+args[2]+".npcs."+args[3]+".action-id");
                return true;
            }
            if (action.equals("sethologram") && args.length >= 5) {
                sendNpcMutation(p,args[3],plugin.getNpcService().setHologramId(args[2],args[3],args[4]),"updated at arenas."+args[2]+".npcs."+args[3]+".hologram-id");
                return true;
            }
            if ((action.equals("enable") || action.equals("disable")) && args.length >= 4) {
                sendNpcMutation(p,args[3],plugin.getNpcService().setEnabled(args[2],args[3],action.equals("enable")),action+"d at arenas."+args[2]+".npcs."+args[3]);
                return true;
            }
            if (action.equals("list") && args.length >= 3) {
                for(String line:plugin.getNpcService().list(args[2]))p.sendMessage(line);
                return true;
            }
            if (action.equals("validate") && args.length >= 3) {
                for(String line:plugin.getNpcService().validate(args[2]))p.sendMessage(line);
                return true;
            }
            if (action.equals("status")) {
                for (String line : plugin.getNpcService().status(args.length >= 3 ? args[2] : plugin.getArenaManager().getCurrentArena())) p.sendMessage(line);
                return true;
            }
            if (action.equals("reload") && args.length >= 3) {
                p.sendMessage(plugin.getNpcService().reload(args[2])?"§aReloaded arena NPC definitions from arenas.yml.":"§cCould not reload that arena's NPC definitions.");
                return true;
            }
            if (!Arrays.asList("select", "list", "debug", "repairholograms", "repair", "reload", "movehere").contains(action) && !plugin.getSetupService().repository().available()) {
                p.sendMessage(storageUnavailable());
                return true;
            }
            if (action.equals("create")) {
                if (args.length < 4) p.sendMessage("§c/" + label + " npc create <name> <displayName>");
                else sendNpcMutation(p, args[2], plugin.getNpcService().create(p, args[2], joinFrom(args, 3)), "created");
            } else if (action.equals("select")) {
                if (args.length < 4 || !plugin.getNpcService().select(p, args[2], args[3])) p.sendMessage(CC.color("&c/npc select <arena-id> <npc-id>"));
            } else if (action.equals("remove") || action.equals("delete")) {
                p.sendMessage(plugin.getNpcService().removeSelected(p) ? "§aNPC removed." : npcFailure("§cNo NPC selected, nearby, or in sight."));
            } else if (action.equals("rename")) {
                if (args.length < 4) p.sendMessage("§c/" + label + " npc rename <name> <displayName>");
                else sendNpcMutation(p, args[2], plugin.getNpcService().renameById(args[2], joinFrom(args, 3)), "renamed");
            } else if ((action.equals("command") || action.equals("setcommand")) && args.length >= 4 && (args[2].equalsIgnoreCase("player") || args[2].equalsIgnoreCase("console"))) {
                sendNpcMutation(p, "selected", plugin.getNpcService().setCommandSelected(p, args[2], joinFrom(args, 3)), "command persisted and activated");
            } else if (action.equals("command") || action.equals("setcommand")) {
                p.sendMessage(args.length >= 3 && plugin.getNpcService().setCommandSelected(p, joinFrom(args, 2)) ? "§aNPC right-click command updated." : npcFailure("§cSelect an NPC and provide a command."));
            } else if (action.equals("skin")) {
                if (args.length < 3) p.sendMessage("§c/pit npc skin <MinecraftName>");
                else plugin.getNpcService().setSkinSelected(p, args[2]);
            } else if (action.equals("movehere")) {
                if (args.length < 3) p.sendMessage("§c/" + label + " npc movehere <npcName>");
                else sendNpcMutation(p, args[2], plugin.getNpcService().moveHere(args[2], p.getLocation()), "moved to your location");
            } else if (action.equals("look")) {
                plugin.getNpcService().toggleLook(p);
            } else if (action.equals("list")) {
                plugin.getNpcService().list(p);
            } else if (action.equals("debug")) {
                if (args.length < 3) p.sendMessage("§c/" + label + " npc debug <npcName>");
                else plugin.getNpcService().debug(p, args[2]);
            } else if (action.equals("repairholograms") || action.equals("repair")) {
                p.sendMessage(plugin.getNpcService().repairHolograms(args.length >= 3 ? args[2] : null));
            } else if (action.equals("reload")) {
                plugin.getNpcService().reload();
                p.sendMessage("§aThe Pit NPCs reloaded.");
            } else {
                sendNpcUsage(p, label);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            plugin.getArenaManager().teleportToCurrentSpawn(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("respawn")) {
            if (!requireFeature(p, "respawn")) return true;
            if (plugin.getBuildScoreboardManager().isCombatTagged(p)) {
                p.sendMessage(CC.color("&cYou cannot use /respawn while combat-tagged."));
                return true;
            }
            plugin.getArenaManager().teleportToCurrentSpawn(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("arenadebug")) {
            if (!has(p, "thepit.admin.arenadebug")) return true;
            if (args.length < 2) { p.sendMessage("§c/pit arenadebug <arena>"); return true; }
            sendArenaDebug(p, joinFrom(args, 1));
            return true;
        }

        if (args[0].equalsIgnoreCase("enderchest") || args[0].equalsIgnoreCase("ec")) {
            if (!requireFeature(p, "enderchest")) return true;
            plugin.getExpandedEnderChestManager().open(p);
            return true;
        }

        if (args[0].equalsIgnoreCase("shop")) {
            if (has(p, "thepit.command.shop")) plugin.getShopManager().openShop(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("pitchat")) {
            if (requireFeature(p, "pitchat")) plugin.getFeatureMenuManager().openChatSettings(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("quests") || args[0].equalsIgnoreCase("quest")) {
            plugin.getQuestManager().open(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("contracts")) {
            if (has(p, "thepit.command.contracts")) plugin.getQuestManager().open(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("contract")) {
            if (!has(p, "thepit.command.contracts")) return true;
            if (args.length == 1) plugin.getQuestManager().open(p);
            else if (args[1].equalsIgnoreCase("status")) plugin.getContractManager().status(p);
            else if (args[1].equalsIgnoreCase("abandon")) plugin.getContractManager().openAbandon(p);
            else p.sendMessage(CC.color("&c/contract <status|abandon>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("perks") || args[0].equalsIgnoreCase("perk") || args[0].equalsIgnoreCase("buffs") || args[0].equalsIgnoreCase("upgrades")) {
            if (!has(p, "thepit.perks")) return true;
            if (!requireFeature(p, "upgrades")) return true;
            plugin.getPerkMenu().open(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("killstreaks")) {
            if (has(p, "thepit.command.killstreaks")) plugin.getKillstreakManager().openKillstreaks(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("prestige")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) plugin.getPrestigeManager().confirm(p);
            else plugin.getPrestigeManager().open(p);
            return true;
        }
        if (args[0].equalsIgnoreCase("level")) {
            org.bukkit.OfflinePlayer target = p;
            if (args.length >= 2) {
                if (!validPlayerName(args[1])) { p.sendMessage("§cInvalid player name."); return true; }
                target = Bukkit.getOfflinePlayer(args[1]);
            }
            plugin.getStatsManager().loadOffline(target);
            UUID uuid = target.getUniqueId();
            p.sendMessage(prefix() + plugin.getStatsManager().getFormattedLevel(uuid) + " §f" + (target.getName() == null ? args[1] : target.getName())
                    + " §7XP: §b" + plugin.getStatsManager().getLevelProgress(uuid) + " §7Prestige: §d" + plugin.getStatsManager().getPrestige(uuid));
            return true;
        }
        if (args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("playerstats")) {
            if (!requireFeature(p, "stats")) return true;
            if (args.length == 1) {
                plugin.getStatsMenuManager().open(p);
                return true;
            }
            if (!has(p, "thepit.stats.others")) return true;
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !plugin.getStatsManager().isProfileLoaded(target.getUniqueId())) {
                p.sendMessage("§cThat player's The Pit profile is not currently loaded.");
                return true;
            }
            plugin.getStatsMenuManager().open(p, target);
            return true;
        }
        if (args[0].equalsIgnoreCase("achievements")) {
            if (!has(p, "thepit.command.achievements")) return true;
            if (args.length == 1) {
                plugin.getAchievementManager().open(p, p);
                return true;
            }
            if (!has(p, "thepit.achievements.others")) return true;
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !plugin.getAchievementManager().isLoaded(target.getUniqueId())) {
                p.sendMessage(CC.color("&cThat player's achievement profile is not currently loaded."));
                return true;
            }
            plugin.getAchievementManager().open(p, target);
            return true;
        }
        if (args[0].equalsIgnoreCase("view")) {
            if (!requireFeature(p, "view")) return true;
            if (args.length < 2) {
                p.sendMessage(CC.color("&c/view <player>"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !plugin.getStatsManager().isProfileLoaded(target.getUniqueId())) p.sendMessage(CC.color("&cThat player's The Pit profile is not loaded."));
            else plugin.getFeatureMenuManager().openView(p, target);
            return true;
        }
        if (args[0].equalsIgnoreCase("gmc") || args[0].equalsIgnoreCase("gms") || args[0].equalsIgnoreCase("gmsp") || args[0].equalsIgnoreCase("gma")) {
            if (!has(p, "thepit.admin.gamemode")) return true;
            Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : p;
            if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
            GameMode gm = args[0].equalsIgnoreCase("gmc") ? GameMode.CREATIVE : args[0].equalsIgnoreCase("gms") ? GameMode.SURVIVAL : args[0].equalsIgnoreCase("gmsp") ? GameMode.SPECTATOR : GameMode.ADVENTURE;
            target.setGameMode(gm);
            p.sendMessage("§aSet §f" + target.getName() + " §ato §f" + gm.name());
            return true;
        }

        if (args[0].equalsIgnoreCase("coinsgive")) {
            if (!has(p, "thepit.admin.coins")) return true;
            int off = 1;
            if (args.length < off + 2) { p.sendMessage("§c/pit coins give <player> <amount>"); return true; }
            Player target = Bukkit.getPlayer(args[off]);
            if (target == null) { p.sendMessage("§cPlayer not found."); return true; }
            int amount = parseInt(args[off + 1], 0);
            if (amount <= 0) { p.sendMessage("§cAmount must be greater than zero."); return true; }
            plugin.getPitGoldService().add(target, amount);
            p.sendMessage("§aGave §6" + amount + " Gold §ato §f" + target.getName());
            target.sendMessage(prefix() + "You received §6" + amount + " Gold§7.");
            return true;
        }

        if (args[0].equalsIgnoreCase("unsafeenchant") || args[0].equalsIgnoreCase("enchantunsafe")) {
            if (!has(p, "thepit.admin.enchantunsafe")) return true;
            if (args.length < 3) { p.sendMessage("§c/pit unsafeenchant <enchant> <level>"); return true; }
            ItemStack hand = p.getItemInHand();
            if (hand == null || hand.getType() == Material.AIR) { p.sendMessage("§cHold an item."); return true; }
            Enchantment ench = Enchantment.getByName(args[1].toUpperCase());
            if (ench == null) { p.sendMessage("§cUnknown enchantment."); return true; }
            hand.addUnsafeEnchantment(ench, parseInt(args[2], 1));
            p.sendMessage("§aUnsafe enchant added.");
            return true;
        }

        if (!has(p, "thepit.admin")) return true;

        if (args[0].equalsIgnoreCase("setspawn")) {
            if (args.length < 2) { p.sendMessage("§c/pit setspawn <arena>"); return true; }
            String arena = ArenaManager.normalize(joinFrom(args, 1));
            if (!plugin.getYamlManager().containsArena(arena)) { p.sendMessage("§cUnknown arena in arenas.yml."); return true; }
            if (!plugin.getArenaManager().setSpawn(arena, p.getLocation())) {
                p.sendMessage("§cCould not save arenas.yml. The previous cached spawn remains active.");
                return true;
            }
            StoredLocation saved = plugin.getArenaManager().getArena(arena).map(ArenaDefinition::spawn).orElse(null);
            if (saved == null) {
                p.sendMessage("§cThe spawn was saved but could not be read back from the runtime cache.");
                return true;
            }
            p.sendMessage(CC.color("&aSpawn for arena &f" + arena + " &awas set to &f" + saved.worldName() + " "
                    + saved.x() + ", " + saved.y() + ", " + saved.z() + " &7(" + saved.yaw() + ", " + saved.pitch() + ")&a."));
            return true;
        }
        if (args[0].equalsIgnoreCase("setdeath")) {
            if (args.length < 2) { p.sendMessage("§c/pit setdeath <arena>"); return true; }
            String arena = ArenaManager.normalize(args[1]);
            if (!plugin.getArenaManager().getArenaIds().contains(arena)) { p.sendMessage("§cUnknown arena."); return true; }
            p.sendMessage(plugin.getArenaManager().setDeath(arena,p.getLocation()) ? "§aDeath location set for arena §f"+arena : storageUnavailable());
            return true;
        }
        if (args[0].equalsIgnoreCase("setspawnprotection")) return setPoint(p, args, "spawn-protection", "§aSpawn protection");
        if (args[0].equalsIgnoreCase("setpvparena")) return setPoint(p, args, "pvp-arena", "§aPvP arena");
        if (args[0].equalsIgnoreCase("removeleaderboard") || args[0].equalsIgnoreCase("deleteleaderboard")) {
            if (!has(p, "thepit.admin.leaderboard")) return true;
            boolean removed = args.length >= 2
                    ? plugin.getLeaderboardManager().removeDisplay(p.getLocation(), args[1], 5.0)
                    : plugin.getLeaderboardManager().removeNearest(p.getLocation(), 5.0);
            p.sendMessage(removed ? "§aNearest leaderboard removed." : "§cNo leaderboard found within 5 blocks.");
            return true;
        }
        if (args[0].equalsIgnoreCase("setleaderboard")) {
            if (!has(p, "thepit.admin.leaderboard")) return true;
            if (args.length < 2) { p.sendMessage("§c/setleaderboard <activeplayers|kills|level|xp|prestige> [arena|period]"); return true; }
            if (args[1].equalsIgnoreCase("activeplayers")) {
                boolean created = plugin.getLeaderboardManager().createActivePlayers(p.getLocation(), args.length >= 3 ? args[2] : null);
                p.sendMessage(created ? CC.color("&aActive Players leaderboard saved at your exact location.")
                        : CC.color("&cCould not resolve that arena at this world. Use /setleaderboard activeplayers <arena>."));
                return true;
            }
            if (args[1].equalsIgnoreCase("unlockedfeatures")) {
                boolean created = plugin.getLeaderboardManager().createUnlockedFeatures(p.getLocation(), args.length >= 3 ? args[2] : null);
                p.sendMessage(created ? CC.color("&aUnlocked Features hologram saved at your exact location.")
                        : CC.color("&cCould not resolve that arena at this world. Use /setleaderboard unlockedfeatures <arena>."));
                return true;
            }
            if (!plugin.getSetupService().repository().available()) { p.sendMessage(storageUnavailable()); return true; }
            StatType type = StatType.from(args[1]);
            Period period = args.length >= 3 ? Period.from(args[2]) : Period.LIFETIME;
            if (type == null || !Arrays.asList(StatType.KILLS, StatType.LEVEL, StatType.XP, StatType.PRESTIGE).contains(type) || period == null) { p.sendMessage("§cInvalid type or period."); return true; }
            boolean created = plugin.getLeaderboardManager().create(p.getLocation(), type, period);
            p.sendMessage(created ? "§aLeaderboard placed. §7Saved boards: §f" + plugin.getLeaderboardManager().getSavedBoardCount() + " §7Spawned lines: §f" + plugin.getLeaderboardManager().getSpawnedLineCount()
                    : storageUnavailable());
            return true;
        }

        p.sendMessage("§cUnknown command. Use /" + label + " help");
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(CC.color("&bThe Pit player commands:"));
        sender.sendMessage(CC.color("&7/" + label + " shop &8- &7open the shop"));
        sender.sendMessage(CC.color("&7/" + label + " perks &8- &7open the perks menu"));
        sender.sendMessage(CC.color("&7/" + label + " level [player] &8- &7show Pit progression"));
        sender.sendMessage(CC.color("&7/" + label + " prestige &8- &7prestige at level 120"));
        sender.sendMessage(CC.color("&7/" + label + " renown &8- &7show permanent Renown"));
        sender.sendMessage(CC.color("&7/" + label + " stats &8- &7open your stats"));
        sender.sendMessage(CC.color("&7/respawn &8- &7return to spawn"));
        sender.sendMessage(CC.color("&7/pitchat &8- &7open chat settings"));
        sender.sendMessage(CC.color("&7/upgrades &8- &7open upgrades and perks"));
        sender.sendMessage(CC.color("&7/contracts &8- &7open contracts"));
        sender.sendMessage(CC.color("&7/view <player> &8- &7view player information"));
        sender.sendMessage(CC.color("&7/gold &8- &7show your Pit Gold"));
        sender.sendMessage(CC.color("&7/" + label + " maprotation status &8- &7show map rotation"));
        sender.sendMessage(CC.color("&7/" + label + " fishing stats &8- &7show fishing progress"));
        sender.sendMessage(CC.color("&7/" + label + " fishingclub &8- &7open Fishing Club upgrades"));
        if (me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin")) sender.sendMessage(CC.color("&c/" + label + " admin &8- &7admin commands"));
    }

    private void sendAdminHelp(CommandSender sender, String label) {
        sender.sendMessage(CC.color("&cThe Pit admin commands:"));
        String[] cmds = new String[]{"packetentities <status|count [arena]|nearby|respawn <player>>", "setspawn <arena>", "setspawnprotection <arena> <pos1|pos2>", "setpvparena <arena> <pos1|pos2>", "arenadebug <arena>", "launchpad <add [name]|remove <name>|list>", "coinspawn <add <name> [reward] [respawnSeconds]|remove <name>|list|respawn|diagnose>", "/carepackageset <arena> <location-id>", "/carepackageremove <arena> <location-id>", "/carepackagelist <arena>", "/carepackagetest <arena> [location-id]", "/event <start|stop> carepackage", "fishing <pos1|pos2|create|remove|list|enable|disable|reload|debug|resetdaily|setclub|givefish|giveaqua>", "/setleaderboard activeplayers [arena]", "/setleaderboard unlockedfeatures [arena]", "/setfeaturehologram [arena]", "/removeleaderboard [activeplayers|unlockedfeatures]", "/removefeaturehologram", "/leaderboard <refresh|list|teleport <id>|debug [player]>", "setleaderboard <kills|level|xp|prestige> [lifetime|monthly|weekly|daily]", "npc create <name> <displayName>", "npc rename <name> <displayName>", "npc movehere <npcName>", "npc debug <npcName>", "npc repairholograms [npcName]", "npc <select|remove|command|skin|look|list|reload>", "perks give <player> <perk>", "perks remove <player> <perk>", "perks equip <player> <slot> <perk>", "perks unlockslot <player> <slot>", "perks reset <player>", "gold <give|take|set <player> <amount>", "maprotation <status|next|set <map>|resettime>", "setlevel <player> <1-120>", "setxp <player> <amount>", "addxp <player> <amount>", "setprestige <player> <amount>", "resetstats <player>", "rankdebug [player]", "/gmc [player]", "/gms [player]", "/gmsp [player]", "/gma [player]", "/unsafeenchant <enchant> <level>", "/buildmode", "reload"};
        for (String sub : cmds) sendClickable(sender, sub.startsWith("/") ? sub : "/" + label + " " + sub);
        sendClickable(sender, "/" + label + " prestige resetcooldown <player>");
        sendClickable(sender, "/" + label + " renown <give|take|set> <player> <amount>");
    }

    private void sendClickable(CommandSender sender, String command) {
        if (sender instanceof Player) {
            TextComponent tc = new TextComponent(CC.color("&7" + command));
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
            ((Player) sender).spigot().sendMessage(tc);
        } else sender.sendMessage(command);
    }

    private boolean setPoint(Player p, String[] args, String region, String msg) {
        if (args.length < 3) { p.sendMessage("§c/pit " + args[0] + " <arena> <pos1|pos2>"); return true; }
        String arena = ArenaManager.normalize(args[1]);
        if (!plugin.getArenaManager().getArenaIds().contains(arena)) { p.sendMessage("§cUnknown arena."); return true; }
        String point = pointName(args[2]);
        if (point == null) { p.sendMessage("§cPoint must be pos1 or pos2."); return true; }
        boolean saved = plugin.getArenaManager().setArenaPoint(arena, region, point, p.getLocation());
        p.sendMessage(saved ? msg + " " + point + " set for arena §f" + arena
                : storageUnavailable());
        return true;
    }

    private boolean has(CommandSender sender, String perm) {
        if (me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, perm) || me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin")) return true;
        sender.sendMessage("§cNo permission.");
        return false;
    }

    private boolean requireFeature(Player player, String id) {
        me.outerspace.thepit.managers.LeaderboardManager.FeatureDefinition feature = plugin.getLeaderboardManager().feature(id);
        if (feature == null) {
            player.sendMessage(CC.color("&cThat The Pit feature is unavailable."));
            return false;
        }
        if (plugin.getLeaderboardManager().isFeatureUnlocked(player.getUniqueId(), id)) return true;
        player.sendMessage(CC.color("&cRequires Level " + feature.level() + "."));
        return false;
    }

    private String pointName(String input) {
        if (input.equalsIgnoreCase("pos1") || input.equalsIgnoreCase("1")) return "pos1";
        if (input.equalsIgnoreCase("pos2") || input.equalsIgnoreCase("2")) return "pos2";
        return null;
    }

    private String joinFrom(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) { if (i > start) sb.append(' '); sb.append(args[i]); }
        return sb.toString();
    }

    private int parseInt(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }
    private Integer integer(String value) { try { return Integer.valueOf(value); } catch (NumberFormatException exception) { return null; } }
    private long parseLong(String s, long def) { try { return Long.parseLong(s); } catch (Exception e) { return def; } }
    private boolean validPlayerName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    private String prefix() { return CC.color(plugin.registry().messages().getString("messages.prefix", "&bThe Pit &8» &7")); }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (alias.equalsIgnoreCase("npc")) {
            String[] routed = new String[args.length + 1];
            routed[0] = "npc";
            System.arraycopy(args, 0, routed, 1, args.length);
            args = routed;
        }
        if (args.length == 1) {
            List<String> out = new ArrayList<>(Arrays.asList("help", "shop", "quests", "perks", "perk", "buffs", "level", "prestige", "renown", "stats", "coins", "maprotation", "fishing", "fishingclub"));
            if (me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin")) out.addAll(Arrays.asList("admin", "setup", "setspawn", "setdeath", "setspawnprotection", "setpvparena", "arenadebug", "launchpad", "coinspawn", "setleaderboard", "removeleaderboard", "leaderboard", "npc", "recoverkit", "coinsgive", "resetstats", "wipestats", "setlevel", "setxp", "addxp", "setprestige", "rankdebug", "gmc", "gms", "gmsp", "gma", "unsafeenchant", "buildmode", "reload"));
            else {
                if (me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.launchpad")) out.add("launchpad");
                if (me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.coinspawn")) out.add("coinspawn");
            }
            return starts(out, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("maprotation")) return starts(me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.maprotation") ? Arrays.asList("status", "next", "set", "resettime") : Collections.singletonList("status"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("maprotation") && args[1].equalsIgnoreCase("set")) return starts(plugin.getMapRotationManager().maps(), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("prestige")) return starts(me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.prestige") ? Arrays.asList("info", "confirm", "resetcooldown") : Arrays.asList("info", "confirm"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("event")) return starts(Arrays.asList("start", "stop"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("ragepit")) return starts(Arrays.asList("setshape", "pos1", "pos2", "setcenter", "setradius", "setheight", "preview", "clearpreview", "validate"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("ragepit")) return starts(new ArrayList<>(plugin.getYamlManager().arenaIds()), args[2]);
        if (args.length == 4 && args[0].equalsIgnoreCase("ragepit") && args[1].equalsIgnoreCase("setshape")) return starts(Arrays.asList("circle", "ellipse", "square", "rectangle", "polygon"), args[3]);
        if (args.length == 3 && args[0].equalsIgnoreCase("event")) return starts(Collections.singletonList("carepackage"), args[2]);
        if (args.length == 2 && Arrays.asList("carepackageset", "carepackageremove", "carepackagelist", "carepackagetest").contains(args[0].toLowerCase(Locale.ENGLISH))) return starts(new ArrayList<>(plugin.getArenaManager().getArenaIds()), args[1]);
        if (args.length == 3 && (args[0].equalsIgnoreCase("carepackageremove") || args[0].equalsIgnoreCase("carepackagetest"))) return starts(new ArrayList<>(plugin.getPitCommandManager().carePackageLocationIds(args[1])), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("renown") && me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.renown")) return starts(Arrays.asList("give", "take", "set"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("prestige") && me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.prestige")
                && args[1].equalsIgnoreCase("resetcooldown")) {
            List<String> players = new ArrayList<>(); for (Player player : Bukkit.getOnlinePlayers()) players.add(player.getName()); return starts(players, args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("renown") && me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.renown")) {
            List<String> players = new ArrayList<>(); for (Player player : Bukkit.getOnlinePlayers()) players.add(player.getName()); return starts(players, args[2]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("fishing")) return starts(me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.fishing")
                ? Arrays.asList("stats", "club", "pos1", "pos2", "create", "remove", "list", "enable", "disable", "reload", "debug", "resetdaily", "setclub", "givefish", "giveaqua")
                : Arrays.asList("stats", "club"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("fishing") && Arrays.asList("pos1", "pos2", "create", "remove", "list", "enable", "disable").contains(args[1].toLowerCase(Locale.ENGLISH))) return starts(new ArrayList<>(plugin.getArenaManager().getArenaIds()), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("fishing") && Arrays.asList("stats", "club", "debug", "resetdaily", "setclub", "givefish", "giveaqua").contains(args[1].toLowerCase(Locale.ENGLISH))) {
            List<String> players = new ArrayList<>(); for (Player player : Bukkit.getOnlinePlayers()) players.add(player.getName()); return starts(players, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("fishing") && args[1].equalsIgnoreCase("setclub")) return starts(Arrays.asList("0", "1", "2", "3", "4", "5"), args[3]);
        if (args.length == 4 && args[0].equalsIgnoreCase("fishing") && args[1].equalsIgnoreCase("givefish")) return starts(Arrays.asList("kloonfish", "salmon"), args[3]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("perks") || args[0].equalsIgnoreCase("perk") || args[0].equalsIgnoreCase("buffs")) && me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.perks")) return starts(Arrays.asList("give", "remove", "equip", "unlockslot", "reset"), args[1]);
        if (args.length == 3 && (args[0].equalsIgnoreCase("perks") || args[0].equalsIgnoreCase("perk") || args[0].equalsIgnoreCase("buffs")) && me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.perks")) {
            List<String> players = new ArrayList<>();for(Player player:Bukkit.getOnlinePlayers())players.add(player.getName());return starts(players,args[2]);
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("perks") || args[0].equalsIgnoreCase("perk") || args[0].equalsIgnoreCase("buffs")) && Arrays.asList("give","remove").contains(args[1].toLowerCase())) return starts(new ArrayList<>(plugin.getPerkManager().perkIds()), args[3]);
        if (args.length == 4 && (args[0].equalsIgnoreCase("perks") || args[0].equalsIgnoreCase("perk") || args[0].equalsIgnoreCase("buffs")) && Arrays.asList("equip","unlockslot").contains(args[1].toLowerCase())) return starts(Arrays.asList("1","2","3","4"),args[3]);
        if (args.length == 5 && (args[0].equalsIgnoreCase("perks") || args[0].equalsIgnoreCase("perk") || args[0].equalsIgnoreCase("buffs")) && args[1].equalsIgnoreCase("equip")) return starts(new ArrayList<>(plugin.getPerkManager().perkIds()),args[4]);
        if (args.length == 2 && Arrays.asList("setspawn", "setdeath", "setspawnprotection", "setpvparena", "arenadebug").contains(args[0].toLowerCase())) return starts(new ArrayList<>(plugin.getArenaManager().getArenaIds()), args[1]);
        if(args.length==2&&args[0].equalsIgnoreCase("setup"))return starts(Arrays.asList("status","validate","export","import"),args[1]);
        if(args.length==3&&args[0].equalsIgnoreCase("setup")&&args[1].equalsIgnoreCase("import"))return starts(Collections.singletonList("confirm-replace"),args[2]);
        if (args.length == 3 && (args[0].equalsIgnoreCase("setspawnprotection") || args[0].equalsIgnoreCase("setpvparena"))) return starts(Arrays.asList("pos1", "pos2"), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("setleaderboard")) return starts(Arrays.asList("activeplayers", "unlockedfeatures", "kills", "level", "xp", "prestige"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) return starts(Arrays.asList("refresh", "list", "teleport", "debug"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("setleaderboard") && (args[1].equalsIgnoreCase("activeplayers") || args[1].equalsIgnoreCase("unlockedfeatures"))) return starts(new ArrayList<>(plugin.getArenaManager().getArenaIds()), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("removeleaderboard")) return starts(Arrays.asList("activeplayers", "unlockedfeatures"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("view")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) players.add(player.getName());
            return starts(players, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("npc")) return starts(Arrays.asList("create", "select", "remove", "rename", "movehere", "command", "setaction", "skin", "sethologram", "look", "enable", "disable", "list", "reload", "validate", "debug"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("select")) return starts(new ArrayList<>(plugin.getArenaManager().getArenaIds()), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("command")) return starts(Arrays.asList("player", "console"), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("npc") && Arrays.asList("movehere", "rename", "debug", "repairholograms", "repair").contains(args[1].toLowerCase(Locale.ENGLISH))) return starts(plugin.getNpcService().stableIds(), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("launchpad")) return starts(Arrays.asList("add", "remove", "list"), args[1]);
        if (args.length == 2 && args[0].equalsIgnoreCase("coinspawn")) return starts(Arrays.asList("add", "remove", "list", "respawn", "diagnose"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("launchpad") && args[1].equalsIgnoreCase("remove")) return starts(plugin.getSlimeLaunchPadManager().locationIds(), args[2]);
        if (args.length == 3 && args[0].equalsIgnoreCase("coinspawn") && args[1].equalsIgnoreCase("remove")) return starts(plugin.getCollectibleCoinManager().locationIds(), args[2]);
        if (args.length == 2 && args[0].equalsIgnoreCase("rankdebug")) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) players.add(player.getName());
            return starts(players, args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setleaderboard")) return starts(Arrays.asList("lifetime", "monthly", "weekly", "daily"), args[2]);
        return Collections.emptyList();
    }

    private List<String> starts(List<String> list, String arg) {
        List<String> out = new ArrayList<>();
        for (String s : list) if (s.toLowerCase().startsWith(arg.toLowerCase())) out.add(s);
        return out;
    }

    private void sendArenaDebug(Player player, String requested) {
        String normalized = ArenaManager.normalize(requested);
        ArenaDefinition arena = plugin.getArenaManager().getArena(normalized).orElse(null);
        ArenaManager.SpawnResolution resolution = plugin.getArenaManager().resolveSpawn(normalized);
        player.sendMessage("§6The Pit arena diagnostics");
        player.sendMessage("§7Requested arena ID: §f" + requested);
        player.sendMessage("§7Normalized arena ID: §f" + normalized);
        player.sendMessage("§7Active arena ID: §f" + plugin.getArenaManager().getCurrentArena());
        player.sendMessage("§7Exists in arenas.yml: §f" + plugin.getYamlManager().containsArena(normalized));
        player.sendMessage("§7Arena world: §f" + (arena == null ? "n/a" : arena.worldName()));
        player.sendMessage("§7World loaded: §f" + (arena != null && Bukkit.getWorld(arena.worldName()) != null));
        player.sendMessage("§7Raw YAML spawn: §f" + plugin.getYamlManager().rawSpawnValues(normalized));
        StoredLocation cached = arena == null ? null : arena.spawn();
        player.sendMessage("§7Cached StoredLocation: §f" + formatStored(cached));
        player.sendMessage("§7Resolved Bukkit Location: §f" + formatBukkit(resolution.location()));
        player.sendMessage("§7Current player location: §f" + formatBukkit(player.getLocation()));
        player.sendMessage("§7Spawn source: §f" + resolution.source());
        player.sendMessage("§7Fallback used: §f" + resolution.fallbackUsed());
        player.sendMessage("§7Tracked arenas.yml: §f" + plugin.getYamlManager().file().getAbsolutePath());
    }

    private String formatStored(StoredLocation location) {
        if (location == null) return "n/a";
        return location.worldName() + " " + location.x() + ", " + location.y() + ", " + location.z()
                + " yaw=" + location.yaw() + " pitch=" + location.pitch();
    }

    private String formatBukkit(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) return "unresolved";
        return location.getWorld().getName() + " " + location.getX() + ", " + location.getY() + ", " + location.getZ()
                + " yaw=" + location.getYaw() + " pitch=" + location.getPitch();
    }

    private String formatNumber(long value) {
        return java.text.NumberFormat.getIntegerInstance(Locale.US).format(value);
    }

    private String storageUnavailable(){return "§cThe Pit setup storage is unavailable. Nothing was changed.";}
    private String npcFailure(String fallback){return plugin.getSetupService().repository().available()?fallback:storageUnavailable();}
    private void sendNpcMutation(Player player,String id,PitNpcService.MutationResult result,String success){
        if(result==PitNpcService.MutationResult.SUCCESS)player.sendMessage(plugin.color("&aNPC &e"+id+" &a"+success+"."));
        else if(result==PitNpcService.MutationResult.NOT_FOUND)player.sendMessage(plugin.color("&cNo NPC exists with the name &e"+id+"&c."));
        else if(result==PitNpcService.MutationResult.ALREADY_EXISTS)player.sendMessage(plugin.color("&cAn NPC already exists with the name &e"+id+"&c."));
        else player.sendMessage(storageUnavailable());
    }
    private void sendNpcUsage(Player player,String label){
        player.sendMessage(CC.color("&6NPC administration"));
        player.sendMessage(CC.color("&e/npc create <arena-id> <npc-id> <type>"));
        player.sendMessage(CC.color("&e/npc select <arena-id> <npc-id>"));
        player.sendMessage(CC.color("&e/npc command [player|console] <command...>"));
        player.sendMessage(CC.color("&e/npc <remove|setlocation|enable|disable> <arena-id> <npc-id>"));
        player.sendMessage(CC.color("&e/npc <setskin|setaction|sethologram> <arena-id> <npc-id> <value>"));
        player.sendMessage(CC.color("&e/npc <list|reload|validate> <arena-id>"));
        player.sendMessage(CC.color("&7Advanced: /npc <status|debug|repairholograms|migratefromdatabase>"));
    }
    private String senderKey(CommandSender sender){return sender instanceof Player p?p.getUniqueId().toString():"console";}
    private void reloadRuntimeSetup(){plugin.getLeaderboardManager().reload();plugin.getNpcService().reload();plugin.getSlimeLaunchPadManager().reload();plugin.getCollectibleCoinManager().reload();plugin.getBuildScoreboardManager().reload();plugin.getTablistManager().start();}
}
