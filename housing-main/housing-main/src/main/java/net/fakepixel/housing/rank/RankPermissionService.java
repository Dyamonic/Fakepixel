package net.fakepixel.housing.rank;

import net.fakepixel.housing.HousingPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;

public class RankPermissionService implements Listener {
    private final HousingPlugin plugin;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();
    private final Map<String, HousingRankDefinition> ranks = new LinkedHashMap<>();

    public RankPermissionService(HousingPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ranks.clear();
        for (HousingRankDefinition rank : HousingRankRegistry.all()) ranks.put(rank.getId(), rank);

        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
        plugin.getLogger().info("Loaded " + ranks.size() + " compiled rank permission mappings.");
    }

    public void shutdown() {
        for (UUID uuid : new ArrayList<>(attachments.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) remove(player);
        }
        attachments.clear();
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) apply(event.getPlayer());
        }, 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    public void apply(Player player) {
        remove(player);

        HousingRankDefinition entry = getEntry(player);
        if (entry == null) return;

        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String permission : entry.getPermissions()) {
            attachment.setPermission(permission, true);
        }

        if (entry.getCategory() == HousingRankDefinition.Category.ADMIN_STAFF) {
            attachment.setPermission("housing.admin", true);
            attachment.setPermission("housing.staff", true);
            attachment.setPermission("housing.staff.admin", true);
            attachment.setPermission("housing.staff.build", true);
            attachment.setPermission("housing.staff.reset", true);
            attachment.setPermission("housing.staff.fly", true);
            attachment.setPermission("housing.staff.spy", true);
            attachment.setPermission("housing.staff.dry", true);
            attachment.setPermission("housing.staff.bypassborder", true);
            attachment.setPermission("housing.staff.award", true);
        } else if (entry.getCategory() == HousingRankDefinition.Category.STAFF) {
            attachment.setPermission("housing.staff", true);
            attachment.setPermission("housing.staff.build", true);
            attachment.setPermission("housing.staff.fly", true);
            attachment.setPermission("housing.staff.spy", true);
            attachment.setPermission("housing.staff.dry", true);
            attachment.setPermission("housing.staff.bypassborder", true);
            attachment.setPermission("housing.staff.award", true);
        }

        attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
    }

    public boolean isHousingStaff(Player player) {
        HousingRankDefinition entry = getEntry(player);
        return entry != null && (entry.getCategory() == HousingRankDefinition.Category.STAFF || entry.getCategory() == HousingRankDefinition.Category.ADMIN_STAFF);
    }

    public boolean isAdminStaff(Player player) {
        HousingRankDefinition entry = getEntry(player);
        return entry != null && entry.getCategory() == HousingRankDefinition.Category.ADMIN_STAFF;
    }

    public String getDetectedRank(Player player) {
        return normalize(plugin.getAbelixHook().getRankName(player));
    }

    public String getDetectedCategory(Player player) {
        HousingRankDefinition entry = getEntry(player);
        return entry == null ? "unknown" : entry.getCategory().name().toLowerCase(Locale.ENGLISH).replace('_', '-');
    }

    private void remove(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) {
            try {
                player.removeAttachment(old);
            } catch (IllegalArgumentException ignored) {}
            player.recalculatePermissions();
        }
    }

    private HousingRankDefinition getEntry(Player player) {
        String rank = getDetectedRank(player);
        return ranks.get(rank);
    }

    private String normalize(String rank) {
        if (rank == null || rank.trim().isEmpty()) return "DEFAULT";
        String cleaned = rank.trim().replace('-', '_').replace(' ', '_');
        return cleaned.toUpperCase(Locale.ENGLISH);
    }

}
