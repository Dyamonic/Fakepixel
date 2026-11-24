package net.fakepixel.housing.hologram;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HologramService implements Listener {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<String, List<ArmorStand>> spawned = new HashMap<>();
    private final Map<UUID, EditTarget> editing = new HashMap<>();
    private final String editTitle = "Hologram Edit";
    private static final String HOLOGRAM_META = "housing-hologram";
    private static final String HOLOGRAM_HOUSE_META = "housing-hologram-house";

    public HologramService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public ItemStack item() {
        ItemStack stack = new ItemStack(net.fakepixel.housing.util.Compat.mat("NAME_TAG"));
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color("&bHologram"));
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Place this inside your house"));
        lore.add(plugin.color("&7to create a floating hologram."));
        lore.add(plugin.color(""));
        lore.add(plugin.color("&7Special Housing item."));
        lore.add(plugin.color("&eClick to receive 1!"));
        lore.add(plugin.color("&8Housing Special: hologram"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isHologramItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            if ("Housing Special: hologram".equals(ChatColor.stripColor(line))) return true;
        }
        return false;
    }

    public void spawnAll(House house, World world) {
        if (house == null || world == null) return;
        despawnHouse(house);
        for (House.Hologram hologram : house.getHolograms().values()) spawn(house, hologram, world);
    }

    public House.Hologram upsertSpecial(House house, World world, String type, String linkedObjectId, Location location, List<String> lines) {
        if (house == null || world == null || location == null) return null;
        House.Hologram found = null;
        for (House.Hologram candidate : new ArrayList<>(house.getHolograms().values())) {
            if (!type.equals(candidate.getType()) || !linkedObjectId.equals(candidate.getLinkedObjectId())) continue;
            if (found == null) found = candidate;
            else deleteHologram(house, candidate.getId());
        }
        if (found == null) found = house.addHologram(location, lines, type, linkedObjectId);
        else {
            found.setLocation(location.getX() + "," + location.getY() + "," + location.getZ());
            found.setLines(lines);
            found.setVisible(true);
            found.setIdentity(type, linkedObjectId);
        }
        spawn(house, found, world);
        return found;
    }

    public void removeSpecial(House house, String type, String linkedObjectId) {
        if (house == null) return;
        for (House.Hologram hologram : new ArrayList<>(house.getHolograms().values()))
            if (type.equals(hologram.getType()) && linkedObjectId.equals(hologram.getLinkedObjectId())) deleteHologram(house, hologram.getId());
    }

    public void despawnHouse(House house) {
        if (house == null) return;
        for (House.Hologram hologram : house.getHolograms().values()) despawn(house, hologram);
    }

    public void spawnHologram(House house, House.Hologram hologram, World world) {
        if (house == null || hologram == null || world == null) return;
        spawn(house, hologram, world);
    }

    public void deleteHologram(House house, String hologramId) {
        if (house == null || hologramId == null || hologramId.trim().isEmpty()) return;
        House.Hologram hologram = house.getHologram(hologramId);
        if (hologram == null) return;
        despawn(house, hologram);
        house.removeHologram(hologram.getId());
    }

    public void despawnAll() {
        for (List<ArmorStand> stands : spawned.values()) {
            for (ArmorStand stand : stands) if (stand != null && !stand.isDead()) stand.remove();
        }
        spawned.clear();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isHologramItem(event.getItem())) return;
        event.setCancelled(true);
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !plugin.canEditHousingObjects(player, house)) {
            player.sendMessage(plugin.color("&cYou need build permission in this house to place holograms."));
            return;
        }
        if (house.getHolograms().size() >= net.fakepixel.housing.cluster.HousingPerformanceRegistry.MAX_HOLOGRAMS_PER_HOUSE) {
            player.sendMessage(plugin.color("&cThis house has reached its hologram limit."));
            return;
        }
        Location location = event.getClickedBlock().getLocation().add(0.5D, 1.8D, 0.5D);
        House.Hologram hologram = house.addHologram(location);
        spawn(house, hologram, player.getWorld());
        houses.saveAll();
        consumeOne(player);
        player.sendMessage(plugin.color("&aCreated a hologram."));
        openEdit(player, house, hologram);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!editTitle.equals(event.getView().getTitle())) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditHolograms(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        House.Hologram hologram = hologramFromToken(house, event.getView().getTopInventory().getItem(26));
        if (hologram == null) {
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 10) beginTextEdit(player, house, hologram, "edit");
        else if (slot == 11) beginTextEdit(player, house, hologram, "add");
        else if (slot == 12) {
            hologram.removeLastLine();
            saveRespawnOpen(player, house, hologram);
        } else if (slot == 13) {
            house.moveHologram(hologram.getId(), player.getLocation().add(0.0D, 1.8D, 0.0D));
            saveRespawnOpen(player, house, hologram);
            player.sendMessage(plugin.color("&aMoved hologram to your location."));
        } else if (slot == 14) {
            despawn(house, hologram);
            if (hologram.getId().equals(house.getParkourLeaderboardHologramId())) house.clearParkourLeaderboardHologramId();
            house.removeHologram(hologram.getId());
            houses.saveAll();
            player.closeInventory();
            player.sendMessage(plugin.color("&cDeleted hologram."));
        } else if (slot == 15) {
            hologram.setVisible(!hologram.isVisible());
            saveRespawnOpen(player, house, hologram);
        } else if (slot == 22) {
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        EditTarget target = editing.remove(event.getPlayer().getUniqueId());
        if (target == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            House house = houses.getHouseByWorld(target.worldName);
            House.Hologram hologram = house == null ? null : house.getHologram(target.hologramId);
            if (house == null || hologram == null || !canEditHolograms(player, house)) {
                player.sendMessage(plugin.color("&cYou cannot edit that hologram."));
                return;
            }
            if ("cancel".equalsIgnoreCase(message.trim())) {
                player.sendMessage(plugin.color("&cHologram edit cancelled."));
                openEdit(player, house, hologram);
                return;
            }
            if ("add".equals(target.action)) hologram.addLine(message);
            else hologram.setLine(0, message);
            saveRespawnOpen(player, house, hologram);
            player.sendMessage(plugin.color("&aSaved hologram text."));
        });
    }

    private void openEdit(Player player, House house, House.Hologram hologram) {
        Inventory inv = Bukkit.createInventory(null, 27, editTitle);
        inv.setItem(4, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&aHologram", "&7Lines: &f" + hologram.getLines().size(), "&7Visible: " + (hologram.isVisible() ? "&aYes" : "&cNo")));
        inv.setItem(10, named(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aEdit Text", "&7Edit the first line.", "", "&eClick to edit!"));
        inv.setItem(11, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&aAdd Line", "&7Add a new hologram line.", "", "&eClick to add!"));
        inv.setItem(12, named(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cRemove Line", "&7Remove the last line.", "", "&eClick to remove!"));
        inv.setItem(13, named(net.fakepixel.housing.util.Compat.mat("ENDER_PEARL"), "&aMove Hologram", "&7Move to your current location.", "", "&eClick to move!"));
        inv.setItem(14, named(net.fakepixel.housing.util.Compat.mat("TNT"), "&cDelete Hologram", "&7Delete this hologram.", "", "&eClick to delete!"));
        inv.setItem(15, named(hologram.isVisible() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aVisibility: " + (hologram.isVisible() ? "&aShown" : "&cHidden"), "&7Toggle hologram visibility."));
        inv.setItem(22, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aClose"));
        inv.setItem(26, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Hologram Token", "&8Hologram ID: " + hologram.getId()));
        player.openInventory(inv);
    }

    private void beginTextEdit(Player player, House house, House.Hologram hologram, String action) {
        editing.put(player.getUniqueId(), new EditTarget(house.getWorldName(), hologram.getId(), action));
        player.closeInventory();
        player.sendMessage(plugin.color("&aType the hologram text in chat. Color codes with & are supported."));
        player.sendMessage(plugin.color("&7Type &ccancel &7to cancel."));
    }

    private void saveRespawnOpen(Player player, House house, House.Hologram hologram) {
        spawn(house, hologram, player.getWorld());
        houses.saveAll();
        openEdit(player, house, hologram);
    }

    private void spawn(House house, House.Hologram hologram, World world) {
        despawn(house, hologram);
        if (!hologram.isVisible()) return;
        Location base = parse(world, hologram.getLocation());
        if (base == null) return;
        List<ArmorStand> stands = new ArrayList<>();
        List<String> lines = hologram.getLines();
        for (int i = 0; i < lines.size(); i++) {
            Location lineLocation = base.clone().subtract(0.0D, i * 0.25D, 0.0D);
            ArmorStand stand = (ArmorStand) world.spawnEntity(lineLocation, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setSmall(true);
            stand.setCanPickupItems(false);
            invokeBoolean(stand, "setMarker", true);
            invokeBoolean(stand, "setInvulnerable", true);
            invokeBoolean(stand, "setSilent", true);
            invokeBoolean(stand, "setCollidable", false);
            stand.setCustomNameVisible(true);
            stand.setMetadata(HOLOGRAM_META, new FixedMetadataValue(plugin, hologram.getId()));
            stand.setMetadata(HOLOGRAM_HOUSE_META, new FixedMetadataValue(plugin, house.getWorldName()));
            String text = plugin.color(lines.get(i));
            stand.setCustomName(text.length() > 64 ? text.substring(0, 64) : text);
            stands.add(stand);
        }
        List<String> ids = new ArrayList<>();
        for (ArmorStand stand : stands) ids.add(stand.getUniqueId().toString());
        hologram.setEntityIds(ids);
        spawned.put(key(house, hologram), stands);
    }

    private void despawn(House house, House.Hologram hologram) {
        List<ArmorStand> stands = spawned.remove(key(house, hologram));
        if (stands != null) {
            for (ArmorStand stand : stands) if (stand != null && !stand.isDead()) stand.remove();
        }
        World world = Bukkit.getWorld(house.getWorldName());
        if (world != null) {
            for (String id : new ArrayList<>(hologram.getEntityIds())) {
                try {
                    UUID uuid = UUID.fromString(id);
                    Entity entity = null;
                    for (Entity candidate : world.getEntities()) {
                        if (candidate.getUniqueId().equals(uuid)) {
                            entity = candidate;
                            break;
                        }
                    }
                    if (entity != null && !entity.isDead()) entity.remove();
                } catch (IllegalArgumentException ignored) {
                }
            }
            for (Entity entity : world.getEntities()) {
                if (isHologramEntity(entity, house.getWorldName(), hologram.getId())) entity.remove();
            }
        }
        hologram.setEntityIds(new ArrayList<>());
    }

    private void invokeBoolean(ArmorStand stand, String method, boolean value) {
        try { stand.getClass().getMethod(method, boolean.class).invoke(stand, value); }
        catch (ReflectiveOperationException ignored) { }
    }

    private void removeUntrackedStandsAt(World world, House.Hologram hologram) {
        Location base = parse(world, hologram.getLocation());
        if (base == null) return;
        double maxDistanceY = Math.max(0.75D, hologram.getLines().size() * 0.35D);
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof ArmorStand)) continue;
            ArmorStand stand = (ArmorStand) entity;
            if (stand.isVisible() || !stand.isCustomNameVisible()) continue;
            Location location = stand.getLocation();
            if (Math.abs(location.getX() - base.getX()) > 0.15D || Math.abs(location.getZ() - base.getZ()) > 0.15D) continue;
            if (location.getY() > base.getY() + 0.25D || location.getY() < base.getY() - maxDistanceY) continue;
            stand.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHologramDamage(EntityDamageEvent event) {
        if (isAnyHologramEntity(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHologramDamageByEntity(EntityDamageByEntityEvent event) {
        if (isAnyHologramEntity(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHologramInteract(PlayerInteractEntityEvent event) {
        if (isAnyHologramEntity(event.getRightClicked())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHologramInteractAt(PlayerInteractAtEntityEvent event) {
        if (isAnyHologramEntity(event.getRightClicked())) event.setCancelled(true);
    }

    private boolean canEditHolograms(Player player, House house) {
        if (player == null || house == null) return false;
        UUID uuid = player.getUniqueId();
        if (plugin.canBypassHouseMode(player)) return true;
        if (!house.isBuildMode()) return false;
        return house.getOwner().equals(uuid)
                || house.hasGroupPermission(uuid, "hologram")
                || house.hasGroupPermission(uuid, "edit")
                || house.hasGroupPermission(uuid, "hologram-edit")
                || house.hasGroupPermission(uuid, "hologram/edit")
                || house.hasGroupPermission(uuid, "hologram.edit")
                || house.canBuild(uuid);
    }

    private boolean isAnyHologramEntity(Entity entity) {
        return entity != null && entity.hasMetadata(HOLOGRAM_META);
    }

    private boolean isHologramEntity(Entity entity, String worldName, String hologramId) {
        if (entity == null || !entity.hasMetadata(HOLOGRAM_META) || !entity.hasMetadata(HOLOGRAM_HOUSE_META)) return false;
        String entityHologram = entity.getMetadata(HOLOGRAM_META).isEmpty() ? "" : entity.getMetadata(HOLOGRAM_META).get(0).asString();
        String entityHouse = entity.getMetadata(HOLOGRAM_HOUSE_META).isEmpty() ? "" : entity.getMetadata(HOLOGRAM_HOUSE_META).get(0).asString();
        return hologramId.equals(entityHologram) && worldName.equals(entityHouse);
    }

    private House.Hologram hologramFromToken(House house, ItemStack item) {
        if (house == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Hologram ID: ")) return house.getHologram(clean.substring("Hologram ID: ".length()));
        }
        return null;
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

    private Location parse(World world, String raw) {
        if (world == null || raw == null) return null;
        String[] split = raw.split(",");
        if (split.length != 3) return null;
        try {
            return new Location(world, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String key(House house, House.Hologram hologram) {
        return house.getWorldName() + ":" + hologram.getId();
    }

    private void consumeOne(Player player) {
        ItemStack hand = player.getItemInHand();
        if (hand == null) return;
        if (hand.getAmount() <= 1) player.setItemInHand(null);
        else hand.setAmount(hand.getAmount() - 1);
    }

    private static class EditTarget {
        private final String worldName;
        private final String hologramId;
        private final String action;

        private EditTarget(String worldName, String hologramId, String action) {
            this.worldName = worldName;
            this.hologramId = hologramId;
            this.action = action;
        }
    }
}
