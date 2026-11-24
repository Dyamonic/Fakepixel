package net.fakepixel.housing.build;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SelectionService {
    private final HousingPlugin plugin;
    private final Map<UUID, Location> pos1 = new HashMap<>();
    private final Map<UUID, Location> pos2 = new HashMap<>();
    private final Map<UUID, Deque<EditHistory>> undo = new HashMap<>();
    private final Map<UUID, Deque<EditHistory>> redo = new HashMap<>();
    private final Map<UUID, Clipboard> clipboard = new HashMap<>();

    public SelectionService(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setPos1(Player player, Location location) {
        if (!canUseSelectionAt(player, location)) return false;
        pos1.put(player.getUniqueId(), blockLocation(location));
        player.sendMessage(plugin.color("&aPosition 1 set."));
        return true;
    }

    public boolean setPos2(Player player, Location location) {
        if (!canUseSelectionAt(player, location)) return false;
        pos2.put(player.getUniqueId(), blockLocation(location));
        player.sendMessage(plugin.color("&aPosition 2 set."));
        return true;
    }

    public Location getPos1(Player player) { return pos1.get(player.getUniqueId()); }
    public Location getPos2(Player player) { return pos2.get(player.getUniqueId()); }

    public boolean hasSelection(Player player) {
        Location a = getPos1(player);
        Location b = getPos2(player);
        return a != null && b != null && a.getWorld() != null && b.getWorld() != null && a.getWorld().equals(b.getWorld());
    }

    public void set(Player player, Material material, byte data, boolean wallsOnly) {
        if (wallsOnly) setWalls(player, material, data);
        else fill(player, material, data);
    }

    @SuppressWarnings("deprecation")
    public void fill(Player player, Material material, byte data) {
        Region region = validRegion(player);
        if (region == null || material == null) return;
        apply(player, region, block -> true, material.getId(), data, "&aFilled &e%d &ablocks.");
    }

    @SuppressWarnings("deprecation")
    public void replace(Player player, MaterialData from, MaterialData to) {
        Region region = validRegion(player);
        if (region == null || from == null || to == null) return;
        apply(player, region, block -> block.getTypeId() == from.material.getId() && block.getData() == from.data,
                to.material.getId(), to.data, "&aReplaced &e%d &ablocks.");
    }

    @SuppressWarnings("deprecation")
    public void clear(Player player) {
        Region region = validRegion(player);
        if (region == null) return;
        apply(player, region, block -> block.getType() != Material.AIR, Material.AIR.getId(), (byte) 0, "&aCleared &e%d &ablocks.");
    }

    @SuppressWarnings("deprecation")
    public void setWalls(Player player, Material material, byte data) {
        Region region = validRegion(player);
        if (region == null || material == null) return;
        apply(player, region, block -> block.getX() == region.minX || block.getX() == region.maxX || block.getZ() == region.minZ || block.getZ() == region.maxZ,
                material.getId(), data, "&aSet walls for &e%d &ablocks.");
    }

    @SuppressWarnings("deprecation")
    public void setFloor(Player player, Material material, byte data) {
        Region region = validRegion(player);
        if (region == null || material == null) return;
        apply(player, region, block -> block.getY() == region.minY, material.getId(), data, "&aSet floor for &e%d &ablocks.");
    }

    public void cut(Player player) {
        if (!copy(player)) return;
        clear(player);
    }

    public boolean copy(Player player) {
        Region region = validRegion(player);
        if (region == null) return false;
        List<ClipboardBlock> copied = new ArrayList<>();
        forEach(region, block -> copied.add(new ClipboardBlock(block, region.minX, region.minY, region.minZ)));
        clipboard.put(player.getUniqueId(), new Clipboard(copied, region.sizeX(), region.sizeY(), region.sizeZ()));
        player.sendMessage(plugin.color("&aCopied &e" + copied.size() + " &ablocks."));
        return true;
    }

    public void paste(Player player) {
        Clipboard copied = clipboard.get(player.getUniqueId());
        if (copied == null || copied.blocks.isEmpty()) {
            player.sendMessage(plugin.color("&cYour clipboard is empty."));
            return;
        }
        Location base = blockLocation(player.getLocation());
        Region region = new Region(base.getWorld(), base.getBlockX(), base.getBlockX() + copied.sizeX - 1,
                base.getBlockY(), base.getBlockY() + copied.sizeY - 1,
                base.getBlockZ(), base.getBlockZ() + copied.sizeZ - 1);
        if (!validRegion(player, region)) return;
        House house = plugin.getHouseManager().getHouseByWorld(base.getWorld().getName());
        List<BlockStateData> before = new ArrayList<>();
        for (ClipboardBlock copiedBlock : copied.blocks) {
            Block block = base.getWorld().getBlockAt(base.getBlockX() + copiedBlock.dx, base.getBlockY() + copiedBlock.dy, base.getBlockZ() + copiedBlock.dz);
            before.add(new BlockStateData(block));
            copiedBlock.apply(block);
            if (house != null) house.markPlayerEditedBlock(block.getX(), block.getY(), block.getZ());
        }
        pushUndo(player, before);
        plugin.getHouseManager().saveAll();
        player.sendMessage(plugin.color("&aPasted &e" + before.size() + " &ablocks."));
    }

    public void undo(Player player) {
        Deque<EditHistory> stack = undo.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            player.sendMessage(plugin.color("&cThere is nothing to undo."));
            return;
        }
        EditHistory history = stack.pop();
        history.restoreBefore();
        markEditedBlocks(history.before);
        plugin.getHouseManager().saveAll();
        redo.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()).push(history);
        player.sendMessage(plugin.color("&aUndid &e" + history.before.size() + " &ablocks."));
    }

    public void redo(Player player) {
        Deque<EditHistory> stack = redo.get(player.getUniqueId());
        if (stack == null || stack.isEmpty()) {
            player.sendMessage(plugin.color("&cThere is nothing to redo."));
            return;
        }
        EditHistory history = stack.pop();
        history.restoreAfter();
        markEditedBlocks(history.after);
        plugin.getHouseManager().saveAll();
        undo.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()).push(history);
        player.sendMessage(plugin.color("&aRedid &e" + history.after.size() + " &ablocks."));
    }

    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(net.fakepixel.housing.util.Compat.mat("WOODEN_AXE"));
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(plugin.color("&aSelection Wand"));
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Left click: &fPosition 1"));
        lore.add(plugin.color("&7Right click: &fPosition 2"));
        lore.add(plugin.color("&8Housing Pro Tool"));
        wand.setItemMeta(meta);
        player.getInventory().addItem(wand);
        player.sendMessage(plugin.color("&aYou received the Selection Wand."));
    }

    public boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        if (item.getType() != net.fakepixel.housing.util.Compat.mat("WOODEN_AXE")) return false;
        String name = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
        return name != null && (name.contains("Selection Wand") || name.contains("Housing Wand"));
    }

    public MaterialData parseMaterial(String input) {
        if (input == null) return null;
        String[] split = input.trim().split(":");
        byte data = 0;
        if (split.length > 1) {
            try { data = Byte.parseByte(split[1]); } catch (NumberFormatException ignored) {}
        }
        Material material = legacyMaterial(split[0], data);
        if (material == null) material = Material.matchMaterial(split[0].toUpperCase(Locale.ENGLISH));
        if (material == null) material = Material.getMaterial(split[0].toUpperCase(Locale.ENGLISH));
        return material == null ? null : new MaterialData(material, data);
    }

    @SuppressWarnings("deprecation")
    private void apply(Player player, Region region, BlockFilter filter, int typeId, byte data, String message) {
        List<BlockStateData> before = new ArrayList<>();
        List<BlockStateData> after = new ArrayList<>();
        House house = plugin.getHouseManager().getHouseByWorld(region.world.getName());
        forEach(region, block -> {
            if (!filter.accept(block)) return;
            before.add(new BlockStateData(block));
            block.setTypeIdAndData(typeId, data, false);
            after.add(new BlockStateData(block));
            if (house != null) house.markPlayerEditedBlock(block.getX(), block.getY(), block.getZ());
        });
        if (before.isEmpty()) {
            player.sendMessage(plugin.color("&cNo blocks were changed."));
            return;
        }
        pushUndo(player, before, after);
        plugin.getHouseManager().saveAll();
        player.sendMessage(plugin.color(String.format(message, before.size())));
    }

    private void pushUndo(Player player, List<BlockStateData> before) {
        List<BlockStateData> after = new ArrayList<>();
        for (BlockStateData state : before) after.add(new BlockStateData(state.location.getBlock()));
        pushUndo(player, before, after);
    }

    private void pushUndo(Player player, List<BlockStateData> before, List<BlockStateData> after) {
        Deque<EditHistory> stack = undo.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        stack.push(new EditHistory(before, after));
        while (stack.size() > 20) stack.removeLast();
        redo.remove(player.getUniqueId());
    }

    private void markEditedBlocks(List<BlockStateData> blocks) {
        if (blocks == null) return;
        for (BlockStateData data : blocks) {
            if (data == null || data.location == null || data.location.getWorld() == null) continue;
            House house = plugin.getHouseManager().getHouseByWorld(data.location.getWorld().getName());
            if (house != null) house.markPlayerEditedBlock(data.location);
        }
    }

    private Region validRegion(Player player) {
        if (!hasSelection(player)) {
            player.sendMessage(plugin.color("&cSelect two positions first with the Selection Wand."));
            return null;
        }
        Location a = getPos1(player);
        Location b = getPos2(player);
        Region region = new Region(a.getWorld(),
                Math.min(a.getBlockX(), b.getBlockX()), Math.max(a.getBlockX(), b.getBlockX()),
                Math.min(a.getBlockY(), b.getBlockY()), Math.max(a.getBlockY(), b.getBlockY()),
                Math.min(a.getBlockZ(), b.getBlockZ()), Math.max(a.getBlockZ(), b.getBlockZ()));
        return validRegion(player, region) ? region : null;
    }

    private boolean validRegion(Player player, Region region) {
        House house = region == null || region.world == null ? null : plugin.getHouseManager().getHouseByWorld(region.world.getName());
        if (house == null) {
            player.sendMessage(plugin.color("&cYou must be inside a house to use Pro Tools."));
            return false;
        }
        if (!canUseProTools(player, house)) {
            player.sendMessage(plugin.color("&cYou do not have permission to use Pro Tools in this house."));
            return false;
        }
        int maxBlocks = plugin.getStaticConfig().getInt("build.max-selection-blocks", 60000);
        if (region.volume() > maxBlocks) {
            player.sendMessage(plugin.color("&cThat selection is too big. Max: &e" + maxBlocks + " &cblocks."));
            return false;
        }
        int minY = plugin.getStaticConfig().getBoolean("border.min-build-y-enabled", true) ? plugin.getStaticConfig().getInt("border.min-build-y", 90) : 0;
        int maxY = Math.min(region.world.getMaxHeight() - 1, 255);
        if (region.minY < minY || region.maxY > maxY) {
            player.sendMessage(plugin.color("&cThat region is outside the allowed build height."));
            return false;
        }
        Location[] corners = new Location[]{
                new Location(region.world, region.minX, region.minY, region.minZ),
                new Location(region.world, region.minX, region.minY, region.maxZ),
                new Location(region.world, region.maxX, region.minY, region.minZ),
                new Location(region.world, region.maxX, region.minY, region.maxZ),
                new Location(region.world, region.minX, region.maxY, region.minZ),
                new Location(region.world, region.minX, region.maxY, region.maxZ),
                new Location(region.world, region.maxX, region.maxY, region.minZ),
                new Location(region.world, region.maxX, region.maxY, region.maxZ)
        };
        for (Location corner : corners) {
            if (!plugin.getHouseManager().isInsideBuildBorder(corner)) {
                player.sendMessage(plugin.color("&cThat region crosses this house's plot border."));
                return false;
            }
        }
        return true;
    }

    private boolean canUseSelectionAt(Player player, Location location) {
        if (location == null || location.getWorld() == null) return false;
        House house = plugin.getHouseManager().getHouseByWorld(location.getWorld().getName());
        if (house == null) {
            player.sendMessage(plugin.color("&cYou must be inside a house to use the Selection Wand."));
            return false;
        }
        if (!canUseProTools(player, house)) {
            player.sendMessage(plugin.color("&cYou do not have permission to use Pro Tools in this house."));
            return false;
        }
        if (!plugin.getHouseManager().isInsideBuildBorder(location)) {
            player.sendMessage(plugin.color("&cYou cannot select outside this house plot."));
            return false;
        }
        int minY = plugin.getStaticConfig().getBoolean("border.min-build-y-enabled", true) ? plugin.getStaticConfig().getInt("border.min-build-y", 90) : 0;
        if (location.getBlockY() < minY || location.getBlockY() >= location.getWorld().getMaxHeight()) {
            player.sendMessage(plugin.color("&cYou cannot select outside the allowed build height."));
            return false;
        }
        return true;
    }

    public boolean canUseProTools(Player player, House house) {
        return plugin.canUseProToolsInHouseMode(player, house);
    }

    private void forEach(Region region, BlockConsumer consumer) {
        for (int x = region.minX; x <= region.maxX; x++) {
            for (int y = region.minY; y <= region.maxY; y++) {
                for (int z = region.minZ; z <= region.maxZ; z++) {
                    consumer.accept(region.world.getBlockAt(x, y, z));
                }
            }
        }
    }

    private Material legacyMaterial(String name, byte data) {
        if (name == null) return null;
        String normalized = name.toUpperCase(Locale.ENGLISH);
        if ("WOOL".equals(normalized) && data == 14) return net.fakepixel.housing.util.Compat.mat("RED_WOOL");
        if ("WOOL".equals(normalized)) return net.fakepixel.housing.util.Compat.mat("WHITE_WOOL");
        if ("STAINED_CLAY".equals(normalized) && data == 14) return net.fakepixel.housing.util.Compat.mat("RED_TERRACOTTA");
        if ("STAINED_CLAY".equals(normalized)) return net.fakepixel.housing.util.Compat.mat("WHITE_TERRACOTTA");
        return null;
    }

    private Location blockLocation(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static class MaterialData {
        public final Material material;
        public final byte data;
        public MaterialData(Material material, byte data) { this.material = material; this.data = data; }
    }

    private interface BlockFilter { boolean accept(Block block); }
    private interface BlockConsumer { void accept(Block block); }

    private static class Region {
        private final World world;
        private final int minX, maxX, minY, maxY, minZ, maxZ;
        private Region(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.world = world;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = Math.max(0, minY);
            this.maxY = Math.min(255, maxY);
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
        private int sizeX() { return maxX - minX + 1; }
        private int sizeY() { return maxY - minY + 1; }
        private int sizeZ() { return maxZ - minZ + 1; }
        private int volume() { return sizeX() * sizeY() * sizeZ(); }
    }

    private static class Clipboard {
        private final List<ClipboardBlock> blocks;
        private final int sizeX, sizeY, sizeZ;
        private Clipboard(List<ClipboardBlock> blocks, int sizeX, int sizeY, int sizeZ) {
            this.blocks = blocks;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }
    }

    @SuppressWarnings("deprecation")
    private static class ClipboardBlock {
        private final int dx, dy, dz;
        private final int typeId;
        private final byte data;
        private ClipboardBlock(Block block, int originX, int originY, int originZ) {
            this.dx = block.getX() - originX;
            this.dy = block.getY() - originY;
            this.dz = block.getZ() - originZ;
            this.typeId = block.getTypeId();
            this.data = block.getData();
        }
        private void apply(Block block) {
            block.setTypeIdAndData(typeId, data, false);
        }
    }

    private static class EditHistory {
        private final List<BlockStateData> before;
        private final List<BlockStateData> after;
        private EditHistory(List<BlockStateData> before, List<BlockStateData> after) {
            this.before = before;
            this.after = after;
        }
        private void restoreBefore() { for (BlockStateData data : before) data.restore(); }
        private void restoreAfter() { for (BlockStateData data : after) data.restore(); }
    }

    @SuppressWarnings("deprecation")
    private static class BlockStateData {
        private final Location location;
        private final int typeId;
        private final byte data;
        private BlockStateData(Block block) {
            this.location = block.getLocation();
            this.typeId = block.getTypeId();
            this.data = block.getData();
        }
        private void restore() {
            Block block = location.getWorld().getBlockAt(location);
            block.setTypeIdAndData(typeId, data, false);
        }
    }
}
