package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlyCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public FlyCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.command.fly")) return CommandUtil.noPermission(plugin, sender);

        Player target = CommandUtil.getTarget(sender, args.length > 0 ? args[0] : null);
        if (target == null) return true;

        target.setAllowFlight(!target.getAllowFlight());
        if (!target.getAllowFlight()) target.setFlying(false);
        target.sendMessage(CC.color(target.getAllowFlight() ? "&aFly enabled." : "&cFly disabled."));
        if (sender != target) sender.sendMessage(CC.color("&aUpdated fly for " + target.getName() + "."));
        return true;
    }
}
