package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.DupeDetectionManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class RenameCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final DupeDetectionManager dupeDetectionManager;

    public RenameCommand(JavaPlugin plugin, DupeDetectionManager dupeDetectionManager) {
        this.plugin = plugin;
        this.dupeDetectionManager = dupeDetectionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.rename")) return CommandUtil.noPermission(plugin, sender);
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage(CC.color("&cHold an item to rename."));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(CC.color("&cUsage: /" + label + " <name|clear>"));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            player.sendMessage(CC.color("&cThat item cannot be renamed."));
            return true;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            meta.setDisplayName(null);
            item.setItemMeta(meta);
            player.sendMessage(CC.color("&aCleared the item name."));
            return true;
        }
        String name = join(args);
        if (dupeDetectionManager.isProtectedMenuName(name)) {
            ItemStack renamed = item.clone();
            ItemMeta renamedMeta = renamed.getItemMeta();
            if (renamedMeta != null) {
                renamedMeta.setDisplayName(CC.color(name));
                renamed.setItemMeta(renamedMeta);
                if (dupeDetectionManager.isFakeMenuChest(renamed)) {
                    player.sendMessage(CC.color("&cYou cannot rename containers to protected menu names."));
                    return true;
                }
            }
        }
        meta.setDisplayName(CC.color(name));
        item.setItemMeta(meta);
        player.sendMessage(CC.color("&aRenamed your held item."));
        return true;
    }

    private String join(String[] args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(arg);
        }
        return builder.toString();
    }
}
