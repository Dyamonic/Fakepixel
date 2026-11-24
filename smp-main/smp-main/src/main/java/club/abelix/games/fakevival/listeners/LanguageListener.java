package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.commands.LanguageCommand;
import club.abelix.games.fakevival.managers.LanguageManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class LanguageListener implements Listener {
    private final LanguageManager languageManager;
    private final LanguageCommand languageCommand;

    public LanguageListener(LanguageManager languageManager, LanguageCommand languageCommand) {
        this.languageManager = languageManager;
        this.languageCommand = languageCommand;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equalsIgnoreCase("languages")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getItemMeta() == null || clicked.getItemMeta().getLore() == null) return;
        for (String line : clicked.getItemMeta().getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (!stripped.startsWith("Language: ")) continue;
            String language = stripped.substring("Language: ".length()).trim();
            if (!languageManager.isLanguage(language)) return;
            languageManager.setLanguage(player, language);
            player.sendMessage(languageManager.message(player, "selected").replace("%language%", languageManager.displayName(language)));
            languageCommand.openMenu(player);
            return;
        }
    }
}
