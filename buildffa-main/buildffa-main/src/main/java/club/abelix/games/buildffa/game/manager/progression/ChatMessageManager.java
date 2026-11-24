package club.abelix.games.buildffa.game.manager.progression;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.purchase.Purchase;
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

public class ChatMessageManager extends YListener {
    private static final String SELECTED_MESSAGE_PROPERTY = "buildffa_selected_chat_message";
    private static final String OWNED_MESSAGE_PROPERTY_PREFIX = "buildffa_chat_message_owned_";
    private static final int[] COSMETIC_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final BuildFFAGame game;
    private final List<ChatMessage> messages = new ArrayList<>();

    public ChatMessageManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;

        messages.add(new ChatMessage(1, "&aDefault", Material.PAPER, "", 0, 0, true));
        messages.add(new ChatMessage(2, "&bCombo'd", Material.PAPER, "&b%killer% &7combo'd &c%victim%&7! &8(&e+%coins% coins, &b+%rating% rating&8)", 120000, 100, false));
        messages.add(new ChatMessage(3, "&eDeleted", Material.PAPER, "&e%killer% &7deleted &c%victim%&7! &8(&e+%coins% coins, &b+%rating% rating&8)", 150000, 125, false));
        messages.add(new ChatMessage(4, "&6Sent To Spawn", Material.PAPER, "&6%killer% &7sent &c%victim% &7back to spawn! &8(&e+%coins% coins, &b+%rating% rating&8)", 220000, 150, false));
        messages.add(new ChatMessage(5, "&cDestroyed", Material.PAPER, "&c%victim% &7was destroyed by &c%killer%&7! &8(&e+%coins% coins, &b+%rating% rating&8)", 250000, 175, false));
        messages.add(new ChatMessage(6, "&dOutplayed", Material.PAPER, "&d%killer% &7outplayed &c%victim%&7! &8(&e+%coins% coins, &b+%rating% rating&8)", 230000, 200, false));
        messages.add(new ChatMessage(7, "&5Obliterated", Material.PAPER, "&5%killer% &7obliterated &c%victim%&7! &8(&e+%coins% coins, &b+%rating% rating&8)", 250000, 225, false));
        messages.add(new ChatMessage(8, "&9Knocked", Material.PAPER, "&9%killer% &7knocked &c%victim% &7out of the fight! &8(&e+%coins% coins, &b+%rating% rating&8)", 140000, 250, false));
        messages.add(new ChatMessage(9, "&aCleaned", Material.PAPER, "&a%killer% &7cleaned up &c%victim%&7! &8(&e+%coins% coins, &b+%rating% rating&8)", 145000, 275, false));
        messages.add(new ChatMessage(10, "&3No Chance", Material.PAPER, "&3%victim% &7stood no chance against &3%killer%&7! &8(&e+%coins% coins, &b+%rating% rating&8)", 150000, 300, false));
        messages.add(new ChatMessage(11, "&4Final Hit", Material.PAPER, "&4%killer% &7landed the final hit on &c%victim%&7! &8(&e+%coins% coins, &b+%rating% rating&8)", 155000, 300, false));
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        int maxPage = Math.max(0, (messages.size() - 1) / COSMETIC_SLOTS.length);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        Inventory inv = Bukkit.createInventory(new ChatMessageHolder(page), 54, color("&8Chat Messages"));
        ItemStack filler = named(Material.STAINED_GLASS_PANE, (short) 7, " ", Collections.emptyList());
        ItemStack border = named(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());

        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9) inv.setItem(i, border);
        for (int i = 17; i < 45; i += 9) inv.setItem(i, border);

        int start = page * COSMETIC_SLOTS.length;
        for (int i = 0; i < COSMETIC_SLOTS.length; i++) {
            int index = start + i;
            if (index >= messages.size()) break;
            inv.setItem(COSMETIC_SLOTS[i], icon(player, messages.get(index)));
        }

        if (page > 0) {
            inv.setItem(46, named(Material.INK_SACK, (short) 8, "&7Previous Page", lore("&eClick to go back.", "&8chatmessage-page:" + (page - 1))));
        }
        inv.setItem(49, named(Material.BARRIER, (short) 0, "&cBack", lore("&7Return to BuildFFA Cosmetics.", "&8cosmetic-back")));
        if (page < maxPage) {
            inv.setItem(52, named(Material.INK_SACK, (short) 10, "&aNext Page", lore("&eClick to go forward.", "&8chatmessage-page:" + (page + 1))));
        }

        player.openInventory(inv);
    }

    private ItemStack icon(Player player, ChatMessage message) {
        boolean owned = isOwned(player, message);
        boolean canUse = game.getCosmeticManager().canUse(player, owned);
        boolean selected = getSelectedMessageId(player) == message.id;
        List<String> lore = new ArrayList<>();

        if (!canUse) {
            lore.add(color("&7Price: &e" + message.coinPrice + " coins &7or &6" + message.goldPrice + " gold"));
            lore.add(color("&eClick To Buy"));
        } else {
            lore.addAll(game.getCosmeticManager().availabilityLore(player, selected));
            lore.add(color("&7Changes the message you see"));
            lore.add(color("&7when you kill a player."));
        }

        if (!message.template.isEmpty()) {
            lore.add("");
            lore.add(color("&7Preview:"));
            lore.add(color(message.template
                    .replace("%killer%", player.getName())
                    .replace("%victim%", "Enemy")
                    .replace("%coins%", "35")
                    .replace("%rating%", "2")));
        }

        lore.add(color("&8chatmessage:" + message.id));
        return named(message.material, (short) 0, message.name, lore);
    }

    private boolean isOwned(Player player, ChatMessage message) {
        if (message.free) return true;
        return Gamer.getGamer(player).getProperty(OWNED_MESSAGE_PROPERTY_PREFIX + message.id) > 0;
    }

    private void setOwned(Player player, ChatMessage message) {
        Gamer.getGamer(player).setProperty(OWNED_MESSAGE_PROPERTY_PREFIX + message.id, 1);
    }

    public void resetForProgressionWipe(Gamer gamer) {
        gamer.setProperty(SELECTED_MESSAGE_PROPERTY, 1);
        for (ChatMessage message : messages) gamer.setProperty(OWNED_MESSAGE_PROPERTY_PREFIX + message.id, 0);
    }

    private int getSelectedMessageId(Player player) {
        int selected = Gamer.getGamer(player).getProperty(SELECTED_MESSAGE_PROPERTY);
        return selected <= 0 ? 1 : selected;
    }

    public String formatKillMessage(Gamer killer, Gamer victim, int coins, int rating, String fallback) {
        ChatMessage message = getMessage(getSelectedMessageId(killer.getPlayer()));
        return message == null || message.template.isEmpty()
                || !game.getCosmeticManager().canUse(killer.getPlayer(), isOwned(killer.getPlayer(), message))
                ? fallback
                : color(message.template
                        .replace("%killer%", killer.getDisplayName())
                        .replace("%victim%", victim.getDisplayName())
                        .replace("%coins%", String.valueOf(coins))
                        .replace("%rating%", String.valueOf(rating)));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof ChatMessageHolder)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return;

        if (isBack(item)) {
            game.getCosmeticManager().open(player);
            return;
        }

        int page = readId(item, "chatmessage-page:");
        if (page >= 0) {
            open(player, page);
            return;
        }

        int id = readId(item, "chatmessage:");
        if (id <= 0) return;

        ChatMessage message = getMessage(id);
        if (message == null) return;

        Gamer gamer = Gamer.getGamer(player);
        if (!game.getCosmeticManager().canUse(player, isOwned(player, message))) {
            Purchase.builder()
                    .onSuccess(() -> {
                        setOwned(player, message);
                        gamer.setProperty(SELECTED_MESSAGE_PROPERTY, message.id);
                    })
                    .purchaseAllowed(() -> !isOwned(player, message))
                    .coinPrice(message.coinPrice)
                    .goldPrice(message.goldPrice)
                    .player(player)
                    .build()
                    .execute();
            return;
        }

        gamer.setProperty(SELECTED_MESSAGE_PROPERTY, message.id);
        player.closeInventory();
        player.sendMessage(color("&aChat message selected."));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ChatMessageHolder) event.setCancelled(true);
    }

    private ChatMessage getMessage(int id) {
        for (ChatMessage message : messages) if (message.id == id) return message;
        return null;
    }

    private int readId(ItemStack item, String prefix) {
        for (String line : item.getItemMeta().getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (!stripped.startsWith(prefix)) continue;
            try { return Integer.parseInt(stripped.substring(prefix.length())); } catch (Exception ignored) { }
        }
        return -1;
    }

    private boolean isBack(ItemStack item) {
        for (String line : item.getItemMeta().getLore()) {
            if (ChatColor.stripColor(line).equals("cosmetic-back")) return true;
        }
        return false;
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

    private static class ChatMessageHolder implements InventoryHolder {
        private final int page;

        ChatMessageHolder(int page) {
            this.page = page;
        }

        public Inventory getInventory() { return null; }
    }

    private static class ChatMessage {
        final int id;
        final String name;
        final Material material;
        final String template;
        final int coinPrice;
        final int goldPrice;
        final boolean free;

        ChatMessage(int id, String name, Material material, String template, int coinPrice, int goldPrice, boolean free) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.template = template;
            this.coinPrice = coinPrice;
            this.goldPrice = goldPrice;
            this.free = free;
        }
    }
}
