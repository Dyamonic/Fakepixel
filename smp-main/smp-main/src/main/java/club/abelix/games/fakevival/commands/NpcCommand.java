package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.NpcManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class NpcCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final NpcManager npcManager;
    private final Map<UUID, String> selectedNpcs = new HashMap<>();

    public NpcCommand(JavaPlugin plugin, NpcManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.admin.npc") && !sender.hasPermission("fakevival.npc.admin"))
            return CommandUtil.adminDenied(sender);

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(CC.color("&bFakevival NPCs: &f" + String.join("&7, &f", npcManager.getNpcIds())));
            return true;
        }

        if (args[0].equalsIgnoreCase("shops")) {
            sender.sendMessage(CC.color("&bNPC Shop IDs: &f" + String.join("&7, &f",
                    club.abelix.games.fakevival.config.defaults.NpcShopDefaults.ids())));
            sender.sendMessage(CC.color("&7Assign with: &f/npc action <id> shop:<shop_id>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /npc debug <id>"));
                return true;
            }
            String id = args[1].trim().toLowerCase(Locale.ROOT);
            String action = npcManager.getEffectiveNpcAction(id);
            String shopId = "";
            if (action.toLowerCase(Locale.ROOT).startsWith("shop:"))
                shopId = action.substring("shop:".length()).trim().toLowerCase(Locale.ROOT);
            boolean exists = plugin instanceof Game game && !shopId.isBlank() && game.getMenuManager().shopExists(shopId);
            int entries = plugin instanceof Game game && !shopId.isBlank() ? game.getMenuManager().shopEntryCount(shopId) : 0;
            String source = plugin instanceof Game game && !shopId.isBlank() ? game.getMenuManager().shopSource(shopId) : "MISSING";
            sender.sendMessage(CC.color("&7NPC ID: &e" + id));
            sender.sendMessage(CC.color("&7Action: &e" + (action.isBlank() ? "none" : action)));
            sender.sendMessage(CC.color("&7Parsed shop id: &e" + (shopId.isBlank() ? "none" : shopId)));
            sender.sendMessage(CC.color("&7Shop exists: " + (exists ? "&atrue" : "&cfalse")));
            sender.sendMessage(CC.color("&7Shop entries: &e" + entries));
            sender.sendMessage(CC.color("&7Shop source: &e" + source));
            return true;
        }

        if (args[0].equalsIgnoreCase("debugteams")) {
            var teams = npcManager.npcTeamDebug();
            sender.sendMessage(CC.color("&bNPC Scoreboard Teams: &f" + teams.size()));
            for (var team : teams) {
                sender.sendMessage(CC.color("&7NPC ID: &e" + team.npcId() + " &7Team Name: &e" + team.teamName()
                        + " &7Registered: " + yes(team.registered()) + " &7Viewers: &e" + team.viewers()));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("skinstatus")) {
            NpcManager.SkinCacheStatus status = npcManager.skinCacheStatus();
            sender.sendMessage(CC.color("&bNPC Skin Cache Status"));
            sender.sendMessage(CC.color("&7Memory cached skins: &e" + status.memoryCached()));
            sender.sendMessage(CC.color("&7DB cached skins: &e" + status.dbCached()));
            sender.sendMessage(CC.color("&7Queued lookups: &e" + status.queued()));
            sender.sendMessage(CC.color("&7Remote requests this minute: &e" + status.requestsThisMinute() + "/" + status.requestLimit()));
            sender.sendMessage(CC.color("&7Backoff active: " + (status.backoffActive() ? "&ctrue" : "&afalse")));
            sender.sendMessage(CC.color("&7Backoff remaining: &e" + duration(status.backoffRemainingMillis())));
            sender.sendMessage(CC.color("&7Last 429: &e" + (status.last429At() <= 0L ? "never" : duration(System.currentTimeMillis() - status.last429At()) + " ago")));
            return true;
        }

        if (args[0].equalsIgnoreCase("skincache")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /npc skincache <skinName>"));
                return true;
            }
            sender.sendMessage(CC.color("&7Skin &e" + args[1] + "&7: &f" + npcManager.skinCacheInfo(args[1])));
            return true;
        }

        if (args[0].equalsIgnoreCase("clearskincache")) {
            if (args.length < 2) {
                sender.sendMessage(CC.color("&cUsage: /npc clearskincache <skinName>"));
                return true;
            }
            npcManager.clearSkinCache(args[1]);
            sender.sendMessage(CC.color("&aCleared the memory and MariaDB cache for &f" + args[1] + "&a."));
            return true;
        }

        if (args[0].equalsIgnoreCase("reloadskins")) {
            npcManager.reloadSkins();
            sender.sendMessage(CC.color("&aReloading NPC skins from MariaDB first; only missing or expired skins will be queued."));
            return true;
        }

        if (args[0].equalsIgnoreCase("validate")) {
            NpcManager.SkinValidation validation = npcManager.validateSkinLoading();
            NpcManager.TeamValidation teamValidation = npcManager.validateNpcTeams();
            sender.sendMessage(CC.color("&bNPC Skin Validation"));
            sender.sendMessage(CC.color("&7Cache preload complete: " + yes(validation.cachePreloaded())));
            sender.sendMessage(CC.color("&7Remote HTTP is asynchronous: " + yes(validation.asyncRemoteLookups())));
            sender.sendMessage(CC.color("&7Duplicate names deduplicated: " + yes(validation.duplicateRequestsDeduplicated())));
            sender.sendMessage(CC.color("&7Fallback profiles supported: " + yes(validation.fallbacksSupported())));
            sender.sendMessage(CC.color("&7HTTP 429/EOF handled safely: " + yes(validation.transientFailuresSafe())));
            sender.sendMessage(CC.color("&bNPC Team Validation"));
            sender.sendMessage(CC.color("&7Unique/stable team names: " + yes(teamValidation.collisions() == 0 && teamValidation.stableNames())));
            sender.sendMessage(CC.color("&7Names at most 16 characters: " + yes(teamValidation.namesOver16() == 0)));
            sender.sendMessage(CC.color("&7CREATE tracked per viewer/team: " + yes(teamValidation.createTrackedPerViewer())));
            sender.sendMessage(CC.color("&7Initial proxy state reset: " + yes(teamValidation.initialProxyReset())));
            sender.sendMessage(CC.color("&7Tablist/NPC namespace isolated: " + yes(teamValidation.tablistNamespaceIsolated())));
            return true;
        }

        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);

        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cUsage: /npc create <id> <skinPlayer> <displayName>"));
                return true;
            }
            String id = args[1].toLowerCase();
            String skin = args[2];
            String name = args.length >= 4 ? join(args, 3) : id;
            if (!npcManager.createNpc(id, name, skin, player.getLocation())) {
                sender.sendMessage(CC.color("&cThat NPC already exists."));
                return true;
            }
            selectedNpcs.put(player.getUniqueId(), id);
            sender.sendMessage(CC.color("&aCreated NPC &f" + id + " &awith skin &f" + skin + "&a."));
            return true;
        }

        if (args[0].equalsIgnoreCase("select")) {
            String id = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : npcManager.getNearestNpcId(player.getLocation(), 6.0D);
            if (id == null || !npcManager.getNpcIds().contains(id)) {
                sender.sendMessage(CC.color("&cNo NPC found. Use &f/npc select <id> &cor stand closer to one."));
                return true;
            }
            selectedNpcs.put(player.getUniqueId(), id);
            sender.sendMessage(CC.color("&aSelected NPC &f" + id + "&a."));
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            String id = resolveNpcId(player, args);
            if (id == null) {
                sender.sendMessage(CC.color("&cUsage: /npc delete <id>"));
                return true;
            }
            sender.sendMessage(npcManager.deleteNpc(id)
                    ? CC.color("&aDeleted NPC &f" + id + "&a.")
                    : CC.color("&cThat NPC does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("movehere")) {
            String id = resolveNpcId(player, args);
            if (id == null) {
                sender.sendMessage(CC.color("&cUsage: /npc movehere <id>"));
                return true;
            }
            sender.sendMessage(npcManager.teleportNpc(id, player.getLocation())
                    ? CC.color("&aMoved NPC &f" + id + " &ato your location.")
                    : CC.color("&cThat NPC does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("skin")) {
            IdAndStart parsed = resolveNpcIdAndStart(player, args);
            if (parsed == null || args.length <= parsed.valueStart()) {
                sender.sendMessage(CC.color("&cUsage: /npc skin [id] <playerName>"));
                return true;
            }
            String skin = args[parsed.valueStart()];
            sender.sendMessage(npcManager.setSkin(parsed.id(), skin)
                    ? CC.color("&aUpdated NPC skin to &f" + skin + "&a.")
                    : CC.color("&cThat NPC does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("name") || args[0].equalsIgnoreCase("rename")) {
            IdAndStart parsed = resolveNpcIdAndStart(player, args);
            if (parsed == null || args.length <= parsed.valueStart()) {
                sender.sendMessage(CC.color("&cUsage: /npc rename [id] <displayName>"));
                return true;
            }
            sender.sendMessage(npcManager.setName(parsed.id(), join(args, parsed.valueStart()))
                    ? CC.color("&aUpdated NPC name.")
                    : CC.color("&cThat NPC does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("action")) {
            IdAndStart parsed = resolveNpcIdAndStart(player, args);
            if (parsed == null || args.length <= parsed.valueStart()) {
                sender.sendMessage(CC.color("&cUsage: /npc action [id] <action>"));
                sender.sendMessage(CC.color("&7Actions: &fauctionhouse&7, &fbazaar&7, &fbank&7, &fadventure_guild&7, &fskills&7, &fjobs&7, &fbattlepass&7, &fshop:<id>&7, &fmayor:<register|vote|results>&7, &fplayer:<cmd>&7, &fconsole:<cmd>"));
                return true;
            }
            String action = join(args, parsed.valueStart());
            sender.sendMessage(npcManager.setAction(parsed.id(), action)
                    ? CC.color("&aUpdated NPC action to &f" + action + "&a.")
                    : CC.color("&cThat NPC does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("look")) {
            IdAndStart parsed = resolveNpcIdAndStart(player, args);
            if (parsed == null || args.length <= parsed.valueStart()) {
                sender.sendMessage(CC.color("&cUsage: /npc look [id] <on|off>"));
                return true;
            }
            Boolean enabled = parseToggle(args[parsed.valueStart()]);
            if (enabled == null) {
                sender.sendMessage(CC.color("&cUse &fon &cor &foff&c."));
                return true;
            }
            sender.sendMessage(npcManager.setLookAtPlayer(parsed.id(), enabled)
                    ? CC.color(enabled ? "&aNPC will now look at nearby players." : "&aNPC stopped looking at players.")
                    : CC.color("&cThat NPC does not exist."));
            return true;
        }

        if (args[0].equalsIgnoreCase("type")) {
            IdAndStart parsed = resolveNpcIdAndStart(player, args);
            if (parsed == null || args.length <= parsed.valueStart()) {
                sender.sendMessage(CC.color("&cUsage: /npc type [id] <player|mobType>"));
                sender.sendMessage(CC.color("&7Examples: &f/npc type villager&7, &f/npc type zombie&7, &f/npc type player"));
                return true;
            }
            String type = args[parsed.valueStart()].toUpperCase(Locale.ROOT);
            sender.sendMessage(npcManager.setType(parsed.id(), type)
                    ? CC.color("&aUpdated NPC type to &f" + type + "&a.")
                    : CC.color("&cInvalid NPC type. Use &fplayer &cor a spawnable mob type like &fvillager&c."));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(CC.color("&bNPC Commands:"));
        sender.sendMessage(CC.color("&7/npc create <id> <skinPlayer> <displayName>"));
        sender.sendMessage(CC.color("&7/npc select [id]"));
        sender.sendMessage(CC.color("&7/npc skin [id] <playerName>"));
        sender.sendMessage(CC.color("&7/npc rename [id] <displayName>"));
        sender.sendMessage(CC.color("&7/npc action [id] <action>"));
        sender.sendMessage(CC.color("&7/npc look [id] <on|off>"));
        sender.sendMessage(CC.color("&7/npc type [id] <player|mobType>"));
        sender.sendMessage(CC.color("&7/npc movehere [id]"));
        sender.sendMessage(CC.color("&7/npc delete [id]"));
        sender.sendMessage(CC.color("&7/npc list"));
        sender.sendMessage(CC.color("&7/npc shops"));
        sender.sendMessage(CC.color("&7/npc debug <id>"));
        sender.sendMessage(CC.color("&7/npc debugteams"));
        sender.sendMessage(CC.color("&7/npc skinstatus"));
        sender.sendMessage(CC.color("&7/npc skincache <skinName>"));
        sender.sendMessage(CC.color("&7/npc clearskincache <skinName>"));
        sender.sendMessage(CC.color("&7/npc reloadskins"));
        sender.sendMessage(CC.color("&7/npc validate"));
    }

    private String yes(boolean value) { return value ? "&atrue" : "&cfalse"; }

    private String duration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        if (seconds < 60L) return seconds + "s";
        return (seconds / 60L) + "m " + (seconds % 60L) + "s";
    }

    private String resolveNpcId(Player player, String[] args) {
        String id = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : selectedNpcs.get(player.getUniqueId());
        if (id == null || !npcManager.getNpcIds().contains(id)) return null;
        return id;
    }

    private IdAndStart resolveNpcIdAndStart(Player player, String[] args) {
        if (args.length > 1) {
            String possibleId = args[1].toLowerCase(Locale.ROOT);
            if (npcManager.getNpcIds().contains(possibleId)) return new IdAndStart(possibleId, 2);
        }
        String selected = selectedNpcs.get(player.getUniqueId());
        if (selected == null || !npcManager.getNpcIds().contains(selected)) return null;
        return new IdAndStart(selected, 1);
    }

    private Boolean parseToggle(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("on") || normalized.equals("true") || normalized.equals("yes") || normalized.equals("start")) return true;
        if (normalized.equals("off") || normalized.equals("false") || normalized.equals("no") || normalized.equals("stop")) return false;
        return null;
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private record IdAndStart(String id, int valueStart) {
    }
}
