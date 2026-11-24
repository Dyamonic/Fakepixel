package me.outerspace.thepit.registry;

import java.util.List;
import java.util.Map;

public final class ScoreboardRegistry extends RegistryNode {
    public ScoreboardRegistry(Map<String, Object> values) { super(values); }
    public boolean enabled() { return getBoolean("scoreboard.enabled", true); }
    public int updateTicks() { return getInt("scoreboard.update-ticks", 20); }
    public String title() { return getString("scoreboard.title", "&b&lThe Pit"); }
    public List<String> lines() { return getStringList("scoreboard.lines"); }
}
