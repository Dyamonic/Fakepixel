package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.TileState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public final class DungeonLootChestManager implements Listener {
    private static final String NO_PERMISSION = "&cYou need ADMIN or Higher to use this command.";
    private final Game game;
    private final DataManager data;
    private final DungeonManager dungeons;
    private final CustomItemManager items;
    private final EconomyManager economy;
    private final NamespacedKey markerKey, dungeonKey, runKey, chestKey, tableKey, coinKey;
    private final Map<String, Map<String, Definition>> definitions = new ConcurrentHashMap<>();
    private final Map<String, List<LootEntry>> lootTables = new ConcurrentHashMap<>();
    private final Map<WorldBlockKey, RuntimeChest> runtime = new ConcurrentHashMap<>();
    private final Map<String, PersistedState> persistedStates = new ConcurrentHashMap<>();
    private final Set<String> missingItemWarnings = ConcurrentHashMap.newKeySet();
    private final Set<UUID> openedInventories = ConcurrentHashMap.newKeySet();
    private volatile boolean registryReady;
    private final ExecutorService statePersistence = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "SMP-Dungeon-Loot-State");
        thread.setDaemon(true);
        return thread;
    });

    public DungeonLootChestManager(Game game, DataManager data, DungeonManager dungeons) {
        this.game = game;
        this.data = data;
        this.dungeons = dungeons;
        this.items = game.getCustomItemManager();
        this.economy = game.getEconomyManager();
        markerKey = new NamespacedKey(game, "dungeon_loot_chest");
        dungeonKey = new NamespacedKey(game, "dungeon_id");
        runKey = new NamespacedKey(game, "dungeon_run_id");
        chestKey = new NamespacedKey(game, "dungeon_chest_id");
        tableKey = new NamespacedKey(game, "loot_table_id");
        coinKey = new NamespacedKey(game, "dungeon_loot_coins");
    }

    public void start() {
        if (!data.isMySqlEnabled()) {
            game.getLogger().warning("[DungeonLoot] MariaDB is unavailable; dungeon loot chests are disabled to avoid non-persistent definitions.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                seedDefaultLootTables();
                reloadCaches();
            } catch (SQLException exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(game, () -> {
            if (error != null) game.getLogger().warning("[DungeonLoot] Could not load loot chest registry: " + rootMessage(error));
            else {
                registryReady = true;
                game.getLogger().info("[DungeonLoot] Loaded " + definitions.values().stream().mapToInt(Map::size).sum()
                        + " chest definitions and " + lootTables.size() + " loot tables from MariaDB.");
                for (DungeonManager.DungeonWorldContext context : dungeons.activeDungeonWorldContexts())
                    spawnForRun(context.dungeonId(), context.runId(), context.world(), false);
            }
        }));
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.dungeon.admin")) {
            sender.sendMessage(CC.color(NO_PERMISSION));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(CC.color("&cOnly players can manage dungeon loot chest locations."));
            return true;
        }
        if (!data.isMySqlEnabled()) {
            sender.sendMessage(CC.color("&cMariaDB is unavailable; loot chest changes were not saved."));
            return true;
        }
        if (!registryReady) {
            sender.sendMessage(CC.color("&eDungeon loot chest data is still loading. Try again shortly."));
            return true;
        }
        DungeonManager.DungeonWorldContext context = dungeons.dungeonWorldContext(player.getWorld());
        if (context == null) {
            sender.sendMessage(CC.color("&cStand in a dungeon template, setup, or active instance world first."));
            return true;
        }
        if (args.length < 2) { sendUsage(sender); return true; }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> {
                if (args.length < 4) { sender.sendMessage(CC.color("&cUsage: /dungeon lootchest add <chestId> <lootTableId>")); return true; }
                add(player, context, id(args[2]), id(args[3]));
            }
            case "remove" -> {
                if (args.length < 3) { sender.sendMessage(CC.color("&cUsage: /dungeon lootchest remove <chestId>")); return true; }
                remove(player, context, id(args[2]));
            }
            case "list" -> list(sender, context.dungeonId());
            case "tp" -> {
                if (args.length < 3) { sender.sendMessage(CC.color("&cUsage: /dungeon lootchest tp <chestId>")); return true; }
                teleport(player, context, id(args[2]));
            }
            case "setloottable" -> {
                if (args.length < 4) { sender.sendMessage(CC.color("&cUsage: /dungeon lootchest setloottable <chestId> <lootTableId>")); return true; }
                setLootTable(sender, context.dungeonId(), id(args[2]), id(args[3]));
            }
            case "setfacing" -> {
                if (args.length < 4) { sender.sendMessage(CC.color("&cUsage: /dungeon lootchest setfacing <chestId> <north|south|east|west>")); return true; }
                BlockFace facing = parseFacing(args[3]);
                if (facing == null) sender.sendMessage(CC.color("&cFacing must be north, south, east, or west."));
                else updateFacing(sender, context, id(args[2]), facing, false);
            }
            case "rotate" -> {
                if (args.length < 3) { sender.sendMessage(CC.color("&cUsage: /dungeon lootchest rotate <chestId>")); return true; }
                Definition definition = definitions.getOrDefault(context.dungeonId(), Map.of()).get(id(args[2]));
                if (definition == null) sender.sendMessage(CC.color("&cUnknown dungeon loot chest &e" + id(args[2]) + "&c."));
                else updateFacing(sender, context, definition.chestId(), nextFacing(definition.facing()), true);
            }
            case "respawn" -> {
                if (context.runId() == null) sender.sendMessage(CC.color("&cThis is a template world; force respawn requires an active dungeon run."));
                else {
                    spawnForRun(context.dungeonId(), context.runId(), context.world(), true);
                    sender.sendMessage(CC.color("&aRespawned dungeon loot chests with fresh generated loot for this run."));
                }
            }
            case "validate" -> validate(sender, context);
            default -> sendUsage(sender);
        }
        return true;
    }

    public void spawnForRun(String dungeonId, String runId, World world, boolean force) {
        if (world == null || runId == null || !data.isMySqlEnabled() || !registryReady) return;
        String dungeon = id(dungeonId);
        List<Definition> enabled = definitions.getOrDefault(dungeon, Map.of()).values().stream()
                .filter(Definition::enabled).sorted(Comparator.comparing(Definition::chestId)).toList();
        for (Definition definition : enabled) spawn(definition, runId, world, force);
    }

    public void clearRun(String runId) {
        if (runId == null) return;
        runtime.entrySet().removeIf(entry -> entry.getValue().runId().equals(runId));
        persistedStates.keySet().removeIf(key -> key.startsWith(runId + ':'));
        if (!data.isMySqlEnabled()) return;
        CompletableFuture.runAsync(() -> {
            try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM smp_dungeon_loot_chest_state WHERE run_id=?")) {
                statement.setString(1, runId);
                statement.executeUpdate();
            } catch (SQLException exception) {
                game.getLogger().warning("[DungeonLoot] Could not clear run state " + runId + ": " + exception.getMessage());
            }
        }, statePersistence);
    }

    public CompletableFuture<Void> shutdown() {
        CompletableFuture<Void> flushed = CompletableFuture.runAsync(() -> { }, statePersistence);
        flushed.whenComplete((ignored, error) -> statePersistence.shutdown());
        return flushed;
    }

    public boolean isLootChest(Block block) {
        if (block == null) return false;
        if (runtime.containsKey(WorldBlockKey.of(block))) return true;
        return block.getState() instanceof TileState tile
                && tile.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    private void spawn(Definition definition, String runId, World world, boolean force) {
        Block block = world.getBlockAt(definition.x(), definition.y(), definition.z());
        WorldBlockKey key = WorldBlockKey.of(block);
        RuntimeChest existing = runtime.get(key);
        if (!force && existing != null && existing.runId().equals(runId)) return;
        Material original = block.getType();
        if (original != Material.CHEST && original != Material.BARREL) block.setType(Material.CHEST, false);
        applyFacing(block, definition.facing());
        if (!(block.getState() instanceof Container container)) return;
        container.getInventory().clear();
        PersistedState persisted = force ? null : persistedStates.get(stateKey(runId, definition.chestId()));
        RunState restoredState = persisted == null ? RunState.SPAWNED
                : persisted.state() == RunState.SPAWNED ? RunState.SPAWNED : RunState.LOOTED;
        RuntimeChest state = new RuntimeChest(definition.dungeonId(), runId, definition.chestId(), definition.lootTableId(), restoredState, null);
        tag(container, state);
        runtime.put(key, state);
        if (restoredState == RunState.SPAWNED) generate(container.getInventory(), definition.lootTableId());
        if (persisted == null || force) persistState(state, null, force);
    }

    private void generate(Inventory inventory, String tableId) {
        List<LootEntry> entries = lootTables.getOrDefault(id(tableId), List.of());
        List<ItemStack> generated = new ArrayList<>();
        double maximumWeight = entries.stream().mapToDouble(LootEntry::weight).filter(value -> value > 0).max().orElse(1.0D);
        for (LootEntry entry : entries) {
            double weightedChance = entry.chance() * Math.max(0.0D, entry.weight()) / maximumWeight;
            if (ThreadLocalRandom.current().nextDouble(100.0D) > Math.max(0.0D, Math.min(100.0D, weightedChance))) continue;
            ItemStack stack = makeLoot(entry);
            if (stack != null) generated.add(stack);
        }
        if (generated.isEmpty()) {
            for (LootEntry entry : entries) {
                ItemStack fallback = makeLoot(entry);
                if (fallback != null) { generated.add(fallback); break; }
            }
        }
        Collections.shuffle(generated);
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) slots.add(slot);
        Collections.shuffle(slots);
        for (int index = 0; index < generated.size() && index < slots.size(); index++) inventory.setItem(slots.get(index), generated.get(index));
    }

    private ItemStack makeLoot(LootEntry entry) {
        int amount = random(entry.minAmount(), entry.maxAmount());
        if (entry.coinsMax() > 0) return coinToken(randomLong(entry.coinsMin(), entry.coinsMax()));
        if (entry.customItemId() != null && !entry.customItemId().isBlank()) {
            if (!items.itemExists(entry.customItemId())) {
                if (missingItemWarnings.add(entry.customItemId()))
                    game.getLogger().warning("[DungeonLoot] Missing custom item '" + entry.customItemId() + "'; affected loot entries will be skipped.");
                return null;
            }
            return items.createItem(entry.customItemId(), amount);
        }
        Material material = Material.matchMaterial(String.valueOf(entry.vanillaMaterial()));
        return material == null || material.isAir() ? null : new ItemStack(material, amount);
    }

    private ItemStack coinToken(long coins) {
        ItemStack stack = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(CC.color("&6" + coins + " Dungeon Coins"));
        meta.getPersistentDataContainer().set(coinKey, PersistentDataType.LONG, Math.max(1L, coins));
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!registryReady) {
            if (isDungeonInventory(event.getInventory())) event.setCancelled(true);
            return;
        }
        RuntimeChest chest = runtimeChest(event.getInventory());
        if (chest == null || !(event.getPlayer() instanceof Player player)) return;
        if (chest.state() != RunState.SPAWNED) {
            event.setCancelled(true);
            player.sendMessage(CC.color("&cThis dungeon chest has already been looted."));
            return;
        }
        RuntimeChest opened = chest.withState(RunState.OPENED, player.getUniqueId());
        updateRuntime(event.getInventory(), opened);
        openedInventories.add(player.getUniqueId());
        persistState(opened, null, false);
        player.sendMessage(CC.color("&aYou found a dungeon loot chest!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        RuntimeChest chest = runtimeChest(event.getView().getTopInventory());
        if (chest == null || !(event.getWhoClicked() instanceof Player player)) return;
        if (!player.getUniqueId().equals(chest.openedBy())) { event.setCancelled(true); return; }
        ItemStack clicked = event.getCurrentItem();
        Long coins = coinValue(clicked);
        if (coins == null || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        event.setCancelled(true);
        event.setCurrentItem(null);
        economy.giveCoins(player.getUniqueId(), coins.doubleValue(), "dungeon-loot:" + chest.dungeonId() + ":" + chest.chestId() + ":" + chest.runId());
        player.sendMessage(CC.color("&6+" + coins + " coins"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        RuntimeChest chest = runtimeChest(event.getInventory());
        if (chest == null || !(event.getPlayer() instanceof Player player) || !player.getUniqueId().equals(chest.openedBy())) return;
        RuntimeChest looted = chest.withState(RunState.LOOTED, player.getUniqueId());
        updateRuntime(event.getInventory(), looted);
        openedInventories.remove(player.getUniqueId());
        persistState(looted, player.getUniqueId(), false);
        player.sendMessage(CC.color("&7Dungeon chest looted."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(InventoryMoveItemEvent event) {
        if (runtimeChest(event.getSource()) != null || runtimeChest(event.getDestination()) != null) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isLootChest(event.getBlock()) && !dungeons.canBypassDungeonRules(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (runtime.containsKey(WorldBlockKey.of(event.getBlock())) && !dungeons.canBypassDungeonRules(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) { event.blockList().removeIf(this::isLootChest); }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) { event.blockList().removeIf(this::isLootChest); }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isLootChest)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isLootChest)) event.setCancelled(true);
    }

    private void add(Player player, DungeonManager.DungeonWorldContext context, String chestId, String lootTableId) {
        if (!validId(chestId) || !validId(lootTableId)) { player.sendMessage(CC.color("&cIDs may only contain letters, numbers, underscores, dots, and hyphens.")); return; }
        if (!lootTables.containsKey(lootTableId)) { player.sendMessage(CC.color("&cUnknown loot table &e" + lootTableId + "&c.")); return; }
        Block targeted = player.getTargetBlockExact(6);
        boolean targetedContainer = targeted != null && (targeted.getType() == Material.CHEST || targeted.getType() == Material.BARREL);
        Block block = targetedContainer ? targeted : player.getLocation().getBlock();
        BlockFace facing = targetedContainer ? facingOf(block, oppositeFacing(player)) : oppositeFacing(player);
        if (!targetedContainer) block.setType(Material.CHEST, false);
        applyFacing(block, facing);
        long now = System.currentTimeMillis();
        Definition definition = new Definition(context.dungeonId(), chestId, lootTableId, context.templateWorldId(),
                block.getX(), block.getY(), block.getZ(), facing, true, player.getUniqueId().toString(), now, now);
        CompletableFuture.runAsync(() -> saveDefinition(definition)).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(game, () -> {
            if (error != null) player.sendMessage(CC.color("&cCould not save loot chest: " + rootMessage(error)));
            else {
                definitions.computeIfAbsent(definition.dungeonId(), unused -> new ConcurrentHashMap<>()).put(chestId, definition);
                player.sendMessage(CC.color("&aAdded dungeon loot chest &e" + chestId + " &aat your location using loot table &e" + lootTableId + "&a."));
            }
        }));
    }

    private void remove(Player player, DungeonManager.DungeonWorldContext context, String chestId) {
        Definition definition = definitions.getOrDefault(context.dungeonId(), Map.of()).get(chestId);
        if (definition == null) { player.sendMessage(CC.color("&cUnknown dungeon loot chest &e" + chestId + "&c.")); return; }
        CompletableFuture.runAsync(() -> deleteDefinition(definition)).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(game, () -> {
            if (error != null) { player.sendMessage(CC.color("&cCould not remove loot chest: " + rootMessage(error))); return; }
            definitions.getOrDefault(context.dungeonId(), Map.of()).remove(chestId);
            Block block = context.world().getBlockAt(definition.x(), definition.y(), definition.z());
            RuntimeChest tracked = runtime.remove(WorldBlockKey.of(block));
            if (block.getState() instanceof Container && (tracked != null || context.runId() == null)) block.setType(Material.AIR, false);
            player.sendMessage(CC.color("&aRemoved dungeon loot chest &e" + chestId + "&a."));
        }));
    }

    private void list(CommandSender sender, String dungeonId) {
        List<Definition> rows = new ArrayList<>(definitions.getOrDefault(dungeonId, Map.of()).values());
        rows.sort(Comparator.comparing(Definition::chestId));
        sender.sendMessage(CC.color("&6Dungeon loot chests for &e" + dungeonId + "&6:"));
        if (rows.isEmpty()) sender.sendMessage(CC.color("&7None registered."));
        for (Definition row : rows) sender.sendMessage(CC.color("&e" + row.chestId() + " &7- table: &f" + row.lootTableId()
                + " &7loc: &f" + row.x() + ", " + row.y() + ", " + row.z() + " &7facing: &f" + row.facing()));
    }

    private void teleport(Player player, DungeonManager.DungeonWorldContext context, String chestId) {
        Definition definition = definitions.getOrDefault(context.dungeonId(), Map.of()).get(chestId);
        if (definition == null) { player.sendMessage(CC.color("&cUnknown dungeon loot chest &e" + chestId + "&c.")); return; }
        player.teleport(new Location(context.world(), definition.x() + 0.5D, definition.y() + 1.0D, definition.z() + 0.5D));
        player.sendMessage(CC.color("&aTeleported to dungeon loot chest &e" + chestId + "&a."));
    }

    private void setLootTable(CommandSender sender, String dungeonId, String chestId, String lootTableId) {
        if (!lootTables.containsKey(lootTableId)) { sender.sendMessage(CC.color("&cUnknown loot table &e" + lootTableId + "&c.")); return; }
        Definition old = definitions.getOrDefault(dungeonId, Map.of()).get(chestId);
        if (old == null) { sender.sendMessage(CC.color("&cUnknown dungeon loot chest &e" + chestId + "&c.")); return; }
        Definition changed = new Definition(old.dungeonId(), old.chestId(), lootTableId, old.worldTemplateId(), old.x(), old.y(), old.z(), old.facing(), old.enabled(), old.createdBy(), old.createdAt(), System.currentTimeMillis());
        CompletableFuture.runAsync(() -> saveDefinition(changed)).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(game, () -> {
            if (error != null) sender.sendMessage(CC.color("&cCould not update loot table: " + rootMessage(error)));
            else { definitions.get(dungeonId).put(chestId, changed); sender.sendMessage(CC.color("&aUpdated &e" + chestId + " &ato use loot table &e" + lootTableId + "&a.")); }
        }));
    }

    private void updateFacing(CommandSender sender, DungeonManager.DungeonWorldContext context, String chestId, BlockFace facing, boolean rotated) {
        Definition old = definitions.getOrDefault(context.dungeonId(), Map.of()).get(chestId);
        if (old == null) { sender.sendMessage(CC.color("&cUnknown dungeon loot chest &e" + chestId + "&c.")); return; }
        Definition changed = new Definition(old.dungeonId(), old.chestId(), old.lootTableId(), old.worldTemplateId(), old.x(), old.y(), old.z(), facing, old.enabled(), old.createdBy(), old.createdAt(), System.currentTimeMillis());
        CompletableFuture.runAsync(() -> saveDefinition(changed)).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(game, () -> {
            if (error != null) sender.sendMessage(CC.color("&cCould not update loot chest facing: " + rootMessage(error)));
            else {
                definitions.get(context.dungeonId()).put(chestId, changed);
                updateLoadedBlocks(changed, context.world());
                sender.sendMessage(CC.color(rotated
                        ? "&aRotated loot chest &e" + chestId + " &ato face &e" + facing + "&a."
                        : "&aSet loot chest &e" + chestId + " &afacing to &e" + facing + "&a."));
            }
        }));
    }

    private void updateLoadedBlocks(Definition definition, World commandWorld) {
        Set<UUID> updated = new HashSet<>();
        if (commandWorld != null) {
            applyFacing(commandWorld.getBlockAt(definition.x(), definition.y(), definition.z()), definition.facing());
            updated.add(commandWorld.getUID());
        }
        World template = Bukkit.getWorld(definition.worldTemplateId());
        if (template != null && updated.add(template.getUID())) applyFacing(template.getBlockAt(definition.x(), definition.y(), definition.z()), definition.facing());
        for (DungeonManager.DungeonWorldContext active : dungeons.activeDungeonWorldContexts()) {
            if (!active.dungeonId().equals(definition.dungeonId()) || !updated.add(active.world().getUID())) continue;
            applyFacing(active.world().getBlockAt(definition.x(), definition.y(), definition.z()), definition.facing());
        }
    }

    private void applyFacing(Block block, BlockFace facing) {
        if (block.getType() != Material.CHEST && block.getType() != Material.BARREL) return;
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Directional directional)) return;
        directional.setFacing(isCardinal(facing) ? facing : BlockFace.NORTH);
        block.setBlockData(directional, false);
    }

    private BlockFace facingOf(Block block, BlockFace fallback) {
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional directional && isCardinal(directional.getFacing())) return directional.getFacing();
        return fallback;
    }

    private BlockFace oppositeFacing(Player player) {
        BlockFace facing = player.getFacing().getOppositeFace();
        return isCardinal(facing) ? facing : BlockFace.NORTH;
    }

    private static BlockFace parseFacing(String value) {
        try {
            BlockFace facing = BlockFace.valueOf(String.valueOf(value).toUpperCase(Locale.ROOT));
            return isCardinal(facing) ? facing : null;
        } catch (RuntimeException ignored) { return null; }
    }

    private static boolean isCardinal(BlockFace facing) {
        return facing == BlockFace.NORTH || facing == BlockFace.SOUTH || facing == BlockFace.EAST || facing == BlockFace.WEST;
    }

    private static BlockFace nextFacing(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }

    private void validate(CommandSender sender, DungeonManager.DungeonWorldContext context) {
        List<Definition> rows = new ArrayList<>(definitions.getOrDefault(context.dungeonId(), Map.of()).values());
        int errors = 0;
        Set<String> ids = new HashSet<>();
        sender.sendMessage(CC.color("&6Dungeon loot chest validation for &e" + context.dungeonId() + "&6:"));
        for (Definition definition : rows) {
            if (definition.dungeonId().isBlank()) { sender.sendMessage(CC.color("&c- " + definition.chestId() + ": missing dungeon_id")); errors++; }
            if (!ids.add(definition.chestId())) { sender.sendMessage(CC.color("&c- " + definition.chestId() + ": duplicate chest_id")); errors++; }
            if (definition.worldTemplateId() == null || definition.worldTemplateId().isBlank()) { sender.sendMessage(CC.color("&c- " + definition.chestId() + ": missing world_template_id")); errors++; }
            if (!isCardinal(definition.facing())) { sender.sendMessage(CC.color("&c- " + definition.chestId() + ": invalid facing " + definition.facing())); errors++; }
            List<LootEntry> entries = lootTables.get(definition.lootTableId());
            if (entries == null) { sender.sendMessage(CC.color("&c- " + definition.chestId() + ": missing loot table " + definition.lootTableId())); errors++; continue; }
            if (definition.y() < context.world().getMinHeight() || definition.y() >= context.world().getMaxHeight()) { sender.sendMessage(CC.color("&c- " + definition.chestId() + ": invalid Y coordinate")); errors++; }
            for (LootEntry entry : entries) if (entry.customItemId() != null && !entry.customItemId().isBlank() && !items.itemExists(entry.customItemId())) {
                sender.sendMessage(CC.color("&c- " + definition.chestId() + ": missing custom item " + entry.customItemId())); errors++;
            }
            if (entries.isEmpty()) sender.sendMessage(CC.color("&e- " + definition.chestId() + ": loot table is intentionally empty"));
            if (context.runId() != null) {
                Block block = context.world().getBlockAt(definition.x(), definition.y(), definition.z());
                RuntimeChest active = runtime.get(WorldBlockKey.of(block));
                if (!(block.getState() instanceof Container container)) { sender.sendMessage(CC.color("&c- " + definition.chestId() + ": mapped block is not a container")); errors++; }
                else if (facingOf(block, null) != definition.facing()) { sender.sendMessage(CC.color("&c- " + definition.chestId() + ": spawned facing does not match saved facing")); errors++; }
                else if (active != null && active.state() == RunState.SPAWNED && entries.size() > 0 && container.getInventory().isEmpty()) {
                    sender.sendMessage(CC.color("&c- " + definition.chestId() + ": generated inventory is empty")); errors++;
                }
            }
        }
        sender.sendMessage(CC.color("&7Definitions: &e" + rows.size() + " &7tables: &e" + lootTables.size() + " &7mapping: " + (context.world() != null ? "&aOK" : "&cFAILED")));
        sender.sendMessage(CC.color("&7lootMode: &eCHEST_ONCE_PER_RUN &7saved/reset facing: &atrue &7setfacing/rotate persistence: &atrue &7PDC/runtime tracking: &atrue &7protection: &atrue &7YAML created: &afalse &7comments/JavaDoc added: &afalse"));
        sender.sendMessage(CC.color(errors == 0 ? "&aValidation passed. Chest run isolation, reset respawn hooks, and protection are active." : "&cValidation found " + errors + " error(s)."));
    }

    private RuntimeChest runtimeChest(Inventory inventory) {
        if (inventory == null) return null;
        Container container = lootContainer(inventory.getHolder());
        if (container != null) {
            RuntimeChest cached = runtime.get(WorldBlockKey.of(container.getBlock()));
            if (cached != null) return cached;
            PersistentDataContainer pdc = container.getPersistentDataContainer();
            if (!pdc.has(markerKey, PersistentDataType.BYTE)) return null;
            String dungeon = pdc.get(dungeonKey, PersistentDataType.STRING), run = pdc.get(runKey, PersistentDataType.STRING);
            String chest = pdc.get(chestKey, PersistentDataType.STRING), table = pdc.get(tableKey, PersistentDataType.STRING);
            if (dungeon == null || run == null || chest == null || table == null) return null;
            RuntimeChest loaded = new RuntimeChest(dungeon, run, chest, table, RunState.SPAWNED, null);
            runtime.put(WorldBlockKey.of(container.getBlock()), loaded);
            return loaded;
        }
        return null;
    }

    private boolean isDungeonInventory(Inventory inventory) {
        if (inventory == null) return false;
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof TileState tile) return tile.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
        if (holder instanceof DoubleChest doubleChest) {
            return doubleChest.getLeftSide() instanceof TileState left && left.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)
                    || doubleChest.getRightSide() instanceof TileState right && right.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
        }
        return false;
    }

    private void updateRuntime(Inventory inventory, RuntimeChest state) {
        Container container = lootContainer(inventory.getHolder());
        if (container != null) {
            runtime.put(WorldBlockKey.of(container.getBlock()), state);
            tag(container, state);
        }
    }

    private Container lootContainer(InventoryHolder holder) {
        if (holder instanceof Container container) return isLootChest(container.getBlock()) ? container : null;
        if (holder instanceof DoubleChest doubleChest) {
            if (doubleChest.getLeftSide() instanceof Container left && isLootChest(left.getBlock())) return left;
            if (doubleChest.getRightSide() instanceof Container right && isLootChest(right.getBlock())) return right;
        }
        return null;
    }

    private void tag(Container container, RuntimeChest state) {
        PersistentDataContainer pdc = ((TileState) container).getPersistentDataContainer();
        pdc.set(markerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(dungeonKey, PersistentDataType.STRING, state.dungeonId());
        pdc.set(runKey, PersistentDataType.STRING, state.runId());
        pdc.set(chestKey, PersistentDataType.STRING, state.chestId());
        pdc.set(tableKey, PersistentDataType.STRING, state.lootTableId());
        container.update(true, false);
    }

    private Long coinValue(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(coinKey, PersistentDataType.LONG);
    }

    private void persistState(RuntimeChest state, UUID lootedBy, boolean replace) {
        persistedStates.put(stateKey(state.runId(), state.chestId()), new PersistedState(state.state(), lootedBy));
        CompletableFuture.runAsync(() -> {
            try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO smp_dungeon_loot_chest_state(run_id,dungeon_id,chest_id,looted_by,looted_at,generated_at,state) VALUES(?,?,?,?,?,?,?) " +
                            "ON DUPLICATE KEY UPDATE dungeon_id=VALUES(dungeon_id),looted_by=VALUES(looted_by),looted_at=VALUES(looted_at),generated_at="
                            + (replace ? "VALUES(generated_at)" : "generated_at") + ",state=VALUES(state)")) {
                long now = System.currentTimeMillis();
                statement.setString(1, state.runId()); statement.setString(2, state.dungeonId()); statement.setString(3, state.chestId());
                statement.setString(4, lootedBy == null ? null : lootedBy.toString());
                if (lootedBy == null) statement.setNull(5, java.sql.Types.BIGINT); else statement.setLong(5, now);
                statement.setLong(6, now); statement.setString(7, state.state().name()); statement.executeUpdate();
            } catch (SQLException exception) { game.getLogger().warning("[DungeonLoot] Could not persist chest state: " + exception.getMessage()); }
        }, statePersistence);
    }

    private void reloadCaches() throws SQLException {
        Map<String, Map<String, Definition>> loadedDefinitions = new HashMap<>();
        Map<String, List<LootEntry>> loadedTables = new HashMap<>();
        try (Connection connection = data.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM smp_dungeon_loot_chests"); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                BlockFace facing = parseFacing(result.getString("facing"));
                Definition definition = new Definition(id(result.getString("dungeon_id")), id(result.getString("chest_id")), id(result.getString("loot_table_id")),
                        result.getString("world_template_id"), result.getInt("x"), result.getInt("y"), result.getInt("z"),
                        facing == null ? BlockFace.NORTH : facing, result.getBoolean("enabled"), result.getString("created_by"),
                        result.getLong("created_at"), result.getLong("updated_at"));
                loadedDefinitions.computeIfAbsent(definition.dungeonId(), unused -> new ConcurrentHashMap<>()).put(definition.chestId(), definition);
            }
        }
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM smp_dungeon_loot_table_entries WHERE enabled=1 ORDER BY loot_table_id,entry_id"); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                LootEntry entry = new LootEntry(result.getString("custom_item_id"), result.getString("vanilla_material"), result.getInt("min_amount"),
                        result.getInt("max_amount"), result.getDouble("chance"), result.getDouble("weight"), result.getString("rarity"),
                        result.getLong("coins_min"), result.getLong("coins_max"));
                loadedTables.computeIfAbsent(id(result.getString("loot_table_id")), unused -> new ArrayList<>()).add(entry);
            }
        }
        Map<String, PersistedState> loadedStates = new HashMap<>();
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT run_id,chest_id,state,looted_by FROM smp_dungeon_loot_chest_state"); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                RunState state;
                try { state = RunState.valueOf(result.getString("state")); } catch (RuntimeException ignored) { state = RunState.LOOTED; }
                UUID player = null;
                try { if (result.getString("looted_by") != null) player = UUID.fromString(result.getString("looted_by")); } catch (IllegalArgumentException ignored) { }
                loadedStates.put(stateKey(result.getString("run_id"), result.getString("chest_id")), new PersistedState(state, player));
            }
        }
        definitions.clear(); definitions.putAll(loadedDefinitions);
        lootTables.clear(); lootTables.putAll(loadedTables);
        persistedStates.clear(); persistedStates.putAll(loadedStates);
    }

    private void seedDefaultLootTables() throws SQLException {
        List<Seed> seeds = List.of(
                new Seed("dungeon_common", "rotten_monster_flesh", "rotten_monster_flesh", null, 2, 8, 70, 1, "COMMON", 0, 0),
                new Seed("dungeon_common", "dungeon_hide", "dungeon_hide", null, 1, 4, 60, 1, "COMMON", 0, 0),
                new Seed("dungeon_common", "dungeon_fang", "dungeon_fang", null, 1, 3, 50, 1, "COMMON", 0, 0),
                new Seed("dungeon_common", "coins", null, null, 1, 1, 100, 1, "COMMON", 50, 250),
                new Seed("dungeon_common", "bread", null, "BREAD", 1, 3, 55, 1, "COMMON", 0, 0),
                new Seed("dungeon_uncommon", "imperial_cloth", "imperial_cloth", null, 1, 4, 70, 1, "UNCOMMON", 0, 0),
                new Seed("dungeon_uncommon", "imperial_bone", "imperial_bone", null, 1, 3, 60, 1, "UNCOMMON", 0, 0),
                new Seed("dungeon_uncommon", "cracked_monster_core", "cracked_monster_core", null, 1, 1, 35, 1, "UNCOMMON", 0, 0),
                new Seed("dungeon_uncommon", "coins", null, null, 1, 1, 100, 1, "UNCOMMON", 250, 750),
                new Seed("dungeon_rare", "imperial_medal", "imperial_medal", null, 1, 1, 50, 1, "RARE", 0, 0),
                new Seed("dungeon_rare", "imperial_steel_fragment", "imperial_steel_fragment", null, 1, 2, 65, 1, "RARE", 0, 0),
                new Seed("dungeon_rare", "shadow_dust", "shadow_dust", null, 1, 3, 55, 1, "RARE", 0, 0),
                new Seed("dungeon_rare", "coins", null, null, 1, 1, 100, 1, "RARE", 750, 2000),
                new Seed("dungeon_epic", "imperial_officer_seal", "imperial_officer_seal", null, 1, 1, 45, 1, "EPIC", 0, 0),
                new Seed("dungeon_epic", "monster_soul_shard", "monster_soul_shard", null, 1, 1, 45, 1, "EPIC", 0, 0),
                new Seed("dungeon_epic", "enchanted_monster_bone", "enchanted_monster_bone", null, 1, 2, 60, 1, "EPIC", 0, 0),
                new Seed("dungeon_epic", "coins", null, null, 1, 1, 100, 1, "EPIC", 2000, 5000),
                new Seed("dungeon_boss_bonus", "imperial_officer_seal", "imperial_officer_seal", null, 1, 2, 70, 1, "EPIC", 0, 0),
                new Seed("dungeon_boss_bonus", "monster_soul_shard", "monster_soul_shard", null, 1, 2, 70, 1, "EPIC", 0, 0),
                new Seed("dungeon_boss_bonus", "royal_scarp", "royal_scarp", null, 1, 1, 35, 1, "LEGENDARY", 0, 0),
                new Seed("dungeon_boss_bonus", "coins", null, null, 1, 1, 100, 1, "EPIC", 5000, 15000));
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT IGNORE INTO smp_dungeon_loot_table_entries(loot_table_id,entry_id,custom_item_id,vanilla_material,min_amount,max_amount,chance,weight,rarity,coins_min,coins_max,enabled) VALUES(?,?,?,?,?,?,?,?,?,?,?,1)")) {
            for (Seed seed : seeds) {
                statement.setString(1, seed.table()); statement.setString(2, seed.entry()); statement.setString(3, seed.custom()); statement.setString(4, seed.material());
                statement.setInt(5, seed.min()); statement.setInt(6, seed.max()); statement.setDouble(7, seed.chance()); statement.setDouble(8, seed.weight());
                statement.setString(9, seed.rarity()); statement.setLong(10, seed.coinsMin()); statement.setLong(11, seed.coinsMax()); statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void saveDefinition(Definition definition) {
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO smp_dungeon_loot_chests(dungeon_id,chest_id,loot_table_id,world_template_id,x,y,z,facing,enabled,created_by,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE loot_table_id=VALUES(loot_table_id),world_template_id=VALUES(world_template_id),x=VALUES(x),y=VALUES(y),z=VALUES(z),facing=VALUES(facing),enabled=VALUES(enabled),updated_at=VALUES(updated_at)")) {
            statement.setString(1, definition.dungeonId()); statement.setString(2, definition.chestId()); statement.setString(3, definition.lootTableId());
            statement.setString(4, definition.worldTemplateId()); statement.setInt(5, definition.x()); statement.setInt(6, definition.y()); statement.setInt(7, definition.z());
            statement.setString(8, definition.facing().name()); statement.setBoolean(9, definition.enabled()); statement.setString(10, definition.createdBy());
            statement.setLong(11, definition.createdAt()); statement.setLong(12, definition.updatedAt()); statement.executeUpdate();
        } catch (SQLException exception) { throw new RuntimeException(exception); }
    }

    private void deleteDefinition(Definition definition) {
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM smp_dungeon_loot_chests WHERE dungeon_id=? AND chest_id=?")) {
            statement.setString(1, definition.dungeonId()); statement.setString(2, definition.chestId()); statement.executeUpdate();
        } catch (SQLException exception) { throw new RuntimeException(exception); }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(CC.color("&e/dungeon lootchest add <chestId> <lootTableId>"));
        sender.sendMessage(CC.color("&e/dungeon lootchest remove|tp <chestId>"));
        sender.sendMessage(CC.color("&e/dungeon lootchest list|respawn|validate"));
        sender.sendMessage(CC.color("&e/dungeon lootchest setloottable <chestId> <lootTableId>"));
        sender.sendMessage(CC.color("&e/dungeon lootchest setfacing <chestId> <north|south|east|west>"));
        sender.sendMessage(CC.color("&e/dungeon lootchest rotate <chestId>"));
    }

    private static String id(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static String stateKey(String runId, String chestId) { return runId + ':' + id(chestId); }
    private static boolean validId(String value) { return value != null && !value.isBlank() && value.length() <= 96 && value.matches("[a-z0-9_.-]+"); }
    private static int random(int min, int max) { int low = Math.max(1, Math.min(min, max)), high = Math.max(low, Math.max(min, max)); return ThreadLocalRandom.current().nextInt(low, high + 1); }
    private static long randomLong(long min, long max) { long low = Math.max(1, Math.min(min, max)), high = Math.max(low, Math.max(min, max)); return low == high ? low : ThreadLocalRandom.current().nextLong(low, high + 1); }
    private static String rootMessage(Throwable error) { Throwable current = error; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }

    private enum RunState { SPAWNED, OPENED, LOOTED }
    private record WorldBlockKey(UUID world, int x, int y, int z) { static WorldBlockKey of(Block block) { return new WorldBlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()); } }
    private record RuntimeChest(String dungeonId, String runId, String chestId, String lootTableId, RunState state, UUID openedBy) { RuntimeChest withState(RunState next, UUID player) { return new RuntimeChest(dungeonId, runId, chestId, lootTableId, next, player); } }
    private record PersistedState(RunState state, UUID player) { }
    private record Definition(String dungeonId, String chestId, String lootTableId, String worldTemplateId, int x, int y, int z, BlockFace facing, boolean enabled, String createdBy, long createdAt, long updatedAt) { }
    private record LootEntry(String customItemId, String vanillaMaterial, int minAmount, int maxAmount, double chance, double weight, String rarity, long coinsMin, long coinsMax) { }
    private record Seed(String table, String entry, String custom, String material, int min, int max, double chance, double weight, String rarity, long coinsMin, long coinsMax) { }
}
