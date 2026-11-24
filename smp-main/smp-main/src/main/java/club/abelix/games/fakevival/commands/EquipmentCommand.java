package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

public final class EquipmentCommand implements CommandExecutor, Listener {
    private static final Map<Integer, String> SLOTS = Map.of(
            10, "helmet",
            11, "chestplate",
            12, "leggings",
            13, "boots",
            20, "ring_1",
            21, "ring_2",
            22, "necklace",
            23, "charm",
            24, "totem"
    );

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final CustomItemManager customItemManager;

    public EquipmentCommand(JavaPlugin plugin, DataManager dataManager, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.customItemManager = customItemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!player.hasPermission("fakevival.command.equipment")) {
            player.sendMessage(CC.color("&cYou do not have permission to use equipment."));
            return true;
        }
        open(player);
        return true;
    }

    private void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("equipment"), 54, CC.color("&8Equipment"));
        fill(inventory);
        for (Map.Entry<Integer, String> entry : SLOTS.entrySet()) {
            ItemStack equipped = equipped(player, entry.getValue());
            inventory.setItem(entry.getKey(), equipped == null ? placeholder(entry.getValue()) : equipped);
        }
        inventory.setItem(49, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equalsIgnoreCase("equipment")) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRawSlot() >= event.getInventory().getSize()) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        String equipmentSlot = SLOTS.get(slot);
        if (equipmentSlot == null) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = equipped(player, equipmentSlot);
        if (cursor == null || cursor.getType().isAir()) {
            if (current == null) return;
            giveOrDrop(player, current);
            setEquipped(player, equipmentSlot, null);
            event.getInventory().setItem(slot, placeholder(equipmentSlot));
            return;
        }
        if (!isValidForSlot(cursor, equipmentSlot)) {
            player.sendMessage(CC.color("&cThat item cannot be equipped in the &f" + pretty(equipmentSlot) + " &cslot."));
            return;
        }

        ItemStack equipped = cursor.clone();
        equipped.setAmount(1);
        cursor.setAmount(cursor.getAmount() - 1);
        player.setItemOnCursor(cursor.getAmount() <= 0 ? null : cursor);
        if (current != null) giveOrDrop(player, current);
        setEquipped(player, equipmentSlot, equipped);
        event.getInventory().setItem(slot, equipped);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        syncSavedArmor(event.getPlayer());
    }

    private boolean isValidForSlot(ItemStack item, String slot) {
        if (item == null || item.getType().isAir()) return false;
        Material type = item.getType();
        String customId = customItemManager == null ? null : customItemManager.getItemId(item);
        String category = customId == null ? "" : customCategory(customId);
        String id = customId == null ? "" : customId.toLowerCase(Locale.ROOT);
        return switch (slot) {
            case "helmet" -> type.name().endsWith("_HELMET");
            case "chestplate" -> type.name().endsWith("_CHESTPLATE");
            case "leggings" -> type.name().endsWith("_LEGGINGS");
            case "boots" -> type.name().endsWith("_BOOTS");
            case "ring_1", "ring_2" -> category.equals("rings") || id.contains("ring");
            case "necklace" -> category.equals("necklaces") || id.contains("necklace") || id.contains("amulet");
            case "charm" -> category.equals("trinkets") || id.contains("charm") || id.contains("compass") || id.contains("rune") || id.contains("idol");
            case "totem" -> type == Material.TOTEM_OF_UNDYING || id.contains("totem");
            default -> false;
        };
    }

    private String customCategory(String id) {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("custom-items.items." + id);
        return section == null ? "" : section.getString("category", "").toLowerCase(Locale.ROOT);
    }

    private ItemStack equipped(Player player, String slot) {
        if (isArmorSlot(slot)) {
            ItemStack actual = actualArmor(player, slot);
            if (actual != null && !actual.getType().isAir()) return actual;
        }
        Object object = dataManager.getData().get(path(player, slot));
        return object instanceof ItemStack item ? item : null;
    }

    public List<ItemStack> equippedAccessories(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (String slot : List.of("ring_1", "ring_2", "necklace", "charm", "totem")) {
            ItemStack item = equipped(player, slot);
            if (item != null && !item.getType().isAir()) items.add(item);
        }
        return List.copyOf(items);
    }

    private void setEquipped(Player player, String slot, ItemStack item) {
        dataManager.getData().set(path(player, slot), item);
        dataManager.save();
        if (isArmorSlot(slot)) setActualArmor(player, slot, item);
    }

    private String path(Player player, String slot) {
        return "players." + player.getUniqueId() + ".equipment." + slot;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), drop);
    }

    private void syncSavedArmor(Player player) {
        for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
            Object object = dataManager.getData().get(path(player, slot));
            if (object instanceof ItemStack item) setActualArmor(player, slot, item);
        }
    }

    private boolean isArmorSlot(String slot) {
        return slot.equals("helmet") || slot.equals("chestplate") || slot.equals("leggings") || slot.equals("boots");
    }

    private ItemStack actualArmor(Player player, String slot) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        return switch (slot) {
            case "helmet" -> armor.length > 3 ? armor[3] : null;
            case "chestplate" -> armor.length > 2 ? armor[2] : null;
            case "leggings" -> armor.length > 1 ? armor[1] : null;
            case "boots" -> armor.length > 0 ? armor[0] : null;
            default -> null;
        };
    }

    private void setActualArmor(Player player, String slot, ItemStack item) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        switch (slot) {
            case "helmet" -> armor[3] = item;
            case "chestplate" -> armor[2] = item;
            case "leggings" -> armor[1] = item;
            case "boots" -> armor[0] = item;
            default -> {
            }
        }
        player.getInventory().setArmorContents(armor);
    }

    private ItemStack placeholder(String slot) {
        Material material = switch (slot) {
            case "helmet" -> Material.LEATHER_HELMET;
            case "chestplate" -> Material.LEATHER_CHESTPLATE;
            case "leggings" -> Material.LEATHER_LEGGINGS;
            case "boots" -> Material.LEATHER_BOOTS;
            case "ring_1", "ring_2" -> Material.GOLD_NUGGET;
            case "necklace" -> Material.AMETHYST_SHARD;
            case "charm" -> Material.NETHER_STAR;
            case "totem" -> Material.TOTEM_OF_UNDYING;
            default -> Material.GRAY_DYE;
        };
        return item(material, "&7" + pretty(slot), List.of(
                "&7Place a valid item on your cursor",
                "&7and click this slot to equip it.",
                "&7Click an equipped item with an empty cursor",
                "&7to remove it.",
                "&8Equipment Slot: " + slot
        ));
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(CC.colorList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
    }

    private String pretty(String text) {
        String clean = ChatColor.stripColor(String.valueOf(text)).toLowerCase(Locale.ROOT).replace('_', ' ');
        return clean.substring(0, 1).toUpperCase(Locale.ROOT) + clean.substring(1);
    }
}
