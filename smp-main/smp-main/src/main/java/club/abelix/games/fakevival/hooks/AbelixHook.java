package club.abelix.games.fakevival.hooks;

import club.abelix.games.fakevival.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AbelixHook {
    private final JavaPlugin plugin;
    private boolean hooked;

    private static final Map<String, RankInfo> RANKS_BY_NAME = new HashMap<>();
    private static final Map<Integer, RankInfo> RANKS_BY_ID = new HashMap<>();

    static {
        // Matches the rank database CSV: id, name, prefix, priority.
        register(1, "DEFAULT", "§7", 0);
        register(2, "VIP", "§a[VIP] ", 10);
        register(3, "VIP_PLUS", "§a[VIP§6+§a] ", 15);
        register(4, "MVP", "§b[MVP] ", 20);
        register(5, "MVP_PLUS", "§b[MVP<plus>§b] ", 25);
        register(6, "MVP_PLUS_PLUS", "§6[MVP<plus><plus>§6] ", 30);
        register(7, "YOUTUBE", "§c[§fYOUTUBE§c] ", 35);
        register(8, "PIG", "§d[PIG] ", 50);
        register(9, "PIG_PLUS", "§d[PIG<plus>§d] ", 51);
        register(10, "PIG_PLUS_PLUS", "§d[PIG<plus><plus>§d] ", 52);
        register(11, "PIG_PLUS_PLUS_PLUS", "§d[PIG<plus><plus><plus>§d] ", 53);
        register(12, "BETA", "§d[BT] ", 54);
        register(13, "SPONSOR_1", "§d[S<plus>§d] ", 61);
        register(14, "SPONSOR_2", "§d[S<plus><plus>§d] ", 62);
        register(15, "SPONSOR_3", "§c[S<plus><plus><plus>§c] ", 63);
        register(16, "SPECIAL", "§c[SPECIAL] ", 64);
        register(17, "BUILD", "§3[BUILD] ", 69);
        register(18, "JR_HELPER", "§9[JR HELPER] ", 70);
        register(19, "HELPER", "§9[HELPER] ", 75);
        register(20, "SUPPORT", "§5[SUPPORT] ", 77);
        register(21, "MODERATOR", "§2[MOD] ", 80);
        register(22, "GM", "§2[GM] ", 85);
        register(23, "SPONSOR_STAFF", "§d[SPONSOR] ", 90);
        register(24, "ADMINISTRATOR", "§c[ADMIN] ", 100);
        register(25, "AX_TEAM", "§5[TEAM] ", 125);
        register(26, "JR_DEVELOPER", "§c[DEV] ", 120);
        register(27, "DEVELOPER", "§c[DEV] ", 125);
        register(28, "SR_DEVELOPER", "§c[DEV] ", 180);
        register(29, "DEPUTY", "§c[DEPUTY] ", 190);
        register(30, "MAIN_ADMIN", "§c[MAIN ADMIN] ", 195);
        register(31, "OWNER", "§c[OWNER] ", 300);
    }

    public AbelixHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        hooked = Bukkit.getPluginManager().getPlugin("stub-abelix-api") != null
                || Bukkit.getPluginManager().getPlugin("AbelixAPI") != null
                || Bukkit.getPluginManager().getPlugin("FalconAPI") != null
                || classExists("club.abelix.api.base.gamer.Gamer")
                || classExists("club.abelix.api.FalconAPI");

        if (hooked && plugin != null) {
            plugin.getLogger().info("Hooked into Abelix/Falcon rank API.");
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public String getPrefix(Player player) {
        Object gamer = getGamer(player);
        if (gamer == null) return "";

        for (String methodName : new String[]{"getPrefix", "getPrefixText", "getRealPrefixText"}) {
            String value = callString(gamer, methodName);
            if (value != null && !value.isBlank() && !value.equalsIgnoreCase("null")) return value;
        }

        Object rank = getRankObject(gamer);
        String rankPrefix = callString(rank, "getPrefix");
        if (rankPrefix != null && !rankPrefix.isBlank() && !rankPrefix.equalsIgnoreCase("null")) return rankPrefix;

        RankInfo rankInfo = getRankInfo(player);
        return rankInfo == null ? "" : rankInfo.prefix;
    }

    public String getRankName(Player player) {
        Object gamer = getGamer(player);
        if (gamer == null) return "DEFAULT";

        for (String methodName : new String[]{"getRankName", "getGroupName"}) {
            String value = callString(gamer, methodName);
            if (isGood(value)) return normalizeRankName(value);
        }

        Object rank = getRankObject(gamer);
        for (String methodName : new String[]{"getName", "name", "getRankName"}) {
            String value = callString(rank, methodName);
            if (isGood(value)) return normalizeRankName(value);
        }

        int id = getRankId(player);
        RankInfo byId = RANKS_BY_ID.get(id);
        return byId == null ? "DEFAULT" : byId.name;
    }

    public int getRankId(Player player) {
        Object gamer = getGamer(player);
        if (gamer == null) return 1;

        for (String methodName : new String[]{"getRankId", "getGroupId"}) {
            Integer value = callInt(gamer, methodName);
            if (value != null) return value;
        }

        Object rank = getRankObject(gamer);
        for (String methodName : new String[]{"getId", "id", "getRankId"}) {
            Integer value = callInt(rank, methodName);
            if (value != null) return value;
        }

        return 1;
    }

    public int getRankPriority(Player player) {
        Object gamer = getGamer(player);
        if (gamer == null) return player != null && player.isOp() ? 300 : 0;

        for (String methodName : new String[]{"getRankPriority", "getPriority", "getGroupPriority"}) {
            Integer value = callInt(gamer, methodName);
            if (value != null) return value;
        }

        Object rank = getRankObject(gamer);
        for (String methodName : new String[]{"getPriority", "priority", "getLevel", "level"}) {
            Integer value = callInt(rank, methodName);
            if (value != null) return value;
        }

        RankInfo byName = RANKS_BY_NAME.get(getRankName(player));
        if (byName != null) return byName.priority;

        RankInfo byId = RANKS_BY_ID.get(getRankId(player));
        if (byId != null) return byId.priority;

        return player != null && player.isOp() ? 300 : 0;
    }

    public boolean hasRank(Player player, String rankName) {
        return getRankName(player).equals(normalizeRankName(rankName));
    }

    public boolean hasRankOrHigher(Player player, String requiredRankName) {
        RankInfo required = RANKS_BY_NAME.get(normalizeRankName(requiredRankName));
        if (required == null) return hasRank(player, requiredRankName);
        return hasRankOrHigher(player, required.priority);
    }

    public boolean hasRankOrHigher(Player player, int requiredPriority) {
        return getRankPriority(player) >= requiredPriority;
    }

    public boolean isHelper(Player player) {
        return hasRankOrHigher(player, "HELPER");
    }

    public boolean isModerator(Player player) {
        return hasRankOrHigher(player, "MODERATOR");
    }

    public boolean isAdmin(Player player) {
        return hasRankOrHigher(player, "ADMINISTRATOR");
    }

    public boolean isDeputy(Player player) {
        return hasRankOrHigher(player, "DEPUTY");
    }

    public boolean isOwner(Player player) {
        return hasRankOrHigher(player, "OWNER");
    }

    public String getGold(Player player) {
        Object gamer = getGamer(player);
        if (gamer == null) return "0";
        Double value = findGoldValue(gamer);
        return value == null ? "0" : EconomyManager.formatCompact(value);
    }

    public void setVanished(Player player, boolean vanished) {
        Object gamer = getGamer(player);
        if (gamer == null) return;
        for (String methodName : new String[]{"setVanished", "setVanish", "setInvisible", "setHidden"}) {
            if (callBoolean(gamer, methodName, boolean.class, vanished)) return;
            if (callBoolean(gamer, methodName, Boolean.class, vanished)) return;
        }
        for (String methodName : new String[]{"vanish", "setVanishMode"}) {
            if (callBoolean(gamer, methodName, boolean.class, vanished)) return;
            if (callBoolean(gamer, methodName, Boolean.class, vanished)) return;
        }
    }

    public Boolean isVanished(Player player) {
        Object gamer = getGamer(player);
        if (gamer == null) return null;
        for (String methodName : new String[]{"isVanished", "isVanish", "isInvisible", "isHidden"}) {
            Object value = call(gamer, methodName);
            if (value instanceof Boolean bool) return bool;
        }
        return null;
    }

    public boolean takeGold(Player player, int amount) {
        if (amount <= 0) return false;
        Object gamer = getGamer(player);
        if (gamer == null) return false;

        Double currentGold = findGoldValue(gamer);
        if (currentGold != null && currentGold < amount) return false;
        Double expectedGold = currentGold == null ? null : currentGold - amount;

        for (Object target : goldTargets(gamer)) {
            for (String methodName : new String[]{"takeGold", "removeGold", "withdrawGold", "subtractGold", "decreaseGold", "decrementGold"}) {
                if (callBoolean(target, methodName, int.class, amount)) return finishGoldMutation(player, gamer, target, expectedGold);
                if (callBoolean(target, methodName, double.class, (double) amount)) return finishGoldMutation(player, gamer, target, expectedGold);
                if (callBoolean(target, methodName, long.class, (long) amount)) return finishGoldMutation(player, gamer, target, expectedGold);
            }
            for (String methodName : new String[]{"addGold", "giveGold"}) {
                if (callBoolean(target, methodName, int.class, -amount)) return finishGoldMutation(player, gamer, target, expectedGold);
                if (callBoolean(target, methodName, double.class, -(double) amount)) return finishGoldMutation(player, gamer, target, expectedGold);
                if (callBoolean(target, methodName, long.class, -(long) amount)) return finishGoldMutation(player, gamer, target, expectedGold);
            }
        }

        if (currentGold != null) {
            double next = currentGold - amount;
            for (Object target : goldTargets(gamer)) {
                for (String methodName : new String[]{"setGold", "setGoldBalance", "updateGold"}) {
                    if (callBoolean(target, methodName, int.class, (int) next)) return finishGoldMutation(player, gamer, target, expectedGold);
                    if (callBoolean(target, methodName, double.class, next)) return finishGoldMutation(player, gamer, target, expectedGold);
                    if (callBoolean(target, methodName, long.class, (long) next)) return finishGoldMutation(player, gamer, target, expectedGold);
                }
            }
        }
        return false;
    }

    /** Fast preflight for store purchases; takeGold remains the authoritative balance mutation. */
    public boolean hasGold(Player player, int amount) {
        if (amount <= 0) return true;
        Object gamer = getGamer(player);
        if (gamer == null) return false;
        Double currentGold = findGoldValue(gamer);
        // Some Abelix API versions expose mutation but not a readable balance; defer to takeGold in that case.
        return currentGold == null || currentGold >= amount;
    }

    private boolean finishGoldMutation(Player player, Object gamer, Object target, Double expectedGold) {
        persistGoldChange(gamer, target);
        scheduleGoldCorrection(player, expectedGold);
        return true;
    }

    private void scheduleGoldCorrection(Player player, Double expectedGold) {
        if (plugin == null || player == null || expectedGold == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            Object refreshedGamer = getGamer(player);
            if (refreshedGamer == null) return;
            Double current = findGoldValue(refreshedGamer);
            if (current == null || Math.abs(current - expectedGold) <= 0.01D) {
                persistGoldChange(refreshedGamer, refreshedGamer);
                return;
            }
            for (Object target : goldTargets(refreshedGamer)) {
                if (applyGoldValue(target, current, expectedGold)) {
                    persistGoldChange(refreshedGamer, target);
                    return;
                }
            }
        }, 20L);
    }

    private boolean applyGoldValue(Object target, double currentGold, double expectedGold) {
        for (String methodName : new String[]{"setGold", "setGoldBalance", "updateGold"}) {
            if (callBoolean(target, methodName, int.class, (int) expectedGold)) return true;
            if (callBoolean(target, methodName, double.class, expectedGold)) return true;
            if (callBoolean(target, methodName, long.class, (long) expectedGold)) return true;
        }

        double delta = expectedGold - currentGold;
        if (Math.abs(delta) <= 0.01D) return true;
        if (delta > 0) {
            for (String methodName : new String[]{"addGold", "giveGold"}) {
                if (callBoolean(target, methodName, int.class, (int) delta)) return true;
                if (callBoolean(target, methodName, double.class, delta)) return true;
                if (callBoolean(target, methodName, long.class, (long) delta)) return true;
            }
        } else {
            double amount = Math.abs(delta);
            for (String methodName : new String[]{"takeGold", "removeGold", "withdrawGold", "subtractGold", "decreaseGold", "decrementGold"}) {
                if (callBoolean(target, methodName, int.class, (int) amount)) return true;
                if (callBoolean(target, methodName, double.class, amount)) return true;
                if (callBoolean(target, methodName, long.class, (long) amount)) return true;
            }
            for (String methodName : new String[]{"addGold", "giveGold"}) {
                if (callBoolean(target, methodName, int.class, (int) delta)) return true;
                if (callBoolean(target, methodName, double.class, delta)) return true;
                if (callBoolean(target, methodName, long.class, (long) delta)) return true;
            }
        }
        return false;
    }

    private boolean persistGoldChange(Object gamer, Object target) {
        boolean persisted = false;
        for (Object object : new Object[]{target, gamer}) {
            if (object == null) continue;
            for (String methodName : new String[]{"save", "saveAsync", "update", "persist", "flush", "sync", "saveData", "updateData"}) {
                persisted |= callVoidLike(object, methodName);
            }
        }

        Object api = falconApiInstance();
        if (api != null) {
            for (String repositoryMethod : new String[]{"getGamerRepository", "getUserRepository", "getProfileRepository", "getRepository"}) {
                Object repository = call(api, repositoryMethod);
                if (repository == null) continue;
                for (String saveMethod : new String[]{"save", "update", "persist", "saveGamer", "updateGamer"}) {
                    persisted |= callWithCompatibleArgument(repository, saveMethod, gamer);
                }
            }
            for (String saveMethod : new String[]{"saveGamer", "updateGamer", "savePlayer", "updatePlayer"}) {
                persisted |= callWithCompatibleArgument(api, saveMethod, gamer);
            }
        }

        return true;
    }

    private Object getGamer(Player player) {
        if (!hooked || player == null) return null;

        Object fromFalcon = getGamerFromFalcon(player);
        if (fromFalcon != null) return fromFalcon;

        try {
            Class<?> gamerClass = Class.forName("club.abelix.api.base.gamer.Gamer");
            for (String methodName : new String[]{"getGamer", "getGamerByUuid", "getByUuid", "get", "of", "findByUuid"}) {
                try {
                    Method method = gamerClass.getMethod(methodName, Player.class);
                    Object gamer = method.invoke(null, player);
                    if (gamer != null) return gamer;
                } catch (Throwable ignored) {
                }
                try {
                    Method method = gamerClass.getMethod(methodName, java.util.UUID.class);
                    Object gamer = method.invoke(null, player.getUniqueId());
                    if (gamer != null) return gamer;
                } catch (Throwable ignored) {
                }
                try {
                    Method method = gamerClass.getMethod(methodName, String.class);
                    Object gamer = method.invoke(null, player.getUniqueId().toString());
                    if (gamer != null) return gamer;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object getGamerFromFalcon(Player player) {
        try {
            Class<?> apiClass = Class.forName("club.abelix.api.FalconAPI");
            Object instance = callStatic(apiClass);
            if (instance == null) return null;

            for (String methodName : new String[]{"getGamer", "getPlayer", "getUser", "getProfile", "getAccount"}) {
                Object gamer = call(instance, methodName, Player.class, player);
                if (gamer != null) return gamer;
                gamer = call(instance, methodName, java.util.UUID.class, player.getUniqueId());
                if (gamer != null) return gamer;
                gamer = call(instance, methodName, String.class, player.getUniqueId().toString());
                if (gamer != null) return gamer;
            }

            Object repository = call(instance, "getGamerRepository");
            if (repository != null) {
                for (String methodName : new String[]{"get", "load", "find", "findByUuid", "getByUuid"}) {
                    Object gamer = call(repository, methodName, java.util.UUID.class, player.getUniqueId());
                    if (gamer != null) return gamer;
                    gamer = call(repository, methodName, String.class, player.getUniqueId().toString());
                    if (gamer != null) return gamer;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object falconApiInstance() {
        try {
            return callStatic(Class.forName("club.abelix.api.FalconAPI"));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Double findGoldValue(Object gamer) {
        for (Object target : goldTargets(gamer)) {
            Double value = readGoldValue(target);
            if (value != null) return value;
        }
        return null;
    }

    private List<Object> goldTargets(Object gamer) {
        List<Object> targets = new ArrayList<>();
        addTarget(targets, gamer);
        for (String methodName : new String[]{"getEconomy", "getWallet", "getBalance", "getBalances", "getCurrencies", "getCurrency", "getAccount", "getProfile", "getData", "getStats"}) {
            addTarget(targets, call(gamer, methodName));
        }
        return targets;
    }

    private void addTarget(List<Object> targets, Object target) {
        if (target != null && !targets.contains(target)) targets.add(target);
    }

    private Double readGoldValue(Object target) {
        if (target == null) return null;
        if (target instanceof Map<?, ?> map) return goldFromMap(map);

        for (String methodName : new String[]{"getGold", "gold", "getGoldBalance", "getBalanceGold", "getAbelixGold", "getPremiumGold", "getStoreGold"}) {
            Double value = toDouble(call(target, methodName));
            if (value != null) return value;
        }

        for (String methodName : new String[]{"get", "getBalance", "getCurrency", "getAmount", "amount"}) {
            for (String key : new String[]{"gold", "GOLD", "abelix_gold", "ABELIX_GOLD", "premium_gold", "store_gold"}) {
                Double value = toDouble(call(target, methodName, String.class, key));
                if (value != null) return value;
            }
        }

        for (String fieldName : new String[]{"gold", "goldBalance", "abelixGold", "premiumGold", "storeGold"}) {
            Double value = toDouble(field(target, fieldName));
            if (value != null) return value;
        }
        return null;
    }

    private Double goldFromMap(Map<?, ?> map) {
        for (String key : new String[]{"gold", "GOLD", "abelix_gold", "ABELIX_GOLD", "premium_gold", "store_gold"}) {
            if (map.containsKey(key)) {
                Double value = toDouble(map.get(key));
                if (value != null) return value;
            }
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        if (text.isBlank() || text.equalsIgnoreCase("null")) return null;
        try {
            return Double.parseDouble(text.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Object getRankObject(Object gamer) {
        if (gamer == null) return null;

        for (String methodName : new String[]{"getRank", "getGroup", "getPriorityRank", "getPriorityGroup"}) {
            Object rank = call(gamer, methodName);
            if (rank != null) {
                Object nestedRank = call(rank, "rank");
                return nestedRank == null ? rank : nestedRank;
            }
        }

        Object ranks = call(gamer, "getRanks");
        Object priorityRank = call(ranks, "findPriorityRank");
        Object rank = call(priorityRank, "rank");
        return rank == null ? priorityRank : rank;
    }

    private RankInfo getRankInfo(Player player) {
        RankInfo byName = RANKS_BY_NAME.get(getRankName(player));
        if (byName != null) return byName;
        return RANKS_BY_ID.get(getRankId(player));
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Object call(Object object, String methodName) {
        if (object == null) return null;
        try {
            Method method = object.getClass().getMethod(methodName);
            return method.invoke(object);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object call(Object object, String methodName, Class<?> type, Object arg) {
        if (object == null) return null;
        try {
            Method method = object.getClass().getMethod(methodName, type);
            return method.invoke(object, arg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean callVoidLike(Object object, String methodName) {
        if (object == null) return false;
        try {
            Method method = object.getClass().getMethod(methodName);
            Object result = method.invoke(object);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean callWithCompatibleArgument(Object object, String methodName, Object arg) {
        if (object == null || arg == null) return false;
        for (Method method : object.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(arg.getClass())) continue;
            try {
                Object result = method.invoke(object, arg);
                return !(result instanceof Boolean) || (Boolean) result;
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    private Object field(Object object, String fieldName) {
        if (object == null) return null;
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object callStatic(Class<?> clazz) {
        try {
            Method method = clazz.getMethod("getInstance");
            return method.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean callBoolean(Object object, String methodName, Class<?> type, Object arg) {
        if (object == null) return false;
        try {
            Method method = object.getClass().getMethod(methodName, type);
            Object result = method.invoke(object, arg);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private Integer callInt(Object object, String methodName) {
        Object value = call(object, methodName);
        if (value instanceof Number number) return number.intValue();
        if (value == null) return null;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String callString(Object object, String methodName) {
        Object value = call(object, methodName);
        return value == null ? null : String.valueOf(value);
    }

    private String formatGold(String value) {
        if (value == null || value.isBlank()) return "0";
        try {
            return EconomyManager.formatCompact(Double.parseDouble(value.replace(",", "").trim()));
        } catch (Exception ignored) {
            return value;
        }
    }

    private static void register(int id, String name, String prefix, int priority) {
        RankInfo info = new RankInfo(id, normalizeRankName(name), prefix, priority);
        RANKS_BY_ID.put(id, info);
        RANKS_BY_NAME.put(info.name, info);
    }

    private static String normalizeRankName(String name) {
        if (name == null || name.isBlank()) return "DEFAULT";
        return name.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean isGood(String value) {
        return value != null && !value.isBlank() && !value.equalsIgnoreCase("null");
    }

    private static final class RankInfo {
        private final String name;
        private final String prefix;
        private final int priority;

        private RankInfo(int id, String name, String prefix, int priority) {
            this.name = name;
            this.prefix = prefix;
            this.priority = priority;
        }
    }
}
