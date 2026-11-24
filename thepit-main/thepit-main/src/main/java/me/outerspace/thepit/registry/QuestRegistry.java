package me.outerspace.thepit.registry;

import me.outerspace.thepit.registry.definition.QuestDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class QuestRegistry extends RegistryNode {
    private final Map<String, QuestDefinition> quests;

    public QuestRegistry(Map<String, Object> values) {
        super(values);
        Map<String, QuestDefinition> built = new LinkedHashMap<>();
        load(built, "daily");
        load(built, "weekly");
        quests = Collections.unmodifiableMap(built);
    }

    private void load(Map<String, QuestDefinition> target, String period) {
        for (String id : keys("quests." + period)) {
            String base = "quests." + period + "." + id;
            target.put(period + "." + id, new QuestDefinition(period, id, getString(base + ".display-name", id),
                    getString(base + ".type", id), getInt(base + ".goal", 0), getInt(base + ".reward-coins", 0),
                    getString(base + ".material", "PAPER")));
        }
    }

    public Optional<QuestDefinition> getQuest(String period, String id) { return Optional.ofNullable(quests.get(period + "." + id)); }
    public List<QuestDefinition> getQuests(String period) {
        List<QuestDefinition> out = new ArrayList<>();
        for (QuestDefinition quest : quests.values()) if (quest.period().equals(period)) out.add(quest);
        return Collections.unmodifiableList(out);
    }
}
