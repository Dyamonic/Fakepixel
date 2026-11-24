package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class HatCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public HatCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.hat")) return CommandUtil.noPermission(plugin, sender);

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            player.sendMessage(CC.color("&cHold a block to wear it as a hat."));
            return true;
        }
        if (!hand.getType().isBlock()) {
            player.sendMessage(CC.color("&cOnly blocks can be used with /hat."));
            return true;
        }

        ItemStack one = hand.clone();
        one.setAmount(1);
        ItemStack helmet = player.getInventory().getHelmet();
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) player.getInventory().setItemInMainHand(null);
        player.getInventory().setHelmet(one);
        if (helmet != null && !helmet.getType().isAir()) {
            player.getInventory().addItem(helmet).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        player.sendMessage(CC.color("&aApplied your hat."));
        return true;
    }
}
