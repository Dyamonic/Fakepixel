package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.HomeManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.SpawnProtectionManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class HomeCommands implements CommandExecutor {
    private final JavaPlugin plugin;
    private final HomeManager homeManager;
    private final SpawnProtectionManager spawnProtectionManager;

    public HomeCommands(JavaPlugin plugin, HomeManager homeManager, SpawnProtectionManager spawnProtectionManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.spawnProtectionManager = spawnProtectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!sender.hasPermission("fakevival.command.home")) return CommandUtil.noPermission(plugin, sender);

        if (plugin instanceof Game game) {
            String name=command.getName().toLowerCase();
            if (name.equals("sethome")) {
                if (!game.getNetworkService().isOverworld()) { player.sendMessage(CC.color("&cYou can only set homes on overworld servers.")); return true; }
            }
        }

        String name = args.length > 0 ? args[0] : "home";
        return switch (command.getName().toLowerCase()) {
            case "sethome" -> setHome(player, name);
            case "home" -> home(player, name);
            case "delhome" -> deleteHome(player, name);
            case "renamehome" -> renameHome(player, args);
            case "homes" -> listHomes(player);
            default -> true;
        };
    }

    private boolean setHome(Player player, String name) {
        if (spawnProtectionManager.isSpawnWorld(player.getLocation())) {
            player.sendMessage(CC.color("&cYou cannot set homes in the spawn world."));
            return true;
        }
        String normalized = homeManager.normalize(name);
        if (normalized.isBlank()) {
            player.sendMessage(CC.color("&cInvalid home name."));
            return true;
        }
        if (!homeManager.setHome(player, normalized, player.getLocation())) {
            player.sendMessage(CC.color("&cYou have reached your home limit of " + homeManager.getMaxHomes(player) + "."));
            return true;
        }
        player.sendMessage(CC.color("&aHome &f" + normalized + " &aset."));
        return true;
    }

    private boolean home(Player player, String name) {
        homeManager.resolveHomeAsync(player, name, home -> finishHome(player, name, home));
        return true;
    }

    private void finishHome(Player player, String name, HomeManager.HomeRecord home) {
        if (!player.isOnline()) return;
        if (home == null) {
            player.sendMessage(CC.color("&cCould not find your home."));
            return;
        }
        if (plugin instanceof Game game) {
            String current = game.getNetworkService().getCurrentServerId();
            if (!home.serverType().isBlank() && !home.isOverworld()) {
                player.sendMessage(CC.color("&cCould not find your home."));
                return;
            }
            if (!home.serverId().isBlank() && !home.serverId().equalsIgnoreCase(current)) {
                game.getNetworkService().routeHome(player, home);
                return;
            }
        }
        Location location = homeManager.getHome(player, name);
        if (location == null) { player.sendMessage(CC.color("&cYour home world is not loaded right now.")); return; }
        player.teleport(location);
        player.sendMessage(CC.color("&aTeleported to home &f" + homeManager.normalize(name) + "&a."));
    }

    private boolean deleteHome(Player player, String name) {
        if (!homeManager.deleteHome(player, name)) {
            player.sendMessage(CC.color("&cThat home does not exist."));
            return true;
        }
        player.sendMessage(CC.color("&aDeleted home &f" + homeManager.normalize(name) + "&a."));
        return true;
    }

    private boolean renameHome(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(CC.color("&cUsage: /renamehome <old> <new>"));
            return true;
        }
        String oldName = homeManager.normalize(args[0]);
        String newName = homeManager.normalize(args[1]);
        if (newName.isBlank()) {
            player.sendMessage(CC.color("&cInvalid home name."));
            return true;
        }
        if (!homeManager.renameHome(player, oldName, newName)) {
            player.sendMessage(CC.color("&cCould not rename that home. Check that the old home exists and the new name is unused."));
            return true;
        }
        player.sendMessage(CC.color("&aRenamed home &f" + oldName + " &ato &f" + newName + "&a."));
        return true;
    }

    private boolean listHomes(Player player) {
        List<String> homes = homeManager.getHomes(player);
        if (homes.isEmpty()) {
            player.sendMessage(CC.color("&cYou do not have any homes set."));
            return true;
        }
        player.sendMessage(CC.color("&aHomes &7(" + homes.size() + "/" + homeManager.getMaxHomes(player) + ")&a: &f" + String.join("&7, &f", homes)));
        return true;
    }
}
