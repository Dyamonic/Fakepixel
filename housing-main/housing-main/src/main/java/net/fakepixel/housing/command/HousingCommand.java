package net.fakepixel.housing.command;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.build.SelectionService;
import net.fakepixel.housing.group.HouseGroups;
import net.fakepixel.housing.hook.AbelixHook;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import net.fakepixel.housing.menu.HousingMenu;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class HousingCommand implements CommandExecutor, TabCompleter {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final HousingMenu menu;
    private final AbelixHook abelix;
    private final Map<UUID, Long> modeClickCooldowns = new HashMap<>();

    public HousingCommand(HousingPlugin plugin, HouseManager houses, HousingMenu menu, AbelixHook abelix) {
        this.plugin = plugin;
        this.houses = houses;
        this.menu = menu;
        this.abelix = abelix;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Players only."); return true; }
        Player player = (Player) sender;
        String commandName = command.getName().toLowerCase();
        if (!commandName.equals("housing") && !commandName.equals("house") && !commandName.equals("h")) {
            return handleToolCommand(player, commandName, args);
        }

        if (!player.hasPermission("housing.use") && !isHouseToolSubcommand(args)) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) { menu.open(player); return true; }
        String sub = args[0].toLowerCase();
        if (isHouseOnlySubcommand(sub) && !requireCurrentHouse(player)) return true;

        if (sub.equals("staff")) { plugin.getStaffMenu().open(player); return true; }
        if (sub.equals("browse")) { menu.openBrowse(player, 0); return true; }
        if (sub.equals("random")) { random(player); return true; }
        if (sub.equals("list")) { menu.openList(player); return true; }
        if (sub.equals("leaderboard") || sub.equals("cookies")) { menu.openLeaderboard(player); return true; }
        if (sub.equals("manage")) { menu.openOwnerMenu(player); return true; }
        if (sub.equals("groups")) { plugin.getGroupsMenu().open(player); return true; }
        if (sub.equals("carpenter")) { menu.openCarpenterMenu(player); return true; }
        if (sub.equals("toggleredborder")) { toggleRedBorder(player); return true; }
        if (sub.equals("buildmode") && ((args.length > 1 && args[1].equalsIgnoreCase("owner")) || ownsCurrentHouse(player))) { setOwnerHouseMode(player, true); return true; }
        if (sub.equals("socialmode")) { setOwnerHouseMode(player, false); return true; }
        if (sub.equals("buildmode") || sub.equals("adminbuild")) { adminBuildMode(player); return true; }
        if (sub.equals("region") || sub.equals("regions")) {
            if (plugin.getRegionService() != null) return plugin.getRegionService().handleCommand(player, args);
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (sub.equals("cookie")) { giveCookie(player); return true; }
        if (sub.equals("home") || sub.equals("open")) { openOwnHouse(player); return true; }
        if (sub.equals("visit") || sub.equals("join")) { visit(player, args); return true; }
        if (sub.equals("invite")) { invite(player, args); return true; }
        if (sub.equals("leave") || sub.equals("lobby")) { houses.leave(player); return true; }
        if (sub.equals("main")) {
            if (plugin.getCurrentServer().isLobby()) menu.open(player);
            else if (plugin.getClusterService() != null) plugin.getClusterService().sendPlayerToLobby(player, "Housing main command", "");
            return true;
        }

        if (sub.equals("public")) { togglePublic(player); return true; }
        if (sub.equals("visitingrule") || sub.equals("visitingrules") || sub.equals("vistingrule")) { visitingRule(player, args); return true; }
        if (sub.equals("name")) { rename(player, args); return true; }
        if (sub.equals("trust")) { trust(player, args, true); return true; }
        if (sub.equals("untrust")) { trust(player, args, false); return true; }

        if (sub.equals("kick")) { kick(player, args); return true; }
        if (sub.equals("ban")) { ban(player, args, true); return true; }
        if (sub.equals("unban")) { ban(player, args, false); return true; }
        if (sub.equals("mute")) { mute(player, args, true); return true; }
        if (sub.equals("unmute")) { mute(player, args, false); return true; }
        if (sub.equals("tp")) { tp(player, args); return true; }
        if (sub.equals("group")) { group(player, args); return true; }
        if (sub.equals("tag") || sub.equals("housingtag") || sub.equals("housetag")) { housingTag(player, args); return true; }
        if (sub.equals("groupname")) { groupName(player, args); return true; }
        if (sub.equals("grouptag")) { groupTag(player, args); return true; }
        if (sub.equals("fly")) { fly(player, args); return true; }
        if (sub.equals("setspawn")) { setSpawn(player, false); return true; }
        if (sub.equals("setguestspawn")) { setSpawn(player, true); return true; }
        if (sub.equals("reset")) { reset(player); return true; }
        if (sub.equals("delete")) { delete(player, args); return true; }
        if (sub.equals("kickall")) { House h = currentManagedHouse(player); if (h != null) houses.kickAll(player, h); return true; }
        if (sub.equals("kickguests")) { House h = currentManagedHouse(player); if (h != null) houses.kickGuests(player, h); return true; }
        if (sub.equals("day")) { time(player, 6000L); return true; }
        if (sub.equals("night")) { time(player, 18000L); return true; }
        if (sub.equals("time")) { time(player, args); return true; }
        if (sub.equals("gmc")) { gameMode(player, GameMode.CREATIVE); return true; }
        if (sub.equals("gms")) { gameMode(player, GameMode.SURVIVAL); return true; }
        if (sub.equals("gma")) { gameMode(player, GameMode.ADVENTURE); return true; }
        if (sub.equals("wand")) { if (canProToolsCommand(player)) plugin.getSelectionService().giveWand(player); return true; }
        if (sub.equals("pos1")) { if (canProToolsCommand(player)) plugin.getSelectionService().setPos1(player, player.getLocation()); return true; }
        if (sub.equals("pos2")) { if (canProToolsCommand(player)) plugin.getSelectionService().setPos2(player, player.getLocation()); return true; }
        if (sub.equals("set")) { set(player, args, false); return true; }
        if (sub.equals("walls")) { set(player, args, true); return true; }
        if (sub.equals("undo")) { if (canProToolsCommand(player)) plugin.getSelectionService().undo(player); return true; }
        if (sub.equals("copy")) { if (canProToolsCommand(player)) plugin.getSelectionService().copy(player); return true; }
        if (sub.equals("tphere")) { tpHere(player, args); return true; }

        if (sub.equals("info")) { info(player); return true; }
        if (sub.equals("npc")) { npc(player, args); return true; }
        if (sub.equals("reload")) { reload(player); return true; }
        sendHelp(player);
        return true;
    }

    private void random(Player player) {
        House random = houses.getRandomPublicHouse();
        if (random == null) player.sendMessage(plugin.color("&cThere are no public houses to join."));
        else houses.visit(player, random);
    }

    private void adminBuildMode(Player player) {
        if (!player.hasPermission("housing.admin.buildmode")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        player.sendMessage(plugin.color(plugin.toggleAdminBuildMode(player) ? "&aAdmin build mode enabled." : "&cAdmin build mode disabled."));
    }

    private boolean ownsCurrentHouse(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        return house != null && house.getOwner().equals(player.getUniqueId());
    }

    private void setOwnerHouseMode(Player player, boolean buildMode) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || houses.isLobbyWorld(player.getWorld().getName())
                || !house.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (house.isBuildMode() == buildMode) {
            if (buildMode) {
                long now = System.currentTimeMillis();
                Long cooldownUntil = modeClickCooldowns.get(player.getUniqueId());
                if (cooldownUntil != null && cooldownUntil > now) return;
                modeClickCooldowns.put(player.getUniqueId(), now + 1000L);
                player.sendMessage(plugin.color("&cThis house is already in Build Mode."));
            } else {
                player.sendMessage(plugin.color("&cThis house is already in Social Mode."));
            }
            return;
        }
        modeClickCooldowns.remove(player.getUniqueId());
        house.setMode(buildMode ? "BUILD" : "SOCIAL");
        houses.saveAll();
        if (plugin.getHousingScoreboard() != null) plugin.getHousingScoreboard().refreshHouse(house);
        plugin.sendHouseModeChanged(player, house);
    }

    private void giveCookie(Player player) {
        House current = houses.getHouseByWorld(player.getWorld().getName());
        if (current == null) { player.sendMessage(plugin.color("&cYou are not inside a player house.")); return; }
        if (houses.giveCookie(player, current)) player.sendMessage(plugin.color("&aYou gave &e" + current.getName() + " &aa cookie!"));
        else player.sendMessage(plugin.color("&cYou already gave this house a cookie, or you own this house."));
    }

    private void openOwnHouse(Player player) {
        String required = plugin.getStaticConfig().getString("limits.create-rank", "DEFAULT");
        if (abelix.hasRankOrHigher(player, required)) { player.sendMessage(plugin.color("&cYou need rank &e" + required + " &cto create a house.")); return; }
        houses.sendToOwnHouse(player);
    }

    private void visit(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /housing visit <player>")); return; }
        House house = houses.findByOwnerName(args[1]);
        if (house == null || !houses.canEnter(player, house)) { player.sendMessage(plugin.msg("not-found")); return; }
        houses.visit(player, house);
    }

    private void toggleRedBorder(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) {
            player.sendMessage(plugin.color("&cYou must be inside a player house to use this command."));
            return;
        }
        if (!house.getOwner().equals(player.getUniqueId()) && !house.hasGroupPermission(player.getUniqueId(), "build")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        boolean visible = house.toggleBorderVisibility(player.getUniqueId());
        houses.saveAll();
        houses.refreshBorderVisibility(player, house);
        player.sendMessage(plugin.color(visible ? "&aRed border visibility enabled." : "&cRed border visibility disabled."));
    }

    private void invite(Player player, String[] args) {
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /housing invite <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(plugin.color("&cThat player must be online.")); return; }
        if (!houses.invite(player, target)) player.sendMessage(plugin.msg("no-permission"));
    }

    private void togglePublic(Player player) {
        House house = currentManagedHouse(player); if (house == null) return;
        house.setPublicHouse(!house.isPublicHouse()); houses.saveAll();
        player.sendMessage(plugin.color("&7Your house is now " + (house.isPublicHouse() ? "&apublic" : "&cprivate") + "&7."));
    }

    private void visitingRule(Player player, String[] args) {
        House house = currentManagedHouse(player); if (house == null) return;
        if (args.length < 2) {
            player.sendMessage(plugin.color("&cUsage: /visitingrule <public|friends|party|guild|private>"));
            return;
        }
        String rule = House.normalizeVisitingRule(args[1]);
        house.setVisitingRule(rule);
        houses.saveAll();
        player.sendMessage(plugin.color("&aVisiting Rules set to &e" + house.getVisitingRuleDisplay() + "&a."));
    }

    private void rename(Player player, String[] args) {
        House house = currentManagedHouse(player); if (house == null) return;
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /housing name <name>")); return; }
        String name = joinFrom(args, 1);
        int max = plugin.getStaticConfig().getInt("limits.max-name-length", 24);
        if (name.length() > max) name = name.substring(0, max);
        house.setName(name); houses.saveAll();
        player.sendMessage(plugin.color("&aHouse renamed to &e" + name + "&a."));
    }

    private void trust(Player player, String[] args, boolean trust) {
        House house = currentManagedHouse(player); if (house == null) return;
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /housing " + (trust ? "trust" : "untrust") + " <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(plugin.color("&cThat player must be online.")); return; }
        if (trust) { house.getTrusted().add(target.getUniqueId()); player.sendMessage(plugin.color("&aTrusted &e" + target.getName() + "&a.")); }
        else { house.getTrusted().remove(target.getUniqueId()); player.sendMessage(plugin.color("&cUntrusted &e" + target.getName() + "&c.")); }
        houses.saveAll();
    }

    private void kick(Player player, String[] args) {
        House house = currentHouseWithPermission(player, "kick"); if (house == null) return;
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /housing kick <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(plugin.color("&cThat player must be online.")); return; }
        houses.kick(player, target, house);
    }

    private void ban(Player player, String[] args, boolean ban) {
        House house = currentHouseWithPermission(player, "ban"); if (house == null) return;
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /housing " + (ban ? "ban" : "unban") + " <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(plugin.color("&cThat player must be online.")); return; }
        if (target.getUniqueId().equals(house.getOwner())) { player.sendMessage(plugin.color("&cYou cannot ban the owner.")); return; }
        if (ban && houses.isPunishmentProtected(target)) { player.sendMessage(plugin.color("&cYou cannot punish staff members.")); return; }
        if (ban) { house.getBanned().add(target.getUniqueId()); if (target.getWorld().getName().equals(house.getWorldName())) houses.leave(target); player.sendMessage(plugin.color("&cBanned &e" + target.getName() + "&c.")); }
        else { house.getBanned().remove(target.getUniqueId()); player.sendMessage(plugin.color("&aUnbanned &e" + target.getName() + "&a.")); }
        houses.saveAll();
    }

    private void mute(Player player, String[] args, boolean mute) {
        House house = currentHouseWithPermission(player, "mute"); if (house == null) return;
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /housing " + (mute ? "mute" : "unmute") + " <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(plugin.color("&cThat player must be online.")); return; }
        if (mute && houses.isPunishmentProtected(target)) { player.sendMessage(plugin.color("&cYou cannot punish staff members.")); return; }
        if (mute) { house.getMuted().add(target.getUniqueId()); player.sendMessage(plugin.color("&cMuted &e" + target.getName() + "&c.")); }
        else { house.getMuted().remove(target.getUniqueId()); player.sendMessage(plugin.color("&aUnmuted &e" + target.getName() + "&a.")); }
        houses.saveAll();
    }

    private void tp(Player player, String[] args) {
        House house = currentHouseWithPermission(player, "teleport"); if (house == null) return;
        if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /housing tp <player> <player>")); return; }
        Player a = Bukkit.getPlayer(args[1]); Player b = Bukkit.getPlayer(args[2]);
        if (a == null || b == null) { player.sendMessage(plugin.color("&cBoth players must be online.")); return; }
        if (!a.getWorld().getName().equals(house.getWorldName()) || !b.getWorld().getName().equals(house.getWorldName())) { player.sendMessage(plugin.color("&cBoth players must be in this house.")); return; }
        a.teleport(b); player.sendMessage(plugin.color("&aTeleported &e" + a.getName() + " &ato &e" + b.getName() + "&a."));
    }

    private void tpHere(Player player, String[] args) {
        House house = currentHouseWithPermission(player, "teleport"); if (house == null) return;
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /tphere <player>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(plugin.color("&cThat player must be online.")); return; }
        if (!target.getWorld().getName().equals(house.getWorldName()) || !player.getWorld().getName().equals(house.getWorldName())) {
            player.sendMessage(plugin.color("&cBoth players must be in this house."));
            return;
        }
        if (target.getUniqueId().equals(house.getOwner())) {
            player.sendMessage(plugin.color("&cYou cannot teleport the owner."));
            return;
        }
        target.teleport(player);
        player.sendMessage(plugin.color("&aTeleported &e" + target.getName() + " &ato you."));
    }

    private void group(Player player, String[] args) {
        House house = currentManagedHouse(player); if (house == null) return;
        if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /housing group <player> <group>")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { player.sendMessage(plugin.color("&cThat player must be online.")); return; }
        String group = HouseGroups.normalize(joinFrom(args, 2));
        if (group.equals(HouseGroups.OWNER)) { player.sendMessage(plugin.color("&cYou cannot assign owner.")); return; }
        house.setGroup(target.getUniqueId(), group); houses.saveAll();
        if (plugin.getNametagService() != null) plugin.getNametagService().update(target);
        player.sendMessage(plugin.color("&aSet &e" + target.getName() + " &ato " + house.groupSuffix(group) + "&a."));
    }


    private void housingTag(Player player, String[] args) {
        House house = currentManagedHouse(player); if (house == null) return;
        if (args.length < 2) {
            player.sendMessage(plugin.color("&cUsage: /housingtag <tag>"));
            player.sendMessage(plugin.color("&7Tags: roleplay, parkour, building, chatting, maze, competition, event, guild-hangout, minigame, new-houses"));
            return;
        }
        String tag = normalizeTag(joinFrom(args, 1));
        house.setHousingTag(tag);
        houses.saveAll();
        player.sendMessage(plugin.color("&aSet housing tag to &e" + prettyTag(tag) + "&a."));
    }

    private void groupName(Player player, String[] args) {
        House house = currentManagedHouse(player); if (house == null) return;
        if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /groupname <group> <new name>")); return; }
        String group = HouseGroups.normalize(args[1]);
        if (group.equals(HouseGroups.OWNER)) { player.sendMessage(plugin.color("&cThe Owner group name cannot be edited.")); return; }
        String name = joinFrom(args, 2);
        if (name.length() > 24) name = name.substring(0, 24);
        if (!house.setGroupDisplayName(group, name)) { player.sendMessage(plugin.color("&cCould not update that group name.")); return; }
        houses.saveAll();
        if (plugin.getNametagService() != null) {
            for (Player online : Bukkit.getOnlinePlayers()) plugin.getNametagService().update(online);
        }
        player.sendMessage(plugin.color("&aSet group &e" + group + " &aname to &r" + name + "&a."));
    }

    private void groupTag(Player player, String[] args) {
        House house = currentManagedHouse(player); if (house == null) return;
        if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /grouptag <group> <new tag>")); return; }
        String group = HouseGroups.normalize(args[1]);
        if (group.equals(HouseGroups.OWNER)) { player.sendMessage(plugin.color("&cThe Owner group tag cannot be edited.")); return; }
        String tag = joinFrom(args, 2);
        if (tag.length() > 24) tag = tag.substring(0, 24);
        if (!house.setGroupTag(group, tag)) { player.sendMessage(plugin.color("&cCould not update that group tag.")); return; }
        houses.saveAll();
        player.sendMessage(plugin.color("&aSet group &e" + group + " &atag to &r" + tag + "&a."));
    }

    private void fly(Player player, String[] args) {
        House house = currentHouseWithPermission(player, "fly");
        if (house == null && isAbelixStaff(player) && player.hasPermission("housing.staff.fly")) {
            house = houses.getHouseByWorld(player.getWorld().getName());
        }
        if (house == null) return;
        if (!house.isFlyEnabled()) { player.setAllowFlight(false); player.setFlying(false); player.sendMessage(plugin.color("&cFly is disabled in this house.")); return; }
        boolean enable = !player.getAllowFlight();
        if (args.length >= 2) {
            String state = args[1].toLowerCase();
            if (state.equals("on") || state.equals("enable") || state.equals("enabled") || state.equals("true")) enable = true;
            if (state.equals("off") || state.equals("disable") || state.equals("disabled") || state.equals("false")) enable = false;
        }
        player.setAllowFlight(enable);
        if (!enable) player.setFlying(false);
        player.sendMessage(plugin.color(enable ? "&aFlight enabled." : "&cFlight disabled."));
    }

    private String normalizeTag(String tag) {
        if (tag == null) return "new-houses";
        String clean = tag.trim().toLowerCase().replace("_", "-").replace(" ", "-");
        if (clean.equals("roleplay") || clean.equals("rp")) return "roleplay";
        if (clean.equals("parkour")) return "parkour";
        if (clean.equals("building")) return "building";
        if (clean.equals("chatting") || clean.equals("chat")) return "chatting";
        if (clean.equals("maze")) return "maze";
        if (clean.equals("competition")) return "competition";
        if (clean.equals("event")) return "event";
        if (clean.equals("guildhangout") || clean.equals("guild-hangout")) return "guild-hangout";
        if (clean.equals("minigame")) return "minigame";
        if (clean.equals("new") || clean.equals("newhouses")) return "new-houses";
        return clean;
    }

    private String prettyTag(String tag) {
        String clean = normalizeTag(tag).replace('-', ' ');
        StringBuilder out = new StringBuilder();
        for (String part : clean.split(" ")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private void reset(Player player) { House house = currentManagedHouse(player); if (house != null) houses.resetHouse(player, house); }

    private void setSpawn(Player player, boolean guest) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) {
            player.sendMessage(plugin.color("&cYou must be inside your house to use this."));
            return;
        }
        if (!canEditHouseSettings(player, house)) {
            player.sendMessage(plugin.color("&cOnly the house owner can set the spawn."));
            return;
        }
        Location location = player.getLocation().clone();
        if (guest) {
            house.setGuestSpawn(location);
            houses.saveAll();
            player.sendMessage(plugin.color("&aGuest spawn set!"));
        } else {
            house.setSpawn(location);
            houses.saveAll();
            player.sendMessage(plugin.color("&aHouse spawn set!"));
        }
    }

    private void delete(Player player, String[] args) {
        boolean staffDelete = player.hasPermission("housing.admin") || player.hasPermission("housing.staff.delete");
        House house = null;
        if (args.length < 2) {
            house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null || !house.getOwner().equals(player.getUniqueId())) house = houses.getHouse(player.getUniqueId());
        } else if (staffDelete) {
            house = houses.findByOwnerName(args[1]);
        } else {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (house == null && args.length >= 2) {
            try {
                house = houses.getHouse(java.util.UUID.fromString(args[1]));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (house == null) {
            player.sendMessage(plugin.color("&cYou do not have a house to delete."));
            return;
        }
        houses.deleteHouse(player, house);
    }

    private void time(Player player, long ticks) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) { player.sendMessage(plugin.color("&cYou can only use this command inside a player house.")); return; }
        if (!canEditHouseSettings(player, house)) { player.sendMessage(plugin.color("&cYou do not have permission to change this house's time.")); return; }
        World world = Bukkit.getWorld(house.getWorldName());
        if (world != null) {
            house.setTimeTicks(ticks);
            world.setTime(ticks);
            houses.saveAll();
            player.sendMessage(plugin.color("&aHouse time set to " + timeName(ticks) + "."));
        }
    }

    private void time(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.color("&cUsage: /time <day|night|noon|midnight|sunrise|sunset>"));
            return;
        }
        Long ticks = timeTicks(args[1]);
        if (ticks == null) {
            player.sendMessage(plugin.color("&cUsage: /time <day|night|noon|midnight|sunrise|sunset>"));
            return;
        }
        time(player, ticks);
    }

    private Long timeTicks(String value) {
        if (value == null) return null;
        String clean = value.toLowerCase();
        if (clean.equals("day")) return 1000L;
        if (clean.equals("noon")) return 6000L;
        if (clean.equals("sunset")) return 12000L;
        if (clean.equals("night")) return 13000L;
        if (clean.equals("midnight")) return 18000L;
        if (clean.equals("sunrise")) return 23000L;
        return null;
    }

    private String timeName(long ticks) {
        if (ticks == 1000L) return "day";
        if (ticks == 6000L) return "noon";
        if (ticks == 12000L) return "sunset";
        if (ticks == 13000L) return "night";
        if (ticks == 18000L) return "midnight";
        if (ticks == 23000L) return "sunrise";
        return "day";
    }

    private void gameMode(Player player, GameMode mode) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) {
            player.sendMessage(plugin.color("&cYou must be inside a player house to use this command."));
            return;
        }
        if (!house.getOwner().equals(player.getUniqueId())) {
            if (!canUseStaffGameMode(player)) {
                player.setGameMode(GameMode.SURVIVAL);
                player.sendMessage(plugin.color("&cYou cannot switch gamemodes in another player's house."));
                return;
            }
        } else if (!house.hasGroupPermission(player.getUniqueId(), "gamemode")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (houses.isVisitingOtherHouse(player) && mode != GameMode.SURVIVAL && !canUseStaffGameMode(player)) {
            player.setGameMode(GameMode.SURVIVAL);
            player.sendMessage(plugin.color("&cYou cannot switch gamemodes in another player's house."));
            return;
        }
        player.setGameMode(mode);
        player.sendMessage(plugin.color("&aGamemode set to &e" + mode.name().toLowerCase() + "&a."));
    }

    private void set(Player player, String[] args, boolean walls) {
        if (!canProToolsCommand(player)) return;
        if (args.length < 2) { player.sendMessage(plugin.color("&cUsage: /housing " + (walls ? "walls" : "set") + " <block[:data]>")); return; }
        SelectionService.MaterialData data = plugin.getSelectionService().parseMaterial(args[1]);
        if (data == null || data.material == net.fakepixel.housing.util.Compat.mat("AIR")) { player.sendMessage(plugin.color("&cUnknown block type.")); return; }
        plugin.getSelectionService().set(player, data.material, data.data, walls);
    }

    private boolean handleToolCommand(Player player, String commandName, String[] args) {
        if (commandName.equals("visit")) { visit(player, prepend(commandName, args)); return true; }
        if (isHouseOnlySubcommand(commandName) && !requireCurrentHouse(player)) return true;
        if (commandName.equals("adminbuild") || commandName.equals("buildmode")) { adminBuildMode(player); return true; }
        if (commandName.equals("gmc")) { gameMode(player, GameMode.CREATIVE); return true; }
        if (commandName.equals("gms")) { gameMode(player, GameMode.SURVIVAL); return true; }
        if (commandName.equals("gma")) { gameMode(player, GameMode.ADVENTURE); return true; }
        if (commandName.equals("wand")) { if (canProToolsCommand(player)) plugin.getSelectionService().giveWand(player); return true; }
        if (commandName.equals("pos1")) { if (canProToolsCommand(player)) plugin.getSelectionService().setPos1(player, player.getLocation()); return true; }
        if (commandName.equals("pos2")) { if (canProToolsCommand(player)) plugin.getSelectionService().setPos2(player, player.getLocation()); return true; }
        if (commandName.equals("undo")) { if (canProToolsCommand(player)) plugin.getSelectionService().undo(player); return true; }
        if (commandName.equals("copy")) { if (canProToolsCommand(player)) plugin.getSelectionService().copy(player); return true; }
        if (commandName.equals("set")) { set(player, prepend(commandName, args), false); return true; }
        if (commandName.equals("walls")) { set(player, prepend(commandName, args), true); return true; }
        if (commandName.equals("tp")) { tp(player, prepend(commandName, args)); return true; }
        if (commandName.equals("tphere")) { tpHere(player, prepend(commandName, args)); return true; }
        if (commandName.equals("housingtag")) { housingTag(player, prepend(commandName, args)); return true; }
        if (commandName.equals("visitingrule") || commandName.equals("visitingrules") || commandName.equals("vistingrule")) { visitingRule(player, prepend(commandName, args)); return true; }
        if (commandName.equals("groupname")) { groupName(player, prepend(commandName, args)); return true; }
        if (commandName.equals("grouptag")) { groupTag(player, prepend(commandName, args)); return true; }
        if (commandName.equals("fly")) { fly(player, prepend(commandName, args)); return true; }
        if (commandName.equals("time")) { time(player, prepend(commandName, args)); return true; }
        return false;
    }

    private String[] prepend(String first, String[] args) {
        String[] shifted = new String[args.length + 1];
        shifted[0] = first;
        System.arraycopy(args, 0, shifted, 1, args.length);
        return shifted;
    }

    private boolean isHouseToolSubcommand(String[] args) {
        if (args == null || args.length == 0 || args[0] == null) return false;
        String sub = args[0].toLowerCase();
        return sub.equals("region")
                || sub.equals("regions")
                || sub.equals("wand")
                || sub.equals("pos1")
                || sub.equals("pos2")
                || sub.equals("set")
                || sub.equals("walls")
                || sub.equals("undo")
                || sub.equals("copy");
    }

    private boolean canBuildCommand(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !plugin.canBuildInHouseMode(player, house)) { player.sendMessage(plugin.msg("no-permission")); return false; }
        return true;
    }

    private boolean canProToolsCommand(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (plugin.getProToolsService() == null || !plugin.getProToolsService().canUse(player, house)) {
            player.sendMessage(plugin.msg("no-permission"));
            return false;
        }
        return true;
    }

    private boolean canUseStaffGameMode(Player player) {
        return isAbelixStaff(player) && player.hasPermission("housing.staff.gamemode");
    }

    private boolean isAbelixStaff(Player player) {
        return plugin.getRankPermissionService() != null && plugin.getRankPermissionService().isHousingStaff(player);
    }

    private House currentManagedHouse(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) { player.sendMessage(plugin.color("&cYou must be inside a player house to use this command.")); return null; }
        if (house == null || !house.canManage(player.getUniqueId())) { player.sendMessage(plugin.color("&cOnly the owner/co-owner can do that.")); return null; }
        return house;
    }

    private House currentHouseWithPermission(Player player, String permission) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) { player.sendMessage(plugin.color("&cYou must be inside a player house to use this command.")); return null; }
        if (house == null || !house.hasGroupPermission(player.getUniqueId(), permission)) {
            player.sendMessage(plugin.msg("no-permission"));
            return null;
        }
        return house;
    }

    private boolean requireCurrentHouse(Player player) {
        if (houses.getHouseByWorld(player.getWorld().getName()) != null) return true;
        player.sendMessage(plugin.color("&cYou must be inside a player house to use this command."));
        return false;
    }

    private boolean isHouseOnlySubcommand(String sub) {
        return sub.equals("name") || sub.equals("gmc") || sub.equals("gms") || sub.equals("gma")
                || sub.equals("mute") || sub.equals("unmute") || sub.equals("ban") || sub.equals("kick")
                || sub.equals("unban") || sub.equals("tp") || sub.equals("tphere") || sub.equals("group")
                || sub.equals("groups") || sub.equals("tag") || sub.equals("housingtag") || sub.equals("housetag")
                || sub.equals("visitingrule") || sub.equals("visitingrules") || sub.equals("vistingrule")
                || sub.equals("groupname") || sub.equals("grouptag") || sub.equals("fly")
                || sub.equals("kickguests") || sub.equals("kickall") || sub.equals("manage")
                || sub.equals("reset") || sub.equals("toggleredborder");
    }

    private boolean canEditHouseSettings(Player player, House house) {
        return player != null && house != null
                && (house.getOwner().equals(player.getUniqueId()) || house.hasGroupPermission(player.getUniqueId(), "settings"));
    }

    private void info(Player player) {
        House house = houses.getHouse(player.getUniqueId());
        player.sendMessage(plugin.color("&6Housing Info"));
        player.sendMessage(plugin.color("&7Rank: &f" + abelix.getRankName(player) + " &8(" + abelix.getRankPriority(player) + ")"));
        player.sendMessage(plugin.color("&7Rank Category: &f" + plugin.getRankPermissionService().getDetectedCategory(player)));
        player.sendMessage(plugin.color("&7Gold: &6" + abelix.getGold(player)));
        player.sendMessage(plugin.color("&7Prefix: &r" + plugin.color(abelix.getPrefix(player)) + player.getName()));
        player.sendMessage(plugin.color("&7World Service: &f" + plugin.getWorldService().getName()));
        player.sendMessage(plugin.color("&7House: &f" + (house == null ? "None" : house.getWorldName())));
    }

    private void reload(Player player) {
        if (!player.hasPermission("housing.admin")) { player.sendMessage(plugin.msg("no-permission")); return; }
        // Static Housing settings are compiled into Java registries.
        plugin.getRankPermissionService().load();
        houses.loadAll();
        if (plugin.getNametagService() != null) plugin.getNametagService().reload();
        player.sendMessage(plugin.msg("reload"));
    }

    private void npc(Player player, String[] args) {
        if (!player.hasPermission("housing.admin")) { player.sendMessage(plugin.msg("no-permission")); return; }
        if (args.length < 2 || args[1].equalsIgnoreCase("create")) {
            plugin.getHousingNpcService().create(player, args.length >= 3 ? joinFrom(args, 2) : plugin.getStaticConfig().getString("housing-npcs.default-name", "Housing"));
            return;
        }
        if (args[1].equalsIgnoreCase("rename")) {
            if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /housing npc rename <name>")); return; }
            if (plugin.getHousingNpcService().renameNearest(player, joinFrom(args, 2))) player.sendMessage(plugin.color("&aRenamed nearest housing NPC."));
            else player.sendMessage(plugin.color("&cNo housing NPC found nearby."));
            return;
        }
        if (args[1].equalsIgnoreCase("skin")) {
            if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /housing npc skin <name>")); return; }
            plugin.getHousingNpcService().setSkinNearest(player, args[2]);
            player.sendMessage(plugin.color("&7Looking up skin for &e" + args[2] + "&7..."));
            return;
        }
        if (args[1].equalsIgnoreCase("command")) {
            if (args.length < 4 || !args[2].equalsIgnoreCase("add")) {
                player.sendMessage(plugin.color("&cUsage: /housing npc command add <command>"));
                return;
            }
            String command = joinFrom(args, 3);
            if (command.startsWith("/")) command = command.substring(1);
            if (plugin.getHousingNpcService().setCommandNearest(player, command)) player.sendMessage(plugin.color("&aSet nearest housing NPC command to &e/" + command + "&a."));
            else player.sendMessage(plugin.color("&cNo housing NPC found nearby."));
            return;
        }
        if (args[1].equalsIgnoreCase("remove")) {
            if (plugin.getHousingNpcService().removeNearest(player)) player.sendMessage(plugin.color("&aRemoved nearest housing NPC."));
            else player.sendMessage(plugin.color("&cNo housing NPC found nearby."));
            return;
        }
        if (args[1].equalsIgnoreCase("reload")) {
            plugin.getHousingNpcService().reload();
            player.sendMessage(plugin.color("&aReloaded &e" + plugin.getHousingNpcService().size() + " &ahousing NPCs."));
            return;
        }
        player.sendMessage(plugin.color("&cUsage: /housing npc [create|rename|skin|command|remove|reload]"));
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.color("&cHousing Commands"));
        player.sendMessage(plugin.color("&c/housing home &7- Go to your house"));
        player.sendMessage(plugin.color("&c/housing invite <player> &7- Invite a player"));
        player.sendMessage(plugin.color("&c/housing visit <player> &7- Visit a house"));
        player.sendMessage(plugin.color("&c/housing join <player> &7- Join a public/invited house"));
        player.sendMessage(plugin.color("&c/housing kick"));
        player.sendMessage(plugin.color("&c/housing ban"));
        player.sendMessage(plugin.color("&c/housing unban"));
        player.sendMessage(plugin.color("&c/housing tp"));
        player.sendMessage(plugin.color("&c/housing group"));
        player.sendMessage(plugin.color("&c/housingtag <tag> &7- Set browse tag"));
        player.sendMessage(plugin.color("&c/groupname <group> <new name> &7- Rename a group"));
        player.sendMessage(plugin.color("&c/grouptag <group> <new tag> &7- Set group tag"));
        player.sendMessage(plugin.color("&c/fly toggle &7- Toggle house flight"));
        player.sendMessage(plugin.color("&c/housing name"));
        if (player.hasPermission("housing.admin") || player.hasPermission("housing.staff.delete")) player.sendMessage(plugin.color("&c/housing delete <player>"));
        player.sendMessage(plugin.color("&c/housing set"));
        player.sendMessage(plugin.color("&c/housing kickall"));
        player.sendMessage(plugin.color("&c/housing kickguests"));
        player.sendMessage(plugin.color("&c/housing day"));
        player.sendMessage(plugin.color("&c/housing night"));
        player.sendMessage(plugin.color("&c/housing mute"));
        player.sendMessage(plugin.color("&c/housing unmute"));
        player.sendMessage(plugin.color("&c/housing walls"));
        player.sendMessage(plugin.color("&c/housing undo"));
        player.sendMessage(plugin.color("&c/housing lobby"));
        player.sendMessage(plugin.color("&c/housing pos1 | pos2"));
        player.sendMessage(plugin.color("&c/housing gma, gmc, gms."));
        player.sendMessage(plugin.color("&c/housing wand - Build tools"));
        if (player.hasPermission("housing.admin")) player.sendMessage(plugin.color("&c/housing npc - Create an player NPC"));
    }

    private void sendToStaticWorld(Player player, String worldName) {
        if (worldName.equalsIgnoreCase(plugin.getStaticConfig().getString("settings.lobby-world", "housinglobby"))) {
            houses.teleportToLobby(player, false);
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) { player.sendMessage(plugin.color("&cWorld &e" + worldName + " &cis not loaded.")); return; }
        player.teleport(world.getSpawnLocation().clone().add(0.5, 1.0, 0.5));
    }

    private String joinFrom(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) { if (builder.length() > 0) builder.append(' '); builder.append(args[i]); }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase();
        if (!commandName.equals("housing") && !commandName.equals("house") && !commandName.equals("h")) {
            if (commandName.equals("housingtag") && args.length == 1) return partial(args[0], Arrays.asList("roleplay", "parkour", "building", "chatting", "maze", "competition", "event", "guild-hangout", "minigame", "new-houses"));
            if ((commandName.equals("visitingrule") || commandName.equals("visitingrules")) && args.length == 1) return partial(args[0], Arrays.asList("public", "friends", "party", "guild", "private"));
            if ((commandName.equals("groupname") || commandName.equals("grouptag")) && args.length == 1) return partial(args[0], Arrays.asList("co-owner", "resident", "guest"));
            if (commandName.equals("fly") && args.length == 1) return partial(args[0], Arrays.asList("toggle", "on", "off"));
            if (commandName.equals("time") && args.length == 1) return partial(args[0], Arrays.asList("day", "night", "noon", "midnight", "sunrise", "sunset"));
            if ((commandName.equals("tp") || commandName.equals("tphere")) && args.length <= 2) {
                List<String> names = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
                return partial(args.length == 0 ? "" : args[args.length - 1], names);
            }
            if ((commandName.equals("set") || commandName.equals("walls")) && args.length == 1) {
                return partial(args[0], Arrays.asList("stone", "grass_block", "oak_planks", "glass", "air"));
            }
            return Collections.emptyList();
        }
        if (args.length == 1) return partial(args[0], Arrays.asList("home", "browse", "random", "list", "invite", "visit", "join", "kick", "ban", "unban", "mute", "unmute", "tp", "group", "tag", "housingtag", "visitingrule", "groupname", "grouptag", "fly", "time", "setspawn", "setguestspawn", "name", "reset", "delete", "kickall", "kickguests", "day", "night", "gmc", "gms", "gma", "wand", "set", "pos1", "pos2", "walls", "undo", "lobby", "main", "manage", "groups", "carpenter", "toggleredborder", "staff", "npc", "region", "reload"));
        if (args.length >= 2 && args[0].equalsIgnoreCase("region") && plugin.getRegionService() != null && sender instanceof Player) return plugin.getRegionService().tab((Player) sender, args);
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) return partial(args[1], houseOwnerNames());
        if (args.length == 2 && args[0].equalsIgnoreCase("npc")) return partial(args[1], Arrays.asList("create", "rename", "skin", "command", "remove", "reload"));
        if (args.length == 2 && args[0].equalsIgnoreCase("time")) return partial(args[1], Arrays.asList("day", "night", "noon", "midnight", "sunrise", "sunset"));
        if (args.length == 3 && args[0].equalsIgnoreCase("npc") && args[1].equalsIgnoreCase("command")) return partial(args[2], Collections.singletonList("add"));
        if (args.length == 2 && (args[0].equalsIgnoreCase("tag") || args[0].equalsIgnoreCase("housingtag") || args[0].equalsIgnoreCase("housetag"))) return partial(args[1], Arrays.asList("roleplay", "parkour", "building", "chatting", "maze", "competition", "event", "guild-hangout", "minigame", "new-houses"));
        if (args.length == 2 && (args[0].equalsIgnoreCase("groupname") || args[0].equalsIgnoreCase("grouptag"))) return partial(args[1], Arrays.asList("co-owner", "resident", "guest"));
        if (args.length == 3 && args[0].equalsIgnoreCase("group")) return partial(args[2], Arrays.asList("co-owner", "resident", "guest"));
        return Collections.emptyList();
    }

    private List<String> partial(String token, List<String> values) {
        List<String> out = new ArrayList<>();
        for (String value : values) if (value.toLowerCase().startsWith(token.toLowerCase())) out.add(value);
        return out;
    }

    private List<String> houseOwnerNames() {
        List<String> names = new ArrayList<>();
        for (House house : houses.getHouses()) names.add(house.getOwnerName());
        return names;
    }
}
