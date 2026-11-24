package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.registries.PlayerLevelRequirementRegistry;
import club.abelix.games.fakevival.registries.PlayerLevelRequirementRegistry.PlayerLevelStatReward;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerLevelManager {
    public static final int DEFAULT_MAX_LEVEL = 10000;
    private static final Map<Integer, String> LEVEL_DISPLAY_CACHE = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final SkillManager skillManager;
    private final CustomItemManager customItemManager;
    private final PlayerLevelRequirementRegistry requirementRegistry = new PlayerLevelRequirementRegistry();
    private final Set<String> missingRequirementWarnings = new HashSet<>();
    private final Set<Integer> missingTierWarnings = new HashSet<>();
    private final Map<UUID, Boolean> cachedNextRequirementsMet = new ConcurrentHashMap<>();

    public PlayerLevelManager(JavaPlugin plugin, DataManager dataManager, SkillManager skillManager, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.skillManager = skillManager;
        this.customItemManager = customItemManager;
    }

    public int getLevel(Player player) {
        return Math.clamp(dataManager.getData().getInt(path(player) + ".level", 1), 1, getMaxLevel());
    }

    public long getXp(Player player) { return Math.max(0L, dataManager.getData().getLong(path(player) + ".xp", 0L)); }

    public int getMaxLevel() {
        // PlayerLevel progression is intentionally network-stable at 10,000. The runtime key is
        // still seeded/exposed for diagnostics, but cannot lower or exceed the supported cap.
        return DEFAULT_MAX_LEVEL;
    }

    public void setLevel(Player player, int level) {
        dataManager.getData().set(path(player) + ".level", Math.clamp(level, 1, getMaxLevel()));
        dataManager.save();
        skillManager.applyPassiveStats(player);
        refreshLevelConsumers(player);
    }

    public void addXp(Player player, long amount) {
        int level = getLevel(player);
        long xp = Math.max(0L, safeAdd(getXp(player), amount));
        int maxLevel = getMaxLevel();
        if (level >= maxLevel) {
            dataManager.getData().set(path(player) + ".xp", xp);
            dataManager.save();
            return;
        }
        int oldLevel = level;
        while (level < maxLevel) {
            long needed = getXpNeeded(level);
            if (xp < needed) break;
            int nextLevel = level + 1;
            if (!requirementsMet(player, nextLevel, false)) {
                player.sendMessage(CC.color("&cYou have enough XP, but you are missing requirements for Level " + nextLevel + "."));
                player.sendMessage(CC.color("&eUse &a/smp levels &eto view what you need."));
                break;
            }
            xp -= needed;
            takeRequiredItems(player, nextLevel);
            level++;
        }
        dataManager.getData().set(path(player) + ".xp", xp);
        if (level != oldLevel) dataManager.getData().set(path(player) + ".level", level);
        dataManager.save();
        skillManager.applyPassiveStats(player);
        if (level != oldLevel) announceLevelUps(player, oldLevel, level);
        if (level != oldLevel) refreshLevelConsumers(player);
    }

    public long getXpNeeded(Player player) { return getXpNeeded(getLevel(player)); }

    public long getXpNeeded(int level) {
        if (level >= getMaxLevel()) return 0L;
        if (level <= 100) return Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("player-levels.xp-per-level", 1000L));
        long value;
        if (level <= 1000) {
            value = 1000L + (long) level * 250L + (long) level * level * 8L;
        } else {
            long over = level - 1000L;
            value = 1000L + (long) level * 350L + (long) level * level * 10L + over * over * 25L;
        }
        return Math.max(1L, value);
    }

    public double getProgress(Player player) {
        long needed = getXpNeeded(player);
        return needed <= 0L ? 100.0D : Math.clamp(getXp(player) * 100.0D / needed, 0.0D, 100.0D);
    }

    public String getDisplay(Player player) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("player-levels.enabled", true)) return "";
        return getLevelDisplay(getLevel(player)) + " ";
    }

    public String getLevelDisplay(int rawLevel) {
        int level = Math.max(1, rawLevel);
        return LEVEL_DISPLAY_CACHE.computeIfAbsent(level, value -> value >= 1000
                ? getSpecialLevelDisplay(value)
                : getColor(value) + "[" + getIcon(value) + value + "]");
    }

    public String getIcon(int rawLevel) {
        int level = Math.max(1, rawLevel);
        if (level >= 10000) return "✫";
        if (level >= 9000) return "✧";
        if (level >= 8000) return "✺";
        if (level >= 7000) return "❂";
        if (level >= 6000) return "✦";
        if (level >= 5000) return "✸";
        if (level >= 4000) return "✷";
        if (level >= 3000) return "✹";
        if (level >= 2000) return "◆";
        if (level >= 1000) return "✪";
        if (level >= 900) return "✷";
        if (level >= 800) return "✹";
        if (level >= 700) return "◆";
        if (level >= 600) return "✦";
        if (level >= 500) return "✸";
        if (level >= 400) return "✷";
        if (level >= 300) return "✹";
        if (level >= 200) return "◆";
        return "✦";
    }

    public String getSpecialLevelDisplay(int rawLevel) {
        int level = Math.max(1000, rawLevel);
        String icon = getIcon(level);
        return switch (Math.min(10, level / 1000)) {
            case 1 -> "&c[&6&l" + icon + "&r&e" + level + "&c]";
            case 2 -> "&d[&c&l" + icon + "&r&6" + level + "&d]";
            case 3 -> "&b[&d&l" + icon + "&r&c" + level + "&b]";
            case 4 -> "&e[&b&l" + icon + "&r&d" + level + "&e]";
            case 5 -> "&6[&e&l" + icon + "&r&a" + level + "&6]";
            case 6 -> "&d[&a&l" + icon + "&r&b" + level + "&d]";
            case 7 -> "&5[&d&l" + icon + "&r&6" + level + "&5]";
            case 8 -> "&b[&5&l" + icon + "&r&e" + level + "&b]";
            case 9 -> "&c[&b&l" + icon + "&r&d" + level + "&c]";
            default -> "&d&l[&r&c&l" + icon + "&r&6&l" + icon + "&r&e" + level + "&d&l]";
        };
    }

    public boolean upgrade(Player player) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("player-levels.enabled", true)) {
            player.sendMessage(CC.color("&cPlayer levels are disabled."));
            return false;
        }
        int nextLevel = getLevel(player) + 1;
        if (nextLevel > getMaxLevel()) {
            player.sendMessage(CC.color("&eYou are at the maximum player level."));
            return false;
        }
        String levelPath = "player-levels.levels." + nextLevel;
        long needed = getXpNeeded(player);
        if (getXp(player) < needed) {
            player.sendMessage(CC.color("&cYou need &f" + needed + " player XP &cto reach Level " + nextLevel + "."));
            return false;
        }
        if (!requirementsMet(player, nextLevel, true)) {
            player.sendMessage(CC.color("&cYou do not meet the requirements for Level " + nextLevel + "."));
            return false;
        }
        takeRequiredItems(player, nextLevel);
        dataManager.getData().set(path(player) + ".xp", getXp(player) - needed);
        dataManager.getData().set(path(player) + ".level", nextLevel);
        dataManager.save();
        skillManager.applyPassiveStats(player);
        refreshLevelConsumers(player);
        announceLevelUps(player, nextLevel - 1, nextLevel);
        return true;
    }

    public Map<String, Integer> skillRequirements(int level) {
        if (requirementRegistry.getTierForLevel(level) == null) { warnMissingTier(level); return Map.of(); }
        Map<String,Integer> clamped = new LinkedHashMap<>();
        Objects.requireNonNull(requirementRegistry.requirement(level, getXpNeeded(Math.max(1, level - 1)))).requiredSkills()
                .forEach((skill, required) -> clamped.put(skill, Math.min(required, skillManager.getMaxLevel(skill))));
        return Map.copyOf(clamped);
    }

    public Map<String, Integer> itemRequirements(int level) {
        Map<String, Integer> requirements = new LinkedHashMap<>();
        if (requirementRegistry.getTierForLevel(level) == null) { warnMissingTier(level); return requirements; }
        for (var item : Objects.requireNonNull(requirementRegistry.requirement(level, getXpNeeded(Math.max(1, level - 1)))).requiredItems()) {
            requirements.put(item.customItemId(), item.amount());
            if (!customItemManager.itemExists(item.customItemId()) && missingRequirementWarnings.add(item.customItemId()))
                plugin.getLogger().warning("[SMP] Missing player level requirement item: " + item.customItemId());
        }
        return requirements;
    }

    public boolean isRequirementItemRegistered(String id) { return customItemManager.itemExists(id); }
    public String requirementItemDisplayName(String id) {
        if (!customItemManager.itemExists(id)) return pretty(id);
        ItemStack sample = customItemManager.createItem(id, 1);
        if (sample != null && sample.hasItemMeta() && sample.getItemMeta().hasDisplayName()) {
            String name = org.bukkit.ChatColor.stripColor(sample.getItemMeta().getDisplayName());
            if (name != null && !name.isBlank()) return name;
        }
        return pretty(id);
    }
    public String requirementTierDisplay(int level) { var tier=requirementRegistry.getTierForLevel(level);return tier==null?"missing":tier.display(); }

    private void warnMissingTier(int level) { if(missingTierWarnings.add(level))plugin.getLogger().warning("[SMP] Missing player level requirement tier for level " + level); }

    public void validateOnStartup() {
        int[] samples={2,100,200,500,1000,2000,10000};for(int level:samples)if(requirementRegistry.getTierForLevel(level)==null)warnMissingTier(level);
        for(int level:new int[]{100,200,300,400,500,600,700,800,900,1000,2000,3000,4000,5000,6000,7000,8000,9000,10000})itemRequirements(level);
        plugin.getLogger().info("[SMP] Loaded " + requirementRegistry.tierCount() + " player level requirement tiers.");
    }

    public List<String> debugRequirementLines(int level) {
        var requirement=requirementRegistry.requirement(Math.max(1,level),getXpNeeded(Math.max(1,level-1)));if(requirement==null)return List.of("&cRequirement data missing for Level "+level+".");
        String skills=requirement.requiredSkills().entrySet().stream().map(e->pretty(e.getKey())+" "+Math.min(e.getValue(),skillManager.getMaxLevel(e.getKey()))).collect(java.util.stream.Collectors.joining(", "));
        String items=requirement.requiredItems().stream().map(i->i.customItemId()+" x"+i.amount()).collect(java.util.stream.Collectors.joining(", "));
        var reward=requirement.statReward();return List.of("&eLevel: &f"+level,"&eTier: &f"+requirementTierDisplay(level),"&eRequired Skills: &f"+(skills.isBlank()?"None":skills),"&eRequired Items: &f"+(items.isBlank()?"None":items),"&eStat Rewards: &f"+formatRewardDebug(reward));
    }

    private String formatRewardDebug(PlayerLevelStatReward r){List<String> p=new java.util.ArrayList<>();if(r.health()>0)p.add("+"+r.health()+" Health");if(r.defense()>0)p.add("+"+r.defense()+" Defense");if(r.mana()>0)p.add("+"+r.mana()+" Mana");if(r.strength()>0)p.add("+"+r.strength()+" Strength");if(r.critChance()>0)p.add("+"+r.critChance()+"% Crit Chance");if(r.critDamage()>0)p.add("+"+r.critDamage()+"% Crit Damage");return String.join(", ",p);}

    public int getSkillLevel(Player player, String skill) {
        return skillManager.getLevel(player, skill);
    }

    public int getCustomItemCount(Player player, String customItemId) {
        return countCustomItems(player, customItemId);
    }

    public double getStatBonus(Player player, String stat) {
        if (stat != null && stat.equalsIgnoreCase("health")) return getLevelHealthBonus(getLevel(player));
        if (stat != null && stat.equalsIgnoreCase("defense")) return getLevelDefenseBonus(getLevel(player));
        if (stat != null && stat.equalsIgnoreCase("mana")) return getLevelManaBonus(getLevel(player));
        return rewardValue(requirementRegistry.totalReward(getLevel(player)), stat);
    }

    public double getStatBonusForLevel(int level, String stat) {
        if (stat != null && stat.equalsIgnoreCase("health"))
            return Math.max(0.0D, getLevelHealthTotal(level) - getLevelHealthTotal(Math.max(0, level - 1)));
        if (stat != null && stat.equalsIgnoreCase("defense"))
            return Math.max(0.0D, getLevelDefenseBonus(level) - getLevelDefenseBonus(Math.max(0, level - 1)));
        if (stat != null && stat.equalsIgnoreCase("mana"))
            return Math.max(0.0D, getLevelManaBonus(level) - getLevelManaBonus(Math.max(0, level - 1)));
        return rewardValue(Objects.requireNonNull(requirementRegistry.requirement(level, getXpNeeded(Math.max(1, level - 1)))).statReward(), stat);
    }

    public double getLevelHealthTotal(int rawLevel) {
        return baseHp() + getLevelHealthBonus(rawLevel);
    }

    public double getLevelHealthBonus(int rawLevel) {
        if (!levelStatsEnabled()) return 0.0D;
        int level = statLevel(rawLevel);
        var settings = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        double value = level * positive(settings.getDouble("playerlevel.hpPerLevel", 0.5D))
                + (level / 10) * positive(settings.getDouble("playerlevel.hpBonusEvery10", 2.0D))
                + (level / 100) * positive(settings.getDouble("playerlevel.hpBonusEvery100", 15.0D))
                + (level / 1000) * positive(settings.getDouble("playerlevel.hpBonusEvery1000", 100.0D));
        return Math.min(positive(settings.getDouble("playerlevel.maxHpFromLevel", 8500.0D)), value);
    }

    public double getLevelDefenseBonus(int rawLevel) {
        if (!levelStatsEnabled()) return 0.0D;
        int level = statLevel(rawLevel);
        var settings = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        double value = level * positive(settings.getDouble("playerlevel.defensePerLevel", 0.03D))
                + (level / 10) * positive(settings.getDouble("playerlevel.defenseBonusEvery10", 0.2D))
                + (level / 100) * positive(settings.getDouble("playerlevel.defenseBonusEvery100", 1.5D))
                + (level / 1000) * positive(settings.getDouble("playerlevel.defenseBonusEvery1000", 10.0D));
        return Math.min(positive(settings.getDouble("playerlevel.maxDefenseFromLevel", 900.0D)), value);
    }

    public double getLevelManaBonus(int rawLevel) {
        if (!levelStatsEnabled()) return 0.0D;
        int level = statLevel(rawLevel);
        var settings = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        double value = level * positive(settings.getDouble("playerlevel.manaPerLevel", 0.2D))
                + (level / 10) * positive(settings.getDouble("playerlevel.manaBonusEvery10", 1.0D))
                + (level / 100) * positive(settings.getDouble("playerlevel.manaBonusEvery100", 10.0D))
                + (level / 1000) * positive(settings.getDouble("playerlevel.manaBonusEvery1000", 75.0D));
        return Math.min(positive(settings.getDouble("playerlevel.maxManaFromLevel", 2500.0D)), value);
    }

    public LevelScalingValidation validateLevelScaling() {
        var settings = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin);
        double hpCap = positive(settings.getDouble("playerlevel.maxHpFromLevel", 8500.0D));
        double defenseCap = positive(settings.getDouble("playerlevel.maxDefenseFromLevel", 900.0D));
        double manaCap = positive(settings.getDouble("playerlevel.maxManaFromLevel", 2500.0D));
        double maxHp = getLevelHealthBonus(getMaxLevel());
        double maxDefense = getLevelDefenseBonus(getMaxLevel());
        double maxMana = getLevelManaBonus(getMaxLevel());
        return new LevelScalingValidation(getMaxLevel(), levelStatsEnabled(), maxHp, hpCap, maxDefense, defenseCap,
                maxMana, manaCap, maxHp <= hpCap + 0.001D && maxDefense <= defenseCap + 0.001D && maxMana <= manaCap + 0.001D);
    }

    private boolean levelStatsEnabled() { return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("playerlevel.stats.enabled", true); }
    private int statLevel(int rawLevel) { return Math.clamp(rawLevel, 0, DEFAULT_MAX_LEVEL); }
    private double positive(double value) { return Math.max(0.0D, value); }
    private double baseHp() { return Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("playerlevel.baseHp", 100.0D)); }
    private double baseMana() { return Math.max(1.0D, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("playerlevel.baseMana", 100.0D)); }

    public boolean requirementsMet(Player player, int level, boolean message) {
        boolean met = meetsSkillRequirements(player, level, message) && hasRequiredItems(player, level, message);
        if (level == getLevel(player) + 1) cachedNextRequirementsMet.put(player.getUniqueId(), met);
        return met;
    }

    public boolean cachedNextRequirementsMet(Player player) { return cachedNextRequirementsMet.getOrDefault(player.getUniqueId(), false); }

    public String nextRequirementsPlaceholder(Player player) {
        int next = Math.min(getMaxLevel(), getLevel(player) + 1);
        return skillRequirements(next).size() + " skills, " + itemRequirements(next).size() + " items";
    }

    public String nextRequirementsSummary(Player player) {
        int next = Math.min(getMaxLevel(), getLevel(player) + 1);
        int missing = 0;
        for (var entry : skillRequirements(next).entrySet()) if (getSkillLevel(player, entry.getKey()) < entry.getValue()) missing++;
        for (var entry : itemRequirements(next).entrySet()) if (getCustomItemCount(player, entry.getKey()) < entry.getValue()) missing++;
        return missing == 0 ? "Ready" : missing + " missing";
    }

    public String nextStatsSummary(Player player) {
        int next = Math.min(getMaxLevel(), getLevel(player) + 1);
        return "+" + formatStat(getStatBonusForLevel(next, "health")) + " Health, +"
                + formatStat(getStatBonusForLevel(next, "defense")) + " Defense, +"
                + formatStat(getStatBonusForLevel(next, "mana")) + " Mana";
    }

    public void giveNextRequiredItems(Player player) {
        int next = Math.min(getMaxLevel(), getLevel(player) + 1);
        for (var entry : itemRequirements(next).entrySet()) {
            int missing = Math.max(0, entry.getValue() - getCustomItemCount(player, entry.getKey()));
            while (missing > 0) { int amount = Math.min(64, missing); player.getInventory().addItem(customItemManager.createItem(entry.getKey(), amount)); missing -= amount; }
        }
    }

    public void setNextRequiredSkills(Player player) {
        int next = Math.min(getMaxLevel(), getLevel(player) + 1);
        skillRequirements(next).forEach((skill, required) -> skillManager.setLevel(player, skill, required));
    }

    public void forceLevelUp(Player player) { if (getLevel(player) < getMaxLevel()) setLevel(player, getLevel(player) + 1); }

    public String validateRequirementRegistry() {
        int[] samples = {10,100,200,300,400,500,600,700,800,900,1000,2000,3000,4000,5000,6000,7000,8000,9000,10000};
        int entries=0; for(int level:samples) entries+=skillRequirements(level).size()+itemRequirements(level).size();
        return "Validated " + samples.length + " requirement tiers with " + entries + " entries.";
    }

    public void recalculateLevelStatRewards(Player player) { refreshLevelConsumers(player); }

    private double rewardValue(PlayerLevelStatReward reward, String stat) {
        return switch (stat.toLowerCase(Locale.ROOT)) {
            case "health" -> reward.health(); case "defense" -> reward.defense(); case "mana" -> reward.mana();
            case "strength" -> reward.strength(); case "damage" -> reward.damage();
            case "crit_chance" -> reward.critChance(); case "crit_damage" -> reward.critDamage(); default -> 0.0D;
        };
    }

    public String getColor(int level) {
        if (level >= 10000) return "&d";
        if (level >= 9000) return "&c";
        if (level >= 8000) return "&b";
        if (level >= 7000) return "&5";
        if (level >= 6000) return "&d";
        if (level >= 5000) return "&6";
        if (level >= 4000) return "&e";
        if (level >= 3000) return "&b";
        if (level >= 2000) return "&d";
        if (level >= 1000) return "&c";
        if (level >= 900) return "&4";
        if (level >= 800) return "&c";
        if (level >= 700) return "&5";
        if (level >= 600) return "&d";
        if (level >= 500) return "&6";
        if (level >= 400) return "&e";
        if (level >= 300) return "&3";
        if (level >= 200) return "&b";
        if (level >= 100) return "&a";
        return "&7";
    }

    private long safeAdd(long value, long amount) {
        if (amount > 0L && value > Long.MAX_VALUE - amount) return Long.MAX_VALUE;
        if (amount < 0L && amount < -value) return 0L;
        return value + amount;
    }

    private void announceLevelUps(Player player, int oldLevel, int newLevel) {
        double hpGain = Math.max(0.0D, getLevelHealthBonus(newLevel) - getLevelHealthBonus(oldLevel));
        double defenseGain = Math.max(0.0D, getLevelDefenseBonus(newLevel) - getLevelDefenseBonus(oldLevel));
        double manaGain = Math.max(0.0D, getLevelManaBonus(newLevel) - getLevelManaBonus(oldLevel));
        player.sendMessage(CC.color("&a&lLEVEL UP!"));
        player.sendMessage(CC.color("&7Player Level: &e" + oldLevel + " &7➜ &a" + newLevel));
        player.sendMessage("");
        player.sendMessage(CC.color("&6Rewards:"));
        if (hpGain > 0.0D) player.sendMessage(CC.color("&c+" + formatStat(hpGain) + "❤ Health"));
        if (defenseGain > 0.0D) player.sendMessage(CC.color("&a+" + formatStat(defenseGain) + "❈ Defense"));
        if (manaGain > 0.0D) player.sendMessage(CC.color("&b+" + formatStat(manaGain) + "✎ Mana"));
        boolean hundredMilestone = newLevel / 100 > oldLevel / 100;
        boolean prestigeMilestone = newLevel / 1000 > oldLevel / 1000;
        if (oldLevel < 1000 && newLevel >= 1000) {
            player.sendMessage(CC.color("&c&lASCENDED! &6You reached &eLevel 1000&6!"));
            player.sendMessage(CC.color("&dYou unlocked rainbow level styles."));
        } else if (prestigeMilestone) {
            player.sendMessage(CC.color("&d&lPRESTIGE MILESTONE! &7You reached " + getLevelDisplay(newLevel) + "&7!"));
            player.sendMessage(CC.color("&eYou unlocked a new special level icon."));
        } else if (hundredMilestone) {
            player.sendMessage(CC.color("&6&lMILESTONE! &7You reached " + getLevelDisplay(newLevel) + "&7!"));
        }
        boolean milestone = hundredMilestone || prestigeMilestone;
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, milestone ? 1.0F : 0.7F, milestone ? 1.15F : 1.35F);
    }

    private void refreshLevelConsumers(Player player) {
        cachedNextRequirementsMet.remove(player.getUniqueId());
        if (plugin instanceof club.abelix.games.fakevival.Game game && game.getMmoStatService() != null) {
            game.getMmoStatService().refreshPlayerMMOStats(player);
            game.getBoardManager().update(player);
            game.getTablistManager().apply(player);
        }
    }

    public PlayerStats getStats(Player player) {
        double hp = baseHp() + skillManager.getStatBonus(player, "health") + getStatBonus(player, "health");
        double mana = baseMana() + skillManager.getStatBonus(player, "mana") + getStatBonus(player, "mana");
        double damage = skillManager.getStatBonus(player, "damage") + getStatBonus(player, "damage");
        double configuredDefense = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("playerlevel.baseDefense",
                club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("mmo.base-defense",
                        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getDouble("skills.base-mmo-defense", 0.0D)));
        double baseDefense = Math.max(0.0D, configuredDefense);
        double defense = baseDefense + skillManager.getStatBonus(player, "defense") + getStatBonus(player, "defense");
        double agility = skillManager.getStatBonus(player, "agility") + getStatBonus(player, "agility");
        return new PlayerStats(hp, mana, damage, defense, agility);
    }

    private boolean meetsSkillRequirements(Player player, int level, boolean message) {
        for (Map.Entry<String, Integer> entry : skillRequirements(level).entrySet()) {
            int current = skillManager.getLevel(player, entry.getKey());
            if (current >= entry.getValue()) continue;
            if (message) player.sendMessage(CC.color("&cRequires &f" + pretty(entry.getKey()) + " " + entry.getValue() + "&c. You have &f" + current + "&c."));
            return false;
        }
        return true;
    }

    private boolean hasRequiredItems(Player player, int level, boolean message) {
        for (Map.Entry<String, Integer> entry : itemRequirements(level).entrySet()) {
            int owned = countCustomItems(player, entry.getKey());
            if (owned >= entry.getValue()) continue;
            if (message) player.sendMessage(CC.color("&cRequires &f" + entry.getValue() + "x " + requirementItemDisplayName(entry.getKey()) + "&c. You have &f" + owned + "&c."));
            return false;
        }
        return true;
    }

    private void takeRequiredItems(Player player, String levelPath) {
        for (Map.Entry<String, Integer> entry : itemRequirements(levelNumber(levelPath)).entrySet()) {
            removeCustomItems(player, entry.getKey(), entry.getValue());
        }
    }

    private void takeRequiredItems(Player player, int level) {
        for (Map.Entry<String, Integer> entry : itemRequirements(level).entrySet()) removeCustomItems(player, entry.getKey(), entry.getValue());
    }

    private int countCustomItems(Player player, String customItemId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && customItemId.equalsIgnoreCase(String.valueOf(customItemManager.getItemId(item)))) count += item.getAmount();
        }
        return count;
    }

    private void removeCustomItems(Player player, String customItemId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || !customItemId.equalsIgnoreCase(String.valueOf(customItemManager.getItemId(item)))) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            contents[slot] = item.getAmount() <= 0 ? null : item;
            remaining -= take;
            if (remaining <= 0) break;
        }
        player.getInventory().setStorageContents(contents);
    }

    private String path(Player player) {
        return "players." + player.getUniqueId() + ".player-level";
    }

    private int levelNumber(String levelPath) {
        String value = levelPath.substring(levelPath.lastIndexOf('.') + 1);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "None";
        text = text.toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private String formatStat(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001D) return String.valueOf((long) Math.rint(value));
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public record PlayerStats(double hp, double mana, double damage, double defense, double agility) {
    }

    public record LevelScalingValidation(int maxLevel, boolean rewardsEnabled,
                                         double maximumLevelHealth, double healthCap,
                                         double maximumLevelDefense, double defenseCap,
                                         double maximumLevelMana, double manaCap, boolean withinCaps) {}
}
