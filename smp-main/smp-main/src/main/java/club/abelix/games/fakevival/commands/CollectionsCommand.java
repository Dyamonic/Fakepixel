package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.CollectionManager;
import club.abelix.games.fakevival.managers.CustomItemManager;
import club.abelix.games.fakevival.managers.RecipeManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.recipes.CustomRecipe;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import club.abelix.games.fakevival.registries.VanillaBazaarRegistry;

@SuppressWarnings("deprecation")
public final class CollectionsCommand implements CommandExecutor, Listener {
    private final JavaPlugin plugin;
    private final CollectionManager collectionManager;
    private final CustomItemManager customItemManager;
    private final RecipeManager recipeManager;
    private final NamespacedKey sourceTypeKey,sourceIdKey,collectionIdKey;

    public CollectionsCommand(JavaPlugin plugin, CollectionManager collectionManager, CustomItemManager customItemManager, RecipeManager recipeManager) {
        this.plugin = plugin;
        this.collectionManager = collectionManager;
        this.customItemManager = customItemManager;
        this.recipeManager = recipeManager;
        this.sourceTypeKey=new NamespacedKey(plugin,"collection_source_type");this.sourceIdKey=new NamespacedKey(plugin,"collection_source_id");this.collectionIdKey=new NamespacedKey(plugin,"collection_id");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.collections.admin")) return CommandUtil.noPermission(plugin, sender);
            collectionManager.rebuildVanillaCollectionLinks();var result=collectionManager.validateVanillaLinks();
            sender.sendMessage(CC.color("&a[SMP] Collections validation complete: collections="+result.collections()+", mappings="+result.vanillaLinks()+", warnings="+result.warnings()+"."));
            sender.sendMessage(CC.color("&7Source tracking: pickup awards=&cdisabled&7, block/kill/fishing/crafting=&aonce&7, player-drop PDC=&aenabled&7."));
            long enchantRewards = collectionManager.getCollectionIds().stream().flatMap(id -> collectionManager.getUnlocks(id).stream()).filter(CollectionManager.CollectionUnlock::isEnchant).count();
            sender.sendMessage(CC.color("&7Enchantment rewards: registered=&e" + enchantRewards + "&7, requirement source=&aenchant registry&7, reward lore=&aenabled&7."));
            return true;
        }
        if(args.length>0&&args[0].equalsIgnoreCase("debug")){if(!sender.hasPermission("fakevival.collections.admin"))return CommandUtil.noPermission(plugin,sender);if(args.length<3){sender.sendMessage(CC.color("&cUsage: /collections debug <player> <materialOrCollectionId>"));return true;}Player target=Bukkit.getPlayerExact(args[1]);if(target==null){sender.sendMessage(CC.color("&cPlayer not found."));return true;}Material material=Material.matchMaterial(args[2]);String internal=material==null?collectionManager.internalCollectionId(args[2]):collectionManager.getPrimaryCollectionId(material);List<CollectionManager.CollectionUnlock>tiers=collectionManager.getUnlocks(internal);int progress=collectionManager.getProgress(target,internal),tier=collectionManager.getUnlockedTier(target,internal),next=nextRequirement(tiers,progress);sender.sendMessage(CC.color("&7Source: &e"+(material==null?args[2]:material.name())));sender.sendMessage(CC.color("&7Mapped Collection: &b"+collectionManager.externalCollectionId(internal)));sender.sendMessage(CC.color("&7Player Progress: &a"+progress));sender.sendMessage(CC.color("&7Tier: &e"+tier));sender.sendMessage(CC.color("&7Next Required: &e"+next));return true;}
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        openCategories(player, 0);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type))) return;
        if (!type.equals("collections") && !type.startsWith("collections-main:") && !type.startsWith("collections-vanilla-") && !type.startsWith("collection:") && !type.startsWith("recipe:")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String pdcCollection=clicked.hasItemMeta()?clicked.getItemMeta().getPersistentDataContainer().get(collectionIdKey,PersistentDataType.STRING):null;
        String pdcSource=clicked.hasItemMeta()?clicked.getItemMeta().getPersistentDataContainer().get(sourceIdKey,PersistentDataType.STRING):null;

        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT)
                : "";
        int slot = event.getRawSlot();
        String[] view = type.split(":", 4);
        if (slot == 49 && name.equals("close")) { player.closeInventory(); return; }
        if (slot == 49 && clicked.getType() == Material.BARRIER && name.equals("back")) {
            if(type.startsWith("collections-vanilla-items:"))openCategories(player,0);
            else if(type.startsWith("collections-vanilla-path:")){Material material=Material.matchMaterial(part(view,1));if(material!=null)VanillaBazaarRegistry.byMaterial(material).ifPresent(e->openCollections(player,e.subcategory(),0));}
            else if (type.startsWith("collection:")) {String material=part(view,3);if(!material.isBlank())openCollectionPaths(player,Material.matchMaterial(material),0);else openCategories(player,0);}
            else if (type.startsWith("recipe:")) openCollection(player, part(view, 1), parsePage(view, 2));
            else player.closeInventory();
            return;
        }
        if (slot == 45 && name.equals("previous page")) {
            openCollectionPage(player, type, Math.max(0, currentPage(type) - 1));
            return;
        }
        if (slot == 53 && name.equals("next page")) {
            openCollectionPage(player, type, currentPage(type) + 1);
            return;
        }
        if(pdcSource!=null&&pdcCollection!=null&&type.startsWith("collections-vanilla-items:")){openCollectionPaths(player,Material.matchMaterial(pdcSource),0);return;}
        if(pdcCollection!=null&&type.startsWith("collections-vanilla-path:")){Material source=Material.matchMaterial(part(view,1));openCollection(player,collectionManager.internalCollectionId(pdcCollection),0,source);return;}

        String categoryId = loreValue(clicked, "Category: ");
        if (categoryId != null) {
            openCollections(player, categoryId, 0);
            return;
        }
        String vanillaMaterial=loreValue(clicked,"Vanilla Material: ");if(vanillaMaterial!=null){Material material=Material.matchMaterial(vanillaMaterial);if(material!=null)openCollectionPaths(player,material,0);return;}

        String collectionId = loreValue(clicked, "Collection: ");
        if (collectionId != null) {
            Material source=type.startsWith("collections-vanilla-path:")?Material.matchMaterial(part(view,1)):null;openCollection(player, collectionId, 0,source);
            return;
        }

        String customItemId = loreValue(clicked, "Recipe: ");
        if (customItemId != null && !customItemId.isBlank()) {
            String sourceCollection = type.startsWith("collection:") ? part(view, 1) : "";
            openRecipe(player, sourceCollection, currentPage(type), customItemId);
        }
    }

    @EventHandler public void onInventoryDrag(InventoryDragEvent event){if(event.getInventory().getHolder() instanceof MenuHolder(String type)&&(type.equals("collections")||type.startsWith("collections-main:")||type.startsWith("collections-vanilla-")||type.startsWith("collection:")||type.startsWith("recipe:")))event.setCancelled(true);}

    private void openCategories(Player player, int page) {
        int[] slots = {10,11,12,13,14,15,16,20,21,22,23,24};
        List<VanillaBazaarRegistry.Subcategory> categories = VanillaBazaarRegistry.subcategories();
        int totalPages = totalPages(categories.size(), slots.length);
        int safePage = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("collections-main:" + safePage), 54, CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("collections.menu.title", "&8Collections")));
        inventory.setItem(4, item(Material.BOOK, "&bCollections", List.of(
                "&7View resource collections by category.",
                "&7Each tier unlocks recipes or rewards.",
                "",
                "&eClick a category to view collections."
        )));

        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < categories.size(); index++) {
            VanillaBazaarRegistry.Subcategory category=categories.get(start+index);String id=category.id();List<VanillaBazaarRegistry.Entry> vanilla=VanillaBazaarRegistry.entries(id);java.util.LinkedHashSet<String> collections=new java.util.LinkedHashSet<>();for(var entry:vanilla)collections.addAll(collectionManager.getCollectionsRelatedTo(entry.material()));int unlocked=0;
            for (String collectionId : collections) {
                if (collectionManager.getUnlockedTier(player, collectionId) > 0) unlocked++;
            }
            List<String> lore = new ArrayList<>();
            lore.add("&7View collections connected to this category.");
            lore.add("&7Vanilla Items: &e"+vanilla.size());
            lore.add("&7Collections: &e" + collections.size());
            lore.add("&7Unlocked: &a" + unlocked + "&7/&a" + collections.size());
            lore.add("");
            lore.add("&eClick to browse!");
            lore.add("&8Category: " + id);
            inventory.setItem(slots[index], item(category.icon(), "&e"+category.displayName(), lore));
        }

        addNavigation(inventory, safePage, totalPages);
        inventory.setItem(49, item(Material.BARRIER, "&cClose", List.of("&7Close the Collections menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openCollections(Player player, String categoryId, int page) {
        categoryId = normalize(categoryId);
        final String selectedCategory=categoryId;
        int[] slots = contentSlots();
        List<VanillaBazaarRegistry.Entry> entries=VanillaBazaarRegistry.entries(categoryId);
        int totalPages = totalPages(entries.size(), slots.length);
        int safePage = Math.clamp(page, 0, totalPages - 1);
        String display=VanillaBazaarRegistry.subcategories().stream().filter(s->s.id().equals(selectedCategory)).map(VanillaBazaarRegistry.Subcategory::displayName).findFirst().orElse("Vanilla");
        Inventory inventory = Bukkit.createInventory(new MenuHolder("collections-vanilla-items:" + categoryId + ":" + safePage), 54, CC.color("&8Collections - " + display));

        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < entries.size(); index++) {
            Material material=entries.get(start+index).material();List<String> collections=collectionManager.getCollectionsRelatedTo(material);int progress=collections.stream().mapToInt(id->collectionManager.getProgress(player,id)).max().orElse(0);long unlocked=collections.stream().filter(id->collectionManager.getUnlockedTier(player,id)>0).count();
            List<String> lore = new ArrayList<>();
            lore.add("&7Material: &e"+material.name());lore.add("&7Related Collections: &a"+collections.size());lore.add("&7Progress: &a"+format(progress));lore.add("&7Unlocked: &a"+unlocked+"&7/&a"+collections.size());
            lore.add("");
            lore.add("&eClick to view collection paths!");lore.add("&8Vanilla Material: "+material.name());
            inventory.setItem(slots[index], collectionButton(item(material,"&f"+pretty(material.name()), lore),material,collectionManager.getPrimaryCollectionId(material)));
        }

        addNavigation(inventory, safePage, totalPages);
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openCollection(Player player, String collectionId, int page) {
        openCollection(player,collectionId,page,null);
    }
    private void openCollection(Player player, String collectionId, int page,Material source) {
        collectionId = normalize(collectionId);
        int progress = collectionManager.getProgress(player, collectionId);
        List<CollectionManager.CollectionUnlock> unlocks = collectionManager.getUnlocks(collectionId);
        int[] slots = contentSlots();
        int totalPages = totalPages(unlocks.size(), slots.length);
        int safePage = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("collection:" + collectionId + ":" + safePage+":"+(source==null?"":source.name())), 54, CC.color("&8" + strip(collectionManager.getDisplayName(collectionId))));
        int unlockedTiers = collectionManager.getUnlockedTier(player, collectionId);
        inventory.setItem(4, item(collectionManager.getIcon(collectionId), collectionManager.getDisplayName(collectionId), List.of(
                "&7Progress: &a" + format(progress) + "&7/&e" + format(nextRequirement(unlocks, progress)),
                progressBar(progress, nextRequirement(unlocks, progress)),
                "&7Tier: &e" + unlockedTiers + "&7/&e" + unlocks.stream().map(CollectionManager.CollectionUnlock::tierNumber).max(Integer::compareTo).orElse(0),
                "&7Total Collected: &b"+format(progress),
                "",
                "&7Tracked: &f" + String.join(", ", collectionManager.getTrackedMaterials(collectionId).stream().map(this::pretty).toList())
        )));

        int start = safePage * slots.length;
        for (int index = 0; index < slots.length && start + index < unlocks.size(); index++) {
            CollectionManager.CollectionUnlock unlock = unlocks.get(start + index);
            boolean unlocked = progress >= unlock.amount();
            ItemStack display = rewardDisplay(unlock);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("&8" + strip(collectionManager.getDisplayName(collectionId)) + " " + collectionManager.toRoman(unlock.tierNumber()));
                lore.add(unlocked ? "&aUnlocked" : "&cLocked");
                lore.add("&7Progress: &e" + format(Math.min(progress, unlock.amount())) + "&7/&e" + format(unlock.amount()));
                lore.add("");
                lore.add("&7Reward:");
                lore.add(unlock.isEnchant() ? "&aUnlocks Enchantment: &9" + strip(unlock.name()) : "&f" + strip(unlock.name()));
                if (unlock.isCustomItem()) lore.add("&8Type: Recipe Unlock");
                else lore.add("&8Type: " + pretty(unlock.rewardType()));
                lore.add("");
                if (unlock.isCustomItem()) lore.add("&eClick to view recipe.");
                else lore.add(unlocked ? "&aReward unlocked." : "&7Reach this tier to unlock.");
                lore.add("&8Recipe: " + unlock.customItemId());
                meta.setDisplayName(CC.color((unlocked ? "&a" : "&c") + strip(collectionManager.getDisplayName(collectionId)) + " " + collectionManager.toRoman(unlock.tierNumber())));
                meta.setLore(color(lore));
                display.setItemMeta(meta);
            }
            inventory.setItem(slots[index], display);
        }

        addNavigation(inventory, safePage, totalPages);
        fill(inventory);
        player.openInventory(inventory);
    }

    private void openCollectionPaths(Player player,Material material,int page){
        if(material==null)return;List<String> collections=collectionManager.getCollectionsRelatedTo(material);int[] slots=contentSlots();int pages=totalPages(collections.size(),slots.length),safe=Math.clamp(page,0,pages-1);Inventory inventory=Bukkit.createInventory(new MenuHolder("collections-vanilla-path:"+material.name()+":"+safe),54,CC.color("&8Collection - "+pretty(material.name())));int start=safe*slots.length;
        for(int i=0;i<slots.length&&start+i<collections.size();i++){String id=collections.get(start+i);int progress=collectionManager.getProgress(player,id);List<CollectionManager.CollectionUnlock> unlocks=collectionManager.getUnlocks(id);int tier=collectionManager.getUnlockedTier(player,id),next=nextRequirement(unlocks,progress);List<String> lore=new ArrayList<>();lore.add("&7Collection: &e"+strip(collectionManager.getDisplayName(id)));lore.add("&7Progress: &a"+format(progress)+"&7/&e"+format(next));lore.add("&7Tier: &e"+tier+"&7/&e"+unlocks.stream().map(CollectionManager.CollectionUnlock::tierNumber).max(Integer::compareTo).orElse(0));lore.add(progressBar(progress,next));lore.add("");lore.add("&eUnlocks:");for(var unlock:unlocks.stream().limit(5).toList())lore.add((progress>=unlock.amount()?"&a✔ ":"&c✖ ")+strip(unlock.name()));List<String> requirements=collectionManager.requirementLines(player,id);if(!requirements.isEmpty()){lore.add("");lore.add("&eRequirements:");lore.addAll(requirements);}lore.add("");lore.add("&eClick to view details!");lore.add("&8Collection: "+id);inventory.setItem(slots[i],item(collectionManager.getIcon(id),collectionManager.getDisplayName(id),lore));}
        for(int slot:slots){ItemStack shown=inventory.getItem(slot);if(shown!=null&&!shown.getType().isAir()){String id=loreValue(shown,"Collection: ");if(id!=null)inventory.setItem(slot,collectionButton(shown,material,id));}}
        addNavigation(inventory,safe,pages);fill(inventory);player.openInventory(inventory);
    }

    private ItemStack rewardDisplay(CollectionManager.CollectionUnlock unlock) {
        if (unlock.isCustomItem() && customItemManager.itemExists(unlock.customItemId())) {
            return customItemManager.createItem(unlock.customItemId(), 1);
        }
        Material material = switch (normalize(unlock.rewardType())) {
            case "coins", "money" -> Material.GOLD_INGOT;
            case "xp", "skill_xp", "experience" -> Material.EXPERIENCE_BOTTLE;
            case "permission", "perk" -> Material.NETHER_STAR;
            case "enchant" -> Material.ENCHANTED_BOOK;
            case "command" -> Material.COMMAND_BLOCK;
            case "bazaar" -> Material.EMERALD;
            case "minion" -> Material.PLAYER_HEAD;
            case "sack" -> Material.BUNDLE;
            default -> Material.PAPER;
        };
        return item(material, unlock.name(), List.of());
    }

    private void openRecipe(Player player, String collectionId, int collectionPage, String customItemId) {
        customItemId = normalize(customItemId);
        if (customItemId.isBlank()) return;
        CustomRecipe configuredRecipe = recipeManager == null ? null : recipeManager.getRecipe(customItemId).orElse(null);
        boolean unlocked = collectionManager.isCustomItemUnlocked(player, customItemId);
        CollectionManager.UnlockRequirement requirement = collectionManager.getUnlockRequirement(customItemId);

        Inventory inventory = Bukkit.createInventory(new MenuHolder("recipe:" + normalize(collectionId) + ":" + Math.max(0, collectionPage) + ":" + customItemId), 54, CC.color("&8Recipe: " + recipeName(customItemId)));
        int[] recipeSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        List<String> shape = configuredRecipe == null
                ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList("custom-items.items." + customItemId + ".recipe.shape")
                : configuredRecipe.shape();
        ConfigurationSection ingredients = configuredRecipe == null
                ? club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("custom-items.items." + customItemId + ".recipe.ingredients")
                : null;
        for (int row = 0; row < 3; row++) {
            String line = row < shape.size() ? shape.get(row) : "   ";
            for (int column = 0; column < 3; column++) {
                char key = column < line.length() ? line.charAt(column) : ' ';
                if (key == ' ') continue;
                if (configuredRecipe != null) {
                    String value = configuredRecipe.ingredients().get(key);
                    if (value == null) continue;
                    int required = configuredRecipe.ingredientAmount(key);
                    ItemStack ingredient = recipeManager.createIngredient(value, required);
                    ItemMeta ingredientMeta = ingredient.getItemMeta();
                    if (ingredientMeta != null) {
                        List<String> lore = ingredientMeta.hasLore() ? new ArrayList<>(Objects.requireNonNull(ingredientMeta.getLore())) : new ArrayList<>();
                        int owned = recipeManager.countIngredient(player, value);
                        lore.add("");
                        lore.add(CC.color("&7Required: &e" + required));
                        lore.add(CC.color("&7You have: " + (owned >= required ? "&a" : "&c") + owned));
                        if (owned < required) lore.add(CC.color("&cMissing: " + (required - owned)));
                        ingredientMeta.setLore(lore);
                        ingredient.setItemMeta(ingredientMeta);
                    }
                    inventory.setItem(recipeSlots[(row * 3) + column], ingredient);
                } else if (ingredients != null) {
                    Material material = parseMaterial(ingredients.getString(String.valueOf(key), "AIR"));
                    if (material != Material.AIR) inventory.setItem(recipeSlots[(row * 3) + column], item(material, "&f" + pretty(material.name()), List.of("&7Recipe ingredient.")));
                }
            }
        }

        inventory.setItem(24, item(unlocked ? Material.LIME_DYE : Material.RED_DYE, unlocked ? "&aRecipe Unlocked" : "&cRecipe Locked", recipeStatusLore(player, requirement, unlocked)));
        ItemStack result = customItemManager.createItem(customItemId, 1);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.getLore())) : new ArrayList<>();
            lore.add("");
            lore.add(CC.color(unlocked ? "&aYou can craft this item." : "&cYou cannot craft this item yet."));
            lore.add(CC.color("&8Recipe: " + customItemId));
            meta.setLore(lore);
            result.setItemMeta(meta);
        }
        inventory.setItem(25, result);
        inventory.setItem(49, backButton());
        fill(inventory);
        player.openInventory(inventory);
    }

    private List<String> recipeStatusLore(Player player, CollectionManager.UnlockRequirement requirement, boolean unlocked) {
        if (requirement == null) return List.of("&aNo collection requirement.");
        int progress = collectionManager.getProgress(player, requirement.collectionId());
        return List.of(
                "&7Collection: &f" + strip(collectionManager.getDisplayName(requirement.collectionId())),
                "&7Progress: &e" + format(progress) + "&7/&e" + format(requirement.required()),
                "",
                unlocked ? "&aCrafting is unlocked." : "&cCrafting is blocked until unlocked."
        );
    }

    private String recipeName(String customItemId) {
        return strip(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("custom-items.items." + customItemId + ".name", pretty(customItemId)));
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = org.bukkit.ChatColor.stripColor(line);
            if (clean != null && clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }

    private void addNavigation(Inventory inventory, int page, int totalPages) {
        if (page > 0) inventory.setItem(45, item(Material.ARROW, "&aPrevious Page", List.of(
                "&7Go to the previous page.", "", "&eClick to go back!"
        )));
        inventory.setItem(49, backButton());
        if (page + 1 < totalPages) inventory.setItem(53, item(Material.ARROW, "&aNext Page", List.of(
                "&7Go to the next page.", "", "&eClick to continue!"
        )));
    }

    private ItemStack backButton() {
        return item(Material.BARRIER, "&cBack", List.of(
                "&7Return to the previous menu.", "", "&eClick to go back!"
        ));
    }

    private void openCollectionPage(Player player, String type, int page) {
        String[] parts = type.split(":", 4);
        if (type.startsWith("collections-main:")) openCategories(player, page);
        else if(type.startsWith("collections-vanilla-items:"))openCollections(player,part(parts,1),page);
        else if(type.startsWith("collections-vanilla-path:"))openCollectionPaths(player,Material.matchMaterial(part(parts,1)),page);
        else if (type.startsWith("collection:")) openCollection(player, part(parts, 1), page,Material.matchMaterial(part(parts,3)));
    }

    private int currentPage(String type) {
        String[] parts = type.split(":", 4);
        if (type.startsWith("collections-main:")) return parsePage(parts, 1);
        if (type.startsWith("collections-vanilla-") || type.startsWith("collection:")) return parsePage(parts, 2);
        return 0;
    }

    private int parsePage(String[] parts, int index) {
        if (parts.length <= index) return 0;
        try {
            return Math.max(0, Integer.parseInt(parts[index]));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String part(String[] parts, int index) {
        return parts.length > index ? parts[index] : "";
    }

    private int totalPages(int size, int pageSize) {
        return Math.max(1, (size + pageSize - 1) / pageSize);
    }

    private int[] contentSlots() {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(color(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack collectionButton(ItemStack item,Material source,String collectionId){ItemMeta meta=item.getItemMeta();if(meta==null)return item;meta.getPersistentDataContainer().set(sourceTypeKey,PersistentDataType.STRING,"VANILLA_MATERIAL");meta.getPersistentDataContainer().set(sourceIdKey,PersistentDataType.STRING,source.name());meta.getPersistentDataContainer().set(collectionIdKey,PersistentDataType.STRING,collectionManager.externalCollectionId(collectionId));item.setItemMeta(meta);return item;}

    private List<String> color(List<String> lore) {
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        return colored;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
    }

    private Material parseMaterial(String name) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.AIR;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String strip(String text) {
        return org.bukkit.ChatColor.stripColor(CC.color(text == null ? "" : text));
    }

    private String format(int number) {
        return String.format(Locale.US, "%,d", number);
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Item";
        String[] parts = text.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private int nextRequirement(List<CollectionManager.CollectionUnlock> unlocks, int progress) {
        return unlocks.stream().mapToInt(CollectionManager.CollectionUnlock::amount).filter(value -> value > progress).min().orElse(Math.max(1, progress));
    }

    private String progressBar(int current, int required) {
        int filled = Math.clamp((int) Math.floor(current / (double) Math.max(1, required) * 20.0D), 0, 20);
        return "&8[&a" + "|".repeat(filled) + "&7" + "|".repeat(20 - filled) + "&8]";
    }

    private int validateCollections() {
        int warnings = 0; java.util.Set<String> ids = new java.util.HashSet<>();
        for (String id : collectionManager.getCollectionIds()) {
            String normalized = normalize(id);
            if (!id.equals(normalized) || !ids.add(normalized)) warnings++;
            if (collectionManager.getTrackedMaterials(id).isEmpty()) warnings++;
            int previous = 0;
            for (CollectionManager.CollectionUnlock unlock : collectionManager.getUnlocks(id)) {
                if (unlock.amount() <= 0 || unlock.amount() < previous) warnings++;
                previous = Math.max(previous, unlock.amount());
                if (unlock.isRecipe() && recipeManager != null && recipeManager.getRecipe(unlock.rewardValue()).isEmpty()) warnings++;
            }
        }
        return warnings;
    }
}
