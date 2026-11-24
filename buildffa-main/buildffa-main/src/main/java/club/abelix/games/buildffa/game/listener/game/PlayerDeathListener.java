package club.abelix.games.buildffa.game.listener.game;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.config.WorldConfig;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.event.GameDeathEvent;
import club.abelix.games.buildffa.game.manager.player.RewardManager;
import club.abelix.network.type.mode.Uncommon;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import club.abelix.games.buildffa.game.kit.BaseKit;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PlayerDeathListener extends YListener implements Listener {
    Cache<UUID, UUID> damageCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.of(15, ChronoUnit.SECONDS))
            .build();
    Set<UUID> processingDeaths = new HashSet<>();
    Set<UUID> respawningPlayers;
    Map<UUID, UUID> lifecycleTokens = new HashMap<>();
    Map<UUID, BukkitTask> deathTasks = new HashMap<>();
    BuildFFAGame game;

    public PlayerDeathListener(final @NotNull BuildFFAGame game) {
        super(game.getPlugin());

        this.game = game;
        this.respawningPlayers = game.getRespawningPlayers();
        game.getPlugin().getServer().getOnlinePlayers().forEach(player ->
                lifecycleTokens.put(player.getUniqueId(), UUID.randomUUID()));
        ProtocolLibrary.getProtocolManager().addPacketListener(
                new PacketAdapter(game.getPlugin(), ListenerPriority.HIGHEST, PacketType.Play.Server.LOGIN) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        if (game.getWorldConfig() == null || event.getPlayer() == null
                                || !event.getPlayer().getWorld().equals(game.getWorldConfig().world())) return;
                        event.setPacket(event.getPacket().shallowClone());
                        event.getPacket().getBooleans().write(0, false);
                    }
                });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        clearLifecycle(event.getPlayer().getUniqueId());
        lifecycleTokens.put(event.getPlayer().getUniqueId(), UUID.randomUUID());
    }

    private void cancelDeathTask(UUID playerId) {
        BukkitTask task = deathTasks.remove(playerId);
        if (task != null) task.cancel();
    }

    private void clearLifecycle(UUID playerId) {
        cancelDeathTask(playerId);
        lifecycleTokens.remove(playerId);
        processingDeaths.remove(playerId);
        respawningPlayers.remove(playerId);
        clearAttribution(playerId);
    }

    @EventHandler
    public void onPlayerDeath(final @NotNull EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        if (game.isFrozen(victim) || game.isSpectating(victim)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        handleKill(victim, true);
    }

    @EventHandler
    public void onPlayerMove(final @NotNull PlayerMoveEvent event) {
        val player = event.getPlayer();
        if (game.isFrozen(player) || game.isSpectating(player) || event.getTo() == null) return;

        val from = event.getFrom();
        val to = event.getTo();

        if (game.isLobbyZone(to) != game.isLobbyZone(from)) clearAttribution(player.getUniqueId());
        if (from.getBlockY() == to.getBlockY()) return;
        if (to.getBlockY() > game.getWorldConfig().deathLevel()) return;

        handleKill(player, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(final @NotNull EntityDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) return;
        if (respawningPlayers.contains(victim.getUniqueId()) || victim.isDead()) {
            event.setCancelled(true);
            return;
        }
        if (game.isSpectating(victim) || game.isLobbyZone(victim.getLocation())
                || event instanceof EntityDamageByEntityEvent && game.isBuildMode(victim)) {
            event.setCancelled(true);
            return;
        }
        if (event instanceof EntityDamageByEntityEvent
                && !game.getCpsCheckManager().validateMelee((EntityDamageByEntityEvent) event)) return;
        if (victim.getHealth() - event.getFinalDamage() > 0) return;
        if (event instanceof EntityDamageByEntityEvent) {
            recordAttack((EntityDamageByEntityEvent) event);
            game.getCpsCheckManager().recordSuccessfulMelee((EntityDamageByEntityEvent) event);
        }

        event.setCancelled(true);
        handleKill(victim, event.getCause() != EntityDamageEvent.DamageCause.CUSTOM);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        try {
            Location quitLocation = player.getLocation().clone();
            UUID attackerId = damageCache.getIfPresent(playerId);
            Player attacker = attackerId == null ? null : Bukkit.getPlayer(attackerId);
            if (attacker != null && attacker.isOnline() && !playerId.equals(attackerId)
                    && game.getEventManager().isArenaParticipant(attacker)
                    && game.getEventManager().isArenaParticipant(player))
                handleKill(player, true, false, quitLocation);
        } finally {
            clearLifecycle(playerId);
            game.invalidateInventoryRequest(player);
            game.resetPlayerKitApplication(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombatTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())
                || game.isLobbyZone(event.getTo()) || game.isLobbyZone(event.getFrom()))
            clearAttribution(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCombatWorldChange(PlayerChangedWorldEvent event) {
        clearAttribution(event.getPlayer().getUniqueId());
    }

    private void clearAttribution(UUID playerId) {
        damageCache.invalidate(playerId);
        damageCache.asMap().values().removeIf(playerId::equals);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHit(final @NotNull EntityDamageByEntityEvent event) {
        recordAttack(event);
    }

    private Player resolveAttacker(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Projectile && ((Projectile) entity).getShooter() instanceof Player)
            return (Player) ((Projectile) entity).getShooter();
        return null;
    }

    private boolean validCombatPair(Player attacker, Player victim) {
        return attacker != null && victim != null && !attacker.getUniqueId().equals(victim.getUniqueId())
                && !processingDeaths.contains(attacker.getUniqueId()) && !processingDeaths.contains(victim.getUniqueId())
                && !respawningPlayers.contains(attacker.getUniqueId()) && !respawningPlayers.contains(victim.getUniqueId())
                && game.getEventManager().isArenaParticipant(attacker) && game.getEventManager().isArenaParticipant(victim);
    }

    private boolean recordAttribution(Player attacker, Player victim) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Combat attribution must run on the server thread");
        if (!validCombatPair(attacker, victim)) return false;
        damageCache.put(victim.getUniqueId(), attacker.getUniqueId());
        return true;
    }

    private void recordAttack(EntityDamageByEntityEvent event) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Combat hits must run on the server thread");
        if (event.isCancelled() || !(event.getEntity() instanceof Player victim)) return;
        if (!game.getCpsCheckManager().validateMelee(event)) return;
        double damage = event.getFinalDamage();
        if (!Double.isFinite(damage) || damage < 0) return;
        Player attacker = resolveAttacker(event.getDamager());
        if (!recordAttribution(attacker, victim)) return;
        if (damage > 0) game.getEventManager().recordDamage(attacker, victim, damage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void protectFishingPull(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY || !(event.getCaught() instanceof Player)) return;
        if (!Bukkit.isPrimaryThread()) {
            event.setCancelled(true);
            return;
        }
        Player attacker = resolveAttacker(event.getHook());
        if (attacker == null || !attacker.getUniqueId().equals(event.getPlayer().getUniqueId())
                || !validCombatPair(attacker, (Player) event.getCaught())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishingPull(PlayerFishEvent event) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Fishing attribution must run on the server thread");
        if (event.isCancelled() || event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY
                || !(event.getCaught() instanceof Player)) return;
        Player attacker = resolveAttacker(event.getHook());
        if (attacker == null || !attacker.getUniqueId().equals(event.getPlayer().getUniqueId())) return;
        recordAttribution(attacker, (Player) event.getCaught());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLobbyHit(final @NotNull EntityDamageEvent event) {
        if (!game.isLobbyZone(event.getEntity().getLocation())) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVanillaDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (game.getFfaConfig().getGameWorlds().stream().noneMatch(config -> config.world().equals(victim.getWorld()))) return;
        UUID playerId = victim.getUniqueId();
        boolean firstDeath = respawningPlayers.add(playerId);
        game.invalidateInventoryRequest(victim);
        game.resetPlayerKitApplication(victim);
        try {
            if (firstDeath) handleKill(victim, true, false, victim.getLocation().clone());
        } finally {
            cancelDeathTask(playerId);
            UUID token = lifecycleTokens.computeIfAbsent(playerId, ignored -> UUID.randomUUID());
            deathTasks.put(playerId, Bukkit.getScheduler().runTaskLater(game.getPlugin(), () -> {
                if (!token.equals(lifecycleTokens.get(playerId))) return;
                deathTasks.remove(playerId);
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline() && player.isDead()) player.spigot().respawn();
            }, 1L));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVanillaDeathDrops(PlayerDeathEvent event) {
        if (game.getFfaConfig().getGameWorlds().stream()
                .anyMatch(config -> config.world().equals(event.getEntity().getWorld())))
            event.getDrops().removeIf(BaseKit::isProtectedKitItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!respawningPlayers.contains(playerId)) return;
        WorldConfig respawnArena = game.getWorldConfig();
        event.setRespawnLocation(game.resolveSpawnLocation());
        cancelDeathTask(playerId);
        UUID token = lifecycleTokens.get(playerId);
        deathTasks.put(playerId, Bukkit.getScheduler().runTaskLater(game.getPlugin(), () -> {
            if (token == null || !token.equals(lifecycleTokens.get(playerId))) return;
            deathTasks.remove(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || player.isDead()) return;
            try {
                if (respawnArena != game.getWorldConfig() || !player.getWorld().equals(game.getWorldConfig().world()))
                    game.teleportToSpawn(player);
                player.setFireTicks(0);
                player.setFallDistance(0);
                player.setFoodLevel(20);
                player.setSaturation(5.0F);
                player.setVelocity(new org.bukkit.util.Vector());
                for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects())
                    player.removePotionEffect(effect.getType());
                respawningPlayers.remove(playerId);
                game.completePlayerReset(player);
                if (!game.isLobbyZone(player.getLocation())) game.applySelectedKit(Gamer.getGamer(player));
            } finally {
                respawningPlayers.remove(playerId);
                processingDeaths.remove(playerId);
            }
        }, 1L));
    }

    private void handleKill(final @NotNull Player victim, boolean allowKillEffect) {
        handleKill(victim, allowKillEffect, true, victim.getLocation().clone());
    }

    private void handleKill(final @NotNull Player victim, boolean allowKillEffect, boolean resetVictim, Location deathLocation) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Deaths must be processed on the server thread");
        if (game.isFrozen(victim) || game.isSpectating(victim)) return;
        UUID victimId = victim.getUniqueId();
        if (!processingDeaths.add(victimId)) return;
        UUID killerId = damageCache.getIfPresent(victimId);
        if (victimId.equals(killerId)) killerId = null;
        Player killer = killerId == null ? null : Bukkit.getPlayer(killerId);
        boolean attributedPvPDeath = allowKillEffect && killer != null && killer.isOnline()
                && game.getEventManager().isArenaParticipant(killer) && game.getEventManager().isDeathParticipant(victim);
        if (!attributedPvPDeath) killer = null;
        boolean progressionEligible = attributedPvPDeath;

        try {
            if (progressionEligible) {
                RewardManager.KillReward reward = game.getRewardManager().calculateReward(killer, victim);
                handleKillerActions(killer);
                Gamer killerGamer = Gamer.getGamer(killer);
                killerGamer.addCoins(Uncommon.BUILDFFA, reward.getFinalCoinsAwarded());

                HeartBeat.callEvent(new GameDeathEvent(
                        killer,
                        victim,
                        reward.getBaseCoins(),
                        reward.getBaseKillRating(),
                        reward.getStreakBonusRating(),
                        reward.getFinalCoinsAwarded(),
                        reward.getFinalRatingAwarded(),
                        reward.getVictimRatingLost(),
                        reward.getEventBonusCoins(),
                        reward.getEventBonusRating(),
                        deathLocation,
                        true
                ));

                int streak = BuildFFAGamer.get(killer).getCurrentKillStreak();
                game.getQuestManager().handleCoinsEarned(killer, reward.getFinalCoinsAwarded());
                game.getKillEffectManager().playSelectedKillEffect(killer, deathLocation);
                game.getKillSoundManager().playSelectedKillSound(killer);
                game.getQuestManager().handleKill(killer, streak);
                game.getBountyManager().claimBounty(killer, victim);
                game.getBountyManager().updateBounty(killer, streak);
            } else {
                HeartBeat.callEvent(new GameDeathEvent(
                        killer,
                        victim,
                        -1,
                        -2,
                        -3,
                        -1,
                        -2,
                        -3,
                        -1,
                        -2,
                        deathLocation,
                        false
                ));
            }
        } finally {
            damageCache.invalidate(victimId);
            try {
                game.getBountyManager().clear(victim);
            } finally {
                try {
                    handleVictimActions(victim, resetVictim);
                } finally {
                    try {
                        BuildFFAGamer.get(victim).resetKillStreak();
                    } finally {
                        if (!respawningPlayers.contains(victimId)) {
                            cancelDeathTask(victimId);
                            UUID token = lifecycleTokens.computeIfAbsent(victimId, ignored -> UUID.randomUUID());
                            deathTasks.put(victimId, Bukkit.getScheduler().runTaskLater(game.getPlugin(), () -> {
                                if (!token.equals(lifecycleTokens.get(victimId))) return;
                                deathTasks.remove(victimId);
                                processingDeaths.remove(victimId);
                            }, 1L));
                        }
                    }
                }
            }
        }
    }

    private void handleKillerActions(Player killer) {
        val killerGamer = Gamer.getGamer(killer);

        killer.setHealth(killer.getMaxHealth());

        killerGamer.addStat("kills", 1);
    }

    private void handleVictimActions(final @NotNull Player victim, boolean resetVictim) {
        val victimGamer = Gamer.getGamer(victim);

        victimGamer.addStat("deaths", 1);

        if (resetVictim && victim.isOnline()) game.resetPlayer(victim);
    }

}
