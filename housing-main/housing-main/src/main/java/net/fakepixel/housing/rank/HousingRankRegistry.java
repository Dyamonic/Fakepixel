package net.fakepixel.housing.rank;

import java.util.*;

public final class HousingRankRegistry {
    private static final Map<String, HousingRankDefinition> VALUES = new LinkedHashMap<>();
    static {
        admin("ADMINISTRATOR", false);
        for (String id : Arrays.asList("AX_TEAM", "JR_DEVELOPER", "DEVELOPER", "SR_DEVELOPER", "DEPUTY", "MAIN_ADMIN", "OWNER")) admin(id, true);
        staff("SPONSOR_STAFF", "fakevival.command.vanish", "fakevival.command.vanish.see", "fakevival.staff.socialspy", "fakevival.staff.xray", "smp.admin.commandlog", "fakevival.staff.chatfilter", "fakevival.chatfilter.bypass");
        staff("GM", "fakevival.command.vanish", "fakevival.command.vanish.see", "fakevival.staff.socialspy", "fakevival.staff.xray", "smp.admin.commandlog", "fakevival.staff.chatfilter", "housing.staff");
        staff("MODERATOR", "fakevival.command.vanish", "fakevival.command.vanish.see", "fakevival.staff.socialspy", "fakevival.staff.xray", "smp.admin.commandlog", "fakevival.staff.chatfilter", "fakevival.admin.itembrowser", "housing.staff");
        staff("HELPER", "fakevival.command.vanish", "fakevival.command.vanish.see", "fakevival.staff.xray", "fakevival.staff.chatfilter", "housing.staff");
        staff("JR_HELPER", "fakevival.command.vanish", "fakevival.command.vanish.see", "fakevival.staff.xray", "fakevival.staff.chatfilter", "housing.staff");
        player("DEFAULT");
        player("VIP", "fakevival.command.kit.vip");
        player("VIP_PLUS", "fakevival.command.kit.vip", "fakevival.command.kit.vip_plus");
        player("MVP", "fakevival.command.kit.vip", "fakevival.command.kit.mvp");
        player("MVP_PLUS", "fakevival.command.kit.vip", "fakevival.command.kit.mvp", "fakevival.command.kit.mvp_plus");
        player("MVP_PLUS_PLUS", "fakevival.command.kit.vip", "fakevival.command.kit.mvp", "fakevival.command.kit.mvp_plus", "fakevival.command.kit.mvp_plus_plus");
        player("YOUTUBE", "fakevival.command.kit.youtube");
        player("PIG", "fakevival.command.kit.pig");
        player("PIG_PLUS", "fakevival.command.kit.pig", "fakevival.command.kit.pig_plus");
        player("PIG_PLUS_PLUS", "fakevival.command.kit.pig", "fakevival.command.kit.pig_plus", "fakevival.command.kit.pig_plus_plus");
        player("PIG_PLUS_PLUS_PLUS", "fakevival.command.kit.pig", "fakevival.command.kit.pig_plus", "fakevival.command.kit.pig_plus_plus", "fakevival.command.kit.pig_plus_plus_plus");
        player("BETA", "fakevival.rank.beta", "fakevival.admin.itembrowser");
        player("SPONSOR_1", "fakevival.rank.sponsor_1");
        player("SPONSOR_2", "fakevival.rank.sponsor_1", "fakevival.rank.sponsor_2");
        player("SPONSOR_3", "fakevival.rank.sponsor_1", "fakevival.rank.sponsor_2", "fakevival.rank.sponsor_3");
        player("SPECIAL", "fakevival.rank.special");
    }
    private HousingRankRegistry() {}
    private static void admin(String id, boolean swm) { List<String> p = new ArrayList<>(Arrays.asList("*", "minecraft.command.*", "bukkit.command.*")); if (swm) p.add("swm.*"); p.addAll(Arrays.asList("fakevival.staff.chatfilter", "fakevival.chatfilter.bypass", "housing.staff.gamemode", "housing.staff")); add(id, HousingRankDefinition.Category.ADMIN_STAFF, p); }
    private static void staff(String id, String... p) { add(id, HousingRankDefinition.Category.STAFF, Arrays.asList(p)); }
    private static void player(String id, String... p) { add(id, HousingRankDefinition.Category.PLAYER, Arrays.asList(p)); }
    private static void add(String id, HousingRankDefinition.Category c, List<String> p) { VALUES.put(id, new HousingRankDefinition(id, c, p)); }
    public static List<HousingRankDefinition> all() { return Collections.unmodifiableList(new ArrayList<>(VALUES.values())); }
    public static HousingRankDefinition get(String id) { return id == null ? null : VALUES.get(id.toUpperCase(Locale.ENGLISH)); }
    public static boolean contains(String id) { return get(id) != null; }
    public static List<HousingRankDefinition> byCategory(HousingRankDefinition.Category c) { List<HousingRankDefinition> out = new ArrayList<>(); for (HousingRankDefinition d : VALUES.values()) if (d.getCategory() == c) out.add(d); return Collections.unmodifiableList(out); }
}
