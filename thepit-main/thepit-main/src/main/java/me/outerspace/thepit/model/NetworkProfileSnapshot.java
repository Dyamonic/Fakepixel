package me.outerspace.thepit.model;

public final class NetworkProfileSnapshot {
    private final String prefix;
    private final String permissionPrefix;
    private final String rankColor;
    private final String group;
    private final int priority;
    private final int gold;

    public NetworkProfileSnapshot(String prefix, String permissionPrefix, String rankColor, String group, int priority, int gold) {
        this.prefix = prefix == null ? "" : prefix;
        this.permissionPrefix = permissionPrefix == null ? "" : permissionPrefix;
        this.rankColor = rankColor == null ? "" : rankColor;
        this.group = group == null || group.isEmpty() ? "default" : group;
        this.priority = priority;
        this.gold = gold;
    }

    public String getPrefix() { return prefix; }
    public String getPermissionPrefix() { return permissionPrefix; }
    public String getRankColor() { return rankColor; }
    public String getGroup() { return group; }
    public int getPriority() { return priority; }
    public int getGold() { return gold; }
}
