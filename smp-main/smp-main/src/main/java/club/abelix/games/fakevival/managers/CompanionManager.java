package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.DyeColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vex;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Phantom;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CompanionManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final CustomItemManager customItemManager;
    private final SkillManager skillManager;
    private final NamespacedKey companionIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey markerKey;
    private final NamespacedKey ownerUuidKey;
    private final Map<UUID, UUID> activeEntities = new ConcurrentHashMap<>();
    private int taskId = -1;

    public CompanionManager(JavaPlugin plugin, DataManager dataManager, CustomItemManager customItemManager, SkillManager skillManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.customItemManager = customItemManager;
        this.skillManager = skillManager;
        this.companionIdKey = new NamespacedKey(plugin, "companion_id");
        this.ownerKey = new NamespacedKey(plugin, "companion_owner");
        this.markerKey = new NamespacedKey(plugin, "smp_companion");
        this.ownerUuidKey = new NamespacedKey(plugin, "companion_owner_uuid");
    }

    public void start() {
        if (taskId != -1) return;
        for (var world : Bukkit.getWorlds()) for (Entity entity : world.getEntities()) {
            if (entity.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE)) entity.remove();
        }
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickActiveCompanions, 40L, 40L).getTaskId();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
        for (UUID entityId : activeEntities.values()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) entity.remove();
        }
        activeEntities.clear();
    }

    public List<String> getCompanionIds() {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("companions.types");
        if (section == null) return List.of();
        List<String> ids = new ArrayList<>(section.getKeys(false));
        ids.sort(Comparator.comparing(this::category).thenComparingInt(id -> rarityRank(rarity(id)))
                .thenComparingInt(id -> club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".slot", 10)));
        return ids;
    }

    public String category(String id) { return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".category", "Misc"); }
    public String rarity(String id) { return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".rarity", "COMMON").toUpperCase(Locale.ROOT); }
    private int rarityRank(String rarity) { return List.of("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC").indexOf(rarity); }

    public boolean canCapture(Entity entity) {
        if (entity == null) return false;
        return companionIdFor(entity.getType()) != null;
    }

    public String companionIdFor(EntityType type) {
        for (String id : getCompanionIds()) {
            String configured = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".entity", "");
            if (configured.equalsIgnoreCase(type.name())) return normalize(id);
        }
        return null;
    }

    public boolean tryCapture(Player player, LivingEntity entity, ItemStack item) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("companions.enabled", true)) return false;
        String captureItem = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("companions.capture-item", "companion_snare"));
        if (!captureItem.equals(normalize(customItemManager.getItemId(item)))) return false;
        String id = companionIdFor(entity.getType());
        if (id == null) {
            player.sendMessage(CC.color("&cThat mob cannot be captured as a companion."));
            return true;
        }
        if (owns(player, id)) {
            player.sendMessage(CC.color("&eYou already captured &f" + displayName(id) + "&e."));
            return true;
        }
        consumeOne(item);
        unlock(player, id);
        entity.remove();
        if (skillManager != null) {
            int xp = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("companions.leveling.capture-taming-xp",
                    club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-action.taming", 6)));
            skillManager.addXp(player, "taming", xp);
        }
        player.sendMessage(CC.color("&aCaptured &f" + displayName(id) + "&a. Use &e/companions &ato summon it."));
        return true;
    }

    public boolean owns(Player player, String id) {
        return dataManager.getData().getBoolean(playerPath(player) + ".owned." + normalize(id), false);
    }

    public void unlock(Player player, String id) {
        dataManager.getData().set(playerPath(player) + ".owned." + normalize(id), true);
        if (selected(player).isBlank()) dataManager.getData().set(playerPath(player) + ".selected", normalize(id));
        dataManager.save();
    }

    public String selected(Player player) {
        return normalize(dataManager.getData().getString(playerPath(player) + ".selected", ""));
    }

    public int level(Player player, String id) {
        return Math.max(1, dataManager.getData().getInt(companionPath(player, id) + ".level", 1));
    }

    public int xp(Player player, String id) {
        return Math.max(0, dataManager.getData().getInt(companionPath(player, id) + ".xp", 0));
    }

    public int xpNeeded(Player player, String id) {
        int level = level(player, id);
        int base = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("companions.leveling.base-xp", 75));
        int perLevel = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("companions.leveling.per-level-xp", 35));
        return base + (level - 1) * perLevel;
    }

    public void addKillXp(Player player, LivingEntity killed) {
        String id = selected(player);
        if (id.isBlank() || !owns(player, id)) return;
        if (!isActive(player)) return;
        int amount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("companions.leveling.mob-kill-xp", 10));
        String basePath = companionPath(player, id);
        int level = level(player, id);
        int maxLevel = maxLevel(id);
        int xp = xp(player, id) + amount;
        while (level < maxLevel && xp >= xpNeededForLevel(level)) {
            xp -= xpNeededForLevel(level);
            level++;
            player.sendMessage(CC.color("&aYour &f" + displayName(id) + " &ais now level &e" + level + "&a."));
        }
        if (level >= maxLevel) xp = 0;
        dataManager.getData().set(basePath + ".level", level);
        dataManager.getData().set(basePath + ".xp", xp);
        dataManager.save();
        refreshActiveName(player, id);
    }

    public boolean summonSelected(Player player) {
        String id = selected(player);
        if (id.isBlank() || !owns(player, id)) {
            player.sendMessage(CC.color("&cSelect a captured companion first."));
            return false;
        }
        return summon(player, id);
    }

    public boolean summon(Player player, String id) {
        id = normalize(id);
        if (!owns(player, id)) return false;
        long cooldownLeft = cooldownLeftMillis(player);
        if (cooldownLeft > 0) {
            player.sendMessage(CC.color("&cYour companion is recovering. Wait &e" + formatDuration(cooldownLeft) + "&c."));
            return false;
        }
        despawn(player, false);
        EntityType type = entityType(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".entity", "WOLF"));
        Location spawn = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(-1.5D));
        Entity entity = player.getWorld().spawnEntity(spawn, type);
        if (!(entity instanceof LivingEntity living)) {
            entity.remove();
            return false;
        }
        living.getPersistentDataContainer().set(companionIdKey, PersistentDataType.STRING, id);
        living.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        living.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        living.getPersistentDataContainer().set(ownerUuidKey, PersistentDataType.STRING, player.getUniqueId().toString());
        living.setCustomNameVisible(true);
        if (living instanceof Ageable ageable) ageable.setAdult();
        if (living instanceof Vex vex) {
            vex.setLimitedLifetime(false);
            vex.setLimitedLifetimeTicks(-1);
            vex.setBound(player.getLocation());
        }
        if (living instanceof Tameable tameable) {
            tameable.setTamed(true);
            tameable.setOwner(player);
        }
        applyVariant(id, living);
        if (isCosmetic(id)) {
            living.setInvulnerable(true);
            living.setCollidable(false);
            living.setCanPickupItems(false);
            living.setRemoveWhenFarAway(false);
            if (living instanceof Mob mob) {
                mob.setTarget(null);
                mob.setAware(false);
            }
            if (living.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                Objects.requireNonNull(living.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(0.0D);
            }
        }
        applyStats(player, id, living);
        activeEntities.put(player.getUniqueId(), living.getUniqueId());
        dataManager.getData().set(playerPath(player) + ".selected", id);
        dataManager.save();
        player.sendMessage(CC.color("&aSummoned &f" + displayName(id) + "&a."));
        sendSummonMessages(player, id);
        return true;
    }

    public void despawn(Player player, boolean message) {
        UUID entityId = activeEntities.remove(player.getUniqueId());
        Entity entity = entityId == null ? null : Bukkit.getEntity(entityId);
        if (entity != null) entity.remove();
        if (message) player.sendMessage(CC.color("&eCompanion despawned."));
    }

    public boolean isCompanion(Entity entity) {
        return entity instanceof LivingEntity living && living.getPersistentDataContainer().has(companionIdKey, PersistentDataType.STRING);
    }

    public boolean useCompanionItem(Player player, ItemStack item) {
        String id = normalize(customItemManager.getItemId(item));
        if (id.isBlank() || !getCompanionIds().contains(id)) return false;
        if (!owns(player, id)) {
            unlock(player, id);
            consumeOne(item);
            player.sendMessage(CC.color("&aUnlocked &f" + displayName(id) + "&a."));
        }
        summon(player, id);
        return true;
    }

    public UUID owner(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return null;
        String owner = living.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        try {
            return UUID.fromString(String.valueOf(owner));
        } catch (Exception ignored) {
            return null;
        }
    }

    public String companionId(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return "";
        return normalize(living.getPersistentDataContainer().get(companionIdKey, PersistentDataType.STRING));
    }

    public void handleDeath(LivingEntity entity) {
        UUID owner = owner(entity);
        if (owner == null) return;
        activeEntities.remove(owner);
        long cooldown = Math.max(0L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("companions.death-cooldown-seconds", 300L)) * 1000L;
        dataManager.getData().set("players." + owner + ".companions.cooldown-until", System.currentTimeMillis() + cooldown);
        dataManager.save();
        Player player = Bukkit.getPlayer(owner);
        if (player != null) player.sendMessage(CC.color("&cYour companion was defeated. Respawn cooldown: &e" + formatDuration(cooldown) + "&c."));
    }

    public boolean upgrade(Player player, String id, String upgrade) {
        id = normalize(id);
        upgrade = normalize(upgrade);
        if (!owns(player, id)) return false;
        String upgradePath = path(id) + ".upgrades." + upgrade;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(upgradePath)) return false;
        int current = upgradeLevel(player, id, upgrade);
        int max = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(upgradePath + ".max-level", 5));
        if (current >= max) {
            player.sendMessage(CC.color("&cThat upgrade is already maxed."));
            return false;
        }
        String itemId = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(upgradePath + ".item", "companion_treat"));
        int amount = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(upgradePath + ".amount", current + 1));
        if (!removeCustomItems(player, itemId, amount)) {
            player.sendMessage(CC.color("&cYou need &e" + amount + "x " + pretty(itemId) + " &cto upgrade this companion."));
            return false;
        }
        dataManager.getData().set(companionPath(player, id) + ".upgrades." + upgrade, current + 1);
        dataManager.save();
        player.sendMessage(CC.color("&aUpgraded &f" + displayName(id) + " &a" + pretty(upgrade) + " to &e" + (current + 1) + "&a."));
        if (selected(player).equals(id) && isActive(player)) {
            Entity entity = Bukkit.getEntity(activeEntities.get(player.getUniqueId()));
            if (entity instanceof LivingEntity living) applyStats(player, id, living);
        }
        return true;
    }

    public int upgradeLevel(Player player, String id, String upgrade) {
        return Math.max(0, dataManager.getData().getInt(companionPath(player, id) + ".upgrades." + normalize(upgrade), 0));
    }

    public List<String> upgrades(String id) {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(path(id) + ".upgrades");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    public List<String> skills(String id) {
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection(path(id) + ".skills");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    public double health(Player player, String id) {
        int level = level(player, id);
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".base-health", 20.0D)
                + (level - 1) * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".health-per-level", 1.5D)
                + upgradeLevel(player, id, "vitality") * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".upgrade-stats.vitality.health", 4.0D)
                + upgradeLevel(player, id, "fortitude") * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".upgrade-stats.fortitude.health", 2.0D);
    }

    public double defense(Player player, String id) {
        int level = level(player, id);
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".base-defense", 0.0D)
                + (level - 1) * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".defense-per-level", 0.15D)
                + upgradeLevel(player, id, "vitality") * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".upgrade-stats.vitality.defense", 0.5D)
                + upgradeLevel(player, id, "fortitude") * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".upgrade-stats.fortitude.defense", 1.25D);
    }

    public double agility(Player player, String id) {
        int level = level(player, id);
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".base-agility", 10.0D)
                + (level - 1) * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".agility-per-level", 0.4D)
                + upgradeLevel(player, id, "agility") * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".upgrade-stats.agility.agility", 3.0D)
                + upgradeLevel(player, id, "focus") * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".upgrade-stats.focus.agility", 1.0D);
    }

    public double damage(Player player, String id) {
        int level = level(player, id);
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".base-damage", 3.0D)
                + (level - 1) * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".damage-per-level", 0.25D)
                + upgradeLevel(player, id, "ferocity") * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".upgrade-stats.ferocity.damage", 1.0D)
                + upgradeLevel(player, id, "focus") * club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".upgrade-stats.focus.damage", 0.5D);
    }

    public String displayName(String id) {
        return CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".name", pretty(id)));
    }

    public Material icon(String id) {
        return material(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".icon", "BONE"));
    }

    public int maxLevel(String id) {
        return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path(id) + ".max-level", 25));
    }

    public long cooldownLeftMillis(Player player) {
        return Math.max(0L, dataManager.getData().getLong(playerPath(player) + ".cooldown-until", 0L) - System.currentTimeMillis());
    }

    public boolean isActive(Player player) {
        UUID entityId = activeEntities.get(player.getUniqueId());
        Entity entity = entityId == null ? null : Bukkit.getEntity(entityId);
        return entity instanceof LivingEntity living && !living.isDead();
    }

    public void attackOwnerTarget(Player owner, LivingEntity target) {
        if (owner == null || target == null || target.isDead() || target instanceof Player || isCompanion(target)) return;
        UUID entityId = activeEntities.get(owner.getUniqueId());
        Entity entity = entityId == null ? null : Bukkit.getEntity(entityId);
        if (entity instanceof Mob mob && !mob.isDead() && !isCosmetic(companionId(entity))) {
            mob.setTarget(target);
        }
    }

    private void tickActiveCompanions() {
        for (Map.Entry<UUID, UUID> entry : new ArrayList<>(activeEntities.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            Entity entity = Bukkit.getEntity(entry.getValue());
            if (player == null || entity == null || entity.isDead()) {
                activeEntities.remove(entry.getKey());
                continue;
            }
            if (entity.getWorld() != player.getWorld() || entity.getLocation().distanceSquared(player.getLocation()) > 144.0D) {
                entity.teleport(player.getLocation());
            } else if (isCosmetic(companionId(entity)) && entity.getLocation().distanceSquared(player.getLocation()) > 9.0D) {
                entity.teleport(player.getLocation().add(player.getLocation().getDirection().normalize().multiply(-1.5D)));
            } else if (entity instanceof Mob mob && mob.getTarget() == null && !isCosmetic(companionId(entity))) {
                mob.getPathfinder().moveTo(player.getLocation(), 1.2D);
            }
            if (entity instanceof LivingEntity living) {
                if (living instanceof Vex vex) vex.setBound(player.getLocation());
                tickCompanionSkills(player, companionId(living), living);
            }
        }
    }

    private void refreshActiveName(Player player, String id) {
        UUID entityId = activeEntities.get(player.getUniqueId());
        Entity entity = entityId == null ? null : Bukkit.getEntity(entityId);
        if (entity instanceof LivingEntity living) applyName(player, id, living);
    }

    private void applyStats(Player player, String id, LivingEntity entity) {
        double health = health(player, id);
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(Math.max(1.0D, health));
        }
        entity.setHealth(Math.clamp(health, 1.0D, entity.getMaxHealth()));
        if (entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            Objects.requireNonNull(entity.getAttribute(Attribute.ATTACK_DAMAGE)).setBaseValue(Math.max(1.0D, damage(player, id)));
        }
        if (entity.getAttribute(Attribute.MOVEMENT_SPEED) != null) {
            double speed = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(path(id) + ".base-speed", 0.25D) + agility(player, id) * 0.002D;
            Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(Math.max(0.05D, speed));
        }
        applyName(player, id, entity);
    }

    public boolean isCosmetic(String id) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean(path(id) + ".cosmetic-only", false);
    }

    private void applyVariant(String id, LivingEntity living) {
        String variant = normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path(id) + ".variant", "normal"));
        if (living instanceof Slime slime && variant.equals("small")) slime.setSize(1);
        if (living instanceof Zombie zombie && variant.equals("baby")) zombie.setBaby(true);
        if (living instanceof Ageable ageable && variant.equals("baby")) ageable.setBaby();
        if (living instanceof Fox fox) fox.setFoxType(variant.equals("snow") ? Fox.Type.SNOW : Fox.Type.RED);
        if (living instanceof Sheep sheep) sheep.setColor(variant.equals("pink") ? DyeColor.PINK : DyeColor.LIGHT_BLUE);
        if (living instanceof Parrot parrot) parrot.setVariant(variant.equals("blue") ? Parrot.Variant.BLUE : Parrot.Variant.GREEN);
        if (living instanceof Axolotl axolotl) axolotl.setVariant(variant.equals("blue") ? Axolotl.Variant.BLUE : Axolotl.Variant.LUCY);
        if (living instanceof Phantom phantom && variant.equals("small")) phantom.setSize(0);
        if (variant.equals("small") && living.getAttribute(Attribute.SCALE) != null) {
            Objects.requireNonNull(living.getAttribute(Attribute.SCALE)).setBaseValue(0.45D);
        }
    }

    private void applyName(Player player, String id, LivingEntity entity) {
        entity.setCustomName(CC.color(displayName(id) + " &7Lv. &e" + level(player, id)));
    }

    public double reduceIncomingDamage(Player player, String id, double damage) {
        double reduction = Math.min(0.60D, defense(player, id) * 0.01D);
        return Math.max(0.0D, damage * (1.0D - reduction));
    }

    private void tickCompanionSkills(Player player, String id, LivingEntity companion) {
        for (String skill : skills(id)) {
            String base = path(id) + ".skills." + skill;
            int unlock = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(base + ".unlock-level", 1));
            if (level(player, id) < unlock) continue;
            switch (normalize(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(base + ".type", skill))) {
                case "regen" -> {
                    double heal = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + ".heal", 0.5D));
                    if (heal > 0.0D && player.getHealth() < player.getMaxHealth()) {
                        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + heal));
                    }
                }
                case "guard" -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 0, true, false, true));
                case "speed" -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, true, false, true));
                case "strength" -> player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, 0, true, false, true));
                case "nightvision" -> player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 260, 0, true, false, true));
                case "self_regen" -> {
                    double heal = Math.max(0.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble(base + ".heal", 0.75D));
                    if (heal > 0.0D && companion.getHealth() < companion.getMaxHealth()) {
                        companion.setHealth(Math.min(companion.getMaxHealth(), companion.getHealth() + heal));
                    }
                }
                default -> {
                }
            }
        }
    }

    private void sendSummonMessages(Player player, String id) {
        List<String> messages = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(path(id) + ".summon-messages");
        if (messages.isEmpty()) return;
        long delay = 10L;
        for (String message : messages) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && selected(player).equals(id) && isActive(player)) {
                    player.sendMessage(CC.color(message.replace("%player%", player.getName()).replace("%companion%", ChatColor.stripColor(displayName(id)))));
                }
            }, delay);
            delay += 35L;
        }
    }

    private int xpNeededForLevel(int level) {
        int base = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("companions.leveling.base-xp", 75));
        int perLevel = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("companions.leveling.per-level-xp", 35));
        return base + (Math.max(1, level) - 1) * perLevel;
    }

    private boolean removeCustomItems(Player player, String itemId, int amount) {
        int found = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && itemId.equals(normalize(customItemManager.getItemId(item)))) found += item.getAmount();
        }
        if (found < amount) return false;
        int left = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && left > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || !itemId.equals(normalize(customItemManager.getItemId(item)))) continue;
            int take = Math.min(left, item.getAmount());
            item.setAmount(item.getAmount() - take);
            left -= take;
            if (item.getAmount() <= 0) contents[i] = null;
        }
        player.getInventory().setStorageContents(contents);
        return true;
    }

    private void consumeOne(ItemStack item) {
        if (item == null) return;
        item.setAmount(Math.max(0, item.getAmount() - 1));
    }

    private EntityType entityType(String name) {
        try {
            return EntityType.valueOf(String.valueOf(name).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return EntityType.WOLF;
        }
    }

    private Material material(String name) {
        try {
            return Material.valueOf(String.valueOf(name).toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Material.BONE;
        }
    }

    private String playerPath(Player player) {
        return "players." + player.getUniqueId() + ".companions";
    }

    private String companionPath(Player player, String id) {
        return playerPath(player) + ".data." + normalize(id);
    }

    private String path(String id) {
        return "companions.types." + normalize(id);
    }

    public String formatDuration(long millis) {
        long seconds = Math.max(1L, millis / 1000L);
        long minutes = seconds / 60L;
        if (minutes > 0) return minutes + "m " + (seconds % 60L) + "s";
        return seconds + "s";
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String pretty(String text) {
        String[] parts = normalize(ChatColor.stripColor(CC.color(String.valueOf(text)))).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
