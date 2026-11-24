package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.ZoneManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.entity.EntityType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZoneCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final ZoneManager zoneManager;

    public ZoneCommand(JavaPlugin plugin, ZoneManager zoneManager) {
        this.plugin = plugin;
        this.zoneManager = zoneManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin instanceof Game game && !game.getAccessGuard().requireHub(sender,"&cZone commands can only be used on SMP hub servers.")) return true;
        if (!sender.hasPermission("fakevival.admin.zone") && !sender.hasPermission("fakevival.zone.admin")) {
            sender.sendMessage(CC.color("&cYou do not have permission to use this command."));
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(CC.color("&aZones: &f" + zoneManager.listZones()));
            return true;
        }
        if (args[0].equalsIgnoreCase("visual") && args.length >= 2) {
            if (!(sender instanceof Player player) || !(plugin instanceof Game game)) { sender.sendMessage(CC.color("&cOnly players can use zone visuals.")); return true; }
            if(args[1].equalsIgnoreCase("off")){game.getAdminWorldEditManager().clearZoneVisual(player);sender.sendMessage(CC.color("&cZone visual disabled."));return true;}
            if(!zoneManager.exists(args[1])){sender.sendMessage(CC.color("&cUnknown zone: &e"+args[1]+"&c."));return true;}
            var selection=zoneManager.getAreaSelection(args[1]);
            if(selection==null){sender.sendMessage(CC.color("&cZone &e"+args[1]+" &chas no valid area."));return true;}
            game.getAdminWorldEditManager().showZoneVisual(player,args[1],selection);
            sender.sendMessage(CC.color("&aShowing zone visual for &e"+args[1]+"&a."));
            return true;
        }
        if (args[0].equalsIgnoreCase("bosses")) {
            sender.sendMessage(CC.color("&aBoss templates: &f" + String.join("&7, &f", zoneManager.getBossTemplateIds())));
            return true;
        }
        if (args[0].equalsIgnoreCase("info") && args.length >= 2) {
            sender.sendMessage(CC.color("&a" + args[1] + ": &f" + zoneManager.info(args[1])));
            return true;
        }
        if (args[0].equalsIgnoreCase("create") && args.length >= 3) {
            zoneManager.create(args[1], args[2]);
            sender.sendMessage(CC.color("&aCreated zone &f" + args[1] + " &aas type &f" + args[2] + "&a."));
            return true;
        }
        if ((args[0].equalsIgnoreCase("createfromselection") || args[0].equalsIgnoreCase("setselection") || args[0].equalsIgnoreCase("setarea")) && args.length >= 2) {
            if (!(sender instanceof Player player)) { sender.sendMessage(CC.color("&cOnly players can use WorldEdit selections.")); return true; }
            if (!(plugin instanceof Game game)) { sender.sendMessage(CC.color("&cAdmin WorldEdit is unavailable.")); return true; }
            boolean create=args[0].equalsIgnoreCase("createfromselection");
            if(!create&&!zoneManager.exists(args[1])){sender.sendMessage(CC.color("&cZone &e"+args[1]+" &cdoes not exist."));return true;}
            var selection=game.getAdminWorldEditManager().getSelection(player);
            if(selection!=null&&zoneManager.setAreaFromSelection(args[1],selection,create))sender.sendMessage(CC.color("&aSaved the &e"+(selection.type().name().equals("POLY")?"Poly":"Cuboid")+" &aselection as zone &e"+args[1]+"&a."));
            else if(selection!=null)sender.sendMessage(CC.color("&cCould not save that selection as a zone."));
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("addwarp")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cOnly players can set zone warp locations."));
                return true;
            }
            if (!zoneManager.exists(args[0])) {
                sender.sendMessage(CC.color("&cZone &e" + args[0] + " &cdoes not exist."));
                return true;
            }
            zoneManager.setWarp(args[0], player.getLocation());
            sender.sendMessage(CC.color("&aSet warp location for zone &e" + args[0] + "&a."));
            return true;
        }
        if (args[0].equalsIgnoreCase("type") && args.length >= 3) {
            zoneManager.setType(args[1], args[2]);
            sender.sendMessage(CC.color("&aSet zone &f" + args[1] + " &atype to &f" + args[2] + "&a."));
            return true;
        }
        if ((args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete")) && args.length >= 2) {
            zoneManager.remove(args[1]);
            sender.sendMessage(CC.color("&cRemoved zone &f" + args[1] + "&c."));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.color("&cOnly players can set zone positions or mob spawns."));
            return true;
        }
        if ((args[0].equalsIgnoreCase("pos1") || args[0].equalsIgnoreCase("pos2")) && args.length >= 2) {
            zoneManager.setPos(args[1], args[0], player.getLocation());
            sender.sendMessage(CC.color("&aSet &f" + args[0].toLowerCase() + " &afor zone &f" + args[1] + "&a."));
            return true;
        }
        if (args[0].equalsIgnoreCase("addspawn") && args.length >= 3) {
            EntityType type;
            try {
                type = EntityType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException ex) {
                int level = args.length >= 4 ? parseLevel(args[3]) : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-mob-level", 1);
                if (zoneManager.addRoleSpawn(args[1], args[2], level, player.getLocation())) {
                    sender.sendMessage(CC.color("&aAdded &f" + args[2].toUpperCase() + " &7Lv." + level + " &arole spawn to zone &f" + args[1] + "&a."));
                    return true;
                }
                sender.sendMessage(CC.color("&cInvalid mob type or role. Roles: grunt, guard, archer, assassin, mage, brute, healer, elite."));
                return true;
            }
            int level = args.length >= 4 ? parseLevel(args[3]) : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-mob-level", 1);
            zoneManager.addSpawn(args[1], type, level, player.getLocation());
            sender.sendMessage(CC.color("&aAdded &f" + type.name() + " &7Lv." + level + " &aspawn to zone &f" + args[1] + "&a."));
            return true;
        }
        if (args[0].equalsIgnoreCase("addspawnboss") && args.length >= 3) {
            EntityType type;
            try {
                type = EntityType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException ex) {
                int level = args.length >= 4 && isInt(args[3]) ? parseLevel(args[3]) : 0;
                int nameStart = args.length >= 4 && isInt(args[3]) ? 4 : 3;
                String bossName = args.length > nameStart ? String.join(" ", java.util.Arrays.copyOfRange(args, nameStart, args.length)) : "";
                if (zoneManager.addBossSpawnTemplate(args[1], args[2], level, bossName, player.getLocation())) {
                    sender.sendMessage(CC.color("&aAdded boss template &f" + args[2].toLowerCase() + " &ato zone &f" + args[1] + "&a."));
                    return true;
                }
                sender.sendMessage(CC.color("&cInvalid boss mob type or template. Use &f/zone bosses &cto list templates."));
                return true;
            }
            int level = args.length >= 4 && isInt(args[3]) ? parseLevel(args[3]) : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("zones.default-boss-level", 10);
            int nameStart = args.length >= 4 && isInt(args[3]) ? 4 : 3;
            String bossName = args.length > nameStart ? String.join(" ", java.util.Arrays.copyOfRange(args, nameStart, args.length)) : "&c" + args[1] + " Boss";
            zoneManager.addBossSpawn(args[1], type, level, bossName, player.getLocation());
            sender.sendMessage(CC.color("&aAdded boss &f" + type.name() + " &aspawn to zone &f" + args[1] + "&a."));
            return true;
        }
        sendUsage(sender);
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(CC.color("&e/zone create <name> <safe|combat|dungeon|mining|farming|fishing|pvp>"));
        sender.sendMessage(CC.color("&e/zone pos1 <name>"));
        sender.sendMessage(CC.color("&e/zone pos2 <name>"));
        sender.sendMessage(CC.color("&e/zone createfromselection <name>"));
        sender.sendMessage(CC.color("&e/zone setselection <name> &7(or setarea)"));
        sender.sendMessage(CC.color("&e/zone <name> addwarp"));
        sender.sendMessage(CC.color("&e/zone addspawn <name> <mob|role> [level]"));
        sender.sendMessage(CC.color("&e/zone addspawnboss <name> <mob|template> [level] [boss name]"));
        sender.sendMessage(CC.color("&e/zone bosses"));
        sender.sendMessage(CC.color("&e/zone type <name> <safe|combat|dungeon|mining|farming|fishing|pvp>"));
        sender.sendMessage(CC.color("&e/zone info <name>"));
        sender.sendMessage(CC.color("&e/zone delete <name>"));
        sender.sendMessage(CC.color("&e/zone list"));
        sender.sendMessage(CC.color("&e/zone visual <name|off>"));
    }

    private int parseLevel(String value) {
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
