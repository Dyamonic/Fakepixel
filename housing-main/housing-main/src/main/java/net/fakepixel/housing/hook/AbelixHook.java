package net.fakepixel.housing.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AbelixHook {
    private final boolean enabled;

    public AbelixHook(Plugin plugin) {
        this.enabled = Bukkit.getPluginManager().getPlugin("stub-abelix-api") != null
                || classExists("club.abelix.api.FalconAPI")
                || classExists("club.abelix.api.base.gamer.Gamer");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getGold(Player player) {
        Object gamer = getGamer(player);
        Object value = invoke(gamer, "getGold");
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public boolean takeGold(Player player, int amount) {
        if (amount <= 0) return true;
        Object gamer = getGamer(player);
        if (gamer == null || getGold(player) < amount) return false;
        invoke(gamer, "takeGold", new Class[]{int.class}, amount);
        invoke(gamer, "save");
        return true;
    }

    public void addGold(Player player, int amount) {
        if (amount <= 0) return;
        Object gamer = getGamer(player);
        if (gamer == null) return;
        invoke(gamer, "addGold", new Class[]{int.class}, amount);
        invoke(gamer, "save");
    }

    public String getPrefix(Player player) {
        return prefixFromGamer(getGamer(player));
    }

    public String getPrefix(UUID uuid, String lastKnownName) {
        return prefixFromGamer(getGamer(uuid, lastKnownName));
    }

    public String getNameColor(UUID uuid, String lastKnownName) {
        Object gamer = getGamer(uuid, lastKnownName);
        Object rank = invoke(gamer, "getRank");
        Object value = firstNonNull(
                firstNonNull(
                        firstNonNull(
                                invoke(gamer, "getNameColor"),
                                invoke(gamer, "getNameColour")
                        ),
                        firstNonNull(
                                invoke(gamer, "getColor"),
                                invoke(gamer, "getChatColor")
                        )
                ),
                firstNonNull(
                        firstNonNull(
                                invoke(rank, "getNameColor"),
                                invoke(rank, "getNameColour")
                        ),
                        firstNonNull(
                                firstNonNull(
                                        invoke(rank, "getColor"),
                                        invoke(rank, "getChatColor")
                                ),
                                invoke(rank, "getPrefixColor")
                        )
                )
        );
        return value == null ? "" : String.valueOf(value);
    }

    public String getDisplayName(UUID uuid, String lastKnownName) {
        String name = lastKnownName == null || lastKnownName.trim().isEmpty()
                ? (uuid == null ? "Unknown" : uuid.toString().substring(0, 8))
                : lastKnownName.trim();
        String prefix = getPrefix(uuid, name);
        String nameColor = getNameColor(uuid, name);
        return (prefix == null ? "" : prefix) + (prefix == null || prefix.trim().isEmpty() ? "" : " ") + (nameColor == null ? "" : nameColor) + name;
    }

    private String prefixFromGamer(Object gamer) {
        Object value = firstNonNull(
                invoke(gamer, "getPrefix"),
                invoke(gamer, "getPrefixText")
        );
        return value == null ? "" : String.valueOf(value);
    }

    public String getRankName(Player player) {
        Object gamer = getGamer(player);
        Object value = invoke(gamer, "getRankName");
        if (value != null) return String.valueOf(value);
        Object rank = invoke(gamer, "getRank");
        value = invoke(rank, "getName");
        return value == null ? "DEFAULT" : String.valueOf(value);
    }

    public List<String> getRankIdentifiers(Player player) {
        List<String> identifiers = new ArrayList<>();
        Object gamer = getGamer(player);
        addIdentifier(identifiers, invoke(gamer, "getRankId"));
        addIdentifier(identifiers, invoke(gamer, "getRankName"));
        Object rank = invoke(gamer, "getRank");
        addIdentifier(identifiers, invoke(rank, "getId"));
        addIdentifier(identifiers, invoke(rank, "getKey"));
        addIdentifier(identifiers, invoke(rank, "getName"));
        return identifiers;
    }

    private void addIdentifier(List<String> identifiers, Object value) {
        if (value == null) return;
        String text = String.valueOf(value).trim();
        if (!text.isEmpty() && !identifiers.contains(text)) identifiers.add(text);
    }

    public int getRankPriority(Player player) {
        Object gamer = getGamer(player);
        Object value = invoke(gamer, "getRankPriority");
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    public boolean hasRankOrHigher(Player player, String rankName) {
        if (rankName == null || rankName.equalsIgnoreCase("DEFAULT") || rankName.equalsIgnoreCase("NONE")) return false;
        Object gamer = getGamer(player);
        Object value = invoke(gamer, "hasRankOrHigher", new Class[]{String.class}, rankName);
        if (value instanceof Boolean) return (Boolean) value;
        Object value2 = invoke(gamer, "hasRank", new Class[]{String.class}, rankName);
        return !(value2 instanceof Boolean) || !((Boolean) value2);
    }

    public String getDisplayName(Player player) {
        String prefix = getPrefix(player);
        return (prefix == null ? "" : prefix) + player.getName();
    }

    private Object getGamer(Player player) {
        if (player == null) return null;
        Object gamer = getGamer(player.getUniqueId(), player.getName());
        if (gamer != null) return gamer;
        return null;
    }

    private Object getGamer(UUID uuid, String playerName) {
        Player online = uuid == null ? null : Bukkit.getPlayer(uuid);
        if (online == null && playerName != null && !playerName.trim().isEmpty()) online = Bukkit.getPlayerExact(playerName.trim());
        try {
            Class<?> gamerClass = Class.forName("club.abelix.api.base.gamer.Gamer");
            if (online != null) {
                try {
                    Method method = gamerClass.getMethod("getGamer", Player.class);
                    Object gamer = method.invoke(null, online);
                    if (gamer != null) return gamer;
                } catch (Throwable ignored) {}
            }
            if (uuid != null) {
                try {
                    Method method = gamerClass.getMethod("getGamer", UUID.class);
                    Object gamer = method.invoke(null, uuid);
                    if (gamer != null) return gamer;
                } catch (Throwable ignored) {}
            }
            if (playerName != null && !playerName.trim().isEmpty()) {
                try {
                    Method method = gamerClass.getMethod("getGamer", String.class);
                    return method.invoke(null, playerName.trim());
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Object invoke(Object target, String methodName) {
        return invoke(target, methodName, new Class[0]);
    }

    private Object invoke(Object target, String methodName, Class[] parameterTypes, Object... args) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Throwable throwable) {
            return null;
        }
    }

    private Object firstNonNull(Object a, Object b) {
        return a != null ? a : b;
    }

    private boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }
}
