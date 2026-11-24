package club.abelix.games.buildffa.game.manager.progression;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CosmeticManager extends YListener {
    public static final String UNLOCK_ALL_PERMISSION = BuildFFAGame.UNLOCK_ALL_COSMETICS_PERMISSION;
    private final BuildFFAGame game;

    public CosmeticManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new CosmeticHolder(), 54, color("&8BuildFFA Cosmetics"));
        ItemStack filler = named(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        inv.setItem(20, named(Material.IRON_SWORD, (short) 0, "&bKill Effects", lore(
                "&7Select particle effects that play",
                "&7when you kill a player.",
                "",
                "&eClick To Open",
                "&8cosmetic:killeffects"
        )));


        inv.setItem(22, named(Material.PAPER, (short) 0, "&aChat Messages", lore(
                "&7Select custom messages shown",
                "&7when you kill a player.",
                "",
                "&eClick To Open",
                "&8cosmetic:chatmessages"
        )));

        inv.setItem(24, named(Material.GREEN_RECORD, (short) 0, "&dKill Sounds", lore(
                "&7Select sound effects that play",
                "&7when you kill a player.",
                "",
                "&eClick To Open",
                "&8cosmetic:killsounds"
        )));

        inv.setItem(49, named(Material.BARRIER, (short) 0, "&cClose", lore("&7Click to close this menu.", "&8cosmetic:close")));
        player.openInventory(inv);
    }

    public boolean canUse(Player player, boolean owned) {
        return owned || game.hasPermission(player, UNLOCK_ALL_PERMISSION);
    }

    public List<String> availabilityLore(Player player, boolean selected) {
        Gamer gamer = Gamer.getGamer(player);
        String key = selected ? "buildffa_cosmetic_selected" : "buildffa_cosmetic_click_select";
        return Collections.singletonList(color(gamer.getLanguage().getMessage(key)));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof CosmeticHolder)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return;

        String action = readAction(item);
        if (action.equals("killeffects")) {
            game.getKillEffectManager().open(player);
            return;
        }
        if (action.equals("killsounds")) {
            game.getKillSoundManager().open(player);
            return;
        }
        if (action.equals("chatmessages")) {
            game.getChatMessageManager().open(player);
            return;
        }
        if (action.equals("close")) player.closeInventory();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CosmeticHolder) event.setCancelled(true);
    }

    private String readAction(ItemStack item) {
        for (String line : item.getItemMeta().getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped.startsWith("cosmetic:")) return stripped.substring("cosmetic:".length());
        }
        return "";
    }

    private List<String> lore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) lore.add(color(line));
        return lore;
    }

    private ItemStack named(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static class CosmeticHolder implements InventoryHolder {
        public Inventory getInventory() { return null; }
    }
}
