package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.mmo.MMOItemValueService;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TradeManager {
    private static final double MAX_COINS = 10_000_000.0D;
    private static final long REQUEST_TIMEOUT_MILLIS = 30_000L;
    private static final int[] LEFT_ITEM_SLOTS = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39};
    private static final int[] RIGHT_ITEM_SLOTS = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44};

    private final JavaPlugin plugin;
    private final EconomyManager economyManager;
    private final EconomyAuditManager auditManager;
    private final DiscordWebhookManager discordWebhookManager;
    private final SocialSpyManager socialSpyManager;
    private final MMOItemValueService itemValueService;
    private SmpAlertService alertService;
    private final Map<String, TradeSession> sessions = new HashMap<>();
    private final Map<UUID, String> playerSessions = new HashMap<>();
    private final Map<UUID, TradeRequest> incomingRequests = new HashMap<>();
    private final Map<UUID, Long> requestCooldowns = new HashMap<>();

    public TradeManager(JavaPlugin plugin, EconomyManager economyManager, EconomyAuditManager auditManager, DiscordWebhookManager discordWebhookManager, SocialSpyManager socialSpyManager, MMOItemValueService itemValueService) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.auditManager = auditManager;
        this.discordWebhookManager = discordWebhookManager;
        this.socialSpyManager = socialSpyManager;
        this.itemValueService = itemValueService;
    }

    public void setAlertService(SmpAlertService alertService) { this.alertService = alertService; }

    public boolean requestTrade(Player requester, Player target) {
        if (requester == null || target == null || requester.equals(target)) return false;
        if (economyManager.isEcoFrozen(requester) || economyManager.isEcoFrozen(target)) return false;
        if (playerSessions.containsKey(requester.getUniqueId()) || playerSessions.containsKey(target.getUniqueId())) return false;
        long now = System.currentTimeMillis();
        long availableAt = requestCooldowns.getOrDefault(requester.getUniqueId(), 0L);
        if (availableAt > now) {
            long seconds = Math.max(1L, (availableAt - now + 999L) / 1000L);
            requester.sendMessage(CC.color("&cWait &f" + seconds + "s &cbefore sending another trade request."));
            return true;
        }

        TradeRequest existing = incomingRequests.get(requester.getUniqueId());
        if (existing != null && existing.requester.equals(target.getUniqueId()) && !existing.isExpired()) {
            incomingRequests.remove(requester.getUniqueId());
            return startTrade(target, requester);
        }

        incomingRequests.put(target.getUniqueId(), new TradeRequest(requester.getUniqueId(), System.currentTimeMillis() + REQUEST_TIMEOUT_MILLIS));
        requestCooldowns.put(requester.getUniqueId(), now + tradeRequestCooldownMillis());
        requester.sendMessage(CC.color("&aTrade request sent to &f" + target.getName() + "&a."));
        target.sendMessage(CC.color("&e" + requester.getName() + " &fwants to trade. &eShift-right-click them &fto accept."));
        return true;
    }

    private long tradeRequestCooldownMillis() {
        return Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("trading.request-cooldown-seconds", 5L)) * 1000L;
    }

    public boolean startTrade(Player left, Player right) {
        if (left == null || right == null || left.equals(right)) return false;
        if (economyManager.isEcoFrozen(left) || economyManager.isEcoFrozen(right)) return false;
        if (playerSessions.containsKey(left.getUniqueId()) || playerSessions.containsKey(right.getUniqueId())) return false;
        String id = UUID.randomUUID().toString();
        Inventory inventory = Bukkit.createInventory(new MenuHolder("trade:" + id), 54, CC.color("&8Trade: " + left.getName() + " <-> " + right.getName()));
        TradeSession session = new TradeSession(id, left.getUniqueId(), right.getUniqueId(), left.getName(), right.getName(), inventory);
        sessions.put(id, session);
        playerSessions.put(left.getUniqueId(), id);
        playerSessions.put(right.getUniqueId(), id);
        draw(session);
        left.openInventory(inventory);
        right.openInventory(inventory);
        left.sendMessage(CC.color("&aTrade started with &f" + right.getName() + "&a."));
        right.sendMessage(CC.color("&aTrade started with &f" + left.getName() + "&a."));
        socialSpyManager.spy("&8[Spy Alert] &eTrade started: &f" + left.getName() + " &7<-> &f" + right.getName());
        return true;
    }

    public void handleClick(InventoryClickEvent event, String holderType) {
        TradeSession session = session(holderType);
        if (session == null || !(event.getWhoClicked() instanceof Player player)) return;
        int raw = event.getRawSlot();
        if (raw >= event.getView().getTopInventory().getSize()) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                moveShiftClickedItem(session, player, event);
            }
            return;
        }

        if (isItemSlot(session, player, raw)) {
            session.leftAccepted = false;
            session.rightAccepted = false;
            Bukkit.getScheduler().runTask(plugin, () -> draw(session));
            return;
        }

        event.setCancelled(true);
        if (raw == 49) {
            cancel(session, "&cTrade cancelled.");
            return;
        }
        if (isAcceptSlot(session, player, raw)) {
            if (session.left.equals(player.getUniqueId())) session.leftAccepted = !session.leftAccepted;
            if (session.right.equals(player.getUniqueId())) session.rightAccepted = !session.rightAccepted;
            draw(session);
            if (session.leftAccepted && session.rightAccepted) complete(session);
            return;
        }
        double coinAmount = coinButton(session, player, raw);
        if (coinAmount > 0) {
            addCoins(session, player, event.getClick(), coinAmount);
            draw(session);
        }
    }

    public void handleDrag(InventoryDragEvent event, String holderType) {
        TradeSession session = session(holderType);
        if (session == null || !(event.getWhoClicked() instanceof Player player)) return;
        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && !isItemSlot(session, player, slot)) {
                event.setCancelled(true);
                return;
            }
        }
        session.leftAccepted = false;
        session.rightAccepted = false;
        Bukkit.getScheduler().runTask(plugin, () -> draw(session));
    }

    public void handleClose(Player player, String holderType) {
        TradeSession session = session(holderType);
        if (session == null || session.finishing) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!session.finishing) cancel(session, "&cTrade cancelled.");
        });
    }

    private void addCoins(TradeSession session, Player player, ClickType click, double amount) {
        boolean left = session.left.equals(player.getUniqueId());
        double current = left ? session.leftCoins : session.rightCoins;
        double next = click.isRightClick() ? current - amount : current + amount;
        next = Math.clamp(next, 0.0D, Math.min(MAX_COINS, economyManager.getCoins(player)));
        if (left) session.leftCoins = next;
        else session.rightCoins = next;
        session.leftAccepted = false;
        session.rightAccepted = false;
    }

    private void moveShiftClickedItem(TradeSession session, Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemStack remaining = clicked.clone();
        int[] slots = session.left.equals(player.getUniqueId()) ? LEFT_ITEM_SLOTS : RIGHT_ITEM_SLOTS;
        for (int slot : slots) {
            ItemStack tradeItem = session.inventory.getItem(slot);
            if (tradeItem == null || tradeItem.getType().isAir()) {
                session.inventory.setItem(slot, remaining);
                event.setCurrentItem(null);
                resetAccepted(session);
                draw(session);
                return;
            }
            if (tradeItem.isSimilar(remaining) && tradeItem.getAmount() < tradeItem.getMaxStackSize()) {
                int move = Math.min(remaining.getAmount(), tradeItem.getMaxStackSize() - tradeItem.getAmount());
                tradeItem.setAmount(tradeItem.getAmount() + move);
                remaining.setAmount(remaining.getAmount() - move);
                if (remaining.getAmount() <= 0) {
                    event.setCurrentItem(null);
                    resetAccepted(session);
                    draw(session);
                    return;
                }
            }
        }
        event.setCurrentItem(remaining);
        resetAccepted(session);
        draw(session);
    }

    private void resetAccepted(TradeSession session) {
        session.leftAccepted = false;
        session.rightAccepted = false;
    }

    private void complete(TradeSession session) {
        Player left = Bukkit.getPlayer(session.left);
        Player right = Bukkit.getPlayer(session.right);
        if (left == null || right == null) {
            cancel(session, "&cTrade cancelled because a player went offline.");
            return;
        }
        if (economyManager.isEcoFrozen(left) || economyManager.isEcoFrozen(right)) {
            cancel(session, "&cTrade cancelled. A player's economy is frozen.");
            return;
        }
        if (economyManager.getCoins(left) < session.leftCoins || economyManager.getCoins(right) < session.rightCoins) {
            session.leftAccepted = false;
            session.rightAccepted = false;
            left.sendMessage(CC.color("&cTrade failed. One player no longer has enough coins."));
            right.sendMessage(CC.color("&cTrade failed. One player no longer has enough coins."));
            draw(session);
            return;
        }

        List<ItemStack> leftItems = takeItems(session.inventory, LEFT_ITEM_SLOTS);
        List<ItemStack> rightItems = takeItems(session.inventory, RIGHT_ITEM_SLOTS);
        if (session.leftCoins > 0) {
            economyManager.takeCoins(left.getUniqueId(), session.leftCoins, "trade " + session.id + " to " + session.rightName);
            economyManager.giveCoins(right.getUniqueId(), session.leftCoins, "trade " + session.id + " from " + session.leftName);
        }
        if (session.rightCoins > 0) {
            economyManager.takeCoins(right.getUniqueId(), session.rightCoins, "trade " + session.id + " to " + session.leftName);
            economyManager.giveCoins(left.getUniqueId(), session.rightCoins, "trade " + session.id + " from " + session.rightName);
        }
        giveItems(left, rightItems);
        giveItems(right, leftItems);
        if (auditManager != null) {
            auditManager.logTrade(session.id, session.left, session.leftName, session.right, session.rightName, leftItems, rightItems, session.leftCoins, session.rightCoins);
        }

        String leftSummary = summarize(leftItems, session.leftCoins);
        String rightSummary = summarize(rightItems, session.rightCoins);
        discordWebhookManager.sendTradeLog("Player trade completed",
                "**" + session.leftName + " gave:** " + leftSummary + "\n"
                        + "**" + session.rightName + " gave:** " + rightSummary);
        socialSpyManager.spy("&8[Spy Alert] &aTrade completed: &f" + session.leftName + " &7gave &f" + leftSummary + " &7| &f" + session.rightName + " &7gave &f" + rightSummary);

        double exchangedItemValue = estimatedValue(leftItems) + estimatedValue(rightItems);
        alertTradeSide(session.id, left, right, session.leftCoins, leftSummary, rightSummary, !leftItems.isEmpty() || !rightItems.isEmpty(), exchangedItemValue);
        alertTradeSide(session.id, right, left, session.rightCoins, rightSummary, leftSummary, !leftItems.isEmpty() || !rightItems.isEmpty(), exchangedItemValue);

        session.finishing = true;
        cleanup(session);
        left.closeInventory();
        right.closeInventory();
        left.sendMessage(CC.color("&aTrade completed."));
        right.sendMessage(CC.color("&aTrade completed."));
    }

    private void alertTradeSide(String tradeId, Player sender, Player receiver, double coins, String sent, String received, boolean hasItems, double itemValue) {
        if (alertService == null || coins <= 0D) return;
        boolean oneSided = received.equalsIgnoreCase("Nothing") || received.isBlank();
        boolean highValueItemsAndCoins = hasItems && itemValue >= alertService.threshold(SmpAlertService.Type.TRADE_COINS);
        if (coins < alertService.threshold(SmpAlertService.Type.TRADE_COINS) && !(hasItems && oneSided) && !highValueItemsAndCoins) return;
        alertService.alert(SmpAlertService.Type.TRADE_COINS, sender.getUniqueId(),
                "&c[SMP ALERT] &e" + sender.getName() + " &7traded &6" + economyManager.format(coins) + " coins &7to &e" + receiver.getName() + "&7.",
                "**" + sender.getName() + "** traded **" + economyManager.format(coins) + " coins** to **" + receiver.getName() + "**. Server: `" + alertService.serverId() + "`",
                "sender=" + sender.getName() + "/" + sender.getUniqueId() + " receiver=" + receiver.getName() + "/" + receiver.getUniqueId() + " amount=" + coins + " trade=" + tradeId + " sent=" + sent + " received=" + received + " itemValue=" + itemValue + " oneSided=" + oneSided + " server=" + alertService.serverId() + " at=" + System.currentTimeMillis());
    }

    private double estimatedValue(List<ItemStack> items) {
        if (itemValueService == null) return 0D;
        double total = 0D;
        for (ItemStack item : items) total += Math.max(0D, itemValueService.calculateValue(item));
        return total;
    }

    private void cancel(TradeSession session, String message) {
        if (session.finishing) return;
        session.finishing = true;
        Player left = Bukkit.getPlayer(session.left);
        Player right = Bukkit.getPlayer(session.right);
        giveItems(left, takeItems(session.inventory, LEFT_ITEM_SLOTS));
        giveItems(right, takeItems(session.inventory, RIGHT_ITEM_SLOTS));
        cleanup(session);
        if (left != null) {
            left.closeInventory();
            left.sendMessage(CC.color(message));
        }
        if (right != null) {
            right.closeInventory();
            right.sendMessage(CC.color(message));
        }
    }

    private void cleanup(TradeSession session) {
        sessions.remove(session.id);
        playerSessions.remove(session.left);
        playerSessions.remove(session.right);
        incomingRequests.remove(session.left);
        incomingRequests.remove(session.right);
    }

    private void draw(TradeSession session) {
        Inventory inventory = session.inventory;
        double leftItemsValue = itemValue(session.inventory, LEFT_ITEM_SLOTS);
        double rightItemsValue = itemValue(session.inventory, RIGHT_ITEM_SLOTS);
        double leftTotal = leftItemsValue + session.leftCoins;
        double rightTotal = rightItemsValue + session.rightCoins;
        for (int slot : new int[]{13, 31}) inventory.setItem(slot, pane());
        inventory.setItem(4, button(Material.GOLD_INGOT, "&e" + session.leftName + " Value", tradeValueLore(leftItemsValue, session.leftCoins, leftTotal, rightTotal)));
        inventory.setItem(22, button(Material.SCAFFOLDING, "&7Trade Balance", tradeBalanceLore(leftTotal, rightTotal)));
        inventory.setItem(40, button(Material.GOLD_INGOT, "&e" + session.rightName + " Value", tradeValueLore(rightItemsValue, session.rightCoins, rightTotal, leftTotal)));
        inventory.setItem(45, button(session.leftAccepted ? Material.LIME_DYE : Material.GRAY_DYE, session.leftAccepted ? "&aAccepted" : "&eAccept Trade", List.of("&7" + session.leftName, "&7Estimated Total: &6" + economyManager.format(leftTotal) + " coins")));
        inventory.setItem(46, button(Material.GOLD_NUGGET, "&eCoins +1k", List.of("&7Left click add.", "&7Right click remove.", "&7Offering: &f" + economyManager.format(session.leftCoins))));
        inventory.setItem(47, button(Material.GOLD_INGOT, "&eCoins +100k", List.of("&7Left click add.", "&7Right click remove.", "&7Offering: &f" + economyManager.format(session.leftCoins))));
        inventory.setItem(48, button(Material.GOLD_BLOCK, "&eCoins +1m", List.of("&7Left click add.", "&7Right click remove.", "&7Offering: &f" + economyManager.format(session.leftCoins))));
        inventory.setItem(49, button(Material.BARRIER, "&cCancel Trade", List.of("&7Click to cancel.")));
        inventory.setItem(50, button(Material.GOLD_BLOCK, "&eCoins +1m", List.of("&7Left click add.", "&7Right click remove.", "&7Offering: &f" + economyManager.format(session.rightCoins))));
        inventory.setItem(51, button(Material.GOLD_INGOT, "&eCoins +100k", List.of("&7Left click add.", "&7Right click remove.", "&7Offering: &f" + economyManager.format(session.rightCoins))));
        inventory.setItem(52, button(Material.GOLD_NUGGET, "&eCoins +1k", List.of("&7Left click add.", "&7Right click remove.", "&7Offering: &f" + economyManager.format(session.rightCoins))));
        inventory.setItem(53, button(session.rightAccepted ? Material.LIME_DYE : Material.GRAY_DYE, session.rightAccepted ? "&aAccepted" : "&eAccept Trade", List.of("&7" + session.rightName, "&7Estimated Total: &6" + economyManager.format(rightTotal) + " coins")));
    }

    private List<String> tradeValueLore(double itemValue, double coins, double total, double otherTotal) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Items: &6" + economyManager.format(itemValue) + " coins");
        lore.add("&7Coins: &e" + economyManager.format(coins));
        lore.add("&7Estimated Total: &6" + economyManager.format(total) + " coins");
        lore.add("");
        lore.add("&7Rolled Stats: &aSaved");
        lore.add("&7Requirements: &aSaved");
        if (isUneven(total, otherTotal)) {
            lore.add("");
            lore.add("&cWarning: This trade looks uneven.");
        }
        return lore;
    }

    private List<String> tradeBalanceLore(double leftTotal, double rightTotal) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Left Value: &6" + economyManager.format(leftTotal) + " coins");
        lore.add("&7Right Value: &6" + economyManager.format(rightTotal) + " coins");
        lore.add("");
        if (isUneven(leftTotal, rightTotal)) {
            lore.add("&cWarning: This trade looks uneven.");
        } else {
            lore.add("&aEstimated values are close.");
        }
        lore.add("&7Value is informational only.");
        return lore;
    }

    private boolean isUneven(double first, double second) {
        double high = Math.max(first, second);
        double low = Math.min(first, second);
        return high >= 1000.0D && high > Math.max(1.0D, low) * 2.0D;
    }

    private double itemValue(Inventory inventory, int[] slots) {
        if (itemValueService == null) return 0.0D;
        double total = 0.0D;
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            total += itemValueService.getTradeDisplayValue(item) * item.getAmount();
        }
        return total;
    }

    private List<ItemStack> takeItems(Inventory inventory, int[] slots) {
        List<ItemStack> items = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            items.add(item.clone());
            inventory.setItem(slot, null);
        }
        return items;
    }

    private void giveItems(Player player, List<ItemStack> items) {
        if (player == null) return;
        for (ItemStack item : items) player.getInventory().addItem(item).values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private boolean isItemSlot(TradeSession session, Player player, int slot) {
        return session.left.equals(player.getUniqueId()) ? contains(LEFT_ITEM_SLOTS, slot) : session.right.equals(player.getUniqueId()) && contains(RIGHT_ITEM_SLOTS, slot);
    }

    private boolean isAcceptSlot(TradeSession session, Player player, int slot) {
        return session.left.equals(player.getUniqueId()) && slot == 45 || session.right.equals(player.getUniqueId()) && slot == 53;
    }

    private double coinButton(TradeSession session, Player player, int slot) {
        if (session.left.equals(player.getUniqueId())) {
            return switch (slot) {
                case 46 -> 1_000.0D;
                case 47 -> 100_000.0D;
                case 48 -> 1_000_000.0D;
                default -> 0.0D;
            };
        }
        if (session.right.equals(player.getUniqueId())) {
            return switch (slot) {
                case 52 -> 1_000.0D;
                case 51 -> 100_000.0D;
                case 50 -> 1_000_000.0D;
                default -> 0.0D;
            };
        }
        return 0.0D;
    }

    private boolean contains(int[] values, int value) {
        for (int candidate : values) if (candidate == value) return true;
        return false;
    }

    private TradeSession session(String holderType) {
        if (holderType == null || !holderType.startsWith("trade:")) return null;
        return sessions.get(holderType.substring("trade:".length()));
    }

    private ItemStack pane() {
        return button(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(CC.color(line));
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String summarize(List<ItemStack> items, double coins) {
        List<String> parts = new ArrayList<>();
        if (coins > 0) parts.add(economyManager.format(coins) + " coins");
        for (ItemStack item : items) parts.add(item.getAmount() + "x " + itemName(item));
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    private String itemName(ItemStack item) {
        String material = pretty(item.getType().name());
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return material;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return name.isBlank() ? material : name + " (" + material + ")";
    }

    private String pretty(String value) {
        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private static final class TradeSession {
        private final String id;
        private final UUID left;
        private final UUID right;
        private final String leftName;
        private final String rightName;
        private final Inventory inventory;
        private double leftCoins;
        private double rightCoins;
        private boolean leftAccepted;
        private boolean rightAccepted;
        private boolean finishing;

        private TradeSession(String id, UUID left, UUID right, String leftName, String rightName, Inventory inventory) {
            this.id = id;
            this.left = left;
            this.right = right;
            this.leftName = leftName;
            this.rightName = rightName;
            this.inventory = inventory;
        }
    }

    private record TradeRequest(UUID requester, long expiresAtMillis) {
        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
