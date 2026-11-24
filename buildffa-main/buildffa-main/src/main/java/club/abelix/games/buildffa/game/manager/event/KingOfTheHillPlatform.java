package club.abelix.games.buildffa.game.manager.event;

import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.config.WorldConfig;
import club.abelix.games.buildffa.game.config.BuildFFAConfig;
import club.abelix.games.buildffa.game.task.BlockTracker;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.TileEntityBeacon;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Beacon;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class KingOfTheHillPlatform extends YListener implements Listener {
    private final BuildFFAGame game;
    private final Map<Block, BlockState> originals = new LinkedHashMap<>();
    private final Map<Block, InstalledBlock> installed = new LinkedHashMap<>();
    private final Map<Block, BlockTracker.SuspendedBlock> suspendedDecay = new LinkedHashMap<>();
    private Location center;
    private int radius;
    private int height;
    private ResolvedHill resolved;
    private String arenaId;
    private String lookupPath;
    private String failureReason;
    private String failureDetail;

    public KingOfTheHillPlatform(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    private String fail(String reason, String detail) {
        failureReason = reason;
        failureDetail = detail;
        return reason;
    }

    private ResolvedHill resolve() {
        resolved = null;
        failureReason = null;
        failureDetail = null;
        WorldConfig arena = game.getWorldConfig();
        arenaId = game.getFfaConfig().getArenaId(arena);
        lookupPath = "king_of_the_hill.arenas." + (arenaId == null ? "<unresolved>" : arenaId);
        if (arena == null || arena.world() == null || Bukkit.getWorld(arena.world().getUID()) != arena.world()) {
            fail("world", "Current arena world is not loaded.");
            return null;
        }
        if (arenaId == null) {
            fail("missing", "Current WorldConfig has no registered Worlds arena ID.");
            return null;
        }
        YamlConfiguration config = game.getFfaConfig().getLoadedConfiguration();
        if (config == null) {
            fail("missing", "The arena configuration has not been loaded.");
            return null;
        }
        ConfigurationSection section = config.getConfigurationSection("king_of_the_hill");
        if (section == null) {
            fail("missing", "Missing root section king_of_the_hill.");
            return null;
        }
        ConfigurationSection arenas = section.getConfigurationSection("arenas");
        ConfigurationSection position = null;
        int matchingKeys = 0;
        if (arenas != null) for (String key : arenas.getKeys(false)) {
            if (!arenaId.equals(BuildFFAConfig.normalizeArenaId(key))) continue;
            if (++matchingKeys > 1) {
                fail("missing", "Ambiguous normalized arena keys for " + arenaId + ".");
                return null;
            }
            lookupPath = "king_of_the_hill.arenas." + key;
            position = arenas.getConfigurationSection(key);
        }
        if (position == null) {
            fail("missing", "Missing arena section " + lookupPath + ".");
            return null;
        }
        if (!section.isInt("radius") || !section.isInt("capture_height")
                || position.contains("radius") && !position.isInt("radius")
                || position.contains("capture_height") && !position.isInt("capture_height")) {
            fail("bounds", "Radius and capture_height must be YAML integers.");
            return null;
        }
        int r = position.getInt("radius", section.getInt("radius"));
        int h = position.getInt("capture_height", section.getInt("capture_height"));
        if (r < 1 || r > 12 || h < 2 || h > 6) {
            fail("bounds", "Radius must be 1-12 and capture_height 2-6.");
            return null;
        }
        int[] coordinates = new int[3];
        String[] axes = {"x", "y", "z"};
        for (int index = 0; index < axes.length; index++) {
            Object value = position.get(axes[index]);
            if (!(value instanceof Number)) {
                fail("missing", "Missing or nonnumeric coordinate " + lookupPath + "." + axes[index] + ".");
                return null;
            }
            double coordinate = ((Number) value).doubleValue();
            if (!Double.isFinite(coordinate) || coordinate < Integer.MIN_VALUE || coordinate > Integer.MAX_VALUE) {
                fail("area", "Coordinate is nonfinite or outside block coordinate bounds: " + axes[index] + ".");
                return null;
            }
            coordinates[index] = (int) Math.floor(coordinate);
        }
        resolved = new ResolvedHill(arena, coordinates[0], coordinates[1], coordinates[2], r, h);
        return resolved;
    }

    public String validate() {
        requireMainThread();
        ResolvedHill setup = resolve();
        if (setup == null) return failureReason;
        WorldConfig arena = setup.arena;
        int x = setup.x, y = setup.y, z = setup.z, r = setup.radius, h = setup.height;
        if (Math.abs((long) x) + r >= 29999984L || Math.abs((long) z) + r >= 29999984L
                || (long) y - 1 <= arena.deathLevel() || y < 1 || (long) y + h + 2 >= arena.world().getMaxHeight()
                || (long) y + h + 2 >= arena.moveThreshold()) return fail("area", "Block location exceeds build/world/death-level bounds.");
        Location borderCenter = arena.world().getWorldBorder().getCenter();
        double halfBorder = arena.world().getWorldBorder().getSize() / 2;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            for (int dy = 0; dy <= h + 2; dy++) {
                Location location = new Location(arena.world(), x + dx, y + dy, z + dz);
                if (location.getX() < borderCenter.getX() - halfBorder || location.getX() + 1 > borderCenter.getX() + halfBorder
                        || location.getZ() < borderCenter.getZ() - halfBorder || location.getZ() + 1 > borderCenter.getZ() + halfBorder)
                    return fail("area", "Platform or standing space crosses the world border at " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ".");
                if (game.isLobbyZone(location)) return fail("spawn", "Platform or standing space overlaps spawn protection.");
                if (dy == 0) {
                    Block block = location.getBlock();
                    if (!owns(block) && !canReplace(block))
                        return fail("unsafe_replacement", "Cannot safely snapshot and restore tile entity or inventory at " + block.getX() + "," + block.getY() + "," + block.getZ() + " (" + block.getType() + ").");
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            Block block = arena.world().getBlockAt(x + dx, y - 1, z + dz);
            if (block.getX() < borderCenter.getX() - halfBorder || block.getX() + 1 > borderCenter.getX() + halfBorder
                    || block.getZ() < borderCenter.getZ() - halfBorder || block.getZ() + 1 > borderCenter.getZ() + halfBorder)
                return fail("area", "Beacon base crosses the world border at " + block.getLocation());
            if (game.isLobbyZone(block.getLocation())) return fail("spawn", "Beacon base overlaps spawn protection at " + block.getLocation());
            if (!owns(block) && !canReplace(block)) return fail("unsafe_replacement", "Unsafe beacon base replacement at " + block.getLocation() + " (" + block.getType() + ").");
        }
        Block glass = arena.world().getBlockAt(x, y + 1, z);
        if (!owns(glass) && !canReplace(glass)) return fail("unsafe_replacement", "Unsafe beam glass replacement at " + glass.getLocation() + " (" + glass.getType() + ").");
        resolved = setup;
        return null;
    }

    private List<String> beamDiagnostics(org.bukkit.World world, int x, int y, int z) {
        List<String> lines = new ArrayList<>();
        if (Bukkit.getWorld(world.getUID()) != world || y < 0 || y >= world.getMaxHeight()
                || Math.abs((long) x) >= 29999984L || Math.abs((long) z) >= 29999984L) {
            lines.add("Cosmetic beam: cannot inspect an unloaded world or out-of-bounds beacon.");
            return lines;
        }
        lines.add("Cosmetic beam scan: " + world.getName() + "; x=" + x + ", z=" + z + ", from y=" + ((long) y + 2));
        CraftWorld craftWorld = (CraftWorld) world;
        boolean recolored = false;
        boolean blocked = false;
        for (int beamY = y + 2; beamY < world.getMaxHeight(); beamY++) {
            Block block = world.getBlockAt(x, beamY, z);
            net.minecraft.server.v1_8_R3.Block beamBlock = craftWorld.getHandle().getType(new BlockPosition(x, beamY, z)).getBlock();
            if (beamBlock == Blocks.STAINED_GLASS || beamBlock == Blocks.STAINED_GLASS_PANE) {
                if (block.getData() != 3 && !recolored) {
                    lines.add("Cosmetic beam recolored at " + world.getName() + ":" + x + "," + beamY + "," + z
                            + " (" + block.getType() + ", data=" + block.getData() + "); capture unaffected.");
                    recolored = true;
                }
            } else if (beamBlock.p() >= 15 && beamBlock != Blocks.BEDROCK) {
                lines.add("Cosmetic beam blocked at " + world.getName() + ":" + x + "," + beamY + "," + z
                        + " (" + block.getType() + "); capture unaffected.");
                blocked = true;
                break;
            }
        }
        if (!blocked && !recolored) lines.add("Cosmetic beam path: clear above the coloring glass.");
        return lines;
    }
    public List<String> diagnostics() {
        requireMainThread();
        validate();
        List<String> lines = new ArrayList<>();
        lines.add("King of the Hill config file: " + (game.getFfaConfig().getLoadedConfigurationFile() == null
                ? "<not loaded>; expected " + game.getFfaConfig().getConfigFile().getAbsolutePath()
                : game.getFfaConfig().getLoadedConfigurationFile().getAbsolutePath()));
        lines.add("Configuration snapshot loaded at: " + (game.getFfaConfig().getLoadedConfigurationAt() == 0L
                ? "<not loaded>" : new java.util.Date(game.getFfaConfig().getLoadedConfigurationAt()).toString()));
        lines.add("Root: king_of_the_hill; arena ID: " + (arenaId == null ? "<unresolved>" : arenaId));
        lines.add("Current world: " + (game.getWorldConfig() == null || game.getWorldConfig().world() == null ? "<none>" : game.getWorldConfig().world().getName()));
        lines.add("Lookup path: " + lookupPath);
        lines.add("Validation: " + (failureReason == null ? "ready" : failureReason + ": " + failureDetail));
        lines.add("Owned/restoration records: " + originals.size() + "; suspended decay records: " + suspendedDecay.size());
        if (resolved != null) {
            YamlConfiguration config = game.getFfaConfig().getLoadedConfiguration();
            lines.add("Loaded numeric coordinates: x=" + config.get(lookupPath + ".x") + ", y=" + config.get(lookupPath + ".y")
                    + ", z=" + config.get(lookupPath + ".z") + "; block conversion=floor");
            lines.add("Loaded platform/beacon block coordinates: " + resolved.arena.world().getName() + ":"
                    + resolved.x + "," + resolved.y + "," + resolved.z + "; radius=" + resolved.radius + "; capture_height=" + resolved.height);
            lines.add("Loaded base y=" + ((long) resolved.y - 1) + "; coloring glass y=" + ((long) resolved.y + 1)
                    + "; capture feet y=[" + ((long) resolved.y + 1) + "," + ((long) resolved.y + 1 + resolved.height) + ")");
        }
        if (center != null && isPlaced()) {
            lines.add("Placed platform/beacon block coordinates: " + center.getWorld().getName() + ":"
                    + center.getBlockX() + "," + center.getBlockY() + "," + center.getBlockZ());
            lines.addAll(beamDiagnostics(center.getWorld(), center.getBlockX(), center.getBlockY(), center.getBlockZ()));
        } else {
            lines.add("Placed platform/beacon: none.");
            if (resolved != null) lines.addAll(beamDiagnostics(resolved.arena.world(), resolved.x, resolved.y, resolved.z));
        }
        return lines;
    }

    public String place() {
        restore();
        if (!originals.isEmpty()) return fail("placement", "A previous platform still has blocks awaiting restoration.");
        String reason = validate();
        if (reason != null) return reason;
        ResolvedHill setup = resolved;
        radius = setup.radius;
        height = setup.height;
        center = new Location(setup.arena.world(), setup.x + 0.5D, setup.y, setup.z + 0.5D);
        try {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) continue;
                Block block = center.clone().add(dx, 0, dz).getBlock();
                replace(block, dx == 0 && dz == 0 ? Material.BEACON : Material.DIAMOND_BLOCK, (byte) 0);
            }
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                replace(center.clone().add(dx, -1, dz).getBlock(), Material.DIAMOND_BLOCK, (byte) 0);
            replace(center.clone().add(0, 1, 0).getBlock(), Material.STAINED_GLASS, (byte) 3);
            org.bukkit.World world = setup.arena.world();
            net.minecraft.server.v1_8_R3.TileEntity tile = ((CraftWorld) world).getHandle().getTileEntity(new BlockPosition(setup.x, setup.y, setup.z));
            if (!(tile instanceof TileEntityBeacon)) throw new IllegalStateException("Hill beacon tile entity was not created");
            TileEntityBeacon beacon = (TileEntityBeacon) tile;
            beacon.b(1, 0);
            beacon.b(2, 0);
            beacon.m();
            if (beacon.getProperty(1) != 0 || beacon.getProperty(2) != 0)
                throw new IllegalStateException("Hill beacon powers were not disabled");
            beacon.update();
        } catch (RuntimeException failure) {
            restore();
            fail("placement", failure.getClass().getSimpleName() + ": " + failure.getMessage());
            throw failure;
        }
        return null;
    }

    private static final class ResolvedHill {
        final WorldConfig arena;
        final int x, y, z, radius, height;
        ResolvedHill(WorldConfig arena, int x, int y, int z, int radius, int height) {
            this.arena = arena;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.height = height;
        }
    }

    public boolean contains(Player player) {
        if (center == null || !player.getWorld().equals(center.getWorld()) || player.isFlying() || !player.isOnGround()) return false;
        Location feet = player.getLocation();
        double dx = feet.getX() - center.getX(), dz = feet.getZ() - center.getZ();
        return feet.getY() >= center.getY() + 1 && feet.getY() < center.getY() + 1 + height
                && dx * dx + dz * dz <= radius * radius;
    }

    public boolean isPlaced() { return center != null && !originals.isEmpty(); }

    public boolean owns(Block block) {
        InstalledBlock expected = installed.get(block);
        return originals.containsKey(block) && expected != null && block.getType() == expected.material && block.getData() == expected.data;
    }

    private void replace(Block block, Material material, byte data) {
        if (!canReplace(block)) throw new IllegalStateException("Unsafe replacement at " + block.getLocation());
        BlockState original = block.getState();
        BlockTracker.SuspendedBlock suspended = game.getBlockTracker().suspend(block);
        if (suspended != null) suspendedDecay.put(block, suspended);
        originals.put(block, original);
        installed.put(block, new InstalledBlock(material, data));
        block.setTypeIdAndData(material.getId(), data, false);
        if (!owns(block)) throw new IllegalStateException("Hill block placement failed at " + block.getLocation());
    }

    private static final class InstalledBlock {
        final Material material;
        final byte data;
        InstalledBlock(Material material, byte data) {
            this.material = material;
            this.data = data;
        }
    }

    private boolean canReplace(Block block) {
        return !(block.getState() instanceof InventoryHolder)
                && ((CraftWorld) block.getWorld()).getHandle().getTileEntity(new BlockPosition(block.getX(), block.getY(), block.getZ())) == null;
    }

    public void restore() {
        requireMainThread();
        for (Map.Entry<Block, BlockState> entry : new ArrayList<>(originals.entrySet())) {
            Block block = entry.getKey();
            BlockState original = entry.getValue();
            if (Bukkit.getWorld(block.getWorld().getUID()) != block.getWorld()) {
                game.getPlugin().getLogger().severe("Cannot restore hill block in unloaded world at " + block.getWorld().getName()
                        + ":" + block.getX() + "," + block.getY() + "," + block.getZ());
                continue;
            }
            boolean unchanged = block.getType() == original.getType() && block.getData() == original.getRawData();
            if (owns(block)) {
                try {
                    if (!original.update(true, false) || block.getType() != original.getType() || block.getData() != original.getRawData()) {
                        game.getPlugin().getLogger().severe("Hill block restoration failed at " + block.getLocation());
                        continue;
                    }
                } catch (RuntimeException failure) {
                    game.getPlugin().getLogger().log(java.util.logging.Level.SEVERE, "Hill block restoration failed at " + block.getLocation(), failure);
                    continue;
                }
                game.getBlockTracker().resume(block, suspendedDecay.remove(block));
            } else if (unchanged) {
                game.getBlockTracker().resume(block, suspendedDecay.remove(block));
            } else {
                suspendedDecay.remove(block);
                game.getPlugin().getLogger().warning("Hill restoration left unrelated edit unchanged at " + block.getLocation());
            }
            originals.remove(block);
            installed.remove(block);
        }
        center = null;
    }

    private void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Hill world operations must run on the server thread");
    }

    private boolean isBeaconInventory(org.bukkit.inventory.Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof Beacon && owns(((Beacon) holder).getBlock());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interactBeacon(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block != null && block.getType() == Material.BEACON && owns(block)) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void openBeacon(InventoryOpenEvent event) {
        if (isBeaconInventory(event.getInventory())) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void moveBeaconItems(InventoryMoveItemEvent event) {
        if (isBeaconInventory(event.getSource()) || isBeaconInventory(event.getDestination())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) { if (owns(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void placeBlock(BlockPlaceEvent event) { if (originals.containsKey(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explode(EntityExplodeEvent event) { event.blockList().removeIf(this::owns); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explodeBlock(BlockExplodeEvent event) { event.blockList().removeIf(this::owns); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void extend(BlockPistonExtendEvent event) {
        if (owns(event.getBlock()) || event.getBlocks().stream().anyMatch(b -> owns(b) || owns(b.getRelative(event.getDirection())))) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void retract(BlockPistonRetractEvent event) {
        if (owns(event.getBlock()) || event.getBlocks().stream().anyMatch(b -> owns(b) || owns(b.getRelative(event.getDirection().getOppositeFace())))) event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void fade(BlockFadeEvent event) { if (owns(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void decay(LeavesDecayEvent event) { if (owns(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void burn(BlockBurnEvent event) { if (owns(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void entityChange(EntityChangeBlockEvent event) { if (owns(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void form(BlockFormEvent event) { if (owns(event.getBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void flow(BlockFromToEvent event) { if (owns(event.getToBlock())) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void unload(ChunkUnloadEvent event) {
        if (originals.keySet().stream().anyMatch(b -> b.getWorld().equals(event.getWorld()) && (b.getX() >> 4) == event.getChunk().getX()
                && (b.getZ() >> 4) == event.getChunk().getZ())) event.setCancelled(true);
    }
}
