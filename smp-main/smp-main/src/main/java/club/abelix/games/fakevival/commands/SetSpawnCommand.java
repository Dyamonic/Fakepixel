package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.SpawnManager;
import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SetSpawnCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final SpawnManager spawnManager;

    public SetSpawnCommand(JavaPlugin plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return CommandUtil.playersOnly(sender);
        if (!sender.hasPermission("fakevival.command.setspawn")) return CommandUtil.noPermission(plugin, sender);

        Player player=(Player)sender;
        spawnManager.setSpawn(player.getLocation());
        if(plugin instanceof Game game && (game.getNetworkService().isHub()||game.getNetworkService().isOverworld()))
            game.getNetworkService().saveServerSpawn(player,game.getNetworkService().isHub()?"hub":"overworld",ignored->{});
        sender.sendMessage(CC.color("&aSpawn has been set."));
        return true;
    }
}
