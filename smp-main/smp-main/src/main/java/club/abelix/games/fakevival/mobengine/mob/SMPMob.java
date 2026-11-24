package club.abelix.games.fakevival.mobengine.mob;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class SMPMob {
    public enum State {
        FULL_COMBAT,
        MEDIUM_AI,
        SLOW_AI,
        SLEEPING,
        VIRTUALIZED
    }

    private final UUID uuid;
    private final String mobId;
    private final LivingEntity entity;
    private BossBar bossBar;
    private final double maxMmoHealth;
    private final double attackDamage;
    private final String displayName;
    private final int level;
    private final List<ItemStack> customDrops;
    private final NamespacedKey currentHealthKey;
    private final NamespacedKey maxHealthKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey displayNameKey;
    private final NamespacedKey pdcMobIdKey;
    private boolean externalDropController;
    private boolean debugMob;
    private double mmoHealth;
    private State state = State.SLOW_AI;
    private RenderTier renderTier = RenderTier.HIDDEN;
    private boolean nameplateDirty = true;
    private long lastNameplateUpdateTick = -1000L;
    private long lastPathfindTick;
    private long lastTargetScanTick;
    private long lastAttackTick;
    private long lastCombatTime;
    private long forceFullAiUntil;

    public SMPMob(String mobId, LivingEntity entity, double maxMmoHealth, double attackDamage, String displayName,
                  int level, List<ItemStack> customDrops, NamespacedKey legacyMarkerKey,
                  NamespacedKey engineMarkerKey, NamespacedKey mobIdKey, NamespacedKey mobUuidKey) {
        this.uuid = entity.getUniqueId();
        this.mobId = mobId;
        this.entity = entity;
        this.maxMmoHealth = Math.max(1.0D, maxMmoHealth);
        this.attackDamage = Math.max(0.0D, attackDamage);
        this.displayName = displayName == null || displayName.isBlank() ? mobId : ChatColor.translateAlternateColorCodes('&', displayName);
        this.level = Math.max(1, level);
        this.customDrops = customDrops == null ? List.of() : new ArrayList<>(customDrops);
        this.mmoHealth = this.maxMmoHealth;
        this.currentHealthKey = NamespacedKey.fromString(engineMarkerKey.getNamespace() + ":smp_mob_current_health");
        this.maxHealthKey = NamespacedKey.fromString(engineMarkerKey.getNamespace() + ":smp_mob_max_health");
        this.levelKey = NamespacedKey.fromString(engineMarkerKey.getNamespace() + ":smp_mob_level");
        this.displayNameKey = NamespacedKey.fromString(engineMarkerKey.getNamespace() + ":smp_mob_display_name");
        this.pdcMobIdKey = legacyMarkerKey;
        entity.getPersistentDataContainer().set(legacyMarkerKey, PersistentDataType.STRING, mobId);
        entity.getPersistentDataContainer().set(engineMarkerKey, PersistentDataType.BYTE, (byte) 1);
        entity.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, mobId);
        entity.getPersistentDataContainer().set(mobUuidKey, PersistentDataType.STRING, uuid.toString());
        entity.setRemoveWhenFarAway(false);
        entity.setCustomNameVisible(true);
        entity.setAI(true);
        if (entity instanceof Mob mob) mob.setAware(true);
        updateNameplate();
    }

    public UUID getUuid() { return uuid; }
    public String getMobId() { return mobId; }
    public LivingEntity getEntity() { return entity; }
    public double getMaxMmoHealth() { return maxMmoHealth; }
    public double getMmoHealth() { return mmoHealth; }
    public double getAttackDamage() { return attackDamage; }
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public List<ItemStack> getCustomDrops() { return Collections.unmodifiableList(customDrops); }
    public boolean hasCustomDrops() { return !customDrops.isEmpty(); }
    public void setCustomDrops(List<ItemStack> drops) {
        customDrops.clear();
        if (drops != null) {
            for (ItemStack drop : drops) {
                if (drop != null && !drop.getType().isAir()) customDrops.add(drop.clone());
            }
        }
    }
    public boolean hasExternalDropController() { return externalDropController; }
    public void setExternalDropController(boolean externalDropController) { this.externalDropController = externalDropController; }
    public boolean isDebugMob() { return debugMob; }
    public void setDebugMob(boolean debugMob) { this.debugMob = debugMob; }

    public boolean isValid() {
        Entity e = entity;
        return e != null && e.isValid() && !e.isDead();
    }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
    public RenderTier getRenderTier() { return renderTier; }
    public void setRenderTier(RenderTier renderTier) { this.renderTier = renderTier == null ? RenderTier.HIDDEN : renderTier; }
    public boolean isImportant() {
        String id = mobId.toLowerCase(java.util.Locale.ROOT);
        return maxMmoHealth >= 5_000.0D || id.contains("boss") || id.contains("elite") || id.contains("mini");
    }
    public long getLastPathfindTick() { return lastPathfindTick; }
    public void setLastPathfindTick(long lastPathfindTick) { this.lastPathfindTick = lastPathfindTick; }
    public long getLastTargetScanTick() { return lastTargetScanTick; }
    public void setLastTargetScanTick(long lastTargetScanTick) { this.lastTargetScanTick = lastTargetScanTick; }
    public long getLastAttackTick() { return lastAttackTick; }
    public void setLastAttackTick(long lastAttackTick) { this.lastAttackTick = lastAttackTick; }
    public long getLastCombatTime() { return lastCombatTime; }
    public long getForceFullAiUntil() { return forceFullAiUntil; }
    public boolean isCombatLocked(long now) { return now < forceFullAiUntil; }
    public void markCombat(long now) {
        lastCombatTime = now;
        forceFullAiUntil = Math.max(forceFullAiUntil, now + 5000L);
    }

    public void damageMmo(double amount) {
        mmoHealth = Math.max(0.0D, mmoHealth - Math.max(0.0D, amount));
        markNameplateDirty();
    }

    public void setMmoHealth(double health) {
        mmoHealth = Math.clamp(health, 0.0D, maxMmoHealth);
        markNameplateDirty();
    }

    public void updateNameplate() {
        entity.getPersistentDataContainer().set(currentHealthKey, PersistentDataType.DOUBLE, mmoHealth);
        entity.getPersistentDataContainer().set(maxHealthKey, PersistentDataType.DOUBLE, maxMmoHealth);
        entity.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);
        entity.getPersistentDataContainer().set(displayNameKey, PersistentDataType.STRING, ChatColor.stripColor(displayName));
        entity.getPersistentDataContainer().set(pdcMobIdKey, PersistentDataType.STRING, mobId);
        String text = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Lv." + level + ChatColor.DARK_GRAY + "] "
                + ChatColor.RED + displayName + " "
                + ChatColor.GREEN + (int) Math.ceil(mmoHealth) + ChatColor.GRAY + "/" + ChatColor.GREEN + (int) Math.ceil(maxMmoHealth)
                + ChatColor.RED + "❤";
        entity.setCustomName(ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Lv." + level + ChatColor.DARK_GRAY + "] "
                + ChatColor.RED + ChatColor.stripColor(displayName) + " "
                + ChatColor.GREEN + (int) Math.ceil(mmoHealth) + "/" + (int) Math.ceil(maxMmoHealth)
                + ChatColor.RED + "\u2764");
        if (isRealBoss()) {
            BossBar bar = bossBar();
            bar.setTitle(ChatColor.RED + displayName + ChatColor.GRAY + " - " + ChatColor.GREEN
                    + (int) Math.ceil(mmoHealth) + ChatColor.GRAY + "/" + ChatColor.GREEN
                    + (int) Math.ceil(maxMmoHealth) + ChatColor.RED + "❤");
            bar.setProgress(Math.max(0.0D, Math.min(1.0D, mmoHealth / maxMmoHealth)));
        } else hideBossBar();
        nameplateDirty = false;
    }

    public boolean updateNameplate(boolean force, long tick) {
        if (!force && !nameplateDirty && tick - lastNameplateUpdateTick < SMPMobEngineSettings.NAMETAG_UPDATE_INTERVAL_TICKS) {
            return false;
        }
        updateNameplate();
        lastNameplateUpdateTick = tick;
        return true;
    }

    public void markNameplateDirty() {
        nameplateDirty = true;
    }

    public boolean shouldUpdateNameplate(long tick, int intervalTicks) {
        return nameplateDirty || tick - lastNameplateUpdateTick >= Math.max(1, intervalTicks);
    }

    public void updateNearbyBossBarViewers() {
        if (!isRealBoss() || state == State.SLEEPING || state == State.VIRTUALIZED || !isValid()) {
            hideBossBar();
            return;
        }
        BossBar bar = bossBar();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(entity.getWorld())) {
                bar.removePlayer(player);
                continue;
            }
            if (player.getLocation().distanceSquared(entity.getLocation()) <= 24.0D * 24.0D) {
                bar.addPlayer(player);
            } else {
                bar.removePlayer(player);
            }
        }
    }

    public boolean isRealBoss() {
        String entityType = pdcString("smp_entity_type");
        if ("ZONE_BOSS".equalsIgnoreCase(entityType) || "DUNGEON_BOSS".equalsIgnoreCase(entityType)) return true;
        if (pdcTrue("smp_boss") || hasPdc("is_zone_boss") || hasPdc("dungeon_boss") || hasPdc("zone_boss")) return true;
        String id = mobId.toLowerCase(java.util.Locale.ROOT);
        return id.contains("_boss_") && !id.contains("minion") && !id.contains("summon");
    }

    private BossBar bossBar() {
        if (bossBar == null) bossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_10);
        return bossBar;
    }

    private boolean hasPdc(String key) { return entity.getPersistentDataContainer().getKeys().stream().anyMatch(value -> value.getKey().equalsIgnoreCase(key)); }
    private String pdcString(String key) { for (NamespacedKey value : entity.getPersistentDataContainer().getKeys()) if (value.getKey().equalsIgnoreCase(key)) return entity.getPersistentDataContainer().get(value, PersistentDataType.STRING); return null; }
    private boolean pdcTrue(String key) { for (NamespacedKey value : entity.getPersistentDataContainer().getKeys()) if (value.getKey().equalsIgnoreCase(key)) { Byte flag=entity.getPersistentDataContainer().get(value,PersistentDataType.BYTE); return flag != null && flag != 0; } return false; }
    public void hideBossBar() { if (bossBar != null) bossBar.removeAll(); }
    public void destroyBossBar() { if (bossBar != null) { bossBar.removeAll(); bossBar = null; } }
    public Location getLocation() { return entity.getLocation(); }

    public enum RenderTier {
        FULL,
        BASIC,
        HIDDEN
    }
}
