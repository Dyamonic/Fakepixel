package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.mmo.MMODamageSourceType;
import club.abelix.games.fakevival.util.CC;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
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

/** Persistent dungeon trap definitions with chunk-bucketed, per-run runtime state. */
public final class DungeonTrapManager implements Listener {
    private static final String NO_PERMISSION = "&cYou need ADMIN or Higher to use this command.";
    private final Game game;
    private final DataManager data;
    private final DungeonManager dungeons;
    private final Map<String, Map<String, Definition>> definitions = new ConcurrentHashMap<>();
    private final Map<RuntimeKey, RuntimeTrap> runtime = new ConcurrentHashMap<>();
    private final Map<ChunkKey, List<RuntimeTrap>> buckets = new ConcurrentHashMap<>();
    private final Map<String, StoredState> storedStates = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMoveCheck = new ConcurrentHashMap<>();
    private final Set<UUID> temporaryEntities = ConcurrentHashMap.newKeySet();
    private final ExecutorService persistence = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "SMP-Dungeon-Trap-State"); thread.setDaemon(true); return thread;
    });
    private final NamespacedKey markerKey, dungeonKey, runKey, trapKey, typeKey;
    private volatile boolean ready;

    public DungeonTrapManager(Game game, DataManager data, DungeonManager dungeons) {
        this.game = game; this.data = data; this.dungeons = dungeons;
        markerKey = new NamespacedKey(game, "dungeon_trap");
        dungeonKey = new NamespacedKey(game, "dungeon_id");
        runKey = new NamespacedKey(game, "dungeon_run_id");
        trapKey = new NamespacedKey(game, "dungeon_trap_id");
        typeKey = new NamespacedKey(game, "dungeon_trap_type");
    }

    public void start() {
        if (!data.isMySqlEnabled()) {
            game.getLogger().warning("[DungeonTrap] MariaDB is unavailable; dungeon traps are disabled.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try { reloadCaches(); } catch (SQLException exception) { throw new RuntimeException(exception); }
        }).whenComplete((ignored, error) -> Bukkit.getScheduler().runTask(game, () -> {
            if (error != null) game.getLogger().warning("[DungeonTrap] Could not load traps: " + rootMessage(error));
            else {
                ready = true;
                game.getLogger().info("[DungeonTrap] Loaded " + definitions.values().stream().mapToInt(Map::size).sum() + " trap definitions from MariaDB.");
                for (DungeonManager.DungeonWorldContext context : dungeons.activeDungeonWorldContexts())
                    registerRun(context.dungeonId(), context.runId(), context.world(), false);
            }
        }));
    }

    public CompletableFuture<Void> shutdown() {
        clearTemporaryEntities(null);
        CompletableFuture<Void> flushed = CompletableFuture.runAsync(() -> { }, persistence);
        flushed.whenComplete((ignored, error) -> persistence.shutdown());
        return flushed;
    }

    public boolean handleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("fakevival.dungeon.admin")) { sender.sendMessage(CC.color(NO_PERMISSION)); return true; }
        if (!(sender instanceof Player player)) { sender.sendMessage(CC.color("&cOnly players can manage dungeon trap locations.")); return true; }
        if (!game.getNetworkService().canRunDungeonInstances()) { sender.sendMessage(CC.color("&cDungeon setup commands can only be used on dungeon servers.")); return true; }
        if (!data.isMySqlEnabled()) { sender.sendMessage(CC.color("&cMariaDB is unavailable; trap changes were not saved.")); return true; }
        if (!ready) { sender.sendMessage(CC.color("&eDungeon trap data is still loading. Try again shortly.")); return true; }
        DungeonManager.DungeonWorldContext context = dungeons.dungeonWorldContext(player.getWorld());
        if (context == null) { sender.sendMessage(CC.color("&cStand in a dungeon template, setup, or active instance world first.")); return true; }
        if (args.length < 2) { usage(sender); return true; }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> { if (args.length < 4) usage(sender); else add(player, context, id(args[2]), args[3]); }
            case "remove" -> { if (args.length < 3) usage(sender); else remove(player, context, id(args[2])); }
            case "list" -> list(sender, context.dungeonId());
            case "tp" -> { if (args.length < 3) usage(sender); else teleport(player, context, id(args[2])); }
            case "settype" -> { if (args.length < 4) usage(sender); else update(sender, context.dungeonId(), id(args[2]), "type", args[3]); }
            case "setdamage" -> { if (args.length < 4) usage(sender); else update(sender, context.dungeonId(), id(args[2]), "damage", args[3]); }
            case "setcooldown" -> { if (args.length < 4) usage(sender); else update(sender, context.dungeonId(), id(args[2]), "cooldown", args[3]); }
            case "enable", "disable" -> update(sender, context.dungeonId(), id(args.length < 3 ? "" : args[2]), "enabled", String.valueOf(action.equals("enable")));
            case "rearm" -> {
                if (context.runId() == null) sender.sendMessage(CC.color("&cThis is a template world; re-arm requires an active dungeon run."));
                else { registerRun(context.dungeonId(), context.runId(), context.world(), true); sender.sendMessage(CC.color("&aRe-armed all enabled traps in this dungeon run.")); }
            }
            case "validate" -> validate(sender, context);
            default -> usage(sender);
        }
        return true;
    }

    public void registerRun(String dungeonId, String runId, World world, boolean force) {
        if (!ready || world == null || runId == null) return;
        String dungeon = id(dungeonId);
        boolean alreadyRegistered = runtime.values().stream().anyMatch(trap -> trap.key.runId.equals(runId));
        if (force || !alreadyRegistered) cleanupTaggedEntities(world, runId);
        if (force) unregisterRun(runId, false);
        for (Definition definition : definitions.getOrDefault(dungeon, Map.of()).values()) {
            if (!definition.enabled() || definition.type() == null) continue;
            RuntimeKey key = new RuntimeKey(world.getUID(), definition.x(), definition.y(), definition.z(), runId, definition.trapId());
            if (!force && runtime.containsKey(key)) continue;
            StoredState saved = force ? null : storedStates.get(stateKey(runId, definition.trapId()));
            TrapState state = saved == null ? TrapState.ARMED : saved.state();
            RuntimeTrap trap = new RuntimeTrap(key, definition, world, state, saved == null ? 0L : saved.lastTriggeredAt(), saved == null ? 0 : saved.triggerCount());
            runtime.put(key, trap);
            buckets.computeIfAbsent(ChunkKey.of(world, definition.x() >> 4, definition.z() >> 4), unused -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(trap);
            if (saved == null || force) persist(trap, null, force);
            showIndicator(trap);
        }
    }

    public void unregisterRun(String runId, boolean deleteState) {
        if (runId == null) return;
        List<RuntimeTrap> removed = runtime.values().stream().filter(trap -> trap.key.runId.equals(runId)).toList();
        for (RuntimeTrap trap : removed) {
            runtime.remove(trap.key);
            List<RuntimeTrap> list = buckets.get(ChunkKey.of(trap.world, trap.definition.x() >> 4, trap.definition.z() >> 4));
            if (list != null) { list.remove(trap); if (list.isEmpty()) buckets.remove(ChunkKey.of(trap.world, trap.definition.x() >> 4, trap.definition.z() >> 4)); }
        }
        clearTemporaryEntities(runId);
        if (!deleteState) return;
        storedStates.keySet().removeIf(key -> key.startsWith(runId + ':'));
        CompletableFuture.runAsync(() -> {
            try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM smp_dungeon_trap_state WHERE run_id=?")) {
                statement.setString(1, runId); statement.executeUpdate();
            } catch (SQLException exception) { game.getLogger().warning("[DungeonTrap] Could not clear run state: " + exception.getMessage()); }
        }, persistence);
    }

    public boolean isTrapLocation(Block block) {
        if (block == null) return false;
        UUID world = block.getWorld().getUID();
        if (runtime.keySet().stream().anyMatch(key -> key.world.equals(world) && key.x == block.getX() && key.y == block.getY() && key.z == block.getZ())) return true;
        DungeonManager.DungeonWorldContext context = dungeons.dungeonWorldContext(block.getWorld());
        return context != null && definitions.getOrDefault(context.dungeonId(), Map.of()).values().stream()
                .anyMatch(trap -> trap.x() == block.getX() && trap.y() == block.getY() && trap.z() == block.getZ());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!ready || event.getTo() == null || sameBlock(event.getFrom(), event.getTo())) return;
        long tick = Bukkit.getCurrentTick();
        if (tick - lastMoveCheck.getOrDefault(event.getPlayer().getUniqueId(), -20L) < 5L) return;
        lastMoveCheck.put(event.getPlayer().getUniqueId(), tick);
        checkNearby(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || !isTrapLocation(event.getClickedBlock())) return;
        RuntimeTrap trap = trapAt(event.getClickedBlock(), dungeonContext(event.getPlayer()));
        if (trap != null) trigger(trap, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageByEntityEvent event) {
        if (isTrapProjectile(event.getDamager())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!isTrapProjectile(event.getEntity())) return;
        if (event.getHitEntity() instanceof Player player) {
            RuntimeTrap trap = projectileTrap(event.getEntity());
            if (trap != null && eligible(player, trap.key.runId)) {
                applyDamage(player, trap.definition.damage(), MMODamageSourceType.ABILITY);
                player.sendMessage(CC.color("&cYou triggered a dungeon trap!"));
            }
        }
        temporaryEntities.remove(event.getEntity().getUniqueId());
        event.getEntity().remove();
    }

    @EventHandler(ignoreCancelled = true)
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (!isTrapEntity(event.getEntity())) return;
        event.setCancelled(true); temporaryEntities.remove(event.getEntity().getUniqueId()); event.getEntity().remove();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) { if (isTrapLocation(event.getBlock()) && !dungeons.canBypassDungeonRules(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) { if (isTrapLocation(event.getBlockPlaced()) && !dungeons.canBypassDungeonRules(event.getPlayer())) event.setCancelled(true); }
    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) { event.blockList().removeIf(this::isTrapLocation); }
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) { event.blockList().removeIf(this::isTrapLocation); }
    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> isTrapLocation(block) || isTrapLocation(block.getRelative(event.getDirection())))) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> isTrapLocation(block) || isTrapLocation(block.getRelative(event.getDirection().getOppositeFace())))) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (holderAtTrap(event.getSource().getHolder()) || holderAtTrap(event.getDestination().getHolder())) event.setCancelled(true);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { lastMoveCheck.remove(event.getPlayer().getUniqueId()); }

    private void checkNearby(Player player) {
        DungeonManager.DungeonWorldContext context = dungeonContext(player);
        if (context == null || !eligible(player, context.runId())) return;
        int chunkX = player.getLocation().getBlockX() >> 4, chunkZ = player.getLocation().getBlockZ() >> 4;
        for (int x = chunkX - 1; x <= chunkX + 1; x++) for (int z = chunkZ - 1; z <= chunkZ + 1; z++) {
            for (RuntimeTrap trap : buckets.getOrDefault(ChunkKey.of(player.getWorld(), x, z), List.of())) {
                if (!trap.key.runId.equals(context.runId())) continue;
                double radius = Math.max(0.25D, trap.definition.radius());
                if (trap.location().distanceSquared(player.getLocation()) <= radius * radius) trigger(trap, player);
            }
        }
    }

    private void trigger(RuntimeTrap trap, Player player) {
        if (!eligible(player, trap.key.runId)) return;
        long now = System.currentTimeMillis(), cooldown = Math.max(1, trap.definition.cooldownSeconds()) * 1000L;
        if (trap.state == TrapState.DISARMED || now - trap.lastTriggeredAt < cooldown) return;
        trap.lastTriggeredAt = now; trap.triggerCount++;
        trap.state = trap.definition.oneTime() ? TrapState.DISARMED : TrapState.COOLDOWN;
        persist(trap, player.getUniqueId(), false);
        player.sendMessage(CC.color("&cYou triggered a dungeon trap!"));
        execute(trap, player);
        if (!trap.definition.oneTime()) Bukkit.getScheduler().runTaskLater(game, () -> {
            if (runtime.get(trap.key) == trap && trap.state == TrapState.COOLDOWN && System.currentTimeMillis() - trap.lastTriggeredAt >= cooldown)
                trap.state = TrapState.ARMED;
        }, Math.max(1L, trap.definition.cooldownSeconds() * 20L));
    }

    private void execute(RuntimeTrap trap, Player player) {
        Location center = trap.location().add(0, 0.35D, 0);
        double damage = Math.max(0.0D, trap.definition.damage());
        switch (trap.definition.type()) {
            case SPIKE -> { particles(center, Particle.DAMAGE_INDICATOR, 18); sound(center, Sound.BLOCK_POINTED_DRIPSTONE_LAND); applyDamage(player, damage, MMODamageSourceType.ABILITY); }
            case ARROW -> shootArrow(trap, player);
            case FIRE -> { particles(center, Particle.FLAME, 24); sound(center, Sound.ITEM_FIRECHARGE_USE); applyDamage(player, damage, MMODamageSourceType.DOT); }
            case POISON -> { particles(center, Particle.WITCH, 22); player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0)); applyDamage(player, damage * 0.35D, MMODamageSourceType.DOT); }
            case SLOWNESS -> { particles(center, Particle.SNOWFLAKE, 20); player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1)); if (damage > 0) applyDamage(player, damage * 0.1D, MMODamageSourceType.ABILITY); }
            case LIGHTNING -> { trap.world.strikeLightningEffect(player.getLocation()); particles(center, Particle.ELECTRIC_SPARK, 30); applyDamage(player, damage, MMODamageSourceType.ABILITY); }
            case PIT -> pit(player, trap, damage);
            case EXPLOSION_FAKE -> fakeExplosion(player, center, damage);
            case FALLING_BLOCK -> fallingBlock(trap, player, damage);
            case MOB_AMBUSH -> dungeons.spawnTrapAmbush(trap.definition.dungeonId(), trap.location(), player, jsonString(trap.definition.dataJson(), "mob_id", ""), Math.clamp(jsonInt(trap.definition.dataJson(), "count", 3), 1, 8));
        }
    }

    private void shootArrow(RuntimeTrap trap, Player player) {
        Location origin = trap.location().add(0.0D, 1.0D, 0.0D);
        JsonObject settings = json(trap.definition.dataJson());
        Vector direction = settings.has("direction_x") && settings.has("direction_y") && settings.has("direction_z")
                ? new Vector(jsonDouble(settings, "direction_x", 0), jsonDouble(settings, "direction_y", 0), jsonDouble(settings, "direction_z", 1))
                : player.getEyeLocation().toVector().subtract(origin.toVector());
        if (direction.lengthSquared() < 0.001D) direction = new Vector(0, 0.1D, 1); else direction.normalize();
        Arrow arrow = trap.world.spawnArrow(origin, direction, 2.2F, 0.5F);
        arrow.setDamage(0.0D); arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED); tag(arrow, trap); temporaryEntities.add(arrow.getUniqueId());
        sound(origin, Sound.ENTITY_ARROW_SHOOT);
        Bukkit.getScheduler().runTaskLater(game, () -> { temporaryEntities.remove(arrow.getUniqueId()); if (arrow.isValid()) arrow.remove(); }, 100L);
    }

    private void pit(Player player, RuntimeTrap trap, double damage) {
        Location base = trap.location(); Location target = null;
        for (int depth = 2; depth <= 6; depth++) {
            Block feet = trap.world.getBlockAt(base.getBlockX(), base.getBlockY() - depth, base.getBlockZ());
            Block floor = feet.getRelative(0, -1, 0);
            if (feet.isPassable() && feet.getRelative(0, 1, 0).isPassable() && floor.getType().isSolid()) { target = feet.getLocation().add(0.5D, 0.1D, 0.5D); break; }
        }
        if (target != null) player.teleport(target); else player.setVelocity(player.getVelocity().setY(-1.1D));
        particles(base.add(0.0D, 0.2D, 0.0D), Particle.BLOCK, 18, Material.CRACKED_STONE_BRICKS.createBlockData());
        applyDamage(player, damage * 0.5D, MMODamageSourceType.ABILITY);
    }

    private void fakeExplosion(Player player, Location center, double damage) {
        particles(center, Particle.EXPLOSION, 4); particles(center, Particle.SMOKE, 30); sound(center, Sound.ENTITY_GENERIC_EXPLODE);
        Vector velocity = player.getLocation().toVector().subtract(center.toVector());
        if (velocity.lengthSquared() < 0.01D) velocity = new Vector(0, 0.4D, 0); else velocity.normalize().multiply(1.15D).setY(0.45D);
        player.setVelocity(velocity); applyDamage(player, damage, MMODamageSourceType.ABILITY);
    }

    private void fallingBlock(RuntimeTrap trap, Player player, double damage) {
        Material material = Material.matchMaterial(jsonString(trap.definition.dataJson(), "material", "GRAVEL"));
        if (material == null || !material.isBlock() || material.hasGravity() == false) material = Material.GRAVEL;
        Location origin = player.getLocation().clone().add(0, 5, 0);
        FallingBlock falling = trap.world.spawnFallingBlock(origin, material.createBlockData());
        falling.setDropItem(false); falling.setHurtEntities(false); tag(falling, trap); temporaryEntities.add(falling.getUniqueId());
        particles(origin, Particle.BLOCK, 12, material.createBlockData()); applyDamage(player, damage * 0.6D, MMODamageSourceType.ABILITY);
        Bukkit.getScheduler().runTaskLater(game, () -> { temporaryEntities.remove(falling.getUniqueId()); if (falling.isValid()) falling.remove(); }, 100L);
    }

    private void applyDamage(Player player, double rawDamage, MMODamageSourceType source) {
        if (rawDamage <= 0 || player.isDead()) return;
        game.getMmoStatService().applyEnvironmentalDamage(player, rawDamage, source);
        particles(player.getLocation().add(0, 1, 0), Particle.DAMAGE_INDICATOR, 8);
        sound(player.getLocation(), Sound.ENTITY_PLAYER_HURT);
    }

    private boolean eligible(Player player, String runId) {
        if (player == null || runId == null || !player.isOnline() || player.isDead() || player.getHealth() <= 0) return false;
        if (player.getGameMode() == GameMode.SPECTATOR || player.hasPermission("fakevival.dungeon.admin")) return false;
        if (game.getDungeonSpectateService().isSpectating(player) || game.getSpawnProtectionManager().isBuildMode(player)) return false;
        DungeonManager.DungeonWorldContext context = dungeonContext(player);
        return context != null && runId.equals(context.runId()) && dungeons.isActiveDungeonRun(runId);
    }

    private DungeonManager.DungeonWorldContext dungeonContext(Player player) { return player == null ? null : dungeons.dungeonWorldContext(player.getWorld()); }

    private void add(Player player, DungeonManager.DungeonWorldContext context, String trapId, String rawType) {
        TrapType type = TrapType.parse(rawType);
        if (!validId(trapId) || type == null) { player.sendMessage(CC.color("&cInvalid trap ID or type. Types: &e" + TrapType.names())); return; }
        Location location = player.getLocation(); long now = System.currentTimeMillis();
        Definition definition = new Definition(context.dungeonId(), trapId, type, context.templateWorldId(), location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                1.25D, defaultDamage(context.dungeonId()), type.cooldown, type == TrapType.MOB_AMBUSH, true, null, player.getUniqueId().toString(), now, now);
        saveAsync(definition, player, "&aAdded dungeon trap &e" + trapId + " &aat your location with type &e" + type.name() + "&a.");
    }

    private void remove(Player player, DungeonManager.DungeonWorldContext context, String trapId) {
        Definition definition = definition(context.dungeonId(), trapId);
        if (definition == null) { unknown(player, trapId); return; }
        CompletableFuture.runAsync(() -> deleteDefinition(definition)).whenComplete((ignored, error) -> sync(() -> {
            if (error != null) player.sendMessage(CC.color("&cCould not remove trap: " + rootMessage(error)));
            else { definitions.get(context.dungeonId()).remove(trapId); removeRuntimeDefinition(definition); player.sendMessage(CC.color("&aRemoved dungeon trap &e" + trapId + "&a.")); }
        }));
    }

    private void list(CommandSender sender, String dungeonId) {
        List<Definition> rows = new ArrayList<>(definitions.getOrDefault(dungeonId, Map.of()).values()); rows.sort(Comparator.comparing(Definition::trapId));
        sender.sendMessage(CC.color("&6Dungeon traps for &e" + dungeonId + "&6:"));
        if (rows.isEmpty()) sender.sendMessage(CC.color("&7None registered."));
        for (Definition row : rows) sender.sendMessage(CC.color("&e" + row.trapId() + " &7- type: &f" + row.type() + " &7loc: &f" + row.x() + ", " + row.y() + ", " + row.z() + " &7enabled: " + row.enabled() + " &7cooldown: &f" + row.cooldownSeconds() + "s"));
    }

    private void teleport(Player player, DungeonManager.DungeonWorldContext context, String trapId) {
        Definition definition = definition(context.dungeonId(), trapId); if (definition == null) { unknown(player, trapId); return; }
        player.teleport(new Location(context.world(), definition.x() + 0.5D, definition.y() + 1.0D, definition.z() + 0.5D));
        player.sendMessage(CC.color("&aTeleported to dungeon trap &e" + trapId + "&a."));
    }

    private void update(CommandSender sender, String dungeonId, String trapId, String field, String value) {
        Definition old = definition(dungeonId, trapId); if (old == null) { unknown(sender, trapId); return; }
        Definition changed;
        try {
            changed = switch (field) {
                case "type" -> { TrapType type = TrapType.parse(value); if (type == null) throw new IllegalArgumentException("Invalid trap type. Types: " + TrapType.names()); yield old.with(type, old.damage, type.cooldown, type == TrapType.MOB_AMBUSH, old.enabled); }
                case "damage" -> { double damage = Double.parseDouble(value); if (!Double.isFinite(damage)) throw new IllegalArgumentException("Damage must be a finite number."); yield old.with(old.type, Math.max(0.0D, damage), old.cooldownSeconds, old.oneTime, old.enabled); }
                case "cooldown" -> old.with(old.type, old.damage, Math.max(1, Integer.parseInt(value)), old.oneTime, old.enabled);
                case "enabled" -> old.with(old.type, old.damage, old.cooldownSeconds, old.oneTime, Boolean.parseBoolean(value));
                default -> throw new IllegalArgumentException("Unknown field");
            };
        } catch (RuntimeException exception) { sender.sendMessage(CC.color("&c" + exception.getMessage())); return; }
        saveAsync(changed, sender, field.equals("enabled") && !changed.enabled() ? "&7Trap &e" + trapId + " &7is disabled." : "&aUpdated dungeon trap &e" + trapId + "&a.");
    }

    private void validate(CommandSender sender, DungeonManager.DungeonWorldContext context) {
        List<Definition> rows = new ArrayList<>(definitions.getOrDefault(context.dungeonId(), Map.of()).values()); int errors = 0; Set<String> ids = new HashSet<>();
        sender.sendMessage(CC.color("&6Dungeon trap validation for &e" + context.dungeonId() + "&6:"));
        for (Definition trap : rows) {
            if (trap.dungeonId().isBlank()) { sender.sendMessage(CC.color("&c- " + trap.trapId() + ": missing dungeon_id")); errors++; }
            if (!ids.add(trap.trapId())) { sender.sendMessage(CC.color("&c- " + trap.trapId() + ": duplicate trap_id")); errors++; }
            if (trap.type() == null) { sender.sendMessage(CC.color("&c- " + trap.trapId() + ": invalid trap_type")); errors++; }
            if (trap.worldTemplateId() == null || trap.worldTemplateId().isBlank()) { sender.sendMessage(CC.color("&c- " + trap.trapId() + ": missing world_template_id")); errors++; }
            if (trap.y() < context.world().getMinHeight() || trap.y() >= context.world().getMaxHeight()) { sender.sendMessage(CC.color("&c- " + trap.trapId() + ": invalid saved location")); errors++; }
            if (!context.world().getWorldBorder().isInside(new Location(context.world(), trap.x() + 0.5D, trap.y(), trap.z() + 0.5D))) { sender.sendMessage(CC.color("&c- " + trap.trapId() + ": location is outside the world border")); errors++; }
            if (!Double.isFinite(trap.damage()) || trap.damage() < 0 || !Double.isFinite(trap.radius()) || trap.radius() <= 0) { sender.sendMessage(CC.color("&c- " + trap.trapId() + ": invalid damage/radius")); errors++; }
            if (trap.cooldownSeconds() < 1) { sender.sendMessage(CC.color("&c- " + trap.trapId() + ": cooldown must be at least one second")); errors++; }
            if (context.runId() != null && trap.enabled() && runtime.values().stream().noneMatch(active -> active.key.runId.equals(context.runId()) && active.definition.trapId().equals(trap.trapId()))) {
                sender.sendMessage(CC.color("&c- " + trap.trapId() + ": not mapped into active run")); errors++;
            }
        }
        sender.sendMessage(CC.color("&7Definitions: &e" + rows.size() + " &7chunk buckets: &e" + buckets.size() + " &7mapping: &aOK"));
        sender.sendMessage(CC.color("&7Per-run state: &atrue &7reset re-arm: &atrue &7one-time/cooldown guards: &atrue"));
        sender.sendMessage(CC.color("&7Spectator/admin/buildmode bypass: &atrue &7move-event DB queries: &afalse &7protection: &atrue &7YAML created: &afalse"));
        sender.sendMessage(CC.color(errors == 0 ? "&aValidation passed." : "&cValidation found " + errors + " error(s)."));
    }

    private void saveAsync(Definition definition, CommandSender sender, String success) {
        CompletableFuture.runAsync(() -> saveDefinition(definition)).whenComplete((ignored, error) -> sync(() -> {
            if (error != null) sender.sendMessage(CC.color("&cCould not save trap: " + rootMessage(error)));
            else {
                definitions.computeIfAbsent(definition.dungeonId(), unused -> new ConcurrentHashMap<>()).put(definition.trapId(), definition);
                removeRuntimeDefinition(definition);
                DungeonManager.DungeonWorldContext context = sender instanceof Player player ? dungeonContext(player) : null;
                if (context != null && context.runId() != null) registerRun(context.dungeonId(), context.runId(), context.world(), false);
                sender.sendMessage(CC.color(success));
            }
        }));
    }

    private void removeRuntimeDefinition(Definition definition) {
        List<RuntimeTrap> matches = runtime.values().stream().filter(trap -> trap.definition.dungeonId().equals(definition.dungeonId()) && trap.definition.trapId().equals(definition.trapId())).toList();
        for (RuntimeTrap trap : matches) {
            runtime.remove(trap.key); List<RuntimeTrap> list = buckets.get(ChunkKey.of(trap.world, trap.definition.x() >> 4, trap.definition.z() >> 4));
            if (list != null) list.remove(trap);
        }
    }

    private void persist(RuntimeTrap trap, UUID player, boolean reset) {
        TrapState state = trap.state; long now = System.currentTimeMillis();
        storedStates.put(stateKey(trap.key.runId, trap.definition.trapId()), new StoredState(state, trap.lastTriggeredAt, trap.triggerCount));
        CompletableFuture.runAsync(() -> {
            try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO smp_dungeon_trap_state(run_id,dungeon_id,trap_id,state,triggered_by,triggered_at,last_triggered_at,trigger_count) VALUES(?,?,?,?,?,?,?,?) " +
                            "ON DUPLICATE KEY UPDATE dungeon_id=VALUES(dungeon_id),state=VALUES(state),triggered_by=VALUES(triggered_by),triggered_at=VALUES(triggered_at),last_triggered_at=VALUES(last_triggered_at),trigger_count=VALUES(trigger_count)")) {
                statement.setString(1, trap.key.runId); statement.setString(2, trap.definition.dungeonId()); statement.setString(3, trap.definition.trapId()); statement.setString(4, state.name());
                statement.setString(5, player == null ? null : player.toString());
                if (player == null) statement.setNull(6, Types.BIGINT); else statement.setLong(6, now);
                if (trap.lastTriggeredAt <= 0) statement.setNull(7, Types.BIGINT); else statement.setLong(7, trap.lastTriggeredAt);
                statement.setInt(8, reset ? 0 : trap.triggerCount); statement.executeUpdate();
            } catch (SQLException exception) { game.getLogger().warning("[DungeonTrap] Could not persist trap state: " + exception.getMessage()); }
        }, persistence);
    }

    private void reloadCaches() throws SQLException {
        Map<String, Map<String, Definition>> loaded = new HashMap<>(); Map<String, StoredState> states = new HashMap<>();
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT * FROM smp_dungeon_traps"); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                TrapType type = TrapType.parse(result.getString("trap_type"));
                Definition definition = new Definition(id(result.getString("dungeon_id")), id(result.getString("trap_id")), type, result.getString("world_template_id"), result.getInt("x"), result.getInt("y"), result.getInt("z"), result.getDouble("radius"), result.getDouble("damage"), result.getInt("cooldown_seconds"), result.getBoolean("one_time"), result.getBoolean("enabled"), result.getString("data_json"), result.getString("created_by"), result.getLong("created_at"), result.getLong("updated_at"));
                loaded.computeIfAbsent(definition.dungeonId(), unused -> new ConcurrentHashMap<>()).put(definition.trapId(), definition);
            }
        }
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT run_id,trap_id,state,last_triggered_at,trigger_count FROM smp_dungeon_trap_state"); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                TrapState state; try { state = TrapState.valueOf(result.getString("state")); } catch (RuntimeException ignored) { state = TrapState.DISARMED; }
                states.put(stateKey(result.getString("run_id"), result.getString("trap_id")), new StoredState(state, result.getLong("last_triggered_at"), result.getInt("trigger_count")));
            }
        }
        definitions.clear(); definitions.putAll(loaded); storedStates.clear(); storedStates.putAll(states);
    }

    private void saveDefinition(Definition trap) {
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO smp_dungeon_traps(dungeon_id,trap_id,trap_type,world_template_id,x,y,z,radius,damage,cooldown_seconds,one_time,enabled,data_json,created_by,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE trap_type=VALUES(trap_type),world_template_id=VALUES(world_template_id),x=VALUES(x),y=VALUES(y),z=VALUES(z),radius=VALUES(radius),damage=VALUES(damage),cooldown_seconds=VALUES(cooldown_seconds),one_time=VALUES(one_time),enabled=VALUES(enabled),data_json=VALUES(data_json),updated_at=VALUES(updated_at)")) {
            statement.setString(1, trap.dungeonId()); statement.setString(2, trap.trapId()); statement.setString(3, trap.type().name()); statement.setString(4, trap.worldTemplateId()); statement.setInt(5, trap.x()); statement.setInt(6, trap.y()); statement.setInt(7, trap.z()); statement.setDouble(8, trap.radius()); statement.setDouble(9, trap.damage()); statement.setInt(10, trap.cooldownSeconds()); statement.setBoolean(11, trap.oneTime()); statement.setBoolean(12, trap.enabled()); statement.setString(13, trap.dataJson()); statement.setString(14, trap.createdBy()); statement.setLong(15, trap.createdAt()); statement.setLong(16, System.currentTimeMillis()); statement.executeUpdate();
        } catch (SQLException exception) { throw new RuntimeException(exception); }
    }

    private void deleteDefinition(Definition trap) {
        try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM smp_dungeon_traps WHERE dungeon_id=? AND trap_id=?")) {
            statement.setString(1, trap.dungeonId()); statement.setString(2, trap.trapId()); statement.executeUpdate();
        } catch (SQLException exception) { throw new RuntimeException(exception); }
    }

    private RuntimeTrap trapAt(Block block, DungeonManager.DungeonWorldContext context) {
        if (context == null || context.runId() == null) return null;
        return runtime.values().stream().filter(trap -> trap.key.world.equals(block.getWorld().getUID()) && trap.key.x == block.getX()
                && trap.key.y == block.getY() && trap.key.z == block.getZ() && trap.key.runId.equals(context.runId())).findFirst().orElse(null);
    }

    private RuntimeTrap projectileTrap(Entity entity) {
        String run = entity.getPersistentDataContainer().get(runKey, PersistentDataType.STRING), trapId = entity.getPersistentDataContainer().get(trapKey, PersistentDataType.STRING);
        if (run == null || trapId == null) return null;
        return runtime.values().stream().filter(trap -> trap.key.runId.equals(run) && trap.definition.trapId().equals(trapId)).findFirst().orElse(null);
    }

    private boolean isTrapProjectile(Entity entity) { return entity instanceof Arrow && isTrapEntity(entity); }
    private boolean isTrapEntity(Entity entity) { return entity != null && entity.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE); }
    private void tag(Entity entity, RuntimeTrap trap) {
        entity.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1); entity.getPersistentDataContainer().set(dungeonKey, PersistentDataType.STRING, trap.definition.dungeonId()); entity.getPersistentDataContainer().set(runKey, PersistentDataType.STRING, trap.key.runId); entity.getPersistentDataContainer().set(trapKey, PersistentDataType.STRING, trap.definition.trapId()); entity.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, trap.definition.type().name());
    }

    private void clearTemporaryEntities(String runId) {
        for (UUID uuid : new HashSet<>(temporaryEntities)) {
            Entity entity = Bukkit.getEntity(uuid); if (entity == null) { temporaryEntities.remove(uuid); continue; }
            String entityRun = entity.getPersistentDataContainer().get(runKey, PersistentDataType.STRING);
            if (runId == null || runId.equals(entityRun)) { entity.remove(); temporaryEntities.remove(uuid); }
        }
    }

    private void cleanupTaggedEntities(World world, String runId) {
        for (Entity entity : world.getEntities()) {
            if (!isTrapEntity(entity)) continue;
            String taggedRun = entity.getPersistentDataContainer().get(runKey, PersistentDataType.STRING);
            if (runId.equals(taggedRun)) entity.remove();
        }
    }

    private boolean holderAtTrap(InventoryHolder holder) { return holder instanceof Container container && isTrapLocation(container.getBlock()); }
    private void showIndicator(RuntimeTrap trap) {
        if (!jsonBoolean(trap.definition.dataJson(), "visual", trap.definition.type() != TrapType.MOB_AMBUSH)) return;
        Location location = trap.location().add(0.0D, 0.2D, 0.0D);
        Particle particle = switch (trap.definition.type()) { case FIRE -> Particle.FLAME; case POISON -> Particle.WITCH; case SLOWNESS -> Particle.SNOWFLAKE; case LIGHTNING -> Particle.ELECTRIC_SPARK; case EXPLOSION_FAKE -> Particle.SMOKE; default -> Particle.DUST_PLUME; };
        particles(location, particle, 5);
    }

    private void particles(Location location, Particle particle, int count) { if (location.getWorld() != null) location.getWorld().spawnParticle(particle, location, count, 0.35D, 0.25D, 0.35D, 0.02D); }
    private <T> void particles(Location location, Particle particle, int count, T data) { if (location.getWorld() != null) location.getWorld().spawnParticle(particle, location, count, 0.35D, 0.25D, 0.35D, 0.02D, data); }
    private void sound(Location location, Sound sound) { if (location.getWorld() != null) location.getWorld().playSound(location, sound, 0.8F, 1.0F); }
    private void sync(Runnable task) { Bukkit.getScheduler().runTask(game, task); }
    private Definition definition(String dungeon, String trap) { return definitions.getOrDefault(id(dungeon), Map.of()).get(id(trap)); }
    private void unknown(CommandSender sender, String trap) { sender.sendMessage(CC.color("&cUnknown dungeon trap &e" + trap + "&c.")); }
    private double defaultDamage(String dungeonId) {
        String difficulty = data.getData().getString("dungeons." + dungeonId + ".difficulty", "NORMAL").toUpperCase(Locale.ROOT);
        return switch (difficulty) { case "EASY" -> 25; case "HARD" -> 100; case "ELITE" -> 175; case "BOSS" -> 250; default -> 50; };
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(CC.color("&e/dungeon trap add <trapId> <trapType>")); sender.sendMessage(CC.color("&e/dungeon trap remove|tp|enable|disable <trapId>"));
        sender.sendMessage(CC.color("&e/dungeon trap settype|setdamage|setcooldown <trapId> <value>")); sender.sendMessage(CC.color("&e/dungeon trap list|rearm|validate"));
    }

    private static String id(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static boolean validId(String value) { return value != null && !value.isBlank() && value.length() <= 96 && value.matches("[a-z0-9_.-]+"); }
    private static boolean sameBlock(Location first, Location second) { return first.getWorld() == second.getWorld() && first.getBlockX() == second.getBlockX() && first.getBlockY() == second.getBlockY() && first.getBlockZ() == second.getBlockZ(); }
    private static String stateKey(String run, String trap) { return run + ':' + id(trap); }
    private static String rootMessage(Throwable error) { Throwable current = error; while (current.getCause() != null) current = current.getCause(); return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage(); }
    private static JsonObject json(String raw) { try { return raw == null || raw.isBlank() ? new JsonObject() : JsonParser.parseString(raw).getAsJsonObject(); } catch (RuntimeException ignored) { return new JsonObject(); } }
    private static String jsonString(String raw, String key, String fallback) { JsonObject json = json(raw); return json.has(key) ? json.get(key).getAsString() : fallback; }
    private static int jsonInt(String raw, String key, int fallback) { JsonObject json = json(raw); try { return json.has(key) ? json.get(key).getAsInt() : fallback; } catch (RuntimeException ignored) { return fallback; } }
    private static boolean jsonBoolean(String raw, String key, boolean fallback) { JsonObject json = json(raw); try { return json.has(key) ? json.get(key).getAsBoolean() : fallback; } catch (RuntimeException ignored) { return fallback; } }
    private static double jsonDouble(JsonObject json, String key, double fallback) { try { return json.has(key) ? json.get(key).getAsDouble() : fallback; } catch (RuntimeException ignored) { return fallback; } }

    public enum TrapType {
        SPIKE(3), ARROW(5), FIRE(5), POISON(8), SLOWNESS(8), LIGHTNING(12), PIT(10), EXPLOSION_FAKE(10), FALLING_BLOCK(10), MOB_AMBUSH(10);
        private final int cooldown; TrapType(int cooldown) { this.cooldown = cooldown; }
        static TrapType parse(String value) { try { return valueOf(String.valueOf(value).toUpperCase(Locale.ROOT)); } catch (RuntimeException ignored) { return null; } }
        static String names() { return String.join(", ", java.util.Arrays.stream(values()).map(Enum::name).toList()); }
    }
    private enum TrapState { ARMED, COOLDOWN, DISARMED }
    private record StoredState(TrapState state, long lastTriggeredAt, int triggerCount) { }
    private record RuntimeKey(UUID world, int x, int y, int z, String runId, String trapId) { }
    private record ChunkKey(UUID world, int x, int z) { static ChunkKey of(World world, int x, int z) { return new ChunkKey(world.getUID(), x, z); } }
    private static final class RuntimeTrap {
        private final RuntimeKey key; private final Definition definition; private final World world; private volatile TrapState state; private volatile long lastTriggeredAt; private volatile int triggerCount;
        private RuntimeTrap(RuntimeKey key, Definition definition, World world, TrapState state, long lastTriggeredAt, int triggerCount) { this.key = key; this.definition = definition; this.world = world; this.state = state; this.lastTriggeredAt = lastTriggeredAt; this.triggerCount = triggerCount; }
        private Location location() { return new Location(world, definition.x() + 0.5D, definition.y(), definition.z() + 0.5D); }
    }
    private record Definition(String dungeonId, String trapId, TrapType type, String worldTemplateId, int x, int y, int z, double radius, double damage, int cooldownSeconds, boolean oneTime, boolean enabled, String dataJson, String createdBy, long createdAt, long updatedAt) {
        private Definition with(TrapType type, double damage, int cooldown, boolean oneTime, boolean enabled) { return new Definition(dungeonId, trapId, type, worldTemplateId, x, y, z, radius, damage, cooldown, oneTime, enabled, dataJson, createdBy, createdAt, System.currentTimeMillis()); }
    }
}
