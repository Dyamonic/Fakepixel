package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.ConfigFileManager;
import club.abelix.games.fakevival.data.DataManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;

public final class QuestManager {
    private final JavaPlugin plugin;
    private final DataManager dataManager;
    private final TablistManager placeholders;
    private final ConfigFileManager configFileManager;
    private final CustomItemManager customItemManager;
    private AdventureGuildManager adventureGuildManager;

    public QuestManager(JavaPlugin plugin, DataManager dataManager, TablistManager placeholders, ConfigFileManager configFileManager, CustomItemManager customItemManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.placeholders = placeholders;
        this.configFileManager = configFileManager;
        this.customItemManager = customItemManager;
    }
    public void setAdventureGuildManager(AdventureGuildManager manager){this.adventureGuildManager=manager;}

    public boolean handleNpcQuest(Player player, String npcId) {
        if(!isOverworld())return false;
        FileConfiguration questsConfig = configFileManager.getNpcQuests();
        if (!questsConfig.getBoolean("quests.enabled", true) || npcId == null) return false;
        ConfigurationSection section = questsConfig.getConfigurationSection("quests.npcs." + npcId.toLowerCase());
        if (section == null) return false;
        for (String questId : section.getKeys(false)) {
            if (isNpcQuestCompleted(player, questId)) continue;
            if (handleQuest(player, npcId.toLowerCase(), questId)) return true;
        }
        player.sendMessage(CC.color("&aYou already completed this NPC's quests."));
        return false;
    }

    public List<String> getAdventureQuestIds(String period) {
        String normalizedPeriod = normalizePeriod(period);
        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("adventure-guild.quests." + normalizedPeriod);
        if (section == null) return List.of();

        List<String> quests = new ArrayList<>(section.getKeys(false));
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("adventure-guild.rotation.enabled", true)) return quests;

        int active = Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt("adventure-guild.rotation." + normalizedPeriod + "-active", 3));
        if (active >= quests.size()) return quests;

        long seed = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("adventure-guild.rotation.seed", 24680L);
        Collections.shuffle(quests, new Random(seed + cycleKey(normalizedPeriod).hashCode()));
        return new ArrayList<>(quests.subList(0, active));
    }

    public String getAdventureQuestPeriod(String questId) {
        if (questId == null) return null;
        for (String period : List.of("daily", "weekly")) {
            if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection(adventureQuestConfigPath(period, questId))) return period;
        }
        return null;
    }

    public String getAdventureQuestName(String period, String questId) {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(adventureQuestConfigPath(period, questId) + ".name", pretty(questId));
    }

    public Material getAdventureQuestMaterial(String period, String questId) {
        return material(club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getString(adventureQuestConfigPath(period, questId) + ".required-item.material", "AIR"));
    }

    public int getAdventureQuestRequiredAmount(String period, String questId) {
        return Math.max(1, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getInt(adventureQuestConfigPath(period, questId) + ".required-item.amount", 1));
    }

    public int getAdventureQuestProgress(Player player, String period, String questId) {
        Material material = getAdventureQuestMaterial(period, questId);
        return material == Material.AIR ? getAdventureQuestRequiredAmount(period, questId) : countItem(player, material);
    }

    public boolean isAdventureQuestAccepted(Player player, String period, String questId) {
        return dataManager.getData().getBoolean(adventureQuestDataPath(player, period, questId) + ".accepted", false);
    }

    public boolean isAdventureQuestClaimed(Player player, String period, String questId) {
        return dataManager.getData().getBoolean(adventureQuestDataPath(player, period, questId) + ".claimed", false);
    }

    public void acceptAdventureQuest(Player player, String period, String questId) {
        if(!isOverworld())return;
        if (period == null || questId == null || isAdventureQuestAccepted(player, period, questId)) return;
        if(adventureGuildManager!=null&&!adventureGuildManager.canAccept(player)){player.sendMessage(CC.color("&cYou can only have &e"+adventureGuildManager.activeLimit(player)+" &cactive Adventurer quests at your rank."));return;}
        dataManager.getData().set(adventureQuestDataPath(player, period, questId) + ".accepted", true);
        dataManager.save();
        for (String line : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(adventureQuestConfigPath(period, questId) + ".accept-messages")) {
            player.sendMessage(parse(player, line));
        }
    }

    public boolean canClaimAdventureQuest(Player player, String period, String questId) {
        if (!isAdventureQuestAccepted(player, period, questId) || isAdventureQuestClaimed(player, period, questId)) return false;
        Material material = getAdventureQuestMaterial(period, questId);
        return material == Material.AIR || hasItem(player, material, getAdventureQuestRequiredAmount(period, questId));
    }

    public boolean claimAdventureQuest(Player player, String period, String questId) {
        if(!isOverworld())return false;
        if (!canClaimAdventureQuest(player, period, questId)) return false;
        Material material = getAdventureQuestMaterial(period, questId);
        int amount = getAdventureQuestRequiredAmount(period, questId);
        if (material != Material.AIR) removeItem(player, material, amount);
        dataManager.getData().set(adventureQuestDataPath(player, period, questId) + ".claimed", true);
        if(adventureGuildManager!=null)adventureGuildManager.questCompleted(player,questId.toLowerCase(Locale.ROOT).contains("boss"));
        dataManager.save();
        for (String command : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(adventureQuestConfigPath(period, questId) + ".reward-commands")) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), scaledAdventureReward(player,parse(player, command).replace("%player%", player.getName())));
        }
        for (String line : club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getStringList(adventureQuestConfigPath(period, questId) + ".claim-messages")) {
            player.sendMessage(parse(player, line));
        }
        return true;
    }

    private String scaledAdventureReward(Player player,String command){
        if(adventureGuildManager==null)return command;
        String lower=command.toLowerCase(Locale.ROOT);
        if(lower.contains("gold")||lower.contains("premium"))return command;
        if(!(lower.contains("coin")||lower.contains(" xp")||lower.contains("token")))return command;
        String[] parts=command.split(" ");
        for(int i=parts.length-1;i>=0;i--)try{double value=Double.parseDouble(parts[i]);parts[i]=String.valueOf(Math.max(1,Math.round(value*adventureGuildManager.rewardMultiplier(player))));break;}catch(NumberFormatException ignored){}
        return String.join(" ",parts);
    }

    private boolean isOverworld(){return !(plugin instanceof club.abelix.games.fakevival.Game game)||game.getNetworkService().isOverworld();}

    private boolean handleQuest(Player player, String npcId, String questId) {
        FileConfiguration questsConfig = configFileManager.getNpcQuests();
        String path = "quests.npcs." + npcId + "." + questId;
        String dataPath = "players." + player.getUniqueId() + ".quests." + questId;
        if (dataManager.getData().getBoolean(dataPath + ".completed", false)) {
            player.sendMessage(parse(player, questsConfig.getString(path + ".completed-message", "&aYou already completed this quest.")));
            return true;
        }
        if (!dataManager.getData().getBoolean(dataPath + ".started", false)) {
            dataManager.getData().set(dataPath + ".started", true);
            dataManager.save();
            for (String line : questsConfig.getStringList(path + ".start-messages")) player.sendMessage(parse(player, line));
            return true;
        }
        Material material = material(questsConfig.getString(path + ".required-item.material", "AIR"));
        String customItem = normalizeCustomItem(questsConfig.getString(path + ".required-item.custom-item", ""));
        int amount = Math.max(1, questsConfig.getInt(path + ".required-item.amount", 1));
        if (!customItem.isBlank() && countCustomItem(player, customItem) < amount) {
            player.sendMessage(parse(player, questsConfig.getString(path + ".need-item-message", "&cYou need &e%amount%x %item% &cto complete this quest.")
                    .replace("%amount%", String.valueOf(amount)).replace("%item%", pretty(customItem))));
            return true;
        }
        if (customItem.isBlank() && material != Material.AIR && !hasItem(player, material, amount)) {
            player.sendMessage(parse(player, questsConfig.getString(path + ".need-item-message", "&cYou need &e%amount%x %item% &cto complete this quest.")
                    .replace("%amount%", String.valueOf(amount)).replace("%item%", material.name())));
            return true;
        }
        if (!customItem.isBlank()) removeCustomItem(player, customItem, amount);
        else if (material != Material.AIR) removeItem(player, material, amount);
        dataManager.getData().set(dataPath + ".completed", true);
        dataManager.save();
        for (String command : questsConfig.getStringList(path + ".reward-commands")) {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), parse(player, command).replace("%player%", player.getName()));
        }
        for (String line : questsConfig.getStringList(path + ".complete-messages")) player.sendMessage(parse(player, line));
        return true;
    }

    private String parse(Player player, String line) {
        return placeholders.parse(player, CC.color(line == null ? "" : line));
    }

    private Material material(String name) {
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            return Material.AIR;
        }
    }

    private boolean hasItem(Player player, Material material, int amount) {
        return countItem(player, material) >= amount;
    }

    private void removeItem(Player player, Material material, int amount) {
        int left = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            int take = Math.min(left, item.getAmount());
            item.setAmount(item.getAmount() - take);
            left -= take;
            if (left <= 0) break;
        }
        player.updateInventory();
    }

    private int countItem(Player player, Material material) {
        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) found += item.getAmount();
        }
        return found;
    }

    private int countCustomItem(Player player, String customItem) {
        int found = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (customItem.equals(normalizeCustomItem(customItemManager.getItemId(item)))) found += Objects.requireNonNull(item).getAmount();
        }
        return found;
    }

    private void removeCustomItem(Player player, String customItem, int amount) {
        int left = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !customItem.equals(normalizeCustomItem(customItemManager.getItemId(item)))) continue;
            int take = Math.min(left, item.getAmount());
            item.setAmount(item.getAmount() - take);
            left -= take;
            if (left <= 0) break;
        }
        player.updateInventory();
    }

    private String normalizeCustomItem(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_").trim();
    }

    private boolean isNpcQuestCompleted(Player player, String questId) {
        return dataManager.getData().getBoolean("players." + player.getUniqueId() + ".quests." + questId + ".completed", false);
    }

    private String adventureQuestConfigPath(String period, String questId) {
        return "adventure-guild.quests." + normalizePeriod(period) + "." + questId.toLowerCase(Locale.ROOT);
    }

    private String adventureQuestDataPath(Player player, String period, String questId) {
        return "players." + player.getUniqueId() + ".adventure-guild." + normalizePeriod(period) + "." + cycleKey(period) + "." + questId.toLowerCase(Locale.ROOT);
    }

    private String cycleKey(String period) {
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        if ("weekly".equalsIgnoreCase(period)) {
            WeekFields weekFields = WeekFields.ISO;
            return now.get(weekFields.weekBasedYear()) + "-W" + now.get(weekFields.weekOfWeekBasedYear());
        }
        return now.toString();
    }

    private String normalizePeriod(String period) {
        return "weekly".equalsIgnoreCase(period) ? "weekly" : "daily";
    }

    private String pretty(String text) {
        if (text == null || text.isBlank()) return "Quest";
        text = text.toLowerCase(Locale.ROOT).replace('_', ' ');
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
