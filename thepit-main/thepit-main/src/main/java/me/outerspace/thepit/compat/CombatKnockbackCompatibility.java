package me.outerspace.thepit.compat;

import me.outerspace.thepit.ThePitPlugin;
import org.bukkit.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CombatKnockbackCompatibility implements Listener, CommandExecutor, TabCompleter {
    private static final long MAXIMUM_PENDING_NANOS = 50_000_000L;
    private final ThePitPlugin plugin;
    private final Map<UUID, PendingHit> pending = new HashMap<>();
    private final Map<String, PhysicalProfile> profiles = new LinkedHashMap<>();
    private final Map<String, PhysicalProfile> undo = new HashMap<>();
    private final Map<UUID, ConnectionState> connections = new HashMap<>();
    private final Map<UUID, TestOverride> tests = new HashMap<>();
    private volatile PhysicalProfile activeProfile;
    private String activeProfileName = "normal";
    private boolean enabled;
    private boolean autoClassification;
    private boolean diagnostics;
    private BukkitTask sampleTask;
    private Method getHandle;
    private Field pingField;
    private long serverTick;

    public CombatKnockbackCompatibility(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        reload();
    }

    public void reload() {
        pending.clear();
        enabled = plugin.registry().settings().getBoolean("combat.knockback.enabled", true);
        autoClassification = plugin.registry().settings().getBoolean("combat.knockback.auto-classification", true);
        diagnostics = plugin.registry().settings().getBoolean("combat.knockback.diagnostics", false);
        profiles.clear();
        for (String name : plugin.registry().settings().keys("combat.knockback.profiles")) profiles.put(normalize(name), readProfile(name));
        ensureBuiltIns();
        activeProfileName = normalize(plugin.registry().settings().getString("combat.knockback.active-profile", "fakepixel"));
        activeProfile = profiles.getOrDefault(activeProfileName, profiles.get("normal"));
        if (enabled) {
            if (sampleTask == null) sampleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickConnections, 1L, 1L);
            resolvePingAccess();
        } else {
            tests.clear(); connections.clear();
            if (sampleTask != null) sampleTask.cancel();
            sampleTask = null;
        }
        plugin.getLogger().info(enabled ? "The Pit server-authoritative combat knockback is enabled." : "The Pit combat knockback override is disabled; PandaSpigot behavior is unchanged.");
    }

    public void shutdown() {
        enabled = false;
        pending.clear();
        tests.clear();
        connections.clear();
        if (sampleTask != null) sampleTask.cancel();
        sampleTask = null;
    }

    public void clearAll() {
        pending.clear();
        tests.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled || !(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
        Player target = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        if (!eligible(attacker, target)) return;
        PhysicalProfile profile = profileFor(target);
        if (profile == null || profile.pandaFallback) {
            ConnectionState state = connections.computeIfAbsent(target.getUniqueId(), ignored -> new ConnectionState());
            state.fallback = true; state.fallbackReason = "PANDA_DEFAULT";
            return;
        }
        double dx = target.getLocation().getX() - attacker.getLocation().getX();
        double dz = target.getLocation().getZ() - attacker.getLocation().getZ();
        double magnitude = Math.sqrt(dx * dx + dz * dz);
        if (!Double.isFinite(magnitude) || magnitude < 1.0E-5D) {
            double yaw = Math.toRadians(attacker.getLocation().getYaw());
            dx = -Math.sin(yaw);
            dz = Math.cos(yaw);
            magnitude = 1.0D;
        }
        int bonusLevel = attacker.getItemInHand() == null ? 0 : attacker.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK);
        if (attacker.isSprinting()) bonusLevel++;
        pending.put(target.getUniqueId(), new PendingHit(System.nanoTime(), target.getVelocity(), dx / magnitude, dz / magnitude,
                Math.toRadians(attacker.getLocation().getYaw()), bonusLevel, attacker.getUniqueId(), profile));
        if (profile.stopSprintAfterHit && bonusLevel > 0) attacker.setSprinting(false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        if (!enabled) return;
        Player target = event.getPlayer();
        PendingHit hit = pending.remove(target.getUniqueId());
        if (hit == null || System.nanoTime() - hit.createdNanos > MAXIMUM_PENDING_NANOS) return;
        if (!eligibleTarget(target) || !(target.getLastDamageCause() instanceof EntityDamageByEntityEvent)) return;
        EntityDamageByEntityEvent damage = (EntityDamageByEntityEvent) target.getLastDamageCause();
        if (!(damage.getDamager() instanceof Player) || !((Player) damage.getDamager()).getUniqueId().equals(hit.attacker)) return;
        PhysicalProfile profile = hit.profile;
        Vector momentum = profile.inheritExistingMotion ? hit.momentum : new Vector();
        Vector result = new Vector(
                momentum.getX() * profile.horizontalFriction + hit.directionX * profile.horizontal,
                momentum.getY() * profile.verticalFriction + profile.vertical,
                momentum.getZ() * profile.horizontalFriction + hit.directionZ * profile.horizontal);
        if (hit.bonusLevel > 0) {
            result.add(new Vector(-Math.sin(hit.attackerYaw) * hit.bonusLevel * profile.sprintHorizontal,
                    profile.sprintVertical,
                    Math.cos(hit.attackerYaw) * hit.bonusLevel * profile.sprintHorizontal));
        }
        result.setY(Math.max(profile.verticalMinimum, Math.min(profile.verticalLimit, result.getY())));
        if (finite(result)) {
            event.setVelocity(result);
            ConnectionState state = connections.computeIfAbsent(target.getUniqueId(), ignored -> new ConnectionState());
            state.lastVector = result.clone();
            state.lastVelocityTick = serverTick;
            state.fallback = state.connectionClass.equals("UNSTABLE") || state.connectionClass.equals("EXTREME");
            state.fallbackReason = state.fallback ? state.connectionClass + "_IMMEDIATE_DELIVERY" : "NONE";
            if (diagnostics) plugin.getLogger().info("Knockback hit target=" + target.getName() + " profile=" + profile.name + " class=" + state.connectionClass + " ping=" + state.rawPing + " smoothed=" + format(state.smoothed) + " jitter=" + format(state.jitter) + " vector=" + vector(result) + " tick=" + serverTick + " fallback=" + state.fallback + " reason=" + state.fallbackReason);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
        connections.remove(event.getPlayer().getUniqueId());
        tests.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
        connections.remove(event.getPlayer().getUniqueId());
        tests.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        pending.remove(event.getEntity().getUniqueId());
        tests.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
        tests.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
        tests.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
        tests.remove(event.getPlayer().getUniqueId());
    }

    private boolean eligible(Player attacker, Player target) {
        if (attacker.equals(target) || !eligibleTarget(attacker) || !eligibleTarget(target)) return false;
        if (attacker.getWorld() != target.getWorld()) return false;
        if (plugin.getArenaManager().isInCurrentSpawnProtection(attacker.getLocation())) return false;
        if (plugin.getArenaManager().isInCurrentSpawnProtection(target.getLocation())) return false;
        return plugin.getArenaManager().isInCurrentPvpArena(attacker.getLocation())
                && plugin.getArenaManager().isInCurrentPvpArena(target.getLocation());
    }

    private boolean eligibleTarget(Player player) {
        return player.isOnline() && !player.isDead() && player.getHealth() > 0.0D
                && player.getGameMode() != GameMode.SPECTATOR && !player.isInsideVehicle() && !player.hasMetadata("NPC");
    }

    private PhysicalProfile profileFor(Player player) {
        TestOverride override = tests.get(player.getUniqueId());
        if (override != null) {
            if (override.expiresAt > System.currentTimeMillis()) return override.profile;
            tests.remove(player.getUniqueId());
        }
        return activeProfile;
    }

    private PhysicalProfile readProfile(String name) {
        String base = "combat.knockback.profiles." + name + ".";
        boolean fallback = normalize(name).equals("default");
        return validated(new PhysicalProfile(normalize(name), fallback,
                plugin.registry().settings().getDouble(base + "horizontal", 0.4D),
                plugin.registry().settings().getDouble(base + "vertical", 0.4D),
                plugin.registry().settings().getDouble(base + "vertical-max", plugin.registry().settings().getDouble(base + "vertical-limit", 0.4D)),
                plugin.registry().settings().getDouble(base + "vertical-min", -1.0D),
                plugin.registry().settings().getDouble(base + "friction-horizontal", plugin.registry().settings().getDouble(base + "horizontal-friction", 0.5D)),
                plugin.registry().settings().getDouble(base + "friction-vertical", plugin.registry().settings().getDouble(base + "vertical-friction", 0.5D)),
                plugin.registry().settings().getDouble(base + "extra-horizontal", plugin.registry().settings().getDouble(base + "sprint-horizontal", 0.5D)),
                plugin.registry().settings().getDouble(base + "extra-vertical", plugin.registry().settings().getDouble(base + "sprint-vertical", 0.1D)),
                plugin.registry().settings().getBoolean(base + "inherit-existing-motion", true),
                plugin.registry().settings().getBoolean(base + "stop-sprint", plugin.registry().settings().getBoolean(base + "stop-sprint-after-hit", true))));
    }

    private void ensureBuiltIns() {
        PhysicalProfile standard = defaults("fakepixel", false);
        profiles.putIfAbsent("default", defaults("default", true));
        profiles.putIfAbsent("fakepixel", standard);
        for (String name : Arrays.asList("normal", "high_latency", "unstable", "extreme", "auto")) profiles.put(name, profiles.get("fakepixel").named(name));
    }

    private PhysicalProfile defaults(String name, boolean fallback) {
        return new PhysicalProfile(name, fallback, 0.4D, 0.4D, 0.4D, -1.0D, 0.5D, 0.5D, 0.5D, 0.1D, true, true);
    }

    private PhysicalProfile validated(PhysicalProfile value) {
        return validProfile(value) ? value : defaults(value.name, value.pandaFallback);
    }

    private boolean validProfile(PhysicalProfile value) { return finite(value.horizontal) && value.horizontal >= 0.0D && value.horizontal <= 4.0D
            && finite(value.vertical) && value.vertical >= 0.0D && value.vertical <= 2.0D && finite(value.verticalLimit) && value.verticalLimit >= 0.0D && value.verticalLimit <= 2.0D
            && finite(value.verticalMinimum) && value.verticalMinimum >= -4.0D && value.verticalMinimum <= value.verticalLimit
            && finite(value.horizontalFriction) && value.horizontalFriction >= 0.0D && value.horizontalFriction <= 2.0D && finite(value.verticalFriction) && value.verticalFriction >= 0.0D && value.verticalFriction <= 2.0D
            && finite(value.sprintHorizontal) && value.sprintHorizontal >= 0.0D && value.sprintHorizontal <= 4.0D && finite(value.sprintVertical) && value.sprintVertical >= 0.0D && value.sprintVertical <= 2.0D; }

    private boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }

    private void tickConnections() {
        serverTick++;
        if (serverTick % 20L != 0L) return;
        sampleConnections();
    }

    private void sampleConnections() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ConnectionState state = connections.computeIfAbsent(player.getUniqueId(), ignored -> new ConnectionState());
            int ping = rawPing(player);
            state.rawPing = ping;
            if (ping < 0 || ping > 10000) {
                state.add(false, 0, networkSampleCount());
            } else state.add(true, ping, networkSampleCount());
            state.classify(autoClassification, networkMaximumPing(), networkMaximumJitter(), networkTimeouts(), networkRecovery());
        }
    }

    private int networkSampleCount() { return Math.max(3, Math.min(60, plugin.registry().settings().getInt("combat.knockback.network.sample-count", 20))); }
    private int networkMaximumJitter() { return Math.max(1, plugin.registry().settings().getInt("combat.knockback.network.maximum-jitter", 80)); }
    private int networkMaximumPing() { return Math.max(151, plugin.registry().settings().getInt("combat.knockback.network.maximum-ping", 350)); }
    private int networkTimeouts() { return Math.max(1, plugin.registry().settings().getInt("combat.knockback.network.consecutive-timeouts", 3)); }
    private int networkRecovery() { return Math.max(2, plugin.registry().settings().getInt("combat.knockback.network.recovery-samples", 5)); }

    private void resolvePingAccess() {
        if (getHandle != null && pingField != null) return;
        try {
            Class<?> craft = Class.forName("org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer");
            getHandle = craft.getMethod("getHandle");
            Class<?> entityPlayer = Class.forName("net.minecraft.server.v1_8_R3.EntityPlayer");
            pingField = entityPlayer.getField("ping");
        } catch (Throwable failure) {
            getHandle = null;
            pingField = null;
        }
    }

    private int rawPing(Player player) {
        try { return getHandle == null || pingField == null ? -1 : pingField.getInt(getHandle.invoke(player)); }
        catch (Throwable ignored) { return -1; }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH).replace('-', '_').replaceAll("[^a-z0-9_]", "");
    }

    private synchronized boolean persistProfile(PhysicalProfile profile) {
        if (!validProfile(profile)) return false;
        try {
            File file = new File(plugin.getDataFolder(), "settings.yml");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            String base = "combat.knockback.profiles." + profile.name + ".";
            yaml.set(base + "horizontal", profile.horizontal); yaml.set(base + "vertical", profile.vertical);
            yaml.set(base + "vertical-max", profile.verticalLimit); yaml.set(base + "vertical-min", profile.verticalMinimum); yaml.set(base + "friction-horizontal", profile.horizontalFriction);
            yaml.set(base + "friction-vertical", profile.verticalFriction); yaml.set(base + "extra-horizontal", profile.sprintHorizontal);
            yaml.set(base + "extra-vertical", profile.sprintVertical); yaml.set(base + "inherit-existing-motion", profile.inheritExistingMotion);
            yaml.set(base + "stop-sprint", profile.stopSprintAfterHit); yaml.save(file);
            undo.put(profile.name, profiles.get(profile.name));
            profiles.put(profile.name, profile);
            if (profile.name.equals("fakepixel")) for (String alias : Arrays.asList("normal", "high_latency", "unstable", "extreme", "auto")) profiles.put(alias, profile.named(alias));
            if (profile.name.equals(activeProfileName)) activeProfile = profile;
            else if (profile.name.equals("fakepixel") && isLatencyAlias(activeProfileName)) activeProfile = profile.named(activeProfileName);
            return true;
        } catch (Exception failure) {
            plugin.getLogger().warning("Could not save knockback profile: " + failure.getMessage());
            return false;
        }
    }

    private synchronized boolean saveSetting(String path, Object value) {
        try {
            File file = new File(plugin.getDataFolder(), "settings.yml");
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file); yaml.set(path, value); yaml.save(file); return true;
        } catch (Exception failure) { plugin.getLogger().warning("Could not save knockback setting: " + failure.getMessage()); return false; }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("thepit.admin.knockback")) { sender.sendMessage("§cYou do not have permission."); return true; }
        if (args.length == 0) {
            if (!(sender instanceof Player)) sender.sendMessage("§cPlayers only for the GUI. Use /knockback profiles or status.");
            else openMain((Player) sender);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ENGLISH);
        if (action.equals("profiles")) { sender.sendMessage("§6Knockback profiles: §f" + String.join("§7, §f", profiles.keySet())); return true; }
        if (action.equals("reload")) { reload(); sender.sendMessage("§aKnockback profiles reloaded."); return true; }
        if (action.equals("auto") && args.length >= 2) {
            autoClassification = args[1].equalsIgnoreCase("on");
            saveSetting("combat.knockback.auto-classification", autoClassification);
            sender.sendMessage("§aAutomatic connection classification: §f" + autoClassification);
            return true;
        }
        if (action.equals("use") && args.length >= 2) {
            String name = normalize(args[1]); PhysicalProfile profile = profiles.get(name);
            if (profile == null) sender.sendMessage("§cUnknown profile.");
            else { activeProfileName = name; activeProfile = profile; saveSetting("combat.knockback.active-profile", name); sender.sendMessage("§aActive physical profile: §f" + name); }
            return true;
        }
        if (action.equals("edit") && args.length >= 2 && sender instanceof Player) {
            String name = normalize(args[1]); if (isLatencyAlias(name)) name = "fakepixel"; if (!profiles.containsKey(name) || name.equals("default")) sender.sendMessage("§cThat profile cannot be edited."); else openEditor((Player) sender, name); return true;
        }
        if (action.equals("create") && args.length >= 2) {
            String name = normalize(args[1]); String copy = args.length >= 3 ? normalize(args[2]) : "normal";
            if (name.isEmpty() || profiles.containsKey(name) || !profiles.containsKey(copy)) sender.sendMessage("§cInvalid, duplicate, or unknown source profile.");
            else { PhysicalProfile profile = profiles.get(copy).named(name); persistProfile(profile); sender.sendMessage("§aCreated profile §f" + name + "§a."); }
            return true;
        }
        if (action.equals("delete") && args.length >= 2) {
            String name = normalize(args[1]);
            if (isBuiltIn(name) || !profiles.containsKey(name) || name.equals(activeProfileName)) sender.sendMessage("§cBuilt-in, missing, or active profiles cannot be deleted.");
            else {
                try { File file = new File(plugin.getDataFolder(), "settings.yml"); YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file); yaml.set("combat.knockback.profiles." + name, null); yaml.save(file); profiles.remove(name); undo.remove(name); sender.sendMessage("§aDeleted §f" + name + "§a."); }
                catch (Exception failure) { sender.sendMessage("§cCould not delete the profile."); }
            }
            return true;
        }
        if (action.equals("reset") && args.length >= 2) {
            String name = normalize(args[1]); if (!profiles.containsKey(name) || name.equals("default")) sender.sendMessage("§cUnknown or immutable profile.");
            else { persistProfile(defaults(name, false)); sender.sendMessage("§aReset §f" + name + "§a."); }
            return true;
        }
        if (action.equals("status")) {
            Player target = args.length >= 2 ? Bukkit.getPlayer(args[1]) : sender instanceof Player ? (Player) sender : null;
            if (target == null) sender.sendMessage("§cPlayer not found."); else sendStatus(sender, target); return true;
        }
        if (action.equals("test") && args.length >= 3) {
            Player target = Bukkit.getPlayer(args[1]); PhysicalProfile profile = profiles.get(normalize(args[2]));
            if (target == null || profile == null) sender.sendMessage("§cUnknown player or profile.");
            else { long seconds = Math.max(5L, Math.min(300L, plugin.registry().settings().getLong("combat.knockback.test-duration-seconds", 60L))); tests.put(target.getUniqueId(), new TestOverride(profile, System.currentTimeMillis() + seconds * 1000L)); sender.sendMessage("§e" + target.getName() + "§a will use §f" + profile.name + "§a for §f" + seconds + "§a seconds."); }
            return true;
        }
        sender.sendMessage("§c/knockback <profiles|use|edit|create|delete|reset|reload|status|test|auto>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("thepit.admin.knockback")) return Collections.emptyList();
        if (args.length == 1) return starts(Arrays.asList("profiles", "use", "edit", "create", "delete", "reset", "reload", "status", "test", "auto"), args[0]);
        if (args.length == 2 && Arrays.asList("use", "edit", "delete", "reset").contains(args[0].toLowerCase(Locale.ENGLISH))) return starts(new ArrayList<>(profiles.keySet()), args[1]);
        if (args.length == 2 && (args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("test"))) return null;
        if (args.length == 2 && args[0].equalsIgnoreCase("auto")) return starts(Arrays.asList("on", "off"), args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("test")) return starts(new ArrayList<>(profiles.keySet()), args[2]);
        return Collections.emptyList();
    }

    private List<String> starts(List<String> values, String prefix) { List<String> result = new ArrayList<>(); for (String value : values) if (value.startsWith(prefix.toLowerCase(Locale.ENGLISH))) result.add(value); return result; }
    private boolean isBuiltIn(String name) { return Arrays.asList("default", "fakepixel", "normal", "high_latency", "unstable", "extreme", "auto").contains(name); }
    private boolean isLatencyAlias(String name) { return Arrays.asList("normal", "high_latency", "unstable", "extreme", "auto").contains(name); }

    private void sendStatus(CommandSender sender, Player player) {
        ConnectionState state = connections.computeIfAbsent(player.getUniqueId(), ignored -> new ConnectionState());
        TestOverride test = tests.get(player.getUniqueId());
        sender.sendMessage("§6Knockback status for §f" + player.getName());
        sender.sendMessage("§7Physical profile: §f" + (test == null ? activeProfileName : test.profile.name + " §e(TEST)"));
        sender.sendMessage("§7Connection class: §f" + state.connectionClass + " §7raw/smoothed/jitter: §f" + state.rawPing + "/" + format(state.smoothed) + "/" + format(state.jitter) + "ms");
        sender.sendMessage("§7Recent timeouts: §f" + state.timeouts + " §7rate: §f" + format(state.timeoutRate * 100.0D) + "% §7last vector: §f" + (state.lastVector == null ? "none" : vector(state.lastVector)));
        sender.sendMessage("§7Velocity-send tick: §f" + state.lastVelocityTick + " §7fallback: §f" + state.fallback + " §7reason: §f" + state.fallbackReason);
    }

    private void openMain(Player player) {
        KnockbackHolder holder = new KnockbackHolder(player.getUniqueId(), null);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§8Knockback Administration"); holder.inventory = inventory; fill(inventory);
        inventory.setItem(10, item(Material.ANVIL, "§aActive: §f" + activeProfileName, Arrays.asList("§7Physical values never vary by ping.")));
        inventory.setItem(12, item(Material.REDSTONE_COMPARATOR, autoClassification ? "§aAuto Classification: ON" : "§cAuto Classification: OFF", Arrays.asList("§eClick to toggle")));
        inventory.setItem(14, item(Material.BOOK, "§bOnline Connection Classes", connectionLore()));
        inventory.setItem(16, item(Material.NETHER_STAR, "§eReload", Arrays.asList("§7Reload profiles from settings.yml")));
        inventory.setItem(22, item(Material.REDSTONE_TORCH_ON, diagnostics ? "§aDiagnostics: ON" : "§cDiagnostics: OFF", Arrays.asList("§eClick to toggle")));
        inventory.setItem(24, item(Material.BARRIER, "§cRestore PandaSpigot Defaults", Arrays.asList("§7Selects the immutable default profile")));
        int slot = 27; for (String name : profiles.keySet()) { if (slot >= 54) break; inventory.setItem(slot++, item(Material.PAPER, "§f" + name, Arrays.asList("§7Left-click: use", "§7Right-click: edit"))); }
        player.openInventory(inventory);
    }

    private void openEditor(Player player, String name) {
        PhysicalProfile profile = profiles.get(name); if (profile == null || profile.pandaFallback) { openMain(player); return; }
        KnockbackHolder holder = new KnockbackHolder(player.getUniqueId(), name);
        Inventory inventory = Bukkit.createInventory(holder, 27, "§8Edit Knockback: " + name); holder.inventory = inventory; fill(inventory);
        String[] labels = {"Horizontal", "Vertical", "Vertical Max", "Vertical Min", "Friction Horizontal", "Friction Vertical", "Extra Horizontal", "Extra Vertical"};
        double[] values = {profile.horizontal, profile.vertical, profile.verticalLimit, profile.verticalMinimum, profile.horizontalFriction, profile.verticalFriction, profile.sprintHorizontal, profile.sprintVertical};
        for (int i = 0; i < 8; i++) inventory.setItem(10 + i, item(Material.INK_SACK, "§b" + labels[i] + ": §f" + format(values[i]), controlLore()));
        inventory.setItem(19, item(Material.LEVER, "§eExisting Motion: §f" + profile.inheritExistingMotion, Arrays.asList("§eClick to toggle")));
        inventory.setItem(20, item(Material.LEVER, "§eSprint Reset: §f" + profile.stopSprintAfterHit, Arrays.asList("§eClick to toggle")));
        inventory.setItem(22, item(Material.EMERALD_BLOCK, "§aApply", Arrays.asList("§7Changes already apply to the next hit.")));
        inventory.setItem(23, item(Material.WATCH, "§eUndo Last Change", Collections.emptyList()));
        inventory.setItem(24, item(Material.TNT, "§cReset Profile", Collections.emptyList()));
        inventory.setItem(26, item(Material.ARROW, "§7Return", Collections.emptyList()));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KnockbackHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked(); KnockbackHolder holder = (KnockbackHolder) event.getInventory().getHolder();
        if (!holder.viewer.equals(player.getUniqueId()) || !player.hasPermission("thepit.admin.knockback") || event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;
        int slot = event.getRawSlot();
        long now = System.nanoTime(); if (holder.lastSlot == slot && now - holder.lastClickNanos < 75_000_000L) return; holder.lastSlot = slot; holder.lastClickNanos = now;
        if (holder.profile == null) {
            if (slot == 12) { autoClassification = !autoClassification; saveSetting("combat.knockback.auto-classification", autoClassification); openMain(player); }
            else if (slot == 16) { reload(); openMain(player); }
            else if (slot == 22) { diagnostics = !diagnostics; saveSetting("combat.knockback.diagnostics", diagnostics); openMain(player); }
            else if (slot == 24) { activeProfileName = "default"; activeProfile = profiles.get("default"); saveSetting("combat.knockback.active-profile", "default"); openMain(player); }
            else if (slot >= 27) {
                ItemStack clicked = event.getCurrentItem(); if (clicked == null || !clicked.hasItemMeta()) return; String name = normalize(org.bukkit.ChatColor.stripColor(clicked.getItemMeta().getDisplayName()));
                if (event.isRightClick()) openEditor(player, isLatencyAlias(name) ? "fakepixel" : name); else { activeProfileName = name; activeProfile = profiles.get(name); saveSetting("combat.knockback.active-profile", name); openMain(player); }
            }
            return;
        }
        PhysicalProfile profile = profiles.get(holder.profile); if (profile == null) return;
        if (slot >= 10 && slot <= 17) {
            int field = slot - 10; double step = event.isShiftClick() ? 0.1D : 0.01D; if (event.isRightClick()) step = -step;
            PhysicalProfile changed = event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE ? defaults(profile.name, false).with(field, defaults(profile.name, false).value(field)) : profile.with(field, profile.value(field) + step);
            changed = validated(changed); if (persistProfile(changed)) openEditor(player, profile.name); return;
        }
        if (slot == 19) { persistProfile(profile.motion(!profile.inheritExistingMotion)); openEditor(player, profile.name); }
        else if (slot == 20) { persistProfile(profile.sprintReset(!profile.stopSprintAfterHit)); openEditor(player, profile.name); }
        else if (slot == 23) { PhysicalProfile previous = undo.remove(profile.name); if (previous != null) { profiles.put(profile.name, previous); if (profile.name.equals(activeProfileName)) activeProfile = previous; persistProfile(previous); } openEditor(player, profile.name); }
        else if (slot == 24) { persistProfile(defaults(profile.name, false)); openEditor(player, profile.name); }
        else if (slot == 26) openMain(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMenuDrag(InventoryDragEvent event) { if (event.getInventory().getHolder() instanceof KnockbackHolder) event.setCancelled(true); }

    private void fill(Inventory inventory) { ItemStack filler = item(Material.STAINED_GLASS_PANE, " ", Collections.emptyList()); for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler); }
    private ItemStack item(Material material, String name, List<String> lore) { ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); meta.setLore(lore); item.setItemMeta(meta); return item; }
    private List<String> controlLore() { return Arrays.asList("§7Left: +0.01", "§7Right: -0.01", "§7Shift: ±0.10", "§7Middle: reset"); }
    private List<String> connectionLore() { List<String> lore = new ArrayList<>(); for (Player player : Bukkit.getOnlinePlayers()) { ConnectionState state = connections.get(player.getUniqueId()); lore.add("§7" + player.getName() + ": §f" + (state == null ? "SAMPLING" : state.connectionClass)); if (lore.size() >= 20) break; } return lore; }
    private String format(double value) { return String.format(Locale.ROOT, "%.3f", value); }
    private String vector(Vector value) { return format(value.getX()) + "," + format(value.getY()) + "," + format(value.getZ()); }

    private boolean finite(Vector vector) {
        return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
    }

    private static final class PendingHit {
        private final long createdNanos;
        private final Vector momentum;
        private final double directionX;
        private final double directionZ;
        private final double attackerYaw;
        private final int bonusLevel;
        private final UUID attacker;
        private final PhysicalProfile profile;

        private PendingHit(long createdNanos, Vector momentum, double directionX, double directionZ, double attackerYaw, int bonusLevel, UUID attacker, PhysicalProfile profile) {
            this.createdNanos = createdNanos;
            this.momentum = momentum.clone();
            this.directionX = directionX;
            this.directionZ = directionZ;
            this.attackerYaw = attackerYaw;
            this.bonusLevel = bonusLevel;
            this.attacker = attacker;
            this.profile = profile;
        }
    }

    private static final class PhysicalProfile {
        private final String name; private final boolean pandaFallback; private final double horizontal; private final double vertical;
        private final double verticalLimit; private final double verticalMinimum; private final double horizontalFriction; private final double verticalFriction;
        private final double sprintHorizontal; private final double sprintVertical; private final boolean inheritExistingMotion; private final boolean stopSprintAfterHit;
        private PhysicalProfile(String name, boolean pandaFallback, double horizontal, double vertical, double verticalLimit, double verticalMinimum, double horizontalFriction, double verticalFriction, double sprintHorizontal, double sprintVertical, boolean inheritExistingMotion, boolean stopSprintAfterHit) {
            this.name=name;this.pandaFallback=pandaFallback;this.horizontal=horizontal;this.vertical=vertical;this.verticalLimit=verticalLimit;this.verticalMinimum=verticalMinimum;this.horizontalFriction=horizontalFriction;this.verticalFriction=verticalFriction;this.sprintHorizontal=sprintHorizontal;this.sprintVertical=sprintVertical;this.inheritExistingMotion=inheritExistingMotion;this.stopSprintAfterHit=stopSprintAfterHit;
        }
        private PhysicalProfile named(String value){return new PhysicalProfile(value,value.equals("default"),horizontal,vertical,verticalLimit,verticalMinimum,horizontalFriction,verticalFriction,sprintHorizontal,sprintVertical,inheritExistingMotion,stopSprintAfterHit);}
        private double value(int field){return switch(field){case 0->horizontal;case 1->vertical;case 2->verticalLimit;case 3->verticalMinimum;case 4->horizontalFriction;case 5->verticalFriction;case 6->sprintHorizontal;default->sprintVertical;};}
        private PhysicalProfile with(int field,double value){double[] v={horizontal,vertical,verticalLimit,verticalMinimum,horizontalFriction,verticalFriction,sprintHorizontal,sprintVertical};v[field]=value;return new PhysicalProfile(name,pandaFallback,v[0],v[1],v[2],v[3],v[4],v[5],v[6],v[7],inheritExistingMotion,stopSprintAfterHit);}
        private PhysicalProfile motion(boolean value){return new PhysicalProfile(name,pandaFallback,horizontal,vertical,verticalLimit,verticalMinimum,horizontalFriction,verticalFriction,sprintHorizontal,sprintVertical,value,stopSprintAfterHit);}
        private PhysicalProfile sprintReset(boolean value){return new PhysicalProfile(name,pandaFallback,horizontal,vertical,verticalLimit,verticalMinimum,horizontalFriction,verticalFriction,sprintHorizontal,sprintVertical,inheritExistingMotion,value);}
    }

    private static final class ConnectionState {
        private final Deque<Integer> samples=new ArrayDeque<>(); private int rawPing=-1; private double smoothed; private double jitter; private int timeouts; private double timeoutRate; private int consecutiveFailures; private int healthy; private String connectionClass="SAMPLING"; private String candidate="SAMPLING"; private int candidateCount; private Vector lastVector; private long lastVelocityTick; private boolean fallback; private String fallbackReason="NONE";
        private void add(boolean success,int ping,int maximum){samples.addLast(success?ping:-1);while(samples.size()>maximum)samples.removeFirst();timeouts=0;for(Integer sample:samples)if(sample<0)timeouts++;timeoutRate=samples.isEmpty()?0.0D:timeouts/(double)samples.size();if(!success){consecutiveFailures++;healthy=0;return;}consecutiveFailures=0;healthy++;if(smoothed==0.0D)smoothed=ping;else{jitter=jitter*0.75D+Math.abs(ping-smoothed)*0.25D;smoothed=smoothed*0.8D+ping*0.2D;}}
        private void classify(boolean automatic,int maxPing,int maxJitter,int maxTimeouts,int recovery){String next;if(!automatic)next="NORMAL";else if(smoothed>maxPing||consecutiveFailures>=maxTimeouts*2)next="EXTREME";else if(jitter>maxJitter||consecutiveFailures>=maxTimeouts)next="UNSTABLE";else if(smoothed>150.0D)next="HIGH_LATENCY";else next="NORMAL";if(!next.equals(candidate)){candidate=next;candidateCount=1;}else candidateCount++;int needed=(next.equals("NORMAL")||next.equals("HIGH_LATENCY"))?recovery:3;if(candidateCount>=needed&&(next.equals("UNSTABLE")||next.equals("EXTREME")||healthy>=needed))connectionClass=next;}
    }

    private static final class TestOverride { private final PhysicalProfile profile; private final long expiresAt; private TestOverride(PhysicalProfile profile,long expiresAt){this.profile=profile;this.expiresAt=expiresAt;} }
    private static final class KnockbackHolder implements InventoryHolder { private final UUID viewer; private final String profile; private Inventory inventory; private int lastSlot=-1; private long lastClickNanos; private KnockbackHolder(UUID viewer,String profile){this.viewer=viewer;this.profile=profile;} public Inventory getInventory(){return inventory;} }
}
