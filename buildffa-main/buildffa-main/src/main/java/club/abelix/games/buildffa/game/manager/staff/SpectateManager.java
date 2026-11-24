package club.abelix.games.buildffa.game.manager.staff;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.event.GameDeathEvent;
import club.abelix.games.buildffa.game.event.GameMapChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpectateManager extends YListener {
    private final BuildFFAGame game;
    private final Map<UUID, SpectateSession> sessions = new HashMap<>();
    private final BukkitTask validationTask;
    private boolean transferringArena;
    private boolean shuttingDown;

    public SpectateManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
        validationTask = Bukkit.getScheduler().runTaskTimer(game.getPlugin(), this::validateSessions, 20L, 20L);
    }

    public void execute(Gamer gamer, String[] arguments) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> execute(gamer, arguments));
            return;
        }
        Player staff = gamer.getPlayer();
        if (shuttingDown || !game.hasPermission(staff, BuildFFAGame.SPECTATE_PERMISSION)) {
            gamer.sendLocalizedMessage("buildffa_spectate_no_permission");
            return;
        }
        if (arguments.length == 0) {
            gamer.sendLocalizedMessage("buildffa_spectate_usage");
            return;
        }
        if (arguments[0].equalsIgnoreCase("leave")) {
            if (!end(staff.getUniqueId(), "buildffa_spectate_ended")) gamer.sendLocalizedMessage("buildffa_spectate_none");
            return;
        }
        Player target = Bukkit.getPlayerExact(arguments[0]);
        if (!isAvailable(target)) {
            gamer.sendLocalizedMessage("buildffa_spectate_target_unavailable", "%player%", arguments[0]);
            return;
        }
        if (staff.getUniqueId().equals(target.getUniqueId())) {
            gamer.sendLocalizedMessage("buildffa_spectate_self");
            return;
        }
        Gamer targetGamer = Gamer.getGamer(target);
        if (targetGamer.isVanished()) {
            gamer.sendLocalizedMessage("buildffa_spectate_target_unavailable", "%player%", target.getName());
            return;
        }
        SpectateSession existing = sessions.get(staff.getUniqueId());
        if (existing == null) {
            existing = new SpectateSession(UUID.randomUUID(), target.getUniqueId(), staff.getLocation().clone(),
                    staff.getGameMode(), staff.getAllowFlight(), staff.isFlying());
            sessions.put(staff.getUniqueId(), existing);
            begin(staff, target, existing.id);
            gamer.sendLocalizedMessage("buildffa_spectate_started", "%player%", targetGamer.getDisplayName());
        } else {
            existing.targetId = target.getUniqueId();
            existing.id = UUID.randomUUID();
            begin(staff, target, existing.id);
            gamer.sendLocalizedMessage("buildffa_spectate_changed", "%player%", targetGamer.getDisplayName());
        }
    }

    private void begin(Player staff, Player target, UUID sessionId) {
        game.invalidateInventoryRequest(staff);
        game.resetPlayerKitApplication(staff);
        game.getEventManager().beforeInventoryReset(staff);
        staff.setGameMode(GameMode.SPECTATOR);
        staff.setAllowFlight(true);
        staff.setFlying(true);
        staff.teleport(target.getLocation().clone());
        SpectateSession current = sessions.get(staff.getUniqueId());
        if (current == null || !current.id.equals(sessionId)) return;
        Gamer targetGamer = Gamer.getGamer(target);
        current.targetName = targetGamer == null ? target.getName() : targetGamer.getDisplayName();
        try {
            staff.setSpectatorTarget(target);
        } catch (RuntimeException ignored) {
        }
    }

    public boolean end(UUID staffId, String messageKey) {
        return end(staffId, messageKey, false);
    }

    private boolean end(UUID staffId, String messageKey, boolean forceFallback) {
        SpectateSession session = sessions.remove(staffId);
        if (session == null) return false;
        Player staff = Bukkit.getPlayer(staffId);
        if (staff == null || !staff.isOnline()) return true;
        try {
            staff.setSpectatorTarget(null);
        } catch (RuntimeException ignored) {
        }
        staff.setGameMode(session.gameMode);
        boolean restored = !forceFallback && !session.arenaTransferred && valid(session.returnLocation);
        if (restored) {
            staff.teleport(session.returnLocation.clone());
            game.resetPlayerKitApplication(staff);
            if (session.gameMode == GameMode.SURVIVAL && !game.isLobbyZone(staff.getLocation()))
                game.applySelectedKit(Gamer.getGamer(staff));
            else game.getEventManager().onInventoryReady(staff);
        } else game.resetPlayer(staff);
        staff.setAllowFlight(session.allowFlight);
        staff.setFlying(session.allowFlight && session.flying);
        Gamer gamer = Gamer.getGamer(staff);
        if (gamer != null) gamer.sendLocalizedMessage(restored ? messageKey : "buildffa_spectate_return_unavailable",
                "%player%", session.targetName);
        return true;
    }

    public boolean isSpectating(Player player) {
        return player != null && sessions.containsKey(player.getUniqueId());
    }

    private void validateSessions() {
        if (shuttingDown || transferringArena) return;
        for (UUID staffId : sessions.keySet().toArray(new UUID[0])) {
            SpectateSession session = sessions.get(staffId);
            Player staff = Bukkit.getPlayer(staffId);
            Player target = session == null ? null : Bukkit.getPlayer(session.targetId);
            if (session == null) continue;
            if (staff == null || !staff.isOnline()) {
                sessions.remove(staffId, session);
                continue;
            }
            if (!game.hasPermission(staff, BuildFFAGame.SPECTATE_PERMISSION)) {
                end(staffId, "buildffa_spectate_ended");
                continue;
            }
            if (target == null || !target.isOnline()) {
                end(staffId, "buildffa_spectate_target_disconnected");
                continue;
            }
            if (target.isDead()) {
                end(staffId, "buildffa_spectate_target_unavailable");
                continue;
            }
            if (!valid(target.getLocation())) continue;
            if (session.reattachNeeded || !staff.getWorld().equals(target.getWorld())) {
                begin(staff, target, session.id);
                session.reattachNeeded = false;
            }
            if (staff.getGameMode() != GameMode.SPECTATOR) staff.setGameMode(GameMode.SPECTATOR);
            try {
                if (staff.getSpectatorTarget() != target) staff.setSpectatorTarget(target);
            } catch (RuntimeException ignored) {
                if (!staff.getWorld().equals(target.getWorld()) || staff.getLocation().distanceSquared(target.getLocation()) > 256.0D) {
                    staff.teleport(target.getLocation().clone());
                }
            }
        }
    }

    private boolean isAvailable(Player target) {
        if (target == null || !target.isOnline() || Bukkit.getPlayer(target.getUniqueId()) != target) return false;
        Gamer gamer = Gamer.getGamer(target);
        if (gamer == null || gamer.getPlayerId() <= 0 || target.isDead()) return false;
        try {
            return BuildFFAGamer.get(gamer) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean valid(Location location) {
        if (location == null || location.getWorld() == null) return false;
        World loaded = Bukkit.getWorld(location.getWorld().getUID());
        return loaded != null && loaded == location.getWorld();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handleDeparture(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        handleDeparture(event.getPlayer());
    }

    private void handleDeparture(Player player) {
        UUID playerId = player.getUniqueId();
        sessions.remove(playerId);
        for (UUID staffId : sessions.keySet().toArray(new UUID[0])) {
            SpectateSession session = sessions.get(staffId);
            if (session != null && session.targetId.equals(playerId)) end(staffId, "buildffa_spectate_target_disconnected");
        }
    }

    @EventHandler
    public void onDeath(GameDeathEvent event) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(game.getPlugin(), () -> onDeath(event));
            return;
        }
        handleDeath(event.getVictim().getUniqueId());
    }

    @EventHandler
    public void onVanillaDeath(PlayerDeathEvent event) {
        handleDeath(event.getEntity().getUniqueId());
    }

    private void handleDeath(UUID victimId) {
        if (sessions.containsKey(victimId)) end(victimId, "buildffa_spectate_ended");
        for (UUID staffId : sessions.keySet().toArray(new UUID[0])) {
            SpectateSession session = sessions.get(staffId);
            if (session != null && session.targetId.equals(victimId)) end(staffId, "buildffa_spectate_target_unavailable");
        }
    }

    @EventHandler
    public void onMapChange(GameMapChangeEvent event) {
        if (event.getGame() != game) return;
        transferringArena = true;
        for (Map.Entry<UUID, SpectateSession> entry : sessions.entrySet()) {
            entry.getValue().arenaTransferred = true;
            entry.getValue().reattachNeeded = true;
            Player staff = Bukkit.getPlayer(entry.getKey());
            if (staff != null) {
                game.invalidateInventoryRequest(staff);
                try {
                    staff.setSpectatorTarget(null);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    public void completeArenaTransfer() {
        transferringArena = false;
        validateSessions();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        for (Map.Entry<UUID, SpectateSession> entry : sessions.entrySet()) {
            Player staff = Bukkit.getPlayer(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getValue().targetId);
            if ((staff != null && staff.getWorld().equals(event.getWorld()))
                    || (target != null && target.getWorld().equals(event.getWorld()))) {
                entry.getValue().reattachNeeded = true;
                if (staff != null) try {
                    staff.setSpectatorTarget(null);
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isSpectating((Player) event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        if (!(event instanceof EntityDamageByEntityEvent)) return;
        org.bukkit.entity.Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
        if (damager instanceof Player && isSpectating((Player) damager)) event.setCancelled(true);
        if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player
                && isSpectating((Player) ((Projectile) damager).getShooter())) event.setCancelled(true);
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != game.getPlugin()) return;
        shuttingDown = true;
        validationTask.cancel();
        endAll();
    }

    private void endAll() {
        for (UUID staffId : sessions.keySet().toArray(new UUID[0])) end(staffId, "buildffa_spectate_ended");
    }

    private static final class SpectateSession {
        UUID id;
        UUID targetId;
        String targetName;
        boolean arenaTransferred;
        boolean reattachNeeded;
        final Location returnLocation;
        final GameMode gameMode;
        final boolean allowFlight;
        final boolean flying;

        SpectateSession(UUID id, UUID targetId, Location returnLocation, GameMode gameMode, boolean allowFlight, boolean flying) {
            this.id = id;
            this.targetId = targetId;
            this.returnLocation = returnLocation;
            this.gameMode = gameMode;
            this.allowFlight = allowFlight;
            this.flying = flying;
        }
    }
}
