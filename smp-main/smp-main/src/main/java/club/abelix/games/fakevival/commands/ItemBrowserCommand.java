package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.CollectionManager;
import club.abelix.games.fakevival.managers.CustomEnchantManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.VendorLoreSanitizer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ItemBrowserCommand implements CommandExecutor, Listener {
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final JavaPlugin plugin;
    private final CustomItemManager customItemManager;
    private final CustomEnchantManager customEnchantManager;
    private final CollectionManager collectionManager;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final Map<String, List<String>> entryCache = new LinkedHashMap<>();
    private static final List<String> TOP_LEVEL=List.of("materials","reforge_stones","cooking_ingredients","cooking","custom_fish","weapons","armor","tools","fishing","accessories","rings","necklaces","charms","totems","staffs_wands","potions","event_items","special_items");
    private static final List<String> MATERIAL_FILTERS=List.of("materials","mining_materials","foraging_materials","farming_materials","combat_materials","fishing_materials","dungeon_materials","magic_materials");
    private static final List<String> WEAPON_FILTERS=List.of("swords","daggers","katanas","bows");

    public ItemBrowserCommand(JavaPlugin plugin, CustomItemManager customItemManager, CustomEnchantManager customEnchantManager, CollectionManager collectionManager) {
        this.plugin = plugin;
        this.customItemManager = customItemManager;
        this.customEnchantManager = customEnchantManager;
        this.collectionManager = collectionManager;
        registerCategories();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!canUse(player)) {
            player.sendMessage(CC.color("&cYou do not have permission to use this command."));
            return true;
        }
        openCategories(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.startsWith("itembrowser:")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!canUse(player)) {
            player.closeInventory();
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        String cleanName = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT)
                : "";
        if (cleanName.equals("close")) {
            player.closeInventory();
            return;
        }
        if (cleanName.equals("back") || cleanName.equals("back to categories")) {
            BrowserView current=parseView(type);
            if(current!=null&&WEAPON_FILTERS.contains(current.category()))openWeaponFilters(player);
            else if(current!=null&&MATERIAL_FILTERS.contains(current.category())&&!current.category().equals("materials"))openMaterialFilters(player);
            else openCategories(player);
            return;
        }

        if (type.equals("itembrowser:categories")) {
            String category = loreValue(clicked, "Category ID: ");
            if ("materials".equals(category)) openMaterialFilters(player);
            else if ("weapons".equals(category)) openWeaponFilters(player);
            else if (category != null && categories.containsKey(category)) openCategory(player, category, 0);
            return;
        }
        if(type.equals("itembrowser:material_filters")){
            String category=loreValue(clicked,"Category ID: ");
            if(category!=null&&MATERIAL_FILTERS.contains(category))openCategory(player,category,0);
            return;
        }
        if(type.equals("itembrowser:weapon_filters")){
            String category=loreValue(clicked,"Category ID: ");
            if(category!=null&&WEAPON_FILTERS.contains(category))openCategory(player,category,0);
            return;
        }

        BrowserView view = parseView(type);
        if (view == null) return;
        if (cleanName.equals("previous page")) {
            openCategory(player, view.category(), Math.max(0, view.page() - 1));
            return;
        }
        if (cleanName.equals("next page")) {
            openCategory(player, view.category(), view.page() + 1);
            return;
        }

        String enchantId = loreValue(clicked, "Custom Enchant ID: ");
        if (enchantId != null && customEnchantManager.get(enchantId) != null) {
            player.getInventory().addItem(customEnchantManager.createBook(enchantId, event.isShiftClick() ? 3 : 1));
            player.sendMessage(CC.color("&aGave custom enchant book &f" + enchantId + "&a."));
            return;
        }

        String itemId = loreValue(clicked, "Item ID: ");
        if (itemId == null || !customItemManager.itemExists(itemId)) return;
        int amount = event.isShiftClick() ? 64 : 1;
        ItemStack given = customItemManager.createItem(itemId, amount).clone();
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(given);
        if (!overflow.isEmpty()) {
            player.sendMessage(CC.color("&cYour inventory is full."));
            overflow.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
        player.sendMessage(CC.color("&aGave &e" + amount + "x " + itemId + "&a."));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder(String type) && type.startsWith("itembrowser:")) event.setCancelled(true);
    }

    private void openCategories(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("itembrowser:categories"), 54, CC.color("&8Admin Item Browser"));

        inventory.setItem(4, item(Material.KNOWLEDGE_BOOK, "&6Admin Item Browser", List.of(
                "&7Browse custom items by category.",
                "&7Custom enchant books have their own category.",
                "",
                "&eClick a category."
        )));

        int slot = 10;
        for (String categoryId : TOP_LEVEL) {
            Category category=categories.get(categoryId);if(category==null)continue;
            int count = entriesFor(category.id()).size();
            int pages = Math.max(1, (int) Math.ceil(count / (double) CONTENT_SLOTS.length));
            inventory.setItem(slot, item(category.icon(), category.display(), List.of(
                    "&7Items: &e" + count,
                    "&7Pages: &e" + pages,
                    "",
                    "&eClick to browse!",
                    "&8Category ID: " + category.id()
            )));
            slot++;
            if (slot == 17) slot = 19;
            else if (slot == 26) slot = 28;
            else if (slot == 35) slot = 37;
        }

        inventory.setItem(49, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openMaterialFilters(Player player){
        Inventory inventory=Bukkit.createInventory(new MenuHolder("itembrowser:material_filters"),54,CC.color("&8Item Browser - Materials"));
        int[] slots={10,11,12,13,14,15,16,22};String[] names={"&eAll Materials","&7Mining Materials","&2Foraging Materials","&aFarming Materials","&cCombat Materials","&3Fishing Materials","&5Dungeon Materials","&dMagic Materials"};
        Material[] icons={Material.AMETHYST_SHARD,Material.IRON_INGOT,Material.OAK_LOG,Material.WHEAT,Material.ROTTEN_FLESH,Material.PRISMARINE_SHARD,Material.ECHO_SHARD,Material.ENCHANTED_BOOK};
        for(int i=0;i<MATERIAL_FILTERS.size();i++){String id=MATERIAL_FILTERS.get(i);int count=entriesFor(id).size();inventory.setItem(slots[i],item(icons[i],names[i],List.of("&7Items: &e"+count,"&7Pages: &e"+Math.max(1,(count+20)/21),"","&eClick to browse!","&8Category ID: "+id)));}
        inventory.setItem(48,item(Material.ARROW,"&eBack to Categories",List.of("&7Return to the main browser.")));inventory.setItem(49,item(Material.BARRIER,"&cClose",List.of()));fill(inventory);player.openInventory(inventory);
    }

    private void openWeaponFilters(Player player){
        Inventory inventory=Bukkit.createInventory(new MenuHolder("itembrowser:weapon_filters"),54,CC.color("&8Item Browser - Weapons"));
        int[] slots={10,12,14,16};
        for(int i=0;i<WEAPON_FILTERS.size();i++){
            String id=WEAPON_FILTERS.get(i);
            Category category=categories.get(id);
            int count=entriesFor(id).size();
            inventory.setItem(slots[i],item(category.icon(),category.display(),List.of("&7Items: &e"+count,
                    "&7Pages: &e"+Math.max(1,(count+20)/21),"","&eClick to browse!","&8Category ID: "+id)));
        }
        inventory.setItem(48,item(Material.ARROW,"&eBack to Categories",List.of("&7Return to the main browser.")));
        inventory.setItem(49,item(Material.BARRIER,"&cClose",List.of()));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openCategory(Player player, String categoryId, int page) {
        Category category = categories.getOrDefault(categoryId, categories.get("swords"));
        List<String> ids = entriesFor(category.id());
        int maxPage = Math.max(0, (int) Math.ceil(ids.size() / (double) CONTENT_SLOTS.length) - 1);
        page = Math.max(0, Math.min(page, maxPage));
        String categoryName = org.bukkit.ChatColor.stripColor(CC.color(category.display()));
        Inventory inventory = Bukkit.createInventory(new MenuHolder("itembrowser:" + category.id() + ":" + page), 54,
                CC.color((MATERIAL_FILTERS.contains(category.id())?"&8Materials - ":"&8Item Browser - ") + categoryName + " &7(" + (page + 1) + "/" + (maxPage + 1) + ")"));

        inventory.setItem(4, item(category.icon(), category.display(), List.of(
                "&7Browse this item category.",
                "&7Click an item to get one.",
                category.id().equals("custom_enchantments") ? "&7Shift-click an enchant to get level 3." : "&7Shift-click an item to get a stack.",
                "",
                "&7Page: &e" + (page + 1) + "&7/&e" + (maxPage + 1)
        )));

        int start = page * CONTENT_SLOTS.length;
        if (ids.isEmpty()) inventory.setItem(22, item(Material.BARRIER, "&cNo Items", List.of("&7This category has no registered items.")));
        for (int slotIndex = 0; slotIndex < CONTENT_SLOTS.length; slotIndex++) {
            int itemIndex = start + slotIndex;
            if (itemIndex >= ids.size()) break;
            String id = ids.get(itemIndex);
            if (category.id().equals("custom_enchantments")) {
                ItemStack book = customEnchantManager.createBook(id, 1);
                ItemMeta meta = book.getItemMeta();
                if (meta != null) {
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add("");
                    lore.add(CC.color("&eClick to get level 1."));
                    lore.add(CC.color("&eShift-click to get level 3."));
                    lore.add(CC.color("&8Custom Enchant ID: " + id));
                    meta.setLore(lore);
                    book.setItemMeta(meta);
                }
                inventory.setItem(CONTENT_SLOTS[slotIndex], book);
                continue;
            }
            ItemStack display = customItemManager.createItem(id, 1);
            customItemManager.rebuildLoreFor(player, display);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                CollectionManager.UnlockRequirement requirement = collectionManager.getUnlockRequirement(id);
                lore.add("");
                lore.add(CC.color("&7Category: &f" + pretty(category.id())));
                lore.add(CC.color("&7Type: &f" + itemType(id)));
                if (requirement != null) {
                    lore.add(CC.color("&7Unlock: &f" + collectionManager.getDisplayName(requirement.collectionId()) + " &e" + requirement.required()));
                } else {
                    lore.add(CC.color("&7Unlock: &aNo collection requirement"));
                }
                lore.add(CC.color("&eClick to get one."));
                lore.add(CC.color("&eShift-click to get 64."));
                lore.add(CC.color("&8Item ID: " + id));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inventory.setItem(CONTENT_SLOTS[slotIndex], display);
        }

        inventory.setItem(45, item(page > 0 ? Material.ARROW : Material.GRAY_DYE,
                page > 0 ? "&aPrevious Page" : "&7Previous Page",
                List.of(page > 0 ? "&7Go to page &e" + page + "&7." : "&cYou are on the first page.")));
        inventory.setItem(53, item(page < maxPage ? Material.ARROW : Material.GRAY_DYE,
                page < maxPage ? "&aNext Page" : "&7Next Page",
                List.of(page < maxPage ? "&7Go to page &e" + (page + 2) + "&7." : "&cNo more pages.")));
        inventory.setItem(48, item(Material.ARROW, "&eBack to Categories", List.of("&7Return to categories.")));
        inventory.setItem(49, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        inventory.setItem(50, item(Material.COMPASS, "&eSearch / Filter", List.of("&7Search and filters are not active.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private boolean canUse(Player player) {
        return player.hasPermission("fakevival.admin.itembrowser") || player.hasPermission("fakevival.developer.itembrowser") || player.hasPermission("fakevival.devcontrols.admin") || player.hasPermission("smp.dev");
    }

    private String itemType(String id) {
        String configuredType = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("custom-items.items." + id + ".type", "");
        if (configuredType.equalsIgnoreCase("COLLECTION_MATERIAL")) return "Collection Material";
        if (configuredType.equalsIgnoreCase("RECIPE_MATERIAL")) return "Recipe Material";
        if (configuredType.equalsIgnoreCase("DUNGEON_MATERIAL")) return "Dungeon Material";
        if (configuredType.equalsIgnoreCase("MONSTER_MATERIAL")) return "Monster Material";
        Material material = customItemManager.getMaterial(id);
        if (material.name().contains("SWORD") || material.name().contains("AXE") || material.name().contains("MACE") || material.name().contains("BOW")) return "Weapon";
        if (material.name().contains("HELMET") || material.name().contains("CHESTPLATE") || material.name().contains("LEGGINGS") || material.name().contains("BOOTS") || material == Material.SHIELD) return "Gear";
        String ability = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("custom-items.items." + id + ".ability.type", "none");
        if (!ability.isBlank() && !ability.equalsIgnoreCase("none")) return "Ability Item";
        return "Item";
    }

    private BrowserView parseView(String type) {
        try {
            String[] parts = type.substring("itembrowser:".length()).split(":", 2);
            if (parts.length != 2) return null;
            return new BrowserView(parts[0], Integer.parseInt(parts[1]));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void registerCategories() {
        addCategory("weapons", "&cWeapons", Material.DIAMOND_SWORD);
        addCategory("accessories", "&dAccessories", Material.NETHER_STAR);
        addCategory("event_items", "&6Event Items", Material.SUNFLOWER);
        addCategory("special_items", "&cSpecial Items", Material.NETHER_STAR);
        addCategory("swords", "&cSwords", Material.DIAMOND_SWORD);
        addCategory("daggers", "&8Daggers", Material.IRON_SWORD);
        addCategory("katanas", "&bKatanas", Material.NETHERITE_SWORD);
        addCategory("tools", "&6Tools", Material.DIAMOND_PICKAXE);
        addCategory("bows", "&aBows", Material.BOW);
        addCategory("armor", "&9Armor", Material.DIAMOND_CHESTPLATE);
        addCategory("shields", "&fShields", Material.SHIELD);
        addCategory("food", "&aFood", Material.GOLDEN_CARROT);
        addCategory("rings", "&eRings", Material.GOLD_NUGGET);
        addCategory("necklaces", "&dNecklaces", Material.AMETHYST_SHARD);
        addCategory("charms", "&aCharms", Material.TOTEM_OF_UNDYING);
        addCategory("totems", "&6Totems", Material.TOTEM_OF_UNDYING);
        addCategory("trinkets", "&5Trinkets", Material.TOTEM_OF_UNDYING);
        addCategory("potions", "&dPotions", Material.POTION);
        addCategory("fishing", "&3Fishing", Material.FISHING_ROD);
        addCategory("custom_fish", "&bCustom Fish", Material.TROPICAL_FISH);
        addCategory("cooking_ingredients", "&6Cooking Ingredients", Material.BOWL);
        addCategory("cooking", "&aCustom Food", Material.CAKE);
        addCategory("materials", "&eMaterials", Material.AMETHYST_SHARD);
        addCategory("reforge_stones", "&bReforge Stones", Material.ANVIL);
        addCategory("mining_materials", "&7Mining Materials", Material.IRON_INGOT);
        addCategory("foraging_materials", "&2Foraging Materials", Material.OAK_LOG);
        addCategory("farming_materials", "&aFarming Materials", Material.WHEAT);
        addCategory("dungeon_materials", "&5Dungeon Materials", Material.ECHO_SHARD);
        addCategory("combat_materials", "&cCombat Materials", Material.ROTTEN_FLESH);
        addCategory("fishing_materials", "&3Fishing Materials", Material.PRISMARINE_SHARD);
        addCategory("magic_materials", "&dMagic Materials", Material.AMETHYST_SHARD);
        addCategory("companions", "&bCompanions", Material.BONE);
        addCategory("staffs_wands", "&bWands/Staffs", Material.BLAZE_ROD);
        addCategory("farming_equipment", "&aFarming Equipment", Material.DIAMOND_HOE);
        addCategory("custom_enchantments", "&dCustom Enchantments", Material.ENCHANTED_BOOK);
    }

    private void addCategory(String id, String display, Material icon) {
        categories.put(id, new Category(id, display, icon));
    }

    private List<String> entriesFor(String category) {
        return entryCache.computeIfAbsent(category, key -> buildEntries(key));
    }

    private List<String> buildEntries(String category) {
        if (category.equals("custom_enchantments")) return List.copyOf(customEnchantManager.ids());
        if(category.equals("materials"))return customItemManager.getItemIds().stream().filter(id->{String type=club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("custom-items.items."+id+".type","").toUpperCase(Locale.ROOT);return type.endsWith("_MATERIAL")||type.equals("MATERIAL");}).sorted().toList();
        if(category.equals("weapons"))return customItemManager.getItemIds().stream().filter(id->Set.of("swords","daggers","katanas","bows").contains(categoryFor(id))).sorted().toList();
        if(category.equals("accessories"))return customItemManager.getItemIds().stream().filter(id->Set.of("rings","necklaces","charms","totems","trinkets").contains(categoryFor(id))).sorted().toList();
        if(category.equals("special_items"))return customItemManager.getItemIds().stream().filter(id->Set.of("SPECIAL","DEVELOPER_NOTEBOOK").contains(configuredType(id))).sorted().toList();
        if(category.equals("event_items"))return customItemManager.getItemIds().stream().filter(id->Set.of("EVENT_ITEM","EVENT_TOKEN","TOKEN").contains(configuredType(id))).sorted().toList();
        return customItemManager.getItemIds().stream()
                .filter(id -> categoryFor(id).equals(category))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    public void rebuildCache() { entryCache.clear(); }

    public boolean containsInCategory(String category, String itemId) {
        return category != null && itemId != null && entriesFor(normalize(category)).contains(normalize(itemId));
    }

    public ValidationResult validateRegistry() {
        rebuildCache();
        int warnings = 0;
        int pages = 0;
        Set<String> seen = new java.util.HashSet<>();
        List<String> invalidSpecialMaterials = new java.util.ArrayList<>();
        Set<String> themedStockLoreIds = new java.util.HashSet<>();
        Set<String> javaRegistryLoreIds = new java.util.HashSet<>();
        Set<String> blockedVendorLoreIds = new java.util.HashSet<>();
        int invalidFish = 0, invalidFood = 0, invalidCooking = 0, invalidFishing = 0, invalidEvent = 0, validSpecial = 0;
        for (String id : customItemManager.getItemIds()) {
            if (!seen.add(id)) warnings++;
            String category = categoryFor(id);
            if (!categories.containsKey(category)) warnings++;
            ItemStack preview = customItemManager.createItem(id, 1);
            if (preview == null || preview.getType().isAir() || !id.equals(customItemManager.getItemId(preview))) warnings++;
            List<String> rawLore = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                    .getStringList("custom-items.items." + id + ".lore");
            List<String> visibleLore = preview != null && preview.hasItemMeta() && preview.getItemMeta().hasLore()
                    ? preview.getItemMeta().getLore() : List.of();
            if (VendorLoreSanitizer.containsPhrase(rawLore, "themed stock from")
                    || VendorLoreSanitizer.containsPhrase(visibleLore, "themed stock from")) themedStockLoreIds.add(id);
            if (VendorLoreSanitizer.containsPhrase(rawLore, "java registry vendor item")
                    || VendorLoreSanitizer.containsPhrase(visibleLore, "java registry vendor item")) javaRegistryLoreIds.add(id);
            if (VendorLoreSanitizer.containsBlockedLore(rawLore)
                    || VendorLoreSanitizer.containsBlockedLore(visibleLore)) blockedVendorLoreIds.add(id);
            if (preview != null && "SPECIAL".equalsIgnoreCase(customItemManager.getCustomItemRarity(preview))) {
                String type = customItemManager.getCustomItemType(preview);
                if (isMaterialType(type)) invalidSpecialMaterials.add(id);
                else if (Set.of("CUSTOM_FISH", "FISH").contains(type)) invalidFish++;
                else if (Set.of("CUSTOM_FOOD", "FOOD").contains(type)) invalidFood++;
                else if (type.equals("COOKING_INGREDIENT")) invalidCooking++;
                else if (Set.of("FISHING_ITEM", "FISHING_BAIT", "BAIT", "FISHING_GEAR_MATERIAL", "FISHING_ROD").contains(type)) invalidFishing++;
                else if (Set.of("EVENT_TOKEN", "EVENT_MATERIAL", "EVENT_ITEM", "NORMAL_EVENT_ITEM", "EVENT_COLLECTIBLE").contains(type)) invalidEvent++;
                else validSpecial++;
            }
        }
        for (String id : club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults60.materialIds()) {
            String base = "custom-items.items." + id;
            if (!customItemManager.itemExists(id)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".mmo.random-roll", true)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".random-rarity", true)
                    || !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(base + ".mmo.stats").isEmpty()) warnings++;
        }
        for (String id : club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100.materialIds()) {
            String base = "custom-items.items." + id;
            if (!customItemManager.itemExists(id)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".mmo.random-roll", true)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".random-rarity", true)
                    || !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(base + ".mmo.stats").isEmpty()) warnings++;
        }
        for (String id : club.abelix.games.fakevival.config.defaults.MissingProgressionDefaults100B.materialIds()) {
            String base = "custom-items.items." + id;
            if (!customItemManager.itemExists(id)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".mmo.random-roll", true)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".random-rarity", true)
                    || !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(base + ".mmo.stats").isEmpty()) warnings++;
        }
        for (String id : club.abelix.games.fakevival.config.defaults.DecorativeCollectionDefaults200.materialIds()) {
            String base = "custom-items.items." + id;
            if (!customItemManager.itemExists(id)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".mmo.random-roll", true)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".random-rarity", true)
                    || !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(base + ".mmo.stats").isEmpty()) warnings++;
        }
        for (String id : club.abelix.games.fakevival.config.defaults.RequestedRecipes200Defaults.materialIds()) {
            String base = "custom-items.items." + id;
            if (!customItemManager.itemExists(id)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".mmo.random-roll", true)
                    || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(base + ".random-rarity", true)
                    || !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(base + ".mmo.stats").isEmpty()) warnings++;
        }
        boolean royalScrapValid = validateRoyalScrap();
        if (!royalScrapValid) warnings++;
        for (Category category : categories.values()) {
            int count = entriesFor(category.id()).size();
            pages += Math.max(1, (count + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length);
        }
        ItemStack notebook = customItemManager.itemExists("developer_notebook")
                ? customItemManager.createItem("developer_notebook", 1) : null;
        boolean notebookSpecialValid = notebook != null
                && "developer_notebook".equals(customItemManager.getItemId(notebook))
                && "SPECIAL".equalsIgnoreCase(customItemManager.getCustomItemRarity(notebook));
        if (!notebookSpecialValid) warnings++;
        int visibleThemedStockLoreItems = 0, visibleJavaRegistryLoreItems = 0, visibleBlockedVendorLoreItems = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (ItemStack stack : online.getInventory().getContents()) {
                if (stack == null || stack.getType().isAir() || !stack.hasItemMeta() || !stack.getItemMeta().hasLore()) continue;
                List<String> lore = stack.getItemMeta().getLore();
                if (VendorLoreSanitizer.containsPhrase(lore, "themed stock from")) visibleThemedStockLoreItems++;
                if (VendorLoreSanitizer.containsPhrase(lore, "java registry vendor item")) visibleJavaRegistryLoreItems++;
                if (VendorLoreSanitizer.containsBlockedLore(lore)) visibleBlockedVendorLoreItems++;
            }
        }
        warnings += blockedVendorLoreIds.size() + visibleBlockedVendorLoreItems;
        warnings += invalidSpecialMaterials.size() + invalidFish + invalidFood + invalidCooking + invalidFishing + invalidEvent;
        return new ValidationResult(customItemManager.getItemIds().size(), categories.size(), pages, warnings,
                invalidSpecialMaterials.size(), List.copyOf(invalidSpecialMaterials), invalidFish, invalidFood,
                invalidCooking, invalidFishing, invalidEvent, validSpecial, notebookSpecialValid,
                themedStockLoreIds.size() + visibleThemedStockLoreItems,
                javaRegistryLoreIds.size() + visibleJavaRegistryLoreItems,
                blockedVendorLoreIds.size() + visibleBlockedVendorLoreItems, royalScrapValid);
    }

    public record ValidationResult(int items, int categories, int pages, int warnings,
                                   int specialMaterials, List<String> invalidSpecialMaterials,
                                   int invalidSpecialCustomFish, int invalidSpecialCustomFood,
                                   int invalidSpecialCookingIngredients, int invalidSpecialFishingItems,
                                   int invalidSpecialEventItems, int validSpecialItems,
                                   boolean developerNotebookSpecialValid, int themedStockLoreItems,
                                   int javaRegistryVendorLoreItems, int blockedVendorLoreItems,
                                   boolean royalScrapValid) {}

    private boolean validateRoyalScrap() {
        String id = "royal_scrap";
        String base = "custom-items.items." + id;
        var config = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        if (!customItemManager.itemExists(id)
                || !"MATERIAL".equalsIgnoreCase(config.getString(base + ".type", ""))
                || !"dungeon_materials".equalsIgnoreCase(config.getString(base + ".category", ""))
                || !"DUNGEON_MATERIAL".equalsIgnoreCase(config.getString(base + ".material-category", ""))
                || !"RARE".equalsIgnoreCase(config.getString(base + ".rarity", ""))
                || !config.getBoolean(base + ".fixed-rarity", false)
                || config.getInt(base + ".max-stack-size", 0) != 64
                || config.getBoolean(base + ".random-stats-allowed", true)
                || config.getBoolean(base + ".random-rarity-allowed", true)
                || config.getBoolean(base + ".random-rarity", true)
                || config.getBoolean(base + ".mmo.random-roll", true)
                || config.getBoolean(base + ".reforge-allowed", true)
                || config.getBoolean(base + ".enchant-allowed", true)
                || config.getBoolean(base + ".special-item", false)
                || config.getBoolean(base + ".developer-notebook", false)
                || !config.getStringList(base + ".mmo.stats").isEmpty()) return false;
        ItemStack item = customItemManager.createItem(id, 1);
        if (item == null || item.getType() != Material.IRON_NUGGET
                || !id.equals(customItemManager.getItemId(item))
                || !"MATERIAL".equals(customItemManager.getCustomItemType(item))
                || !"RARE".equalsIgnoreCase(customItemManager.getCustomItemRarity(item))) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        List<String> lore = item.getItemMeta().getLore();
        String last = lore == null || lore.isEmpty() ? "" : org.bukkit.ChatColor.stripColor(lore.getLast());
        return "RARE MATERIAL".equalsIgnoreCase(last);
    }

    private boolean isMaterialType(String type) {
        if (type == null) return false;
        String normalized = type.toUpperCase(Locale.ROOT);
        return normalized.equals("MATERIAL") || normalized.endsWith("_MATERIAL")
                || normalized.equals("COOKING_INGREDIENT");
    }
    private String configuredType(String id){return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("custom-items.items."+id+".type","").toUpperCase(Locale.ROOT);}

    private String categoryFor(String id) {
        String base = "custom-items.items." + id;
        String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".category", "");
        String customType = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString(base + ".type", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".mmo.type", "")));
        if (Set.of("pickaxe", "drill", "shovel", "farming_hoe", "hoe", "foraging_axe", "axe", "multi_tool").contains(customType)) return "tools";
        if (Set.of("helmet", "chestplate", "leggings", "boots", "armor").contains(customType)) return "armor";
        if (Set.of("wand", "staff").contains(customType)) return "staffs_wands";
        if (Set.of("potion", "dungeon_potion", "consumable_potion").contains(customType)) return "potions";
        if (Set.of("bow", "shortbow", "longbow", "crossbow").contains(customType)) return "bows";
        if (Set.of("sword", "greatsword").contains(customType)) return "swords";
        if (normalize(configured).equals("dungeon_weapons") && Set.of("dagger", "katana").contains(customType)) return "swords";
        if (!configured.isBlank()) return normalize(configured);
        String lower = id.toLowerCase(Locale.ROOT);
        Material material = customItemManager.getMaterial(id);
        String type = material.name();
        if (lower.contains("fishing") || lower.contains("fish") || lower.contains("rod") || lower.contains("bait") || lower.contains("tackle")) return "fishing";
        if (lower.contains("companion") || lower.contains("snare") || lower.contains("treat")) return "companions";
        if (lower.contains("staff") || lower.contains("wand") || lower.contains("focus") || lower.contains("scepter")) return "staffs_wands";
        if (lower.contains("farm") || lower.contains("harvest") || lower.contains("crop") || lower.contains("hoe") || lower.contains("sprinkler")) return "farming_equipment";
        if (lower.contains("dagger")) return "daggers";
        if (lower.contains("katana")) return "katanas";
        if (lower.contains("ring")) return "rings";
        if (lower.contains("necklace") || lower.contains("amulet")) return "necklaces";
        if (lower.contains("potion") || type.equals("POTION") || type.equals("SPLASH_POTION")) return "potions";
        if (lower.contains("charm") || lower.contains("totem") || lower.contains("compass") || lower.contains("rune")) return "trinkets";
        if (type.endsWith("_SWORD") || type.equals("TRIDENT") || type.equals("MACE")) return "swords";
        if (type.equals("BOW") || type.equals("CROSSBOW")) return "bows";
        if (type.endsWith("_PICKAXE") || type.endsWith("_AXE") || type.endsWith("_SHOVEL") || type.endsWith("_HOE")) return "tools";
        if (type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE") || type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS")) return "armor";
        if (type.equals("SHIELD")) return "shields";
        if (type.equals("COOKIE") || type.equals("BREAD") || type.contains("CARROT") || type.contains("APPLE")) return "food";
        return "trinkets";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "_").replace("-", "_");
    }

    private String pretty(String value) {
        if (value == null || value.isBlank()) return "Item";
        String text = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = org.bukkit.ChatColor.stripColor(line);
            if (clean != null && clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(lore.stream().map(CC::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }

    private record Category(String id, String display, Material icon) {}

    private record BrowserView(String category, int page) {}
}
