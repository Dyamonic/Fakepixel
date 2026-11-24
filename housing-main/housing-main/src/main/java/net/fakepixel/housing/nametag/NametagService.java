package net.fakepixel.housing.nametag;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.hook.AbelixHook;
import net.fakepixel.housing.house.House;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.*;

public class NametagService {
    private static final int MODERN_TEAM_TEXT_FALLBACK_LIMIT = 256;

    private final HousingPlugin plugin;
    private final AbelixHook abelix;
    private final Map<UUID, ArmorStand> holograms = new HashMap<>();
    private int taskId = -1;
    private int movementTaskId = -1;

    public NametagService(HousingPlugin plugin, AbelixHook abelix) {
        this.plugin = plugin;
        this.abelix = abelix;
    }

    public void start() {
        if (!plugin.getStaticConfig().getBoolean("nametags.enabled", true) && !plugin.getStaticConfig().getBoolean("tablist.hide-house-players", true)) return;
        long ticks = plugin.getStaticConfig().getLong("nametags.update-ticks", 20L);
        taskId = new BukkitRunnable() {
            @Override public void run() {
                updateAll();
            }
        }.runTaskTimer(plugin, 20L, Math.max(1L, ticks)).getTaskId();

        if (plugin.getStaticConfig().getBoolean("nametags.enabled", true) && plugin.getStaticConfig().getBoolean("nametags.armorstand-overlay", false)) {
            long movementTicks = plugin.getStaticConfig().getLong("nametags.armorstand-movement-ticks", 1L);
            movementTaskId = new BukkitRunnable() {
                @Override public void run() {
                    updateHologramPositions();
                }
            }.runTaskTimer(plugin, 1L, Math.max(1L, movementTicks)).getTaskId();
        }
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
        if (movementTaskId != -1) Bukkit.getScheduler().cancelTask(movementTaskId);
        taskId = -1;
        movementTaskId = -1;
        clearHolograms();
    }

    public void reload() {
        stop();
        clearScoreboardTeams();
        start();
        updateAll();
    }

    public void updateAll() {
        if (!plugin.getStaticConfig().getBoolean("nametags.enabled", true) && !plugin.getStaticConfig().getBoolean("tablist.hide-house-players", true)) return;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (plugin.getStaticConfig().getBoolean("nametags.enabled", true)) {
                apply(viewer.getScoreboard());
                updatePlayerNames(viewer);
            }
            updateTabVisibility(viewer);
        }
        updateHolograms();
    }

    public void update(Player player) {
        if (player == null || (!plugin.getStaticConfig().getBoolean("nametags.enabled", true) && !plugin.getStaticConfig().getBoolean("tablist.hide-house-players", true))) return;
        if (plugin.getStaticConfig().getBoolean("nametags.enabled", true)) {
            updatePlayerNames(player);
            for (Player viewer : Bukkit.getOnlinePlayers()) apply(viewer.getScoreboard());
        }
        updateAll();
    }

    public void apply(Scoreboard board) {
        if (board == null || !plugin.getStaticConfig().getBoolean("nametags.enabled", true)) return;

        for (Player target : Bukkit.getOnlinePlayers()) {
            String teamName = teamName(target);
            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);

            String visible = formatNametag(target);
            if (plugin.getStaticConfig().getBoolean("nametags.armorstand-overlay", false)) {
                hideVanillaName(team);
            } else {
                NameParts parts = splitName(target, visible);
                showVanillaName(team);
                setTeamColor(team, abelixRankColor(target));
                setTeamText(team, parts.prefix, parts.suffix);
            }

            for (Team other : board.getTeams()) {
                if (!other.getName().startsWith("hntag")) continue;
                if (!other.getName().equals(teamName) && other.hasEntry(target.getName())) {
                    other.removeEntry(target.getName());
                }
            }

            if (!team.hasEntry(target.getName())) team.addEntry(target.getName());
        }
        if (plugin.getHousingNpcService() != null) {
            plugin.getHousingNpcService().applyHiddenNpcTeams(board);
        }
    }

    private void updatePlayerNames(Player player) {
        String displayFormat = plugin.getStaticConfig().getString("display-names.display-name-format", "%abelix_prefix%%player%");
        String display = plugin.applyPlaceholders(player, colorPlayerToken(player, normalizeFormat(displayFormat)));
        player.setDisplayName(display);

        String tabFormat = plugin.getStaticConfig().getString("display-names.tablist-format", "%abelix_prefix%%player%");
        String tab = plugin.applyPlaceholders(player, colorPlayerToken(player, normalizeFormat(tabFormat)));
        setPlayerListNameSafe(player, tab);
    }

    private void updateTabVisibility(Player viewer) {
        if (!plugin.getStaticConfig().getBoolean("tablist.hide-house-players", true) || plugin.getHouseManager() == null) return;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target) || shouldSeeInTab(viewer, target)) viewer.showPlayer(target);
            else viewer.hidePlayer(target);
        }
    }

    private boolean shouldSeeInTab(Player viewer, Player target) {
        House viewerHouse = plugin.getHouseManager().getHouseByWorld(viewer.getWorld().getName());
        House targetHouse = plugin.getHouseManager().getHouseByWorld(target.getWorld().getName());
        if (viewerHouse != null) {
            return targetHouse != null && viewer.getWorld().getName().equals(target.getWorld().getName());
        }
        return targetHouse == null;
    }

    private void updateHolograms() {
        if (!plugin.getStaticConfig().getBoolean("nametags.enabled", true) || !plugin.getStaticConfig().getBoolean("nametags.armorstand-overlay", false)) {
            clearHolograms();
            return;
        }

        removeStaleHolograms();

        for (Player player : Bukkit.getOnlinePlayers()) {
            ArmorStand stand = getOrCreateHologram(player);
            stand.setCustomName(trimTeamText(formatNametag(player)));
            moveHologram(player, stand);
        }
    }

    private void updateHologramPositions() {
        if (!plugin.getStaticConfig().getBoolean("nametags.enabled", true) || !plugin.getStaticConfig().getBoolean("nametags.armorstand-overlay", false)) return;
        removeStaleHolograms();
        for (Player player : Bukkit.getOnlinePlayers()) {
            ArmorStand stand = getOrCreateHologram(player);
            moveHologram(player, stand);
        }
    }

    private void removeStaleHolograms() {
        Iterator<Map.Entry<UUID, ArmorStand>> iterator = holograms.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ArmorStand> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            ArmorStand stand = entry.getValue();
            if (player == null || !player.isOnline() || stand == null || stand.isDead()) {
                if (stand != null && !stand.isDead()) stand.remove();
                iterator.remove();
            }
        }
    }

    private ArmorStand getOrCreateHologram(Player player) {
        ArmorStand stand = holograms.get(player.getUniqueId());
        if (stand == null || stand.isDead() || !stand.getWorld().equals(player.getWorld())) {
            if (stand != null && !stand.isDead()) stand.remove();
            stand = spawnHologram(player);
            holograms.put(player.getUniqueId(), stand);
        }
        return stand;
    }

    private ArmorStand spawnHologram(Player player) {
        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(hologramLocation(player), EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCanPickupItems(false);
        stand.setRemoveWhenFarAway(false);
        stand.setCustomNameVisible(true);
        tryCall(stand, "setInvisible", new Class[]{boolean.class}, true);
        tryCall(stand, "setInvulnerable", new Class[]{boolean.class}, true);
        tryCall(stand, "setCollidable", new Class[]{boolean.class}, false);
        tryCall(stand, "setSilent", new Class[]{boolean.class}, true);
        try { stand.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false)); } catch (Throwable ignored) {}
        tryCall(stand, "setBasePlate", new Class[]{boolean.class}, false);
        tryCall(stand, "setMarker", new Class[]{boolean.class}, true);
        tryCall(stand, "setSmall", new Class[]{boolean.class}, isAttachedMode());
        moveHologram(player, stand);
        return stand;
    }

    private Location hologramLocation(Player player) {
        double offset = isAttachedMode() ? 0.0 : plugin.getStaticConfig().getDouble("nametags.armorstand-offset-y", 0.0);
        return player.getLocation().clone().add(0.0, offset, 0.0);
    }

    private void moveHologram(Player player, ArmorStand stand) {
        if (isAttachedMode() && attachToPlayer(player, stand)) return;
        stand.teleport(hologramLocation(player));
    }

    private boolean attachToPlayer(Player player, ArmorStand stand) {
        Entity passenger = player.getPassenger();
        if (stand.equals(passenger)) return true;
        if (passenger != null) return false;
        stand.teleport(player.getLocation());
        return player.setPassenger(stand);
    }

    private boolean isAttachedMode() {
        return plugin.getStaticConfig().getBoolean("nametags.armorstand-attach-to-player", true);
    }

    private void clearHolograms() {
        for (ArmorStand stand : holograms.values()) {
            if (stand != null && !stand.isDead()) stand.remove();
        }
        holograms.clear();
    }

    private void clearScoreboardTeams() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == null) continue;
            for (Team team : new ArrayList<>(board.getTeams())) {
                if (!team.getName().startsWith("hntag")) continue;
                team.setPrefix("");
                team.setSuffix("");
                for (String entry : new ArrayList<>(team.getEntries())) {
                    team.removeEntry(entry);
                }
            }
        }
    }

    private void hideVanillaName(Team team) {
        team.setPrefix("");
        team.setSuffix("");
        setNameTagVisibility(team, "NEVER");
    }

    private void showVanillaName(Team team) {
        setNameTagVisibility(team, "ALWAYS");
    }

    private void setTeamColor(Team team, ChatColor color) {
        if (team == null || color == null) return;
        try {
            Team.class.getMethod("setColor", ChatColor.class).invoke(team, color);
        } catch (Throwable ignored) {

        }
    }

    private void setNameTagVisibility(Team team, String visibilityName) {
        try {
            Class<?> visibility = Class.forName("org.bukkit.scoreboard.NameTagVisibility");
            Object never = enumConstant(visibility, visibilityName);
            Method method = team.getClass().getMethod("setNameTagVisibility", visibility);
            method.invoke(team, never);
        } catch (Throwable ignored) {
        }
    }

    private Object enumConstant(Class<?> enumClass, String name) {
        for (Object constant : enumClass.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) return constant;
        }
        throw new IllegalArgumentException(name);
    }

    private void tryCall(Object target, String name, Class[] types, Object... args) {
        try {
            Method method = target.getClass().getMethod(name, types);
            method.invoke(target, args);
        } catch (Throwable ignored) {
        }
    }

    private void setTeamText(Team team, String prefix, String suffix) {
        prefix = safeTeamText(prefix);
        suffix = safeTeamText(suffix);
        if (!prefix.equals(team.getPrefix())) setTeamPrefixSafe(team, prefix);
        if (!suffix.equals(team.getSuffix())) setTeamSuffixSafe(team, suffix);
    }

    private void setTeamPrefixSafe(Team team, String text) {
        try {
            team.setPrefix(text == null ? "" : text);
        } catch (IllegalArgumentException ex) {
            team.setPrefix(fitRawWithColors(text, MODERN_TEAM_TEXT_FALLBACK_LIMIT));
        }
    }

    private void setTeamSuffixSafe(Team team, String text) {
        try {
            team.setSuffix(text == null ? "" : text);
        } catch (IllegalArgumentException ex) {
            team.setSuffix(fitRawWithColors(text, MODERN_TEAM_TEXT_FALLBACK_LIMIT));
        }
    }

    private String safeTeamText(String text) {
        if (text == null || text.isEmpty()) return "";
        return text;
    }

    private String fitRawWithColors(String text, int maxRawCharacters) {
        if (text == null || text.isEmpty() || maxRawCharacters <= 0) return "";
        int max = Math.min(text.length(), maxRawCharacters);
        if (max < text.length() && text.charAt(max - 1) == ChatColor.COLOR_CHAR) max--;
        return text.substring(0, max);
    }

    private void setPlayerListNameSafe(Player player, String text) {
        if (text == null) text = player.getName();
        try {
            player.setPlayerListName(text);
            return;
        } catch (IllegalArgumentException ignored) {
        }
        String coloredName = abelixNameColor(player) + player.getName();
        String trimmed = fitRawWithColors(text, plugin.getStaticConfig().getInt("display-names.max-tablist-length", 64));
        try {
            player.setPlayerListName(trimmed);
        } catch (IllegalArgumentException ignored) {
            player.setPlayerListName(coloredName);
        }
    }

    private String teamName(Player player) {
        int sort = Math.max(0, Math.min(9999, 9999 - rankSortPriority(player)));
        String hash = Integer.toHexString(player.getUniqueId().hashCode()).replace("-", "0");
        if (hash.length() > 7) hash = hash.substring(0, 7);
        while (hash.length() < 7) hash = MessageFormat.format("0{0}", hash);
        return "hntag" + String.format(java.util.Locale.ENGLISH, "%04d", sort) + hash;
    }

    private int rankSortPriority(Player player) {
        int priority = plugin.getAbelixHook() == null ? 0 : plugin.getAbelixHook().getRankPriority(player);
        if (priority > 0) return priority;
        if (player.hasPermission("housing.admin")) return 10000;
        if (player.hasPermission("housing.staff")) return 9000;
        String rank = plugin.getAbelixHook() == null ? "" : plugin.getAbelixHook().getRankName(player);
        String clean = rank == null ? "" : rank.toLowerCase(java.util.Locale.ENGLISH).replace("_", "").replace("-", "").replace(" ", "");
        if (clean.contains("owner") || clean.contains("admin") || clean.contains("manager") || clean.contains("developer")) return 10000;
        if (clean.contains("staff") || clean.contains("mod") || clean.contains("helper")) return 9000;
        if (clean.contains("youtube") || clean.contains("yt")) return 8000;
        if (clean.contains("mvp++") || clean.contains("mvpplusplus")) return 7000;
        if (clean.contains("mvp+")) return 6000;
        if (clean.contains("mvp")) return 5000;
        if (clean.contains("vip+")) return 4000;
        if (clean.contains("vip")) return 3000;
        return 0;
    }

    private String formatNametag(Player player) {
        String configured = plugin.getStaticConfig().getString("nametags.format", "%abelix_prefix%%player%");
        configured = normalizeFormat(configured);
        configured = colorPlayerToken(player, configured);
        return plugin.applyPlaceholders(player, configured);
    }

    private String normalizeFormat(String format) {
        if (format == null || format.trim().isEmpty()) return "%abelix_prefix% %player%";
        return format
                .replace("%abelix_prefix%%player%", "%abelix_prefix% %player%")
                .replace("%housing_prefix%%abelix_prefix%", "%housing_prefix% %abelix_prefix%")
                .replace("%group_suffix%%abelix_prefix%", "%group_suffix% %abelix_prefix%");
    }

    private String colorPlayerToken(Player player, String format) {
        String nameColor = abelixNameColor(player);
        if (nameColor.isEmpty() || format == null || !format.contains("%player%")) return format;
        return format.replace("%player%", nameColor + "%player%");
    }

    private String abelixNameColor(Player player) {
        ChatColor color = abelixRankColor(player);
        return color == null ? "" : color.toString();
    }

    private ChatColor abelixRankColor(Player player) {
        if (abelix == null || player == null) return null;
        String prefix = stripTrailingWhiteReset(plugin.color(abelix.getPrefix(player)));
        ChatColor lastColor = null;
        for (int i = 0; i < prefix.length() - 1; i++) {
            if (prefix.charAt(i) != ChatColor.COLOR_CHAR) continue;
            ChatColor color = ChatColor.getByChar(prefix.charAt(i + 1));
            if (color == null) continue;
            if (color == ChatColor.RESET || color == ChatColor.WHITE) continue;
            if (color.isColor()) lastColor = color;
        }
        return lastColor;
    }

    private String stripTrailingWhiteReset(String text) {
        if (text == null) return "";
        while (text.length() >= 2) {
            String ending = text.substring(text.length() - 2);
            if (!ending.equals(ChatColor.WHITE.toString()) && !ending.equals(ChatColor.RESET.toString())) break;
            text = text.substring(0, text.length() - 2);
        }
        return text;
    }

    private NameParts splitName(Player player, String visible) {
        String name = player.getName();
        String prefix = visible == null ? "" : visible;
        String suffix = "";
        int nameIndex = prefix.indexOf(name);
        if (nameIndex >= 0) {
            suffix = prefix.substring(nameIndex + name.length());
            prefix = prefix.substring(0, nameIndex);
        }

        ChatColor rankColor = abelixRankColor(player);
        String nameColor = rankColor == null ? ChatColor.getLastColors(prefix) : rankColor.toString();
        if (nameColor.isEmpty()) nameColor = ChatColor.WHITE.toString();

        String cleanPrefix = stripTrailingNameStart(prefix);
        String separator = nameSeparator(cleanPrefix);
        String safePrefix = cleanPrefix.isEmpty()
                ? trimTeamText(nameColor)
                : trimTeamText(cleanPrefix + nameColor + separator);
        String suffixColor = ChatColor.getLastColors(safePrefix);
        return new NameParts(safePrefix, trimTeamText(suffixColor + suffix));
    }

    private String stripTrailingNameStart(String text) {
        if (text == null) return "";
        text = stripTrailingWhiteReset(text);
        boolean changed;
        do {
            changed = false;
            while (!text.isEmpty() && Character.isWhitespace(text.charAt(text.length() - 1))) {
                text = text.substring(0, text.length() - 1);
                changed = true;
            }
            while (text.length() >= 2 && text.charAt(text.length() - 2) == ChatColor.COLOR_CHAR) {
                text = text.substring(0, text.length() - 2);
                changed = true;
            }
        } while (changed);
        return text;
    }

    private String nameSeparator(String prefix) {
        if (prefix == null || prefix.isEmpty()) return "";
        String separator = plugin.getStaticConfig().getString("nametags.name-separator", "");
        separator = plugin.color(separator);
        if (separator.isEmpty()) return "";
        String plainPrefix = ChatColor.stripColor(prefix);
        if (plainPrefix == null || plainPrefix.isEmpty()) return "";
        char last = plainPrefix.charAt(plainPrefix.length() - 1);
        if (Character.isWhitespace(last)) return "";
        return separator;
    }

    private String trimTeamText(String text) {
        return safeTeamText(text);
    }

    private static class NameParts {
        private final String prefix;
        private final String suffix;

        private NameParts(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }
}
