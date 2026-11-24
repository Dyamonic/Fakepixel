package me.outerspace.thepit.registry;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TablistRegistry {
    public static final String TAB_PLAYER_NAME = "tab_player_name";
    public static final String NAMETAG = "nametag";
    public static final String CHAT_PLAYER_NAME = "chat_player_name";
    public static final String TAB_PLAYER_FORMAT = "&r%level_formatted% %abelix_prefix%%player% %supporter_star% %bounty_formatted%";
    public static final String NAMETAG_FORMAT = "&r%level_formatted% %abelix_rankcolor_player% %supporter_star% %bounty_formatted%";
    public static final String CHAT_PLAYER_FORMAT = "&r%level_formatted% %abelix_prefix%%player% %supporter_star%";
    private static final List<String> HEADER = List.of("&bYou are playing on &e&lMC.FAKEPIXEL.ME", "");
    private static final List<String> FOOTER = List.of("", "&aRanks, Boosters & MORE! &c&lSTORE.FAKEPIXEL.ME");
    private static final List<String> RANK_PRIORITY = List.of("team", "owner", "mainadmin", "deputy", "developer", "admin", "support", "gm", "mod", "helper", "jrhelper", "special", "s+++", "s++", "s+", "beta-tester", "youtube", "mvp++", "mvp+", "mvp", "vip+", "vip", "default");
    private static final Set<String> PLACEHOLDERS = Set.of("level_formatted", "abelix_prefix", "abelix_rankcolor", "abelix_rank_color", "abelix_rankcolor_player", "player", "supporter_star", "bounty_formatted", "map", "kills", "deaths", "kd", "killstreak", "level", "level_progress", "coins", "pit_gold", "abelix_gold", "prestige", "prestige_icon", "multiplier", "permission_prefix", "group_prefix");
    private static final Pattern PLACEHOLDER = Pattern.compile("%([a-zA-Z0-9_]+)%");

    public boolean enabled() { return true; }
    public boolean nametagsEnabled() { return true; }
    public boolean unlimitedPlayerListName() { return true; }
    public boolean debugEnabled() { return Boolean.getBoolean("thepit.tablist.debug"); }
    public long updateTicks() { return 40L; }
    public List<String> header() { return HEADER; }
    public List<String> footer() { return FOOTER; }
    public String template(String key) { return TAB_PLAYER_NAME.equals(key) ? TAB_PLAYER_FORMAT : NAMETAG.equals(key) ? NAMETAG_FORMAT : CHAT_PLAYER_NAME.equals(key) ? CHAT_PLAYER_FORMAT : ""; }
    public List<String> rankPriority() { return RANK_PRIORITY; }
    public Set<String> unknownPlaceholders(String key) {
        String template = template(key);
        java.util.LinkedHashSet<String> unknown = new java.util.LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) if (!PLACEHOLDERS.contains(matcher.group(1))) unknown.add(matcher.group());
        return java.util.Collections.unmodifiableSet(unknown);
    }
}
