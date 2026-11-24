package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.SoundUtil;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class SkillManager implements Listener {
    private static final double BUKKIT_MAX_HEALTH_CAP = 1024.0D;
    private static final double BUKKIT_MIN_HEALTH = 1.0D;

    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final ProgressBossBarManager bossBarManager;
    private final BattlePassManager battlePassManager;
    private final Set<UUID> cappedBukkitHealthLogged = new HashSet<>();
    private boolean missingMaxHealthAttributeLogged;
    private PlayerLevelManager playerLevelManager;
    private BiFunction<Player, String, Double> eventXpMultiplier = (player, skill) -> 1.0D;
    private Function<Player, Double> consumableXpMultiplier = player -> 1.0D;
    private BiFunction<Player, String, Double> passiveSkillXpMultiplier = (player, skill) -> 1.0D;
    private final Set<UUID> loadedSkillData = ConcurrentHashMap.newKeySet();
    private final Set<UUID> loadingSkillData = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastSkillLoad = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String,Integer>> lastDatabaseSkills = new ConcurrentHashMap<>();
    private static final Set<String> CORE_SKILLS = Set.of("mining","combat","farming","fishing","foraging");

    public SkillManager(JavaPlugin plugin, DataManager dataManager, ProgressBossBarManager bossBarManager, BattlePassManager battlePassManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.bossBarManager = bossBarManager;
        this.battlePassManager = battlePassManager;
    }

    public String playerPath(Player player) {
        return "players." + player.getUniqueId();
    }

    public void setPlayerLevelManager(PlayerLevelManager playerLevelManager) {
        this.playerLevelManager = playerLevelManager;
    }

    public void setEventXpMultiplier(BiFunction<Player, String, Double> eventXpMultiplier) {
        this.eventXpMultiplier = eventXpMultiplier == null ? (player, skill) -> 1.0D : eventXpMultiplier;
    }

    public void setConsumableXpMultiplier(Function<Player, Double> consumableXpMultiplier) {
        this.consumableXpMultiplier = consumableXpMultiplier == null ? player -> 1.0D : consumableXpMultiplier;
    }

    public void setPassiveSkillXpMultiplier(BiFunction<Player, String, Double> passiveSkillXpMultiplier) {
        this.passiveSkillXpMultiplier = passiveSkillXpMultiplier == null ? (player, skill) -> 1.0D : passiveSkillXpMultiplier;
    }

    private int xpToLevel(int xp) {
        int level = 1;
        int remaining = Math.max(0, xp);
        while (remaining >= getXpNeededForLevel(level)) {
            remaining -= getXpNeededForLevel(level);
            level++;
        }
        return level;
    }

    private int getXpPerLevel() {
        return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-level", 100));
    }

    public int getXp(Player player, String skill) {
        if (skill == null) return 0;
        return dataManager.getData().getInt(playerPath(player) + ".skills." + skill.toLowerCase() + ".xp", 0);
    }

    public int getLevel(Player player, String skill) {
        return xpToLevel(getXp(player, skill));
    }

    public boolean isSkillDataLoaded(UUID uuid) {
        return uuid != null && loadedSkillData.contains(uuid);
    }

    @EventHandler
    public void onSkillJoin(PlayerJoinEvent event) {
        if(plugin instanceof club.abelix.games.fakevival.Game game&&game.getPlayerDataSyncManager().isEnabled())return;
        loadSkillData(event.getPlayer());
    }

    @EventHandler
    public void onSkillQuit(PlayerQuitEvent event) {
        loadedSkillData.remove(event.getPlayer().getUniqueId());
        loadingSkillData.remove(event.getPlayer().getUniqueId());
    }

    public void loadSkillData(Player player) {
        UUID uuid=player.getUniqueId();
        loadedSkillData.remove(uuid);
        if(!dataManager.isMySqlEnabled()){loadedSkillData.add(uuid);lastSkillLoad.put(uuid,System.currentTimeMillis());return;}
        if(!loadingSkillData.add(uuid))return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            Map<String,Integer> database=new HashMap<>();
            try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("SELECT skill_id,xp FROM smp_player_skills WHERE player_uuid=?")){
                statement.setString(1,uuid.toString());
                try(ResultSet result=statement.executeQuery()){while(result.next())database.put(result.getString(1).toLowerCase(Locale.ROOT),Math.max(0,result.getInt(2)));}
            }catch(Exception exception){plugin.getLogger().warning("[Skills] Could not load network skills for "+player.getName()+": "+exception.getMessage());}
            Map<String,Integer> loaded=Map.copyOf(database);
            Bukkit.getScheduler().runTask(plugin,()->{
                try{
                    if(!player.isOnline())return;
                    loaded.forEach((skill,xp)->dataManager.getData().set(playerPath(player)+".skills."+skill+".xp",xp));
                    lastDatabaseSkills.put(uuid,loaded);lastSkillLoad.put(uuid,System.currentTimeMillis());loadedSkillData.add(uuid);
                    applyPassiveStats(player);
                    if(plugin instanceof club.abelix.games.fakevival.Game game&&game.getMmoStatService()!=null)game.getMmoStatService().refreshPlayerMMOStats(player);
                    // Seed legacy blob values only from the authoritative overworld.
                    if(loaded.isEmpty()&&plugin instanceof club.abelix.games.fakevival.Game game&&game.getNetworkService().isOverworld())CORE_SKILLS.forEach(skill->saveSkillAsync(uuid,skill,getXp(player,skill)));
                }finally{loadingSkillData.remove(uuid);}
            });
        });
    }

    public List<String> debugSkillData(Player player){
        Map<String,Integer> db=lastDatabaseSkills.getOrDefault(player.getUniqueId(),Map.of());
        String server=plugin instanceof club.abelix.games.fakevival.Game game?game.getNetworkService().getCurrentServerId():"unknown";
        String type=plugin instanceof club.abelix.games.fakevival.Game game?(game.getNetworkService().isHub()?"HUB":game.getNetworkService().isDungeon()?"DUNGEON":game.getNetworkService().isOverworld()?"OVERWORLD":"UNKNOWN"):"unknown";
        return List.of("&7Server ID: &e"+server,"&7Server Type: &e"+type,"&7Skill data loaded: "+(isSkillDataLoaded(player.getUniqueId())?"&atrue":"&cfalse"),"&7DB Mining: &e"+xpToLevel(db.getOrDefault("mining",0)),"&7Cache Mining: &e"+getLevel(player,"mining"),"&7DB Combat: &e"+xpToLevel(db.getOrDefault("combat",0)),"&7Cache Combat: &e"+getLevel(player,"combat"),"&7Last load time: &e"+lastSkillLoad.getOrDefault(player.getUniqueId(),0L),"&7Requirement checks blocked by loading: &e"+!isSkillDataLoaded(player.getUniqueId()));
    }

    private void saveSkillAsync(UUID uuid,String skill,int xp){
        if(plugin instanceof club.abelix.games.fakevival.Game game&&game.getPlayerDataSyncManager().isEnabled()&&!game.getPlayerDataSyncManager().ownsCurrentSessionLock(uuid))return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{try(Connection connection=dataManager.getConnection();PreparedStatement statement=connection.prepareStatement("INSERT INTO smp_player_skills(player_uuid,skill_id,xp,updated_at) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE xp=VALUES(xp),updated_at=VALUES(updated_at)")){statement.setString(1,uuid.toString());statement.setString(2,skill.toLowerCase(Locale.ROOT));statement.setInt(3,Math.max(0,xp));statement.setLong(4,System.currentTimeMillis());statement.executeUpdate();}catch(Exception exception){plugin.getLogger().warning("[Skills] Could not save "+skill+" for "+uuid+": "+exception.getMessage());}});
    }

    public int getXpNeededForLevel(int level) {
        int base = getXpPerLevel();
        int growth = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.xp-per-level-growth", 25));
        return base + Math.max(0, level - 1) * growth;
    }

    public int getMaxLevel(String skill) {
        return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .getInt("skills.definitions." + skill.toLowerCase() + ".max-level",
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.max-level", 100)));
    }

    public boolean skillExists(String skill) {
        if (skill == null || skill.isBlank()) return false;
        String id = skill.trim().toLowerCase(Locale.ROOT);
        return CORE_SKILLS.contains(id) || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin)
                .isConfigurationSection("skills.definitions." + id);
    }

    public void setLevel(Player player, String skill, int level) {
        int target = Math.clamp(level, 1, getMaxLevel(skill));
        long xp = 0;
        for (int current = 1; current < target; current++) xp += getXpNeededForLevel(current);
        dataManager.getData().set(playerPath(player) + ".skills." + skill.toLowerCase() + ".xp", (int) Math.min(Integer.MAX_VALUE, xp));
        dataManager.save();
        saveSkillAsync(player.getUniqueId(),skill,(int)Math.min(Integer.MAX_VALUE,xp));
        applyPassiveStats(player);
    }

    public int getXpIntoLevel(Player player, String skill) {
        return getXpIntoLevel(getXp(player, skill), getLevel(player, skill));
    }

    public void addXp(Player player, String skill, int amount) {
        if(plugin instanceof club.abelix.games.fakevival.Game game&&!game.getNetworkService().isOverworld())return;
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("skills.enabled", true) || skill == null) return;
        skill = skill.toLowerCase();
        int oldLevel = getLevel(player, skill);
        int baseAmount = Math.max(0, amount);
        double eventMultiplier = Math.max(1.0D, eventXpMultiplier.apply(player, skill));
        double foodMultiplier = Math.max(1.0D, consumableXpMultiplier.apply(player));
        double passiveMultiplier = Math.max(1.0D, passiveSkillXpMultiplier.apply(player, skill));
        int finalAmount = (int) Math.floor((baseAmount + baseAmount * getXpBonusMultiplier(player, skill)) * eventMultiplier * foodMultiplier * passiveMultiplier);
        int xp = getXp(player, skill) + finalAmount;
        dataManager.getData().set(playerPath(player) + ".skills." + skill + ".xp", xp);
        dataManager.save();
        saveSkillAsync(player.getUniqueId(),skill,xp);
        if (battlePassManager != null) battlePassManager.addSkillXp(player, finalAmount);
        int newLevel = getLevel(player, skill);
        int needed = getXpNeededForLevel(newLevel);
        bossBarManager.showSkill(player, skill, newLevel, xp, Math.min(needed, getXpIntoLevel(xp, newLevel)), needed);
        if (newLevel > oldLevel) {
            addSkillPoints(player, newLevel - oldLevel);
            applyPassiveStats(player);
            playLevelSound(player);
            player.sendMessage(CC.color(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("messages.skill-level-up", "&aYour %skill% skill is now level %level%!")
                    .replace("%skill%", pretty(skill))
                    .replace("%level%", String.valueOf(newLevel))));
        }
    }

    public int getSkillPoints(Player player) {
        return dataManager.getData().getInt(playerPath(player) + ".skill-points", 0);
    }

    public void addSkillPoints(Player player, int amount) {
        int points = Math.max(0, getSkillPoints(player) + Math.max(0, amount));
        dataManager.getData().set(playerPath(player) + ".skill-points", points);
        dataManager.save();
    }

    public boolean spendSkillPoint(Player player) {
        int points = getSkillPoints(player);
        if (points <= 0) return false;
        dataManager.getData().set(playerPath(player) + ".skill-points", points - 1);
        dataManager.save();
        return true;
    }

    public int getTreeLevel(Player player, String skill, String node) {
        if (skill == null || node == null) return 0;
        return dataManager.getData().getInt(playerPath(player) + ".skill-tree." + skill.toLowerCase() + "." + node.toLowerCase(), 0);
    }

    public int getMiningTreeLevel(Player player, String node) {
        return getTreeLevel(player, "mining", node);
    }

    public boolean upgradeTree(Player player, String skill, String node) {
        if (skill == null || node == null) return false;
        skill = skill.toLowerCase();
        node = node.toLowerCase();
        int max = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.trees." + skill + "." + node + ".max-level", club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("skills.mining-tree." + node + ".max-level", 5));
        int current = getTreeLevel(player, skill, node);
        if (current >= max) return false;
        if (!meetsTreeRequirement(player, skill, node)) return false;
        if (!spendSkillPoint(player)) return false;
        dataManager.getData().set(playerPath(player) + ".skill-tree." + skill + "." + node, current + 1);
        dataManager.save();
        applyPassiveStats(player);
        return true;
    }

    public boolean upgradeMiningTree(Player player, String node) {
        return upgradeTree(player, "mining", node);
    }

    public double getXpBonusMultiplier(Player player, String skill) {
        if (skill == null) return 0.0D;
        skill = skill.toLowerCase();
        org.bukkit.configuration.ConfigurationSection nodes = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("skills.trees." + skill);
        if (nodes == null) return (getTreeLevel(player, skill, "xp_boost") * 0.05D) + getStoreBoosterBonus(player);

        double multiplier = 0.0D;
        for (String node : nodes.getKeys(false)) {
            int level = getTreeLevel(player, skill, node);
            if (level <= 0) continue;
            multiplier += level * Math.max(0.0D, nodes.getDouble(node + ".xp-bonus-per-level", 0.0D));
        }
        return multiplier + getStoreBoosterBonus(player);
    }

    public double getStatBonus(Player player, String stat) {
        double bonus = 0.0D;
        ConfigurationSection trees = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("skills.trees");
        if (trees == null) return 0.0D;
        String key = stat.toLowerCase() + "-per-level";
        for (String skill : trees.getKeys(false)) {
            ConfigurationSection nodes = trees.getConfigurationSection(skill);
            if (nodes == null) continue;
            for (String node : nodes.getKeys(false)) {
                int level = getTreeLevel(player, skill, node);
                if (level <= 0) continue;
                bonus += level * Math.max(0.0D, nodes.getDouble(node + "." + key, 0.0D));
            }
        }
        return bonus;
    }

    public int getBonusMana(Player player) {
        return (int) Math.floor(getTotalStatBonus(player, "mana"));
    }

    public double getBonusHealth(Player player) {
        return getTotalStatBonus(player, "health");
    }

    public double applyOutgoingDamage(Player player, double damage) {
        double bonus = getTotalStatBonus(player, "damage");
        return Math.max(0.0D, damage + bonus);
    }

    public double applyIncomingDamage(Player player, double damage) {
        double defense = getTotalStatBonus(player, "defense");
        double reduction = Math.min(0.70D, defense * 0.01D);
        return Math.max(0.0D, damage * (1.0D - reduction));
    }

    public void applyPassiveStats(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) return;
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            if (!missingMaxHealthAttributeLogged) {
                missingMaxHealthAttributeLogged = true;
                plugin.getLogger().warning("[Skills] Cannot apply Bukkit health because Attribute.MAX_HEALTH is unavailable.");
            }
            return;
        }

        double customMaxHealth = sanitizeCustomMaxHealth(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("skills.base-health", 20.0D) + getBonusHealth(player));
        double oldBukkitMax = sanitizeBukkitMaxHealth(attribute.getValue());
        double bukkitMaxHealth = sanitizeBukkitMaxHealth(customMaxHealth);
        attribute.setBaseValue(bukkitMaxHealth);

        if (customMaxHealth > bukkitMaxHealth && cappedBukkitHealthLogged.add(player.getUniqueId())) {
            plugin.getLogger().info("[Skills] Capped Bukkit health for " + player.getName()
                    + " from " + customMaxHealth
                    + " to " + bukkitMaxHealth
                    + "; custom MMO HP remains " + customMaxHealth);
        }

        double desiredHealth = player.getHealth();
        if (desiredHealth > bukkitMaxHealth) {
            desiredHealth = bukkitMaxHealth;
        } else if (bukkitMaxHealth > oldBukkitMax) {
            desiredHealth = Math.min(bukkitMaxHealth, desiredHealth + (bukkitMaxHealth - oldBukkitMax));
        }
        safeSetBukkitHealth(player, desiredHealth, bukkitMaxHealth);
    }

    public double getTotalStatBonus(Player player, String stat) {
        return getStatBonus(player, stat) + (playerLevelManager == null ? 0.0D : playerLevelManager.getStatBonus(player, stat));
    }

    private double sanitizeCustomMaxHealth(double maxHealth) {
        if (!Double.isFinite(maxHealth) || maxHealth < BUKKIT_MIN_HEALTH) return 20.0D;
        return maxHealth;
    }

    private double sanitizeBukkitMaxHealth(double desiredMaxHealth) {
        if (!Double.isFinite(desiredMaxHealth)) return 20.0D;
        return Math.max(BUKKIT_MIN_HEALTH, Math.min(desiredMaxHealth, BUKKIT_MAX_HEALTH_CAP));
    }

    private void safeSetBukkitHealth(Player player, double desiredHealth, double desiredMaxHealth) {
        if (player == null || !player.isOnline() || player.isDead()) return;
        double safeMax = sanitizeBukkitMaxHealth(desiredMaxHealth);
        double safeHealth = Double.isFinite(desiredHealth) ? Math.clamp(desiredHealth, 0.0D, safeMax) : safeMax;
        try {
            player.setHealth(safeHealth);
        } catch (IllegalArgumentException ex) {
            player.setHealth(Math.min(safeMax, Math.max(BUKKIT_MIN_HEALTH, safeMax)));
        }
    }

    private double getStoreBoosterBonus(Player player) {
        String path = playerPath(player) + ".store.boosters." + "skills";
        long expiresAt = dataManager.getData().getLong(path + ".expires-at", 0L);
        if (expiresAt <= System.currentTimeMillis()) return 0.0D;
        return Math.max(0.0D, dataManager.getData().getDouble(path + ".multiplier", 1.0D) - 1.0D);
    }

    private int getXpIntoLevel(int totalXp, int level) {
        int previous = 0;
        for (int current = 1; current < level; current++) previous += getXpNeededForLevel(current);
        return Math.max(0, totalXp - previous);
    }

    public boolean meetsTreeRequirement(Player player, String skill, String node) {
        if (skill == null || node == null) return false;
        String path = "skills.trees." + skill.toLowerCase() + "." + node.toLowerCase();
        String requiredNode = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(path + ".requires-node", "");
        if (!requiredNode.isBlank()) {
            int requiredLevel = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".requires-level", 1));
            if (getTreeLevel(player, skill, requiredNode) < requiredLevel) return false;
        }
        int requiredSkillLevel = Math.max(0, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(path + ".requires-skill-level", 0));
        return requiredSkillLevel == 0 || getLevel(player, skill) >= requiredSkillLevel;
    }

    private void playLevelSound(Player player) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("sounds.skill-level-up.enabled", true)) return;
        try {
            Sound sound = SoundUtil.parse(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString("sounds.skill-level-up.sound", "ENTITY_PLAYER_LEVELUP"));
            if (sound != null) player.playSound(player.getLocation(), sound, (float) club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("sounds.skill-level-up.volume", 1.0D), (float) club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("sounds.skill-level-up.pitch", 1.0D));
        } catch (Exception ignored) {
        }
    }

    private String pretty(String text) {
        if (text == null || text.isEmpty()) return "None";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}
