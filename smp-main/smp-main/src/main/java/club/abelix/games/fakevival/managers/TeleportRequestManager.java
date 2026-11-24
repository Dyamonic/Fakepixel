package club.abelix.games.fakevival.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TeleportRequestManager {
    private final JavaPlugin plugin;
    private final Map<UUID, TeleportRequest> requestsByTarget = new HashMap<>();

    public TeleportRequestManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void request(Player requester, Player target, boolean here) {
        purgeExpired();
        requestsByTarget.put(target.getUniqueId(), new TeleportRequest(requester.getUniqueId(), target.getUniqueId(), here, System.currentTimeMillis()));
    }

    public TeleportRequest getRequest(Player target, String requesterName) {
        purgeExpired();
        TeleportRequest request = requestsByTarget.get(target.getUniqueId());
        if (request == null) return null;
        if (requesterName == null) return request;
        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null || !requester.getName().equalsIgnoreCase(requesterName)) return null;
        return request;
    }

    public boolean cancelOutgoing(Player requester) {
        purgeExpired();
        Iterator<TeleportRequest> iterator = requestsByTarget.values().iterator();
        boolean removed = false;
        while (iterator.hasNext()) {
            if (iterator.next().requester().equals(requester.getUniqueId())) {
                iterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    public void remove(TeleportRequest request) {
        requestsByTarget.remove(request.target());
    }

    public boolean isExpired(TeleportRequest request) {
        long timeoutMillis = Math.max(1L, club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getLong("tpa.request-timeout-seconds", 60L)) * 1000L;
        return System.currentTimeMillis() - request.createdAtMillis() > timeoutMillis;
    }

    private void purgeExpired() {
        requestsByTarget.values().removeIf(this::isExpired);
    }

    public record TeleportRequest(UUID requester, UUID target, boolean here, long createdAtMillis) {
    }
}
