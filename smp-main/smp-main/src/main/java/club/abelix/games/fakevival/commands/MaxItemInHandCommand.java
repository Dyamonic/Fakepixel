package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class MaxItemInHandCommand implements CommandExecutor {
    private final Game plugin;
    private final CustomItemManager customItems;

    public MaxItemInHandCommand(Game plugin, CustomItemManager customItems) {
        this.plugin = plugin;
        this.customItems = customItems;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!CommandUtil.requireDev(plugin, sender, "fakevival.dev.maxiteminhand")) return true;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType().isAir()) {
            player.sendMessage(CC.color("&cHold a custom item in your hand."));
            return true;
        }

        CustomItemManager.MaxItemResult result = customItems.maxCustomItem(player, held);
        if (result.status() == CustomItemManager.MaxItemStatus.DEVELOPER_NOTEBOOK) {
            player.sendMessage(CC.color("&cDeveloper's Notebook cannot be modified."));
            return true;
        }
        if (result.status() != CustomItemManager.MaxItemStatus.SUCCESS) {
            player.sendMessage(CC.color("&cThis item cannot be maxed."));
            return true;
        }

        player.getInventory().setItemInMainHand(held);
        String itemName = displayName(held, result.itemId());
        player.sendMessage(CC.color("&aMaxed out &f" + itemName + "&a with the highest allowed stats and enchants."));
        plugin.getLogger().info("[SMP Admin] " + player.getName() + " used /maxiteminhand on " + result.itemId());
        return true;
    }

    private String displayName(ItemStack item, String id) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        if (id == null || id.isBlank()) return "Custom Item";
        StringBuilder output = new StringBuilder();
        for (String part : id.toLowerCase(Locale.ROOT).split("_")) {
            if (part.isBlank()) continue;
            if (!output.isEmpty()) output.append(' ');
            output.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return output.toString();
    }

}
