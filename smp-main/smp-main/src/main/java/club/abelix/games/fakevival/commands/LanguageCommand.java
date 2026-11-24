package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.LanguageManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class LanguageCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final LanguageManager languageManager;

    public LanguageCommand(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can select a language.");
            return true;
        }
        if (args.length >= 1) {
            if (!languageManager.isLanguage(args[0])) {
                player.sendMessage(languageManager.message(player, "invalid-language"));
                return true;
            }
            languageManager.setLanguage(player, args[0]);
            player.sendMessage(languageManager.message(player, "selected").replace("%language%", languageManager.displayName(args[0])));
            return true;
        }
        openMenu(player);
        return true;
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("languages"), 27, languageManager.message(player, "menu-title"));
        int slot = 10;
        for (String language : languageManager.languages()) {
            inventory.setItem(slot++, item(player, language));
            if (slot == 17) slot = 19;
        }
        player.openInventory(inventory);
    }

    private ItemStack item(Player player, String language) {
        Material material = Material.matchMaterial(languageManager.material(language));
        if (material == null) material = Material.PAPER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languageManager.displayName(language));
            List<String> lore = new ArrayList<>(languageManager.lore(player, "language-lore"));
            String selected = languageManager.getLanguage(player).equalsIgnoreCase(language)
                    ? languageManager.message(player, "selected-lore")
                    : languageManager.message(player, "click-lore");
            lore.add(selected);
            lore.add(CC.color("&8Language: " + language.toLowerCase()));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
