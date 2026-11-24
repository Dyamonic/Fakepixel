package net.fakepixel.housing.command;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.npc.HousingNpcService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NpcCommand implements CommandExecutor, TabCompleter {
    private final HousingPlugin plugin;

    public NpcCommand(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("housing.admin")) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        HousingNpcService npcs = plugin.getHousingNpcService();
        if (args.length == 0) {
            help(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("create")) {
            String name = namedValue(args, 1, plugin.getStaticConfig().getString("housing-npcs.default-name", "Housing"));
            npcs.create(player, name);
            return true;
        }
        if (sub.equals("skin")) {
            if (args.length < 2) {
                player.sendMessage(plugin.color("&cUsage: /npc skin <name>"));
                return true;
            }
            npcs.setSkinSelected(player, args[1]);
            return true;
        }
        if (sub.equals("select")) {
            if (args.length >= 2) {
                npcs.select(player, joinFrom(args, 1));
            } else {
                npcs.selectLookedAtOrNearest(player);
            }
            return true;
        }
        if (sub.equals("remove") || sub.equals("delete")) {
            boolean removed = args.length >= 2 ? npcs.remove(player, joinFrom(args, 1)) : npcs.removeSelected(player);
            player.sendMessage(plugin.color(removed ? "&aRemoved housing NPC." : "&cNo housing NPC found."));
            return true;
        }
        if (sub.equals("rename")) {
            if (args.length < 2) {
                player.sendMessage(plugin.color("&cUsage: /npc rename <name>"));
                return true;
            }
            if (npcs.renameSelected(player, namedValue(args, 1, ""))) player.sendMessage(plugin.color("&aRenamed selected housing NPC."));
            else player.sendMessage(plugin.color("&cNo housing NPC selected or nearby."));
            return true;
        }
        if (sub.equals("command")) {
            if (args.length < 3 || !args[1].equalsIgnoreCase("add")) {
                player.sendMessage(plugin.color("&cUsage: /npc command add <command>"));
                return true;
            }
            String npcCommand = joinFrom(args, 2);
            if (npcCommand.startsWith("/")) npcCommand = npcCommand.substring(1);
            if (npcs.setCommandSelected(player, npcCommand)) player.sendMessage(plugin.color("&aSet selected housing NPC command to &e/" + npcCommand + "&a."));
            else player.sendMessage(plugin.color("&cNo housing NPC selected or nearby."));
            return true;
        }
        if (sub.equals("tphere")) {
            if (args.length >= 2 ? npcs.teleport(player, joinFrom(args, 1), player.getLocation()) : npcs.teleportSelected(player, player.getLocation())) {
                player.sendMessage(plugin.color("&aTeleported housing NPC to you."));
            } else {
                player.sendMessage(plugin.color("&cNo housing NPC found."));
            }
            return true;
        }
        if (sub.equals("look")) {
            npcs.toggleLook(player);
            return true;
        }
        if (sub.equals("list")) {
            npcs.list(player);
            return true;
        }
        if (sub.equals("reload")) {
            npcs.reload();
            player.sendMessage(plugin.color("&aReloaded &e" + npcs.size() + " &ahousing NPCs."));
            return true;
        }

        help(player);
        return true;
    }

    private void help(Player player) {
        player.sendMessage(plugin.color("&cNPC Commands"));
        player.sendMessage(plugin.color("&c/npc create name <name>"));
        player.sendMessage(plugin.color("&c/npc skin <name>"));
        player.sendMessage(plugin.color("&c/npc select [name]"));
        player.sendMessage(plugin.color("&c/npc remove [name]"));
        player.sendMessage(plugin.color("&c/npc rename <name>"));
        player.sendMessage(plugin.color("&c/npc command add <command>"));
        player.sendMessage(plugin.color("&c/npc tphere [name]"));
        player.sendMessage(plugin.color("&c/npc look"));
        player.sendMessage(plugin.color("&c/npc list"));
    }

    private String namedValue(String[] args, int start, String fallback) {
        if (args.length > start && args[start].equalsIgnoreCase("name")) start++;
        String value = joinFrom(args, start);
        return value.isEmpty() ? fallback : value;
    }

    private String joinFrom(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return partial(args[0], Arrays.asList("create", "skin", "select", "remove", "delete", "rename", "command", "tphere", "look", "list", "reload"));
        if (args.length == 2 && args[0].equalsIgnoreCase("command")) return partial(args[1], Collections.singletonList("add"));
        if (args.length == 2 && (args[0].equalsIgnoreCase("select") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("tphere"))) {
            if (sender instanceof Player) return partial(args[1], plugin.getHousingNpcService().names((Player) sender));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("skin")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
            return partial(args[1], names);
        }
        return Collections.emptyList();
    }

    private List<String> partial(String token, List<String> values) {
        List<String> out = new ArrayList<>();
        for (String value : values) if (value.toLowerCase().startsWith(token.toLowerCase())) out.add(value);
        return out;
    }
}
