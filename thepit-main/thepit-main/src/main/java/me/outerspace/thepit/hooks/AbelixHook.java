package me.outerspace.thepit.hooks;

import me.outerspace.thepit.ThePitPlugin;
import me.outerspace.thepit.util.CC;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AbelixHook {
    private static final List<String> DEFAULT_GAMER_CLASSES = Arrays.asList(
            "club.abelix.api.base.gamer.Gamer",
            "club.abelix.api.gamer.Gamer",
            "me.abelix.api.base.gamer.Gamer",
            "net.fakepixel.api.base.gamer.Gamer"
    );

    private final ThePitPlugin plugin;
    private final Map<MethodKey, Optional<Method>> noArgMethods = new ConcurrentHashMap<>();
    private final Map<MethodKey, Optional<Method>> amountMethods = new ConcurrentHashMap<>();
    private boolean enabled;
    private Method getGamer;

    public AbelixHook(ThePitPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        enabled = false;
        getGamer = null;
        noArgMethods.clear();
        amountMethods.clear();
        List<String> classes = plugin.registry().abelixRanks().getStringList("abelix.gamer-classes");
        if (classes == null || classes.isEmpty()) classes = DEFAULT_GAMER_CLASSES;
        for (String className : classes) {
            try {
                Class<?> gamerClass = Class.forName(className);
                getGamer = findLookup(gamerClass);
                if (getGamer != null) {
                    enabled = true;
                    plugin.getLogger().info("Abelix/FakePixel API hook enabled using " + className + ".");
                    return;
                }
            } catch (Throwable ignored) { }
        }
        plugin.getLogger().warning("Abelix/FakePixel Gamer API was not found; external placeholders and LuckPerms will be used as fallbacks.");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Profile resolveProfile(Player player) {
        Object gamer = gamer(player);
        if (gamer == null) return new Profile("", "", "default", -1, null);
        Object group = invokeAny(gamer, "getPriorityGroup", "getRank", "rank", "getGroup");
        Object rank = invokeAny(group, "rank", "getRank");
        if (rank == null) rank = group;
        Object prefix = invokeAny(gamer, "getPrefix", "prefix", "getRankPrefix");
        if (prefix == null) prefix = invokeAny(rank, "getPrefix", "prefix", "getDisplayPrefix");
        Object color = invokeAny(gamer, "getRankColor", "getNameColor", "getPlayerNameColor", "rankColor", "nameColor");
        if (color == null) color = invokeAny(rank, "getColor", "getRankColor", "getNameColor", "color", "rankColor");
        Object name = invokeAny(rank, "getName", "name", "getId", "id");
        Object priority = invokeAny(rank, "getPriority", "priority", "getWeight", "weight");
        Object gold = invokeAny(gamer, "getGold", "gold", "getGoldBalance");
        return new Profile(
                prefix == null ? "" : CC.color(String.valueOf(prefix)),
                colorValue(color),
                name == null ? "default" : String.valueOf(name).toLowerCase(Locale.ENGLISH),
                number(priority, -1),
                gold == null ? null : number(gold, 0)
        );
    }

    public RankDetection detectRank(Player player) {
        Object gamer = gamer(player);
        if (gamer == null) return null;
        Object priorityGroup = invokeAny(gamer, "getPriorityGroup");
        RankDetection result = named(invokeAny(priorityGroup, "rank"), "Gamer#getPriorityGroup().rank().getName()");
        if (result != null) return result;
        result = named(invokeAny(priorityGroup, "getRank"), "Gamer#getPriorityGroup().getRank().getName()");
        if (result != null) return result;
        result = named(invokeAny(gamer, "getRank"), "Gamer#getRank().getName()");
        if (result != null) return result;
        result = named(invokeAny(gamer, "getGroup"), "Gamer#getGroup().getName()");
        if (result != null) return result;
        result = directName(gamer, "getRankName", "Gamer#getRankName()");
        return result != null ? result : directName(gamer, "getGroupName", "Gamer#getGroupName()");
    }

    public String getPrefix(Player player) { return resolveProfile(player).getPrefix(); }
    public String getRankColor(Player player) { return resolveProfile(player).getRankColor(); }
    public String getGroup(Player player) { return resolveProfile(player).getGroup(); }
    public int getPriority(Player player) { return resolveProfile(player).getPriority(); }
    public Integer getGold(Player player) { return resolveProfile(player).getGold(); }

    public boolean takeGold(Player player, int amount) {
        if (amount <= 0) return true;
        Object gamer = gamer(player);
        if (gamer == null) return false;
        Object value = invokeAny(gamer, "getGold", "gold", "getGoldBalance");
        Integer balance = value == null ? null : number(value, 0);
        if (balance == null || balance < amount) return false;
        if (!invokeAmount(gamer, amount, "takeGold", "removeGold", "withdrawGold")
                && !invokeAmount(gamer, balance - amount, "setGold", "setGoldBalance")) return false;
        invokeAny(gamer, "save", "update", "flush");
        if (plugin.getNetworkProfileCache() != null) plugin.getNetworkProfileCache().adjustGold(player, -amount);
        return true;
    }

    public boolean addGold(Player player, int amount) {
        if (amount == 0) return true;
        Object gamer = gamer(player);
        if (gamer == null || !invokeAmount(gamer, amount, "addGold", "giveGold", "depositGold")) return false;
        invokeAny(gamer, "save", "update", "flush");
        if (plugin.getNetworkProfileCache() != null) plugin.getNetworkProfileCache().adjustGold(player, amount);
        return true;
    }

    public boolean addExperience(Player player, int amount) {
        if (amount <= 0) return true;
        Object gamer = gamer(player);
        if (gamer == null || !invokeAmount(gamer, amount, "addExperience", "addNetworkExperience", "giveExperience", "addXp", "giveXp")) return false;
        invokeAny(gamer, "save", "update", "flush");
        return true;
    }

    public boolean addEventExperience(Player player, int amount) {
        if (amount <= 0) return true;
        Object gamer = gamer(player);
        if (gamer == null || !invokeAmount(gamer, amount, "addEventExperience", "giveEventExperience", "addEventXp", "giveEventXp")) return false;
        invokeAny(gamer, "save", "update", "flush");
        return true;
    }

    private RankDetection named(Object target, String method) {
        Object value = invokeAny(target, "getName");
        return value == null || String.valueOf(value).trim().isEmpty() ? null : new RankDetection(String.valueOf(value).trim(), method);
    }

    private RankDetection directName(Object target, String accessor, String method) {
        Object value = invokeAny(target, accessor);
        return value == null || String.valueOf(value).trim().isEmpty() ? null : new RankDetection(String.valueOf(value).trim(), method);
    }

    private Method findLookup(Class<?> type) {
        for (String name : new String[]{"getGamer", "getPlayer", "getProfile", "getPlayerData"}) {
            for (Method method : type.getMethods()) {
                if (!method.getName().equals(name) || !Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 1) continue;
                Class<?> parameter = method.getParameterTypes()[0];
                if (parameter == Player.class || parameter == java.util.UUID.class || parameter == String.class) {
                    method.setAccessible(true);
                    return method;
                }
            }
        }
        return null;
    }

    private Object gamer(Player player) {
        if (!enabled || player == null || getGamer == null) return null;
        try {
            Class<?> parameter = getGamer.getParameterTypes()[0];
            Object argument = parameter == Player.class ? player : parameter == java.util.UUID.class ? player.getUniqueId() : player.getName();
            return getGamer.invoke(null, argument);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object invokeAny(Object target, String... names) {
        if (target == null) return null;
        for (String name : names) {
            Method method = cachedMethod(target.getClass(), name, false);
            if (method == null) continue;
            try {
                return method.invoke(target);
            } catch (Throwable ignored) { }
        }
        return null;
    }

    private boolean invokeAmount(Object target, int amount, String... names) {
        if (target == null) return false;
        for (String name : names) {
            Method method = cachedMethod(target.getClass(), name, true);
            if (method == null) continue;
            Class<?> type = method.getParameterTypes()[0];
            Object value = type == long.class || type == Long.class ? (long) amount
                    : type == double.class || type == Double.class ? (double) amount : amount;
            try {
                Object result = method.invoke(target, value);
                return !(result instanceof Boolean) || (Boolean) result;
            } catch (Throwable ignored) { }
        }
        return false;
    }

    private Method cachedMethod(Class<?> type, String name, boolean amount) {
        MethodKey key = new MethodKey(type, name);
        Map<MethodKey, Optional<Method>> cache = amount ? amountMethods : noArgMethods;
        Optional<Method> cached = cache.get(key);
        if (cached != null) return cached.orElse(null);
        Method found = null;
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name)) continue;
            if ((!amount && method.getParameterTypes().length == 0) || (amount && method.getParameterTypes().length == 1)) {
                method.setAccessible(true);
                found = method;
                break;
            }
        }
        cache.put(key, Optional.ofNullable(found));
        return found;
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value).replaceAll("[^0-9-]", ""));
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private String colorValue(Object value) {
        if (value == null) return "";
        Object nested = invokeAny(value, "getChatColor", "getColorCode", "getCode");
        String text = String.valueOf(nested == null ? value : nested).trim();
        String colored = CC.color(text);
        if (colored.indexOf(org.bukkit.ChatColor.COLOR_CHAR) >= 0) return colored;
        try {
            return org.bukkit.ChatColor.valueOf(text.toUpperCase(Locale.ENGLISH)).toString();
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    public static final class Profile {
        private final String prefix;
        private final String rankColor;
        private final String group;
        private final int priority;
        private final Integer gold;

        public Profile(String prefix, String rankColor, String group, int priority, Integer gold) {
            this.prefix = prefix;
            this.rankColor = rankColor;
            this.group = group;
            this.priority = priority;
            this.gold = gold;
        }

        public String getPrefix() { return prefix; }
        public String getRankColor() { return rankColor; }
        public String getGroup() { return group; }
        public int getPriority() { return priority; }
        public Integer getGold() { return gold; }
    }

    public static final class RankDetection {
        private final String rawRank;
        private final String method;

        public RankDetection(String rawRank, String method) {
            this.rawRank = rawRank;
            this.method = method;
        }

        public String getRawRank() { return rawRank; }
        public String getMethod() { return method; }
    }

    private static final class MethodKey {
        private final Class<?> type;
        private final String name;

        private MethodKey(Class<?> type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof MethodKey)) return false;
            MethodKey key = (MethodKey) other;
            return type.equals(key.type) && name.equals(key.name);
        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + name.hashCode();
        }
    }
}
