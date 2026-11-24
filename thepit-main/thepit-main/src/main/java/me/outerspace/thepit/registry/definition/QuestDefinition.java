package me.outerspace.thepit.registry.definition;

public record QuestDefinition(String period, String id, String displayName, String type, int goal,
                              int rewardCoins, String material) {
    public String getPeriod() { return period; }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getType() { return type; }
    public int getGoal() { return goal; }
    public int getRewardCoins() { return rewardCoins; }
    public String getMaterial() { return material; }
}
