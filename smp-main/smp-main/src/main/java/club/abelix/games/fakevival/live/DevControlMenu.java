package club.abelix.games.fakevival.live;

import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DevControlMenu {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());
    private final LiveControlManager liveControls;
    private final JavaPlugin plugin;

    public DevControlMenu(JavaPlugin plugin, LiveControlManager liveControls) {
        this.plugin = plugin;
        this.liveControls = liveControls;
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("smp-dev:main"), 54, CC.color("&8SMP Developer Controls"));
        inventory.setItem(10, menuItem(Material.CHEST, "&aItems & Registries", "items", List.of("&7ItemBrowser, EditItem, registries, recipes, enchants.")));
        inventory.setItem(11, menuItem(Material.EMERALD, "&6Shops & Economy", "shops", List.of("&7Shops, Bazaar, Bank, coins, tokens, vendors.")));
        inventory.setItem(12, menuItem(Material.WRITABLE_BOOK, "&dQuests & Collections", "quests", List.of("&7Quests, collections, recipes, unlock testing.")));
        inventory.setItem(13, menuItem(Material.ZOMBIE_HEAD, "&cZones & Dungeons", "zones", List.of("&7Zones, dungeons, bosses, mobs, portals.")));
        inventory.setItem(14, menuItem(Material.EXPERIENCE_BOTTLE, "&bSkills & Jobs", "skills", List.of("&7Skills, jobs, player levels, MMO stat testing.")));
        inventory.setItem(15, menuItem(Material.CLOCK, "&eEvents", "events", List.of("&7Event calendar, event shop, rewards, debug.")));
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("furniture.admin-debug-enabled", false))
            inventory.setItem(16, menuItem(Material.ARMOR_STAND, "&9Furniture", "furniture", List.of("&7Furniture shop, registry, models, barriers, cleanup.")));
        inventory.setItem(19, menuItem(Material.PLAYER_HEAD, "&5NPCs", "npcs", List.of("&7NPC manager, shop NPCs, quest NPCs, validation.")));
        inventory.setItem(20, menuItem(Material.PLAYER_HEAD, "&2Player Testing", "player", List.of("&7Currency, profile, test mode, reset/debug controls.")));
        inventory.setItem(21, menuItem(Material.NETHER_STAR, "&3Menus", "menus", List.of("&7Quick shortcuts into player and admin test menus.")));
        inventory.setItem(22, menuItem(Material.COMMAND_BLOCK, "&4Debug Tools", "debug", List.of("&7Database, registry, entity, dungeon, zone debug tools.")));
        inventory.setItem(23, menuItem(Material.ENDER_CHEST, "&5Storage", "storage", List.of("&7Open, save, reload, unlock, and debug storage.")));
        inventory.setItem(49, item(Material.BARRIER, "&cClose", List.of("&eClick to close.", "&8SMP Dev Action: close")));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openToolMenu(Player player, String menu, int page) {
        List<DevButton> buttons = buttons(menu);
        int[] slots = controlSlots();
        int totalPages = Math.max(1, (int) Math.ceil(buttons.size() / (double) slots.length));
        page = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("smp-dev:tools:" + menu + ":" + page), 54, CC.color(title(menu)));
        int start = page * slots.length;
        for (int index = 0; index < slots.length && start + index < buttons.size(); index++) {
            DevButton button = buttons.get(start + index);
            inventory.setItem(slots[index], actionItem(button));
        }
        if (page > 0) inventory.setItem(45, item(Material.ARROW, "&ePrevious Page", List.of("&8SMP Dev Page: " + (page - 1))));
        inventory.setItem(49, item(Material.ARROW, "&aBack", List.of("&7Return to developer controls.", "&8SMP Dev Action: back")));
        if (page + 1 < totalPages) inventory.setItem(53, item(Material.ARROW, "&eNext Page", List.of("&8SMP Dev Page: " + (page + 1))));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openCategory(Player player, String category, int page) {
        List<LiveControlDefinition> definitions = liveControls.definitions().stream()
                .filter(definition -> definition.category().equalsIgnoreCase(category))
                .sorted(Comparator.comparing(LiveControlDefinition::key))
                .toList();
        int[] slots = controlSlots();
        int totalPages = Math.max(1, (int) Math.ceil(definitions.size() / (double) slots.length));
        page = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("smp-dev:category:" + category + ":" + page), 54, CC.color("&8SMP Dev: " + pretty(category)));
        int start = page * slots.length;
        for (int index = 0; index < slots.length && start + index < definitions.size(); index++) {
            inventory.setItem(slots[index], controlItem(definitions.get(start + index)));
        }
        inventory.setItem(45, item(Material.ARROW, "&ePrevious Page", page > 0 ? List.of("&8SMP Dev Page: " + (page - 1)) : List.of("&7No previous page.")));
        inventory.setItem(49, item(Material.ARROW, "&aBack", List.of("&7Return to developer controls.", "&8SMP Dev Action: back")));
        inventory.setItem(53, item(Material.ARROW, "&eNext Page", page + 1 < totalPages ? List.of("&8SMP Dev Page: " + (page + 1)) : List.of("&7No next page.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openStatus(Player player, int page) {
        List<LiveControlDefinition> definitions = liveControls.definitions().stream()
                .sorted(Comparator.comparing(LiveControlDefinition::category).thenComparing(LiveControlDefinition::key))
                .toList();
        int[] slots = controlSlots();
        int totalPages = Math.max(1, (int) Math.ceil(definitions.size() / (double) slots.length));
        page = Math.clamp(page, 0, totalPages - 1);
        Inventory inventory = Bukkit.createInventory(new MenuHolder("smp-dev:status:" + page), 54, CC.color("&8SMP Dev: Live Status"));
        int start = page * slots.length;
        for (int index = 0; index < slots.length && start + index < definitions.size(); index++) {
            inventory.setItem(slots[index], controlItem(definitions.get(start + index)));
        }
        if (page > 0) inventory.setItem(45, item(Material.ARROW, "&ePrevious Page", List.of("&8SMP Dev Page: " + (page - 1))));
        inventory.setItem(49, item(Material.ARROW, "&aBack", List.of("&7Return to developer controls.", "&8SMP Dev Action: back")));
        if (page + 1 < totalPages) inventory.setItem(53, item(Material.ARROW, "&eNext Page", List.of("&8SMP Dev Page: " + (page + 1))));
        fill(inventory);
        player.openInventory(inventory);
    }

    public void openConfirm(Player player, LiveControlDefinition definition, String newValue, String returnType) {
        Inventory inventory = Bukkit.createInventory(new MenuHolder("smp-dev:confirm:" + definition.key() + ":" + newValue + ":" + returnType), 27, CC.color("&8Confirm Major Change"));
        inventory.setItem(11, item(Material.LIME_CONCRETE, "&aConfirm", List.of("&7Control: &f" + definition.key(), "&7New Value: &f" + newValue, "", "&cThis affects a major SMP system.", "&8SMP Dev Action: confirm")));
        inventory.setItem(15, item(Material.RED_CONCRETE, "&cCancel", List.of("&7Return without saving.", "&8SMP Dev Action: cancel")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private ItemStack menuItem(Material material, String name, String menu, List<String> lore) {
        List<String> lines = new ArrayList<>(lore);
        lines.add("");
        lines.add("&eClick to open.");
        lines.add("&8SMP Dev Menu: " + menu);
        return item(material, name, lines);
    }

    private ItemStack actionItem(DevButton button) {
        List<String> lines = new ArrayList<>(button.lore());
        lines.add("");
        lines.add(button.dangerous() ? "&cClick to run test action." : "&eClick to open/run.");
        lines.add("&8SMP Dev Action: " + button.action());
        return item(button.material(), button.name(), lines);
    }

    private List<DevButton> buttons(String menu) {
        List<DevButton> result = switch (menu.toLowerCase(Locale.ROOT)) {
            case "items" -> List.of(
                    b(Material.CHEST, "&aItemBrowser", "cmd:itembrowser", "&7Open the existing custom item browser."),
                    b(Material.REDSTONE, "&eRebuild Item Browser Cache", "cmd:smp items rebuildcache", "&7Refresh cached category item ID lists."),
                    b(Material.COMPARATOR, "&aValidate Item Browser Categories", "cmd:smp items validate", "&7Check IDs, categories, PDC, and pagination."),
                    b(Material.SPYGLASS, "&bDebug Held Item Custom ID", "cmd:smp items debugheld", "&7Read the held item's PDC custom ID."),
                    b(Material.ANVIL, "&aEditItem", "cmd:edititem", "&7Open the existing held item editor."),
                    b(Material.ENCHANTED_BOOK, "&dEnchantments", "cmd:enchants", "&7Open custom enchantment listings."),
                    b(Material.ANVIL, "&aOpen Anvil Enchant Menu", "cmd:smp anvil open", "&7Open the protected custom enchant anvil."),
                    b(Material.COMPARATOR, "&eValidate Enchants", "cmd:smp enchants validate", "&7Validate enchant IDs, types, and levels."),
                    b(Material.IRON_BLOCK, "&eValidate Anvil Menu", "cmd:smp anvil validate", "&7Validate input return and protected slots."),
                    b(Material.SPYGLASS, "&bDebug Held Item Enchants", "cmd:edititem debug", "&7Inspect the held item's PDC and enchants."),
                    b(Material.DIAMOND_SWORD, "&6Give Test Enchantable Item", "cmd:itembrowser", "&7Give an enchantable item through ItemBrowser."),
                    b(Material.BOOKSHELF, "&bCustom Item Registry", "cmd:itembrowser", "&7Browse configured static and database items."),
                    b(Material.KNOWLEDGE_BOOK, "&eRecipe Registry", "cmd:recipes", "&7Open the existing recipe registry/menu."),
                    b(Material.WRITABLE_BOOK, "&dCollection Registry", "cmd:collections", "&7Open the existing collection menu."),
                    b(Material.NETHER_STAR, "&6Rarity Test", "msg:&eUse /edititem rarity <rarity> on a held test item.", "&7Uses existing EditItem rarity handling."),
                    b(Material.SLIME_BALL, "&bRandom Stat Roll Test", "msg:&eUse ItemBrowser loot/custom item give tools to test existing rolls.", "&7No new random stat logic is added here."),
                    b(Material.DROPPER, "&aGive Custom Item", "cmd:itembrowser", "&7Give items through ItemBrowser only."),
                    b(Material.COMPASS, "&eReload Java Registries", "cmd:smp reload", "&7Runs the existing SMP reload path."),
                    b(Material.STRUCTURE_VOID, "&cValidate Registry Data", "msg:&eRegistry validation uses existing startup/console warnings. Use /smp reload to re-validate.", "&7Safe validation shortcut."),
                    b(Material.COMPARATOR, "&aValidate Ability Registry", "abilityvalidate", "&7Validate metadata for all registered abilities."),
                    b(Material.BLAZE_ROD, "&eTest Held Item Ability", "abilitytestheld", "&7Runs the held custom item's configured trigger."),
                    b(Material.MILK_BUCKET, "&bClear Ability Cooldowns", "abilityclearcooldowns", "&7Clear your in-memory ability cooldowns."),
                    b(Material.CHEST, "&6Give Ability Test Items", "abilitygivetest", "&7Give one existing PDC custom item per weapon family."),
                    b(Material.SPYGLASS, "&dDebug Last Ability Cast", "abilitydebuglast", "&7Show the last successful ability activation.")
            );
            case "shops" -> List.of(
                    shop("&eOpen General Merchant", "general", Material.CHEST), shop("&8Open Blacksmith", "blacksmith", Material.ANVIL), shop("&dOpen Alchemist", "alchemist", Material.BREWING_STAND), shop("&aOpen Farmer/Food Shop", "farmer", Material.HAY_BLOCK), shop("&3Open Fisherman", "fisherman", Material.FISHING_ROD), shop("&bOpen Miner", "miner", Material.IRON_PICKAXE), shop("&2Open Lumberjack", "lumberjack", Material.IRON_AXE), shop("&5Open Dungeon Vendor", "dungeon_vendor", Material.ENDER_CHEST), b(Material.SUNFLOWER, "&dOpen Event Vendor", "eventshop", "&7Open existing EventManager event shop."), b(Material.ARMOR_STAND, "&6Open Furniture Vendor", "furnitureshop", "&7Open existing furniture shop."), b(Material.EMERALD, "&aOpen Bazaar", "bazaar", "&7Open the improved Bazaar menu."), b(Material.STRUCTURE_VOID, "&cValidate Bazaar Items", "bazaarvalidate", "&7Validate custom IDs, types, PDC, and materials."), b(Material.COMPASS, "&eReload Bazaar Cache", "bazaarreload", "&7Reload cached market rows asynchronously."), b(Material.COMPARATOR, "&6Recalculate Bazaar Prices", "bazaarrecalculate", "&7Start one asynchronous recalculation."), b(Material.SPYGLASS, "&eDebug Bazaar Item Held", "bazaardebugheld", "&7Show the held item's custom ID and market row."), b(Material.PAPER, "&eDebug Bazaar Market Rows", "bazaardebugrows", "&7Show cached row and validation counts."), b(Material.CHEST, "&aGive Bazaar Test Materials", "bazaargivetest", "&7Give samples generated by CustomItemManager."), b(Material.HOPPER, "&cClear Bazaar Cache", "bazaarclear", "&7Clear only in-memory Bazaar market rows."), b(Material.GOLD_INGOT, "&6Open Bank", "bank", "&7Open existing Bank menu."), b(Material.SUNFLOWER, "&eCoins Generator", "cmd:coins give %player% 1000", "&7Adds 1,000 test coins to yourself."), b(Material.AMETHYST_SHARD, "&dEvent Token Generator", "msg:&eUse existing event reward/token commands if configured.", "&7Does not create a new token system."), b(Material.PRISMARINE_SHARD, "&5Dungeon Token/Fragment Generator", "cmd:itembrowser", "&7Use ItemBrowser to give existing dungeon fragments."), b(Material.COMPASS, "&eReload Shops", "cmd:smp reload", "&7Reloads static/runtime registry data."), b(Material.STRUCTURE_VOID, "&cValidate Shop Items", "msg:&eOpen each shop after /smp reload to validate item PDC/lore.", "&7No fake preview items are generated.")
            );
            case "quests" -> List.of(
                    b(Material.KNOWLEDGE_BOOK,"&aOpen Recipe Book","cmd:recipes","&7Open the Java-backed recipe book."),b(Material.COMPARATOR,"&eValidate Recipes","cmd:recipes validate","&7Validate outputs and ingredients."),b(Material.GRASS_BLOCK,"&aValidate Vanilla Recipe Paths","cmd:recipes validate","&7Rebuild and validate vanilla links."),b(Material.LIME_DYE,"&aUnlock Recipe For Self","msg:&eUse the linked collection/skill controls to unlock a recipe.","&7Keeps progression authoritative."),b(Material.GRAY_DYE,"&cLock Recipe For Self","msg:&eReset the linked collection/skill requirement to lock it.","&7Does not duplicate unlock state."),b(Material.CHEST,"&6Give Recipe Ingredients","msg:&eUse ItemBrowser with the recipe ingredient IDs shown in preview.","&7Gives exact PDC items."),b(Material.SPYGLASS,"&bDebug Held Item Recipe Links","cmd:recipes","&7Browse the matching vanilla or custom path."),b(Material.REDSTONE,"&eRebuild Recipe Cache","cmd:recipes validate","&7Rebuild cached vanilla relationships."),b(Material.WRITABLE_BOOK, "&dOpen Quest Menu", "cmd:quests", "&7Open existing quest menu."), b(Material.EMERALD,"&aOpen Adventurer's Guild","cmd:adventureguild","&7Open the ranked guild menu."),b(Material.NAME_TAG,"&eSet Adventure Rank","msg:&eUse the Adventure Guild rank admin/profile tools.","&7Targets a selected player."),b(Material.BARRIER,"&cReset Adventure Rank","msg:&eReset requires an explicit target in the rank admin tools.","&7Preserves quest data."),b(Material.EXPERIENCE_BOTTLE,"&aAdd Completed Adventurer Quests","msg:&eUse the Adventure Guild profile admin tools.","&7Updates the persistent profile."),b(Material.COMPARATOR,"&eValidate Adventure Requirements","cmd:quests rankup","&7Checks your next-rank requirements."),b(Material.CHEST,"&6Give Test Adventure Quest","cmd:quests daily","&7Open an eligible test contract."),b(Material.SPYGLASS,"&bDebug Adventure Profile","cmd:quests","&7Shows rank and progression in the guild menu."), b(Material.CLOCK, "&aOpen Daily Quests", "quests:daily", "&7Open daily quest page."), b(Material.CLOCK, "&bOpen Weekly Quests", "quests:weekly", "&7Open weekly quest page."), b(Material.PLAYER_HEAD, "&eOpen NPC Quests", "msg:&eUse /npc or configured quest NPCs to test NPC quests.", "&7Only shown when existing systems are configured."), b(Material.LIME_CONCRETE, "&aForce Complete Selected Quest", "msg:&eUse existing QuestManager/admin commands for selected quest completion.", "&7No new quest mutation logic added."), b(Material.RED_CONCRETE, "&cReset Selected Quest", "msg:&eUse existing quest reset/debug command if configured.", "&7Safe placeholder until a selected quest exists."), b(Material.EXPERIENCE_BOTTLE, "&aAdd Quest Progress", "msg:&eProgress changes use existing quest triggers/managers.", "&7Affects yourself unless existing target flow is used."), b(Material.BOOK, "&dOpen Collections Menu", "cmd:collections", "&7Open collections menu."), b(Material.STRUCTURE_VOID, "&cValidate Quests", "cmd:smp reload", "&7Reload quest data.")
            );
            case "zones" -> List.of(
                    b(Material.SPYGLASS, "&dDungeon Spectate", "cmd:dungeon spectate", "&7Open the active dungeon spectate menu."),
                    b(Material.CLOCK, "&eActive Dungeon Runs", "cmd:dungeon runs", "&7List active and rejoin-waiting runs."),
                    b(Material.RECOVERY_COMPASS, "&aRejoinable Runs", "cmd:dungeon runs", "&7Inspect runs still inside their rejoin timeout."),
                    b(Material.BARRIER, "&cForce Close Dungeon Run", "msg:&eUse /dungeon forceclose <runId>.", "&7Expires one selected temporary run."),
                    b(Material.WITHER_SKELETON_SKULL, "&6Boss Types", "cmd:dungeon bosstype", "&7List registered dungeon boss types."),
                    b(Material.ZOMBIE_HEAD, "&6Mob Types", "cmd:dungeon mobtype", "&7List registered dungeon mob types."),
                    b(Material.COMPASS, "&eSet Boss Location Help", "msg:&e/setbosslocation <dungeon> <mobId> <level>", "&7Save a persistent boss spawn."),
                    b(Material.STRUCTURE_VOID, "&cValidate Dungeon Spawns", "dungeonvalidate", "&7Validate persistent dungeon spawn metadata."),
                    b(Material.ENDER_EYE, "&5Open Dungeons Menu", "dungeonsmenu", "&7Open the player-facing Dungeons menu."),
                    b(Material.LIME_DYE, "&aStart Test Dungeon", "cmd:dungeon", "&7Open dungeon menu/setup commands for a test entry."),
                    b(Material.GOLD_BLOCK, "&6Complete Current Dungeon", "msg:&eKill the active dungeon boss to complete through the normal reward/reset flow.", "&7Uses the existing boss defeat flow."),
                    b(Material.REDSTONE_BLOCK, "&cReset Dungeon", "msg:&eDungeon reset is automatic 10 seconds after boss completion. Use existing dungeon admin tools for manual arena edits.", "&7Avoids deleting active player runs by accident."),
                    b(Material.WITHER_SKELETON_SKULL, "&4Spawn Dungeon Boss", "cmd:dungeon", "&7Use existing dungeon setup/spawn commands."),
                    b(Material.ZOMBIE_SPAWN_EGG, "&cSpawn Dungeon Mobs", "cmd:dungeon", "&7Use existing dungeon setup/spawn commands."),
                    b(Material.BARRIER, "&cClear Dungeon Mobs", "msg:&eDungeon mobs are cleared by completion/reset. Use a targeted admin command for manual clears if needed.", "&7No accidental mass entity deletion."),
                    b(Material.TRIPWIRE_HOOK, "&dGive Dungeon Key", "dungeonkey", "&7Give yourself an existing dungeon key custom item."),
                    b(Material.PAPER, "&eShow Dungeon Run State", "dungeonstate", "&7Print current dungeon state near you."),
                    b(Material.STRUCTURE_VOID, "&cValidate Dungeon Registry", "dungeonvalidate", "&7Validate Java/default dungeon registry entries."),
                    b(Material.KNOWLEDGE_BOOK, "&eValidate Dungeon Requirements", "dungeonrequirements", "&7Validate your unlock/key requirements."),
                    b(Material.COMPASS, "&aOpen Zone List", "zonesmenu", "&7Open discovered zone/warp menus."), b(Material.ENDER_PEARL, "&bTeleport to Zone", "warps", "&7Open zone warp controls."), b(Material.LIME_DYE, "&aDiscover Zone", "msg:&eUse /smp zones discover " + "%player% <zone>", "&7Uses existing ZoneManager."), b(Material.GRAY_DYE, "&cUndiscover Zone", "msg:&eUse /smp zones undiscover " + "%player% <zone>", "&7Uses existing ZoneManager."), b(Material.LODESTONE, "&eSet Zone Warp", "msg:&eUse existing /zone warp commands if supported.", "&7No new warp storage."), b(Material.ZOMBIE_SPAWN_EGG, "&cSpawn Dungeon Mob", "cmd:dungeon", "&7Open/show existing dungeon admin commands."), b(Material.WITHER_SKELETON_SKULL, "&4Spawn Zone Boss", "msg:&eUse existing ZoneManager boss spawn/debug commands if configured.", "&7Does not spawn huge amounts."), b(Material.NETHER_STAR, "&cRespawn Bosses", "msg:&eUse existing boss respawn tools if configured.", "&7Existing caps/batching stay in control."), b(Material.BARRIER, "&cClear Nearby Dungeon Mobs", "msg:&eUse existing dungeon clear/debug command if configured.", "&7No new mass clear task added."), b(Material.END_PORTAL_FRAME, "&5Open Dungeon Portal Controls", "cmd:dungeon", "&7Open/show dungeon portal commands."), shop("&5Open Dungeon Shop/Vendor", "dungeon_vendor", Material.ENDER_CHEST), b(Material.STRUCTURE_VOID, "&cValidate Zone Spawns", "cmd:smp reload", "&7Reload existing zone spawn data."), b(Material.SPYGLASS, "&eDebug Nearby Mobs", "entitycount", "&7Counts nearby living entities safely."), b(Material.REDSTONE, "&cDebug Boss Health", "msg:&eUse existing boss debug outputs if configured.", "&7No extra mob scanning loop added.")
            );
            case "skills" -> List.of(
                    b(Material.EXPERIENCE_BOTTLE, "&bOpen Skills Menu", "cmd:skills", "&7Open existing skills menu."), b(Material.DIAMOND_PICKAXE, "&bModify Skill Level", "msg:&eUse existing SkillManager/admin command if configured.", "&7Supported: Mining, Farming, Foraging, Combat, Fishing, Unarmed, Excavation, Archery, Taming, Acrobatics, Alchemy, Repair, Smelting, Enchanting."), b(Material.LAPIS_LAZULI, "&bModify Skill XP", "msg:&eUse existing skill XP tooling/triggers.", "&7No new stat storage added."), b(Material.CRAFTING_TABLE, "&aOpen Jobs Menu", "cmd:jobs", "&7Open existing jobs menu."), b(Material.EMERALD_BLOCK, "&aModify Job Level", "msg:&eUse existing JobManager/admin command if configured.", "&7Target defaults to yourself."), b(Material.EMERALD, "&aModify Job XP", "msg:&eUse existing job XP tooling/triggers.", "&7Target defaults to yourself."), b(Material.NETHER_STAR, "&dModify Player Level", "cmd:playerlevel", "&7Open existing player level menu."), b(Material.IRON_SWORD, "&cModify MMO Stats", "cmd:stats", "&7Open existing stats/debug view."), b(Material.CLOCK, "&aRefresh MMO Stats", "refreshhud", "&7Refresh scoreboard/tablist/actionbar safely."), b(Material.REDSTONE_TORCH, "&eDebug Actionbar Stats", "refreshhud", "&7Forces lightweight HUD refresh."), b(Material.TNT, "&cReset Admin Test Stats", "msg:&eUse /wipestats %player% only when you intentionally want a full reset.", "&7No accidental wipe from a menu click.")
            );
            case "events" -> List.of(
                    b(Material.CLOCK, "&eOpen Event Calendar", "eventcalendar", "&7Open existing event calendar."), b(Material.LIME_CONCRETE, "&aStart Event", "cmd:smp event start mining_fiesta", "&7Starts a safe existing event id."), b(Material.RED_CONCRETE, "&cStop Current Event", "cmd:smp event stop", "&7Stops the active event."), b(Material.ARROW, "&eForce Next Event", "cmd:smp event next", "&7Rerolls next event using EventManager."), b(Material.EXPERIENCE_BOTTLE, "&aAdd Event Progress", "msg:&eUse existing event progress triggers/tools.", "&7No new progress store."), b(Material.SUNFLOWER, "&eAdd Event Tokens", "msg:&eUse existing event token reward command/item if configured.", "&7No new token type created."), b(Material.SUNFLOWER, "&dOpen Event Shop", "eventshop", "&7Open existing event shop."), b(Material.CHEST, "&aTest Event Rewards", "msg:&eUse current EventManager reward test/admin tools if configured.", "&7Safe test shortcut."), b(Material.REDSTONE, "&eDebug Current Event", "cmd:smp event status", "&7Prints current EventManager status."), b(Material.STRUCTURE_VOID, "&cValidate Event Registry", "cmd:smp reload", "&7Reloads/validates event registry.")
            );
            case "furniture" -> List.of(
                    b(Material.ARMOR_STAND, "&6Open Furniture Shop", "furnitureshop", "&7Open existing furniture shop."), b(Material.BOOK, "&6Open Furniture Registry List", "cmd:smp furniture list", "&7List existing furniture definitions."), b(Material.ITEM_FRAME, "&aGive Furniture Item", "msg:&eUse /smp furniture give <id> [player] [amount]", "&7Uses existing FurnitureRegistry."), b(Material.OAK_STAIRS, "&aTest Place Furniture", "msg:&eUse /smp furniture testplace <id>", "&7Uses existing placement service."), b(Material.ANVIL, "&eOpen Furniture Editor", "cmd:smp furniture editor", "&7Open existing furniture editor."), b(Material.SPYGLASS, "&eDebug Nearby Furniture", "cmd:smp furniture debugnear", "&7Debug nearby placed furniture."), b(Material.BARRIER, "&eDebug Furniture Barriers", "cmd:smp furniture debugbarriers", "&7Debug barrier collision blocks."), b(Material.TNT, "&cCleanup Duplicate Furniture", "msg:&eUse existing cleanup command if configured.", "&7No automatic destructive cleanup from click."), b(Material.STRUCTURE_VOID, "&cValidate Furniture Models", "cmd:smp furniture list", "&7Loads/list definitions for model validation."), b(Material.STRUCTURE_VOID, "&cValidate Collision Blocks", "cmd:smp furniture debugbarriers", "&7Uses existing barrier debug."), b(Material.STRUCTURE_VOID, "&cValidate Sit Points", "cmd:smp furniture list", "&7Check existing sit/bed definitions."), b(Material.BARRIER, "&cRemove Nearby Test Furniture", "msg:&eUse existing furniture delete/removal command on selected test furniture.", "&7Prevents accidental mass deletion.")
            );
            case "storage" -> List.of(
                    b(Material.ENDER_CHEST, "&aOpen Storage", "cmd:enderchest", "&7Open your personal storage menu."),
                    b(Material.LIME_DYE, "&aGive Storage Page Unlock", "cmd:storage admin give", "&7Unlock the next page for yourself."),
                    b(Material.RED_DYE, "&cReset Storage Pages", "cmd:storage admin reset", "&7Reset your unlock count to page one."),
                    b(Material.COMPARATOR, "&eDebug Storage Data", "cmd:storage admin debug", "&7Show cached unlock and session state."),
                    b(Material.CHEST, "&aSave Storage Now", "cmd:storage admin save", "&7Queue a save for your open storage page."),
                    b(Material.REPEATER, "&bReload Storage From DB", "cmd:storage admin reload", "&7Reload your storage unlock cache."),
                    b(Material.GOLD_BLOCK, "&6Open Gold Store Account Upgrades", "cmd:storage admin store", "&7Open account upgrades in the Gold Store."));
            case "npcs" -> List.of(
                    b(Material.PLAYER_HEAD, "&5Open NPC Manager", "cmd:npc", "&7Open/show existing NPC manager commands."), b(Material.EMERALD, "&aCreate Shop NPC", "msg:&eUse /npc create ... then set action to shop:<shopId>.", "&7Uses existing NPC framework."), b(Material.WRITABLE_BOOK, "&dCreate Quest NPC", "msg:&eUse existing /npc quest/action commands.", "&7No new NPC framework."), b(Material.ZOMBIE_HEAD, "&cCreate Dungeon NPC", "msg:&eUse existing /npc action command with dungeon/menu action.", "&7No new NPC framework."), b(Material.SPYGLASS, "&eDebug Nearby NPCs", "cmd:npc list", "&7Uses existing NPC list/debug output."), b(Material.COMPASS, "&eReload NPCs", "cmd:smp reload", "&7Reload runtime/static data."), b(Material.STRUCTURE_VOID, "&cValidate NPC Actions", "cmd:npc list", "&7Inspect existing configured NPC actions.")
            );
            case "player" -> List.of(
                    b(Material.SUNFLOWER, "&eAdd Coins", "cmd:coins give %player% 1000", "&7Adds 1,000 coins to yourself."), b(Material.REDSTONE, "&cRemove Coins", "cmd:coins take %player% 1000", "&7Removes 1,000 coins from yourself."), b(Material.GOLD_INGOT, "&6Add Gold", "msg:&eGold belongs to Abelix hook/core. Use existing Abelix test command if available.", "&7No fake gold store added."), b(Material.AMETHYST_SHARD, "&dAdd Event Tokens", "msg:&eUse existing event token item/reward command if configured.", "&7Target defaults to yourself."), b(Material.PRISMARINE_SHARD, "&5Add Dungeon Tokens", "cmd:itembrowser", "&7Give existing dungeon fragments through ItemBrowser."), b(Material.ENDER_CHEST, "&6Modify Bank Balance", "bank", "&7Open bank menu for safe deposit/withdraw tools."), b(Material.PAPER, "&eModify Purse", "cmd:coins", "&7View current purse/coin balance."), b(Material.NETHER_STAR, "&dModify Player Level", "cmd:playerlevel", "&7Open existing player level menu."), b(Material.EXPERIENCE_BOTTLE, "&bModify Skill Levels", "cmd:skills", "&7Open existing skills menu."), b(Material.WRITABLE_BOOK, "&dModify Quest Progress", "cmd:quests", "&7Open existing quest menu."), b(Material.COMPASS, "&aToggle Zone Discovery", "msg:&eUse /smp zones discover|undiscover %player% <zone>", "&7Uses existing ZoneManager."), b(Material.BRICKS, "&2Toggle BuildMode", "cmd:buildmode", "&7Uses existing buildmode command."), b(Material.TOTEM_OF_UNDYING, "&eToggle God/Test Mode", "cmd:sandbox", "&7Open existing admin sandbox/test tools."), b(Material.CLOCK, "&aRefresh Scoreboard/Tablist/Actionbar", "refreshhud", "&7Restarts board/HUD updates safely.")
            );
            case "menus" -> List.of(
                    b(Material.BOOK,"&aOpen Collections Book","cmd:collections","&7Open the vanilla-path Collections Book."),b(Material.COMPARATOR,"&eValidate Collections","cmd:collections validate","&7Validate collection definitions and rewards."),b(Material.GRASS_BLOCK,"&aValidate Vanilla Collection Paths","cmd:collections validate","&7Rebuild and validate cached links."),b(Material.EXPERIENCE_BOTTLE,"&aAdd Collection Progress","msg:&eGain progress through the normal protected activity or collection admin flow.","&7Uses real collection sources."),b(Material.BARRIER,"&cReset Collection Progress","msg:&eUse an explicit collection admin reset with a target.","&7Avoids accidental bulk resets."),b(Material.LIME_DYE,"&aUnlock Collection Tier","msg:&eAdd the required collection progress for the selected tier.","&7Keeps tier thresholds authoritative."),b(Material.SPYGLASS,"&bDebug Held Item Collection Links","cmd:collections","&7Browse the held material's vanilla path."),b(Material.REDSTONE,"&eRebuild Collection Cache","cmd:collections validate","&7Rebuild cached Material links."),b(Material.NETHER_STAR, "&aSurvival Menu", "cmd:menu", "&7Open main survival menu."), b(Material.EXPERIENCE_BOTTLE, "&bSkills Menu", "cmd:skills", "&7Open skills."), b(Material.CRAFTING_TABLE, "&aJobs Menu", "cmd:jobs", "&7Open jobs."), b(Material.KNOWLEDGE_BOOK, "&eRecipes Menu", "cmd:recipes", "&7Open recipes."), b(Material.EMERALD, "&aBazaar", "bazaar", "&7Open bazaar."), b(Material.GOLD_INGOT, "&6Bank", "bank", "&7Open bank."), b(Material.CLOCK, "&eEvent Calendar", "eventcalendar", "&7Open event calendar."), b(Material.SUNFLOWER, "&dEvent Shop", "eventshop", "&7Open event shop."), b(Material.ARMOR_STAND, "&6Furniture Shop", "furnitureshop", "&7Open furniture shop."), b(Material.WRITABLE_BOOK, "&dQuests Menu", "cmd:quests", "&7Open quests."), b(Material.ENDER_EYE, "&5Dungeons Menu", "dungeonsmenu", "&7Open Dungeons."), b(Material.COMPASS, "&bZone Warps", "warps", "&7Open warps."), b(Material.CHEST, "&aItemBrowser", "cmd:itembrowser", "&7Open ItemBrowser."), b(Material.ANVIL, "&aEditItem", "cmd:edititem", "&7Open EditItem.")
            );
            case "debug" -> List.of(
                    b(Material.REDSTONE_TORCH, "&aDatabase Status", "msg:&eUse /smp dev status and console startup logs for DB-backed systems.", "&7Does not run blocking SQL."), b(Material.WATER_BUCKET, "&bHikari Pool Status", "msg:&eHikari status is logged by existing database manager when available.", "&7No main-thread SQL query."), b(Material.EMERALD, "&aBazaar Debug", "cmd:smp bazaarprice diamond", "&7Shows sample bazaar market status safely."), b(Material.ARMOR_STAND, "&6Furniture Debug Near", "cmd:smp furniture debugnear", "&7Uses existing furniture debug."), b(Material.SPYGLASS, "&eEntity Count Near", "entitycount", "&7Counts nearby entities once."), b(Material.ZOMBIE_HEAD, "&cDungeon Mob Count Near", "entitycount", "&7Counts nearby living entities once."), b(Material.COMPASS, "&bZone Debug", "zonesmenu", "&7Open zone debug/menu info."), b(Material.STRUCTURE_VOID, "&cRegistry Validation", "cmd:smp reload", "&7Runs existing reload validation."), b(Material.CHEST, "&3Menu Debug", "msg:&eDeveloper Controls menu click cancellation and holder checks are active.", "&7Safe debug note."), b(Material.HOPPER, "&eClear Admin Cache", "refreshhud", "&7Refresh HUD/menus without heavy data scans."), b(Material.COMPARATOR, "&eReload Runtime Settings", "cmd:smp reload", "&7Runs existing reload path."), b(Material.PAPER, "&9Admin Command Webhook Test", "msg:&eUse existing admin command logger/webhook test command if configured.", "&7No new webhook call added."), b(Material.KNOWLEDGE_BOOK, "&fLive Control Status", "status", "&7View all database-backed live controls."), b(Material.COMPASS, "&aReload Live Controls", "reload", "&7Reloads existing live controls."),
                    b(Material.BELL, "&eToggle SMP Alerts", "cmd:alerts", "&7Toggle your in-game economy alerts."),
                    b(Material.COMPARATOR, "&eView Alert Thresholds", "cmd:smp alerts thresholds", "&7Show runtime high-value thresholds."),
                    b(Material.GOLD_BLOCK, "&6Test Auction Alert", "cmd:smp alerts test AUCTION_BID", "&7Sends a test without mutating the economy."),
                    b(Material.EMERALD_BLOCK, "&aTest Trade Alert", "cmd:smp alerts test TRADE_COINS", "&7Sends a test without mutating the economy."),
                    b(Material.IRON_BLOCK, "&fTest Bank Alert", "cmd:smp alerts test BANK_WITHDRAW", "&7Sends a test without mutating the economy."),
                    b(Material.CHEST, "&eTest Bazaar Alert", "cmd:smp alerts test BAZAAR_SELL", "&7Sends a test without mutating the economy."),
                    b(Material.SPYGLASS, "&cOpen Macrocheck Tool", "msg:&eUse /macrocheck <player> to begin a non-punitive review.", "&7Never auto-punishes."),
                    b(Material.BOOK, "&6View Recent Macrocheck Logs", "cmd:macrocheck history", "&7Loads recent records asynchronously.")
            );
            default -> List.of(b(Material.BARRIER, "&cUnknown Menu", "back", "&7Return to Developer Controls."));
        };
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("furniture.admin-debug-enabled", false))
            return result.stream().filter(button -> !button.action().toLowerCase(Locale.ROOT).contains("furniture") && !button.name().toLowerCase(Locale.ROOT).contains("furniture")).toList();
        return result;
    }

    private DevButton shop(String name, String id, Material material) {
        return b(material, name, "shop:" + id, "&7Opens existing shop id &f" + id + "&7.");
    }

    private DevButton b(Material material, String name, String action, String lore) {
        return new DevButton(material, name, action, List.of(lore), action.startsWith("cmd:wipestats") || action.startsWith("cmd:smp event stop"));
    }

    private String title(String menu) {
        return switch (menu.toLowerCase(Locale.ROOT)) {
            case "items" -> "&8Dev Controls - Items";
            case "shops" -> "&8Dev Controls - Shops";
            case "quests" -> "&8Dev Controls - Quests";
            case "zones" -> "&8Dev Controls - Zones";
            case "skills" -> "&8Dev Controls - Skills";
            case "events" -> "&8Dev Controls - Events";
            case "storage" -> "&8Dev Controls - Storage";
            case "furniture" -> "&8Dev Controls - Furniture";
            case "npcs" -> "&8Dev Controls - NPCs";
            case "player" -> "&8Dev Controls - Player Testing";
            case "menus" -> "&8Dev Controls - Menus";
            case "debug" -> "&8Dev Controls - Debug";
            default -> "&8SMP Developer Controls";
        };
    }

    private ItemStack controlItem(LiveControlDefinition definition) {
        boolean override = liveControls.hasOverride(definition.key());
        LiveControlValue value = liveControls.override(definition.key());
        List<String> lore = new ArrayList<>();
        lore.add("&7Key: &f" + definition.key());
        lore.add("&7Value: &f" + liveControls.value(definition.key()));
        lore.add("&7Source: " + (override ? "&eDATABASE OVERRIDE" : "&aDEFAULT"));
        lore.add("&7Type: &f" + definition.type());
        if (override && value != null) {
            lore.add("&7Reason: &f" + value.reason());
            lore.add("&7Updated By: &f" + value.updatedBy());
            lore.add("&7Updated At: &f" + TIME_FORMAT.format(Instant.ofEpochMilli(value.updatedAt())));
        }
        lore.add("");
        if (definition.type() == LiveControlType.BOOLEAN) lore.add("&eLeft Click &7to toggle.");
        else if (definition.type() == LiveControlType.DOUBLE) lore.add("&eLeft Click &7to cycle multiplier.");
        else lore.add("&7Use &f/smp dev set " + definition.key() + " <value>");
        lore.add("&cRight Click &7to reset to default.");
        lore.add("&8SMP Dev Control: " + definition.key());
        Material material = switch (definition.type()) {
            case BOOLEAN -> Boolean.parseBoolean(liveControls.value(definition.key())) ? Material.REDSTONE_BLOCK : Material.EMERALD_BLOCK;
            case DOUBLE -> Material.EXPERIENCE_BOTTLE;
            case STRING -> Material.PAPER;
        };
        return item(material, (override ? "&e" : "&a") + definition.label(), lore);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material == null ? Material.PAPER : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            List<String> colored = new ArrayList<>();
            for (String line : lore) colored.add(CC.color(line));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) if (inventory.getItem(i) == null) inventory.setItem(i, filler);
    }

    private int[] controlSlots() {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    }

    private String pretty(String text) {
        text = text == null ? "" : text.toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
        return text.isBlank() ? "Controls" : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private record DevButton(Material material, String name, String action, List<String> lore, boolean dangerous) {}
}
