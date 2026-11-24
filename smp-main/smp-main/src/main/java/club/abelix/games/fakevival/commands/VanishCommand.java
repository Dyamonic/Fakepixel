package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.VanishManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class VanishCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final VanishManager vanishManager;

    public VanishCommand(JavaPlugin plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.command.vanish")) return CommandUtil.noPermission(plugin, sender);

        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("fakevival.command.vanish.others")) return CommandUtil.noPermission(plugin, sender);
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(CC.color("&cThat player is not online."));
                return true;
            }
        } else {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            target = player;
        }

        boolean vanished = vanishManager.toggle(target);
        target.sendMessage(CC.color(vanished ? "&aVanish enabled." : "&cVanish disabled."));
        if (!sender.equals(target)) {
            sender.sendMessage(CC.color("&aVanish " + (vanished ? "enabled" : "disabled") + " for &f" + target.getName() + "&a."));
        }
        return true;
    }
}
