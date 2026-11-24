package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.LootBoxManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LootBoxCommand implements CommandExecutor {
    private final LootBoxManager lootBoxManager;

    public LootBoxCommand(LootBoxManager lootBoxManager) {
        this.lootBoxManager = lootBoxManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(CC.color("&e/lootbox give <player> <weapon|gear|item> [amount]"));
            return true;
        }
        if (!sender.hasPermission("fakevival.admin.lootbox")) {
            sender.sendMessage(CC.color("&cYou do not have permission to use this command."));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(CC.color("&cThat player is not online."));
            return true;
        }
        int amount = 1;
        if (args.length >= 4) {
            try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) {}
        }
        if (!lootBoxManager.give(target, args[2], amount)) {
            sender.sendMessage(CC.color("&cUnknown loot box."));
            return true;
        }
        sender.sendMessage(CC.color("&aGave &f" + amount + "x " + args[2] + " &aloot box to &f" + target.getName() + "&a."));
        return true;
    }
}
