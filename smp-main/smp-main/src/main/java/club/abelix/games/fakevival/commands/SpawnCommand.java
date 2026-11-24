package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.SpawnManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import club.abelix.games.fakevival.Game;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SpawnCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final SpawnManager spawnManager;

    public SpawnCommand(JavaPlugin plugin, SpawnManager spawnManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return CommandUtil.playersOnly(sender);
        if (!sender.hasPermission("fakevival.command.spawn")) return CommandUtil.noPermission(plugin, sender);
        Player player = (Player) sender;
        if (plugin instanceof Game game && !game.getNetworkService().isHub()) {
            if (game.getDataManager().isMySqlEnabled()) {
                player.sendMessage(CC.color("&aSending you to the SMP hub..."));
                game.getNetworkService().sendToHub(player, () -> {
                    player.sendMessage(CC.color("&cServer transfer is not available. Sending you to this server's spawn instead."));
                    if (spawnManager.getSpawn() != null && spawnManager.getSpawn().getWorld() != null) player.teleport(spawnManager.getSpawn());
                });
                return true;
            }
            player.sendMessage(CC.color("&cServer transfer is not available. Sending you to this server's spawn instead."));
        }
        if (spawnManager.getSpawn() == null || spawnManager.getSpawn().getWorld() == null) {
            sender.sendMessage(CC.color("&cSpawn has not been set yet."));
            return true;
        }

        player.teleport(spawnManager.getSpawn());
        sender.sendMessage(CC.color("&aTeleported to spawn."));
        return true;
    }
}
