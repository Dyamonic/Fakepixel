package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.live.DevControlMenu;
import club.abelix.games.fakevival.live.LiveControlDefinition;
import club.abelix.games.fakevival.live.LiveControlManager;
import club.abelix.games.fakevival.live.LiveControlType;
import club.abelix.games.fakevival.menus.BankMenu;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class DevControlListener implements Listener {
    private static final String PERMISSION = "fakevival.devcontrols.admin";
    private final JavaPlugin plugin;
    private final LiveControlManager liveControls;
    private final DevControlMenu menu;

    public DevControlListener(JavaPlugin plugin, LiveControlManager liveControls, DevControlMenu menu) {
        this.plugin = plugin;
        this.liveControls = liveControls;
        this.menu = menu;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder(String type) && type.startsWith("smp-dev")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.startsWith("smp-dev")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !canUse(player)) return;
        if (event.getClickedInventory() == null || event.getRawSlot() >= event.getInventory().getSize()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (type.equals("smp-dev:main")) {
            handleMain(player, clicked);
            return;
        }
        if (type.startsWith("smp-dev:tools:")) {
            handleToolMenu(player, type, clicked);
            return;
        }
        if (type.startsWith("smp-dev:category:")) {
            handleCategory(player, type, clicked, event.isRightClick());
            return;
        }
        if (type.startsWith("smp-dev:status:")) {
            handleStatus(player, type, clicked, event.isRightClick());
            return;
        }
        if (type.startsWith("smp-dev:confirm:")) {
            handleConfirm(player, type, clicked);
        }
    }

    private boolean canUse(Player player) {
        return player.hasPermission(PERMISSION) || player.hasPermission("smp.dev");
    }

    private void handleMain(Player player, ItemStack clicked) {
        String action = loreValue(clicked, "SMP Dev Action: ");
        if ("close".equalsIgnoreCase(action)) {
            player.closeInventory();
            return;
        }
        String toolMenu = loreValue(clicked, "SMP Dev Menu: ");
        if (toolMenu != null) menu.openToolMenu(player, toolMenu, 0);
    }

    private void handleToolMenu(Player player, String type, ItemStack clicked) {
        String action = loreValue(clicked, "SMP Dev Action: ");
        if ("back".equalsIgnoreCase(action)) {
            menu.openMain(player);
            return;
        }
        Integer page = pageLore(clicked);
        if (page != null) {
            String[] parts = type.split(":");
            menu.openToolMenu(player, parts.length >= 3 ? parts[2] : "debug", page);
            return;
        }
        if (action == null) return;
        runAction(player, action);
    }

    private void runAction(Player player, String action) {
        if (action.equalsIgnoreCase("close")) {
            player.closeInventory();
            return;
        }
        if (action.equalsIgnoreCase("back")) {
            menu.openMain(player);
            return;
        }
        if (action.equalsIgnoreCase("reload")) {
            liveControls.reload();
            player.sendMessage(CC.color("&aReloaded and applied SMP live controls."));
            menu.openToolMenu(player, "debug", 0);
            return;
        }
        if (action.equalsIgnoreCase("status")) {
            menu.openStatus(player, 0);
            return;
        }
        if (action.startsWith("msg:")) {
            player.closeInventory();
            player.sendMessage(CC.color(action.substring(4).replace("%player%", player.getName())));
            return;
        }
        if (action.startsWith("cmd:")) {
            player.closeInventory();
            String command = action.substring(4).replace("%player%", player.getName());
            Bukkit.dispatchCommand(player, command);
            log(player, command);
            return;
        }
        if (!(plugin instanceof Game game)) {
            player.sendMessage(CC.color("&cDeveloper Controls require the SMP Game plugin instance."));
            return;
        }
        switch (action.toLowerCase()) {
            case "bazaar" -> game.getMenuManager().openBazaar(player);
            case "bazaarvalidate" -> game.getBazaarManager().validateCustomMaterialRegistry()
                    .forEach(line -> player.sendMessage(CC.color(line)));
            case "bazaarreload" -> {
                game.getBazaarManager().refreshCustomMaterialRegistry();
                player.sendMessage(CC.color(game.getBazaarManager().requestMarketReload()
                        ? "&eReloading the Bazaar cache asynchronously..."
                        : "&7A Bazaar cache reload is already running or MariaDB is unavailable."));
            }
            case "bazaarrecalculate" -> player.sendMessage(CC.color(game.getBazaarManager().requestPriceRecalculation()
                    ? "&eRecalculating Bazaar prices asynchronously..."
                    : "&7A price recalculation is already running or MariaDB is unavailable."));
            case "bazaardebugheld" -> {
                String id = game.getCustomItemManager().getItemId(player.getInventory().getItemInMainHand());
                if (id == null) player.sendMessage(CC.color("&cThe held item has no custom item ID PDC."));
                else game.getBazaarManager().marketStatusLines(id).forEach(line -> player.sendMessage(CC.color(line)));
            }
            case "bazaardebugrows" -> game.getBazaarManager().cachedMarketDebugLines(10)
                    .forEach(line -> player.sendMessage(CC.color(line)));
            case "bazaargivetest" -> {
                int given = 0;
                for (String id : game.getBazaarManager().getItemIds()) {
                    if (given >= 9) break;
                    player.getInventory().addItem(game.getBazaarManager().createItemStack(id, 16));
                    given++;
                }
                player.sendMessage(CC.color("&aGave &e" + given + " &aBazaar test material stack(s) with custom ID PDC."));
            }
            case "bazaarclear" -> {
                game.getBazaarManager().clearMarketCache();
                player.sendMessage(CC.color("&aCleared the in-memory Bazaar market cache."));
            }
            case "abilityvalidate" -> game.getCustomItemManager().validateAbilityRegistry()
                    .forEach(line -> player.sendMessage(CC.color(line)));
            case "abilitytestheld" -> player.sendMessage(CC.color(game.getCustomItemManager().testHeldItemAbility(player)
                    ? "&aTested the held custom item's ability through its registered trigger."
                    : "&cHold a compatible custom ability item near a test mob."));
            case "abilityclearcooldowns" -> {
                game.getCustomItemManager().clearAbilityCooldowns(player);
                player.sendMessage(CC.color("&aCleared your in-memory ability cooldowns and pending ability state."));
            }
            case "abilitygivetest" -> {
                int given = game.getCustomItemManager().giveAbilityTestItems(player);
                player.sendMessage(CC.color("&aGave &e" + given + " &aability test item(s) with custom ID PDC."));
            }
            case "abilitydebuglast" -> player.sendMessage(CC.color("&dLast ability cast: &f" + game.getCustomItemManager().debugLastAbilityCast(player)));
            case "bank" -> new BankMenu(plugin, game.getBankManager(), game.getEconomyManager()).open(player);
            case "eventcalendar" -> game.getMenuManager().openEventCalendar(player, 0);
            case "eventshop" -> game.getMenuManager().openEventShop(player, 0);
            case "dungeonsmenu" -> game.getMenuManager().openDungeons(player);
            case "dungeonkey" -> giveDungeonKey(player, game);
            case "dungeonstate" -> showDungeonState(player, game);
            case "dungeonvalidate" -> validateDungeons(player, game);
            case "dungeonrequirements" -> validateDungeonRequirements(player, game);
            case "furnitureshop" -> game.getFurnitureManager().openShop(player, club.abelix.games.fakevival.furniture.FurnitureDefinition.Category.CHAIRS);
            case "warps" -> game.getMenuManager().openWarps(player, 0);
            case "zonesmenu" -> game.getMenuManager().openDiscoveredZones(player, 0);
            case "refreshhud" -> {
                game.getBoardManager().start();
                for (Player online : Bukkit.getOnlinePlayers()) game.getTablistManager().apply(online);
                player.sendMessage(CC.color("&aRefreshed scoreboard/tablist/actionbar systems."));
            }
            case "entitycount" -> debugEntityCount(player);
            default -> {
                if (action.startsWith("shop:")) {
                    game.getMenuManager().openShop(player, action.substring("shop:".length()));
                } else if (action.startsWith("quests:")) {
                    game.getMenuManager().openAdventureGuildQuests(player, action.substring("quests:".length()), 0);
                } else {
                    player.sendMessage(CC.color("&cUnknown Developer Control action: &f" + action));
                }
            }
        }
    }

    private void debugEntityCount(Player player) {
        int entities = 0;
        int living = 0;
        int mobs = 0;
        for (Entity entity : player.getNearbyEntities(32, 32, 32)) {
            entities++;
            if (entity instanceof LivingEntity) living++;
            if (entity instanceof LivingEntity && !(entity instanceof Player)) mobs++;
        }
        player.sendMessage(CC.color("&eNearby entities within 32 blocks: &f" + entities + " &7(living: &f" + living + "&7, non-player mobs: &f" + mobs + "&7)"));
    }

    private void giveDungeonKey(Player player, Game game) {
        for (String id : java.util.List.of("dungeon_key_imperial", "dungeon_key_frozen", "dungeon_key_shadow", "dungeon_entry_token")) {
            if (!game.getCustomItemManager().itemExists(id)) continue;
            player.getInventory().addItem(game.getCustomItemManager().createItem(id, 1));
            player.sendMessage(CC.color("&aGave dungeon key/item &f" + id + "&a."));
            return;
        }
        player.sendMessage(CC.color("&cNo registered dungeon key custom item was found."));
    }

    private void showDungeonState(Player player, Game game) {
        club.abelix.games.fakevival.managers.DungeonManager.DungeonSnapshot snapshot = game.getDungeonManager().getSnapshot(player);
        player.sendMessage(CC.color("&eDungeon: &f" + snapshot.name() + " &7active=&f" + snapshot.active() + " &7mobs=&f" + snapshot.mobsAlive() + " &7boss=&f" + snapshot.bossStatus() + " &7progress=&f" + snapshot.progress() + "%"));
    }

    private void validateDungeons(Player player, Game game) {
        java.util.List<club.abelix.games.fakevival.managers.DungeonManager.DungeonDefinition> dungeons = game.getDungeonManager().dungeonDefinitions();
        player.sendMessage(CC.color("&aValidated &f" + dungeons.size() + " &adungeon registry entries."));
    }

    private void validateDungeonRequirements(Player player, Game game) {
        for (club.abelix.games.fakevival.managers.DungeonManager.DungeonDefinition dungeon : game.getDungeonManager().dungeonDefinitions()) {
            club.abelix.games.fakevival.managers.DungeonManager.EntryCheck check = game.getDungeonManager().checkEntry(player, dungeon.id());
            player.sendMessage(CC.color("&7- &f" + dungeon.id() + "&7: " + check.message()));
        }
    }

    private void log(Player player, String command) {
        if (plugin instanceof Game game && game.getDiscordWebhookManager() != null) {
            game.getLogger().info("[DeveloperControls] " + player.getName() + " ran /" + command);
        } else {
            plugin.getLogger().info("[DeveloperControls] " + player.getName() + " ran /" + command);
        }
    }

    private void handleCategory(Player player, String type, ItemStack clicked, boolean rightClick) {
        String action = loreValue(clicked, "SMP Dev Action: ");
        if ("back".equalsIgnoreCase(action)) {
            menu.openMain(player);
            return;
        }
        Integer page = pageLore(clicked);
        if (page != null) {
            String[] parts = type.split(":");
            menu.openCategory(player, parts.length >= 3 ? parts[2] : "system", page);
            return;
        }
        String key = loreValue(clicked, "SMP Dev Control: ");
        if (key == null) return;
        applyClick(player, key, rightClick, type);
    }

    private void handleStatus(Player player, String type, ItemStack clicked, boolean rightClick) {
        String action = loreValue(clicked, "SMP Dev Action: ");
        if ("back".equalsIgnoreCase(action)) {
            menu.openMain(player);
            return;
        }
        Integer page = pageLore(clicked);
        if (page != null) {
            menu.openStatus(player, page);
            return;
        }
        String key = loreValue(clicked, "SMP Dev Control: ");
        if (key == null) return;
        applyClick(player, key, rightClick, type);
    }

    private void handleConfirm(Player player, String type, ItemStack clicked) {
        String action = loreValue(clicked, "SMP Dev Action: ");
        String[] parts = type.split(":", 5);
        if (parts.length < 5 || "cancel".equalsIgnoreCase(action)) {
            menu.openMain(player);
            return;
        }
        if (!"confirm".equalsIgnoreCase(action)) return;
        String key = parts[2];
        String value = parts[3];
        String returnType = parts[4];
        if (liveControls.setOverride(key, value, player.getName(), "Developer GUI")) {
            player.sendMessage(CC.color("&aSaved live control &f" + key + "&a = &f" + value));
        } else {
            player.sendMessage(CC.color("&cCould not save live control. Value failed validation."));
        }
        reopen(player, returnType);
    }

    private void applyClick(Player player, String key, boolean rightClick, String returnType) {
        LiveControlDefinition definition = liveControls.definition(key);
        if (definition == null) return;
        if (rightClick) {
            liveControls.resetOverride(key, player.getName(), "Developer GUI");
            player.sendMessage(CC.color("&aReset &f" + key + " &ato its Java default."));
            reopen(player, returnType);
            return;
        }
        if (definition.type() == LiveControlType.STRING) {
            player.sendMessage(CC.color("&eUse &f/smp dev set " + key + " <value> [reason] &eto change text values."));
            return;
        }
        String nextValue = nextValue(definition);
        if (definition.major() && "true".equalsIgnoreCase(nextValue) && !liveControls.bool(key)) {
            menu.openConfirm(player, definition, nextValue, returnType);
            return;
        }
        if (liveControls.setOverride(key, nextValue, player.getName(), "Developer GUI")) {
            player.sendMessage(CC.color("&aSaved live control &f" + key + "&a = &f" + nextValue));
        } else {
            player.sendMessage(CC.color("&cCould not save live control. Value failed validation."));
        }
        reopen(player, returnType);
    }

    private String nextValue(LiveControlDefinition definition) {
        if (definition.type() == LiveControlType.BOOLEAN) return String.valueOf(!liveControls.bool(definition.key()));
        double value = liveControls.decimal(definition.key());
        if (value < 1.0D) return "1.00";
        if (value < 1.5D) return "1.50";
        if (value < 2.0D) return "2.00";
        if (value < 3.0D) return "3.00";
        return "0.50";
    }

    private void reopen(Player player, String returnType) {
        if (returnType.startsWith("smp-dev:category:")) {
            String[] parts = returnType.split(":");
            int page = parts.length >= 4 ? parseInt(parts[3]) : 0;
            menu.openCategory(player, parts.length >= 3 ? parts[2] : "system", page);
            return;
        }
        if (returnType.startsWith("smp-dev:status:")) {
            String[] parts = returnType.split(":");
            menu.openStatus(player, parts.length >= 3 ? parseInt(parts[2]) : 0);
            return;
        }
        menu.openMain(player);
    }

    private Integer pageLore(ItemStack item) {
        String raw = loreValue(item, "SMP Dev Page: ");
        if (raw == null) return null;
        return parseInt(raw);
    }

    private int parseInt(String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String loreValue(ItemStack item, String prefix) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : Objects.requireNonNull(item.getItemMeta().getLore())) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith(prefix)) return clean.substring(prefix.length()).trim();
        }
        return null;
    }
}
