package club.abelix.games.fakevival.menus;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.DungeonManager;
import club.abelix.games.fakevival.managers.DungeonAccessResult;
import club.abelix.games.fakevival.network.SMPServerType;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DungeonMenuService implements Listener {
    private static final int[] CONTENT_SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private final Game game;

    public DungeonMenuService(Game game) {
        this.game = game;
    }

    public void openSelection(Player player, int page) {
        List<DungeonManager.DungeonDefinition> definitions = game.getDungeonManager().dungeonDefinitions();
        int pages = Math.max(1, (definitions.size() + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        Inventory inventory = Bukkit.createInventory(new MenuHolder("dungeon-select:" + safePage), 54, CC.color("&8Dungeons"));
        int start = safePage * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length && start + i < definitions.size(); i++) {
            DungeonManager.DungeonDefinition definition = definitions.get(start + i);
            Details details = details(player, definition.id());
            inventory.setItem(CONTENT_SLOTS[i], item(definition.material(), (details.access.allowed() ? "&6&l" : "&8&l") + definition.displayName(), List.of(
                    "&7Dungeon ID: &e" + definition.id(),
                    "&7Mobs: &e" + details.mobCount,
                    "&7Bosses: &c" + details.bossCount,
                    "&7Requirements: " + (details.access.requirementsMet() ? "&aMet" : "&cLocked"),
                    "",
                    details.access.allowed() ? "&eClick to view or enter!" : "&cClick to view requirements.",
                    "&8Dungeon: " + definition.id())));
        }
        if (safePage > 0) inventory.setItem(45, item(Material.ARROW, "&ePrevious Page", List.of()));
        if (safePage + 1 < pages) inventory.setItem(53, item(Material.ARROW, "&eNext Page", List.of()));
        inventory.setItem(49, item(Material.BARRIER, "&cBack", List.of("&7Return to the main menu.")));
        player.openInventory(inventory);
    }

    public boolean openDetails(Player player, String requestedId) {
        DungeonManager.DungeonDefinition definition = definition(requestedId);
        if (definition == null) return false;
        Details details = details(player, definition.id());
        Inventory inventory = Bukkit.createInventory(new MenuHolder("dungeon-detail:" + definition.id()), 54, CC.color("&8" + definition.displayName()));
        inventory.setItem(13, item(definition.material(), "&6&l" + definition.displayName(), List.of(
                "&7Dungeon ID: &e" + definition.id(), "&7Status: " + details.status,
                "&7Mobs: &e" + details.mobCount + "x", "&7Bosses: &c" + details.bossCount + "x",
                "&7Size: &e" + details.size, "&7World/Template: &e" + details.world)));
        inventory.setItem(29, item(details.access.requirementsMet() ? Material.BOOK : Material.BARRIER, "&eRequirements", details.requirementLore));
        if (!details.access.exists()) {
            inventory.setItem(31, item(Material.BARRIER, "&cDungeon Not Found", List.of("&7This dungeon could not be found.")));
        } else if (!details.access.enabled()) {
            inventory.setItem(31, item(Material.BARRIER, "&cDungeon Disabled", List.of("&7This dungeon is currently disabled.")));
        } else if (!details.access.requirementsMet()) {
            List<String> missing = new ArrayList<>();
            missing.add("&7Missing requirements:");
            for (String reason : details.access.reasons()) missing.add("&c- " + reason);
            inventory.setItem(31, item(Material.BARRIER, "&cDungeon Locked", missing));
        } else {
            inventory.setItem(31, item(Material.ENDER_PEARL, "&aEnter Dungeon", List.of("&7Click to enter this dungeon.", "&eClick to start!")));
        }
        inventory.setItem(33, item(Material.CHEST, "&6Rewards & Information", List.of("&7" + definition.description(), "&7Boss: &c" + definition.bossName())));
        inventory.setItem(49, item(Material.ARROW, "&eBack", List.of("&7Return to all dungeons.")));
        player.openInventory(inventory);
        return true;
    }

    public boolean openMobs(Player player, String requestedId, int page) {
        DungeonManager.DungeonDefinition definition = definition(requestedId);
        if (definition == null) return false;
        List<Spawn> spawns = setupSpawns(definition.id());
        int size = spawns.size() <= 7 ? 27 : spawns.size() <= 14 ? 36 : 54;
        int capacity = size - 9;
        int pages = Math.max(1, (spawns.size() + capacity - 1) / capacity);
        int safePage = Math.max(0, Math.min(page, pages - 1));
        Inventory inventory = Bukkit.createInventory(new MenuHolder("dungeon-mobs:" + definition.id() + ":" + safePage), size,
                CC.color("&8Dungeon Mobs: " + definition.id()));
        int start = safePage * capacity;
        for (int i = 0; i < capacity && start + i < spawns.size(); i++) {
            Spawn spawn = spawns.get(start + i);
            Material material = spawn.boss ? Material.WITHER_SKELETON_SKULL : mobMaterial(spawn.entityType);
            inventory.setItem(i, item(material, (spawn.boss ? "&c&lBoss " : "&c") + spawn.id, List.of(
                    "&7Dungeon: &e" + definition.id(), "&7Type: &e" + (spawn.boss ? "boss" : "mob"),
                    "&7ID: &e" + spawn.id, "&7Entity: &e" + spawn.entityType, "&7Level: &e" + spawn.level,
                    "&7World: &e" + spawn.world, "&7X/Y/Z: &e" + spawn.xyz, "&7Spawn ID: &e" + spawn.id,
                    "", "&cClick to delete this spawn location.")));
        }
        if (safePage > 0) inventory.setItem(size - 9, item(Material.ARROW, "&ePrevious Page", List.of()));
        if (safePage + 1 < pages) inventory.setItem(size - 1, item(Material.ARROW, "&eNext Page", List.of()));
        inventory.setItem(size - 5, item(Material.BARRIER, "&cClose", List.of()));
        player.openInventory(inventory);
        return true;
    }

    public boolean setRequirement(String requestedId, String type, String value) {
        DungeonManager.DungeonDefinition definition = definition(requestedId);
        if (definition == null) return false;
        String path = switch (type.toLowerCase(Locale.ROOT)) {
            case "player", "playerlevel", "player_level" -> "required-player-level";
            case "combat", "combatlevel", "combat_level" -> "required-combat-level";
            case "key" -> "required-key";
            case "permission" -> "required-permission";
            default -> null;
        };
        if (path == null) return false;
        if (path.endsWith("level")) {
            try { game.getDataManager().getData().set(base(definition.id()) + "." + path, Math.max(0, Integer.parseInt(value))); }
            catch (NumberFormatException ignored) { return false; }
        } else game.getDataManager().getData().set(base(definition.id()) + "." + path, value.equalsIgnoreCase("none") ? null : value);
        game.getDataManager().save();
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder) || !(event.getWhoClicked() instanceof Player player)) return;
        String type = holder.type();
        if (!type.startsWith("dungeon-select:") && !type.startsWith("dungeon-detail:") && !type.startsWith("dungeon-mobs:")) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String name = plain(clicked);
        if (type.startsWith("dungeon-select:")) {
            int page = integer(type.substring(type.lastIndexOf(':') + 1));
            if (name.equals("previous page")) openSelection(player, page - 1);
            else if (name.equals("next page")) openSelection(player, page + 1);
            else if (clicked.getType() == Material.BARRIER) game.getMenuManager().openMainMenu(player);
            else { String dungeon = loreValue(clicked, "Dungeon: "); if (dungeon != null) openDetails(player, dungeon); }
            return;
        }
        if (type.startsWith("dungeon-detail:")) {
            String dungeon = type.substring("dungeon-detail:".length());
            if (name.equals("back")) openSelection(player, 0);
            else if (name.equals("enter dungeon")) enter(player, dungeon);
            else if (name.equals("dungeon disabled")) player.sendMessage(CC.color("&cThat dungeon is currently disabled."));
            else if (name.equals("dungeon locked")) player.sendMessage(CC.color("&cYou do not meet the requirements for this dungeon."));
            return;
        }
        if (!player.hasPermission("fakevival.dungeon.admin") && !player.hasPermission("fakevival.admin.dungeon")) return;
        String[] parts = type.split(":", 3);
        String dungeon = parts[1];
        int page = parts.length == 3 ? integer(parts[2]) : 0;
        if (name.equals("previous page")) openMobs(player, dungeon, page - 1);
        else if (name.equals("next page")) openMobs(player, dungeon, page + 1);
        else if (name.equals("close")) player.closeInventory();
        else {
            String spawnId = loreValue(clicked, "Spawn ID: ");
            String spawnType = loreValue(clicked, "Type: ");
            if (spawnId != null && deleteSpawn(dungeon, spawnId, "boss".equalsIgnoreCase(spawnType))) {
                player.sendMessage(CC.color(("boss".equalsIgnoreCase(spawnType) ? "&aDeleted dungeon boss spawn &e" : "&aDeleted dungeon mob spawn &e") + spawnId + "&a."));
                player.sendMessage(CC.color("&aDeleted spawn. It will no longer appear in new dungeon runs."));
                openMobs(player, dungeon, page);
            }
        }
    }

    private void enter(Player player, String dungeon) {
        DungeonAccessResult access = game.getDungeonManager().getAccessResult(player, dungeon);
        if (!access.exists()) { player.sendMessage(CC.color("&cDungeon not found.")); return; }
        if (!access.enabled()) { player.sendMessage(CC.color("&cThat dungeon is currently disabled.")); return; }
        if (!access.requirementsMet()) { player.sendMessage(CC.color("&cYou do not meet the requirements for this dungeon.")); return; }
        SMPServerType type = game.getNetworkService().getCurrentServerType();
        if (type == SMPServerType.DUNGEON) game.getDungeonManager().enterFromMenu(player, dungeon);
        else if (type == SMPServerType.HUB || type == SMPServerType.OVERWORLD) game.getNetworkService().sendToDungeon(player, dungeon);
        else player.sendMessage(CC.color("&cDungeon routing is unavailable on this server."));
    }

    private Details details(Player player, String dungeon) {
        DungeonManager.DungeonDefinition definition = definition(dungeon);
        String base = base(dungeon);
        ConfigurationSection data = game.getDataManager().getData();
        List<Spawn> spawns = setupSpawns(dungeon);
        long mobs = spawns.stream().filter(spawn -> !spawn.boss).count();
        long bosses = spawns.stream().filter(spawn -> spawn.boss).count();
        int playerRequired = data.getInt(base + ".required-player-level", 0);
        int combatRequired = data.getInt(base + ".required-combat-level", definition == null ? 0 : definition.requiredSkills().getOrDefault("combat", 0));
        String key = data.getString(base + ".required-key", definition == null ? "" : definition.requiredKeyId());
        String permission = data.getString(base + ".required-permission", "");
        DungeonAccessResult access = game.getDungeonManager().getAccessResult(player, dungeon);
        List<String> requirements = new ArrayList<>();
        requirements.add(status(access.playerLevelMet()) + " &7Player Level: &e" + playerRequired);
        requirements.add(status(access.combatLevelMet()) + " &7Combat Level: &e" + combatRequired);
        requirements.add(status(access.keyMet()) + " &7Key Required: &e" + (key == null || key.isBlank() ? "None" : key));
        if (permission != null && !permission.isBlank()) requirements.add(status(access.permissionMet()) + " &7Permission: &e" + permission);
        return new Details((int)mobs, (int)bosses, dungeonSize(base), data.getString(base + ".world", "Unknown"),
                access, requirements, "&e" + game.getDungeonManager().dungeonState(dungeon).name());
    }

    private List<Spawn> setupSpawns(String dungeon) {
        List<Spawn> result = new ArrayList<>();
        ConfigurationSection data = game.getDataManager().getData();
        String base = base(dungeon);
        ConfigurationSection mobs = firstSection(data, base + ".mobs", base + ".spawns");
        if (mobs != null) for (String id : mobs.getKeys(false)) result.add(spawn(mobs.getConfigurationSection(id), id, false));
        ConfigurationSection bosses = firstSection(data, base + ".bosses", base + ".boss-spawns");
        if (bosses != null) for (String id : bosses.getKeys(false)) result.add(spawn(bosses.getConfigurationSection(id), id, true));
        if (bosses == null && data.getConfigurationSection(base + ".boss.location") != null) result.add(spawn(data.getConfigurationSection(base + ".boss"), "legacy_boss", true));
        return result;
    }

    private Spawn spawn(ConfigurationSection section, String id, boolean boss) {
        if (section == null) return new Spawn(id, boss, "UNKNOWN", 1, "Unknown", "Unknown");
        ConfigurationSection location = section.getConfigurationSection("location");
        if (location == null) location = section;
        return new Spawn(id, boss, section.getString("type", boss ? "ZOMBIE" : id).toUpperCase(Locale.ROOT),
                section.getInt("level", 1), location.getString("world", "Unknown"),
                number(location, "x") + "/" + number(location, "y") + "/" + number(location, "z"));
    }

    private boolean deleteSpawn(String dungeon, String id, boolean boss) {
        ConfigurationSection data = game.getDataManager().getData();
        String root = base(dungeon) + (boss ? ".bosses." : ".mobs.") + id;
        if (!data.contains(root)) root = base(dungeon) + (boss ? ".boss-spawns." : ".spawns.") + id;
        if (boss && "legacy_boss".equalsIgnoreCase(id)) root = base(dungeon) + ".boss.location";
        if (!data.contains(root)) return false;
        data.set(root, null);
        game.getDataManager().save();
        return true;
    }

    private String dungeonSize(String base) {
        ConfigurationSection data = game.getDataManager().getData();
        if (!data.contains(base + ".area.pos1.x") || !data.contains(base + ".area.pos2.x")) return "Unknown";
        long x = Math.round(Math.abs(data.getDouble(base + ".area.pos1.x") - data.getDouble(base + ".area.pos2.x")));
        long y = Math.round(Math.abs(data.getDouble(base + ".area.pos1.y") - data.getDouble(base + ".area.pos2.y")));
        long z = Math.round(Math.abs(data.getDouble(base + ".area.pos1.z") - data.getDouble(base + ".area.pos2.z")));
        return x + "x" + y + "x" + z;
    }

    private DungeonManager.DungeonDefinition definition(String requested) {
        String normalized = normalize(requested);
        return game.getDungeonManager().dungeonDefinitions().stream().filter(value -> normalize(value.id()).equals(normalized)).findFirst().orElse(null);
    }

    private String base(String dungeon) {
        String normalized = normalize(dungeon);
        ConfigurationSection root = game.getDataManager().getData().getConfigurationSection("dungeons");
        if (root != null) for (String key : root.getKeys(false)) if (normalize(key).equals(normalized)) return "dungeons." + key;
        return "dungeons." + dungeon.toLowerCase(Locale.ROOT);
    }

    private boolean hasKey(Player player, String key) {
        for (ItemStack stack : player.getInventory().getStorageContents())
            if (stack != null && key.equalsIgnoreCase(String.valueOf(game.getCustomItemManager().getItemId(stack)))) return true;
        return false;
    }

    private static ConfigurationSection firstSection(ConfigurationSection root, String... paths) { for (String path : paths) { ConfigurationSection value = root.getConfigurationSection(path); if (value != null) return value; } return null; }
    private static String normalize(String value) { return DungeonManager.canonicalDungeonId(value); }
    private static String status(boolean met) { return met ? "&a✔" : "&c✘"; }
    private static String number(ConfigurationSection section, String key) { return section.contains(key) ? String.format(Locale.ROOT, "%.1f", section.getDouble(key)) : "?"; }
    private static int integer(String value) { try { return Integer.parseInt(value); } catch (Exception ignored) { return 0; } }
    private static Material mobMaterial(String type) { return type.contains("SKELETON") || type.contains("STRAY") ? Material.SKELETON_SKULL : type.contains("CREEPER") ? Material.CREEPER_HEAD : type.contains("ZOMBIE") || type.contains("HUSK") ? Material.ZOMBIE_HEAD : Material.PLAYER_HEAD; }
    private static String plain(ItemStack item) { return item.hasItemMeta() ? org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT) : ""; }
    private static String loreValue(ItemStack item, String prefix) { if (!item.hasItemMeta() || item.getItemMeta().getLore() == null) return null; for (String line : item.getItemMeta().getLore()) { String plain = org.bukkit.ChatColor.stripColor(line); if (plain.startsWith(prefix)) return plain.substring(prefix.length()); } return null; }
    private static ItemStack item(Material material, String name, List<String> lore) { ItemStack stack = new ItemStack(material == null || !material.isItem() ? Material.PAPER : material); ItemMeta meta = stack.getItemMeta(); if (meta != null) { meta.setDisplayName(CC.color(name)); meta.setLore(lore.stream().map(CC::color).toList()); stack.setItemMeta(meta); } return stack; }

    private record Spawn(String id, boolean boss, String entityType, int level, String world, String xyz) {}
    private record Details(int mobCount, int bossCount, String size, String world, DungeonAccessResult access, List<String> requirementLore, String status) {}
}
