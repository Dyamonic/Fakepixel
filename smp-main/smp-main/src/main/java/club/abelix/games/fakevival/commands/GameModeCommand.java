package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameModeCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public GameModeCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.command.gamemode")) return CommandUtil.noPermission(plugin, sender);

        Player target = CommandUtil.getTarget(sender, args.length > 0 ? args[0] : null);
        if (target == null) return true;

        GameMode mode = getMode(command.getName().toLowerCase());
        target.setGameMode(mode);
        target.sendMessage(CC.color("&aYour gamemode was set to &f" + mode.name() + "&a."));
        if (sender != target) sender.sendMessage(CC.color("&aSet " + target.getName() + " to " + mode.name() + "."));
        return true;
    }

    private GameMode getMode(String command) {
        if (command.equals("gmc")) return GameMode.CREATIVE;
        if (command.equals("gmsp")) return GameMode.SPECTATOR;
        if (command.equals("gma")) return GameMode.ADVENTURE;
        return GameMode.SURVIVAL;
    }
}
