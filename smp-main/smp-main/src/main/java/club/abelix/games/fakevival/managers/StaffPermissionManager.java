package club.abelix.games.fakevival.managers;

import club.abelix.games.fakevival.data.ConfigFileManager;
import club.abelix.games.fakevival.hooks.AbelixHook;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffPermissionManager {
    private final JavaPlugin plugin;
    private final AbelixHook abelixHook;
    private final ConfigFileManager configFileManager;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private final Set<UUID> originalOps = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> RANK_ALIASES = aliases();
    private static final List<PriorityRank> PRIORITY_RANKS = priorityRanks();

    public StaffPermissionManager(JavaPlugin plugin, AbelixHook abelixHook, ConfigFileManager configFileManager) {
        this.plugin = plugin;
        this.abelixHook = abelixHook;
        this.configFileManager = configFileManager;
        ensureDefaults();
    }

    private void ensureDefaults() {
        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).isConfigurationSection("staff-permissions")) return;
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("staff-permissions.enabled", true);
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("staff-permissions.set-operator", true);
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("staff-permissions.operator-from-wildcard", true);
        club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).set("staff-permissions.notify", true);
        plugin.saveConfig();
    }

    public void applyAsync(Player player) {
        if (player == null || !club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("staff-permissions.enabled", true)) return;
        if (!abelixHook.isHooked()) return;
        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String rankName = abelixHook.getRankName(player);
            int rankPriority = abelixHook.getRankPriority(player);
            RankMatch rankMatch = getRankMatch(rankName, rankPriority);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online == null) return;
                if (rankMatch.group != RankGroup.NONE) grant(online, rankName, rankMatch);
                else revoke(online);
            });
        });
    }

    public void revoke(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        PermissionAttachment attachment = attachments.remove(uuid);
        if (attachment != null) player.removeAttachment(attachment);

        if (club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("staff-permissions.set-operator", false) && originalOps.remove(uuid) && player.isOp()) {
            player.setOp(false);
        }
    }

    public void cleanupAll() {
        for (Player player : Bukkit.getOnlinePlayers()) revoke(player);
        attachments.clear();
        originalOps.clear();
    }

    private void grant(Player player, String rankName, RankMatch rankMatch) {
        PermissionAttachment existing = attachments.remove(player.getUniqueId());
        if (existing != null) player.removeAttachment(existing);
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachments.put(player.getUniqueId(), attachment);

        List<String> grantedPermissions = getGrantedPermissions(rankMatch);
        for (String permission : grantedPermissions) {
            attachment.setPermission(permission, true);
        }
        player.recalculatePermissions();

        if (rankMatch.group == RankGroup.ADMIN_STAFF
                && grantedPermissions.contains("*")
                && shouldAutoOpWildcardRank()
                && !player.isOp()) {
            originalOps.add(player.getUniqueId());
            player.setOp(true);
        }

        if (rankMatch.group != RankGroup.PLAYER_RANK && club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("staff-permissions.notify", true)) {
            player.sendMessage(CC.color("&a" + rankMatch.group.displayName + " permissions applied for rank &f" + rankName + "&a."));
        }
    }

    private RankMatch getRankMatch(String rankName, int rankPriority) {
        String normalized = normalize(rankName);
        normalized = RANK_ALIASES.getOrDefault(normalized, normalized);
        String adminPath = findRankSection("rank-detection.admin-staff", normalized);
        if (adminPath != null) return new RankMatch(RankGroup.ADMIN_STAFF, adminPath);
        String staffPath = findRankSection("rank-detection.staff", normalized);
        if (staffPath != null) return new RankMatch(RankGroup.STAFF, staffPath);
        String playerPath = findRankSection("rank-detection.player-ranks", normalized);
        if (playerPath != null) return new RankMatch(RankGroup.PLAYER_RANK, playerPath);
        RankMatch priorityMatch = rankPriorityMatch(rankPriority);
        if (priorityMatch.group != RankGroup.NONE) return priorityMatch;
        return new RankMatch(RankGroup.NONE, null);
    }

    private String findRankSection(String path, String normalizedRank) {
        ConfigurationSection section = getRanksConfig().getConfigurationSection(path);
        if (section == null) return null;
        for (String rankKey : section.getKeys(false)) {
            if (normalizedRank.equals(normalize(rankKey))) return path + "." + rankKey;
            if (getRanksConfig().getStringList(path + "." + rankKey + ".aliases").stream().map(this::normalize).anyMatch(normalizedRank::equals)) {
                return path + "." + rankKey;
            }
        }
        return null;
    }

    private RankMatch rankPriorityMatch(int priority) {
        for (PriorityRank rank : PRIORITY_RANKS) {
            if (priority < rank.minimumPriority()) continue;
            String path = findRankSection(rank.group().path, rank.rank());
            if (path != null) return new RankMatch(rank.group(), path);
        }
        return new RankMatch(RankGroup.NONE, null);
    }

    private List<String> getGrantedPermissions(RankMatch rankMatch) {
        Set<String> permissions = new HashSet<>();
        if (rankMatch.group == RankGroup.ADMIN_STAFF) {
            for (Permission permission : plugin.getDescription().getPermissions()) {
                if (permission.getDefault() == PermissionDefault.OP
                        || permission.getName().contains(".admin.")
                        || permission.getName().startsWith("smp.admin.")
                        || permission.getName().contains(".staff.")) {
                    permissions.add(permission.getName());
                }
            }
        }
        if (rankMatch.rankPath != null) permissions.addAll(getRanksConfig().getStringList(rankMatch.rankPath + ".permissions"));
        permissions.addAll(expandWildcards(permissions));
        permissions.removeIf(permission -> permission == null || permission.isBlank());
        List<String> sorted = new ArrayList<>(permissions);
        sorted.sort(String::compareToIgnoreCase);
        return sorted;
    }

    private Set<String> expandWildcards(Set<String> permissions) {
        Set<String> expanded = new HashSet<>();
        for (String permission : permissions) {
            if (permission == null || !permission.endsWith(".*")) continue;
            String prefix = permission.substring(0, permission.length() - 1).toLowerCase(Locale.ROOT);
            for (Permission registered : plugin.getDescription().getPermissions()) {
                String name = registered.getName();
                if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) expanded.add(name);
            }
        }
        if (permissions.contains("*")) {
            for (Permission registered : plugin.getDescription().getPermissions()) expanded.add(registered.getName());
        }
        return expanded;
    }

    private FileConfiguration getRanksConfig() {
        return configFileManager.getRanks();
    }

    private boolean shouldAutoOpWildcardRank() {
        return club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("staff-permissions.set-operator", false)
                || club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("staff-permissions.operator-from-wildcard", true);
    }

    private String normalize(String rankName) {
        return normalizeRank(rankName);
    }

    private static String normalizeRank(String rankName) {
        if (rankName == null || rankName.isBlank()) return "";
        return rankName.trim()
                .replace("+++", "_PLUS_PLUS_PLUS")
                .replace("++", "_PLUS_PLUS")
                .replace("+", "_PLUS")
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static Map<String, String> aliases() {
        Map<String, String> aliases = new HashMap<>();
        alias(aliases, "OWNER", "OWNER");
        alias(aliases, "MAIN_ADMIN", "MAIN_ADMIN");
        alias(aliases, "DEPUTY", "DEPUTY");
        alias(aliases, "SR_DEV", "SR_DEVELOPER");
        alias(aliases, "DEV", "DEVELOPER");
        alias(aliases, "JR_DEV", "JR_DEVELOPER");
        alias(aliases, "ADMIN", "ADMINISTRATOR");
        alias(aliases, "SPONSOR_STAFF", "SPONSOR_STAFF");
        alias(aliases, "SUPPORT", "SUPPORT");
        alias(aliases, "GM", "MODERATOR");
        alias(aliases, "MOD", "MODERATOR");
        alias(aliases, "HELPER", "HELPER");
        alias(aliases, "JR_HELPER", "JR_HELPER");
        alias(aliases, "SPECIAL", "SPECIAL");
        alias(aliases, "SPONSOR_PLUS_PLUS_PLUS", "SPONSOR_3");
        alias(aliases, "SPONSOR_PLUS_PLUS", "SPONSOR_2");
        alias(aliases, "SPONSOR_PLUS", "SPONSOR_1");
        alias(aliases, "BETA", "BETA");
        alias(aliases, "PIG+++", "PIG_PLUS_PLUS_PLUS");
        alias(aliases, "PIG++", "PIG_PLUS_PLUS");
        alias(aliases, "PIG+", "PIG_PLUS");
        alias(aliases, "YOUTUBE", "YOUTUBE");
        alias(aliases, "MVP++", "MVP_PLUS_PLUS");
        alias(aliases, "MVP+", "MVP_PLUS");
        alias(aliases, "MVP", "MVP");
        alias(aliases, "VIP+", "VIP_PLUS");
        alias(aliases, "VIP", "VIP");
        alias(aliases, "DEFAULT", "DEFAULT");
        return Map.copyOf(aliases);
    }

    private static void alias(Map<String, String> aliases, String alias, String rank) {
        aliases.put(normalizeRank(alias), rank);
    }

    private static List<PriorityRank> priorityRanks() {
        List<PriorityRank> ranks = new ArrayList<>(List.of(
                new PriorityRank(300, "OWNER", RankGroup.ADMIN_STAFF),
                new PriorityRank(195, "MAIN_ADMIN", RankGroup.ADMIN_STAFF),
                new PriorityRank(190, "DEPUTY", RankGroup.ADMIN_STAFF),
                new PriorityRank(180, "SR_DEVELOPER", RankGroup.ADMIN_STAFF),
                new PriorityRank(125, "DEVELOPER", RankGroup.ADMIN_STAFF),
                new PriorityRank(120, "JR_DEVELOPER", RankGroup.ADMIN_STAFF),
                new PriorityRank(100, "ADMINISTRATOR", RankGroup.ADMIN_STAFF),
                new PriorityRank(90, "SPONSOR_STAFF", RankGroup.STAFF),
                new PriorityRank(85, "GM", RankGroup.STAFF),
                new PriorityRank(80, "MODERATOR", RankGroup.STAFF),
                new PriorityRank(75, "HELPER", RankGroup.STAFF),
                new PriorityRank(70, "JR_HELPER", RankGroup.STAFF),
                new PriorityRank(64, "SPECIAL", RankGroup.PLAYER_RANK),
                new PriorityRank(63, "SPONSOR_3", RankGroup.PLAYER_RANK),
                new PriorityRank(62, "SPONSOR_2", RankGroup.PLAYER_RANK),
                new PriorityRank(61, "SPONSOR_1", RankGroup.PLAYER_RANK),
                new PriorityRank(54, "BETA", RankGroup.PLAYER_RANK),
                new PriorityRank(53, "PIG_PLUS_PLUS_PLUS", RankGroup.PLAYER_RANK),
                new PriorityRank(52, "PIG_PLUS_PLUS", RankGroup.PLAYER_RANK),
                new PriorityRank(51, "PIG_PLUS", RankGroup.PLAYER_RANK),
                new PriorityRank(50, "PIG", RankGroup.PLAYER_RANK),
                new PriorityRank(35, "YOUTUBE", RankGroup.PLAYER_RANK),
                new PriorityRank(30, "MVP_PLUS_PLUS", RankGroup.PLAYER_RANK),
                new PriorityRank(25, "MVP_PLUS", RankGroup.PLAYER_RANK),
                new PriorityRank(20, "MVP", RankGroup.PLAYER_RANK),
                new PriorityRank(15, "VIP_PLUS", RankGroup.PLAYER_RANK),
                new PriorityRank(10, "VIP", RankGroup.PLAYER_RANK),
        new PriorityRank(5, "DEFAULT", RankGroup.PLAYER_RANK)
        ));
        ranks.sort(Comparator.comparingInt(PriorityRank::minimumPriority).reversed());
        return List.copyOf(ranks);
    }

    private enum RankGroup {
        ADMIN_STAFF("Admin staff", "rank-detection.admin-staff"),
        STAFF("Staff", "rank-detection.staff"),
        PLAYER_RANK("Rank", "rank-detection.player-ranks"),
        NONE("No", "");

        private final String displayName;
        private final String path;

        RankGroup(String displayName, String path) {
            this.displayName = displayName;
            this.path = path;
        }
    }

    private record RankMatch(RankGroup group, String rankPath) {
    }

    private record PriorityRank(int minimumPriority, String rank, RankGroup group) {
    }
}
