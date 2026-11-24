package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.SpawnProtectionManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnProtectionCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final SpawnProtectionManager manager;

    public SpawnProtectionCommand(JavaPlugin plugin, SpawnProtectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.admin.spawnprotection")) return CommandUtil.noPermission(plugin, sender);
        if (args.length == 0) {
            sender.sendMessage(CC.color("&bSpawnProtection: " + manager.getStatus()));
            sender.sendMessage(CC.color("&7/spawnprotection pos1"));
            sender.sendMessage(CC.color("&7/spawnprotection pos2"));
            return true;
        }
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (args[0].equalsIgnoreCase("pos1")) {
            manager.setPos1(player.getLocation());
            sender.sendMessage(CC.color("&aSpawn protection position 1 set."));
            return true;
        }
        if (args[0].equalsIgnoreCase("pos2")) {
            manager.setPos2(player.getLocation());
            sender.sendMessage(CC.color("&aSpawn protection position 2 set."));
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /spawnprotection <pos1|pos2>"));
        return true;
    }
}
