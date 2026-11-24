package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.config.RuntimeSettings;
import club.abelix.games.fakevival.config.defaults.CustomEnchantUnlockDefaults;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.managers.CollectionManager;
import club.abelix.games.fakevival.managers.CustomEnchantManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.managers.SkillManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class EnchantTableCommand implements CommandExecutor, Listener {
    private static final int ITEM_SLOT = 13;
    private static final int PLACEHOLDER_SLOT = 4;
    private static final int CLOSE_SLOT = 40;
    private static final int INFO_SLOT = 31;
    private static final int MAX_BOOKSHELF_POWER = 30;

    private final JavaPlugin plugin;
    private final CustomEnchantManager enchantManager;
    private final CollectionManager collectionManager;
    private final SkillManager skillManager;
    private final EconomyManager economyManager;
    private final CustomItemManager customItemManager;
    private final DataManager dataManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey customItemTypeKey;
    private final NamespacedKey unlockEnchantKey;
    private final Set<UUID> transitioning = new java.util.HashSet<>();
    private final Map<UUID, Set<String>> learnedEnchants = new ConcurrentHashMap<>();
    private final Set<UUID> learnedLoaded = ConcurrentHashMap.newKeySet();

    public EnchantTableCommand(JavaPlugin plugin, CustomEnchantManager enchantManager, CollectionManager collectionManager, SkillManager skillManager, EconomyManager economyManager, CustomItemManager customItemManager, DataManager dataManager) {
        this.plugin = plugin;
        this.enchantManager = enchantManager;
        this.collectionManager = collectionManager;
        this.skillManager = skillManager;
        this.economyManager = economyManager;
        this.customItemManager = customItemManager;
        this.dataManager = dataManager;
        this.actionKey = new NamespacedKey(plugin, "enchant_table_action");
        this.customItemTypeKey = new NamespacedKey(plugin, "custom_item_type");
        this.unlockEnchantKey = new NamespacedKey(plugin, "unlock_enchant_id");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (command.getName().equalsIgnoreCase("enchants")) {
            if (!player.hasPermission("fakevival.command.enchants")) return CommandUtil.noPermission(plugin, sender);
            showEnchantList(player);
            return true;
        }
        if (!player.hasPermission("fakevival.command.etable")) return CommandUtil.noPermission(plugin, sender);
        openMain(player, bookshelfPower(player.getLocation()), null);
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantTableUse(PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && useUnlockBook(event.getPlayer(), event.getItem())) {
            event.setCancelled(true);
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.ENCHANTING_TABLE) return;
        event.setCancelled(true);
        openMain(event.getPlayer(), bookshelfPower(event.getClickedBlock().getLocation()), null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadLearned(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        learnedEnchants.remove(event.getPlayer().getUniqueId());
        learnedLoaded.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.startsWith("etable:")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean main = type.startsWith("etable:main:");
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();
        if (main && rawSlot == ITEM_SLOT) {
            if (!inputSlotClickAllowed(event)) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(false);
            refreshMainNextTick(player, event.getInventory(), holderPower(type));
            return;
        }

        if (rawSlot >= topSize) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                shiftIntoInput(player, event, event.getInventory(), event.getCurrentItem(), holderPower(type), main);
            } else {
                event.setCancelled(false);
            }
            return;
        }

        event.setCancelled(true);
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String action = action(clicked);
        int power = holderPower(type);

        if (event.getRawSlot() == CLOSE_SLOT || action.equals("close")) {
            player.closeInventory();
            return;
        }
        if (action.equals("back")) {
            transition(player, () -> openMain(player, power, heldItem(event.getInventory())));
            return;
        }
        if (action.equals("previous") || action.equals("next")) {
            String[] parts = type.split(":");
            if (parts.length >= 5 && parts[1].equals("list")) {
                String category = parts[2];
                int page = parseInt(parts[3]);
                int nextPage = page + (action.equals("next") ? 1 : -1);
                transition(player, () -> openEnchantList(player, category, nextPage, power, heldItem(event.getInventory())));
            }
            return;
        }
        if (main && action.startsWith("category:")) {
            ItemStack target = heldItem(event.getInventory());
            if (target == null) {
                player.sendMessage(CC.color("&cPlace an item in the center slot first."));
                return;
            }
            String typeId = action.substring("category:".length());
            transition(player, () -> openEnchantList(player, typeId, 0, power, target));
            return;
        }
        if (action.startsWith("enchant:")) {
            String enchantId = action.substring("enchant:".length());
            RequirementStatus status = status(player, enchantId, 1, power);
            if (!status.unlocked()) {
                sendMissing(player, status);
                return;
            }
            String[] parts = type.split(":");
            String category = parts.length > 2 ? parts[2] : "weapon";
            transition(player, () -> openLevelMenu(player, category, enchantId, power, heldItem(event.getInventory())));
            return;
        }
        if (action.startsWith("level:")) {
            String enchantId = action.substring("level:".length(), action.lastIndexOf(':'));
            int level = Integer.parseInt(action.substring(action.lastIndexOf(':') + 1));
            apply(player, event.getInventory(), enchantId, level, power);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.startsWith("etable:")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize());
        if (!touchesTop) {
            event.setCancelled(false);
            return;
        }
        boolean main = type.startsWith("etable:main:");
        if (main && event.getRawSlots().size() == 1 && event.getRawSlots().contains(ITEM_SLOT) && isValidInput(event.getOldCursor())) {
            event.setCancelled(false);
            refreshMainNextTick(player, event.getInventory(), holderPower(type));
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.startsWith("etable:")) return;
        if (transitioning.remove(event.getPlayer().getUniqueId())) return;
        ItemStack item = heldItem(event.getInventory());
        if (item == null) return;
        event.getInventory().setItem(ITEM_SLOT, null);
        Map<Integer, ItemStack> leftovers = event.getPlayer().getInventory().addItem(item);
        leftovers.values().forEach(leftover -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), leftover));
    }

    private void openMain(Player player, int power, ItemStack carried) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("etable:main:" + power), 54, CC.color("&8SMP Enchant Table"));
        fill(inventory);
        inventory.setItem(PLACEHOLDER_SLOT, item(Material.GRAY_STAINED_GLASS_PANE, "&7Place Item", List.of("&7Put an item in the empty center slot", "&7to view matching enchant groups."), ""));
        inventory.setItem(ITEM_SLOT, carried == null ? null : carried.clone());
        ItemStack target = carried;
        putCategory(inventory, 20, "weapon", Material.DIAMOND_SWORD, "&cSword Enchants", target);
        putCategory(inventory, 21, "bow", Material.BOW, "&aBow Enchants", target);
        putCategory(inventory, 22, "armor", Material.DIAMOND_CHESTPLATE, "&9Armor Enchants", target);
        putCategory(inventory, 23, "tool", Material.DIAMOND_PICKAXE, "&eTool Enchants", target);
        putCategory(inventory, 24, "fishing", Material.FISHING_ROD, "&bFishing Rod Enchants", target);
        putCategory(inventory, 29, "magic", Material.BLAZE_ROD, "&dMagic Enchants", target);
        putCategory(inventory, 30, "accessory", Material.AMETHYST_SHARD, "&5Accessory Enchants", target);
        putCategory(inventory, 32, "utility", Material.ANVIL, "&6Utility Enchants", target);
        inventory.setItem(INFO_SLOT, item(Material.BOOKSHELF, "&eBookshelf Power", List.of("&7Power: &e" + power + "&7/&e" + MAX_BOOKSHELF_POWER, "&7Used by stronger enchant levels."), ""));
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "&cClose", List.of(), "close"));
        player.openInventory(inventory);
    }

    private void openEnchantList(Player player, String category, int requestedPage, int power, ItemStack target) {
        List<CustomEnchantManager.CustomEnchant> entries = enchantManager.byType(category).stream()
                .filter(enchant -> target == null || enchantManager.canApply(target, enchant.id())).toList();
        int[] slots = {10, 11, 12, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 32, 33, 34};
        int pages = Math.max(1, (int) Math.ceil(entries.size() / (double) slots.length));
        int page = Math.clamp(requestedPage, 0, pages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("etable:list:" + category + ":" + page + ":" + power), 54, CC.color("&8" + pretty(category) + " Enchants"));
        fill(inventory);
        inventory.setItem(ITEM_SLOT, target == null ? null : target.clone());
        int start = page * slots.length;
        for (int index = 0; index < slots.length && start + index < entries.size(); index++) {
            CustomEnchantManager.CustomEnchant enchant = entries.get(start + index);
            RequirementStatus status = status(player, enchant.id(), 1, power);
            List<String> lore = enchantLore(enchant, 1, status, false);
            inventory.setItem(slots[index], item(status.unlocked() ? Material.ENCHANTED_BOOK : Material.GRAY_DYE,
                    (status.unlocked() ? "&a" : "&c") + enchant.name() + " " + enchantManager.roman(1), lore, "enchant:" + enchant.id()));
        }
        if (page > 0) inventory.setItem(45, item(Material.ARROW, "&ePrevious Page", List.of("&7Page " + page + " of " + pages), "previous"));
        if (page + 1 < pages) inventory.setItem(53, item(Material.ARROW, "&eNext Page", List.of("&7Page " + (page + 2) + " of " + pages), "next"));
        inventory.setItem(48, item(Material.ARROW, "&aBack", List.of(), "back"));
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "&cClose", List.of(), "close"));
        player.openInventory(inventory);
    }

    private void openLevelMenu(Player player, String category, String enchantId, int power, ItemStack target) {
        CustomEnchantManager.CustomEnchant enchant = enchantManager.get(enchantId);
        if (enchant == null) return;
        Inventory inventory = Bukkit.createInventory(new MenuHolder("etable:level:" + category + ":" + enchantId + ":" + power), 54, CC.color("&8" + enchant.name()));
        fill(inventory);
        inventory.setItem(ITEM_SLOT, target == null ? null : target.clone());
        int[] slots = {20, 21, 22, 23, 24};
        for (int level = 1; level <= Math.min(5, enchant.maxLevel()); level++) {
            RequirementStatus status = status(player, enchant.id(), level, power);
            int xp = xpCost(level);
            double coins = coinCost(level);
            List<String> lore = enchantLore(enchant, level, status, true);
            lore.add("&7XP Cost: &e" + xp + " levels");
            lore.add("&7Coin Cost: &6" + economyManager.format(coins));
            lore.add("");
            lore.add(status.unlocked() ? "&aUnlocked" : "&8Locked");
            if (status.unlocked()) lore.add("&eClick to apply!");
            inventory.setItem(slots[level - 1], item(status.unlocked() ? Material.ENCHANTED_BOOK : Material.GRAY_DYE,
                    (status.unlocked() ? "&a" : "&c") + enchant.name() + " " + enchantManager.roman(level), lore, "level:" + enchant.id() + ":" + level));
        }
        inventory.setItem(48, item(Material.ARROW, "&aBack", List.of(), "back"));
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "&cClose", List.of(), "close"));
        player.openInventory(inventory);
    }

    private void apply(Player player, Inventory inventory, String enchantId, int level, int power) {
        ItemStack target = heldItem(inventory);
        if (target == null) {
            player.sendMessage(CC.color("&cPlace an item in the center slot first."));
            return;
        }
        RequirementStatus status = status(player, enchantId, level, power);
        if (!status.unlocked()) {
            sendMissing(player, status);
            return;
        }
        if (!enchantManager.canApply(target, enchantId)) {
            player.sendMessage(CC.color("&cThat enchantment cannot be applied to this item."));
            return;
        }
        int currentLevel = enchantManager.appliedEnchants(target).getOrDefault(normalize(enchantId), 0);
        if (currentLevel >= level) {
            player.sendMessage(CC.color("&cThat item already has this enchantment at an equal or higher level."));
            return;
        }
        int xp = xpCost(level);
        if (player.getLevel() < xp) {
            player.sendMessage(CC.color("&cYou do not have enough XP levels!"));
            return;
        }
        double coins = coinCost(level);
        if (coins > 0.0D && economyManager.getCoins(player) < coins) {
            player.sendMessage(CC.color("&cYou do not have enough coins!"));
            return;
        }
        String conflict = enchantManager.conflict(target, enchantId);
        if (conflict != null) {
            player.sendMessage(CC.color("&cThis enchant conflicts with " + enchantManager.plainName(conflict) + "."));
            return;
        }
        boolean charged = coins <= 0.0D || economyManager.takeCoins(player.getUniqueId(), coins, "enchant-table:" + enchantId + ":" + level);
        if (!charged) {
            player.sendMessage(CC.color("&cYou do not have enough coins!"));
            return;
        }
        CustomEnchantManager.ApplyResult result = enchantManager.applyEnchant(target, enchantId, level);
        if (!result.success()) {
            if (coins > 0.0D) economyManager.giveCoins(player.getUniqueId(), coins, "enchant-table-refund:" + enchantId + ":" + level);
            player.sendMessage(CC.color(result.message()));
            return;
        }
        player.setLevel(player.getLevel() - xp);
        skillManager.addXp(player, "enchanting", enchantingXp(level));
        inventory.setItem(ITEM_SLOT, target);
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.2F);
        player.sendMessage(CC.color("&aApplied " + result.message() + " to your item!"));
    }

    private RequirementStatus status(Player player, String enchantId, int level, int power) {
        List<String> lore = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        boolean unlocked = true;
        int bookshelf = Math.min(MAX_BOOKSHELF_POWER, Math.max(0, level * 3 - 1));
        if (power < bookshelf) {
            unlocked = false;
            lore.add("&cRequires Bookshelf Power " + bookshelf);
            messages.add("&cRequires Bookshelf Power " + bookshelf + ".");
        }

        CustomEnchantUnlockDefaults.UnlockRequirement requirement = enchantManager.unlockRequirement(enchantId);
        if (RuntimeSettings.config(plugin).getBoolean("skills.enabled", true)) {
            int required = Math.max(requirement.requiredEnchantingLevel(), level * 2);
            int current = skillManager.getLevel(player, "enchanting");
            if (current < required) {
                unlocked = false;
                lore.add("&cRequires Enchanting Level " + required);
                messages.add("&cRequires Enchanting Level " + required + ".");
            }
        }

        for (Map.Entry<String, Integer> entry : requirement.requiredCollections().entrySet()) {
            int currentTier = collectionManager.getUnlockedTier(player, entry.getKey());
            if (currentTier < entry.getValue()) {
                unlocked = false;
                String name = strip(collectionManager.getDisplayName(entry.getKey()));
                lore.add("&cRequires " + name + " Collection " + collectionManager.toRoman(entry.getValue()));
                messages.add("&cRequires " + name + " Collection " + collectionManager.toRoman(entry.getValue()) + ".");
            }
        }
        for (Map.Entry<String, Integer> entry : requirement.requiredDungeonCollection().entrySet()) {
            int currentTier = collectionManager.getUnlockedTier(player, entry.getKey());
            if (currentTier < entry.getValue()) {
                unlocked = false;
                String name = strip(collectionManager.getDisplayName(entry.getKey()));
                lore.add("&cRequires " + name + " Collection " + collectionManager.toRoman(entry.getValue()));
                messages.add("&cRequires " + name + " Collection " + collectionManager.toRoman(entry.getValue()) + ".");
            }
        }
        if (plugin instanceof Game game) {
            int currentLevel = game.getPlayerLevelManager().getLevel(player);
            if (currentLevel < requirement.requiredPlayerLevel()) {
                unlocked = false;
                lore.add("&cRequires PlayerLevel " + requirement.requiredPlayerLevel());
                messages.add("&cRequires PlayerLevel " + requirement.requiredPlayerLevel() + ".");
            }
            int currentBosses = game.getAdventureGuildManager().profile(player).bosses();
            if (currentBosses < requirement.requiredBossKills()) {
                unlocked = false;
                String boss = requirement.bossName().isBlank() ? "Boss" : requirement.bossName();
                lore.add("&cRequires " + boss + " Kills: " + currentBosses + "/" + requirement.requiredBossKills());
                messages.add("&cRequires " + boss + " Kills: " + currentBosses + "/" + requirement.requiredBossKills() + ".");
            }
        }
        if (requirement.requiresBookUnlock() && !isLearned(player, enchantId)) {
            unlocked = false;
            lore.add("&cRequires Enchantment Book Unlock");
            messages.add("&cYou need to learn this enchantment from a book first.");
        }
        return new RequirementStatus(unlocked, List.copyOf(lore), List.copyOf(messages));
    }

    private int bookshelfPower(Location center) {
        int power = 0;
        boolean enchantedBookshelfKnown = customItemManager.itemExists("enchanted_bookshelf");
        for (int x = -4; x <= 4; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -4; z <= 4; z++) {
                    Block block = center.getBlock().getRelative(x, y, z);
                    if (block.getType() == Material.BOOKSHELF) power++;
                    if (enchantedBookshelfKnown && block.getType() == Material.CHISELED_BOOKSHELF) power += 5;
                }
            }
        }
        return Math.min(MAX_BOOKSHELF_POWER, power);
    }

    private void putCategory(Inventory inventory, int slot, String category, Material material, String name, ItemStack target) {
        if (target == null) return;
        if (target != null && !categoryMatches(target, category)) return;
        inventory.setItem(slot, item(material, name, List.of("&7View matching " + pretty(category).toLowerCase(Locale.ROOT) + " enchants."), "category:" + category));
    }

    private boolean categoryMatches(ItemStack item, String category) {
        if (item == null || item.getType().isAir()) return false;
        if (item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK) return true;
        return enchantManager.byType(category).stream().anyMatch(enchant -> enchantManager.canApply(item, enchant.id()));
    }

    private boolean inputSlotClickAllowed(InventoryClickEvent event) {
        InventoryType.SlotType slotType = event.getSlotType();
        if (slotType == InventoryType.SlotType.OUTSIDE) return false;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        return isEmpty(cursor) || isEmpty(current) || isValidInput(cursor);
    }

    private void shiftIntoInput(Player player, InventoryClickEvent event, Inventory inventory, ItemStack clicked, int power, boolean main) {
        if (!main) return;
        if (!isValidInput(clicked)) return;
        if (!isEmpty(inventory.getItem(ITEM_SLOT))) {
            player.sendMessage(CC.color("&cRemove the current item first!"));
            return;
        }
        ItemStack moved = clicked.clone();
        event.setCurrentItem(null);
        inventory.setItem(ITEM_SLOT, moved);
        refreshMainNextTick(player, inventory, power);
    }

    private void refreshMainNextTick(Player player, Inventory inventory, int power) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top != inventory || !(top.getHolder() instanceof MenuHolder(String type)) || !type.startsWith("etable:main:")) return;
            refreshMainInventory(top, power);
        });
    }

    private void refreshMainInventory(Inventory inventory, int power) {
        ItemStack target = heldItem(inventory);
        inventory.setItem(PLACEHOLDER_SLOT, item(Material.GRAY_STAINED_GLASS_PANE, "&7Place Item", List.of("&7Put an item in the empty center slot", "&7to view matching enchant groups."), ""));
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "");
        for (int slot : new int[]{20, 21, 22, 23, 24, 29, 30, 32}) inventory.setItem(slot, filler);
        putCategory(inventory, 20, "weapon", Material.DIAMOND_SWORD, "&cSword Enchants", target);
        putCategory(inventory, 21, "bow", Material.BOW, "&aBow Enchants", target);
        putCategory(inventory, 22, "armor", Material.DIAMOND_CHESTPLATE, "&9Armor Enchants", target);
        putCategory(inventory, 23, "tool", Material.DIAMOND_PICKAXE, "&eTool Enchants", target);
        putCategory(inventory, 24, "fishing", Material.FISHING_ROD, "&bFishing Rod Enchants", target);
        putCategory(inventory, 29, "magic", Material.BLAZE_ROD, "&dMagic Enchants", target);
        putCategory(inventory, 30, "accessory", Material.AMETHYST_SHARD, "&5Accessory Enchants", target);
        putCategory(inventory, 32, "utility", Material.ANVIL, "&6Utility Enchants", target);
        inventory.setItem(INFO_SLOT, item(Material.BOOKSHELF, "&eBookshelf Power", List.of("&7Power: &e" + power + "&7/&e" + MAX_BOOKSHELF_POWER, "&7Used by stronger enchant levels."), ""));
        inventory.setItem(CLOSE_SLOT, item(Material.BARRIER, "&cClose", List.of(), "close"));
    }

    private boolean isValidInput(ItemStack item) {
        if (isEmpty(item) || !action(item).isBlank()) return false;
        Material material = item.getType();
        if (material == Material.BOOK || material == Material.ENCHANTED_BOOK) return true;
        String name = material.name();
        if (name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")
                || name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || material == Material.BOW
                || material == Material.CROSSBOW
                || material == Material.TRIDENT
                || material == Material.MACE
                || material == Material.FISHING_ROD) return true;
        return customItemManager.getItemId(item) != null;
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    private ItemStack heldItem(Inventory inventory) {
        ItemStack item = inventory.getItem(ITEM_SLOT);
        if (item == null || item.getType().isAir() || action(item).isBlank()) return item == null || item.getType().isAir() ? null : item.clone();
        return null;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "");
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
    }

    private ItemStack item(Material material, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(material == null ? Material.STONE : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(CC.colorList(lore));
            if (action != null && !action.isBlank()) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String action(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(actionKey, PersistentDataType.STRING, "");
    }

    private int holderPower(String holder) {
        String[] parts = holder.split(":");
        try {
            return Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void transition(Player player, Runnable action) {
        transitioning.add(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, action);
    }

    private int xpCost(int level) {
        return Math.max(1, level * 3);
    }

    private double coinCost(int level) {
        return level * 250.0D;
    }

    private int enchantingXp(int level) {
        return 15 + level * 10;
    }

    private void showEnchantList(Player player) {
        player.sendMessage(CC.color("&6&lSMP Custom Enchants"));
        for (String category : List.of("weapon", "bow", "armor", "tool", "fishing", "magic", "accessory", "utility")) {
            List<String> names = enchantManager.byType(category).stream().map(CustomEnchantManager.CustomEnchant::name).toList();
            if (!names.isEmpty()) player.sendMessage(CC.color("&e" + pretty(category) + ": &f" + String.join("&7, &f", names)));
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String text) {
        String[] parts = normalize(text).split("_");
        List<String> out = new ArrayList<>();
        for (String part : parts) if (!part.isBlank()) out.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
        return String.join(" ", out);
    }

    private List<String> enchantLore(CustomEnchantManager.CustomEnchant enchant, int level, RequirementStatus status, boolean levelChoice) {
        List<String> lore = new ArrayList<>();
        lore.add((status.unlocked() ? "&a" : "&c") + enchant.name() + " " + enchantManager.roman(level));
        lore.add("&7" + enchantManager.description(enchant.id()));
        lore.add("");
        lore.addAll(status.lore());
        if (!status.lore().isEmpty()) lore.add("");
        if (!levelChoice) {
            lore.add(status.unlocked() ? "&aUnlocked" : "&8Locked");
            if (status.unlocked()) lore.add("&eClick to apply!");
        }
        return lore;
    }

    private void sendMissing(Player player, RequirementStatus status) {
        player.sendMessage(CC.color("&cYou have not unlocked this enchantment yet."));
        status.messages().forEach(message -> player.sendMessage(CC.color(message)));
    }

    private boolean useUnlockBook(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String type = pdc.get(customItemTypeKey, PersistentDataType.STRING);
        String enchantId = normalize(pdc.get(unlockEnchantKey, PersistentDataType.STRING));
        if (!"ENCHANT_UNLOCK_BOOK".equalsIgnoreCase(type) || enchantId.isBlank()) return false;
        CustomEnchantManager.CustomEnchant enchant = enchantManager.get(enchantId);
        if (enchant == null) {
            player.sendMessage(CC.color("&cThat enchantment manual is invalid."));
            return true;
        }
        if (isLearned(player, enchantId)) {
            player.sendMessage(CC.color("&cYou already know this enchantment."));
            return true;
        }
        saveLearned(player, enchantId, "BOOK", success -> {
            if (!success || !player.isOnline()) return;
            consumeUnlockBook(player, enchantId);
            player.sendMessage(CC.color("&aYou learned the &9" + enchant.name() + " &aenchantment!"));
        });
        return true;
    }

    private boolean isLearned(Player player, String enchantId) {
        return learnedEnchants.getOrDefault(player.getUniqueId(), Set.of()).contains(normalize(enchantId));
    }

    private void loadLearned(Player player) {
        UUID uuid = player.getUniqueId();
        if (!dataManager.isMySqlEnabled()) {
            Set<String> values = new java.util.HashSet<>(dataManager.getData().getStringList("players." + uuid + ".enchant-unlocks"));
            learnedEnchants.put(uuid, values);
            learnedLoaded.add(uuid);
            return;
        }
        CompletableFuture.runAsync(() -> {
            Set<String> values = ConcurrentHashMap.newKeySet();
            try (Connection connection = dataManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT enchant_id FROM smp_player_enchant_unlocks WHERE player_uuid=?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) values.add(normalize(result.getString(1)));
                }
                learnedEnchants.put(uuid, values);
                learnedLoaded.add(uuid);
            } catch (SQLException error) {
                plugin.getLogger().warning("Could not load enchant unlocks for " + uuid + ": " + error.getMessage());
            }
        });
    }

    private void saveLearned(Player player, String enchantId, String source, java.util.function.Consumer<Boolean> callback) {
        UUID uuid = player.getUniqueId();
        String normalized = normalize(enchantId);
        if (!dataManager.isMySqlEnabled()) {
            learnedEnchants.computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet()).add(normalized);
            List<String> values = new ArrayList<>(learnedEnchants.get(uuid));
            dataManager.getData().set("players." + uuid + ".enchant-unlocks", values);
            callback.accept(true);
            return;
        }
        CompletableFuture.runAsync(() -> {
            boolean success = false;
            try (Connection connection = dataManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement("INSERT INTO smp_player_enchant_unlocks(player_uuid,enchant_id,unlock_source,unlocked_at) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE unlock_source=VALUES(unlock_source),unlocked_at=VALUES(unlocked_at)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, normalized);
                statement.setString(3, source);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
                learnedEnchants.computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet()).add(normalized);
                success = true;
            } catch (SQLException error) {
                plugin.getLogger().warning("Could not save enchant unlock for " + uuid + ": " + error.getMessage());
            }
            boolean saved = success;
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(saved));
        });
    }

    private void consumeUnlockBook(Player player, String enchantId) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            var pdc = item.getItemMeta().getPersistentDataContainer();
            if (!"ENCHANT_UNLOCK_BOOK".equalsIgnoreCase(pdc.get(customItemTypeKey, PersistentDataType.STRING))) continue;
            if (!normalize(enchantId).equals(normalize(pdc.get(unlockEnchantKey, PersistentDataType.STRING)))) continue;
            item.setAmount(item.getAmount() - 1);
            return;
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String strip(String value) {
        String stripped = org.bukkit.ChatColor.stripColor(CC.color(value == null ? "" : value));
        return stripped == null ? "" : stripped;
    }

    public ValidationResult validateSetup() {
        int requirements = (int) enchantManager.all().stream().filter(enchant -> enchantManager.unlockRequirement(enchant.id()) != null).count();
        return new ValidationResult(true, true, requirements, CustomEnchantUnlockDefaults.collectionRewards().size(),
                CustomEnchantUnlockDefaults.bookUnlockIds().size(), dataManager.isMySqlEnabled());
    }

    public record ValidationResult(boolean rightClickExistingMenu, boolean pdcActions, int requirementEntries,
                                   int collectionRewards, int bookUnlocks, boolean mariaDbEnabled) {
    }

    private record RequirementStatus(boolean unlocked, List<String> lore, List<String> messages) {
    }
}
