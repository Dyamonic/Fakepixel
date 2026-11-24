package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.hooks.AbelixHook;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public final class GiveDevNotebookCommand implements CommandExecutor, Listener {
    private static final String ITEM_ID = "developer_notebook";
    private static final String COUNTER_ID = "developer_notebook_edition";

    private final Game plugin;
    private final DataManager dataManager;
    private final CustomItemManager customItems;
    private final AbelixHook abelix;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey itemTypeKey;
    private final NamespacedKey rarityKey;
    private final NamespacedKey specialItemKey;
    private final NamespacedKey giverNameKey;
    private final NamespacedKey receiverNameKey;
    private final NamespacedKey editionKey;
    private final NamespacedKey blockedEnchantingKey;
    private final NamespacedKey blockedAnvilKey;
    private final NamespacedKey blockedGrindstoneKey;
    private final NamespacedKey blockedInteractOpenKey;
    private final NamespacedKey developerNotebookKey,randomStatsBlockedKey,randomRarityBlockedKey,reforgeAllowedKey,enchantAllowedKey;
    private final NamespacedKey reforgeBlockedKey,enchantBlockedKey,plainEditionKey,receiverUuidKey,uniqueItemIdKey;

    public GiveDevNotebookCommand(Game plugin, CustomItemManager customItems) {
        this.plugin = plugin;
        this.dataManager = plugin.getDataManager();
        this.customItems = customItems;
        this.abelix = plugin.getAbelixHook();
        this.itemIdKey = new NamespacedKey(plugin, "custom_item_id");
        this.itemTypeKey = new NamespacedKey(plugin, "custom_item_type");
        this.rarityKey = new NamespacedKey(plugin, "custom_item_rarity");
        this.specialItemKey = new NamespacedKey(plugin, "special_item");
        this.giverNameKey = new NamespacedKey(plugin, "giver_name");
        this.receiverNameKey = new NamespacedKey(plugin, "receiver_name");
        this.editionKey = new NamespacedKey(plugin, "developer_notebook_edition");
        this.blockedEnchantingKey = new NamespacedKey(plugin, "blocked_from_enchanting");
        this.blockedAnvilKey = new NamespacedKey(plugin, "blocked_from_anvil");
        this.blockedGrindstoneKey = new NamespacedKey(plugin, "blocked_from_grindstone");
        this.blockedInteractOpenKey = new NamespacedKey(plugin, "blocked_from_interact_open");
        this.developerNotebookKey=new NamespacedKey(plugin,"developer_notebook");this.randomStatsBlockedKey=new NamespacedKey(plugin,"random_stats_blocked");this.randomRarityBlockedKey=new NamespacedKey(plugin,"random_rarity_blocked");this.reforgeAllowedKey=new NamespacedKey(plugin,"reforge_allowed");this.enchantAllowedKey=new NamespacedKey(plugin,"enchant_allowed");
        this.reforgeBlockedKey=new NamespacedKey(plugin,"reforge_blocked");this.enchantBlockedKey=new NamespacedKey(plugin,"enchant_blocked");this.plainEditionKey=new NamespacedKey(plugin,"edition");this.receiverUuidKey=new NamespacedKey(plugin,"receiver_uuid");this.uniqueItemIdKey=new NamespacedKey(plugin,"unique_item_id");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fakevival.admin.givedevnotebook")) {
            sender.sendMessage(CC.color("&cYou do not have permission to use this command."));
            return true;
        }
        if (args.length != 2) {
            sender.sendMessage(CC.color("&cUsage: /givedevnotebook <giver> <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(CC.color("&cPlayer not found: " + args[1]));
            return true;
        }

        String giverName = args[0];
        nextDeveloperNotebookEdition().whenComplete((edition, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null || edition == null) {
                plugin.getLogger().warning("Could not allocate Developer's Notebook edition: " + (error == null ? "unknown error" : error.getMessage()));
                sender.sendMessage(CC.color("&cCould not create Developer's Notebook because the edition counter failed."));
                return;
            }
            if (!target.isOnline()) {
                sender.sendMessage(CC.color("&cPlayer is no longer online. Edition &e" + edition + " &cwas reserved and will not be reused."));
                return;
            }
            ItemStack notebook = createNotebook(giverName, target, edition);
            Map<Integer, ItemStack> overflow = target.getInventory().addItem(notebook);
            overflow.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
            sender.sendMessage(CC.color("&aGave &cDeveloper's Notebook &ato &e" + target.getName() + "&a. &7Edition: &e" + edition + "#"));
            target.sendMessage(CC.color("&aYou received &cDeveloper's Notebook&a."));
        }));
        return true;
    }

    private ItemStack createNotebook(String giverName, Player target, long edition) {
        ItemStack item = customItems.itemExists(ITEM_ID)
                ? customItems.createItem(ITEM_ID, 1)
                : new ItemStack(Material.BOOK);
        item.setType(Material.BOOK);
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(CC.color("&cDeveloper's Notebook"));
        meta.setLore(List.of(
                CC.color("&7Every page rewrites itself"),
                CC.color("&7whenever a new bug is discovered."),
                CC.color("&7Some pages are best left unread."),
                "",
                CC.color(prefix(Bukkit.getPlayerExact(giverName)) + giverName + " &7has given"),
                CC.color("&7this to " + prefix(target) + target.getName()),
                "",
                CC.color("&8Edition: &8" + edition + "#"),
                "",
                CC.color("&c&lSPECIAL")
        ));
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemIdKey, PersistentDataType.STRING, ITEM_ID);
        pdc.set(itemTypeKey, PersistentDataType.STRING, "SPECIAL");
        pdc.set(rarityKey, PersistentDataType.STRING, "SPECIAL");
        pdc.set(specialItemKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(developerNotebookKey,PersistentDataType.BYTE,(byte)1);
        pdc.set(randomStatsBlockedKey,PersistentDataType.BYTE,(byte)1);
        pdc.set(randomRarityBlockedKey,PersistentDataType.BYTE,(byte)1);
        pdc.set(reforgeAllowedKey,PersistentDataType.BYTE,(byte)0);
        pdc.set(enchantAllowedKey,PersistentDataType.BYTE,(byte)0);
        pdc.set(reforgeBlockedKey,PersistentDataType.BYTE,(byte)1);
        pdc.set(enchantBlockedKey,PersistentDataType.BYTE,(byte)1);
        pdc.set(giverNameKey, PersistentDataType.STRING, giverName);
        pdc.set(receiverNameKey, PersistentDataType.STRING, target.getName());
        pdc.set(editionKey, PersistentDataType.LONG, edition);
        pdc.set(plainEditionKey, PersistentDataType.LONG, edition);
        pdc.set(uniqueItemIdKey, PersistentDataType.STRING, "developer_notebook:" + edition);
        pdc.set(receiverUuidKey, PersistentDataType.STRING, target.getUniqueId().toString());
        pdc.set(blockedEnchantingKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(blockedAnvilKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(blockedGrindstoneKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(blockedInteractOpenKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public CompletableFuture<Long> nextDeveloperNotebookEdition() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        if (!dataManager.isMySqlEnabled()) {
            future.completeExceptionally(new SQLException("MariaDB is unavailable"));
            return future;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = dataManager.getConnection()) {
                boolean autoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    long edition;
                    try (PreparedStatement select = connection.prepareStatement(
                            "SELECT next_value FROM smp_special_item_counters WHERE counter_id=? FOR UPDATE")) {
                        select.setString(1, COUNTER_ID);
                        try (ResultSet rows = select.executeQuery()) {
                            if (rows.next()) {
                                edition = rows.getLong(1);
                                try (PreparedStatement update = connection.prepareStatement(
                                        "UPDATE smp_special_item_counters SET next_value=? WHERE counter_id=?")) {
                                    update.setLong(1, Math.addExact(edition, 1L));
                                    update.setString(2, COUNTER_ID);
                                    update.executeUpdate();
                                }
                            } else {
                                edition = 1L;
                                try (PreparedStatement insert = connection.prepareStatement(
                                        "INSERT INTO smp_special_item_counters(counter_id,next_value) VALUES(?,2)")) {
                                    insert.setString(1, COUNTER_ID);
                                    insert.executeUpdate();
                                }
                            }
                        }
                    }
                    connection.commit();
                    connection.setAutoCommit(autoCommit);
                    future.complete(edition);
                } catch (Throwable error) {
                    try { connection.rollback(); } catch (SQLException ignored) { }
                    future.completeExceptionally(error);
                }
            } catch (Throwable error) {
                future.completeExceptionally(error);
            }
        });
        return future;
    }

    public boolean isDeveloperNotebook(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(itemIdKey, PersistentDataType.STRING);
        // Both identity tags are required. SPECIAL, blocked-randomness and the
        // material alone are shared by many legitimate custom items.
        return ITEM_ID.equalsIgnoreCase(id) && byteFlag(pdc, developerNotebookKey);
    }

    private boolean byteFlag(PersistentDataContainer pdc, NamespacedKey key) {
        return Byte.valueOf((byte) 1).equals(pdc.get(key, PersistentDataType.BYTE));
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isDeveloperNotebook(event.getItem())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(CC.color("&cThe Developer's Notebook refuses to open."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (isDeveloperNotebook(event.getItem())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        if (isDeveloperNotebook(event.getItem())) event.setCancelled(true);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (containsNotebook(event.getInventory())) event.setResult(null);
    }

    @EventHandler
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (containsNotebook(event.getInventory())) event.setResult(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void onProtectedInventoryClick(InventoryClickEvent event) {
        InventoryType type = event.getView().getTopInventory().getType();
        if (type != InventoryType.ENCHANTING && type != InventoryType.ANVIL && type != InventoryType.GRINDSTONE) return;

        Inventory top = event.getView().getTopInventory();
        boolean notebookInTransaction = containsNotebook(top)
                || isDeveloperNotebook(event.getCurrentItem())
                || isDeveloperNotebook(event.getCursor());
        if (!notebookInTransaction) return;

        event.setCancelled(true);
        if (type == InventoryType.ANVIL || type == InventoryType.GRINDSTONE) top.setItem(2, null);
    }

    private boolean containsNotebook(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (isDeveloperNotebook(item)) return true;
        }
        return false;
    }

    private String prefix(Player player) {
        if (player == null || abelix == null) return "";
        String prefix = abelix.getPrefix(player);
        return prefix == null || prefix.equalsIgnoreCase("null") ? "" : prefix;
    }
}
