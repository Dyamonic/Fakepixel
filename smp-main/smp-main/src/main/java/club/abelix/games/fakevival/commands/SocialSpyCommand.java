package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.SocialSpyManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SocialSpyCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final SocialSpyManager socialSpyManager;

    public SocialSpyCommand(JavaPlugin plugin, SocialSpyManager socialSpyManager) {
        this.plugin = plugin;
        this.socialSpyManager = socialSpyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.staff.socialspy")) return CommandUtil.noPermission(plugin, sender);
        boolean enabled = socialSpyManager.toggle(player);
        player.sendMessage(CC.color(enabled ? "&aSocialSpy enabled." : "&cSocialSpy disabled."));
        return true;
    }
}
