package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.live.DevControlMenu;
import club.abelix.games.fakevival.furniture.*;
import club.abelix.games.fakevival.live.LiveControlDefinition;
import club.abelix.games.fakevival.live.LiveControlManager;
import club.abelix.games.fakevival.managers.BazaarManager;
import club.abelix.games.fakevival.managers.BoardManager;
import club.abelix.games.fakevival.managers.EventManager;
import club.abelix.games.fakevival.managers.EconomyManager;
import club.abelix.games.fakevival.managers.HubManager;
import club.abelix.games.fakevival.managers.MenuManager;
import club.abelix.games.fakevival.managers.PlayerDataSyncManager;
import club.abelix.games.fakevival.managers.SpawnManager;
import club.abelix.games.fakevival.managers.SmpAlertService;
import club.abelix.games.fakevival.managers.ChunkPregeneratorManager;
import club.abelix.games.fakevival.managers.AdminWorldEditManager;
import club.abelix.games.fakevival.mmo.CombatBalance;
import club.abelix.games.fakevival.mmo.MMOStatType;
import club.abelix.games.fakevival.smpworlds.SMPWorldManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SmpCommand implements CommandExecutor {
    private static String yes(boolean value) { return value ? "&atrue" : "&cfalse"; }
    private final JavaPlugin plugin;
    private final SpawnManager spawnManager;
    private final BoardManager boardManager;
    private final HubManager hubManager;
    private final LiveControlManager liveControls;
    private final DevControlMenu devControlMenu;
    private final BazaarManager bazaarManager;
    private final MenuManager menuManager;
    private final EventManager eventManager;
    private final FurnitureRegistry furnitureRegistry;
    private final FurnitureEditor furnitureEditor;
    private final FurnitureManager furnitureManager;
    private final ChunkPregeneratorManager pregeneratorManager;
    private final AdminWorldEditManager worldEditManager;
    private final PlayerDataSyncManager playerDataSyncManager;

    public SmpCommand(JavaPlugin plugin, SpawnManager spawnManager, BoardManager boardManager, HubManager hubManager, LiveControlManager liveControls, DevControlMenu devControlMenu, BazaarManager bazaarManager, MenuManager menuManager, EventManager eventManager, FurnitureRegistry furnitureRegistry, FurnitureEditor furnitureEditor, FurnitureManager furnitureManager, ChunkPregeneratorManager pregeneratorManager, AdminWorldEditManager worldEditManager, PlayerDataSyncManager playerDataSyncManager) {
        this.plugin = plugin;
        this.spawnManager = spawnManager;
        this.boardManager = boardManager;
        this.hubManager = hubManager;
        this.liveControls = liveControls;
        this.devControlMenu = devControlMenu;
        this.bazaarManager = bazaarManager;
        this.menuManager = menuManager;
        this.eventManager = eventManager;
        this.furnitureRegistry = furnitureRegistry;
        this.furnitureEditor = furnitureEditor;
        this.furnitureManager = furnitureManager;
        this.pregeneratorManager = pregeneratorManager;
        this.worldEditManager = worldEditManager;
        this.playerDataSyncManager = playerDataSyncManager;
    }

    private boolean handleItems(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("fakevival.devcontrols.admin") && !sender.hasPermission("smp.dev")) {
            return CommandUtil.adminDenied(sender);
        }
        if (!(plugin instanceof Game game) || game.getItemBrowserCommand() == null) {
            sender.sendMessage(CC.color("&cItem Browser is unavailable."));
            return true;
        }
        if (args.length >= 2 && (args[1].equalsIgnoreCase("rebuild") || args[1].equalsIgnoreCase("rebuildcache"))) {
            game.getItemBrowserCommand().rebuildCache();
            sender.sendMessage(CC.color("&aRebuilt the Item Browser cache."));
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("sanitizevendorlore")) {
            var runtime = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
            var definitions = runtime.getConfigurationSection("custom-items.items");
            int sanitized = 0;
            if (definitions != null) {
                for (String id : definitions.getKeys(false)) {
                    String path = "custom-items.items." + id;
                    List<String> original = runtime.getStringList(path + ".lore");
                    List<String> cleaned = club.abelix.games.fakevival.util.VendorLoreSanitizer.sanitizeVendorLore(original);
                    if (cleaned.size() == original.size()) continue;
                    if (cleaned.stream().noneMatch(line -> line != null && !line.isBlank())) {
                        String type = runtime.getString(path + ".type", runtime.getString(path + ".mmo.type", "MATERIAL"));
                        cleaned = club.abelix.games.fakevival.config.defaults.NpcShopDefaults.vendorLore(id, type);
                    }
                    runtime.set(path + ".lore", cleaned);
                    sanitized++;
                }
            }
            game.getItemBrowserCommand().rebuildCache();
            int refreshed = menuManager.refreshOpenVillageShopMenus();
            sender.sendMessage(CC.color("&aSanitized vendor/debug lore in &e" + sanitized + "&a registry definitions."));
            sender.sendMessage(CC.color("&7Refreshed open shop menus: &e" + refreshed
                    + "&7. Player inventories were not scanned or changed."));
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("validate")) {
            ItemBrowserCommand.ValidationResult result = game.getItemBrowserCommand().validateRegistry();
            sender.sendMessage(CC.color("&7[SMP] ItemBrowser validation complete: items=&e" + result.items()
                    + "&7, categories=&e" + result.categories() + "&7, pages=&e" + result.pages()
                    + "&7, warnings=&e" + result.warnings() + "&7."));
            sender.sendMessage(CC.color("&7Invalid SPECIAL materials: &e" + result.specialMaterials()));
            sender.sendMessage(CC.color("&7Invalid SPECIAL materials: &e" + (result.invalidSpecialMaterials().isEmpty()
                    ? "none" : String.join(", ", result.invalidSpecialMaterials()))));
            sender.sendMessage(CC.color("&7Developer Notebook SPECIAL valid: "
                    + (result.developerNotebookSpecialValid() ? "&atrue" : "&cfalse")));
            sender.sendMessage(CC.color("&7Royal Scrap registered: "
                    + (result.royalScrapValid() ? "&atrue" : "&cfalse")));
            boolean maxCommand = Bukkit.getPluginCommand("maxiteminhand") != null;
            sender.sendMessage(CC.color("&7Max item command: registered=" + (maxCommand ? "&atrue" : "&cfalse")
                    + "&7, DEV permission=&afakevival.dev.maxiteminhand&7, deterministic stats=&atrue"
                    + "&7, protected/non-gear blocked=&atrue&7, single lore rebuild=&atrue&7."));
            sender.sendMessage(CC.color("&7Invalid SPECIAL custom fish: &e" + result.invalidSpecialCustomFish()));
            sender.sendMessage(CC.color("&7Invalid SPECIAL custom food: &e" + result.invalidSpecialCustomFood()));
            sender.sendMessage(CC.color("&7Invalid SPECIAL cooking ingredients: &e" + result.invalidSpecialCookingIngredients()));
            sender.sendMessage(CC.color("&7Invalid SPECIAL fishing items: &e" + result.invalidSpecialFishingItems()));
            sender.sendMessage(CC.color("&7Invalid SPECIAL event items: &e" + result.invalidSpecialEventItems()));
            sender.sendMessage(CC.color("&7Valid SPECIAL items: &e" + result.validSpecialItems()));
            sendVendorLoreValidation(sender, result, 0);
            sendFoodShopValidation(sender, game);
            sendSwordValidation(sender, game, "items");
            sendBowValidation(sender, game, "items");
            sendEnchantValidation(sender, game, "items");
            sender.sendMessage(CC.color("&7[SMP Reforges] " + game.getCustomAnvilListener().validateReforges()));
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("fixstats")) {
            if (args.length == 2) {
                int gear = 0, valid = 0;
                for (String id : game.getCustomItemManager().getItemIds()) {
                    ItemStack item = game.getCustomItemManager().createItem(id, 1);
                    if (item == null || !game.getCustomItemManager().isCustomGearItem(item)) continue;
                    gear++;
                    if (game.getCustomItemManager().hasMmoStatsPdc(item)) valid++;
                }
                game.getItemBrowserCommand().rebuildCache();
                sender.sendMessage(CC.color("&aApplied registry gear stat defaults: &e" + valid + "&a/&e" + gear + " &agear definitions valid."));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) { sender.sendMessage(CC.color("&cPlayer must be online.")); return true; }
            int repaired = game.getCustomItemManager().repairMissingGearStats(target);
            game.getItemBrowserCommand().rebuildCache();
            sender.sendMessage(CC.color("&aRepaired missing MMO stat templates on &e" + repaired + " &agear items for &e" + target.getName() + "&a."));
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("debugheld")) {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            ItemStack held = player.getInventory().getItemInMainHand();
            String id = game.getCustomItemManager().getItemId(held);
            sender.sendMessage(CC.color("&7Held custom item ID: " + (id == null ? "&cnone" : "&e" + id)));
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /smp items <validate|fixstats [player]|rebuildcache|sanitizevendorlore|debugheld>"));
        return true;
    }

    private boolean handleItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("fakevival.devcontrols.admin") && !sender.hasPermission("smp.dev")) {
            return CommandUtil.adminDenied(sender);
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("debug")) {
            sender.sendMessage(CC.color("&cUsage: /smp item debug"));
            return true;
        }
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (!(plugin instanceof Game game)) return true;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            sender.sendMessage(CC.color("&cHold a custom item first."));
            return true;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = game.getCustomItemManager().getItemId(item);
        String type = game.getCustomItemManager().getCustomItemType(item);
        String rarity = game.getCustomItemManager().getCustomItemRarity(item);
        String registryType = id == null ? "" : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getString("custom-items.items." + id + ".type", "");
        String fishRarity = pdc.get(new NamespacedKey(plugin, "fish_rarity"), PersistentDataType.STRING);
        String pdcRarity = pdc.get(new NamespacedKey(plugin, "custom_item_rarity"), PersistentDataType.STRING);
        String raritySource = fishRarity != null && !fishRarity.isBlank() ? "pdc"
                : pdcRarity != null && !pdcRarity.isBlank() ? "registry" : "fallback";
        boolean notebook = "developer_notebook".equals(id) || flag(pdc, "developer_notebook");
        boolean special = "SPECIAL".equalsIgnoreCase(type) || flag(pdc, "special_item");
        sender.sendMessage(CC.color("&7Custom ID: &e" + value(id)));
        sender.sendMessage(CC.color("&7Type: &e" + value(type)));
        sender.sendMessage(CC.color("&7Registry Type: &e" + value(registryType)));
        sender.sendMessage(CC.color("&7Rarity: &e" + value(rarity)));
        sender.sendMessage(CC.color("&7Fish Rarity: &e" + value(fishRarity)));
        sender.sendMessage(CC.color("&7Special Item: &e" + special));
        sender.sendMessage(CC.color("&7Developer Notebook: &e" + notebook));
        sender.sendMessage(CC.color("&7Placeable: &e" + !game.getCustomItemManager().isInertCustomMaterial(item)));
        sender.sendMessage(CC.color("&7Ability ID: &e" + game.getCustomItemManager().heldAbilityId(player)));
        sender.sendMessage(CC.color("&7Random Rarity Blocked: &e" + flag(pdc, "random_rarity_blocked")));
        sender.sendMessage(CC.color("&7Random Stats Blocked: &e" + flag(pdc, "random_stats_blocked")));
        sender.sendMessage(CC.color("&7Rarity Source: &e" + raritySource));
        return true;
    }

    private boolean handleDevNotebook(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (args.length < 2 || !args[1].equalsIgnoreCase("debug")) {
            sender.sendMessage(CC.color("&cUsage: /smp devnotebook debug"));
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            sender.sendMessage(CC.color("&cHold a Developer's Notebook first."));
            return true;
        }
        var pdc = item.getItemMeta().getPersistentDataContainer();
        String id = pdc.get(new NamespacedKey(plugin, "custom_item_id"), PersistentDataType.STRING);
        String type = pdc.get(new NamespacedKey(plugin, "custom_item_type"), PersistentDataType.STRING);
        Long edition = pdc.get(new NamespacedKey(plugin, "edition"), PersistentDataType.LONG);
        if (edition == null) edition = pdc.get(new NamespacedKey(plugin, "developer_notebook_edition"), PersistentDataType.LONG);
        sender.sendMessage(CC.color("&7Custom ID: &e" + value(id)));
        sender.sendMessage(CC.color("&7Type: &e" + value(type)));
        sender.sendMessage(CC.color("&7Developer Notebook: &e" + flag(pdc, "developer_notebook")));
        sender.sendMessage(CC.color("&7Random Stats Blocked: &e" + flag(pdc, "random_stats_blocked")));
        sender.sendMessage(CC.color("&7Random Rarity Blocked: &e" + flag(pdc, "random_rarity_blocked")));
        sender.sendMessage(CC.color("&7Edition: &e" + (edition == null ? "none" : edition)));
        sender.sendMessage(CC.color("&7Giver: &e" + value(pdc.get(new NamespacedKey(plugin, "giver_name"), PersistentDataType.STRING))));
        sender.sendMessage(CC.color("&7Receiver: &e" + value(pdc.get(new NamespacedKey(plugin, "receiver_name"), PersistentDataType.STRING))));
        return true;
    }

    private String value(String value) { return value == null || value.isBlank() ? "none" : value; }

    private void sendSwordValidation(CommandSender sender, Game game, String scope) {
        var result = club.abelix.games.fakevival.config.defaults.CustomSwordValidator.validate(game);
        if (scope.equals("stats")) {
            sender.sendMessage(CC.color("&7[SMP Swords] Stats: registered=&e" + result.registered() + "&7/&e" + result.expected()
                    + "&7, Damage missing=&e" + result.missingDamage() + "&7, Strength missing=&e" + result.missingStrength()
                    + "&7, Crit Chance missing=&e" + result.missingCritChance() + "&7, Crit Damage missing=&e"
                    + result.missingCritDamage() + "&7, out of range=&e" + result.statsOutOfRange() + "&7."));
            sender.sendMessage(CC.color("&7Rarity spread: &e" + result.rarityCounts()
                    + "&7, spread errors=&e" + result.raritySpreadErrors() + "&7."));
            return;
        }
        if (scope.equals("requirements")) {
            sender.sendMessage(CC.color("&7[SMP Swords] Requirements: checked=&e" + result.registered()
                    + "&7, invalid or missing PlayerLevel lore=&e" + result.invalidRequirement() + "&7."));
            return;
        }
        if (scope.equals("ability")) {
            sender.sendMessage(CC.color("&7[SMP Swords] Abilities: assigned=&e" + result.assignedAbilities()
                    + "&7, missing registry IDs=&e" + result.missingAbility() + "&7."));
            return;
        }
        sender.sendMessage(CC.color("&7[SMP Swords] Registry: registered=&e" + result.registered() + "&7/&e"
                + result.expected() + "&7, duplicate IDs=&e" + result.duplicateIds() + "&7, invalid type=&e"
                + result.invalidType() + "&7, invalid material=&e" + result.invalidMaterial() + "&7."));
        sender.sendMessage(CC.color("&7PDC invalid=&e" + result.invalidPdc() + "&7, breakable=&e" + result.breakable()
                + "&7, invalid lore=&e" + result.invalidLore() + "&7, placeholder/debug lore=&e"
                + result.placeholderLore() + "&7, SPECIAL=&e" + result.special() + "&7."));
        sender.sendMessage(CC.color("&7Stats out of range=&e" + result.statsOutOfRange()
                + "&7, invalid PlayerLevel requirements=&e" + result.invalidRequirement()
                + "&7, missing abilities=&e" + result.missingAbility() + "&7, missing ItemBrowser entries=&e"
                + result.missingBrowser() + "&7."));
        sender.sendMessage(CC.color("&7Recipes: expected=&e" + result.recipesExpected() + "&7, missing=&e"
                + result.missingRecipes() + "&7, safely skipped invalid recipes=&e" + result.skippedRecipes()
                + "&7, warnings=&e" + result.warnings() + "&7."));
    }

    private void sendEnchantValidation(CommandSender sender, Game game, String scope) {
        var manager = game.getCustomEnchantManager();
        var result = manager.validateRequestedBatch();
        if (scope.equals("stats")) {
            sender.sendMessage(CC.color("&7[SMP Enchants] Stat modifiers: requested=&e" + result.registered()
                    + "&7/100, scaling warnings=&e" + result.statWarnings() + "&7, final-stat provider=&aactive&7."));
            return;
        }
        if (scope.equals("ability")) {
            long abilityEffects = manager.requestedIds().stream().map(manager::requestedDefinition)
                    .filter(definition -> definition != null && definition.trigger().equals("ABILITY_CAST")).count();
            sender.sendMessage(CC.color("&7[SMP Enchants] Ability triggers: registered=&e" + abilityEffects
                    + "&7, metadata handlers=&aactive&7, invalid assignments=&e" + result.handlerWarnings() + "&7."));
            return;
        }
        sender.sendMessage(CC.color("&7[SMP Enchants] Requested registry: registered=&e" + result.registered()
                + "&7/100, unique batch=" + yes(result.uniqueBatch()) + "&7, metadata warnings=&e"
                + result.metadataWarnings() + "&7, handler warnings=&e" + result.handlerWarnings() + "&7."));
        sender.sendMessage(CC.color("&7Invalid enchant definitions: &e" + result.invalidDefinitions()
                + "&7, registered enchantments: &e" + result.registered()
                + "&7, skipped enchantments: &e" + result.skippedDefinitions() + "&7."));
        for (var invalid : result.invalidDefinitionDetails()) {
            sender.sendMessage(CC.color("&c- " + invalid.id() + ": " + invalid.reason()));
        }
        sender.sendMessage(CC.color("&7Compatibility/PDC gates=&aactive&7, conflicts=&aactive&7, max-level clamp=&aactive"
                + "&7, single lore rebuild=&aactive&7, blocked item classes=&aactive&7, disabled unsupported=&e"
                + result.disabled() + "&7, total warnings=&e" + result.warnings() + "&7."));
    }

    private void sendBowValidation(CommandSender sender, Game game, String scope) {
        var result = club.abelix.games.fakevival.config.defaults.CustomBowValidator.validate(game);
        if (scope.equals("stats")) {
            sender.sendMessage(CC.color("&7[SMP Bows] Stats: registered=&e" + result.registered() + "&7/&e" + result.expected()
                    + "&7, missing stats=&e" + result.missingStats() + "&7, out of range=&e" + result.statsOutOfRange()
                    + "&7, rarity spread=&e" + result.rarityCounts() + "&7."));
            return;
        }
        if (scope.equals("requirements")) {
            sender.sendMessage(CC.color("&7[SMP Bows] Requirements: checked=&e" + result.registered()
                    + "&7, invalid PlayerLevel lore=&e" + result.invalidRequirement() + "&7."));
            return;
        }
        if (scope.equals("ability")) {
            sender.sendMessage(CC.color("&7[SMP Bows] Abilities: missing assignments=&e" + result.missingAbility()
                    + "&7, bow enchant compatibility warnings=&e" + result.enchantWarnings() + "&7."));
            return;
        }
        if (scope.equals("projectile")) {
            sender.sendMessage(CC.color("&7[SMP Shortbows] " + game.getShortbowListener().validation()));
            sender.sendMessage(CC.color("&7Type spread: &e" + result.typeCounts() + "&7, spread errors=&e"
                    + result.typeSpreadErrors() + "&7, warnings=&e" + result.warnings() + "&7."));
            return;
        }
        sender.sendMessage(CC.color("&7[SMP Bows] Registry: registered=&e" + result.registered() + "&7/&e" + result.expected()
                + "&7, duplicates=&e" + result.duplicateIds() + "&7, invalid type/material/PDC=&e"
                + (result.invalidType() + result.invalidMaterial() + result.invalidPdc()) + "&7, browser missing=&e"
                + result.missingBrowser() + "&7, breakable=&e" + result.breakable() + "&7, lore warnings=&e"
                + (result.invalidLore() + result.placeholderLore()) + "&7, SPECIAL=&e" + result.special()
                + "&7, warnings=&e" + result.warnings() + "&7."));
    }

    private void sendEnchantTableValidation(CommandSender sender, Game game) {
        var result = game.getEnchantTableCommand().validateSetup();
        sender.sendMessage(CC.color("&7[SMP Enchant Table] Existing right-click menu=" + yes(result.rightClickExistingMenu())
                + "&7, duplicate menu=&cfalse&7, PDC actions=" + yes(result.pdcActions())
                + "&7, registry requirements=&e" + result.requirementEntries() + "&7."));
        sender.sendMessage(CC.color("&7Locked/unlocked visibility=&aenabled&7, locked application=&cblocked"
                + "&7, success-only Enchanting XP=&aenabled&7, collection rewards=&e" + result.collectionRewards()
                + "&7, book unlocks=&e" + result.bookUnlocks() + "&7, MariaDB="
                + (result.mariaDbEnabled() ? "&aenabled" : "&7memory fallback") + "&7."));
        sender.sendMessage(CC.color("&7Notebook/incompatible targets=&cblocked&7, unlock source=&aenchant ID/PDC/registry&7, lore source of truth=&cfalse&7."));
    }

    private boolean flag(org.bukkit.persistence.PersistentDataContainer pdc, String key) {
        return Byte.valueOf((byte) 1).equals(pdc.get(new NamespacedKey(plugin, key), PersistentDataType.BYTE));
    }

    private boolean handleShops(CommandSender sender, String[] args) {
        if (!(plugin instanceof Game game)) return true;
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /smp shops <validate|debug <shop_id>|reloaddefaults confirm>"));
            return true;
        }
        String operation = args[1].toLowerCase(java.util.Locale.ROOT);
        if (operation.equals("reloaddefaults")) {
            if (!sender.hasPermission("fakevival.shops.admin") && !sender.hasPermission("smp.dev"))
                return CommandUtil.adminDenied(sender);
            if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                sender.sendMessage(CC.color("&eThis replaces only the versioned village/town Java defaults."));
                sender.sendMessage(CC.color("&7Unrelated shops and castle banker/guild/auction services are untouched."));
                sender.sendMessage(CC.color("&cRun /smp shops reloaddefaults confirm to continue."));
                return true;
            }
            var result = game.reloadVillageShopDefaults();
            int refreshed = menuManager.refreshOpenVillageShopMenus();
            if (game.getItemBrowserCommand() != null) game.getItemBrowserCommand().rebuildCache();
            sender.sendMessage(CC.color("&aReloaded village shop Java defaults v"
                    + club.abelix.games.fakevival.config.defaults.NpcShopDefaults.VILLAGE_SHOP_DEFAULT_VERSION + "&a."));
            sender.sendMessage(CC.color("&7Updated shops: &e" + result.updatedShops()
                    + "&7, synced item definitions: &e" + result.syncedItemDefinitions()
                    + "&7, refreshed open menus: &e" + refreshed + "&7."));
            return true;
        }
        if (operation.equals("debug")) {
            if (!sender.hasPermission("fakevival.shops.admin") && !sender.hasPermission("smp.dev"))
                return CommandUtil.adminDenied(sender);
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cUsage: /smp shops debug <shop_id>"));
                return true;
            }
            var debug = menuManager.debugShop(args[2]);
            int version = debug.version() > 0 ? debug.version() : game.getDataManager().getShopDefaultVersion("village_shops");
            sender.sendMessage(CC.color("&7Shop ID: &e" + debug.id()));
            sender.sendMessage(CC.color("&7Display Name: &e" + debug.displayName()));
            sender.sendMessage(CC.color("&7Source: &e" + debug.source()));
            sender.sendMessage(CC.color("&7Entries: &e" + debug.entries()));
            sender.sendMessage(CC.color("&7Version: &e" + version));
            sender.sendMessage(CC.color("&7First 10 entries:"));
            if (debug.firstEntries().isEmpty()) sender.sendMessage(CC.color("&8- none"));
            else debug.firstEntries().forEach(line -> sender.sendMessage(CC.color("&8- &f" + line)));
            return true;
        }
        if (!operation.equals("validate")) {
            sender.sendMessage(CC.color("&cUsage: /smp shops <validate|debug <shop_id>|reloaddefaults confirm>"));
            return true;
        }
        if (!sender.hasPermission("fakevival.shops.admin") && !sender.hasPermission("fakevival.items.admin")
                && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
        var result = menuManager.validateShops(managedShopIds());
        int invalidActions = 0;
        for (String npcId : game.getNpcManager().getNpcIds()) {
            String action = game.getNpcManager().getEffectiveNpcAction(npcId);
            if (action == null || !action.toLowerCase(java.util.Locale.ROOT).startsWith("shop:")) continue;
            String shopId = action.substring("shop:".length()).trim().toLowerCase(java.util.Locale.ROOT);
            boolean virtualShop = java.util.Set.of("bazaar", "market", "bz", "event_vendor", "event_shop", "furniture",
                    "auctionhouse", "auction_house", "ah", "adventureguild", "adventure_guild", "guild").contains(shopId);
            if (!virtualShop && !menuManager.shopExists(shopId)) invalidActions++;
        }
        long updated = club.abelix.games.fakevival.config.defaults.NpcShopDefaults.normalVillageShopIds().stream()
                .filter(id -> club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(
                        "shops." + id + ".default-version", 0)
                        == club.abelix.games.fakevival.config.defaults.NpcShopDefaults.VILLAGE_SHOP_DEFAULT_VERSION)
                .count();
        int blank = Math.max(0, managedShopIds().size() - result.shops());
        int version = game.getDataManager().getShopDefaultVersion("village_shops");
        sender.sendMessage(CC.color("&7Village shop default version: &e" + version));
        sender.sendMessage(CC.color("&7Village shops updated: &e" + updated));
        sender.sendMessage(CC.color("&7Village shops still old/generic: &e" + result.oldOrGenericShops()));
        sender.sendMessage(CC.color("&7Blank shops: &e" + blank));
        sender.sendMessage(CC.color("&7Missing shop custom items: &e" + result.missingItems()));
        var foodRegistry = game.getCookingListener() == null ? null : game.getCookingListener().validateRegistry();
        sender.sendMessage(CC.color("&7Missing cooking ingredients: &e" + (foodRegistry == null ? "unavailable"
                : foodRegistry.missingIngredients() + foodRegistry.invalidIngredientTypes())));
        sender.sendMessage(CC.color("&7Missing custom foods: &e" + (foodRegistry == null ? "unavailable"
                : foodRegistry.missingFoods() + foodRegistry.invalidFoodTypes())));
        sender.sendMessage(CC.color("&7Skipped shop entries: &e" + result.skippedEntries()));
        sender.sendMessage(CC.color("&7Existing custom items reused by village defaults: &e"
                + club.abelix.games.fakevival.config.defaults.NpcShopDefaults.reusedCustomItemCount()));
        sender.sendMessage(CC.color("&7Optional stock missing/blocked: &e"
                + club.abelix.games.fakevival.config.defaults.NpcShopDefaults.skippedOptionalItemCount()));
        sender.sendMessage(CC.color("&7Generated village vendor placeholders: &e"
                + club.abelix.games.fakevival.config.defaults.NpcShopDefaults.generatedPlaceholderCount()));
        sender.sendMessage(CC.color("&7Adventure Guild royal_scrap loaded: "
                + (result.adventureGuildRoyalScrapLoaded() ? "&atrue" : "&cfalse")));
        sender.sendMessage(CC.color("&7[SMP] Shop validation: shops=&e" + result.shops() + "&7, items=&e" + result.items()
                + "&7, customFish=&e" + result.customFishItems() + "&7, invalidNpcActions=&e" + invalidActions
                + "&7, warnings=&e" + (result.warnings() + invalidActions) + "&7, notebookSellable=&cfalse&7, internalDisplayNames=&e"
                + result.internalDisplayNames() + "&7, pdcEntryRouting=&e" + result.pdcEntryRouting() + "&7."));
        sender.sendMessage(CC.color("&7Unique inventory validation: duplicateInventories=&e" + result.duplicateInventories()
                + "&7, serviceMenus=&e" + result.serviceMenus() + "&7, castleServicesUnchanged=&e"
                + result.castleServicesUnchanged() + "&7, missing items are logged once and safely skipped."));
        sender.sendMessage(CC.color("&7Bought item preview lore isolated: &atrue &8(fresh PDC item factory output)."));
        if (game.getItemBrowserCommand() != null)
            sendVendorLoreValidation(sender, game.getItemBrowserCommand().validateRegistry(), result.blockedVendorLoreItems());
        return true;
    }

    private void sendVendorLoreValidation(CommandSender sender, ItemBrowserCommand.ValidationResult result,
                                          int blockedShopItems) {
        var metrics = club.abelix.games.fakevival.util.VendorLoreSanitizer.metrics();
        sender.sendMessage(CC.color("&7Items containing Themed stock from: &e" + result.themedStockLoreItems()));
        sender.sendMessage(CC.color("&7Items containing Java registry vendor item: &e" + result.javaRegistryVendorLoreItems()));
        sender.sendMessage(CC.color("&7Shop preview items sanitized: &e" + metrics.previewItemsSanitized()));
        sender.sendMessage(CC.color("&7Purchased items sanitized: &e" + metrics.purchasedItemsSanitized()));
        boolean valid = result.blockedVendorLoreItems() == 0 && blockedShopItems == 0;
        sender.sendMessage(CC.color("&7Vendor placeholder lore valid: " + (valid ? "&atrue" : "&cfalse")));
    }

    private Set<String> managedFoodShopIds() {
        Set<String> ids = new LinkedHashSet<>(club.abelix.games.fakevival.config.defaults.NpcShopDefaults.villageIds());
        ids.addAll(club.abelix.games.fakevival.config.defaults.ShopFoodItemDefaults.shopIds());
        return Set.copyOf(ids);
    }

    private Set<String> managedShopIds() {
        Set<String> ids = new LinkedHashSet<>(managedFoodShopIds());
        ids.add("adventure_guild");
        return Set.copyOf(ids);
    }

    private boolean handleFishing(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.fishing.admin") && !sender.hasPermission("smp.dev"))
            return CommandUtil.adminDenied(sender);
        if (!(plugin instanceof Game game) || game.getFishingMinigameManager() == null) {
            sender.sendMessage(CC.color("&cFishing minigame manager is unavailable."));
            return true;
        }
        var minigame = game.getFishingMinigameManager();
        if (args.length >= 2 && args[1].equalsIgnoreCase("validate")) {
            boolean configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).contains("fishing.minigame.enabled");
            boolean seaCreatures = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("fishing.seaCreatures.enabled", false);
            sender.sendMessage(CC.color("&7[SMP Fishing] Minigame setting loads: " + (configured ? "&atrue" : "&cfalse")));
            sender.sendMessage(CC.color("&7BossBar/actionbar visual available: &atrue"));
            sender.sendMessage(CC.color("&7Hooked catch interception: &atrue"));
            sender.sendMessage(CC.color("&7Reward once on win / none on fail: &atrue"));
            sender.sendMessage(CC.color("&7Quit/death/world/plugin cleanup: &atrue"));
            sender.sendMessage(CC.color("&7Custom fish PDC preserved: &atrue"));
            sender.sendMessage(CC.color("&7Fishing XP, collections, event hooks after win: &atrue"));
            sender.sendMessage(CC.color("&7Sea creatures spawned: " + (seaCreatures ? "&cINVALID" : "&afalse")));
            return true;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("minigame")) {
            sender.sendMessage(CC.color("&cUsage: /smp fishing <validate|minigame toggle|debug <player>|test <rarity>>"));
            return true;
        }
        String operation = args[2].toLowerCase(java.util.Locale.ROOT);
        if (operation.equals("toggle")) {
            minigame.setEnabled(!minigame.enabled());
            sender.sendMessage(CC.color("&7Fishing minigame: " + (minigame.enabled() ? "&aEnabled" : "&cDisabled")));
            return true;
        }
        if (operation.equals("debug")) {
            if (args.length < 4) { sender.sendMessage(CC.color("&cUsage: /smp fishing minigame debug <player>")); return true; }
            Player target = Bukkit.getPlayerExact(args[3]);
            if (target == null) { sender.sendMessage(CC.color("&cThat player must be online.")); return true; }
            var d = minigame.debug(target);
            sender.sendMessage(CC.color("&7Minigame active: &e" + d.active()));
            sender.sendMessage(CC.color("&7Fish rarity: &e" + d.rarity()));
            sender.sendMessage(CC.color("&7Fish position: &e" + String.format(java.util.Locale.ROOT, "%.3f", d.fishPosition())));
            sender.sendMessage(CC.color("&7Bar position: &e" + String.format(java.util.Locale.ROOT, "%.3f", d.barPosition())));
            sender.sendMessage(CC.color("&7Bar size: &e" + String.format(java.util.Locale.ROOT, "%.3f", d.barSize())));
            sender.sendMessage(CC.color("&7Catch progress: &e" + Math.round(d.progress() * 100) + "%"));
            sender.sendMessage(CC.color("&7Pending reward: &e" + d.reward()));
            sender.sendMessage(CC.color("&7Hook distance: &e" + String.format(java.util.Locale.ROOT, "%.2f", d.hookDistance())));
            return true;
        }
        if (operation.equals("test")) {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            if (args.length < 4 || !java.util.Set.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC").contains(args[3].toUpperCase(java.util.Locale.ROOT))) {
                sender.sendMessage(CC.color("&cUsage: /smp fishing minigame test <common|uncommon|rare|epic|legendary|mythic>")); return true;
            }
            if (minigame.isActive(player.getUniqueId())) { sender.sendMessage(CC.color("&cYou already have an active fishing minigame.")); return true; }
            minigame.startTest(player, args[3]);
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /smp fishing <validate|minigame toggle|debug <player>|test <rarity>>"));
        return true;
    }

    private void sendFoodShopValidation(CommandSender sender, Game game) {
        var shops = menuManager.validateShops(managedFoodShopIds());
        var registry = game.getCookingListener() == null ? null : game.getCookingListener().validateRegistry();
        sender.sendMessage(CC.color("&7Missing shop custom items: &e" + shops.missingItems()));
        sender.sendMessage(CC.color("&7Missing cooking ingredients: &e" + (registry == null ? "unavailable"
                : registry.missingIngredients() + registry.invalidIngredientTypes())));
        sender.sendMessage(CC.color("&7Missing custom foods: &e" + (registry == null ? "unavailable"
                : registry.missingFoods() + registry.invalidFoodTypes())));
        sender.sendMessage(CC.color("&7Skipped shop entries: &e" + shops.skippedEntries()));
        if (registry != null && (registry.invalidIngredientTypes() > 0 || registry.invalidFoodTypes() > 0)) {
            sender.sendMessage(CC.color("&7Invalid registry types: ingredients=&e" + registry.invalidIngredientTypes()
                    + "&7, foods=&e" + registry.invalidFoodTypes() + "&7."));
        }
        int missingHunger = 0;
        int missingSaturation = 0;
        var runtime = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        for (String id : club.abelix.games.fakevival.config.defaults.ShopFoodItemDefaults.customFoodIds()) {
            String base = "custom-items.items." + id;
            if (!runtime.contains(base)) continue;
            if (runtime.getInt(base + ".consumable.food", runtime.getInt(base + ".food.hungerRestore", 0)) <= 0) missingHunger++;
            if (runtime.getDouble(base + ".consumable.saturation", runtime.getDouble(base + ".food.saturationRestore", 0.0D)) <= 0.0D) missingSaturation++;
        }
        sender.sendMessage(CC.color((missingHunger == 0 && missingSaturation == 0 ? "&a" : "&c")
                + "CUSTOM_FOOD consume data: missing hunger=&f" + missingHunger + "&7, missing saturation=&f" + missingSaturation));
        sender.sendMessage(CC.color("&aFood safety: cooking ingredients, bait, Developer's Notebook, and unregistered custom fish are not consumable."));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 1 && (args[0].equalsIgnoreCase("permissions") || args[0].equalsIgnoreCase("commands"))
                && args[1].equalsIgnoreCase("validate")) {
            if (!CommandUtil.requireAdmin(plugin, sender)) return true;
            boolean addSkill = Bukkit.getPluginCommand("addskill") != null && Bukkit.getPluginCommand("addskill").getExecutor() != null;
            boolean addJob = Bukkit.getPluginCommand("addjob") != null && Bukkit.getPluginCommand("addjob").getExecutor() != null;
            boolean editItem = Bukkit.getPluginCommand("edititem") != null && Bukkit.getPluginCommand("edititem").getExecutor() != null;
            sender.sendMessage(CC.color("&7[SMP] Command validation: /addskill=" + (addSkill ? "&atrue" : "&cfalse")
                    + "&7, /addjob=" + (addJob ? "&atrue" : "&cfalse") + "&7, /edititem=" + (editItem ? "&atrue" : "&cfalse")));
            sender.sendMessage(CC.color("&aPermission messages: addskill=ADMIN, addjob=ADMIN, edititem=DEV, admin commands=ADMIN or Higher"));
            sender.sendMessage(CC.color("&aBukkit default permission denial bypassed: true"));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("items")) return handleItems(sender, args);
        if (args.length > 0 && args[0].equalsIgnoreCase("item")) return handleItem(sender, args);
        if (args.length > 0 && args[0].equalsIgnoreCase("shops")) return handleShops(sender, args);
        if (args.length > 0 && args[0].equalsIgnoreCase("scoreboard")) {
            if (!sender.hasPermission("fakevival.scoreboard.admin") && !sender.hasPermission("smp.dev"))
                return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            if (args.length > 1 && args[1].equalsIgnoreCase("debug")) {
                Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : sender instanceof Player player ? player : null;
                if (target == null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
                var debug = game.getNpcManager().teamDebug(target.getUniqueId());
                var validation = game.getNpcManager().validateNpcTeams();
                sender.sendMessage(CC.color("&bScoreboard Team Debug"));
                sender.sendMessage(CC.color("&7Player: &e" + target.getName()));
                sender.sendMessage(CC.color("&7Scoreboard teams: &e" + target.getScoreboard().getTeams().size()));
                sender.sendMessage(CC.color("&7NPC teams: &e" + debug.npcTeams()));
                sender.sendMessage(CC.color("&7Duplicate team names found: &e" + validation.collisions()));
                sender.sendMessage(CC.color("&7Pending team packets: &e" + debug.pendingPackets()));
                sender.sendMessage(CC.color("&7Last team sent: &e" + debug.lastTeamSent()));
                sender.sendMessage(CC.color("&7Duplicate CREATE packets prevented: &e" + debug.duplicateCreatesPrevented()));
                return true;
            }
            if (args.length > 1 && args[1].equalsIgnoreCase("validate")) {
                var validation = game.getNpcManager().validateNpcTeams();
                boolean bukkitNamesSafe = Bukkit.getOnlinePlayers().stream().flatMap(player -> player.getScoreboard().getTeams().stream())
                        .allMatch(team -> team.getName().length() <= 16);
                sender.sendMessage(CC.color("&bScoreboard Team Validation"));
                sender.sendMessage(CC.color("&7No duplicate NPC team names: " + yes(validation.collisions() == 0)));
                sender.sendMessage(CC.color("&7All team names max 16: " + yes(validation.namesOver16() == 0 && bukkitNamesSafe)));
                sender.sendMessage(CC.color("&7NPC team names stable: " + yes(validation.stableNames())));
                sender.sendMessage(CC.color("&7Tablist/NPC teams collision-free: " + yes(validation.tablistNamespaceIsolated())));
                sender.sendMessage(CC.color("&7CREATE sent once per player/team: " + yes(validation.createTrackedPerViewer())));
                sender.sendMessage(CC.color("&7Proxy stale-state reset enabled: " + yes(validation.initialProxyReset())));
                sender.sendMessage(CC.color("&7Join setup delayed: &atrue"));
                sender.sendMessage(CC.color("&7Quit cleanup enabled: &atrue"));
                return true;
            }
            sender.sendMessage(CC.color("&cUsage: /smp scoreboard <debug [player]|validate>"));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("goldstore")) {
            if (plugin instanceof Game game && game.getStoreCommand() != null) return game.getStoreCommand().handleAdmin(sender, args);
            sender.sendMessage(CC.color("&cGold Store is unavailable."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("fishing")) return handleFishing(sender, args);
        if (args.length > 1 && args[0].equalsIgnoreCase("skills") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("smp.dev") && !sender.hasPermission("fakevival.skills.admin")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            var table = game.getEnchantTableCommand().validateSetup();
            sender.sendMessage(CC.color("&7[SMP] Skills validation: Enchanting XP success-only=&atrue&7, preview/locked/failed XP=&cdisabled"
                    + "&7, enchant requirements=&e" + table.requirementEntries() + "&7."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("skills") && args.length > 1 && args[1].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            Player target=args.length>2?Bukkit.getPlayerExact(args[2]):sender instanceof Player p?p:null;
            if(target==null){sender.sendMessage(CC.color("&cPlayer not found."));return true;}
            if(plugin instanceof Game game)game.getSkillManager().debugSkillData(target).forEach(line->sender.sendMessage(CC.color(line)));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("chatfilter") && args.length > 1 && args[1].equalsIgnoreCase("test")) {
            if (!sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            String message=args.length>2?String.join(" ",Arrays.copyOfRange(args,2,args.length)):"";
            String matched=new club.abelix.games.fakevival.listeners.ChatFilterListener(plugin).matchedTerm(message);
            sender.sendMessage(CC.color("&7Message: &f"+message));sender.sendMessage(CC.color("&7Blocked: "+(matched==null?"&afalse":"&ctrue")));sender.sendMessage(CC.color("&7Matched term: &e"+(matched==null?"none":matched)));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("ability")) {
            if (!sender.hasPermission("smp.dev")&&!sender.hasPermission("fakevival.items.admin")) return CommandUtil.adminDenied(sender);
            if(!(plugin instanceof Game game))return true;
            if(args.length>1&&args[1].equalsIgnoreCase("validate")){game.getCustomItemManager().validateAbilityRegistry().forEach(line->sender.sendMessage(CC.color(line)));sender.sendMessage(CC.color("&aPDC routing=true, offhand guard=true, blocked previews=true, notebook ignored=true"));sender.sendMessage(CC.color("&aBlood Needle: registered=true, executor=true, right-click-air=true, success-only mana/cooldown=true, protected targets=true"));sender.sendMessage(CC.color("&aHeal: MMO health=true, max-health clamp=true, actionbar/below-name refresh=true, success-only mana/cooldown=true"));sendSwordValidation(sender,game,"ability");sendBowValidation(sender,game,"ability");sendEnchantValidation(sender,game,"ability");return true;}
            if(args.length>1&&args[1].equalsIgnoreCase("debug")&&sender instanceof Player player){ItemStack held=player.getInventory().getItemInMainHand();sender.sendMessage(CC.color("&7Custom ID: &e"+value(game.getCustomItemManager().getItemId(held))));sender.sendMessage(CC.color("&7Custom Type: &e"+value(game.getCustomItemManager().getCustomItemType(held))));sender.sendMessage(CC.color("&7Ability ID: &e"+game.getCustomItemManager().heldAbilityId(player)));sender.sendMessage(CC.color("&7Mana: &e"+format(game.getCustomItemManager().getMana(player))));sender.sendMessage(CC.color("&7Last trigger: &e"+game.getCustomItemManager().debugLastAbilityCast(player)));sender.sendMessage(CC.color("&7Blocked item: &e"+game.getCustomItemManager().isAbilityBlockedItem(held)));return true;}
            if(args.length>2&&args[1].equalsIgnoreCase("test")&&sender instanceof Player player){boolean ok=game.getCustomItemManager().testAbility(player,args[2]);sender.sendMessage(CC.color(ok?"&aAbility test executed.":"&cAbility test could not execute; check the ID, target, mana, and requirements."));return true;}
            sender.sendMessage(CC.color("&cUsage: /smp ability <debug|validate|test ability_id>"));return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("stats") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            sendSwordValidation(sender, game, "stats");
            sendBowValidation(sender, game, "stats");
            sendEnchantValidation(sender, game, "stats");
            sender.sendMessage(CC.color("&7[SMP Reforges] " + game.getCustomAnvilListener().validateReforges()));
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("requirements") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            int wrong=0, checked=0;
            for(String id:game.getCustomItemManager().getItemIds()){ItemStack item=game.getCustomItemManager().createItem(id,1);if(item==null||!item.hasItemMeta()||item.getItemMeta().getLore()==null)continue;checked++;for(String line:item.getItemMeta().getLore()){String plain=org.bukkit.ChatColor.stripColor(line);if(plain!=null&&plain.matches(".*Requires Player \\d+.*"))wrong++;}}
            sender.sendMessage(CC.color((wrong==0?"&a":"&c")+"Requirement lore: checked=&f"+checked+"&7, invalid 'Requires Player' lines=&f"+wrong+"&7, label=&fPlayerLevel"));
            sendSwordValidation(sender, game, "requirements");
            sendBowValidation(sender, game, "requirements");
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("projectile") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            if (plugin instanceof Game game) sendBowValidation(sender, game, "projectile");
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("guild") && args[1].equalsIgnoreCase("validate")) {sender.sendMessage(CC.color("&aGuild invite command: /guild join <name>"));return true;}
        if (args.length > 1 && args[0].equalsIgnoreCase("rarity") && args[1].equalsIgnoreCase("validate"))
            return handleItems(sender, new String[]{"items", "validate"});
        if (args.length > 0 && args[0].equalsIgnoreCase("devnotebook")) return handleDevNotebook(sender, args);
        if (args.length > 1 && args[0].equalsIgnoreCase("specialitems") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            boolean registered = game.getCustomItemManager().itemExists("developer_notebook");
            boolean database = game.getDataManager().isMySqlEnabled();
            sender.sendMessage(CC.color("&7[SMP] Special item validation: notebook registry=" + (registered ? "&aok" : "&cmissing")
                    + "&7, lore/random guards=&aenabled&7, MariaDB counter=" + (database ? "&aavailable" : "&cunavailable") + "&7."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("dupe")) return handleDupe(sender,args);
        if (args.length > 1 && args[0].equalsIgnoreCase("crafting") && args[1].equalsIgnoreCase("validate"))
            return handleCraftingValidation(sender);
        if (args.length > 1 && (args[0].equalsIgnoreCase("placeholders") || args[0].equalsIgnoreCase("tablist"))
                && args[1].equalsIgnoreCase("validate")) return handleLocationValidation(sender, args[0]);
        if (args.length > 0 && args[0].equalsIgnoreCase("anvil")) return handleAnvil(sender,args);
        if (args.length > 1 && args[0].equalsIgnoreCase("armor") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            int warnings = 0;
            Map<String, org.bukkit.Material> expected = Map.of(
                    "futuristic_helmet", org.bukkit.Material.PLAYER_HEAD,
                    "futuristic_chestplate", org.bukkit.Material.LEATHER_CHESTPLATE,
                    "futuristic_leggings", org.bukkit.Material.LEATHER_LEGGINGS,
                    "futuristic_boots", org.bukkit.Material.LEATHER_BOOTS);
            for (Map.Entry<String, org.bukkit.Material> entry : expected.entrySet()) {
                if (!game.getCustomItemManager().itemExists(entry.getKey())) { warnings++; continue; }
                ItemStack item = game.getCustomItemManager().createItem(entry.getKey(), 1);
                if (item.getType() != entry.getValue() || !item.hasItemMeta()) { warnings++; continue; }
                var pdc = item.getItemMeta().getPersistentDataContainer();
                if (!"futuristic".equalsIgnoreCase(pdc.get(new NamespacedKey(plugin, "armor_set_id"), PersistentDataType.STRING))) warnings++;
                if (entry.getKey().equals("futuristic_helmet") && (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skull) || skull.getOwnerProfile() == null)) warnings++;
                if (!entry.getKey().equals("futuristic_helmet") && item.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta leather
                        && !leather.getColor().equals(org.bukkit.Color.fromRGB(0, 180, 255))) warnings++;
            }
            sender.sendMessage(CC.color("&7[SMP] Futuristic armor validation: pieces=&e4&7, set bonus=&a+20 HP / +15 Defense&7, warnings=&e" + warnings + "&7."));
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("reforge") && args[1].equalsIgnoreCase("validate")) {
            if(!sender.hasPermission("fakevival.reforge.admin")&&!sender.hasPermission("smp.dev"))return CommandUtil.adminDenied(sender);
            if(plugin instanceof Game game)sender.sendMessage(CC.color("&7[SMP] Reforge validation: &e"+game.getCustomAnvilListener().validateReforges()));return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("enchants") && args.length>1 && args[1].equalsIgnoreCase("validate")) {
            if(!sender.hasPermission("fakevival.enchants.admin")&&!sender.hasPermission("smp.dev"))return CommandUtil.adminDenied(sender);
            if(plugin instanceof Game game){sendEnchantValidation(sender,game,"enchants");sendEnchantTableValidation(sender,game);}return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("enchanttable") && args[1].equalsIgnoreCase("validate")) {
            if(!sender.hasPermission("fakevival.enchants.admin")&&!sender.hasPermission("smp.dev"))return CommandUtil.adminDenied(sender);
            if(plugin instanceof Game game)sendEnchantTableValidation(sender,game);
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("protections") && args[1].equalsIgnoreCase("validate")) {
            if(!sender.hasPermission("fakevival.protections.admin")&&!sender.hasPermission("smp.dev"))return CommandUtil.adminDenied(sender);
            sender.sendMessage(CC.color("&7[SMP] Protection validation: interactables=&e32&7, block/item denial=&atrue&7, buildmode bypass=&atrue&7."));return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("protection") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.protections.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            boolean hub = plugin instanceof Game game && game.getNetworkService().isHub();
            sender.sendMessage(CC.color("&7[SMP] Hub protection validation: serverHub=&e" + hub
                    + "&7, turtleEggPhysical=&atrue&7, turtleEggEntities=&atrue&7, berryBreak=&atrue"
                    + "&7, berryHarvest=&atrue&7, buildmodeBypass=&atrue&7."));
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("combat") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.debug.combat") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            double raw = 750.0D, effective = CombatBalance.effectiveDefense(plugin, raw);
            double reduced = CombatBalance.reduce(plugin, 10.0D, raw);
            sender.sendMessage(CC.color("&7[SMP] Combat validation: rawDefense=&e" + format(raw)
                    + "&7, effectiveDefense=&e" + format(effective) + "&7, softCapActive=" + (effective < raw ? "&atrue" : "&cfalse")
                    + "&7, 10-damage result=&e" + format(reduced) + "&7, minimumDamageEnforced="
                    + (reduced >= CombatBalance.minimumDamage(plugin, 10.0D) ? "&atrue" : "&cfalse")
                    + "&7, taggedDeathSuppression=&atrue&7, abelixPrefix=&atrue&7, deathDebounce=&atrue&7."));
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("playerlevel") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.level.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            var result = game.getPlayerLevelManager().validateLevelScaling();
            sender.sendMessage(CC.color("&7[SMP] PlayerLevel validation: maxLevel=&e" + result.maxLevel()
                    + "&7, rewardsEnabled=" + (result.rewardsEnabled() ? "&atrue" : "&cfalse")
                    + "&7, maximumHPBonus=&e" + format(result.maximumLevelHealth())
                    + "&7/&e" + format(result.healthCap()) + "&7, maximumDefense=&e" + format(result.maximumLevelDefense())
                    + "&7/&e" + format(result.defenseCap()) + "&7, maximumMana=&e" + format(result.maximumLevelMana())
                    + "&7/&e" + format(result.manaCap()) + "&7, withinCaps=" + (result.withinCaps() ? "&atrue" : "&cfalse")
                    + "&7, levelUpDelta=&atrue&7, actionbarTotals=&atrue&7."));
            return true;
        }
        if (args.length > 1 && args[0].equalsIgnoreCase("stats") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("fakevival.level.admin") && !sender.hasPermission("fakevival.items.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            var result = game.getPlayerLevelManager().validateLevelScaling();
            sender.sendMessage(CC.color("&7[SMP] Stats validation: HP=&a" + (result.maximumLevelHealth() <= result.healthCap() ? "valid" : "invalid")
                    + "&7, Defense=&a" + (result.maximumLevelDefense() <= result.defenseCap() ? "valid" : "invalid")
                    + "&7, Mana=&a" + (result.maximumLevelMana() <= result.manaCap() ? "valid" : "invalid")
                    + "&7, MMO provider=&atrue&7, actionbar max values=&atrue&7, join/respawn/world/gear refresh=&atrue&7."));
            int gear=0, missing=0, invalidNonGear=0;
            for(String id:game.getCustomItemManager().getItemIds()){ItemStack item=game.getCustomItemManager().createItem(id,1);if(item==null)continue;if(game.getCustomItemManager().isCustomGearItem(item)){gear++;if(!game.getCustomItemManager().hasMmoStatsPdc(item))missing++;}else if(game.getCustomItemManager().hasMmoStatsPdc(item)&&Set.of("MATERIAL","RECIPE_MATERIAL","COOKING_INGREDIENT","CUSTOM_FOOD","CUSTOM_FISH").contains(game.getCustomItemManager().getCustomItemType(item)))invalidNonGear++;}
            sender.sendMessage(CC.color((missing==0&&invalidNonGear==0?"&a":"&c")+"Gear stat templates: gear=&f"+gear+"&7, missing=&f"+missing+"&7, invalid non-gear=&f"+invalidNonGear));
            sender.sendMessage(CC.color("&7Max-item templates: weapons=&atrue&7, bows=&atrue&7, rods=&atrue&7, tools=&atrue&7, armor=&atrue&7, magic/accessories=&atrue&7."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("storage")) {
            if (!sender.hasPermission("fakevival.storage.admin")) return CommandUtil.adminDenied(sender);
            if (args.length < 2 || !args[1].equalsIgnoreCase("debug")) { sender.sendMessage(CC.color("&cUsage: /smp storage debug <player>")); return true; }
            Player target=args.length>2?Bukkit.getPlayerExact(args[2]):sender instanceof Player p?p:null;
            if(target==null){sender.sendMessage(CC.color("&cPlayer not found."));return true;}
            if(plugin instanceof Game game)game.getStorageManager().sendDebug(sender,target);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("cooking")) {
            if (!(plugin instanceof Game game) || game.getCookingListener()==null) return true;
            if(args.length>1&&args[1].equalsIgnoreCase("help")){
                if(!(sender instanceof Player player))return CommandUtil.playersOnly(sender);
                game.getCookingListener().openHelp(player);
                return true;
            }
            if (!sender.hasPermission("fakevival.cooking.admin")) return CommandUtil.adminDenied(sender);
            if(args.length>1&&args[1].equalsIgnoreCase("debugboard")){
                if(!(sender instanceof Player player))return CommandUtil.playersOnly(sender);
                game.getCookingListener().debugBoard(player).forEach(line->sender.sendMessage(CC.color(line)));
                return true;
            }
            if(args.length>1&&args[1].equalsIgnoreCase("validate")){
                sender.sendMessage(CC.color("&7[SMP] Cooking validation: &e"+game.getCookingListener().validateBoards()));
                sender.sendMessage(CC.color("&7[SMP] Cooking categories: &e"+game.getCookingListener().validateCategories()));
                sendFoodShopValidation(sender, game);
                return true;
            }
            sender.sendMessage(CC.color("&cUsage: /smp cooking <help|debugboard|validate>"));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("food")) {
            if (!sender.hasPermission("fakevival.cooking.admin") && !sender.hasPermission("fakevival.items.admin")
                    && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            if (args.length > 1 && args[1].equalsIgnoreCase("validate")) {
                sender.sendMessage(CC.color("&7[SMP] Food registry validation:"));
                sendFoodShopValidation(sender, game);
                return true;
            }
            sender.sendMessage(CC.color("&cUsage: /smp food validate"));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("alerts")) return handleAlerts(sender, args);
        if (args.length > 0 && args[0].equalsIgnoreCase("macrocheck")) {
            String nested = "macrocheck" + (args.length > 1 ? " " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "");
            return Bukkit.dispatchCommand(sender, nested);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("networkstatus")) {
            return Bukkit.dispatchCommand(sender, "networkstatus");
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("collections")) {
            return Bukkit.dispatchCommand(sender, "collections" + (args.length > 1 ? " " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : ""));
        }
        if(args.length>0&&args[0].equalsIgnoreCase("recipes"))return Bukkit.dispatchCommand(sender,"recipes"+(args.length>1?" "+String.join(" ",Arrays.copyOfRange(args,1,args.length)):""));

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("smp.admin.reload") && !sender.hasPermission("fakevival.admin.reload") && !sender.hasPermission("fakevival.devcontrols.admin") && !sender.hasPermission("smp.dev")) {
                return CommandUtil.adminDenied(sender);
            }

            plugin.reloadConfig();
            if (plugin instanceof Game) ((Game) plugin).getConfigFileManager().reload();
            liveControls.reload();
            spawnManager.loadSpawn();
            bazaarManager.refreshCustomMaterialRegistry();
            if (plugin instanceof Game game && game.getItemBrowserCommand() != null) game.getItemBrowserCommand().rebuildCache();
            if (plugin instanceof Game game && game.getMmoStatService() != null)
                Bukkit.getOnlinePlayers().forEach(game.getMmoStatService()::refreshPlayerMMOStats);
            boardManager.start();
            sender.sendMessage(CC.color("&aSMP config reloaded."));
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("dev") || args[0].equalsIgnoreCase("devcontrols") || args[0].equalsIgnoreCase("developercontrols"))) {
            return handleDev(sender, args);
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("event") || args[0].equalsIgnoreCase("events"))) {
            return handleEvent(sender, args);
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("mayor") || args[0].equalsIgnoreCase("election"))) {
            return Bukkit.dispatchCommand(sender, "mayor" + (args.length > 1 ? " " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : ""));
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("zones")) {
            return handleZones(sender, args);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("dungeon")) {
            String nested = args.length == 1 ? "dungeon" : "dungeon " + String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            return Bukkit.dispatchCommand(sender, nested);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("dungeons")) {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            menuManager.openDungeons(player);
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("deaths") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("smp.admin") && !sender.hasPermission("fakevival.admin") && !sender.hasPermission("smp.dev")) {
                return CommandUtil.adminDenied(sender);
            }
            if (!(plugin instanceof Game game)) return true;
            var result = game.getSmpDeathMessageListener().validate();
            sender.sendMessage(CC.color("&7[SMP] Death validation: PlayerDeathEvent central="
                    + yes(result.playerDeathBroadcasts()) + "&7, damage event stores only="
                    + yes(result.damageEventStoresOnly()) + "&7, vanilla suppressed="
                    + yes(result.vanillaSuppressed()) + "&7, debounce 1500ms="
                    + yes(result.debounce1500Millis()) + "&7, Abelix prefix="
                    + yes(result.abelixPrefix()) + "&7, PDC identity=" + yes(result.pdcIdentity()) + "&7."));
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("mobs") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("smp.admin") && !sender.hasPermission("fakevival.admin") && !sender.hasPermission("smp.dev")) {
                return CommandUtil.adminDenied(sender);
            }
            if (!(plugin instanceof Game game)) return true;
            var result = game.getDungeonManager().validateDungeonMobs(args.length > 2 ? args[2] : null, true);
            sender.sendMessage(CC.color("&7Dungeon mob validation: checked=&e" + result.checked() + "&7, repaired=&e"
                    + result.repaired() + "&7, invalid=&e" + result.invalid() + "&7, dirty names=&e" + result.dirtyNames() + "&7."));
            sender.sendMessage(CC.color("&7Valid EntityTypes=" + yes(result.entityTypesValid()) + "&7, aliases="
                    + yes(result.aliasesValid()) + "&7, clean names=" + yes(result.displayNamesClean())
                    + "&7, MMO/PDC health=" + yes(result.pdcHealthSource()) + "&7, exact red nametag="
                    + yes(result.exactNametagFormat()) + "&7, YAML created=&afalse&7."));
            if (!result.unknownAliases().isEmpty()) sender.sendMessage(CC.color("&cUnknown aliases: " + String.join(", ", result.unknownAliases())));
            var zoneHealth = game.getZoneManager().validateBossHealthScaling();
            sender.sendMessage(CC.color("&7Normal mob Lv.15 HP=&e" + (long) zoneHealth.normalLevel15Health()
                    + "&7, expected 1000=" + yes(Math.abs(zoneHealth.normalLevel15Health() - 1000.0D) < 0.01D) + "&7."));
            sendSmpBossValidation(sender, game.getDungeonManager().validateDefaultDungeonBosses());
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("boss") && args[1].equalsIgnoreCase("validate")) {
            if (!sender.hasPermission("smp.admin") && !sender.hasPermission("fakevival.admin") && !sender.hasPermission("smp.dev")) {
                return CommandUtil.adminDenied(sender);
            }
            if (plugin instanceof Game game) {
                sendSmpBossValidation(sender, game.getDungeonManager().validateDefaultDungeonBosses());
                sendBossHealthValidation(sender, game);
            }
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("mobs") && args[1].equalsIgnoreCase("limits")) {
            if (!sender.hasPermission("smp.admin") && !sender.hasPermission("fakevival.admin") && !sender.hasPermission("smp.dev")) {
                return CommandUtil.adminDenied(sender);
            }
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            if (!(plugin instanceof Game game)) return true;
            var limits = game.getDungeonManager().mobLimits(player);
            sender.sendMessage(CC.color("&6Mobs nearby: &f" + limits.nearby()));
            sender.sendMessage(CC.color("&6Mobs in chunk: &f" + limits.chunkCount() + "&7/&f" + limits.maxChunk()));
            sender.sendMessage(CC.color("&6Mobs in room: &f" + limits.roomCount() + "&7/&f" + limits.maxRoom()));
            sender.sendMessage(CC.color("&6Active mobs near player: &f" + limits.active() + "&7/&f" + limits.maxActive()));
            sender.sendMessage(CC.color("&6Dungeon mobs alive: &f" + limits.alive() + "&7/&f" + limits.maxInstance()));
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("levels") || args[0].equalsIgnoreCase("playerlevel"))) {
            if (args.length > 1 && args[1].equalsIgnoreCase("debug")) {
                String target = args.length > 2 ? " " + args[2] : "";
                return Bukkit.dispatchCommand(sender, "smp debug stats" + target);
            }
            if (args.length > 1 && args[1].equalsIgnoreCase("debugrequirements")) {
                if (!sender.hasPermission("fakevival.level.admin")) return CommandUtil.adminDenied(sender);
                int fallback = plugin instanceof Game game && sender instanceof Player p ? game.getPlayerLevelManager().getLevel(p) + 1 : 1;
                int level = args.length > 2 ? integer(args[2], fallback) : fallback;
                if (plugin instanceof Game game) game.getPlayerLevelManager().debugRequirementLines(level).forEach(line -> sender.sendMessage(CC.color(line)));
                return true;
            }
            return Bukkit.dispatchCommand(sender, "playerlevel" + (args.length > 1 ? " " + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : ""));
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("cleanrolllore")) {
            if (!sender.hasPermission("fakevival.item.admin")) return CommandUtil.adminDenied(sender);
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            ItemStack held = player.getInventory().getItemInMainHand();
            if (!(plugin instanceof Game game) || !game.getCustomItemManager().cleanVisibleRollLore(player, held)) {
                sender.sendMessage(CC.color("&cHold a valid custom item first."));
                return true;
            }
            sender.sendMessage(CC.color("&aRemoved visible stat roll lore from your held item."));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("furniture")) return handleFurniture(sender, args);

        if (args.length > 0 && (args[0].equalsIgnoreCase("playerdatasync") || args[0].equalsIgnoreCase("pdsync") || args[0].equalsIgnoreCase("playerdata"))) {
            return playerDataSyncManager.handleCommand(sender, args);
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("debug")
                && Set.of("protection", "deathmessage", "stats").contains(args[1].toLowerCase(java.util.Locale.ROOT))) {
            if (!sender.hasPermission("smp.dev") && !sender.hasPermission("fakevival.debug.combat")) return CommandUtil.adminDenied(sender);
            if (!(plugin instanceof Game game)) return true;
            Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : sender instanceof Player player ? player : null;
            if (target == null) { sender.sendMessage(CC.color("&cPlayer not found.")); return true; }
            if (args[1].equalsIgnoreCase("protection")) {
                sender.sendMessage(CC.color("&7Player: &e" + target.getName()));
                sender.sendMessage(CC.color("&7Server: &e" + game.getNetworkService().getCurrentServerId()
                        + " &7(" + game.getNetworkService().identity().serverType() + ")"));
                sender.sendMessage(CC.color("&7Buildmode: " + (game.getSpawnProtectionManager().isBuildMode(target) ? "&atrue" : "&cfalse")));
                sender.sendMessage(CC.color("&7Hub build bypass: " + (game.getSpawnProtectionManager().canBuildInHub(target) ? "&atrue" : "&cfalse")));
                sender.sendMessage(CC.color("&7Protected location: &e" + game.getSpawnProtectionManager().isInside(target.getLocation())));
                return true;
            }
            if (args[1].equalsIgnoreCase("deathmessage")) {
                long age = game.getSmpDeathMessageListener().lastAnnouncementAge(target.getUniqueId());
                String prefix = game.getAbelixHook().getPrefix(target);
                sender.sendMessage(CC.color("&7Player: &e" + target.getName()));
                sender.sendMessage(CC.color("&7Abelix prefix: &f" + (prefix == null || prefix.isBlank() ? "none" : prefix)));
                sender.sendMessage(CC.color("&7Custom death debounce: &e" + (age < 0L ? "never" : age + "ms ago")));
                sender.sendMessage(CC.color("&7Vanilla suppression for tagged SMP attackers: &atrue"));
                return true;
            }
            var levels = game.getPlayerLevelManager();
            var stats = game.getMmoStatService().getTotalStats(target);
            var gear = game.getMmoStatService().getEquipmentStats(target, true);
            var buffs = game.getCustomItemManager().getTemporaryConsumableStats(target);
            var settings = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
            double baseHp = settings.getDouble("playerlevel.baseHp", 100.0D);
            double baseDefense = settings.getDouble("playerlevel.baseDefense", 0.0D);
            double baseMana = settings.getDouble("playerlevel.baseMana", 100.0D);
            double rawDefense = stats.get(MMOStatType.DEFENSE);
            double effective = CombatBalance.effectiveDefense(plugin, rawDefense);
            sender.sendMessage(CC.color("&7PlayerLevel: &e" + levels.getLevel(target) + " / " + levels.getMaxLevel()));
            sender.sendMessage(CC.color("&7Base HP: &e" + format(baseHp)));
            sender.sendMessage(CC.color("&7HP from PlayerLevel: &e" + format(levels.getLevelHealthBonus(levels.getLevel(target)))));
            sender.sendMessage(CC.color("&7Defense from level: &e" + format(levels.getLevelDefenseBonus(levels.getLevel(target)))));
            sender.sendMessage(CC.color("&7Gear HP: &e" + format(gear.get(MMOStatType.HEALTH))));
            sender.sendMessage(CC.color("&7Gear Defense: &e" + format(gear.get(MMOStatType.DEFENSE))));
            sender.sendMessage(CC.color("&7Buff HP: &e" + format(buffs.get(MMOStatType.HEALTH))));
            sender.sendMessage(CC.color("&7Buff Defense: &e" + format(buffs.get(MMOStatType.DEFENSE))));
            sender.sendMessage(CC.color("&7Total HP: &e" + format(stats.get(MMOStatType.HEALTH))));
            sender.sendMessage(CC.color("&7Base Defense: &e" + format(baseDefense)));
            sender.sendMessage(CC.color("&7Raw Defense: &e" + format(rawDefense)));
            sender.sendMessage(CC.color("&7Effective Defense: &e" + format(effective)));
            sender.sendMessage(CC.color("&7Damage Reduction: &e" + format(CombatBalance.reductionPercent(plugin, rawDefense)) + "%"));
            sender.sendMessage(CC.color("&7Minimum Damage: &e" + format(CombatBalance.minimumDamage(plugin, 10.0D)) + " &7(for 10 incoming)"));
            sender.sendMessage(CC.color("&7Base Mana: &e" + format(baseMana)));
            sender.sendMessage(CC.color("&7Mana from PlayerLevel: &e" + format(levels.getLevelManaBonus(levels.getLevel(target)))));
            sender.sendMessage(CC.color("&7Gear Mana: &e" + format(gear.get(MMOStatType.MANA))));
            sender.sendMessage(CC.color("&7Buff Mana: &e" + format(buffs.get(MMOStatType.MANA))));
            sender.sendMessage(CC.color("&7Total Mana: &e" + format(stats.get(MMOStatType.MANA))));
            return true;
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("combat")) {
            if (!sender.hasPermission("fakevival.debug.combat")) {
                sender.sendMessage(CC.color("&cYou do not have permission to use combat debug."));
                return true;
            }
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            if (!(plugin instanceof Game game)) return true;
            boolean enabled;
            if (args.length < 3) enabled = game.getCombatDebugManager().toggleCombatDebug(player);
            else if (args[2].equalsIgnoreCase("on")) enabled = game.getCombatDebugManager().setCombatDebug(player, true);
            else if (args[2].equalsIgnoreCase("off")) enabled = game.getCombatDebugManager().setCombatDebug(player, false);
            else {
                sender.sendMessage(CC.color("&cUsage: /smp debug combat [on|off]"));
                return true;
            }
            sender.sendMessage(CC.color(enabled ? "&aCombat debug enabled." : "&cCombat debug disabled."));
            return true;
        }
        if (args.length > 0 && (args[0].equalsIgnoreCase("pregenerate") || args[0].equalsIgnoreCase("pregen"))) {
            if(plugin instanceof Game game&&!game.getNetworkService().isOverworld()){sender.sendMessage(CC.color(game.getNetworkService().isDungeon()?"&cPregen cannot be used on dungeon servers.":"&cPregen can only be used on overworld servers."));return true;}
            return pregeneratorManager.handle(sender, args, 1);
        }
        if (args.length > 0 && (args[0].equalsIgnoreCase("worldedit") || args[0].equalsIgnoreCase("we"))) return worldEditManager.handle(sender, args, 1);

        if (args.length > 0 && args[0].equalsIgnoreCase("sandbox")) {
            return Bukkit.dispatchCommand(sender, "sandbox");
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            menuManager.openMainMenu(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("hubsave")) {
            if (!sender.hasPermission("smp.admin.hub") && !sender.hasPermission("fakevival.admin.hub")) {
                return CommandUtil.adminDenied(sender);
            }
            if (args.length > 1 && args[1].equalsIgnoreCase("export")) {
                File exportFile = hubManager.exportHubStaticRegistryJson();
                if (exportFile == null) {
                    sender.sendMessage(CC.color("&cCould not export hub StaticRegistryData JSON. Check console."));
                } else {
                    sender.sendMessage(CC.color("&aExported hub StaticRegistryData JSON to &f" + exportFile.getName() + "&a."));
                }
                return true;
            }
            if (args.length > 1 && args[1].equalsIgnoreCase("exportstatic") && plugin instanceof Game game) {
                sender.sendMessage(CC.color("&aExporting latest SMP setup snapshot for StaticRegistryData..."));
                game.getSetupSnapshotService().exportLatestJava().whenComplete((file,error)->Bukkit.getScheduler().runTask(plugin,()->{if(error!=null)sender.sendMessage(CC.color("&cFailed to export SMP setup snapshot. Check console."));else sender.sendMessage(CC.color("&aExported StaticRegistryData setup to &e"+file.getFileName()+"&a."));}));
                return true;
            }
            if (plugin instanceof Game game && !game.getNetworkService().isHub()) {
                sender.sendMessage(CC.color("&aSaving SMP network setup..."));
                plugin.getLogger().info("[SMP] /smp hubsave server="+game.getNetworkService().getCurrentServerId()+" type="+game.getNetworkService().getCurrentServerType()+" systems=SMPWorlds/database registries");
                if(sender instanceof Player player && (game.getNetworkService().isOverworld() || player.getWorld().getName().equalsIgnoreCase("lobby"))){
                    String key=game.getNetworkService().isDungeon()?"dungeon_lobby":"overworld";
                    game.getNetworkService().saveServerSpawn(player,key,saved->saveSMPWorldsAfterHubSave(sender,!saved));
                }else saveSMPWorldsAfterHubSave(sender,false);
                return true;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cOnly players can save the active hub bundle because the spawn location comes from your current position."));
                return true;
            }
            if (!hubManager.isDatabaseEnabled()) {
                sender.sendMessage(CC.color("&cHub saving requires MariaDB persistence."));
                return true;
            }
            if (hubManager.isHubSaveRunning()) {
                sender.sendMessage(CC.color("&cA hub save is already running."));
                return true;
            }
            org.bukkit.Location saveLocation = player.getLocation().clone();
            sender.sendMessage(CC.color("&aSaving SMP network setup..."));
            if (!hubManager.saveHubDataAsync(saveLocation, player.getUniqueId().toString(), success -> {
                if (success) {
                    spawnManager.setCachedSpawn(saveLocation);
                    if(plugin instanceof Game game)game.getNetworkService().saveServerSpawn(player,"hub",saved->saveSMPWorldsAfterHubSave(sender,!saved));
                    else saveSMPWorldsAfterHubSave(sender,false);
                } else {
                    sender.sendMessage(CC.color("&cFailed to save hub data. Check console for details."));
                }
            })) {
                sender.sendMessage(CC.color("&cA hub save is already running."));
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("bazaarprice")) {
            return handleBazaarPrice(sender, args);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("bazaarreset")) {
            return handleBazaarReset(sender, args);
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("bazaarreload")) {
            return handleBazaarReload(sender);
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("bazaar") && args[1].equalsIgnoreCase("validate")) {
            return handleBazaarLifecycleValidation(sender);
        }

        if (args.length > 1 && args[0].equalsIgnoreCase("tasks") && args[1].equalsIgnoreCase("validate")) {
            return handleBazaarLifecycleValidation(sender);
        }

        sender.sendMessage(CC.color("&bSMP Commands:&f /spawn, /skills, /jobs, /fly, /gmc, /gms, /gmsp, /gma, /invsee, /npc"));
        sender.sendMessage(CC.color("&7Admin:&f /smp reload&7, &f/smp hubsave&7, &f/smp event status&7, &f/smp playerdatasync status&7, &f/smp devcontrols"));
        return true;
    }

    private boolean handleDupe(CommandSender sender,String[] args){
        if(!sender.hasPermission("fakevival.dupe.admin")&&!sender.hasPermission("smp.dev"))return CommandUtil.adminDenied(sender);
        if(!(plugin instanceof Game game))return true;
        if(args.length>2&&args[1].equalsIgnoreCase("readonly")){boolean value=args[2].equalsIgnoreCase("on")||args[2].equalsIgnoreCase("true");game.getDupeDetectionManager().setReadOnlyMode(value);sender.sendMessage(CC.color(value?"&aDupe detection read-only mode enabled.":"&cDupe detection read-only mode disabled."));return true;}
        if(args.length>2&&args[1].equalsIgnoreCase("scan")){Player target=Bukkit.getPlayerExact(args[2]);if(target==null){sender.sendMessage(CC.color("&cPlayer not found."));return true;}var report=game.getDupeDetectionManager().scanPlayer(target);sender.sendMessage(CC.color("&7Dupe scan: player=&e"+target.getName()+"&7 uniqueItems=&e"+report.uniqueItems()+"&7 duplicates=&e"+report.duplicates()+"&7 readOnly=&e"+report.readOnly()));return true;}
        if(args.length>2&&args[1].equalsIgnoreCase("debug")){Player target=Bukkit.getPlayerExact(args[2]);if(target==null){sender.sendMessage(CC.color("&cPlayer not found."));return true;}var report=game.getDupeDetectionManager().debug(target);sender.sendMessage(CC.color("&7Read only: &e"+report.readOnly()));sender.sendMessage(CC.color("&7Auto remove: &e"+report.autoRemove()));sender.sendMessage(CC.color("&7Cancel suspicious clicks: &e"+report.cancelSuspiciousClicks()));sender.sendMessage(CC.color("&7Queued scan: &e"+report.queued()));sender.sendMessage(CC.color("&7Last scan age: &e"+(report.lastScanAgeMillis()<0?"never":report.lastScanAgeMillis()+"ms")));sender.sendMessage(CC.color("&7Suppressed until: &e"+(report.suppressedUntilMillis()<=System.currentTimeMillis()?"not suppressed":new java.util.Date(report.suppressedUntilMillis()).toString())));sender.sendMessage(CC.color("&7Recent scan requests: &e"+report.recentScanRequests()));sender.sendMessage(CC.color("&7Recent alerts: &e"+report.recentAlerts()));return true;}
        if(args.length>2&&args[1].equalsIgnoreCase("repair")){Player target=Bukkit.getPlayerExact(args[2]);if(target==null){sender.sendMessage(CC.color("&cPlayer not found."));return true;}var report=game.getDupeDetectionManager().repairPlayer(target);plugin.getLogger().warning("[SMP Dupe] Manual admin repair actor="+sender.getName()+" player="+target.getName()+" inspected="+report.inspectedItems()+" removed="+report.removedDuplicates());sender.sendMessage(CC.color("&aManual dupe repair complete for &e"+target.getName()+"&a: inspected=&e"+report.inspectedItems()+"&a, confirmed duplicates removed=&e"+report.removedDuplicates()+"&a."));return true;}
        if(args.length>1&&args[1].equalsIgnoreCase("validate")){var manager=game.getDupeDetectionManager();var report=manager.validatePdcSafety();var crafting=manager.validateCraftingSafety();sender.sendMessage(CC.color("&7[SMP] Dupe validation: readOnly="+yes(manager.isReadOnlyMode())+"&7, autoRemove=&e"+manager.isAutoRemoveItems()+"&7, cancelClicks=&e"+manager.cancelSuspiciousClicks()+"&7, scanSafe=&e"+report.scanSafe()+"&7, unique_item_id STRING=&e"+report.uniqueIdString()+"&7, edition LONG=&e"+report.editionLong()+"&7, fish fields excluded=&e"+report.fishFieldsExcluded()+"&7, craftingScan="+yes(crafting.craftingScanEnabled())+"&7, delayed2to5Ticks="+yes(crafting.delayed())+"&7, craftingMutation=&cfalse&7, craftingUpdateInventory=&cfalse&7, craftingPlayerDataSave=&cfalse&7, suppressedWarnSpam=&cfalse&7, explicitUniqueIdsOnly=&atrue&7, manualRepairOnly=&atrue&7."));return true;}
        sender.sendMessage(CC.color("&cUsage: /smp dupe <debug|scan|repair> <player> or /smp dupe readonly <on|off>"));return true;
    }

    private boolean handleAnvil(CommandSender sender,String[] args){
        if(!sender.hasPermission("fakevival.anvil.admin")&&!sender.hasPermission("fakevival.devcontrols.admin")&&!sender.hasPermission("smp.dev"))return CommandUtil.adminDenied(sender);
        if(!(plugin instanceof Game game)||game.getCustomAnvilListener()==null)return true;
        if(args.length>1&&args[1].equalsIgnoreCase("validate")){sender.sendMessage(CC.color("&7[SMP] Anvil validation: &e"+game.getCustomAnvilListener().validateMenu()));return true;}
        if(args.length>1&&args[1].equalsIgnoreCase("open")){if(!(sender instanceof Player player))return CommandUtil.playersOnly(sender);game.getCustomAnvilListener().open(player,0);return true;}
        sender.sendMessage(CC.color("&cUsage: /smp anvil <open|validate>"));return true;
    }

    private boolean handleAlerts(CommandSender sender, String[] args) {
        if (!(plugin instanceof Game game)) return true;
        SmpAlertService alerts = game.getSmpAlertService();
        if (args.length == 1) {
            if (!(sender instanceof Player player) || !sender.hasPermission("fakevival.alerts.staff")) {
                sender.sendMessage(CC.color("&cYou do not have permission to use SMP alerts.")); return true;
            }
            sender.sendMessage(CC.color(alerts.toggle(player) ? "&aSMP alerts enabled." : "&cSMP alerts disabled."));
            return true;
        }
        if (!sender.hasPermission("fakevival.alerts.admin")) {
            sender.sendMessage(CC.color("&cYou do not have permission to manage alert thresholds.")); return true;
        }
        if (args[1].equalsIgnoreCase("thresholds")) {
            sender.sendMessage(CC.color("&6SMP Alert Thresholds"));
            alerts.thresholds().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                    sender.sendMessage(CC.color("&8- &e" + entry.getKey() + "&7: &6" + EconomyManager.formatCompact(entry.getValue()))));
            return true;
        }
        if (args[1].equalsIgnoreCase("setthreshold") && args.length == 4) {
            try {
                SmpAlertService.Type type = SmpAlertService.Type.valueOf(args[2].toUpperCase(java.util.Locale.ROOT));
                double amount = Double.parseDouble(args[3].replace(",", ""));
                if (!Double.isFinite(amount) || amount < 0D) throw new NumberFormatException();
                alerts.setThreshold(type, amount);
                sender.sendMessage(CC.color("&aSet &e" + type + " &ato &6" + EconomyManager.formatCompact(amount) + "&a."));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(CC.color("&cType must be AUCTION_BID, TRADE_COINS, BANK_WITHDRAW, or BAZAAR_SELL, with a valid amount."));
            }
            return true;
        }
        if (args[1].equalsIgnoreCase("test") && args.length == 3) {
            try {
                SmpAlertService.Type type = SmpAlertService.Type.valueOf(args[2].toUpperCase(java.util.Locale.ROOT));
                String name = sender.getName();
                java.util.UUID uuid = sender instanceof Player player ? player.getUniqueId() : null;
                alerts.alert(type, uuid, "&c[SMP ALERT] &e" + name + " &7triggered a &6" + type + " &7test alert.",
                        "**" + name + "** triggered a **" + type + "** test alert. Server: `" + alerts.serverId() + "`",
                        "test=true actor=" + name + " type=" + type + " server=" + alerts.serverId() + " at=" + System.currentTimeMillis());
            } catch (IllegalArgumentException e) { sender.sendMessage(CC.color("&cUnknown alert type.")); }
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /smp alerts [thresholds|setthreshold <type> <amount>]"));
        return true;
    }

    private void saveSMPWorldsAfterHubSave(CommandSender sender,boolean localPartial) {
        if (!(plugin instanceof Game game)) {
            sender.sendMessage(CC.color("&aHub data saved successfully."));
            return;
        }
        SMPWorldManager worldManager = game.getSMPWorldManager();
        if (worldManager == null) {
            sender.sendMessage(CC.color("&aHub data saved successfully."));
            return;
        }
        worldManager.saveLoadedWorldsToDatabase().whenComplete((result, throwable) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (throwable != null) {
                        sender.sendMessage(CC.color("&eSetup saved, but some SMPWorlds/database saves failed. Check console."));
                        plugin.getLogger().severe("[SMPWorlds] /smp hubsave database save failed: " + rootMessage(throwable));
                        createSetupSnapshot(sender,true);
                        return;
                    }
                    if (result.hasFailures()) {
                        sender.sendMessage(CC.color("&eHub saved, but some SMPWorlds failed to save: &c"
                                + result.failedCount()));
                        sender.sendMessage(CC.color("&7Check console for details. SMPWorlds saved: &a"
                                + result.savedCount() + " &7world(s), skipped: &e" + result.skippedCount() + "&7."));
                        createSetupSnapshot(sender,true);
                        return;
                    }
                    String success;
                    if (game.getNetworkService().isOverworld()) success="&aOverworld setup saved. &7Saved worlds: &a"+result.savedCount()+"&7.";
                    else if(game.getNetworkService().isDungeon()) success="&aDungeon setup saved. &7Saved templates: &a"+result.savedCount()+"&7.";
                    else success="&aHub setup saved. &7Saved worlds: &a"+result.savedCount()+"&7.";
                    sender.sendMessage(CC.color(success));
                    if (result.skippedCount() > 0) {
                        sender.sendMessage(CC.color("&7SMPWorlds skipped: &e" + result.skippedCount()
                                + " &7world(s)."));
                    }
                    createSetupSnapshot(sender,localPartial);
                }));
    }

    private void createSetupSnapshot(CommandSender sender,boolean partial){
        if(!(plugin instanceof Game game))return;java.util.UUID creator=sender instanceof Player player?player.getUniqueId():null;
        game.getSetupSnapshotService().createSnapshot(creator).whenComplete((snapshot,error)->Bukkit.getScheduler().runTask(plugin,()->{if(error!=null){plugin.getLogger().severe("[SMP Setup] Snapshot export failed: "+rootMessage(error));sender.sendMessage(CC.color("&cFailed to save SMP network setup. Check console."));}else if(partial)sender.sendMessage(CC.color("&eSMP setup saved, but some parts failed. Check console. &7Snapshot: &e"+snapshot.snapshotId()));else sender.sendMessage(CC.color("&aSMP network setup saved. &7Snapshot: &e"+snapshot.snapshotId()));}));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private boolean handleFurniture(CommandSender sender, String[] args) {
        String operation=args.length>=2?args[1].toLowerCase(java.util.Locale.ROOT):"";
        boolean cleanup=java.util.Set.of("cleanupserver","removenear","removehere","debugnear","debugbarriers","info","cleanupduplicates").contains(operation);
        boolean override=club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("furniture.admin-override-enabled",false)
                && sender.hasPermission("fakevival.furniture.admin");
        if(!furnitureManager.isEnabled()&&!cleanup&&!override){sender.sendMessage(CC.color("&cFurniture is currently disabled."));return true;}
        if (args.length >= 2 && args[1].equalsIgnoreCase("shop")) {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            furnitureManager.openShop(player, FurnitureDefinition.Category.CHAIRS); return true;
        }
        if (args.length >= 2 && (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("pickup")
                || args[1].equalsIgnoreCase("delete") && args.length == 2)) {
            if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
            furnitureManager.pickupTarget(player);
            return true;
        }
        if (!sender.hasPermission("fakevival.furniture.admin") && !sender.hasPermission("fakevival.devcontrols.admin") && !sender.hasPermission("smp.dev")) {
            sender.sendMessage(CC.color("&cYou do not have permission to use this command.")); return true;
        }
        if (args.length < 2) { sender.sendMessage(CC.color("&cUsage: /smp furniture <create|editor|preview|testplace|give|removenear|cleanupserver|list|shop>")); return true; }
        switch (args[1].toLowerCase()) {
            case "create" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); if(args.length<4){sender.sendMessage(CC.color("&cUsage: /smp furniture create <id> <displayName>"));return true;} sender.sendMessage(CC.color(furnitureEditor.create(player,args[2],String.join(" ",Arrays.copyOfRange(args,3,args.length))))); }
            case "editor" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); sender.sendMessage(CC.color(furnitureEditor.editor(player))); }
            case "addpart" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); sender.sendMessage(CC.color(furnitureEditor.addPart(player))); }
            case "removepart" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); if(args.length<3)return usageFurniture(sender);sender.sendMessage(CC.color(furnitureEditor.removePart(player,integer(args[2],-1)))); }
            case "movepart" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); if(args.length<6)return usageFurniture(sender);try{sender.sendMessage(CC.color(furnitureEditor.movePart(player,Integer.parseInt(args[2]),Double.parseDouble(args[3]),Double.parseDouble(args[4]),Double.parseDouble(args[5]))));}catch(NumberFormatException e){sender.sendMessage(CC.color("&cPart index and offsets must be numbers."));} }
            case "rotatepart" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); if(args.length<6)return usageFurniture(sender);try{sender.sendMessage(CC.color(furnitureEditor.rotatePart(player,Integer.parseInt(args[2]),Float.parseFloat(args[3]),Float.parseFloat(args[4]),Float.parseFloat(args[5]))));}catch(NumberFormatException e){sender.sendMessage(CC.color("&cPart index and rotation must be numbers."));} }
            case "partmaterial" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); if(args.length<4)return usageFurniture(sender);sender.sendMessage(CC.color(furnitureEditor.material(player,integer(args[2],-1),args[3]))); }
            case "parttype" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); if(args.length<4)return usageFurniture(sender);sender.sendMessage(CC.color(furnitureEditor.type(player,integer(args[2],-1),args[3]))); }
            case "setprice" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);try{sender.sendMessage(CC.color(furnitureEditor.setPrice(player,Double.parseDouble(args[2]))));}catch(Exception e){sender.sendMessage(CC.color("&cUsage: /smp furniture setprice <coins>"));} }
            case "setcategory" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);if(args.length<3)return usageFurniture(sender);sender.sendMessage(CC.color(furnitureEditor.setCategory(player,args[2]))); }
            case "importmodel", "listimports" -> furnitureEditor.folderImportDisabled(message->sender.sendMessage(CC.color(message)));
            case "importbuiltin" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);if(args.length<3){sender.sendMessage(CC.color("&cUsage: /smp furniture importbuiltin <id>"));return true;}furnitureEditor.importBuiltin(player,args[2],message->sender.sendMessage(CC.color(message))); }
            case "importjson" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);if(args.length<4){sender.sendMessage(CC.color("&cUsage: /smp furniture importjson <id> <base64Json>"));return true;}furnitureEditor.importJson(player,args[2],args[3],message->sender.sendMessage(CC.color(message))); }
            case "listbuiltin" -> {sender.sendMessage(CC.color("&bBuilt-in furniture models:"));furnitureEditor.builtinIds().forEach(id->sender.sendMessage(CC.color("&7- &e"+id)));}
            case "preview" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);SmpFurnitureModel model=furnitureEditor.draft(player).orElse(null);sender.sendMessage(CC.color(model!=null&&furnitureManager.preview(player,model)?"&aPreviewing the draft for 20 seconds.":"&cCould not preview the current draft here.")); }
            case "save" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); furnitureEditor.save(player,message->sender.sendMessage(CC.color(message))); }
            case "cancel" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); sender.sendMessage(CC.color(furnitureEditor.cancel(player))); }
            case "testplace" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); FurnitureDefinition definition=args.length>2?furnitureRegistry.get(args[2]).orElse(null):null; sender.sendMessage(CC.color(definition!=null&&furnitureManager.testPlace(player,definition)?"&aPlaced furniture test model.":"&cCould not test-place that furniture here.")); }
            case "delete" -> { if(args.length<3){sender.sendMessage(CC.color("&cUsage: /smp furniture delete <id>"));return true;} furnitureRegistry.delete(args[2],deleted->sender.sendMessage(CC.color(deleted?"&aDeleted furniture &e"+args[2]+"&a.":"&cCould not delete that furniture. It may be placed or missing."))); }
            case "reload" -> { furnitureRegistry.reload(); sender.sendMessage(CC.color("&aReloaded &e" + furnitureRegistry.all().size() + " &afurniture items from the database.")); }
            case "list" -> {
                sender.sendMessage(CC.color("&bInstalled furniture packs:"));
                if (furnitureRegistry.packCounts().isEmpty()) sender.sendMessage(CC.color("&7None."));
                furnitureRegistry.packCounts().forEach((id,count)->sender.sendMessage(CC.color("&7- &e"+id+"&7: &f"+count+" items")));
                sender.sendMessage(CC.color("&bCategories:"));
                for (FurnitureDefinition.Category category : FurnitureDefinition.Category.values()) sender.sendMessage(CC.color("&7- &e"+category.displayName()+"&7: &f"+furnitureRegistry.category(category).size()));
            }
            case "give" -> {
                if (args.length < 4) { sender.sendMessage(CC.color("&cUsage: /smp furniture give <player> <furnitureId> [amount]")); return true; }
                Player player=Bukkit.getPlayerExact(args[2]); FurnitureDefinition f=furnitureRegistry.get(args[3]).orElse(null); int amount=1;
                if(args.length>4)try{amount=Math.max(1,Math.min(64,Integer.parseInt(args[4])));}catch(NumberFormatException ignored){}
                if(player==null||f==null)sender.sendMessage(CC.color("&cPlayer or furniture id was not found."));else{player.getInventory().addItem(furnitureManager.createItem(f,amount));sender.sendMessage(CC.color("&aGave furniture to &e"+player.getName()+"&a."));}
            }
            case "removehere" -> {
                if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); double radius=5;
                if(args.length>2)try{radius=Math.max(1,Math.min(32,Double.parseDouble(args[2])));}catch(NumberFormatException ignored){}
                sender.sendMessage(CC.color("&aRemoved &e"+furnitureManager.removeHere(player,radius)+" &afurniture entities."));
            }
            case "removenear" -> {
                if(!sender.hasPermission("fakevival.furniture.admin"))return CommandUtil.adminDenied(sender);
                if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); double radius=5;
                if(args.length>2)try{radius=Math.max(1,Math.min(32,Double.parseDouble(args[2])));}catch(NumberFormatException ignored){}
                sender.sendMessage(CC.color("&aRemoved &e"+furnitureManager.removeHere(player,radius)+" &afurniture groups."));
            }
            case "cleanupserver" -> {
                if(!sender.hasPermission("fakevival.furniture.admin"))return CommandUtil.adminDenied(sender);
                if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
                if(args.length<3||!args[2].equalsIgnoreCase("--confirm")){sender.sendMessage(CC.color("&cThis removes tracked furniture from the current server world only."));sender.sendMessage(CC.color("&eRun /smp furniture cleanupserver --confirm to continue."));return true;}
                int removed=furnitureManager.cleanupCurrentServerWorld(player);
                sender.sendMessage(CC.color("&aRemoved &e"+removed+" &afurniture groups from &e"+player.getWorld().getName()+"&a on this server."));
            }
            case "info" -> { if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender); furnitureManager.info(player).forEach(line->sender.sendMessage(CC.color(line))); }
            case "debugnear" -> {
                if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);double radius=16;
                if(args.length>2)try{radius=Math.max(1,Math.min(64,Double.parseDouble(args[2])));}catch(NumberFormatException ignored){}
                furnitureManager.debugNear(player,radius).forEach(line->sender.sendMessage(CC.color(line)));
            }
            case "cleanupduplicates" -> {
                if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);double radius=16;
                if(args.length>2)try{radius=Math.max(1,Math.min(64,Double.parseDouble(args[2])));}catch(NumberFormatException ignored){}
                sender.sendMessage(CC.color(furnitureManager.cleanupDuplicates(player,radius)));
            }
            case "debugbarriers" -> {
                if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);double radius=16;
                if(args.length>2)try{radius=Math.max(1,Math.min(64,Double.parseDouble(args[2])));}catch(NumberFormatException ignored){}
                sender.sendMessage(CC.color(furnitureManager.debugBarriers(player,radius)));
            }
            case "debugrotation", "debugplacement" -> {
                if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
                if(args.length<3){sender.sendMessage(CC.color("&cUsage: /smp furniture debugrotation <id>"));return true;}
                furnitureManager.debugRotation(player,args[2]).forEach(line->sender.sendMessage(CC.color(line)));
            }
            default -> sender.sendMessage(CC.color("&cUsage: /smp furniture <create|editor|preview|testplace|give|removenear|cleanupserver|list|shop>"));
        }
        return true;
    }

    private String format(double value){return String.format(java.util.Locale.ROOT,"%.2f",value).replaceAll("\\.?0+$","");}
    private int integer(String value,int fallback){try{return Integer.parseInt(value);}catch(NumberFormatException ignored){return fallback;}}
    private boolean usageFurniture(CommandSender sender){sender.sendMessage(CC.color("&cFurniture editor: create, addpart, removepart, movepart, rotatepart, partmaterial, parttype, setprice, setcategory, importbuiltin, importjson, save, cancel."));return true;}

    private boolean handleEvent(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.events.admin") && !sender.hasPermission("fakevival.event.admin")
                && !sender.hasPermission("fakevival.devcontrols.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            Player player = sender instanceof Player online ? online : null;
            for (String line : eventManager.statusLines(player)) sender.sendMessage(CC.color(line));
            return true;
        }
        if (args[1].equalsIgnoreCase("stop")) {
            sender.sendMessage(CC.color(eventManager.stopEvent() ? "&aStopped the active SMP event." : "&cNo SMP event is active."));
            return true;
        }
        if (args[1].equalsIgnoreCase("next")) {
            eventManager.forceNext();
            sender.sendMessage(CC.color("&aRerolled the next event. It starts in 10 seconds."));
            return true;
        }
        if (args[1].equalsIgnoreCase("start") && args.length >= 3) {
            sender.sendMessage(CC.color(eventManager.startEvent(args[2], sender.getName())
                    ? "&aStarting event &f" + args[2] + "&a."
                    : "&cUnknown event ID or the event has no valid server scope."));
            return true;
        }
        if (args[1].equalsIgnoreCase("reload")) {
            eventManager.reloadNetworkState();
            sender.sendMessage(CC.color("&aQueued an asynchronous network event refresh."));
            return true;
        }
        if (args[1].equalsIgnoreCase("debug")) {
            Player player = sender instanceof Player online ? online : null;
            eventManager.debugLines(player).forEach(line -> sender.sendMessage(CC.color(line)));
            return true;
        }
        if (args[1].equalsIgnoreCase("validate")) {
            var result = eventManager.validateNetworkEvents();
            sender.sendMessage(CC.color("&7[SMP Events] Validation: missingScopes=&e" + result.missingScopes()
                    + "&7, destructiveHub=&e" + result.destructiveHubEvents()
                    + "&7, invalidRequirements=&e" + result.invalidRequirements()
                    + "&7, placeholders=&e" + result.placeholdersResolve()
                    + "&7, timerHealthy=&e" + result.timerHealthy()
                    + "&7, dbCacheLoaded=&e" + result.cacheLoaded()
                    + "&7, scoreStorage=&e" + result.scoreDatabaseAvailable()
                    + "&7, hubWorldMutation=&cfalse&7."));
            return true;
        }
        sender.sendMessage(CC.color("&cUsage: /smp events <start <event_id>|stop|status|reload|debug|validate>"));
        return true;
    }

    private boolean handleZones(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.zones.admin") && !sender.hasPermission("fakevival.devcontrols.admin") && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
        if (args.length > 1 && args[1].equalsIgnoreCase("validate")) {
            handleLocationValidation(sender, "zones");
            if (plugin instanceof Game game) sendBossHealthValidation(sender, game);
            return true;
        }
        if (plugin instanceof Game game && !game.getAccessGuard().requireHub(sender,"&cZone commands can only be used on SMP hub servers.")) return true;
        if (!(plugin instanceof Game game) || args.length < 4) {
            sender.sendMessage(CC.color("&cUsage: /smp zones <discover|undiscover|unlocktravel> <player> <zone>"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(CC.color("&cThat player must be online."));
            return true;
        }
        String zone = args[3];
        if (!game.getZoneManager().exists(zone)) {
            sender.sendMessage(CC.color("&cZone &e" + zone + " &cdoes not exist."));
            return true;
        }
        boolean success = switch (args[1].toLowerCase()) {
            case "discover" -> game.getZoneManager().setDiscovered(target, zone, true);
            case "undiscover" -> game.getZoneManager().setDiscovered(target, zone, false);
            case "unlocktravel" -> game.getZoneManager().unlockTravel(target, zone);
            default -> false;
        };
        if (!success) sender.sendMessage(CC.color("&cUsage: /smp zones <discover|undiscover|unlocktravel> <player> <zone>"));
        else sender.sendMessage(CC.color("&aUpdated zone access for &e" + target.getName() + "&a: &f" + zone + "&a."));
        return true;
    }

    private boolean handleLocationValidation(CommandSender sender, String source) {
        if (!sender.hasPermission("fakevival.zones.admin") && !sender.hasPermission("fakevival.devcontrols.admin")
                && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
        if (!(plugin instanceof Game game)) return true;
        var report = game.getTablistManager().validateLocationResolver();
        sender.sendMessage(CC.color("&7[SMP] " + source + " location validation: hubNotWilderness="
                + yes(report.hubNotWilderness()) + "&7, hubFallback=" + yes(report.hubFallback())
                + "&7, smpIdFallback=" + yes(report.hubIdFallback()) + "&7, overworldWilderness="
                + yes(report.overworldFallback()) + "&7, dungeonFallback=" + yes(report.dungeonFallback())
                + "&7, namedZoneOverride=" + yes(report.namedZoneOverride())
                + "&7, sharedTablistScoreboardActionbarMenusPlaceholders=&atrue&7."));
        return true;
    }

    private boolean handleCraftingValidation(CommandSender sender) {
        if (!sender.hasPermission("fakevival.dupe.admin") && !sender.hasPermission("fakevival.devcontrols.admin")
                && !sender.hasPermission("smp.dev")) return CommandUtil.adminDenied(sender);
        if (!(plugin instanceof Game game)) return true;
        boolean workbench = club.abelix.games.fakevival.util.SmpInventoryPolicy
                .isVanillaCrafting(InventoryType.WORKBENCH, false);
        boolean playerCrafting = club.abelix.games.fakevival.util.SmpInventoryPolicy
                .isVanillaCrafting(InventoryType.CRAFTING, false);
        boolean customLocked = !club.abelix.games.fakevival.util.SmpInventoryPolicy
                .isVanillaCrafting(InventoryType.WORKBENCH, true);
        var dupe = game.getDupeDetectionManager().validateCraftingSafety();
        sender.sendMessage(CC.color("&7[SMP] Crafting validation: workbenchVanilla=" + yes(workbench)
                + "&7, playerCraftingVanilla=" + yes(playerCrafting) + "&7, customMenusSeparate=" + yes(customLocked)
                + "&7, dragClick=" + yes(workbench) + "&7, shiftClick=" + yes(workbench)
                + "&7, resultSlot=" + yes(workbench) + "&7, numberKey=" + yes(workbench)
                + "&7, cursorMovement=" + yes(workbench) + "&7, virtualWorkbenchNative=&atrue"
                + "&7, dupeReadOnly=" + yes(dupe.readOnly()) + "&7, dupeDelayed=" + yes(dupe.delayed())
                + "&7, dupeCancelsNormal=" + yes(!dupe.normalClicksNotCancelled())
                + "&7, dupeEventMutation=&cfalse&7, dupeUpdateInventory=&cfalse&7."));
        return true;
    }

    private boolean handleBazaarPrice(CommandSender sender, String[] args) {
        if (!hasBazaarAdmin(sender)) return CommandUtil.adminDenied(sender);
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /smp bazaarprice <item>"));
            return true;
        }
        List<String> lines = bazaarManager.marketStatusLines(args[1]);
        for (String line : lines) sender.sendMessage(CC.color(line));
        return true;
    }

    private boolean handleBazaarReset(CommandSender sender, String[] args) {
        if (!hasBazaarAdmin(sender)) return CommandUtil.adminDenied(sender);
        if (args.length < 2) {
            sender.sendMessage(CC.color("&cUsage: /smp bazaarreset <item>"));
            return true;
        }
        String itemId = args[1];
        sender.sendMessage(CC.color("&eResetting bazaar market price asynchronously..."));
        if (!bazaarManager.requestMarketReset(itemId, reset -> sender.sendMessage(CC.color(reset
                ? "&aReset bazaar market price for &f" + itemId + "&a."
                : "&cCould not reset that bazaar item. MariaDB may be unavailable or the item id is invalid.")))) {
            sender.sendMessage(CC.color("&cBazaar is unavailable or shutting down."));
        }
        return true;
    }

    private boolean handleBazaarReload(CommandSender sender) {
        if (!hasBazaarAdmin(sender)) return CommandUtil.adminDenied(sender);
        bazaarManager.refreshCustomMaterialRegistry();
        sender.sendMessage(CC.color(bazaarManager.requestMarketReload()
                ? "&eReloading bazaar market data asynchronously..."
                : "&7A Bazaar reload is already running or MariaDB is unavailable."));
        return true;
    }

    private boolean hasBazaarAdmin(CommandSender sender) {
        return sender.hasPermission("smp.admin.bazaar") || sender.hasPermission("smp.dev") || sender.hasPermission("fakevival.devcontrols.admin") || sender.hasPermission("fakevival.admin.reload");
    }

    private boolean handleBazaarLifecycleValidation(CommandSender sender) {
        if (!hasBazaarAdmin(sender)) return CommandUtil.adminDenied(sender);
        for (String line : bazaarManager.lifecycleValidationLines()) sender.sendMessage(CC.color(line));
        return true;
    }

    private void sendSmpBossValidation(CommandSender sender, club.abelix.games.fakevival.managers.DungeonManager.DungeonBossValidation result) {
        sender.sendMessage(CC.color("&7Default bosses: pirate=" + yes(result.pirateRegistered() && result.pirateDungeon())
                + "&7, sacred=" + yes(result.sacredRegistered() && result.sacredDungeon())
                + "&7, existing command=" + yes(result.commandUsable()) + "&7, EntityTypes=" + yes(result.entityTypesValid())
                + "&7, nametags=" + yes(result.nametagsValid()) + "&7, MMO/PDC health=" + yes(result.pdcHealthSource())
                + "&7, bossbars=" + yes(result.bossbarsValid()) + "&7, abilities=" + yes(result.abilitiesRegistered())
                + "&7, cooldowns=" + yes(result.cooldownsValid()) + "&7, minions=" + yes(result.minionsValid())
                + "&7, safe drops=" + yes(result.missingDropsSkipped()) + "&7, duplicate/reset/cleanup="
                + yes(result.duplicateSpawnBlocked() && result.resetValid() && result.minionsClear()) + "&7, YAML created=&afalse&7, added comments=&afalse&7."));
        if (!result.missingSpawns().isEmpty()) sender.sendMessage(CC.color("&cMissing boss spawns: " + String.join(", ", result.missingSpawns())));
    }

    private void sendBossHealthValidation(CommandSender sender, Game game) {
        var zone = game.getZoneManager().validateBossHealthScaling();
        var dungeon = game.getDungeonManager().validateBossHealthScaling();
        sender.sendMessage(CC.color("&7Boss health: normal Lv.15=&e" + (long) zone.normalLevel15Health()
                + "&7, Pharaoh Lv.15=&e" + (long) zone.pharaohLevel15Health()
                + "&7, Pharaoh target range=" + yes(zone.pharaohTargetRange())
                + "&7, Pharaoh above normal=" + yes(zone.pharaohHigherThanNormal()) + "&7."));
        sender.sendMessage(CC.color("&7Zone bosses >=5x=" + yes(zone.allBossesMeetMinimum())
                + "&7, dungeon bosses >=8x=" + yes(dungeon.allBossesMeetMinimum())
                + "&7, PDC/MMO health=" + yes(zone.pdcHealthSource() && dungeon.pdcHealthSource())
                + "&7, PDC nametag/bossbar=" + yes(dungeon.pdcNametagAndBossbar())
                + "&7, Bukkit safe cap=" + yes(zone.safeBukkitHealth() && dungeon.safeBukkitHealth())
                + "&7, YAML created=&afalse&7, added comments=&afalse&7."));
    }

    private boolean handleDev(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.devcontrols.admin") && !sender.hasPermission("smp.dev")) {
            sender.sendMessage(CC.color("&cYou do not have permission to use Developer Controls."));
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(CC.color("&cConsole usage: /smp dev status|reload|set|reset"));
                return true;
            }
            devControlMenu.openMain(player);
            return true;
        }

        if (args[1].equalsIgnoreCase("reload")) {
            liveControls.reload();
            sender.sendMessage(CC.color("&aReloaded and applied SMP live controls from persistent storage."));
            return true;
        }

        if (args[1].equalsIgnoreCase("status")) {
            sender.sendMessage(CC.color("&bSMP Live Controls:"));
            for (LiveControlDefinition definition : liveControls.definitions()) {
                String source = liveControls.hasOverride(definition.key()) ? "&eDATABASE OVERRIDE" : "&aDEFAULT";
                sender.sendMessage(CC.color("&7- &f" + definition.key() + "&7 = &f" + liveControls.value(definition.key()) + " &8(" + source + "&8)"));
            }
            return true;
        }

        if (args[1].equalsIgnoreCase("reset")) {
            if (args.length < 3) {
                sender.sendMessage(CC.color("&cUsage: /smp dev reset <control_key> [reason]"));
                return true;
            }
            String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : "Developer command";
            if (liveControls.resetOverride(args[2], sender.getName(), reason)) {
                sender.sendMessage(CC.color("&aReset &f" + args[2] + " &ato its Java default."));
            } else {
                sender.sendMessage(CC.color("&cUnknown live control key: &f" + args[2]));
            }
            return true;
        }

        if (args[1].equalsIgnoreCase("set")) {
            if (args.length < 4) {
                sender.sendMessage(CC.color("&cUsage: /smp dev set <control_key> <value> [reason]"));
                return true;
            }
            String reason = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "Developer command";
            if (liveControls.setOverride(args[2], args[3], sender.getName(), reason)) {
                sender.sendMessage(CC.color("&aSaved live control &f" + args[2] + "&a = &f" + args[3]));
            } else {
                sender.sendMessage(CC.color("&cUnknown key or invalid value. Use /smp dev status to view valid controls."));
            }
            return true;
        }

        sender.sendMessage(CC.color("&cUsage: /smp devcontrols or /smp dev [status|reload|set|reset]"));
        return true;
    }
}
