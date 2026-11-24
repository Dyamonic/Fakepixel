package me.outerspace.thepit.managers;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.model.Period;
import me.outerspace.thepit.events.PitLevelChangeEvent;
import me.outerspace.thepit.registry.RegistryNode;
import me.outerspace.thepit.util.CC;
import me.outerspace.thepit.util.SecureItemTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KillstreakManager implements Listener {
    private static final Set<String> REGISTERED_KILLSTREAK_BEHAVIORS = Set.of(
            "second-gapple", "explicious", "feast", "counter-strike", "arquebusier",
            "fight-or-flight", "spongesteve", "heros-haste", "r-and-r", "tough-skin",
            "tactical-retreat", "monster", "pungent", "glass-pickaxe", "aura-of-protection",
            "ice-cube", "khanate", "rush", "gold-nano-factory", "leech", "assured-strike",
            "apostle", "super-streaker", "gold-stack", "xp-stack");
    private final ThePitPlugin plugin;
    private final Map<String, KillstreakDefinition> killstreaks = new LinkedHashMap<>();
    private final Map<String, MegastreakDefinition> megastreaks = new LinkedHashMap<>();
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    private final Map<UUID, LifeState> lives = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Set<UUID> killstreakPurchaseProcessing = ConcurrentHashMap.newKeySet();
    private final Set<UUID> megastreakPurchaseProcessing = ConcurrentHashMap.newKeySet();
    private int saveTask = -1;

    public KillstreakManager(ThePitPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void start() {
        if (saveTask != -1) Bukkit.getScheduler().cancelTask(saveTask);
        long period = Math.max(100L, plugin.registry().settings().getLong("killstreaks.save-interval-seconds", 10L) * 20L);
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushDirty, period, period).getTaskId();
        for (Player player : Bukkit.getOnlinePlayers()) load(player);
    }

    public void shutdown() {
        if (saveTask != -1) Bukkit.getScheduler().cancelTask(saveTask);
        saveTask = -1;
        flushDirty();
        for (Player player : Bukkit.getOnlinePlayers()) cleanup(player, false);
        profiles.clear();
        lives.clear();
        killstreakPurchaseProcessing.clear();
        megastreakPurchaseProcessing.clear();
    }

    public boolean reload() {
        Map<String, KillstreakDefinition> nextKillstreaks = new LinkedHashMap<>();
        Map<String, MegastreakDefinition> nextMegastreaks = new LinkedHashMap<>();
        Set<String> configuredBundles = new HashSet<>();
        for (String raw : plugin.registry().settings().keys("megastreaks.definitions")) {
            String id = normalize(raw);
            if (!id.isEmpty()) configuredBundles.add(id);
        }
        for (String raw : plugin.registry().settings().keys("killstreaks.definitions")) {
            String id = normalize(raw);
            RegistryNode node = plugin.registry().settings().getConfigurationSection("killstreaks.definitions." + raw);
            if (node == null || !node.getBoolean("enabled", true)) continue;
            String rejection = "";
            if (id.isEmpty()) rejection = "stable normalized ID is empty";
            else if (nextKillstreaks.containsKey(id)) rejection = "duplicate normalized ID '" + id + "'";
            Material material = null;
            String materialName = node.getString("material", "");
            try {
                material = Material.valueOf(materialName.trim().toUpperCase(Locale.ENGLISH));
            } catch (Exception exception) {
                if (rejection.isEmpty()) rejection = "invalid material '" + materialName + "'";
            }
            int price = node.getInt("price", -1);
            int requiredLevel = node.getInt("required-level", 0);
            int interval = node.getInt("trigger-interval", 0);
            String bundle = normalize(node.getString("required-bundle", ""));
            String effect = normalize(node.getString("effect", id));
            if (rejection.isEmpty() && price < 0) rejection = "Pit Gold price must be nonnegative";
            if (rejection.isEmpty() && requiredLevel <= 0) rejection = "required level must be positive";
            if (rejection.isEmpty() && interval <= 0) rejection = "trigger interval must be positive";
            if (rejection.isEmpty() && !bundle.isEmpty() && !configuredBundles.contains(bundle)) {
                rejection = "required bundle '" + bundle + "' is not defined";
            }
            if (rejection.isEmpty() && !REGISTERED_KILLSTREAK_BEHAVIORS.contains(effect)) {
                rejection = "behavior implementation '" + effect + "' is not registered";
            }
            if (!rejection.isEmpty()) {
                plugin.getLogger().severe("Rejected Killstreak definition '" + raw + "': " + rejection + ".");
                continue;
            }
            nextKillstreaks.put(id, new KillstreakDefinition(id, node.getString("name", readable(id)), material,
                    (short) node.getInt("data", 0), node.getInt("menu-slot", nextKillstreaks.size()),
                    price, requiredLevel,
                    Math.max(0, node.getInt("required-prestige", 0)), bundle,
                    interval, effect,
                    node.getStringList("lore"), node));
        }
        for (String raw : plugin.registry().settings().keys("megastreaks.definitions")) {
            String id = normalize(raw);
            RegistryNode node = plugin.registry().settings().getConfigurationSection("megastreaks.definitions." + raw);
            if (id.isEmpty() || node == null || !node.getBoolean("enabled", true)) continue;
            nextMegastreaks.put(id, new MegastreakDefinition(id, node.getString("name", readable(id)),
                    material(node.getString("material", "NETHER_STAR"), Material.NETHER_STAR),
                    (short) node.getInt("data", 0), node.getInt("menu-slot", nextMegastreaks.size()),
                    Math.max(1, node.getInt("activation-streak", 50)), Math.max(1, node.getInt("required-level", 1)),
                    Math.max(0, node.getInt("required-prestige", 0)), Math.max(0, node.getInt("bundle-renown-price", 0)),
                    Math.max(0, node.getInt("price", 0)), node));
        }
        if (!nextMegastreaks.containsKey("overdrive")) {
            plugin.getLogger().warning("Megastreak definitions must contain Overdrive. Previous definitions remain active.");
            return false;
        }
        killstreaks.clear();
        killstreaks.putAll(nextKillstreaks);
        megastreaks.clear();
        megastreaks.putAll(nextMegastreaks);
        for (Profile profile : profiles.values()) sanitize(profile);
        return true;
    }

    public void load(Player player) {
        if (player == null || !loading.add(player.getUniqueId())) return;
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String stored = plugin.getStorageManager().getOverride("killstreaks", uuid.toString()).orElse("");
            Profile loaded = decode(stored);
            Bukkit.getScheduler().runTask(plugin, () -> {
                loading.remove(uuid);
                if (!player.isOnline()) return;
                sanitize(loaded);
                profiles.put(uuid, loaded);
                lives.putIfAbsent(uuid, new LifeState());
                applySelectedLifeEffect(player);
            });
        });
    }

    public Reward handleKill(Player player, Player victim, int streak, int baseXp, int baseGold) {
        if (player == null) return new Reward(baseXp, baseGold);
        Profile profile = profile(player.getUniqueId());
        LifeState life = life(player.getUniqueId());
        life.deathProcessed = false;
        MegastreakDefinition selected = megastreaks.get(profile.selectedMegastreak);
        if (selected == null) selected = megastreaks.get("overdrive");
        if (!life.activeMegastreak && selected != null && streak >= selected.activationStreak) activate(player, selected, streak);
        int xp = baseXp;
        int gold = baseGold;
        for (String id : profile.equipped) {
            KillstreakDefinition definition = killstreaks.get(id);
            if (definition == null || streak % definition.interval != 0) continue;
            String trigger = definition.id + "@" + streak;
            if (!life.processedTriggers.add(trigger)) continue;
            Reward reward = trigger(player, definition, xp, gold);
            xp = reward.xp;
            gold = reward.gold;
        }
        Reward megaReward = modifyKillReward(player, streak, xp, gold);
        xp = megaReward.xp;
        gold = megaReward.gold;
        if (life.activeMegastreak && "hermit".equals(profile.selectedMegastreak) && streak > 50 && streak % 10 == 0
                && life.hermitBedrockAwarded.add(streak)) {
            give(player, special(Material.BEDROCK, Math.max(1, selected.values.getInt("bedrock-per-ten", 16)),
                    "hermit-bedrock", "&8Temporary Bedrock"));
        }
        if (life.activeMegastreak && "to-the-moon".equals(profile.selectedMegastreak)) life.toMoonXp += Math.max(0, xp);
        if (life.activeMegastreak && "uberstreak".equals(profile.selectedMegastreak) && streak >= 200) {
            double loss = megastreaks.get("uberstreak").values.getDouble("max-health-loss-at-200", 4.0D);
            if (life.baseMaxHealth <= 0.0D) life.baseMaxHealth = player.getMaxHealth();
            player.setMaxHealth(Math.max(2.0D, life.baseMaxHealth - loss));
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        }
        plugin.getBuildScoreboardManager().update(player);
        return new Reward(xp, gold);
    }

    private Reward trigger(Player player, KillstreakDefinition definition, int xp, int gold) {
        LifeState life = life(player.getUniqueId());
        String effect = definition.effect;
        switch (effect) {
            case "second-gapple" -> {
                xp = safeAdd(xp, definition.values.getInt("bonus-xp", 5));
                gold = safeAdd(gold, definition.values.getInt("bonus-gold", 5));
                give(player, new ItemStack(Material.GOLDEN_APPLE, Math.max(1, definition.values.getInt("golden-apples", 1))));
            }
            case "explicious" -> xp = safeAdd(xp, definition.values.getInt("bonus-xp", 12));
            case "feast" -> give(player, special(Material.MUTTON, 1, "aaa-rated-steak", "&cAAA-Rated Steak"));
            case "counter-strike" -> life.counterStrikeUntil = until("killstreaks.effects.counter-strike.duration-seconds", 8);
            case "arquebusier" -> {
                gold = safeAdd(gold, definition.values.getInt("bonus-gold", 7));
                give(player, new ItemStack(Material.ARROW, Math.max(1, definition.values.getInt("arrows", 16))));
                potion(player, PotionEffectType.SPEED, definition.values.getInt("speed-seconds", 10), 0);
            }
            case "fight-or-flight" -> {
                long until = until("killstreaks.effects.fight-or-flight.duration-seconds", 7);
                if (player.getHealth() < player.getMaxHealth() * 0.5D) {
                    potion(player, PotionEffectType.SPEED, 7, 0);
                    potion(player, PotionEffectType.DAMAGE_RESISTANCE, 7, 0);
                } else life.flightDamageUntil = until;
            }
            case "spongesteve" -> setAbsorption(player, Math.max(0.0D, definition.values.getDouble("absorption-hearts", 15.0D)) * 2.0D);
            case "heros-haste" -> potion(player, PotionEffectType.SPEED, definition.values.getInt("speed-seconds", 5),
                    Math.max(0, definition.values.getInt("speed-amplifier", 2) - 1));
            case "r-and-r" -> {
                potion(player, PotionEffectType.DAMAGE_RESISTANCE, 3, 0);
                potion(player, PotionEffectType.REGENERATION, 3, 1);
            }
            case "tough-skin" -> life.toughSkinStacks = Math.min(plugin.registry().settings().getInt("killstreaks.effects.tough-skin.maximum-stacks", 8), life.toughSkinStacks + 1);
            case "tactical-retreat" -> {
                potion(player, PotionEffectType.REGENERATION, 5, 3);
                potion(player, PotionEffectType.WEAKNESS, 5, 3);
            }
            case "monster" -> {
                int maximum = plugin.registry().settings().getInt("killstreaks.effects.monster.maximum-stacks", 2);
                if (life.monsterStacks < maximum) {
                    life.monsterStacks++;
                    updateMaximumHealth(player, life);
                }
            }
            case "pungent" -> {
                for (Player nearby : player.getWorld().getPlayers()) {
                    if (nearby.equals(player) || nearby.getLocation().distanceSquared(player.getLocation()) > 36.0D) continue;
                    potion(nearby, PotionEffectType.SLOW, 5, 0);
                }
            }
            case "glass-pickaxe" -> give(player, special(Material.DIAMOND_PICKAXE, 1, "glass-pickaxe", "&bGlass Pickaxe"));
            case "aura-of-protection" -> {
                potion(player, PotionEffectType.DAMAGE_RESISTANCE, 4, 1);
                life.trueDamageImmuneUntil = System.currentTimeMillis() + 15000L;
            }
            case "ice-cube" -> {
                life.iceCube = true;
                potion(player, PotionEffectType.SLOW, 10, 0);
                xp = safeAdd(xp, 40);
            }
            case "khanate" -> {
                life.khanateStacks = Math.min(plugin.registry().settings().getInt("killstreaks.effects.khanate.maximum-stacks", 10), life.khanateStacks + 1);
                gold = safeAdd(gold, 8);
            }
            case "rush" -> {
                life.rushStacks = Math.min(plugin.registry().settings().getInt("killstreaks.effects.rush.maximum-stacks", 10), life.rushStacks + 1);
                double speed = 0.2D * (1.0D + life.rushStacks * plugin.registry().settings().getDouble("killstreaks.effects.rush.speed-per-stack", 0.015D));
                player.setWalkSpeed((float) Math.min(1.0D, speed));
            }
            case "gold-nano-factory" -> give(player, special(Material.GOLD_NUGGET, 1, "gold-nano-factory", "&6Gold Nano-factory"));
            case "leech" -> life.leech = true;
            case "assured-strike" -> {
                life.assuredStrike = true;
                potion(player, PotionEffectType.SPEED, 20, 0);
            }
            case "apostle" -> apostle(player);
            case "super-streaker" -> {
                life.superStreakerStacks = Math.min(plugin.registry().settings().getInt("killstreaks.effects.super-streaker.maximum-stacks", 10), life.superStreakerStacks + 1);
                xp = safeAdd(xp, plugin.registry().settings().getInt("killstreaks.effects.super-streaker.base-xp", 50));
            }
            case "gold-stack" -> {
                double increase = plugin.registry().settings().getDouble("killstreaks.effects.gold-stack.progress-per-trigger", 0.1D);
                double cap = plugin.registry().settings().getDouble("killstreaks.effects.gold-stack.base-cap", 0.5D)
                        + plugin.getStatsManager().getPrestige(player.getUniqueId()) * plugin.registry().settings().getDouble("killstreaks.effects.gold-stack.cap-per-prestige", 0.1D);
                profile(player.getUniqueId()).goldStack = Math.min(cap, profile(player.getUniqueId()).goldStack + increase);
                dirty.add(player.getUniqueId());
            }
            case "xp-stack" -> {
                double increase = plugin.registry().settings().getDouble("killstreaks.effects.xp-stack.progress-per-trigger", 0.05D);
                double cap = plugin.registry().settings().getDouble("killstreaks.effects.xp-stack.base-cap", 1.0D)
                        + plugin.getStatsManager().getPrestige(player.getUniqueId()) * plugin.registry().settings().getDouble("killstreaks.effects.xp-stack.cap-per-prestige", 0.2D);
                profile(player.getUniqueId()).xpStack = Math.min(cap, profile(player.getUniqueId()).xpStack + increase);
                dirty.add(player.getUniqueId());
            }
            default -> { }
        }
        player.sendMessage(CC.color("&a&lSTREAK! &7" + definition.name + " &7triggered."));
        return new Reward(xp, gold);
    }

    private Reward modifyKillReward(Player player, int streak, int xp, int gold) {
        Profile profile = profile(player.getUniqueId());
        LifeState life = life(player.getUniqueId());
        profile.xpCarry += profile.xpStack;
        profile.goldCarry += profile.goldStack;
        int stackXp = (int) Math.floor(profile.xpCarry);
        int stackGold = (int) Math.floor(profile.goldCarry);
        profile.xpCarry -= stackXp;
        profile.goldCarry -= stackGold;
        xp = safeAdd(xp, stackXp);
        gold = safeAdd(gold, stackGold);
        if (stackXp > 0 || stackGold > 0) dirty.add(player.getUniqueId());
        if (life.superStreakerStacks > 0) xp = multiply(xp, 1.0D + life.superStreakerStacks
                * plugin.registry().settings().getDouble("killstreaks.effects.super-streaker.bonus-per-stack", 0.05D));
        if (!life.activeMegastreak) return new Reward(xp, gold);
        MegastreakDefinition definition = megastreaks.get(profile.selectedMegastreak);
        if (definition == null) return new Reward(xp, gold);
        if ("hermit".equals(definition.id)) {
            double bonus = Math.min(definition.values.getDouble("maximum-reward-bonus", 0.75D),
                    definition.values.getDouble("initial-reward-bonus", 0.05D)
                            + Math.max(0, (streak - definition.activationStreak) / 10) * definition.values.getDouble("reward-bonus-per-ten", 0.05D));
            xp = multiply(xp, 1.0D + bonus);
            gold = multiply(gold, 1.0D + bonus);
        } else {
            xp = multiply(xp, definition.values.getDouble("kill-xp-multiplier", 1.0D));
            gold = multiply(gold, definition.values.getDouble("kill-gold-multiplier", 1.0D));
        }
        if ("to-the-moon".equals(definition.id)) xp = safeAdd(xp, definition.values.getInt("maximum-kill-xp-bonus", 100));
        return new Reward(xp, gold);
    }

    private void activate(Player player, MegastreakDefinition definition, int streak) {
        LifeState life = life(player.getUniqueId());
        if (life.activeMegastreak) return;
        life.activeMegastreak = true;
        life.activationStreak = streak;
        plugin.getBuildScoreboardManager().setMegastreakStatus(player.getUniqueId(), definition.id);
        switch (definition.id) {
            case "overdrive", "highlander" -> potion(player, PotionEffectType.SPEED, Integer.MAX_VALUE / 40, 0);
            case "beastmode" -> {
                ItemStack helmet = SecureItemTag.unbreakable(new ItemStack(Material.DIAMOND_HELMET));
                player.getInventory().setHelmet(helmet);
                profile(player.getUniqueId()).beastHelmet = true;
                dirty.add(player.getUniqueId());
            }
            case "hermit" -> {
                potion(player, PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE / 40, 0);
                life.hermitTrueDamageImmune = true;
                give(player, special(Material.BEDROCK, definition.values.getInt("bedrock-at-activation", 32), "hermit-bedrock", "&8Temporary Bedrock"));
            }
            case "magnum-opus" -> {
                player.getWorld().createExplosion(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 0.0F, false, false);
                plugin.getStatsManager().changeRenown(player, definition.values.getLong("renown-reward", 1L), StatsManager.RenownMode.GIVE);
                Bukkit.getScheduler().runTask(plugin, () -> plugin.getGameListener().forceSelfDeath(player));
            }
            case "uberstreak" -> {
                profile(player.getUniqueId()).permanentUberBonus = Math.max(profile(player.getUniqueId()).permanentUberBonus,
                        definition.values.getDouble("mystic-drop-bonus", 0.50D));
                dirty.add(player.getUniqueId());
            }
            default -> { }
        }
        String title = plugin.registry().settings().getString("megastreaks.activation.title", "&c&lMEGASTREAK!");
        String subtitle = plugin.registry().settings().getString("megastreaks.activation.subtitle", "&f%megastreak% &7activated at &a%streak% kills")
                .replace("%megastreak%", definition.name).replace("%streak%", String.valueOf(streak));
        plugin.getLevelUpFeedbackManager().queueTitle(player, title, subtitle, 1L);
        playSound(player, plugin.registry().settings().getString("megastreaks.activation.sound", "WITHER_SPAWN"));
        String broadcast = plugin.registry().settings().getString("megastreaks.activation.broadcast", "");
        if (!broadcast.isEmpty()) Bukkit.broadcastMessage(CC.color(broadcast
                .replace("%megastreak%", definition.name)
                .replace("%player%", player.getName())
                .replace("%level_formatted%", plugin.getStatsManager().getFormattedLevel(player.getUniqueId()))));
    }

    public double modifyDamage(Player attacker, Player victim, double damage, boolean projectile) {
        if (attacker == null || victim == null || damage <= 0.0D) return damage;
        long now = System.currentTimeMillis();
        LifeState attacking = life(attacker.getUniqueId());
        LifeState defending = life(victim.getUniqueId());
        if (!projectile && "glass-pickaxe".equals(SecureItemTag.string(attacker.getItemInHand(), "ThePitItem"))) {
            damage += 8.5D;
            attacking.glassPickaxeTrueDamage = true;
            attacker.setItemInHand(null);
            attacker.updateInventory();
        }
        if (attacking.feastUntil > now) damage *= 1.0D + plugin.registry().settings().getDouble("killstreaks.effects.feast.damage-bonus", 0.20D);
        if (attacking.counterStrikeUntil > now) damage *= 1.0D + plugin.registry().settings().getDouble("killstreaks.effects.counter-strike.damage-bonus", 0.15D);
        if (attacking.flightDamageUntil > now) damage *= 1.0D + plugin.registry().settings().getDouble("killstreaks.effects.fight-or-flight.damage-bonus", 0.20D);
        if (attacking.khanateStacks > 0 && plugin.getStatsManager().getBounty(victim.getUniqueId()) > 0) {
            damage *= 1.0D + attacking.khanateStacks * plugin.registry().settings().getDouble("killstreaks.effects.khanate.damage-per-stack", 0.04D);
        }
        if (attacking.leech && !projectile) {
            attacking.leech = false;
            damage *= 1.20D;
            heal(attacker, 1.0D);
        }
        if (attacking.assuredStrike && !projectile) {
            attacking.assuredStrike = false;
            damage *= 1.35D;
        }
        Profile attackerProfile = profile(attacker.getUniqueId());
        Profile victimProfile = profile(victim.getUniqueId());
        if (attacking.activeMegastreak) {
            MegastreakDefinition mega = megastreaks.get(attackerProfile.selectedMegastreak);
            if (mega != null) {
                damage *= 1.0D + mega.values.getDouble("outgoing-damage", 0.0D);
                if ("highlander".equals(mega.id) && plugin.getStatsManager().getBounty(victim.getUniqueId()) > 0) damage *= 1.0D + mega.values.getDouble("bountied-damage-bonus", 0.33D);
                if ("uberstreak".equals(mega.id) && plugin.getStatsManager().getPrestige(victim.getUniqueId()) == 0) damage *= 1.0D - mega.values.getDouble("non-prestige-damage-reduction", 0.40D);
            }
        }
        if (defending.counterStrikeUntil > now) damage = Math.max(0.0D, damage - plugin.registry().settings().getDouble("killstreaks.effects.counter-strike.flat-reduction", 2.0D));
        if (defending.toughSkinStacks > 0) damage *= Math.max(0.0D, 1.0D - defending.toughSkinStacks
                * plugin.registry().settings().getDouble("killstreaks.effects.tough-skin.reduction-per-stack", 0.03D));
        if (defending.activeMegastreak) {
            MegastreakDefinition mega = megastreaks.get(victimProfile.selectedMegastreak);
            int streak = plugin.getStatsManager().getKillStreak(victim.getUniqueId(), Period.LIFETIME);
            if (mega != null && "hermit".equals(mega.id)) damage *= 1.0D + Math.max(0, streak - 50) * mega.values.getDouble("incoming-damage-per-kill", 0.003D);
            if (mega != null && "to-the-moon".equals(mega.id)) damage *= 1.0D + Math.max(0, (streak - 100) / 20) * mega.values.getDouble("incoming-per-twenty", 0.10D);
            if (mega != null && "uberstreak".equals(mega.id)) damage *= 1.0D + Math.max(0, streak / 100) * mega.values.getDouble("incoming-per-hundred", 0.10D);
            if (mega != null && "beastmode".equals(mega.id) && streak >= 55) {
                damage += ((streak - 50) / 5) * mega.values.getDouble("incoming-heart-per-five", 0.1D) * 2.0D;
            }
            if (mega != null && "highlander".equals(mega.id) && plugin.getPerkManager().has(attacker, "bounty_hunter")) {
                damage *= 1.0D + Math.max(0, streak - 50) * mega.values.getDouble("bounty-hunter-incoming-per-kill", 0.003D);
            }
        }
        return Math.max(0.0D, damage);
    }

    public void applyPostDamage(Player attacker, Player victim) {
        if (attacker == null || victim == null) return;
        LifeState defending = life(victim.getUniqueId());
        int streak = plugin.getStatsManager().getKillStreak(victim.getUniqueId(), Period.LIFETIME);
        double hearts = 0.0D;
        if (defending.activeMegastreak) {
            String id = profile(victim.getUniqueId()).selectedMegastreak;
            MegastreakDefinition mega = megastreaks.get(id);
            if ("overdrive".equals(id) && streak >= 55) hearts += ((streak - 50) / 5) * mega.values.getDouble("true-damage-heart-per-five", 0.1D);
            if ("to-the-moon".equals(id) && streak >= 220) hearts += ((streak - 200) / 20) * mega.values.getDouble("true-damage-heart-per-twenty-over-200", 0.1D);
        }
        LifeState attacking = life(attacker.getUniqueId());
        if (attacking.iceCube) {
            attacking.iceCube = false;
            hearts += 1.0D;
        }
        if (attacking.glassPickaxeTrueDamage) {
            attacking.glassPickaxeTrueDamage = false;
            hearts += 0.5D;
        }
        if (hearts <= 0.0D || defending.hermitTrueDamageImmune || defending.trueDamageImmuneUntil > System.currentTimeMillis()) return;
        double points = hearts * 2.0D;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!victim.isOnline() || victim.isDead()) return;
            victim.setHealth(Math.max(0.0D, victim.getHealth() - Math.min(points, victim.getHealth())));
        });
    }

    public void cleanup(Player player, boolean rewardDeath) {
        if (player == null) return;
        LifeState life = life(player.getUniqueId());
        if (life.deathProcessed) return;
        life.deathProcessed = true;
        int streak = plugin.getStatsManager().getKillStreak(player.getUniqueId(), Period.LIFETIME);
        String selected = profile(player.getUniqueId()).selectedMegastreak;
        if (rewardDeath && life.activeMegastreak) {
            MegastreakDefinition definition = megastreaks.get(selected);
            if ("overdrive".equals(selected) && definition != null) {
                plugin.getStatsManager().addXp(player, definition.values.getLong("death-xp", 4000L), StatsManager.XpSource.KILLSTREAK);
            } else if ("to-the-moon".equals(selected) && definition != null && life.toMoonXp > 0L) {
                int capped = Math.min(streak, definition.values.getInt("death-streak-cap", 300));
                double multiplier = Math.min(definition.values.getDouble("death-multiplier-cap", 1.0D),
                        Math.max(0, capped - 100) * definition.values.getDouble("death-multiplier-per-kill", 0.005D));
                plugin.getStatsManager().addXp(player, (long) Math.floor(life.toMoonXp * multiplier), StatsManager.XpSource.KILLSTREAK);
            } else if ("highlander".equals(selected)) {
                int bounty = plugin.getStatsManager().getBounty(player.getUniqueId());
                if (bounty > 0) plugin.getPitGoldService().award(player, bounty, ThePitPlugin.GoldSource.EVENT);
            } else if ("uberstreak".equals(selected) && definition != null && streak >= definition.values.getInt("uberdrop-minimum-streak", 400)) {
                awardUberdrop(player, definition);
            }
        }
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.SLOW);
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.WEAKNESS);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.setWalkSpeed(0.2F);
        removeTemporaryItems(player);
        plugin.getPerkManager().applyHealth(player);
        plugin.getBuildScoreboardManager().clearMegastreakStatus(player.getUniqueId());
        lives.put(player.getUniqueId(), new LifeState());
        if (plugin.isEnabled()) Bukkit.getScheduler().runTask(plugin, () -> applySelectedLifeEffect(player));
    }

    public boolean isActive(UUID uuid) {
        return uuid != null && life(uuid).activeMegastreak;
    }

    public double blockDurationMultiplier(UUID uuid) {
        if (uuid == null || !"hermit".equals(profile(uuid).selectedMegastreak)) return 1.0D;
        MegastreakDefinition definition = megastreaks.get("hermit");
        return definition == null ? 1.0D : Math.max(1.0D, definition.values.getDouble("block-duration-multiplier", 2.0D));
    }

    public long blockLifetimeSeconds(ItemStack item) {
        if (item != null && "hermit-bedrock".equals(SecureItemTag.string(item, "ThePitItem"))) {
            MegastreakDefinition definition = megastreaks.get("hermit");
            return definition == null ? 60L : Math.max(1L, definition.values.getLong("bedrock-duration-seconds", 60L));
        }
        return -1L;
    }

    public void arenaCleanup() {
        for (Player player : Bukkit.getOnlinePlayers()) cleanup(player, true);
    }

    public List<String> effectLines(Player player) {
        if (player == null) return Collections.emptyList();
        LifeState life = life(player.getUniqueId());
        long now = System.currentTimeMillis();
        List<String> lines = new ArrayList<>();
        addEffectLine(lines, "&fFeast &cI &7(%seconds%s)", life.feastUntil, now);
        addEffectLine(lines, "&fCounter-Strike &eI &7(%seconds%s)", life.counterStrikeUntil, now);
        addEffectLine(lines, "&fFight or Flight &cI &7(%seconds%s)", life.flightDamageUntil, now);
        addEffectLine(lines, "&fTrue Damage Immunity &9I &7(%seconds%s)", life.trueDamageImmuneUntil, now);
        return lines;
    }

    public void afterRespawn(Player player) {
        if (player == null || !profile(player.getUniqueId()).beastHelmet) return;
        player.getInventory().setHelmet(SecureItemTag.unbreakable(new ItemStack(Material.DIAMOND_HELMET)));
        player.updateInventory();
    }

    public void openKillstreaks(Player player) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId()) || loading.contains(player.getUniqueId())) {
            if (player != null) player.sendMessage(CC.color("&cYour Pit profile is still loading."));
            return;
        }
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.LOADOUT, -1);
        int size = 54;
        Inventory inventory = Bukkit.createInventory(holder, size, CC.color(plugin.registry().settings().getString("killstreaks.title", "Killstreaks")));
        holder.inventory = inventory;
        fill(inventory);
        Profile profile = profile(player.getUniqueId());
        for (int index = 0; index < 3; index++) {
            int slot = plugin.registry().settings().getInt("killstreaks.slots." + (index + 1) + ".inventory-slot", 20 + index * 2);
            if (slot < 0 || slot >= size) continue;
            holder.actions.put(slot, "@slot:" + index);
            inventory.setItem(slot, slotIcon(player, profile, index));
        }
        int mega = plugin.registry().settings().getInt("killstreaks.menu.selected-megastreak-slot", 40);
        if (mega >= 0 && mega < size) {
            holder.actions.put(mega, "@choose-megastreak");
            inventory.setItem(mega, selectedMegastreakIcon(player, profile));
        }
        int back = plugin.registry().settings().getInt("killstreaks.menu.back-slot", 49);
        if (back >= 0 && back < size) {
            holder.actions.put(back, "@back-upgrades");
            inventory.setItem(back, item(Material.ARROW, (short) 0, "&aBack to Upgrades", Collections.emptyList()));
        }
        player.openInventory(inventory);
    }

    private void openKillstreakChooser(Player player, int selectedSlot) {
        if (player == null || selectedSlot < 0 || selectedSlot >= 3 || !slotUnlocked(player, selectedSlot)) {
            if (player != null) player.sendMessage(CC.color("&cThat killstreak slot is locked."));
            return;
        }
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.KILLSTREAK_CHOOSER, selectedSlot);
        int size = boundedSize(plugin.registry().settings().getInt("killstreaks.choose-size", 54));
        Inventory inventory = Bukkit.createInventory(holder, size,
                CC.color(plugin.registry().settings().getString("killstreaks.choose-title", "Choose a Killstreak")));
        holder.inventory = inventory;
        fill(inventory);
        Profile profile = profile(player.getUniqueId());
        for (KillstreakDefinition definition : killstreaks.values()) {
            if (definition.slot < 0 || definition.slot >= size) continue;
            holder.actions.put(definition.slot, definition.id);
            inventory.setItem(definition.slot, SecureItemTag.string(
                    killstreakIcon(player, profile, definition, selectedSlot),
                    "BuildFFAKillstreakId", canonicalId(definition.id)));
        }
        int back = plugin.registry().settings().getInt("killstreaks.menu.back-slot", 49);
        if (back >= 0 && back < size) {
            holder.actions.put(back, "@back-loadout");
            inventory.setItem(back, item(Material.ARROW, (short) 0, "&aGo Back", Collections.emptyList()));
        }
        player.openInventory(inventory);
    }

    public void openMegastreaks(Player player) {
        if (player == null || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId()) || loading.contains(player.getUniqueId())) {
            if (player != null) player.sendMessage(CC.color("&cYour Pit profile is still loading."));
            return;
        }
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuType.MEGASTREAK_CHOOSER, -1);
        int size = boundedSize(plugin.registry().settings().getInt("megastreaks.size", 45));
        Inventory inventory = Bukkit.createInventory(holder, size, CC.color(plugin.registry().settings().getString("megastreaks.title", "Choose a Megastreak")));
        holder.inventory = inventory;
        fill(inventory);
        for (MegastreakDefinition definition : megastreaks.values()) {
            if (definition.slot < 0 || definition.slot >= size) continue;
            holder.actions.put(definition.slot, definition.id);
            inventory.setItem(definition.slot, megastreakIcon(player, definition));
        }
        int close = plugin.registry().settings().getInt("megastreaks.close-slot", 40);
        if (close >= 0 && close < size) {
            holder.actions.put(close, "@back-loadout");
            inventory.setItem(close, item(Material.ARROW, (short) 0, "&aGo Back", Collections.emptyList()));
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.owner.equals(player.getUniqueId())) return;
        if (event.isShiftClick() || event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.DOUBLE_CLICK
                || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        String action = holder.actions.get(event.getRawSlot());
        if (action == null) return;
        if ("@back-upgrades".equals(action)) {
            plugin.getPerkMenu().open(player);
            return;
        }
        if ("@back-loadout".equals(action)) {
            openKillstreaks(player);
            return;
        }
        if (action.startsWith("@slot:")) {
            int selectedSlot;
            try {
                selectedSlot = Integer.parseInt(action.substring(6));
            } catch (NumberFormatException exception) {
                return;
            }
            openKillstreakChooser(player, selectedSlot);
            return;
        }
        if ("@choose-megastreak".equals(action)) {
            openMegastreaks(player);
            return;
        }
        if (holder.type == MenuType.KILLSTREAK_CHOOSER
                && killstreakPurchaseProcessing.contains(player.getUniqueId())) {
            sendKillstreakPurchaseFailure(player, null, KillstreakPurchaseResult.PURCHASE_IN_PROGRESS);
            return;
        }
        if (holder.type == MenuType.MEGASTREAK_CHOOSER) purchaseOrSelectMegastreak(player, action);
        else if (holder.type == MenuType.KILLSTREAK_CHOOSER) {
            purchaseOrEquipKillstreak(player, action, holder.selectedSlot, event.getClick() == ClickType.RIGHT);
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder
                || event.getView().getTopInventory().getHolder() instanceof RenownBundleHolder) event.setCancelled(true);
    }

    private void purchaseOrEquipKillstreak(Player player, String id, int selectedSlot, boolean right) {
        KillstreakDefinition definition = killstreaks.get(id);
        Profile profile = profile(player.getUniqueId());
        if (selectedSlot < 0 || selectedSlot >= 3 || !slotUnlocked(player, selectedSlot)) {
            player.sendMessage(CC.color("&cThat killstreak slot is locked."));
            openKillstreaks(player);
            return;
        }
        if (definition == null) {
            player.sendMessage(CC.color("&cThe purchase failed. Nothing was charged."));
            return;
        }
        if (!profile.purchasedKillstreaks.contains(id)) {
            purchaseKillstreak(player, definition, () -> openKillstreakChooser(player, selectedSlot));
            return;
        }
        int equipped = profile.equipped.indexOf(id);
        if (right) {
            if (equipped >= 0) profile.equipped.set(equipped, "");
        } else {
            if (equipped >= 0) profile.equipped.set(equipped, "");
            profile.equipped.set(selectedSlot, id);
        }
        dirty.add(player.getUniqueId());
        UUID uuid = player.getUniqueId();
        String encoded = encode(profile);
        List<String> equippedSlots = List.copyOf(profile.equipped);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getPurchaseRepository().saveKillstreakLoadout(uuid, equippedSlots, encoded)) {
                dirty.remove(uuid);
            }
        });
        openKillstreaks(player);
    }

    private void purchaseOrSelectMegastreak(Player player, String id) {
        MegastreakDefinition definition = megastreaks.get(id);
        Profile profile = profile(player.getUniqueId());
        if (definition == null) return;
        if (!bundleOwned(player, definition) || plugin.getStatsManager().getLevel(player.getUniqueId()) < definition.requiredLevel
                || plugin.getStatsManager().getPrestige(player.getUniqueId()) < definition.requiredPrestige) {
            player.sendMessage(CC.color("&cYou do not meet this megastreak's requirements."));
            return;
        }
        if (!profile.purchasedMegastreaks.contains(id)) {
            purchase(player, "megastreak-" + definition.id, definition.price,
                    plugin.getRenownShopManager().discountedPrice(player.getUniqueId(), definition.price),
                    () -> profile.purchasedMegastreaks.add(id), () -> profile.purchasedMegastreaks.remove(id),
                    "&aPermanently unlocked " + definition.name + "&a.", () -> openMegastreaks(player));
            return;
        }
        profile.selectedMegastreak = id;
        dirty.add(player.getUniqueId());
        saveNowAsync(player.getUniqueId(), null);
        cleanup(player, false);
        openMegastreaks(player);
    }

    private void purchaseKillstreak(Player player, KillstreakDefinition definition, Runnable refresh) {
        UUID uuid = player.getUniqueId();
        UUID transactionId = UUID.randomUUID();
        if (!killstreakPurchaseProcessing.add(uuid)) {
            logKillstreakPreflight(player, definition, transactionId,
                    KillstreakPurchaseResult.PURCHASE_IN_PROGRESS, "duplicate pending purchase request", null);
            sendKillstreakPurchaseFailure(player, definition, KillstreakPurchaseResult.PURCHASE_IN_PROGRESS);
            return;
        }
        Profile profile = profiles.get(uuid);
        int price = plugin.getRenownShopManager().discountedPrice(uuid, definition.price);
        int originalBalance = plugin.getPitGoldService().balance(uuid);
        try {
            KillstreakPurchaseResult validation = validateKillstreakPurchase(player, definition, profile, price);
            if (validation != KillstreakPurchaseResult.SUCCESS) {
                logKillstreakPreflight(player, definition, transactionId, validation,
                        "requirement check returned " + validation, null);
                sendKillstreakPurchaseFailure(player, definition, validation);
                killstreakPurchaseProcessing.remove(uuid);
                return;
            }
            Set<String> purchased = new LinkedHashSet<>(profile.purchasedKillstreaks);
            purchased.add(definition.id);
            String value = encode(profile, purchased);
            String playerName = player.getName();
            PurchaseRepository.Request request = new PurchaseRepository.Request(
                    transactionId, uuid, playerName, true, originalBalance,
                    PurchaseRepository.Type.KILLSTREAK, definition.id, definition.price, price,
                    KillstreakPurchaseResult.SUCCESS.name(), value, System.currentTimeMillis(), 0L,
                    Collections.emptyList());
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                PurchaseRepository.Report stored;
                if (plugin.getStatsManager().awaitPendingPitGoldWrites(uuid, 5000L)) {
                    stored = plugin.getPurchaseRepository().purchase(request);
                } else {
                    stored = new PurchaseRepository.Report(transactionId,
                            PurchaseRepository.Status.PERSISTENCE_FAILURE, -1, originalBalance,
                            0, 0, false, false, "timed out waiting for pending Pit Gold writes", null);
                    plugin.getLogger().severe("[Purchase tx=" + transactionId + " player=" + uuid
                            + " type=KILLSTREAK id=" + definition.id
                            + "] exactFailure=timed out waiting for pending Pit Gold writes");
                }
                Bukkit.getScheduler().runTask(plugin, () -> finishKillstreakPurchase(
                        player, profile, definition, price, request, stored, refresh));
            });
        } catch (Throwable throwable) {
            killstreakPurchaseProcessing.remove(uuid);
            logKillstreakPreflight(player, definition, transactionId,
                    KillstreakPurchaseResult.STORAGE_FAILURE, "exception before persistence", throwable);
            if (player.isOnline()) player.sendMessage(CC.color("&cThe purchase failed. Nothing was charged."));
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Killstreak purchase failed before persistence: player=" + uuid
                            + ", killstreak=" + definition.id, throwable);
        }
    }

    private void logKillstreakPreflight(Player player, KillstreakDefinition definition, UUID transactionId,
                                        KillstreakPurchaseResult result, String reason, Throwable exception) {
        if (player == null || definition == null || plugin.getPurchaseRepository() == null) return;
        UUID uuid = player.getUniqueId();
        PurchaseRepository.Request request = new PurchaseRepository.Request(
                transactionId, uuid, player.getName(),
                profiles.containsKey(uuid) && plugin.getStatsManager().isProfileLoaded(uuid),
                plugin.getPitGoldService().balance(uuid), PurchaseRepository.Type.KILLSTREAK,
                definition.id, definition.price,
                plugin.getRenownShopManager().discountedPrice(uuid, definition.price),
                result.name(), "", 0L, 0L, Collections.emptyList());
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> plugin.getPurchaseRepository().rejected(request, result.name(), reason, exception));
    }

    private void finishKillstreakPurchase(Player player, Profile profile, KillstreakDefinition definition, int price,
                                          PurchaseRepository.Request request, PurchaseRepository.Report stored,
                                          Runnable refresh) {
        UUID uuid = player.getUniqueId();
        try {
            if (stored.success()) {
                if (player.isOnline() && profiles.get(uuid) == profile) {
                    profile.purchasedKillstreaks.add(definition.id);
                    dirty.remove(uuid);
                    plugin.getPitGoldService().applyPersistedBalance(player, stored.updatedCoins());
                    plugin.getPurchaseRepository().cacheUpdated(request, true, "");
                    player.sendMessage(CC.color("&aKillstreak purchased!"));
                    player.sendMessage(CC.color("&7Cost: " + plugin.getPitGoldService().formatPitGold(price)));
                } else plugin.getPurchaseRepository().cacheUpdated(request, false,
                        "player offline or Killstreak profile instance changed after commit");
            } else if (player.isOnline()) {
                plugin.getPurchaseRepository().cacheUpdated(request, false, stored.failureReason());
                sendKillstreakPurchaseFailure(player, definition,
                        stored.status() == PurchaseRepository.Status.ROLLBACK_FAILURE
                                ? KillstreakPurchaseResult.REFUND_FAILURE : KillstreakPurchaseResult.STORAGE_FAILURE);
            }
        } finally {
            killstreakPurchaseProcessing.remove(uuid);
            if (player.isOnline()) refresh.run();
        }
    }

    private KillstreakPurchaseResult validateKillstreakPurchase(Player player, KillstreakDefinition definition, Profile profile, int price) {
        if (player == null || !player.isOnline() || profile == null || loading.contains(player.getUniqueId()) || !plugin.getStatsManager().isProfileLoaded(player.getUniqueId())) return KillstreakPurchaseResult.PROFILE_NOT_LOADED;
        if (!killstreaks.containsKey(definition.id)) return KillstreakPurchaseResult.INVALID_KILLSTREAK;
        if (profile.purchasedKillstreaks.contains(definition.id)) return KillstreakPurchaseResult.ALREADY_OWNED;
        if (plugin.getStatsManager().getLevel(player.getUniqueId()) < definition.requiredLevel) return KillstreakPurchaseResult.LEVEL_REQUIRED;
        if (plugin.getStatsManager().getPrestige(player.getUniqueId()) < definition.requiredPrestige) return KillstreakPurchaseResult.PRESTIGE_REQUIRED;
        if (!definition.requiredBundle.isEmpty() && !plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "megastreak-bundle-" + definition.requiredBundle)) return KillstreakPurchaseResult.BUNDLE_REQUIRED;
        if (!plugin.getPitGoldService().has(player.getUniqueId(), price)) return KillstreakPurchaseResult.INSUFFICIENT_GOLD;
        if (plugin.getPurchaseRepository() == null || !plugin.getPurchaseRepository().killstreakAvailable()) {
            return KillstreakPurchaseResult.STORAGE_FAILURE;
        }
        return KillstreakPurchaseResult.SUCCESS;
    }

    private void sendKillstreakPurchaseFailure(Player player, KillstreakDefinition definition, KillstreakPurchaseResult result) {
        if (result == KillstreakPurchaseResult.PROFILE_NOT_LOADED) player.sendMessage(CC.color("&cYour Pit profile is still loading."));
        else if (result == KillstreakPurchaseResult.LEVEL_REQUIRED) player.sendMessage(CC.color("&cYou must reach level " + definition.requiredLevel + " to purchase this!"));
        else if (result == KillstreakPurchaseResult.PRESTIGE_REQUIRED) player.sendMessage(CC.color("&cYou must reach Prestige " + definition.requiredPrestige + " to purchase this!"));
        else if (result == KillstreakPurchaseResult.BUNDLE_REQUIRED) player.sendMessage(CC.color("&cYou have not unlocked the required killstreak bundle!"));
        else if (result == KillstreakPurchaseResult.ALREADY_OWNED) player.sendMessage(CC.color("&cYou already own this killstreak!"));
        else if (result == KillstreakPurchaseResult.INSUFFICIENT_GOLD) player.sendMessage(CC.color("&cYou do not have enough Gold!"));
        else if (result == KillstreakPurchaseResult.PURCHASE_IN_PROGRESS) player.sendMessage(CC.color("&cA killstreak purchase is already being processed."));
        else if (result == KillstreakPurchaseResult.REFUND_FAILURE) player.sendMessage(CC.color("&cThe purchase failed and the Gold refund could not be saved."));
        else player.sendMessage(CC.color("&cThe purchase failed. Nothing was charged."));
    }

    private void purchase(Player player, String purchaseId, int originalPrice, int price, Runnable apply,
                          Runnable rollback, String success, Runnable refresh) {
        UUID uuid = player.getUniqueId();
        UUID transactionId = UUID.randomUUID();
        if (!megastreakPurchaseProcessing.add(uuid)) {
            player.sendMessage(CC.color("&cA megastreak purchase is already being processed."));
            return;
        }
        int originalBalance = plugin.getPitGoldService().balance(uuid);
        try {
            if (!plugin.getPitGoldService().has(uuid, price)) {
                player.sendMessage(CC.color("&cYou do not have enough Gold!"));
                megastreakPurchaseProcessing.remove(uuid);
                refresh.run();
                return;
            }
            apply.run();
            String encoded = encode(profile(uuid));
            rollback.run();
            PurchaseRepository.Request request = new PurchaseRepository.Request(
                    transactionId, uuid, player.getName(), plugin.getStatsManager().isProfileLoaded(uuid),
                    originalBalance, PurchaseRepository.Type.KILLSTREAK, purchaseId, originalPrice, price,
                    "SUCCESS", encoded, System.currentTimeMillis(), 0L, Collections.emptyList());
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                PurchaseRepository.Report report = plugin.getStatsManager().awaitPendingPitGoldWrites(uuid, 5000L)
                        ? plugin.getPurchaseRepository().purchase(request)
                        : new PurchaseRepository.Report(transactionId,
                        PurchaseRepository.Status.PERSISTENCE_FAILURE, -1, originalBalance,
                        0, 0, false, false, "timed out waiting for pending Pit Gold writes", null);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        if (report.success()) {
                            apply.run();
                            plugin.getPitGoldService().applyPersistedBalance(player, report.updatedCoins());
                            plugin.getPurchaseRepository().cacheUpdated(request, true, "");
                            dirty.remove(uuid);
                            player.sendMessage(CC.color(success));
                        } else {
                            plugin.getPurchaseRepository().cacheUpdated(request, false, report.failureReason());
                            player.sendMessage(CC.color("&cThe purchase failed. Nothing was charged."));
                        }
                    } finally {
                        megastreakPurchaseProcessing.remove(uuid);
                        if (player.isOnline()) refresh.run();
                    }
                });
            });
        } catch (Throwable throwable) {
            rollback.run();
            player.sendMessage(CC.color("&cThe purchase failed. Nothing was charged."));
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                    "Megastreak purchase failed before persistence for " + uuid, throwable);
            megastreakPurchaseProcessing.remove(uuid);
            refresh.run();
        } finally {
            // The asynchronous completion owns the processing lock after scheduling.
        }
    }

    public Set<String> renownBundleIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (MegastreakDefinition definition : megastreaks.values()) if (definition.bundleRenownPrice > 0) ids.add("megastreak-bundle-" + definition.id);
        ids.add("killstreak-slot");
        return ids;
    }

    public void openRenownBundles(Player player) {
        RenownBundleHolder holder = new RenownBundleHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 54, CC.color("&8Streak Bundles"));
        holder.inventory = inventory;
        fill(inventory);
        List<String> ids = new ArrayList<>(renownBundleIds());
        int[] slots = {10, 12, 14, 16, 28, 30, 32};
        for (int index = 0; index < ids.size() && index < slots.length; index++) {
            String id = ids.get(index);
            holder.actions.put(slots[index], id);
            inventory.setItem(slots[index], renownItem(player, id));
        }
        inventory.setItem(49, item(Material.ARROW, (short) 0, "&aGo Back", Collections.emptyList()));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onRenownBundleClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof RenownBundleHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !holder.owner.equals(player.getUniqueId())) return;
        if (event.isShiftClick() || event.getClick() == ClickType.NUMBER_KEY || event.getClick() == ClickType.DOUBLE_CLICK) return;
        if (event.getRawSlot() == 49) {
            plugin.getRenownShopManager().open(player);
            return;
        }
        String id = holder.actions.get(event.getRawSlot());
        if (id == null) return;
        if (plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), id)) {
            player.sendMessage(CC.color("&aYou already own that streak upgrade."));
        } else if (purchaseRenown(player, id)) {
            player.sendMessage(CC.color("&aPurchased that streak upgrade permanently."));
        } else {
            player.sendMessage(CC.color("&cThat streak upgrade could not be purchased."));
        }
        openRenownBundles(player);
    }

    public ItemStack renownItem(Player player, String id) {
        if ("killstreak-slot".equals(id)) return renownItem(player, id, "&eKillstreak Slot", Material.CHEST, 4, 50,
                Arrays.asList("&7Unlocks the third killstreak slot", "&7at level 100."));
        String megaId = id.replace("megastreak-bundle-", "");
        MegastreakDefinition definition = megastreaks.get(megaId);
        if (definition == null) return item(Material.BARRIER, (short) 0, "&cUnavailable", Collections.emptyList());
        return renownItem(player, id, definition.name + " &7Bundle", definition.material, definition.requiredPrestige,
                definition.bundleRenownPrice, Arrays.asList("&7Unlocks " + definition.name + "&7 and", "&7its bundled killstreak perks."));
    }

    public int renownSlot(String id) {
        if ("killstreak-slot".equals(id)) return 44;
        List<String> order = Arrays.asList("beastmode", "hermit", "highlander", "magnum-opus", "to-the-moon", "uberstreak");
        int index = order.indexOf(id.replace("megastreak-bundle-", ""));
        return index < 0 ? -1 : 45 + index;
    }

    public boolean purchaseRenown(Player player, String id) {
        int prestige;
        int price;
        if ("killstreak-slot".equals(id)) {
            prestige = 4;
            price = 50;
        } else {
            MegastreakDefinition definition = megastreaks.get(id.replace("megastreak-bundle-", ""));
            if (definition == null) return false;
            prestige = definition.requiredPrestige;
            price = definition.bundleRenownPrice;
        }
        return plugin.getStatsManager().purchaseRenownUpgrade(player, id, prestige, price);
    }

    private ItemStack renownItem(Player player, String id, String name, Material material, int prestige, int price, List<String> description) {
        List<String> lore = new ArrayList<>(description);
        lore.add("");
        lore.add("&7Cost: &e" + price + " Renown");
        lore.add("&7Requires: &dPrestige " + roman(prestige));
        lore.add("");
        if (plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), id)) lore.add("&aPurchased permanently!");
        else if (plugin.getStatsManager().getPrestige(player.getUniqueId()) < prestige) lore.add("&cPrestige requirement not met!");
        else if (plugin.getStatsManager().getRenown(player.getUniqueId()) < price) lore.add("&cNot enough Renown!");
        else lore.add("&eClick to purchase!");
        return item(material, (short) 0, name, lore);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        load(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLevelChanged(PitLevelChangeEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerUuid());
        if (player == null) return;
        InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
        if (holder instanceof MenuHolder menu && menu.owner.equals(player.getUniqueId())) {
            if (menu.type == MenuType.MEGASTREAK_CHOOSER) openMegastreaks(player);
            else if (menu.type == MenuType.KILLSTREAK_CHOOSER) openKillstreakChooser(player, menu.selectedSlot);
            else openKillstreaks(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getBuildScoreboardManager().isCombatTagged(player)) cleanup(player, true);
        else cleanup(player, false);
        UUID uuid = player.getUniqueId();
        String value = encode(profile(uuid));
        boolean purchaseInProgress = killstreakPurchaseProcessing.remove(uuid);
        profiles.remove(uuid);
        lives.remove(uuid);
        dirty.remove(uuid);
        megastreakPurchaseProcessing.remove(uuid);
        plugin.getPitGoldService().unlock(uuid);
        if (!purchaseInProgress) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> plugin.getStorageManager().putOverride("killstreaks", uuid.toString(), value));
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        cleanup(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isActive(event.getPlayer().getUniqueId()) || me.outerspace.thepit.util.PermissionCompat.has(plugin, event.getPlayer(), "thepit.megastreak.command-bypass")) return;
        String message = event.getMessage().substring(1);
        int cut = message.indexOf(' ');
        String command = normalize(cut < 0 ? message : message.substring(0, cut));
        int colon = command.indexOf(':');
        if (colon >= 0) command = command.substring(colon + 1);
        for (String restricted : plugin.registry().settings().getStringList("megastreaks.command-restrictions")) {
            if (command.equals(normalize(restricted))) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(CC.color("&cYou cannot use this command during an active megastreak!"));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        String id = SecureItemTag.string(item, "ThePitItem");
        if ("aaa-rated-steak".equals(id)) {
            event.setCancelled(true);
            consumeOne(event.getPlayer());
            LifeState life = life(event.getPlayer().getUniqueId());
            life.feastUntil = until("killstreaks.effects.feast.duration-seconds", 10);
            potion(event.getPlayer(), PotionEffectType.SPEED, 10, 0);
            potion(event.getPlayer(), PotionEffectType.DAMAGE_RESISTANCE, 10, 0);
        } else if ("gold-nano-factory".equals(id)) {
            event.setCancelled(true);
            consumeOne(event.getPlayer());
            potion(event.getPlayer(), PotionEffectType.REGENERATION, 2, 3);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHealing(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (life(player.getUniqueId()).activeMegastreak && "uberstreak".equals(profile(player.getUniqueId()).selectedMegastreak)
                && plugin.getStatsManager().getKillStreak(player.getUniqueId(), Period.LIFETIME) >= 400) event.setCancelled(true);
    }

    private void applySelectedLifeEffect(Player player) {
        if (player == null || !player.isOnline()) return;
        if ("hermit".equals(profile(player.getUniqueId()).selectedMegastreak)) potion(player, PotionEffectType.SLOW, Integer.MAX_VALUE / 40, 0);
    }

    private boolean requirements(Player player, KillstreakDefinition definition) {
        return plugin.getStatsManager().getLevel(player.getUniqueId()) >= definition.requiredLevel
                && plugin.getStatsManager().getPrestige(player.getUniqueId()) >= definition.requiredPrestige
                && (definition.requiredBundle.isEmpty() || plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "megastreak-bundle-" + definition.requiredBundle));
    }

    private boolean bundleOwned(Player player, MegastreakDefinition definition) {
        return definition.bundleRenownPrice <= 0 || plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), "megastreak-bundle-" + definition.id);
    }

    private boolean slotUnlocked(Player player, int index) {
        int number = index + 1;
        int level = plugin.registry().settings().getInt("killstreaks.slots." + number + ".required-level", number == 1 ? 10 : number == 2 ? 75 : 100);
        int prestige = plugin.registry().settings().getInt("killstreaks.slots." + number + ".required-prestige", number == 1 ? 0 : number == 2 ? 1 : 4);
        String upgrade = normalize(plugin.registry().settings().getString("killstreaks.slots." + number + ".required-renown-upgrade", number == 3 ? "killstreak-slot" : ""));
        return plugin.getStatsManager().getLevel(player.getUniqueId()) >= level
                && plugin.getStatsManager().getPrestige(player.getUniqueId()) >= prestige
                && (upgrade.isEmpty() || plugin.getStatsManager().hasRenownUpgrade(player.getUniqueId(), upgrade));
    }

    private ItemStack killstreakIcon(Player player, Profile profile, KillstreakDefinition definition, int selectedSlot) {
        boolean purchased = profile.purchasedKillstreaks.contains(definition.id);
        int equipped = profile.equipped.indexOf(definition.id);
        int price = plugin.getRenownShopManager().discountedPrice(player.getUniqueId(), definition.price);
        List<String> lore = new ArrayList<>(definition.lore);
        lore.add("");
        lore.add("&7Triggers every &e" + definition.interval + " kills");
        lore.add("&7Requires: &bLevel " + definition.requiredLevel);
        if (definition.requiredPrestige > 0) lore.add("&7Requires: &dPrestige " + roman(definition.requiredPrestige));
        if (!definition.requiredBundle.isEmpty()) lore.add("&7Bundle: &d" + readable(definition.requiredBundle));
        lore.add("&7Cost: &6" + format(price) + " Gold");
        lore.add("");
        if (equipped == selectedSlot) {
            lore.add("&aSelected for this slot");
            lore.add("&eRight-click to unequip!");
        } else if (equipped >= 0) {
            lore.add("&7Equipped in Slot #" + (equipped + 1));
            lore.add("&eClick to move to Slot #" + (selectedSlot + 1) + "!");
        } else if (purchased) lore.add("&eClick to equip!");
        else if (!requirements(player, definition)) lore.add("&cRequirements not met!");
        else if (!plugin.getPitGoldService().has(player.getUniqueId(), price)) lore.add("&cNot enough Gold!");
        else lore.add("&eClick to purchase!");
        return item(definition.material, definition.data, definition.name, lore);
    }

    private ItemStack selectedMegastreakIcon(Player player, Profile profile) {
        MegastreakDefinition selected = megastreaks.get(profile.selectedMegastreak);
        Material material = selected == null ? Material.NETHER_STAR : selected.material;
        short data = selected == null ? 0 : selected.data;
        String selectedName = selected == null ? "&cNone" : selected.name;
        List<String> lore = new ArrayList<>();
        lore.add("&7Current: " + selectedName);
        if (selected != null) lore.add("&7Activates at: &a" + selected.activationStreak + " kills");
        lore.add("&7Your Gold: &6" + format(plugin.getPitGoldService().balance(player.getUniqueId())));
        lore.add("");
        lore.add("&eClick to choose a Megastreak!");
        return item(material, data, "&cSelected Megastreak", lore);
    }

    private ItemStack slotIcon(Player player, Profile profile, int index) {
        if (!slotUnlocked(player, index)) {
            int number = index + 1;
            List<String> lore = new ArrayList<>();
            lore.add("&7Level: &b" + plugin.registry().settings().getInt("killstreaks.slots." + number + ".required-level", 1));
            lore.add("&7Prestige: &d" + roman(plugin.registry().settings().getInt("killstreaks.slots." + number + ".required-prestige", 0)));
            String upgrade = plugin.registry().settings().getString("killstreaks.slots." + number + ".required-renown-upgrade", "");
            if (!upgrade.isEmpty()) lore.add("&7Renown upgrade: &eKillstreak Slot");
            lore.add("");
            lore.add("&cSlot locked!");
            return item(Material.STAINED_GLASS_PANE, (short) 14, "&cKillstreak Slot " + number, lore);
        }
        String id = profile.equipped.get(index);
        KillstreakDefinition definition = killstreaks.get(id);
        return definition == null
                ? item(Material.STAINED_GLASS_PANE, (short) 5, "&aKillstreak Slot " + (index + 1), Arrays.asList("&7Nothing equipped.", "", "&eClick to choose a Killstreak!"))
                : item(definition.material, definition.data, "&aKillstreak Slot " + (index + 1), Arrays.asList("&7Selected: " + definition.name, "&7Triggers every &e" + definition.interval + " kills.", "", "&eClick to choose a Killstreak!"));
    }

    private ItemStack megastreakIcon(Player player, MegastreakDefinition definition) {
        Profile profile = profile(player.getUniqueId());
        boolean purchased = profile.purchasedMegastreaks.contains(definition.id);
        boolean bundle = bundleOwned(player, definition);
        int price = plugin.getRenownShopManager().discountedPrice(player.getUniqueId(), definition.price);
        List<String> lore = new ArrayList<>();
        lore.add("&7Activates at &a" + definition.activationStreak + " kills");
        lore.add("&7Requires: &bLevel " + definition.requiredLevel);
        lore.add("&7Requires: &dPrestige " + roman(definition.requiredPrestige));
        if (definition.bundleRenownPrice > 0) lore.add("&7Bundle: &e" + definition.bundleRenownPrice + " Renown");
        lore.add("&7Activation unlock: &6" + format(price) + " Gold");
        lore.add("&7Your Gold: &6" + format(plugin.getPitGoldService().balance(player.getUniqueId())));
        lore.add("");
        if (definition.id.equals(profile.selectedMegastreak)) lore.add("&aSelected");
        else if (purchased) lore.add("&eClick to select!");
        else if (!bundle) lore.add("&cRequires bundle from the Renown Shop");
        else if (plugin.getStatsManager().getPrestige(player.getUniqueId()) < definition.requiredPrestige) lore.add("&cRequires Prestige " + roman(definition.requiredPrestige));
        else if (plugin.getStatsManager().getLevel(player.getUniqueId()) < definition.requiredLevel) lore.add("&cRequires Level " + definition.requiredLevel);
        else if (!plugin.getPitGoldService().has(player.getUniqueId(), price)) lore.add("&cNot enough Gold!");
        else lore.add("&eClick to permanently unlock!");
        return item(definition.material, definition.data, definition.name, lore);
    }

    private void awardUberdrop(Player player, MegastreakDefinition definition) {
        Profile profile = profile(player.getUniqueId());
        long now = System.currentTimeMillis();
        long window = Math.max(1L, definition.values.getLong("uberdrop-window-hours", 24L)) * 3600000L;
        while (!profile.uberdrops.isEmpty() && now - profile.uberdrops.peekFirst() >= window) profile.uberdrops.removeFirst();
        int limit = Math.max(1, definition.values.getInt("uberdrop-limit", 4));
        if (profile.uberdrops.size() >= limit) return;
        List<Integer> costs = plugin.registry().settings().getIntegerList("megastreaks.definitions.uberstreak.uberdrop-renown-costs");
        int cost = profile.uberdrops.size() < costs.size() ? Math.max(0, costs.get(profile.uberdrops.size())) : 0;
        if (plugin.getStatsManager().getRenown(player.getUniqueId()) < cost) return;
        List<String> rewards = plugin.registry().settings().getStringList("megastreaks.definitions.uberstreak.uberdrop-rewards");
        if (rewards.isEmpty()) return;
        String reward = normalize(rewards.get(profile.uberdrops.size() % rewards.size()));
        ItemStack item = special(Material.NETHER_STAR, 1, "uberdrop-" + reward, "&5Uberdrop: &d" + readable(reward));
        if (cost > 0 && !plugin.getStatsManager().changeRenown(player, cost, StatsManager.RenownMode.TAKE)) return;
        give(player, item);
        profile.uberdrops.addLast(now);
        dirty.add(player.getUniqueId());
    }

    private void apostle(Player player) {
        int roll = 1 + (int) (Math.random() * 100.0D);
        if (roll <= 20) potion(player, PotionEffectType.SPEED, 20, 1);
        else if (roll <= 40) potion(player, PotionEffectType.DAMAGE_RESISTANCE, 15, 1);
        else if (roll <= 60) setAbsorption(player, 20.0D);
        else if (roll <= 80) plugin.getStatsManager().addXp(player, 100, StatsManager.XpSource.KILLSTREAK);
        else plugin.getPitGoldService().award(player, 100, ThePitPlugin.GoldSource.EVENT);
    }

    private void updateMaximumHealth(Player player, LifeState life) {
        double per = Math.max(0.0D, plugin.registry().settings().getDouble("killstreaks.effects.monster.hearts-per-stack", 1.0D)) * 2.0D;
        if (life.baseMaxHealth <= 0.0D) life.baseMaxHealth = player.getMaxHealth();
        player.setMaxHealth(Math.max(2.0D, life.baseMaxHealth + life.monsterStacks * per));
    }

    private void give(Player player, ItemStack item) {
        if (player == null || item == null || item.getAmount() <= 0) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (plugin.getPitCommandManager() != null) for (ItemStack remaining : leftovers.values()) plugin.getPitCommandManager().addToStash(player.getUniqueId(), remaining);
    }

    private void removeTemporaryItems(Player player) {
        Set<String> temporary = new HashSet<>(Arrays.asList("aaa-rated-steak", "glass-pickaxe", "gold-nano-factory", "hermit-bedrock"));
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item != null && temporary.contains(SecureItemTag.string(item, "ThePitItem"))) player.getInventory().setItem(slot, null);
        }
        player.updateInventory();
    }

    private ItemStack special(Material material, int amount, String id, String name) {
        ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        item.setItemMeta(meta);
        return SecureItemTag.string(item, "ThePitItem", id);
    }

    private void consumeOne(Player player) {
        ItemStack item = player.getItemInHand();
        if (item == null) return;
        if (item.getAmount() <= 1) player.setItemInHand(null);
        else item.setAmount(item.getAmount() - 1);
        player.updateInventory();
    }

    private void potion(Player player, PotionEffectType type, int seconds, int amplifier) {
        if (player == null || type == null) return;
        LifeState life = life(player.getUniqueId());
        int duration = Math.max(1, seconds * 20);
        if (life.activeMegastreak && "uberstreak".equals(profile(player.getUniqueId()).selectedMegastreak)
                && plugin.getStatsManager().getKillStreak(player.getUniqueId(), Period.LIFETIME) >= 300) duration = Math.max(1, duration / 2);
        player.addPotionEffect(new PotionEffect(type, duration, Math.max(0, amplifier)), true);
    }

    private void heal(Player player, double amount) {
        if (life(player.getUniqueId()).activeMegastreak && "uberstreak".equals(profile(player.getUniqueId()).selectedMegastreak)
                && plugin.getStatsManager().getKillStreak(player.getUniqueId(), Period.LIFETIME) >= 400) return;
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + Math.max(0.0D, amount)));
    }

    private void setAbsorption(Player player, double points) {
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Method get = handle.getClass().getMethod("getAbsorptionHearts");
            Method set = handle.getClass().getMethod("setAbsorptionHearts", float.class);
            float current = ((Number) get.invoke(handle)).floatValue();
            set.invoke(handle, Math.max(current, (float) points));
        } catch (Throwable ignored) {
            potion(player, PotionEffectType.ABSORPTION, 120, Math.max(0, (int) Math.ceil(points / 4.0D) - 1));
        }
    }

    private long until(String path, int fallbackSeconds) {
        return System.currentTimeMillis() + Math.max(1L, plugin.registry().settings().getLong(path, fallbackSeconds)) * 1000L;
    }

    private void playSound(Player player, String name) {
        try {
            player.playSound(player.getLocation(), Sound.valueOf(name.trim().toUpperCase(Locale.ENGLISH)), 1.0F, 1.0F);
        } catch (Exception ignored) { }
    }

    private void addEffectLine(List<String> lines, String format, long expires, long now) {
        if (expires <= now) return;
        long seconds = Math.max(1L, (expires - now + 999L) / 1000L);
        lines.add(format.replace("%seconds%", String.valueOf(seconds)));
    }

    private Profile profile(UUID uuid) {
        return profiles.computeIfAbsent(uuid, ignored -> new Profile());
    }

    public boolean isLoaded(UUID uuid) {
        return uuid != null && profiles.containsKey(uuid) && !loading.contains(uuid);
    }

    public int definitionCount() {
        return killstreaks.size();
    }

    public boolean purchasePending(UUID uuid) {
        return killstreakPurchaseProcessing.contains(uuid);
    }

    private LifeState life(UUID uuid) {
        return lives.computeIfAbsent(uuid, ignored -> new LifeState());
    }

    private void sanitize(Profile profile) {
        profile.purchasedMegastreaks.add("overdrive");
        if (!megastreaks.containsKey(profile.selectedMegastreak) || !profile.purchasedMegastreaks.contains(profile.selectedMegastreak)) profile.selectedMegastreak = "overdrive";
        while (profile.equipped.size() < 3) profile.equipped.add("");
        while (profile.equipped.size() > 3) profile.equipped.remove(profile.equipped.size() - 1);
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < profile.equipped.size(); index++) {
            String id = normalize(profile.equipped.get(index));
            if (!killstreaks.containsKey(id) || !profile.purchasedKillstreaks.contains(id) || !seen.add(id)) id = "";
            profile.equipped.set(index, id);
        }
    }

    private void flushDirty() {
        for (UUID uuid : new HashSet<>(dirty)) {
            Profile profile = profiles.get(uuid);
            if (profile != null && plugin.getStorageManager().putOverride("killstreaks", uuid.toString(), encode(profile))) dirty.remove(uuid);
        }
    }

    private void saveNowAsync(UUID uuid, Runnable completion) {
        String value = encode(profile(uuid));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean saved = plugin.getStorageManager().putOverride("killstreaks", uuid.toString(), value);
            if (saved) dirty.remove(uuid);
            if (completion != null) Bukkit.getScheduler().runTask(plugin, completion);
        });
    }

    private String encode(Profile profile) {
        return encode(profile, profile.purchasedKillstreaks);
    }

    private String encode(Profile profile, Set<String> purchasedKillstreaks) {
        String drops = joinLong(profile.uberdrops);
        return "p=" + String.join(",", purchasedKillstreaks)
                + ";e=" + String.join(",", profile.equipped)
                + ";m=" + String.join(",", profile.purchasedMegastreaks)
                + ";s=" + profile.selectedMegastreak
                + ";g=" + decimal(profile.goldStack)
                + ";x=" + decimal(profile.xpStack)
                + ";c=" + decimal(profile.goldCarry)
                + ";y=" + decimal(profile.xpCarry)
                + ";u=" + decimal(profile.permanentUberBonus)
                + ";b=" + (profile.beastHelmet ? "1" : "0")
                + ";d=" + drops;
    }

    private Profile decode(String raw) {
        Profile profile = new Profile();
        if (raw == null || raw.isEmpty()) return profile;
        for (String part : raw.split(";")) {
            int cut = part.indexOf('=');
            if (cut < 1) continue;
            String key = part.substring(0, cut);
            String value = part.substring(cut + 1);
            if ("p".equals(key)) addIds(profile.purchasedKillstreaks, value);
            else if ("e".equals(key)) profile.equipped = new ArrayList<>(Arrays.asList(value.split(",", -1)));
            else if ("m".equals(key)) addIds(profile.purchasedMegastreaks, value);
            else if ("s".equals(key)) profile.selectedMegastreak = normalize(value);
            else if ("g".equals(key)) profile.goldStack = number(value);
            else if ("x".equals(key)) profile.xpStack = number(value);
            else if ("c".equals(key)) profile.goldCarry = number(value);
            else if ("y".equals(key)) profile.xpCarry = number(value);
            else if ("u".equals(key)) profile.permanentUberBonus = number(value);
            else if ("b".equals(key)) profile.beastHelmet = "1".equals(value) || Boolean.parseBoolean(value);
            else if ("d".equals(key)) for (String item : value.split(",")) try { profile.uberdrops.add(Long.parseLong(item)); } catch (Exception ignored) { }
        }
        return profile;
    }

    private void addIds(Set<String> target, String raw) {
        for (String value : raw.split(",")) {
            String id = normalize(value);
            if (!id.isEmpty()) target.add(id);
        }
    }

    private String joinLong(Deque<Long> values) {
        List<String> output = new ArrayList<>();
        for (Long value : values) output.add(String.valueOf(value));
        return String.join(",", output);
    }

    private double number(String value) {
        try {
            return Math.max(0.0D, Double.parseDouble(value));
        } catch (Exception ignored) {
            return 0.0D;
        }
    }

    private String decimal(double value) {
        return BigDecimal.valueOf(Math.max(0.0D, value)).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private Material material(String name, Material fallback) {
        try {
            return Material.valueOf(name.trim().toUpperCase(Locale.ENGLISH));
        } catch (Exception exception) {
            plugin.getLogger().warning("Invalid killstreak material '" + name + "'. Using " + fallback.name() + ".");
            return fallback;
        }
    }

    private int boundedSize(int configured) {
        int value = Math.max(9, Math.min(54, configured));
        return value - value % 9;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
    }

    private ItemStack item(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(CC.color(line));
        meta.setLore(colored);
        item.setItemMeta(meta);
        return item;
    }

    private int multiply(int value, double multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, (long) Math.floor(value * Math.max(0.0D, multiplier) + 0.0000001D)));
    }

    private int safeAdd(int left, int right) {
        long value = (long) left + right;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value < 0L ? 0 : (int) value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH).replace('_', '-').replace(' ', '-').replace("&", "and");
    }

    private String canonicalId(String value) {
        return normalize(value).toUpperCase(Locale.ENGLISH).replace('-', '_');
    }

    private String readable(String value) {
        StringBuilder output = new StringBuilder();
        for (String word : normalize(value).split("-")) {
            if (word.isEmpty()) continue;
            if (output.length() > 0) output.append(' ');
            output.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return output.toString();
    }

    private String format(long value) {
        return String.format(Locale.US, "%,d", Math.max(0L, value));
    }

    private String roman(int value) {
        if (value <= 0) return "0";
        int[] numbers = {50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < numbers.length; index++) while (value >= numbers[index]) {
            result.append(numerals[index]);
            value -= numbers[index];
        }
        return result.toString();
    }

    public static final class Reward {
        private final int xp;
        private final int gold;

        public Reward(int xp, int gold) {
            this.xp = xp;
            this.gold = gold;
        }

        public int xp() {
            return xp;
        }

        public int gold() {
            return gold;
        }
    }

    private static final class KillstreakDefinition {
        private final String id;
        private final String name;
        private final Material material;
        private final short data;
        private final int slot;
        private final int price;
        private final int requiredLevel;
        private final int requiredPrestige;
        private final String requiredBundle;
        private final int interval;
        private final String effect;
        private final List<String> lore;
        private final RegistryNode values;

        private KillstreakDefinition(String id, String name, Material material, short data, int slot, int price,
                                     int requiredLevel, int requiredPrestige, String requiredBundle, int interval,
                                     String effect, List<String> lore, RegistryNode values) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.data = data;
            this.slot = slot;
            this.price = price;
            this.requiredLevel = requiredLevel;
            this.requiredPrestige = requiredPrestige;
            this.requiredBundle = requiredBundle;
            this.interval = interval;
            this.effect = effect;
            this.lore = lore;
            this.values = values;
        }
    }

    private static final class MegastreakDefinition {
        private final String id;
        private final String name;
        private final Material material;
        private final short data;
        private final int slot;
        private final int activationStreak;
        private final int requiredLevel;
        private final int requiredPrestige;
        private final int bundleRenownPrice;
        private final int price;
        private final RegistryNode values;

        private MegastreakDefinition(String id, String name, Material material, short data, int slot, int activationStreak,
                                     int requiredLevel, int requiredPrestige, int bundleRenownPrice, int price, RegistryNode values) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.data = data;
            this.slot = slot;
            this.activationStreak = activationStreak;
            this.requiredLevel = requiredLevel;
            this.requiredPrestige = requiredPrestige;
            this.bundleRenownPrice = bundleRenownPrice;
            this.price = price;
            this.values = values;
        }
    }

    private static final class Profile {
        private final Set<String> purchasedKillstreaks = new LinkedHashSet<>();
        private List<String> equipped = new ArrayList<>(Arrays.asList("", "", ""));
        private final Set<String> purchasedMegastreaks = new LinkedHashSet<>(Collections.singleton("overdrive"));
        private String selectedMegastreak = "overdrive";
        private double goldStack;
        private double xpStack;
        private double goldCarry;
        private double xpCarry;
        private double permanentUberBonus;
        private boolean beastHelmet;
        private final Deque<Long> uberdrops = new ArrayDeque<>();
    }

    private static final class LifeState {
        private final Set<String> processedTriggers = new HashSet<>();
        private final Set<Integer> hermitBedrockAwarded = new HashSet<>();
        private boolean activeMegastreak;
        private boolean deathProcessed;
        private int activationStreak;
        private long feastUntil;
        private long counterStrikeUntil;
        private long flightDamageUntil;
        private long trueDamageImmuneUntil;
        private boolean hermitTrueDamageImmune;
        private int toughSkinStacks;
        private int monsterStacks;
        private int khanateStacks;
        private int rushStacks;
        private int superStreakerStacks;
        private boolean iceCube;
        private boolean leech;
        private boolean assuredStrike;
        private boolean glassPickaxeTrueDamage;
        private long toMoonXp;
        private double baseMaxHealth;
    }

    private enum MenuType {
        LOADOUT,
        KILLSTREAK_CHOOSER,
        MEGASTREAK_CHOOSER
    }

    private static final class MenuHolder implements InventoryHolder {
        private final UUID owner;
        private final MenuType type;
        private final int selectedSlot;
        private final Map<Integer, String> actions = new HashMap<>();
        private Inventory inventory;

        private MenuHolder(UUID owner, MenuType type, int selectedSlot) {
            this.owner = owner;
            this.type = type;
            this.selectedSlot = selectedSlot;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class RenownBundleHolder implements InventoryHolder {
        private final UUID owner;
        private final Map<Integer, String> actions = new HashMap<>();
        private Inventory inventory;

        private RenownBundleHolder(UUID owner) {
            this.owner = owner;
        }

        public Inventory getInventory() {
            return inventory;
        }
    }
}
