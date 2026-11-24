package club.abelix.games.fakevival.registries;

import club.abelix.games.fakevival.config.StaticConfigDefaults;
import club.abelix.games.fakevival.registries.definitions.QuestDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class QuestRegistry extends StaticConfigRegistry {
    private final StaticConfigRegistry npcQuests;
    private Map<String, QuestDefinition> quests = Map.of();
    private Map<String, QuestDefinition> npcQuestDefinitions = Map.of();

    public QuestRegistry(JavaPlugin plugin) {
        super(StaticConfigDefaults.QUESTS);
        npcQuests = new StaticConfigRegistry(StaticConfigDefaults.NPC_QUESTS) {
        };
        reload(plugin);
        npcQuests.reload(plugin);
        rebuildNpcQuests();
    }

    @Override
    protected void rebuild() {
        Map<String, QuestDefinition> nextQuests = new LinkedHashMap<>();
        ConfigurationSection periods = section("adventure-guild.quests");
        if (periods != null) {
            for (String period : periods.getKeys(false)) {
                for (String questId : adventureQuestIds(period)) {
                    ConfigurationSection quest = section("adventure-guild.quests." + period + "." + questId);
                    if (quest == null) continue;
                    String id = period + "." + questId;
                    nextQuests.put(id, new QuestDefinition(
                            id,
                            period,
                            displayName(quest, questId),
                            quest.getBoolean("enabled", true),
                            data(quest)
                    ));
                }
            }
        }
        quests = Map.copyOf(nextQuests);
    }

    public StaticConfigRegistry npcQuests() {
        return npcQuests;
    }

    public Set<String> adventureQuestIds(String period) {
        return keys("adventure-guild.quests." + period);
    }

    public void reloadNpcQuests(JavaPlugin plugin) {
        npcQuests.reload(plugin);
        rebuildNpcQuests();
    }

    public Optional<QuestDefinition> get(String id) {
        QuestDefinition quest = quests.get(id);
        return quest == null ? Optional.ofNullable(npcQuestDefinitions.get(id)) : Optional.of(quest);
    }

    public List<QuestDefinition> all() {
        return List.copyOf(quests.values());
    }

    public List<QuestDefinition> allNpcQuests() {
        return List.copyOf(npcQuestDefinitions.values());
    }

    public boolean contains(String id) {
        return quests.containsKey(id) || npcQuestDefinitions.containsKey(id);
    }

    private void rebuildNpcQuests() {
        Map<String, QuestDefinition> nextNpcQuests = new LinkedHashMap<>();
        ConfigurationSection npcs = npcQuests.section("npc-quests");
        if (npcs != null) {
            for (String npcId : npcs.getKeys(false)) {
                ConfigurationSection questsSection = npcQuests.section("npc-quests." + npcId + ".quests");
                if (questsSection == null) continue;
                for (String questId : questsSection.getKeys(false)) {
                    ConfigurationSection quest = questsSection.getConfigurationSection(questId);
                    if (quest == null) continue;
                    String id = npcId + "." + questId;
                    nextNpcQuests.put(id, new QuestDefinition(
                            id,
                            "npc:" + npcId,
                            npcQuests.displayName(quest, questId),
                            quest.getBoolean("enabled", true),
                            npcQuests.data(quest)
                    ));
                }
            }
        }
        npcQuestDefinitions = Map.copyOf(nextNpcQuests);
    }
}
