package club.abelix.games.fakevival.menus;

import club.abelix.games.fakevival.managers.BankManager;
import club.abelix.games.fakevival.managers.BankTier;
import club.abelix.games.fakevival.managers.BankTransaction;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class BankMenu implements Listener {
    private static final String MAIN_TYPE = "bank";
    private static final String UPGRADE_TYPE = "bank:upgrades";
    private static final String HISTORY_TYPE = "bank:history";
    private static final String ACTION_PREFIX = "Bank Action: ";

    private final BankManager bankManager;
    private final EconomyManager economyManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.US);
    private final Set<UUID> loadingBankAccounts = ConcurrentHashMap.newKeySet();

    public BankMenu(JavaPlugin plugin, BankManager bankManager, EconomyManager economyManager) {
        this.bankManager = bankManager;
        this.economyManager = economyManager;
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        if (!beginLoading(player, "&eLoading bank data...")) return;
        bankManager.getAccountAsync(uuid, account -> {
            loadingBankAccounts.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || account == null) return;
            openLoaded(online, account);
        });
    }

    private void openLoaded(Player player, BankManager.BankAccount account) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(MAIN_TYPE), 54, CC.color("&6Bank"));
        fill(inventory);

        inventory.setItem(4, item(Material.GOLD_INGOT, "&6Bank Info", List.of(
                "&7Purse: &e" + economyManager.format(economyManager.getCoins(player)) + " coins",
                "&7Bank: &e" + economyManager.format(account.bankBalance()) + " coins",
                "&7Tier: &f" + account.tier().displayName(),
                "&7Capacity: &e" + economyManager.format(account.tier().capacity()) + " coins",
                "",
                bankManager.isEnabled() ? "&aMariaDB persistence online." : "&cMariaDB persistence required."
        ), null));

        inventory.setItem(10, item(Material.EMERALD_BLOCK, "&aDeposit All", List.of(
                "&7Move all purse coins into your bank.",
                "&7Bank capacity still applies."
        ), "deposit_all"));
        inventory.setItem(11, item(Material.EMERALD, "&aDeposit Half", List.of(
                "&7Move half of your purse coins into your bank."
        ), "deposit_half"));
        inventory.setItem(15, item(Material.CHEST, "&eWithdraw Half", List.of(
                "&7Move half of your bank coins into your purse."
        ), "withdraw_half"));
        inventory.setItem(16, item(Material.HOPPER, "&eWithdraw Amount", List.of(
                "&7Withdraw up to &e10,000 coins&7.",
                "&7Use this as a quick withdrawal amount."
        ), "withdraw_amount"));
        inventory.setItem(29, item(Material.ENDER_CHEST, "&bBank Upgrades", List.of(
                "&7Upgrade your bank capacity.",
                "&7Current: &f" + account.tier().displayName()
        ), "upgrades"));
        inventory.setItem(31, item(Material.SUNFLOWER, "&6Interest", List.of(
                "&7Claim daily capped interest.",
                "&7Rate: &f1% &8(capped by tier)",
                "&7Tier Cap: &e" + economyManager.format(account.tier().dailyInterestCap()) + " coins"
        ), "interest"));
        inventory.setItem(33, item(Material.PAPER, "&fTransaction History", List.of(
                "&7View recent deposits, withdrawals,",
                "&7interest claims, upgrades, and death losses."
        ), "history"));
        inventory.setItem(49, item(Material.BARRIER, "&cClose", List.of(), "close"));

        player.openInventory(inventory);
    }

    private void openUpgrades(Player player) {
        UUID uuid = player.getUniqueId();
        if (!beginLoading(player, "&eLoading bank upgrades...")) return;
        bankManager.getAccountAsync(uuid, account -> {
            loadingBankAccounts.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || account == null) return;
            openUpgradesLoaded(online, account);
        });
    }

    private void openUpgradesLoaded(Player player, BankManager.BankAccount account) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(UPGRADE_TYPE), 54, CC.color("&6Bank Upgrades"));
        fill(inventory);

        int[] slots = {10, 12, 14, 16, 28};
        BankTier[] tiers = BankTier.values();
        for (int i = 0; i < tiers.length && i < slots.length; i++) {
            BankTier tier = tiers[i];
            boolean current = account.tier() == tier;
            boolean unlocked = account.tier().ordinal() >= tier.ordinal();
            List<String> lore = new ArrayList<>();
            lore.add("&7Capacity: &e" + economyManager.format(tier.capacity()) + " coins");
            if (tier.upgradeCost() > 0D) lore.add("&7Cost: &e" + economyManager.format(tier.upgradeCost()) + " coins");
            lore.add("");
            lore.add(current ? "&aCurrent tier" : unlocked ? "&aUnlocked" : "&eClick to upgrade in order.");
            inventory.setItem(slots[i], item(tier.icon(), (current ? "&a" : "&b") + tier.displayName() + " Bank", lore, unlocked ? null : "upgrade"));
        }
        BankTier next = account.tier().next();
        inventory.setItem(31, item(Material.ANVIL, "&aUpgrade Bank", List.of(
                next == null ? "&7Your bank is already maxed." : "&7Next Tier: &f" + next.displayName(),
                next == null ? "&7No upgrade available." : "&7Cost: &e" + economyManager.format(next.upgradeCost()) + " coins"
        ), "upgrade"));
        inventory.setItem(49, item(Material.ARROW, "&eBack", List.of(), "back"));
        player.openInventory(inventory);
    }

    private void openHistory(Player player) {
        UUID uuid = player.getUniqueId();
        if (!beginLoading(player, "&eLoading bank history...")) return;
        bankManager.recentTransactionsAsync(uuid, 28, transactions -> {
            loadingBankAccounts.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || transactions == null) return;
            openHistoryLoaded(online, transactions);
        });
    }

    private void openHistoryLoaded(Player player, List<BankTransaction> transactions) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder(HISTORY_TYPE), 54, CC.color("&6Bank History"));
        fill(inventory);
        int slot = 10;
        for (BankTransaction transaction : transactions) {
            if (slot == 17 || slot == 26 || slot == 35) slot += 2;
            if (slot >= 44) break;
            inventory.setItem(slot++, item(Material.PAPER, "&f" + pretty(transaction.type()), List.of(
                    "&7Amount: &e" + economyManager.format(transaction.amount()) + " coins",
                    "&7Before: &f" + economyManager.format(transaction.balanceBefore()),
                    "&7After: &f" + economyManager.format(transaction.balanceAfter()),
                    "&7Source: &f" + transaction.source(),
                    "&8" + dateFormat.format(new Date(transaction.createdAt()))
            ), null));
        }
        if (transactions.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, "&cNo Transactions", List.of("&7Your bank history is empty."), null));
        }
        inventory.setItem(49, item(Material.ARROW, "&eBack", List.of(), "back"));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MenuHolder(String type))) return;
        if (!type.equals(MAIN_TYPE) && !type.equals(UPGRADE_TYPE) && !type.equals(HISTORY_TYPE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String action = action(event.getCurrentItem());
        if (action == null) return;

        switch (action) {
            case "close" -> player.closeInventory();
            case "back" -> open(player);
            case "deposit_all" -> runAsync(player, callback -> bankManager.depositAsync(player.getUniqueId(), economyManager.getCoins(player), "deposit_all", callback), false);
            case "deposit_half" -> runAsync(player, callback -> bankManager.depositAsync(player.getUniqueId(), economyManager.getCoins(player) / 2D, "deposit_half", callback), false);
            case "withdraw_half" -> runAsync(player, callback -> bankManager.withdrawHalfAsync(player.getUniqueId(), callback), false);
            case "withdraw_amount" -> runAsync(player, callback -> bankManager.withdrawAmountAsync(player.getUniqueId(), callback), false);
            case "interest" -> runAsync(player, callback -> bankManager.claimInterestAsync(player.getUniqueId(), callback), false);
            case "upgrades" -> openUpgrades(player);
            case "upgrade" -> runAsync(player, callback -> bankManager.upgradeBankAsync(player.getUniqueId(), callback), true);
            case "history" -> openHistory(player);
            default -> {
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        double lost = bankManager.applyDeathLoss(event.getEntity());
        if (lost <= 0D) return;
        event.getEntity().sendMessage(CC.color("&cYou lost &e" + economyManager.format(lost) + " purse coins &con death."));
    }

    private void runAsync(Player player, Consumer<Consumer<BankManager.BankResult>> operation, boolean upgrades) {
        UUID uuid = player.getUniqueId();
        if (!beginLoading(player, "&eUpdating bank data...")) return;
        operation.accept(result -> {
            loadingBankAccounts.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online == null || result == null) return;
            run(online, result, upgrades);
        });
    }

    private void run(Player player, BankManager.BankResult result, boolean upgrades) {
        player.sendMessage(CC.color(result.message()));
        player.playSound(player.getLocation(), result.success() ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.ENTITY_VILLAGER_NO, 1F, 1F);
        if (upgrades) {
            openUpgrades(player);
        } else {
            open(player);
        }
    }

    private boolean beginLoading(Player player, String message) {
        if (!loadingBankAccounts.add(player.getUniqueId())) {
            player.sendMessage(CC.color("&eBank data is still loading..."));
            return false;
        }
        player.sendMessage(CC.color(message));
        return true;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), null);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
    }

    private ItemStack item(Material material, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(CC.color(name));
        List<String> lines = new ArrayList<>();
        for (String line : lore) lines.add(CC.color(line));
        if (action != null && !action.isBlank()) lines.add(CC.color("&8" + ACTION_PREFIX + action));
        meta.setLore(lines);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        addItemFlagIfPresent(meta, "HIDE_ADDITIONAL_TOOLTIP");
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addItemFlagIfPresent(ItemMeta meta, String name) {
        try {
            meta.addItemFlags((ItemFlag) Enum.valueOf((Class<Enum>) (Class) ItemFlag.class, name));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private String action(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null || !item.getItemMeta().hasLore()) return null;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        for (String line : lore) {
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.startsWith(ACTION_PREFIX)) {
                return stripped.substring(ACTION_PREFIX.length()).trim().toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    private String pretty(String id) {
        if (id == null || id.isBlank()) return "Unknown";
        String[] parts = id.toLowerCase(Locale.ROOT).split("[_\\- ]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? id : builder.toString();
    }
}
