package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.RegionManager;
import club.abelix.games.fakevival.managers.SpawnProtectionManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RegionCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final RegionManager regionManager;
    private final SpawnProtectionManager spawnProtectionManager;

    public RegionCommand(JavaPlugin plugin, RegionManager regionManager, SpawnProtectionManager spawnProtectionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.spawnProtectionManager = spawnProtectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.admin.region")) {
            sender.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.no-permission", "&cNo permission.")));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(CC.color("&e/region create <name>"));
            sender.sendMessage(CC.color("&e/region pos1 <name>"));
            sender.sendMessage(CC.color("&e/region pos2 <name>"));
            sender.sendMessage(CC.color("&e/region list"));
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(CC.color("&aRegions: &f" + regionManager.listRegions()));
            return true;
        }
        if (args[0].equalsIgnoreCase("create") && args.length >= 2) {
            if (sender instanceof Player player && spawnProtectionManager.isSpawnWorld(player.getLocation())) {
                sender.sendMessage(CC.color("&cYou cannot create land claims or regions in the spawn world."));
                return true;
            }
            regionManager.create(args[1]);
            sender.sendMessage(CC.color("&aCreated region &f" + args[1] + "&a."));
            return true;
        }
        if ((args[0].equalsIgnoreCase("pos1") || args[0].equalsIgnoreCase("pos2")) && args.length >= 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cOnly players can set positions."));
                return true;
            }
            if (spawnProtectionManager.isSpawnWorld(player.getLocation())) {
                sender.sendMessage(CC.color("&cYou cannot claim or define regions in the spawn world."));
                return true;
            }
            regionManager.setPos(args[1], args[0], player.getLocation());
            sender.sendMessage(CC.color("&aSet &f" + args[0].toLowerCase() + " &afor region &f" + args[1] + "&a."));
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /region create <name>, /region pos1 <name>, /region pos2 <name>"));
        return true;
    }
}
