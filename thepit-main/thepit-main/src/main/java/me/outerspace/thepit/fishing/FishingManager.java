package me.outerspace.thepit.fishing;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FishingManager implements Listener {
    private final ThePitPlugin plugin;
    private final File arenasFile;
    private final File fallbackFile;
    private final Random random = new Random();
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, DebugState> debug = new ConcurrentHashMap<>();
    private final Map<UUID, Fish> activeHooks = new ConcurrentHashMap<>();
    private final Map<UUID, HookSession> hookSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final Set<Integer> processedHooks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, Set<UUID>> debugWatchers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> interactionGuard = new HashMap<>();
    private final Map<UUID, Long> combatGuard = new HashMap<>();
    private final Map<UUID, Long> stereoGuard = new HashMap<>();
    private final Map<UUID, ItemStack> pendingAqua = new HashMap<>();
    private volatile Map<String, ArenaFishing> arenas = Collections.emptyMap();
    private YamlConfiguration fallback;
    private BukkitTask waterTask;

    public FishingManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        fallbackFile = new File(plugin.getDataFolder(), "fishing-profiles.yml");
        reload();
        loadFallback();
    }

    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) load(player);
        if (waterTask == null) waterTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sampleHooks, 1L, 1L);
    }

    public void shutdown() {
        if (waterTask != null) {
            waterTask.cancel();
            waterTask = null;
        }
        removeHooks();
        hookSessions.clear();
        processedHooks.clear();
        debugWatchers.clear();
        interactionGuard.clear();
        combatGuard.clear();
        stereoGuard.clear();
        pendingAqua.clear();
        for (Map.Entry<UUID, Profile> entry : profiles.entrySet()) save(entry.getKey(), entry.getValue());
        saveFallback();
        profiles.clear();
    }

    public void arenaCleanup() {
        removeHooks();
        hookSessions.clear();
        processedHooks.clear();
        debug.clear();
    }

    public void reload() {
        processedHooks.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(arenasFile);
        Map<String, ArenaFishing> loaded = new LinkedHashMap<>();
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root != null) {
            for (String rawArena : root.getKeys(false)) {
                String arena = normalize(rawArena);
                ConfigurationSection fishing = root.getConfigurationSection(rawArena + ".fishing");
                boolean enabled = fishing != null && fishing.getBoolean("enabled", false);
                Map<String, Cuboid> regions = new LinkedHashMap<>();
                ConfigurationSection regionRoot = fishing == null ? null : fishing.getConfigurationSection("regions");
                if (regionRoot != null) {
                    for (String rawRegion : regionRoot.getKeys(false)) {
                        ConfigurationSection region = regionRoot.getConfigurationSection(rawRegion);
                        LocationPoint pos1 = point(region == null ? null : region.getConfigurationSection("pos1"));
                        LocationPoint pos2 = point(region == null ? null : region.getConfigurationSection("pos2"));
                        if (pos1 != null && pos2 != null && pos1.world.equalsIgnoreCase(pos2.world)) regions.put(normalize(rawRegion), new Cuboid(pos1, pos2));
                    }
                }
                loaded.put(arena, new ArenaFishing(enabled, regions));
            }
        }
        arenas = Collections.unmodifiableMap(loaded);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Profile profile = profiles.remove(uuid);
        if (profile != null) save(uuid, profile);
        debug.remove(uuid);
        debugWatchers.remove(uuid);
        for (Set<UUID> watchers : debugWatchers.values()) watchers.remove(uuid);
        interactionGuard.remove(uuid);
        combatGuard.remove(uuid);
        stereoGuard.remove(uuid);
        Fish hook = activeHooks.remove(uuid);
        hookSessions.remove(uuid);
        if (hook != null && !hook.isDead()) hook.remove();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            if (event.getHook() != null && activeArenaPlayer(player)) {
                activeHooks.put(player.getUniqueId(), event.getHook());
                hookSessions.put(player.getUniqueId(), new HookSession(plugin.getArenaManager().getCurrentArena(), plugin.getArenaManager().getCurrentWorld(), event.getHook().getEntityId()));
            }
            double speed = speedIncrease(player);
            if (speed > 0.0D) FishingTimingCompatibility.accelerate(plugin, event.getHook(), speed);
            return;
        }
        activeHooks.remove(player.getUniqueId());
        HookSession hookSession = hookSessions.remove(player.getUniqueId());
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            if (event.getCaught() instanceof Player && !isNpc((Player) event.getCaught())) applyHookEnchant(player, (Player) event.getCaught());
            else if (event.getCaught() != null) event.setCancelled(true);
            return;
        }
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!activeArenaPlayer(player) || event.getHook() == null || event.getCaught() == null) return;
        Entity caught = event.getCaught();
        if (caught != null) caught.remove();
        event.setCancelled(true);
        if (event.getHook() == null || !processedHooks.add(event.getHook().getEntityId())) {
            reject(player, event, "duplicate-hook", false);
            return;
        }
        int hookId = event.getHook().getEntityId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> processedHooks.remove(hookId), 200L);
        Profile profile = profile(player);
        if (profile == null) {
            reject(player, event, "profile-not-loaded", false);
            return;
        }
        String arenaId = plugin.getArenaManager().getCurrentArena();
        Location playerLocation = player.getLocation();
        Location hookLocation = event.getHook().getLocation();
        String playerRegion = "active-arena";
        String hookRegion = isWater(hookLocation) ? "automatic-water" : null;
        String access = access(player);
        DebugState state = new DebugState(arenaId, true, playerRegion, hookRegion,
                hookLocation.getBlock().getType().name(), hookLocation.getBlockY(), access, clubTier(player),
                plugin.getStatsManager().getPrestige(player.getUniqueId()), catchesToday(profile), limit(player), profile.lastReward, "");
        debug.put(player.getUniqueId(), state);
        if (!plugin.registry().settings().getBoolean("fishing.enabled", true)) {
            reject(player, event, "globally-disabled", false);
            return;
        }
        if (hookSession == null || hookSession.hookId != event.getHook().getEntityId() || !hookSession.matches(arenaId, plugin.getArenaManager().getCurrentWorld()) || !hookSession.enteredWater || hookSession.invalidWater) {
            reject(player, event, "invalid-water-history", true);
            return;
        }
        if (player.getWorld() != hookLocation.getWorld() || hookLocation.getWorld() != plugin.getArenaManager().getCurrentWorld()) {
            reject(player, event, "inactive-arena", true);
            return;
        }
        if (!isWater(hookLocation)) {
            reject(player, event, "hook-not-in-water", true);
            return;
        }
        if (playerLocation.getY() > maximumY()) {
            reject(player, event, "too-high", true);
            return;
        }
        if (access.equals("none")) {
            reject(player, event, "no-access", true);
            return;
        }
        normalizeDate(profile);
        if (profile.catches >= limit(player)) {
            reject(player, event, "daily-limit", true);
            return;
        }
        long now = System.currentTimeMillis();
        long minimum = Math.max(0L, plugin.registry().settings().getLong("fishing.anti-automation.minimum-success-interval-millis", 2500L));
        if (now - profile.lastSuccess < minimum) {
            profile.repeatedTiming++;
            reject(player, event, "minimum-interval", false);
            return;
        }
        long interval = profile.lastSuccess <= 0L ? 0L : now - profile.lastSuccess;
        long timingWindow = Math.max(0L, plugin.registry().settings().getLong("fishing.anti-automation.repeated-timing-window-millis", 150L));
        if (interval > 0L && profile.lastInterval > 0L && Math.abs(interval - profile.lastInterval) <= timingWindow) profile.repeatedTiming++;
        else profile.repeatedTiming = 0;
        profile.lastInterval = interval;
        if (profile.repeatedTiming >= Math.max(2, plugin.registry().settings().getInt("fishing.anti-automation.repeated-timing-threshold", 4))) {
            reject(player, event, "repeated-timing", false);
            saveAsync(player.getUniqueId(), profile);
            return;
        }
        Reward reward = roll(player, profile);
        if (reward == null || !deliver(player, reward)) {
            reject(player, event, "reward-delivery-failed", false);
            return;
        }
        profile.catches++;
        profile.lastSuccess = now;
        profile.lastReward = reward.id;
        plugin.getStatsManager().addLongStat(player.getUniqueId(), me.outerspace.thepit.managers.StatsManager.FISHED_ANYTHING, 1L);
        if (reward.type.equals("FISH")) plugin.getStatsManager().addLongStat(player.getUniqueId(), me.outerspace.thepit.managers.StatsManager.FISHED_FISH, 1L);
        if (plugin.getContractManager() != null) plugin.getContractManager().onFishingReward(player, reward.id, reward.material);
        if (plugin.getAchievementManager() != null) plugin.getAchievementManager().publish(
                new me.outerspace.thepit.managers.AchievementManager.ProgressEvent(player.getUniqueId(),
                        me.outerspace.thepit.managers.AchievementManager.EventType.FISHING_REWARD,
                        1L, java.util.Map.of("id", reward.id, "special_fish", reward.type.equals("FISH"),
                        "fresh_aqua_pants", reward.type.equals("AQUA"), "fishing_club_tier", clubTier(player)), false));
        if (clubTier(player) >= 2) {
            int xp = Math.max(0, plugin.getRenownShopManager().effect("fishing-club", "catch-xp",
                    plugin.registry().settings().getInt("fishing.club.catch-xp", 25)));
            plugin.getStatsManager().addXp(player, xp, me.outerspace.thepit.managers.StatsManager.XpSource.FISHING);
            message(player, "xp", "&b+%amount% XP", "%amount%", String.valueOf(xp));
        }
        long stereoNow = System.currentTimeMillis();
        if (hasAqua(player.getInventory().getLeggings(), "stereo")
                && stereoNow - stereoGuard.getOrDefault(player.getUniqueId(), 0L) >= plugin.registry().settings().getLong("fishing.aqua-enchants.stereo.cooldown-millis", 3000L)) {
            stereoGuard.put(player.getUniqueId(), stereoNow);
            try { player.getWorld().playSound(player.getLocation(), Sound.NOTE_PLING, 0.8F, 0.8F + random.nextFloat() * 0.8F); } catch (Throwable ignored) { }
        }
        saveAsync(player.getUniqueId(), profile);
        updateSuccess(player, profile);
        notifyDebugWatchers(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        String id = SecureItemTag.string(item, "ThePitItem");
        if (!id.equals("kloonfish") && !id.equals("salmon")) return;
        long now = System.currentTimeMillis();
        if (now - interactionGuard.getOrDefault(event.getPlayer().getUniqueId(), 0L) < 250L) return;
        interactionGuard.put(event.getPlayer().getUniqueId(), now);
        event.setCancelled(true);
        Player player = event.getPlayer();
        consumeOne(player, item);
        if (id.equals("kloonfish")) {
            double amount = Math.max(0.0D, plugin.registry().settings().getDouble("fishing.special-fish.kloonfish.absorption-hearts", 1.5D)) * 2.0D;
            setAbsorption(player, amount);
        } else {
            int ticks = Math.max(1, plugin.registry().settings().getInt("fishing.special-fish.salmon.duration-seconds", 3)) * 20;
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, ticks, 2), true);
        }
        title(player, messageValue("nom-nom", "&aNom nom!"));
        if (random.nextDouble() < plugin.registry().settings().getDouble("fishing.special-fish.burp-chance", 0.03D)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> title(player, messageValue("burp", "&6Burp!")), 10L);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 4), true);
        }
        player.playSound(player.getLocation(), Sound.EAT, 1.0F, 1.0F);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAquaEnchant(EnchantItemEvent event) {
        ItemStack item = event.getItem();
        if (!SecureItemTag.is(item, "fresh_aqua_pants")) return;
        event.setCancelled(true);
        Player player = event.getEnchanter();
        if (SecureItemTag.getInteger(item, "AquaTier", 0) > 0) {
            player.sendMessage(CC.color("&cFresh Aqua Pants can only be enchanted once."));
            return;
        }
        int cost = Math.max(0, plugin.registry().settings().getInt("fishing.aqua-pants.enchant-cost-gold", 5000));
        if (!plugin.takeGold(player, cost)) {
            player.sendMessage(CC.color("&cYou need " + cost + " gold to enchant Fresh Aqua Pants."));
            return;
        }
        String enchant = randomAquaEnchant();
        ItemStack enchanted = SecureItemTag.integer(item, "AquaTier", 1);
        enchanted = SecureItemTag.integer(enchanted, "MysticLives", 5);
        enchanted = SecureItemTag.string(enchanted, "ClubRod", "true");
        enchanted = SecureItemTag.string(enchanted, "AquaEnchant", enchant);
        applyAquaLore(enchanted, enchant);
        event.getInventory().setItem(0, enchanted);
        player.sendMessage(CC.color("&bYour Fresh Aqua Pants received Club Rod and " + displayEnchant(enchant) + "."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAquaCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        boolean hook = false;
        if (event.getDamager() instanceof Player) attacker = (Player) event.getDamager();
        else if (event.getDamager() instanceof Fish fish) {
            if (fish.getShooter() instanceof Player) attacker = (Player) fish.getShooter();
            hook = true;
        }
        if (attacker == null || attacker.equals(victim)) return;
        if (isNpc(attacker) || isNpc(victim)) return;
        ItemStack attackerPants = attacker.getInventory().getLeggings();
        ItemStack victimPants = victim.getInventory().getLeggings();
        if (!hook && isAqua(attackerPants) && isAqua(victimPants) && (hasAqua(attackerPants, "unite") || hasAqua(victimPants, "unite"))) {
            event.setCancelled(true);
            return;
        }
        if (hook && isAqua(attackerPants)) {
            long now = System.currentTimeMillis();
            if (now - combatGuard.getOrDefault(attacker.getUniqueId(), 0L) < 500L) return;
            combatGuard.put(attacker.getUniqueId(), now);
            if (hasAqua(attackerPants, "grandmaster")) {
                event.setCancelled(true);
                try { victim.setHealth(Math.max(0.0D, victim.getHealth() - 1.0D)); } catch (Throwable ignored) { }
            }
            if (hasAqua(attackerPants, "rodback")) victim.setVelocity(victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize().multiply(1.2D).setY(0.35D));
        }
        if (isAqua(victimPants) && isAqua(attackerPants) && hasAqua(attackerPants, "rogue")) event.setDamage(event.getDamage() * (1.0D + plugin.registry().settings().getDouble("fishing.aqua-enchants.rogue.damage-increase", 0.25D)));
        if (isAqua(victimPants) && hasAqua(victimPants, "rogue")) event.setDamage(event.getDamage() * (1.0D - plugin.registry().settings().getDouble("fishing.aqua-enchants.rogue.damage-reduction", 0.10D)));
        if (isAqua(victimPants) && hasAqua(victimPants, "tough_crew")
                && aquaNearby(victim) >= plugin.registry().settings().getInt("fishing.aqua-enchants.tough-crew.nearby-required", 3)) {
            event.setDamage(event.getDamage() * (1.0D - plugin.registry().settings().getDouble("fishing.aqua-enchants.tough-crew.damage-reduction", 0.15D)));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClubMenu(InventoryClickEvent event) {
        if (!event.getInventory().getTitle().equals(CC.color("&8Fishing Club"))) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int tier = event.getRawSlot() / 2;
        if (tier < 1 || tier > 5 || event.getRawSlot() != tier * 2) return;
        purchaseTier(player, tier);
        openClub(player);
    }

    public void openClub(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, CC.color("&8Fishing Club"));
        Profile profile = profile(player);
        int current = clubTier(player);
        for (int tier = 1; tier <= 5; tier++) {
            Material material = tier <= current ? Material.EMERALD_BLOCK : Material.INK_SACK;
            ItemStack icon = new ItemStack(material, 1, (short) (tier <= current ? 0 : 8));
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(CC.color("&bFishing Club " + roman(tier)));
            List<String> lore = new ArrayList<>();
            lore.add(CC.color("&7" + effect(tier)));
            lore.add("");
            lore.add(CC.color("&7Purchased in the &eRenown Shop&7."));
            lore.add(CC.color("&7Required prestige: &e" + requiredPrestige(tier)));
            lore.add(CC.color("&7Prerequisite: &f" + (tier == 1 ? "None" : "Fishing Club " + roman(tier - 1))));
            lore.add(CC.color(tier <= current ? "&aPurchased" : tier == current + 1 ? "&eClick to purchase" : "&cUnavailable"));
            meta.setLore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(tier * 2, icon);
        }
        player.openInventory(inventory);
    }

    public void syncAquaLoadout(Player player) {
        ItemStack pants = player.getInventory().getLeggings();
        removeItem(player, "club_rod");
        if (isAqua(pants) && SecureItemTag.string(pants, "ClubRod").equals("true") && !containsItem(player, "fishing_rod")) {
            ItemStack rod = SecureItemTag.string(SecureItemTag.unbreakable(new ItemStack(Material.FISHING_ROD)), "ThePitItem", "club_rod");
            player.getInventory().addItem(rod);
        }
        if (!hasAqua(pants, "portable_pond")) {
            removeItem(player, "portable_pond");
            return;
        }
        for (ItemStack item : player.getInventory().getContents()) if (SecureItemTag.is(item, "portable_pond")) return;
        ItemStack bucket = new ItemStack(Material.WATER_BUCKET);
        bucket = SecureItemTag.string(bucket, "ThePitItem", "portable_pond");
        bucket = SecureItemTag.integer(bucket, "PondUses", Math.max(1, plugin.registry().settings().getInt("fishing.aqua-enchants.portable-pond.bucket-uses", 4)));
        giveOrDrop(player, bucket);
    }

    public void captureAquaDeath(Player player) {
        ItemStack pants = player.getInventory().getLeggings();
        if (!isAqua(pants)) return;
        ItemStack saved = pants.clone();
        int tier = SecureItemTag.getInteger(saved, "AquaTier", 0);
        if (tier > 0) {
            int lives = SecureItemTag.getInteger(saved, "MysticLives", 5) - 1;
            if (lives <= 0) {
                player.sendMessage(CC.color("&cYour Aqua Pants ran out of lives."));
                return;
            }
            saved = SecureItemTag.integer(saved, "MysticLives", lives);
            replaceLivesLore(saved, lives);
        }
        pendingAqua.put(player.getUniqueId(), saved);
    }

    public void restoreAquaDeath(Player player) {
        ItemStack saved = pendingAqua.remove(player.getUniqueId());
        if (saved == null || !player.isOnline()) return;
        player.getInventory().setLeggings(saved);
        syncAquaLoadout(player);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortablePond(PlayerBucketEmptyEvent event) {
        ItemStack item = event.getPlayer().getItemInHand();
        if (!SecureItemTag.is(item, "portable_pond")) return;
        int uses = SecureItemTag.getInteger(item, "PondUses", 1);
        if (uses <= 1) return;
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack bucket = new ItemStack(Material.WATER_BUCKET);
            bucket = SecureItemTag.string(bucket, "ThePitItem", "portable_pond");
            bucket = SecureItemTag.integer(bucket, "PondUses", uses - 1);
            player.setItemInHand(bucket);
            player.updateInventory();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorInventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getInventory().getTitle().equals(CC.color("&8Fishing Club"))) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) syncAquaLoadout(player);
        });
    }

    public boolean handleCommand(org.bukkit.command.CommandSender sender, String[] args) {
        if (!me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.fishing") && !me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.fishing")) {
            sender.sendMessage("§cYou do not have permission.");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player) openClub((Player) sender);
            else sender.sendMessage("§c/pit fishing stats <player>");
            return true;
        }
        String action = args[0].toLowerCase(Locale.ENGLISH);
        if (action.equals("club") && args.length == 1 && sender instanceof Player) {
            openClub((Player) sender);
            return true;
        }
        if (action.equals("stats") || action.equals("club")) {
            Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player ? (Player) sender : null;
            if (target == null) sender.sendMessage("§cThat player must be online.");
            else sendStats(sender, target);
            return true;
        }
        if (!me.outerspace.thepit.util.PermissionCompat.has(plugin, sender, "thepit.admin.fishing")) {
            sender.sendMessage("§cYou do not have permission.");
            return true;
        }
        if (action.equals("reload")) {
            reload();
            sender.sendMessage("§aFishing configuration reloaded.");
            return true;
        }
        if (action.equals("debug")) {
            Player target = args.length >= 2 ? Bukkit.getPlayerExact(args[1]) : sender instanceof Player ? (Player) sender : null;
            if (target == null) sender.sendMessage("§cThat player must be online.");
            else {
                if (!(sender instanceof Player)) {
                    sendDebug(sender, target);
                    return true;
                }
                UUID watcher = ((Player) sender).getUniqueId();
                Set<UUID> watchers = debugWatchers.computeIfAbsent(target.getUniqueId(), ignored -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
                boolean enabled = watchers.add(watcher);
                if (!enabled) watchers.remove(watcher);
                sender.sendMessage(enabled ? "§aFishing debug enabled for " + target.getName() + "." : "§cFishing debug disabled for " + target.getName() + ".");
                sendDebug(sender, target);
            }
            return true;
        }
        if (action.equals("resetdaily") || action.equals("setclub") || action.equals("givefish") || action.equals("giveaqua")) return handlePlayerAdmin(sender, action, args);
        if (action.equals("pos1") || action.equals("pos2") || action.equals("create") || action.equals("remove") || action.equals("list") || action.equals("enable") || action.equals("disable")) {
            sender.sendMessage(CC.color("&eFishing regions are no longer required; water is detected automatically in the active arena."));
            return true;
        }
        sender.sendMessage("§c/pit fishing <stats|club|reload|debug|resetdaily|setclub|givefish|giveaqua>");
        return true;
    }

    private void sampleHooks() {
        for (Map.Entry<UUID, Fish> entry : activeHooks.entrySet()) {
            HookSession session = hookSessions.get(entry.getKey());
            Fish hook = entry.getValue();
            Player owner = Bukkit.getPlayer(entry.getKey());
            if (session == null || hook == null || hook.isDead() || owner == null) {
                activeHooks.remove(entry.getKey(), hook);
                if (session == null) hookSessions.remove(entry.getKey());
                else hookSessions.remove(entry.getKey(), session);
                continue;
            }
            if (!activeArenaPlayer(owner)
                    || hook.getWorld() != session.world || !session.matches(plugin.getArenaManager().getCurrentArena(), plugin.getArenaManager().getCurrentWorld())) {
                session.invalidWater = true;
                continue;
            }
            boolean water = isWater(hook.getLocation());
            if (water) session.enteredWater = true;
            else if (session.enteredWater) session.invalidWater = true;
        }
    }

    private boolean activeArenaPlayer(Player player) {
        return player != null && player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR
                && plugin.getStatsManager().isProfileLoaded(player.getUniqueId())
                && plugin.getArenaManager().getCurrentArena() != null
                && plugin.getArenaManager().getCurrentWorld() != null
                && player.getWorld() == plugin.getArenaManager().getCurrentWorld();
    }

    private boolean isWater(Location location) {
        if (location == null || location.getWorld() == null) return false;
        Material material = location.getBlock().getType();
        return material == Material.WATER || material == Material.STATIONARY_WATER;
    }

    public int catchesToday(Player player) {
        Profile profile = profile(player);
        return profile == null ? 0 : catchesToday(profile);
    }

    public int limit() {
        return Math.max(1, plugin.registry().settings().getInt("fishing.default-daily-limit", 100));
    }

    public int limit(Player player) {
        int base = limit();
        if (player == null || plugin.getRenownShopManager() == null) return base;
        int tier = plugin.getRenownShopManager().tier(player.getUniqueId(), "fish-maxxing");
        return tier <= 0 ? base : Math.max(base,
                plugin.getRenownShopManager().effect("fish-maxxing", "tier-" + tier + "-limit", base));
    }

    public int remaining(Player player) {
        return Math.max(0, limit(player) - catchesToday(player));
    }

    public int clubTier(Player player) {
        Profile profile = profile(player);
        int legacy = profile == null ? 0 : profile.clubTier;
        int renown = plugin.getRenownShopManager() == null ? 0
                : plugin.getRenownShopManager().tier(player.getUniqueId(), "fishing-club");
        return Math.max(legacy, renown);
    }

    public ItemStack aquaPants() {
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(0x55FFFF));
        meta.setDisplayName(CC.color("&bFresh Aqua Pants"));
        meta.setLore(Arrays.asList(CC.color("&7Special fishing pants."), CC.color("&7Can be enchanted once in the Mystic Well."), "", CC.color("&bAqua obtained from fishing!")));
        item.setItemMeta(meta);
        item = SecureItemTag.string(item, "ThePitItem", "fresh_aqua_pants");
        return SecureItemTag.integer(item, "AquaTier", 0);
    }

    private void load(Player player) {
        UUID uuid = player.getUniqueId();
        profiles.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String stored = plugin.getStorageManager().getOverride("fishing", uuid.toString()).orElse("");
            Profile loaded = stored.isEmpty() ? fallbackProfile(uuid) : Profile.deserialize(stored);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) profiles.put(uuid, loaded);
            });
        });
    }

    private void removeHooks() {
        for (Fish hook : activeHooks.values()) {
            try { if (hook != null && !hook.isDead()) hook.remove(); } catch (Throwable ignored) { }
        }
        activeHooks.clear();
    }

    private Profile profile(Player player) {
        return profiles.get(player.getUniqueId());
    }

    private Profile fallbackProfile(UUID uuid) {
        return Profile.deserialize(fallback.getString("players." + uuid, ""));
    }

    private void saveAsync(UUID uuid, Profile profile) {
        String value = profile.serialize();
        fallback.set("players." + uuid, value);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getStorageManager().putOverride("fishing", uuid.toString(), value);
            saveFallback();
        });
    }

    private void save(UUID uuid, Profile profile) {
        String value = profile.serialize();
        fallback.set("players." + uuid, value);
        plugin.getStorageManager().putOverride("fishing", uuid.toString(), value);
    }

    private synchronized void loadFallback() {
        fallback = YamlConfiguration.loadConfiguration(fallbackFile);
    }

    private synchronized void saveFallback() {
        try {
            fallbackFile.getParentFile().mkdirs();
            fallback.save(fallbackFile);
        } catch (Exception exception) {
            plugin.getLogger().warning("Could not save fishing profiles: " + exception.getMessage());
        }
    }

    private Reward roll(Player player, Profile profile) {
        List<Reward> rewards = new ArrayList<>();
        int club = clubTier(player);
        add(rewards, "one-gold", "GOLD", 1, Material.AIR, weight("basic.one-gold", 70));
        add(rewards, "six-gold", "GOLD", 6, Material.AIR, weight("basic.six-gold", 25));
        add(rewards, "diamond-sword", "ITEM", 1, Material.DIAMOND_SWORD, weight("basic.diamond-sword", 5));
        if (club >= 1) {
            add(rewards, "kloonfish", "FISH", 1, Material.RAW_FISH, weight("club.kloonfish", 8));
            add(rewards, "salmon", "FISH", 1, Material.RAW_FISH, weight("club.salmon", 8));
        }
        if (club >= 3) {
            add(rewards, "diamond-chestplate", "ITEM", 1, Material.DIAMOND_CHESTPLATE, weight("club.diamond-chestplate", 3));
            add(rewards, "diamond-leggings", "ITEM", 1, Material.DIAMOND_LEGGINGS, weight("club.diamond-leggings", 3));
            add(rewards, "diamond-boots", "ITEM", 1, Material.DIAMOND_BOOTS, weight("club.diamond-boots", 3));
            if (plugin.registry().settings().getBoolean("fishing.rewards.club.diamond-helmet.enabled", false)) add(rewards, "diamond-helmet", "ITEM", 1, Material.DIAMOND_HELMET, weight("club.diamond-helmet", 3));
        }
        if (club >= plugin.registry().settings().getInt("fishing.aqua-pants.required-club-tier", 5)
                && plugin.getStatsManager().getPrestige(player.getUniqueId()) >= plugin.registry().settings().getInt("fishing.aqua-pants.required-prestige", 7)
                && plugin.registry().settings().getBoolean("fishing.aqua-pants.enabled", true)) {
            add(rewards, "fresh-aqua-pants", "AQUA", 1, Material.LEATHER_LEGGINGS, plugin.registry().settings().getInt("fishing.aqua-pants.weight", 1));
        }
        int total = 0;
        for (Reward reward : rewards) total += Math.max(0, reward.weight);
        if (total <= 0) return null;
        int value = random.nextInt(total);
        for (Reward reward : rewards) {
            value -= Math.max(0, reward.weight);
            if (value < 0) return reward;
        }
        return null;
    }

    private boolean deliver(Player player, Reward reward) {
        if (reward.type.equals("GOLD")) {
            int amount = reward.amount;
            if (isAqua(player.getInventory().getLeggings()) && hasAqua(player.getInventory().getLeggings(), "trophy")) amount += plugin.registry().settings().getInt("fishing.aqua-enchants.trophy.bonus-gold", 2);
            if (!plugin.awardPitGold(player, amount)) return false;
            message(player, "caught-gold", "&6You caught %amount% gold!", "%amount%", String.valueOf(amount));
            return true;
        }
        ItemStack item;
        if (reward.type.equals("FISH")) item = fish(reward.id);
        else if (reward.type.equals("AQUA")) item = aquaPants();
        else item = SecureItemTag.unbreakable(new ItemStack(reward.material, reward.amount));
        giveOrDrop(player, item);
        if (reward.type.equals("FISH")) {
            String size = SecureItemTag.string(item, "FishSize");
            message(player, "caught-fish", "&aYou caught a %size% %fish%!", "%size%", size, "%fish%", reward.id.equals("salmon") ? "Salmon" : "Kloonfish");
        } else if (reward.type.equals("AQUA")) message(player, "caught-aqua-pants", "&b&lRARE CATCH! &fYou caught Fresh Aqua Pants!");
        else message(player, "caught-item", "&aYou caught %item%!", "%item%", pretty(reward.id));
        player.getWorld().playEffect(player.getLocation(), Effect.HAPPY_VILLAGER, 0);
        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1.0F, 1.2F);
        return true;
    }

    private ItemStack fish(String id) {
        String size = rollSize();
        short data = (short) (id.equals("salmon") ? plugin.registry().settings().getInt("fishing.special-fish.salmon.data", 1) : plugin.registry().settings().getInt("fishing.special-fish.kloonfish.data", 0));
        ItemStack item = new ItemStack(Material.RAW_FISH, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color("&a" + size + " " + (id.equals("salmon") ? "Salmon" : "Kloonfish")));
        item.setItemMeta(meta);
        item = SecureItemTag.string(item, "ThePitItem", id);
        return SecureItemTag.string(item, "FishSize", size);
    }

    private String rollSize() {
        LinkedHashMap<String, Integer> sizes = new LinkedHashMap<>();
        sizes.put("Nearly microscopic", plugin.registry().settings().getInt("fishing.fish-sizes.nearly-microscopic", 1));
        sizes.put("Tiny", plugin.registry().settings().getInt("fishing.fish-sizes.tiny", 12));
        sizes.put("Small", plugin.registry().settings().getInt("fishing.fish-sizes.small", 22));
        sizes.put("Medium", plugin.registry().settings().getInt("fishing.fish-sizes.medium", 30));
        sizes.put("Big", plugin.registry().settings().getInt("fishing.fish-sizes.big", 20));
        sizes.put("X-Large", plugin.registry().settings().getInt("fishing.fish-sizes.x-large", 10));
        sizes.put("Massive", plugin.registry().settings().getInt("fishing.fish-sizes.massive", 4));
        sizes.put("Gargantuan", plugin.registry().settings().getInt("fishing.fish-sizes.gargantuan", 1));
        int total = sizes.values().stream().mapToInt(value -> Math.max(0, value)).sum();
        int roll = random.nextInt(Math.max(1, total));
        for (Map.Entry<String, Integer> entry : sizes.entrySet()) {
            roll -= Math.max(0, entry.getValue());
            if (roll < 0) return entry.getKey();
        }
        return "Medium";
    }

    private String region(ArenaFishing arena, Location location) {
        if (arena == null || location == null) return null;
        for (Map.Entry<String, Cuboid> entry : arena.regions.entrySet()) if (entry.getValue().contains(location)) return entry.getKey();
        return null;
    }

    private String access(Player player) {
        if (clubTier(player) >= 1) return "renown-fishing-club";
        if (plugin.getPerkManager().has(player, "fishing_rod")) return "fishing-rod-perk";
        if (isAqua(player.getInventory().getLeggings()) && SecureItemTag.string(player.getInventory().getLeggings(), "ClubRod").equals("true")) return "club-rod";
        return "none";
    }

    private double speedIncrease(Player player) {
        if (!activeArenaPlayer(player) || access(player).equals("none")) return 0.0D;
        double speed = clubTier(player) >= 4 ? plugin.getRenownShopManager().effect("fishing-club", "speed-increase",
                plugin.registry().settings().getDouble("fishing.club.tier-4-speed-increase", 0.20D)) : 0.0D;
        if (hasAqua(player.getInventory().getLeggings(), "luck_of_the_pond")) speed += plugin.registry().settings().getDouble("fishing.aqua-enchants.luck-of-the-pond.speed-increase", 0.10D);
        return Math.max(0.0D, Math.min(0.75D, speed));
    }

    private void purchaseTier(Player player, int tier) {
        player.sendMessage(CC.color("&eFishing Club tiers are purchased with Renown."));
        plugin.getRenownShopManager().open(player);
    }

    private boolean handlePlayerAdmin(org.bukkit.command.CommandSender sender, String action, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cSpecify an online player.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cThat player must be online.");
            return true;
        }
        Profile profile = profile(target);
        if (profile == null) {
            sender.sendMessage("§cThat player's fishing profile is still loading.");
            return true;
        }
        if (action.equals("resetdaily")) {
            profile.dateKey = utcDate();
            profile.catches = 0;
            saveAsync(target.getUniqueId(), profile);
            sender.sendMessage("§aDaily fishing count reset.");
        } else if (action.equals("setclub")) {
            if (args.length < 3) sender.sendMessage("§c/pit fishing setclub <player> <0-5>");
            else {
                int tier;
                try { tier = Integer.parseInt(args[2]); } catch (NumberFormatException exception) { tier = -1; }
                if (tier < 0 || tier > 5) sender.sendMessage("§cTier must be 0-5.");
                else {
                    profile.clubTier = tier;
                    saveAsync(target.getUniqueId(), profile);
                    sender.sendMessage("§aFishing Club tier updated.");
                }
            }
        } else if (action.equals("givefish")) {
            if (args.length < 3 || !args[2].equalsIgnoreCase("kloonfish") && !args[2].equalsIgnoreCase("salmon")) sender.sendMessage("§c/pit fishing givefish <player> <kloonfish|salmon> [size]");
            else {
                ItemStack item = fish(args[2].toLowerCase(Locale.ENGLISH));
                if (args.length >= 4) {
                    if (!validSize(args[3])) {
                        sender.sendMessage("§cUnknown fish size.");
                        return true;
                    }
                    String size = pretty(args[3]);
                    item = SecureItemTag.string(item, "FishSize", size);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(CC.color("&a" + size + " " + pretty(args[2])));
                    item.setItemMeta(meta);
                }
                giveOrDrop(target, item);
                sender.sendMessage("§aFish given.");
            }
        } else {
            giveOrDrop(target, aquaPants());
            sender.sendMessage("§aFresh Aqua Pants given.");
        }
        return true;
    }

    private boolean handleRegionAdmin(org.bukkit.command.CommandSender sender, String action, String[] args) {
        if (action.equals("pos1") || action.equals("pos2")) {
            if (!(sender instanceof Player player) || args.length < 3) {
                sender.sendMessage("§c/pit fishing " + action + " <arena> <region>");
                return true;
            }
            if (normalize(args[2]).isEmpty()) {
                sender.sendMessage("§cInvalid region ID.");
                return true;
            }
            Selection selection = selections.computeIfAbsent(player.getUniqueId(), ignored -> new Selection());
            if (action.equals("pos1")) selection.pos1 = LocationPoint.from(player.getLocation());
            else selection.pos2 = LocationPoint.from(player.getLocation());
            sender.sendMessage("§aFishing region " + action + " selected.");
            return true;
        }
        if (action.equals("list")) {
            if (args.length < 2) sender.sendMessage("§c/pit fishing list <arena>");
            else {
                ArenaFishing arena = arenas.get(normalize(args[1]));
                sender.sendMessage("§6Fishing regions: §f" + (arena == null ? "none" : String.join(", ", arena.regions.keySet())));
            }
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cSpecify an arena.");
            return true;
        }
        String arena = normalize(args[1]);
        if (!plugin.getArenaManager().getArenaIds().contains(arena)) {
            sender.sendMessage("§cUnknown arena.");
            return true;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(arenasFile);
        if (action.equals("enable") || action.equals("disable")) {
            yaml.set("arenas." + arena + ".fishing.enabled", action.equals("enable"));
        } else {
            if (args.length < 3) {
                sender.sendMessage("§cSpecify a region.");
                return true;
            }
            String region = normalize(args[2]);
            if (region.isEmpty()) {
                sender.sendMessage("§cInvalid region ID.");
                return true;
            }
            String base = "arenas." + arena + ".fishing.regions." + region;
            if (action.equals("remove")) yaml.set(base, null);
            else if (action.equals("create")) {
                if (!(sender instanceof Player)) return true;
                Selection selection = selections.get(((Player) sender).getUniqueId());
                if (selection == null || selection.pos1 == null || selection.pos2 == null) {
                    sender.sendMessage("§cSelect pos1 and pos2 first.");
                    return true;
                }
                writePoint(yaml, base + ".pos1", selection.pos1);
                writePoint(yaml, base + ".pos2", selection.pos2);
            } else return true;
        }
        try {
            yaml.save(arenasFile);
            reload();
            sender.sendMessage("§aFishing arena configuration saved.");
        } catch (Exception exception) {
            sender.sendMessage("§cCould not save arenas.yml.");
            plugin.getLogger().warning("Could not save fishing region: " + exception.getMessage());
        }
        return true;
    }

    private void sendStats(org.bukkit.command.CommandSender sender, Player target) {
        sender.sendMessage("§6Fishing stats for §f" + target.getName());
        sender.sendMessage("§7Catches today: §f" + catchesToday(target) + "§7/§f" + limit(target));
        sender.sendMessage("§7Fishing Club: §b" + roman(clubTier(target)));
    }

    private void sendDebug(org.bukkit.command.CommandSender sender, Player target) {
        DebugState state = debug.get(target.getUniqueId());
        if (state == null) {
            sender.sendMessage("§7No fishing attempt has been recorded.");
            return;
        }
        sender.sendMessage("§6Fishing debug for §f" + target.getName());
        sender.sendMessage("§7Arena: §f" + state.arena + " §7enabled: §f" + state.enabled);
        sender.sendMessage("§7Player region: §f" + state.playerRegion + " §7hook region: §f" + state.hookRegion);
        sender.sendMessage("§7Hook block: §f" + state.block + " §7Y: §f" + state.y);
        sender.sendMessage("§7Access: §f" + state.access + " §7club: §f" + state.clubTier + " §7prestige: §f" + state.prestige);
        sender.sendMessage("§7Daily: §f" + state.catches + "§7/§f" + state.limit + " §7last reward: §f" + state.lastReward);
        sender.sendMessage("§7Last rejection: §f" + state.rejection);
    }

    private void reject(Player player, PlayerFishEvent event, String reason, boolean notify) {
        updateDebug(player, reason);
        if (notify) {
            String key = reason.equals("too-high") ? "too-high" : reason.equals("daily-limit") ? "daily-limit" : "invalid-area";
            message(player, key, key.equals("too-high") ? "&c&lNO FISH! &7You can't fish this high up!" : key.equals("daily-limit") ? "&cYou have reached your daily fishing limit!" : "&cYour fishing hook must remain in water in the active arena.");
        }
        notifyDebugWatchers(player);
    }

    private void notifyDebugWatchers(Player player) {
        for (UUID watcher : debugWatchers.getOrDefault(player.getUniqueId(), Collections.emptySet())) {
            Player administrator = Bukkit.getPlayer(watcher);
            if (me.outerspace.thepit.util.PermissionCompat.has(plugin, administrator, "thepit.admin.fishing")) sendDebug(administrator, player);
        }
    }

    private void updateDebug(Player player, String rejection) {
        DebugState old = debug.get(player.getUniqueId());
        if (old != null) debug.put(player.getUniqueId(), old.withRejection(rejection));
    }

    private void updateSuccess(Player player, Profile profile) {
        DebugState old = debug.get(player.getUniqueId());
        if (old != null) {
            debug.put(player.getUniqueId(), new DebugState(old.arena, old.enabled, old.playerRegion, old.hookRegion,
                    old.block, old.y, old.access, clubTier(player), old.prestige, profile.catches, limit(player), profile.lastReward, ""));
        }
    }

    private int catchesToday(Profile profile) {
        normalizeDate(profile);
        return profile.catches;
    }

    private void normalizeDate(Profile profile) {
        String today = utcDate();
        if (!today.equals(profile.dateKey)) {
            profile.dateKey = today;
            profile.catches = 0;
            profile.lastSuccess = 0L;
            profile.repeatedTiming = 0;
        }
    }

    private int maximumY() {
        return plugin.registry().settings().getInt("fishing.maximum-y", 105);
    }

    private int weight(String path, int fallback) {
        return Math.max(0, plugin.registry().settings().getInt("fishing.rewards." + path + ".weight", fallback));
    }

    private void add(List<Reward> rewards, String id, String type, int amount, Material material, int weight) {
        if (weight > 0) rewards.add(new Reward(id, type, amount, material, weight));
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            if (plugin.getPitCommandManager() != null) plugin.getPitCommandManager().addToStash(player.getUniqueId(), leftover);
            else plugin.getLogger().severe("Fishing reward could not be stashed for " + player.getUniqueId() + ": " + leftover.getType() + " x" + leftover.getAmount());
        }
        player.updateInventory();
    }

    private void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() <= 1) player.setItemInHand(null);
        else item.setAmount(item.getAmount() - 1);
        player.updateInventory();
    }

    private void removeItem(Player player, String id) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (SecureItemTag.is(player.getInventory().getItem(slot), id)) player.getInventory().setItem(slot, null);
        }
    }

    private boolean containsItem(Player player, String id) {
        for (ItemStack item : player.getInventory().getContents()) if (SecureItemTag.is(item, id)) return true;
        return false;
    }

    private void setAbsorption(Player player, double amount) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            java.lang.reflect.Method get = handle.getClass().getMethod("getAbsorptionHearts");
            java.lang.reflect.Method set = handle.getClass().getMethod("setAbsorptionHearts", float.class);
            float current = ((Number) get.invoke(handle)).floatValue();
            set.invoke(handle, Math.max(current, (float) amount));
        } catch (Throwable ignored) { }
    }

    private boolean isAqua(ItemStack item) {
        return SecureItemTag.is(item, "fresh_aqua_pants");
    }

    private boolean hasAqua(ItemStack item, String enchant) {
        return isAqua(item) && SecureItemTag.string(item, "AquaEnchant").equals(enchant);
    }

    private int aquaNearby(Player player) {
        int count = isAqua(player.getInventory().getLeggings()) ? 1 : 0;
        double radius = Math.max(1.0D, plugin.registry().settings().getDouble("fishing.aqua-enchants.tough-crew.radius", 12.0D));
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) if (entity instanceof Player && isAqua(((Player) entity).getInventory().getLeggings())) count++;
        return count;
    }

    private boolean isNpc(Player player) {
        return player.hasMetadata("NPC") || player.getUniqueId().version() == 2;
    }

    private void applyHookEnchant(Player attacker, Player victim) {
        if (attacker == null || victim == null || attacker.equals(victim) || isNpc(attacker) || isNpc(victim)) return;
        ItemStack pants = attacker.getInventory().getLeggings();
        if (!isAqua(pants)) return;
        long now = System.currentTimeMillis();
        if (now - combatGuard.getOrDefault(attacker.getUniqueId(), 0L) < 500L) return;
        combatGuard.put(attacker.getUniqueId(), now);
        if (hasAqua(pants, "grandmaster")) {
            double damage = Math.max(0.0D, plugin.registry().settings().getDouble("fishing.aqua-enchants.grandmaster.true-damage-hearts", 0.5D)) * 2.0D;
            try { victim.setHealth(Math.max(0.0D, victim.getHealth() - damage)); } catch (Throwable ignored) { }
        }
        if (hasAqua(pants, "rodback")) {
            org.bukkit.util.Vector velocity = victim.getLocation().toVector().subtract(attacker.getLocation().toVector());
            double multiplier = plugin.registry().settings().getDouble("fishing.aqua-enchants.rodback.knockback-multiplier", 1.2D);
            if (velocity.lengthSquared() > 0.01D) victim.setVelocity(velocity.normalize().multiply(multiplier).setY(0.35D));
        }
    }

    private String randomAquaEnchant() {
        List<String> enabled = new ArrayList<>();
        for (String id : Arrays.asList("grandmaster", "luck_of_the_pond", "portable_pond", "rodback", "rogue", "tough_crew", "trophy", "unite", "stereo")) {
            if (plugin.registry().settings().getBoolean("fishing.aqua-enchants." + id.replace('_', '-') + ".enabled", true)) enabled.add(id);
        }
        return enabled.isEmpty() ? "trophy" : enabled.get(random.nextInt(enabled.size()));
    }

    private void applyAquaLore(ItemStack item, String enchant) {
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore() == null ? new ArrayList<>() : new ArrayList<>(meta.getLore());
        lore.add("");
        lore.add(CC.color("&9Club Rod"));
        lore.add(CC.color("&b" + displayEnchant(enchant)));
        lore.add(CC.color("&7Lives: &a5"));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private void replaceLivesLore(ItemStack item, int lives) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return;
        List<String> lore = new ArrayList<>(meta.getLore());
        for (int index = 0; index < lore.size(); index++) {
            if (org.bukkit.ChatColor.stripColor(lore.get(index)).startsWith("Lives:")) lore.set(index, CC.color("&7Lives: &a" + lives));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private String displayEnchant(String enchant) {
        return pretty(enchant);
    }

    private int requiredPrestige(int tier) {
        return Math.max(0, plugin.registry().settings().getInt("fishing-club.tiers." + tier + ".required-prestige", tier == 5 ? 7 : 3));
    }

    private int tierPrice(int tier) {
        int[] prices = {0, 5000, 7500, 10000, 15000, 25000};
        return Math.max(0, plugin.registry().settings().getInt("fishing-club.tiers." + tier + ".price", prices[tier]));
    }

    private String effect(int tier) {
        return switch (tier) {
            case 1 -> "Unlock Kloonfish and Salmon";
            case 2 -> "Earn XP from successful catches";
            case 3 -> "Catch diamond armor";
            case 4 -> "Fishing bites occur faster";
            default -> "Catch Fresh Aqua Pants";
        };
    }

    private void message(Player player, String key, String fallback, String... replacements) {
        String value = messageValue(key, fallback);
        for (int i = 0; i + 1 < replacements.length; i += 2) value = value.replace(replacements[i], replacements[i + 1]);
        player.sendMessage(CC.color(value));
    }

    private String messageValue(String key, String fallback) {
        return plugin.registry().settings().getString("fishing.messages." + key, fallback);
    }

    private void title(Player player, String title) {
        try { player.sendTitle(CC.color(title), ""); } catch (Throwable ignored) { player.sendMessage(CC.color(title)); }
    }

    private LocationPoint point(ConfigurationSection section) {
        if (section == null || section.getString("world") == null || !section.contains("x") || !section.contains("y") || !section.contains("z")) return null;
        return new LocationPoint(section.getString("world"), section.getDouble("x"), section.getDouble("y"), section.getDouble("z"));
    }

    private void writePoint(YamlConfiguration yaml, String path, LocationPoint point) {
        yaml.set(path + ".world", point.world);
        yaml.set(path + ".x", point.x);
        yaml.set(path + ".y", point.y);
        yaml.set(path + ".z", point.z);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9_-]", "");
    }

    private String pretty(String value) {
        String[] words = value.replace('-', '_').split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ENGLISH));
        }
        return out.toString();
    }

    private boolean validSize(String value) {
        String id = normalize(value);
        return Arrays.asList("nearly-microscopic", "tiny", "small", "medium", "big", "x-large", "massive", "gargantuan").contains(id);
    }

    private String roman(int tier) {
        return tier <= 0 ? "0" : new String[]{"", "I", "II", "III", "IV", "V"}[Math.min(5, tier)];
    }

    private String utcDate() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }

    private static final class Profile {
        private int clubTier;
        private String dateKey = "";
        private int catches;
        private long lastSuccess;
        private int repeatedTiming;
        private long lastInterval;
        private String lastReward = "none";

        private String serialize() {
            return clubTier + "|" + dateKey + "|" + catches + "|" + lastSuccess + "|" + repeatedTiming + "|" + lastReward + "|" + lastInterval;
        }

        private static Profile deserialize(String value) {
            Profile profile = new Profile();
            if (value == null || value.isEmpty()) return profile;
            String[] split = value.split("\\|", -1);
            try {
                if (split.length > 0) profile.clubTier = Math.max(0, Math.min(5, Integer.parseInt(split[0])));
                if (split.length > 1) profile.dateKey = split[1];
                if (split.length > 2) profile.catches = Math.max(0, Integer.parseInt(split[2]));
                if (split.length > 3) profile.lastSuccess = Math.max(0L, Long.parseLong(split[3]));
                if (split.length > 4) profile.repeatedTiming = Math.max(0, Integer.parseInt(split[4]));
                if (split.length > 5) profile.lastReward = split[5];
                if (split.length > 6) profile.lastInterval = Math.max(0L, Long.parseLong(split[6]));
            } catch (NumberFormatException ignored) { }
            return profile;
        }
    }

    private record ArenaFishing(boolean enabled, Map<String, Cuboid> regions) {}
    private static final class HookSession {
        private final String arenaId;
        private final World world;
        private final int hookId;
        private boolean enteredWater;
        private boolean invalidWater;

        private HookSession(String arenaId, World world, int hookId) {
            this.arenaId = arenaId;
            this.world = world;
            this.hookId = hookId;
        }

        private boolean matches(String arenaId, World world) {
            return this.world == world && this.arenaId != null && this.arenaId.equalsIgnoreCase(arenaId == null ? "" : arenaId);
        }
    }
    private record Reward(String id, String type, int amount, Material material, int weight) {}
    private record LocationPoint(String world, double x, double y, double z) {
        private static LocationPoint from(Location location) {
            return new LocationPoint(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
        }
    }
    private record Cuboid(LocationPoint one, LocationPoint two) {
        private boolean contains(Location location) {
            if (location == null || location.getWorld() == null || !location.getWorld().getName().equalsIgnoreCase(one.world)) return false;
            return location.getX() >= Math.min(one.x, two.x) && location.getX() <= Math.max(one.x, two.x)
                    && location.getY() >= Math.min(one.y, two.y) && location.getY() <= Math.max(one.y, two.y)
                    && location.getZ() >= Math.min(one.z, two.z) && location.getZ() <= Math.max(one.z, two.z);
        }
    }
    private static final class Selection {
        private LocationPoint pos1;
        private LocationPoint pos2;
    }
    private record DebugState(String arena, boolean enabled, String playerRegion, String hookRegion, String block,
                              int y, String access, int clubTier, int prestige, int catches, int limit,
                              String lastReward, String rejection) {
        private DebugState withRejection(String value) {
            return new DebugState(arena, enabled, playerRegion, hookRegion, block, y, access, clubTier, prestige, catches, limit, lastReward, value);
        }
    }
}
