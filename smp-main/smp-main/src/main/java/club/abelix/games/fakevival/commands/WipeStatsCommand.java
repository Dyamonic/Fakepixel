package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class WipeStatsCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final DataManager dataManager;

    public WipeStatsCommand(JavaPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.admin.wipestats")) {
            sender.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.no-permission", "&cNo permission.")));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(CC.color("&cUsage: /wipestats <player>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        String path = "players." + target.getUniqueId();
        dataManager.getData().set(path + ".skills", null);
        dataManager.getData().set(path + ".coins", null);
        dataManager.getData().set(path + ".jobs", null);
        dataManager.getData().set(path + ".jobs-joined", null);
        dataManager.getData().set(path + ".job", null);
        dataManager.getData().set(path + ".skill-points", null);
        dataManager.getData().set(path + ".skill-tree", null);
        dataManager.getData().set(path + ".battlepass", null);
        dataManager.getData().set(path + ".quests", null);
        dataManager.getData().set(path + ".regions", null);
        dataManager.getData().set(path + ".dungeon", null);
        dataManager.save();
        sender.sendMessage(CC.color("&aWiped Fakevival stats for &f" + args[0] + "&a. Abelix data was not touched."));
        return true;
    }
}
