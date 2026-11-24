package club.abelix.games.fakevival.network;

import club.abelix.games.fakevival.Game;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class NetworkStatusCommand implements CommandExecutor {
    private final Game plugin;
    private final SMPNetworkService network;
    public NetworkStatusCommand(Game plugin, SMPNetworkService network) { this.plugin = plugin; this.network = network; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.proxy.admin")) { sender.sendMessage("§cYou do not have permission to view network status."); return true; }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<SMPNetworkRepository.ServerRecord> servers = network.repository().servers();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    print(sender, "Hub Servers:", servers, SMPServerType.HUB);
                    print(sender, "Overworld Servers:", servers, SMPServerType.OVERWORLD);
                    print(sender, "Dungeon Servers:", servers, SMPServerType.DUNGEON);
                });
            } catch (Exception e) { Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cCould not load network status.")); }
        });
        return true;
    }

    private void print(CommandSender sender, String heading, List<SMPNetworkRepository.ServerRecord> servers, SMPServerType type) {
        sender.sendMessage("§6" + heading);
        servers.stream().filter(s -> s.type() == type).forEach(s -> sender.sendMessage("§f" + s.serverId() + " §a" + s.status() + " §e" + s.onlinePlayers() + "/" + s.maxPlayers() + " §bTPS " + String.format("%.1f", s.tps()) + (type == SMPServerType.DUNGEON ? " §dactiveRuns=" + s.activeDungeonRuns() : "")));
    }
}
