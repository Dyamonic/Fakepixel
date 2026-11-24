package net.fakepixel.housing.npc;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HouseNpcService implements Listener {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<UUID, SpawnedNpc> spawned = new HashMap<>();
    private final Map<UUID, EditPrompt> prompts = new HashMap<>();
    private final String editTitle = "NPC Edit";

    public HouseNpcService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public ItemStack item() {
        ItemStack stack = new ItemStack(net.fakepixel.housing.util.Compat.mat("MONSTER_EGG"));
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color("&aNPC"));
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Place this inside your house"));
        lore.add(plugin.color("&7to create a Housing NPC."));
        lore.add(plugin.color(""));
        lore.add(plugin.color("&7Special Housing item."));
        lore.add(plugin.color("&eClick to receive 1!"));
        lore.add(plugin.color("&8Housing Special: npc"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isNpcItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            if ("Housing Special: npc".equals(ChatColor.stripColor(line))) return true;
        }
        return false;
    }

    public void spawnAll(House house, World world) {
        if (house == null || world == null) return;
        despawnHouse(house);
        for (House.HouseNpc npc : house.getHouseNpcs().values()) spawn(house, npc, world);
    }

    public void despawnAll() {
        for (SpawnedNpc npc : new ArrayList<>(spawned.values())) {
            if (plugin.getHousingNpcService() != null) plugin.getHousingNpcService().removeManagedNpc(npc.handle);
        }
        spawned.clear();
    }

    @EventHandler
    public void onUseItem(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (!isNpcItem(event.getItem())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !plugin.canEditHousingObjects(player, house)) {
            player.sendMessage(plugin.color("&cYou need build permission in this house to place NPCs."));
            return;
        }
        if (house.getHouseNpcs().size() >= net.fakepixel.housing.cluster.HousingPerformanceRegistry.MAX_NPCS_PER_HOUSE) {
            player.sendMessage(plugin.color("&cThis house has reached its NPC limit."));
            return;
        }
        Location location = event.getClickedBlock() == null ? player.getLocation() : event.getClickedBlock().getLocation().add(0.5D, 1.0D, 0.5D);
        location.setYaw(player.getLocation().getYaw());
        location.setPitch(0.0F);
        House.HouseNpc npc = house.addHouseNpc(location);
        houses.saveAll();
        spawn(house, npc, player.getWorld());
        consumeOne(player);
        player.sendMessage(plugin.color("&aCreated a Housing NPC."));
        openEdit(player, house, npc);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!editTitle.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEdit(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        House.HouseNpc npc = npcFromToken(house, event.getView().getTopInventory().getItem(35));
        if (npc == null) {
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 10) beginPrompt(player, house, npc, "name");
        else if (slot == 11) beginPrompt(player, house, npc, "skin");
        else if (slot == 12) {
            npc.setAction(nextAction(npc.getAction()));
            saveRespawnOpen(player, house, npc);
        } else if (slot == 13) beginPrompt(player, house, npc, "messages");
        else if (slot == 14) {
            if ("CALL_FUNCTION".equals(npc.getAction())) beginPrompt(player, house, npc, "function");
            else {
                npc.setTeleportLocation(serialize(player.getLocation()));
                saveRespawnOpen(player, house, npc);
                player.sendMessage(plugin.color("&aSet NPC teleport location."));
            }
        } else if (slot == 15) {
            npc.setLookAtPlayers(!npc.isLookAtPlayers());
            saveRespawnOpen(player, house, npc);
        } else if (slot == 16) {
            npc.setVisible(!npc.isVisible());
            saveRespawnOpen(player, house, npc);
        } else if (slot == 21) {
            house.moveHouseNpc(npc.getId(), player.getLocation());
            saveRespawnOpen(player, house, npc);
            player.sendMessage(plugin.color("&aTeleported NPC to you."));
        } else if (slot == 23) {
            despawn(house, npc);
            if (plugin.getHologramService() != null) plugin.getHologramService().removeSpecial(house, "HOUSE_NPC_NAMEPLATE", npc.getId());
            house.removeHouseNpc(npc.getId());
            houses.saveAll();
            player.closeInventory();
            player.sendMessage(plugin.color("&cDeleted NPC."));
        } else if (slot == 31) {
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        EditPrompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            House house = houses.getHouseByWorld(prompt.worldName);
            House.HouseNpc npc = house == null ? null : house.getHouseNpc(prompt.npcId);
            if (house == null || npc == null || !canEdit(player, house)) {
                player.sendMessage(plugin.color("&cYou cannot edit that NPC."));
                return;
            }
            if ("cancel".equalsIgnoreCase(message.trim())) {
                player.sendMessage(plugin.color("&cNPC edit cancelled."));
                openEdit(player, house, npc);
                return;
            }
            if ("name".equals(prompt.field)) npc.setName(message);
            else if ("skin".equals(prompt.field)) npc.setSkin(message);
            else if ("messages".equals(prompt.field)) {
                List<String> lines = new ArrayList<>();
                for (String split : message.split("\\|")) lines.add(split.trim());
                npc.setMessages(lines);
            } else if ("function".equals(prompt.field)) {
                npc.setTeleportLocation(House.cleanFunctionId(message));
            }
            saveRespawnOpen(player, house, npc);
            player.sendMessage(plugin.color("&aSaved NPC " + prompt.field + "."));
        });
    }

    private void handleNpcClick(Player player, SpawnedNpc spawnedNpc) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !house.getWorldName().equals(spawnedNpc.worldName)) return;
        House.HouseNpc npc = house.getHouseNpc(spawnedNpc.npcId);
        if (npc == null) return;
        if (canEdit(player, house) && player.isSneaking()) {
            openEdit(player, house, npc);
            return;
        }
        if ("MESSAGE".equals(npc.getAction())) {
            for (String line : npc.getMessages()) player.sendMessage(format(line, player, house));
        } else if ("TELEPORT".equals(npc.getAction())) {
            Location target = parse(player.getWorld(), npc.getTeleportLocation());
            if (target == null) {
                player.sendMessage(plugin.color("&cThis NPC does not have a teleport location set."));
                return;
            }
            player.teleport(target);
        } else if ("CALL_FUNCTION".equals(npc.getAction()) && plugin.getFunctionService() != null) {
            plugin.getFunctionService().runFunction(player, house, npc.getTeleportLocation());
        }
    }

    private void openEdit(Player player, House house, House.HouseNpc npc) {
        Inventory inv = Bukkit.createInventory(null, 36, editTitle);
        inv.setItem(4, named(net.fakepixel.housing.util.Compat.mat("PLAYER_HEAD"), npc.getName(),
                "&7Action: &f" + npc.getAction(),
                "&7Skin: &f" + (npc.getSkin().isEmpty() ? "Default" : npc.getSkin()),
                "&7Visible: " + (npc.isVisible() ? "&aYes" : "&cNo")));
        inv.setItem(10, named(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aChange Name", "&7Supports & color codes.", "", "&eClick to edit!"));
        inv.setItem(11, named(net.fakepixel.housing.util.Compat.mat("SKULL_ITEM"), "&aChange Skin", "&7Stores a skin/player name.", "&7Uses the custom Housing NPC skin system.", "", "&eClick to edit!"));
        inv.setItem(12, named(net.fakepixel.housing.util.Compat.mat("REDSTONE"), "&aChange Action", "&7Current: &f" + npc.getAction(), "&7Safe actions only.", "", "&eClick to cycle!"));
        inv.setItem(13, named(net.fakepixel.housing.util.Compat.mat("BOOK"), "&aEdit Messages", "&7Separate multiple lines with &f|&7.", "&7Placeholders: %player%, %owner%, %house_name%", "", "&eClick to edit!"));
        inv.setItem(14, named(net.fakepixel.housing.util.Compat.mat("ENDER_PEARL"),
                "CALL_FUNCTION".equals(npc.getAction()) ? "&aSet Function" : "&aSet Teleport Location",
                "CALL_FUNCTION".equals(npc.getAction()) ? "&7Current: &f" + (npc.getTeleportLocation().isEmpty() ? "none" : npc.getTeleportLocation()) : "&7Set to your current location.",
                "",
                "&eClick to set!"));
        inv.setItem(15, named(npc.isLookAtPlayers() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aLooking at Players: " + (npc.isLookAtPlayers() ? "&aEnabled" : "&cDisabled"), "&7Toggle look behavior."));
        inv.setItem(16, named(npc.isVisible() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aVisibility: " + (npc.isVisible() ? "&aShown" : "&cHidden"), "&7Toggle NPC visibility."));
        inv.setItem(21, named(net.fakepixel.housing.util.Compat.mat("COMPASS"), "&aTeleport NPC Here", "&7Move NPC to your current location.", "", "&eClick to move!"));
        inv.setItem(23, named(net.fakepixel.housing.util.Compat.mat("TNT"), "&cDelete NPC", "&7Delete this NPC.", "", "&eClick to delete!"));
        inv.setItem(31, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aClose"));
        inv.setItem(35, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7NPC Token", "&8NPC ID: " + npc.getId()));
        player.openInventory(inv);
    }

    private void beginPrompt(Player player, House house, House.HouseNpc npc, String field) {
        prompts.put(player.getUniqueId(), new EditPrompt(house.getWorldName(), npc.getId(), field));
        player.closeInventory();
        player.sendMessage(plugin.color("&aType the new NPC " + field + " in chat. Type &ccancel &ato cancel."));
        if ("messages".equals(field)) player.sendMessage(plugin.color("&7Use &f| &7between lines. Placeholders: &e%player% %owner% %house_name%"));
        if ("function".equals(field)) player.sendMessage(plugin.color("&7Type the function id, like &estart_parkour&7."));
    }

    private void saveRespawnOpen(Player player, House house, House.HouseNpc npc) {
        houses.saveAll();
        spawn(house, npc, player.getWorld());
        openEdit(player, house, npc);
    }

    private void spawn(House house, House.HouseNpc npc, World world) {
        despawn(house, npc);
        if (!npc.isVisible()) {
            if (plugin.getHologramService() != null) plugin.getHologramService().removeSpecial(house, "HOUSE_NPC_NAMEPLATE", npc.getId());
            return;
        }
        Location location = parse(world, npc.getLocation());
        if (location == null || plugin.getHousingNpcService() == null) return;
        final UUID[] handleRef = new UUID[1];
        String name = npc.getName()
                .replace("%owner%", house.getOwnerName())
                .replace("%house_name%", house.getName())
                .replace("%player%", "");
        UUID handle = plugin.getHousingNpcService().spawnManagedNpc(name, location, npc.getSkin(), npc.isLookAtPlayers(), player -> {
            SpawnedNpc spawnedNpc = spawned.get(handleRef[0]);
            if (spawnedNpc != null) handleNpcClick(player, spawnedNpc);
        });
        if (handle == null) return;
        handleRef[0] = handle;
        spawned.put(handle, new SpawnedNpc(house.getWorldName(), npc.getId(), handle));
        if (plugin.getHologramService() != null) {
            Location nameplate = location.clone().add(0.0D, 2.35D, 0.0D);
            plugin.getHologramService().upsertSpecial(house, world, "HOUSE_NPC_NAMEPLATE", npc.getId(), nameplate,
                    java.util.Arrays.asList(name, "&e&lRIGHT CLICK"));
        }
    }

    private void despawn(House house, House.HouseNpc npc) {
        spawned.entrySet().removeIf(entry -> {
            SpawnedNpc value = entry.getValue();
            if (!value.worldName.equals(house.getWorldName()) || !value.npcId.equals(npc.getId())) return false;
            if (plugin.getHousingNpcService() != null) plugin.getHousingNpcService().removeManagedNpc(value.handle);
            return true;
        });
    }

    private void despawnHouse(House house) {
        for (House.HouseNpc npc : house.getHouseNpcs().values()) despawn(house, npc);
    }

    public void despawnHouseEntities(House house) {
        if (house != null) despawnHouse(house);
    }

    private House.HouseNpc npcFromToken(House house, ItemStack item) {
        if (house == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("NPC ID: ")) return house.getHouseNpc(clean.substring("NPC ID: ".length()));
        }
        return null;
    }

    private boolean canEdit(Player player, House house) {
        return plugin.canEditHousingObjects(player, house)
                || (player != null && house != null && house.hasGroupPermission(player.getUniqueId(), "settings"));
    }

    private String nextAction(String action) {
        if ("NONE".equals(action)) return "MESSAGE";
        if ("MESSAGE".equals(action)) return "TELEPORT";
        if ("TELEPORT".equals(action)) return "CALL_FUNCTION";
        return "NONE";
    }

    private String format(String raw, Player player, House house) {
        return plugin.color((raw == null ? "" : raw)
                .replace("%player%", player.getName())
                .replace("%owner%", house.getOwnerName())
                .replace("%house_name%", house.getName()));
    }

    private ItemStack named(Material material, String name, String... lore) {
        return named(new ItemStack(material), name, lore);
    }

    private ItemStack named(ItemStack stack, String name, String... lore) {
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        List<String> lines = new ArrayList<>();
        for (String line : lore) lines.add(plugin.color(line));
        meta.setLore(lines);
        stack.setItemMeta(meta);
        return stack;
    }

    private String serialize(Location location) {
        return location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private Location parse(World world, String raw) {
        if (world == null || raw == null || raw.trim().isEmpty()) return null;
        String[] split = raw.split(",");
        if (split.length < 3) return null;
        try {
            Location location = new Location(world, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
            if (split.length >= 5) {
                location.setYaw(Float.parseFloat(split[3]));
                location.setPitch(Float.parseFloat(split[4]));
            }
            return location;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void consumeOne(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand == null) return;
        if (hand.getAmount() <= 1) player.setItemInHand(null);
        else hand.setAmount(hand.getAmount() - 1);
    }

    private static class SpawnedNpc {
        private final String worldName;
        private final String npcId;
        private final UUID handle;

        private SpawnedNpc(String worldName, String npcId, UUID handle) {
            this.worldName = worldName;
            this.npcId = npcId;
            this.handle = handle;
        }
    }

    private static class EditPrompt {
        private final String worldName;
        private final String npcId;
        private final String field;

        private EditPrompt(String worldName, String npcId, String field) {
            this.worldName = worldName;
            this.npcId = npcId;
            this.field = field;
        }
    }
}
