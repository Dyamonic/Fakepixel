package club.abelix.api.server.game.listener.game;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerGameModeChangeEvent;
import club.abelix.api.base.gamer.event.GamerVanishEvent;
import club.abelix.api.nms.sound.SoundType;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.*;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.enums.GameState;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("UnstableApiUsage")
public class GameDeathListener extends GameListener<Game> {

    /**
     * Последний игрок, который ударил другого
     * <p>
     * Максимальный срок, когда он еще будет убийцей задается в конструкторе или не задается вообще, тогда он будет 20
     * секунд по умолчанию
     */
    private final Cache<Player, DamageEntry> playerDamageHistoryCache;

    public GameDeathListener(Game game) {
        this(game, 10, TimeUnit.SECONDS);
    }

    public GameDeathListener(Game game, int interval, TimeUnit unit) {
        super(game);

        this.playerDamageHistoryCache = Caffeine.newBuilder()
                .expireAfterWrite(interval, unit)
                .build();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        checkGame(event, () -> {
            val entity = event.getEntity();

            if (!(entity instanceof Player)) {
                return;
            }

            val gamer = Gamer.getGamer((Player) entity);

            if (gamer.isSpectator()) {
                return;
            }

            val damager = event.getDamager();

            if (damager instanceof Player) {
                val damagerGamer = Gamer.getGamer(((Player) damager));

                if (damagerGamer.isSpectator()) {
                    return;
                }
            }

            val player = (Player) event.getEntity();
            Player killer = null;

            //проверяем, если его атаковал проджектайл (т.е стрела, снежок, итд)
            //если нет, то проверяем, если он был дамагнут ЧЬИМ ТО энтити
            if (damager instanceof TNTPrimed && ((TNTPrimed) damager).getSource() instanceof Player) {
                killer = (Player) ((TNTPrimed) damager).getSource();
            } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
                killer = (Player) ((Projectile) damager).getShooter();
            } else if (!(damager instanceof Player) && game.getMetadata().has(damager, "owner")) {
                killer = game.getMetadata().get(damager, "owner");
            } else if (damager instanceof Player) {
                killer = (Player) damager;
            }

            // атаковал не игрок, досвидули
            // либо просто игрок уже вышел, он тоже идет нахуй
            // либо он сам себе нанес урон
            if (killer == null || !killer.isOnline() || killer.equals(player)) {
                return;
            }

            playerDamageHistoryCache.cleanUp();
            playerDamageHistoryCache.put(player, new DamageEntry(player, killer,
                    event.getCause(), System.currentTimeMillis()));
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(GameQuitEvent e) {
        checkGame(e, () -> {
            val gamer = e.getGamer();
            val player = gamer.getPlayer();

            Runnable cleanCache = () -> {
                //подчищаем кеш
                playerDamageHistoryCache.cleanUp();
                playerDamageHistoryCache.invalidate(player);

                val damageHistoryMap = playerDamageHistoryCache.asMap();

                for (Map.Entry<Player, DamageEntry> damageCache : damageHistoryMap.entrySet()) {
                    val damager = damageCache.getValue().damager;

                    if (damager != null && damager.equals(player)) {
                        playerDamageHistoryCache.invalidate(damageCache.getKey());
                    }
                }
            };

            val teamManager = game.getTeamManager();

            if (teamManager != null) {
                val gameTeam = teamManager.getTeam(gamer);

                if (gameTeam == null || !gameTeam.isAlive(gamer)) {
                    cleanCache.run();
                    return;
                }
            }

            if (game.getGameState() == GameState.GAME) {
                processDeath(gamer, cleanCache, true);
            }
        });
    }

    @EventHandler
    public void onGameModeChange(final @NonNull GamerGameModeChangeEvent event) {
        val gamer = event.getTarget();

        if (gamer.hasPermission("gamemode.game.bypass")) {
            return;
        }

        val player = gamer.getPlayer();

        val entry = playerDamageHistoryCache.getIfPresent(player);
        val damagerEntry = findByDamager(player);

        if ((event.getNewGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.SPECTATOR)
                && (checkFightStatus(entry) || checkFightStatus(damagerEntry))) {
            gamer.sendLocalizedMessage("gamemode_unavailable_timer");
            event.setCancelled(true);
        }
    }

    private boolean checkFightStatus(DamageEntry entry) {
        return entry != null && System.currentTimeMillis() - entry.time
                < TimeUnit.SECONDS.toMillis(10);
    }

    private long getFightTime(DamageEntry entry) {
        return entry != null ? System.currentTimeMillis() - entry.time : 0;
    }

    private DamageEntry findByDamager(@NonNull Entity damager) {
        for (Map.Entry<Player, DamageEntry> damageCache : playerDamageHistoryCache.asMap().entrySet()) {
            DamageEntry entry = damageCache.getValue();

            if (entry.damager == damager) {
                return entry;
            }
        }

        return null;
    }

    @EventHandler
    public void onDeath(final @NonNull PlayerDeathEvent event) {
        checkGame(event, () -> {
            event.setDeathMessage(null);

            val player = event.getEntity();
            val gamer = Gamer.getGamer(player);

            if (gamer.isSpectator()) {
                return;
            }

            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            player.setItemOnCursor(null);

            GameDeathEvent gameDeathEvent = processDeath(gamer, () -> {
                player.setVelocity(new Vector(0, 0, 0));
                player.setHealth(player.getMaxHealth());
                player.setFallDistance(0.0f);
                player.setFireTicks(0);
            }, false);

            if (!gameDeathEvent.isAllowItemDrops()) {
                event.getDrops().clear();
            }

            if (!gameDeathEvent.isAllowLevelDrops()) {
                event.setDroppedExp(0);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(final @NonNull PlayerMoveEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ENABLE_DIE_ON_VOID_FALL, true)) {
                return;
            }

            if (event.getTo().getY() > game.getSetting(GameSetting.VOID_FALL_MINIMUM_Y, 0)) {
                return;
            }

            val player = event.getPlayer();
            val gamer = Gamer.getGamer(player);

            if (gamer.isSpectator()) {
                return;
            }

            playerDamageHistoryCache.cleanUp();

            val entry = playerDamageHistoryCache.getIfPresent(player);

            val damager = entry == null ? null : entry.damager;

            playerDamageHistoryCache.put(player, new DamageEntry(player, damager,
                    EntityDamageEvent.DamageCause.VOID, System.currentTimeMillis()));

            player.setItemOnCursor(null);
            player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));

            //собственно производим все действия
            processDeath(gamer, () -> {
                player.setFallDistance(0);
                player.closeInventory();
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.setFoodLevel(20);
                player.setHealth(player.getMaxHealth());
            }, false);
        });
    }

    // выполняет ПОХОРОННУЮ ЦЕРЕМОНИЮ: определяет киллера, кидает евенты
    @SneakyThrows
    @SuppressWarnings("UnstableApiUsage")
    private GameDeathEvent processDeath(Gamer gamer, Runnable death, boolean playerLeft) {
        Player player = gamer.getPlayer();
        Location killLocation = player.getLocation().clone();

        //подчищаем кэш от ненужных записей
        playerDamageHistoryCache.cleanUp();

        PluginManager pluginManager = game.getPlugin().getServer().getPluginManager();

        //вызываем событие смерти игрока, именно в НЕМ плагины могут выставить,
        //например, выпадение дропа или EXP, сохранение инвентаря и прочую дичь
        GameDeathEvent gameDeathEvent = new GameDeathEvent(game, gamer);
        HeartBeat.callEvent(gameDeathEvent);

        //последний дамаг данного игрока
        DamageEntry entry = playerDamageHistoryCache.get(player, (player1) -> new DamageEntry(player));

        Player killer = entry.getDamager();

        //каким-то раком игрок убил САМ СЕБЯ, такого не должно быть
        if (player == killer) {
            killer = null;
        } else if (killer != null) {
            FalconAPI.getNmsApi().getPlayerApi().setKiller(player, killer);

            playerDamageHistoryCache.invalidate(player);
        }

        Gamer killerGamer = null;

        if (killer != null) {
            killerGamer = Gamer.getGamer(killer);
        }

        EntityDamageEvent.DamageCause deathCause = entry.getCause();

        GameKillMessageEvent killMessageEvent;

        String localizationKey = "game_death_cause_" + deathCause.name().toLowerCase();

        boolean by = false;
        if (killer != null) {
            by = true;
            localizationKey += "_by";
        }

        if (playerLeft) {
            killMessageEvent = new GameKillMessageEvent(
                    game, gamer, killerGamer, deathCause,
                    "game_player_left", true, false
            );
        } else {
            killMessageEvent = new GameKillMessageEvent(
                    game, gamer, killerGamer, deathCause,
                    localizationKey, false, by
            );
        }

        pluginManager.callEvent(killMessageEvent);

        killMessageEvent.setMessageParameter("%player%", lang -> player.getDisplayName());

        String displayName = killer == null ? "§4N/A" : killer.getDisplayName();
        killMessageEvent.setMessageParameter("%killer%", lang -> displayName);

        if (!killMessageEvent.isCancelled()) {
            killMessageEvent.broadcast();
        }

        GameKillEvent gameKillEvent = new GameKillEvent(game, gamer, killerGamer, deathCause, playerLeft, killLocation);
        HeartBeat.callEvent(gameKillEvent);

        death.run();

        if (killerGamer != null) {
            FalconAPI.getNmsApi().getSoundApi().playSound(killer.getPlayer(), SoundType.NOTIFICATION);
        }

        player.setLastDamageCause(null);

        return gameDeathEvent;
    }

    @EventHandler
    public void onVanish(GamerVanishEvent event) {
        checkGame(event, () -> {
            if (!event.isVanished()) {
                return;
            }

            Gamer gamer = event.getGamer();

            GameKillMessageEvent killMsg = new GameKillMessageEvent(game, gamer, null, null, "game_player_left", true, false);
            HeartBeat.callEvent(killMsg);

            if (!killMsg.isCancelled()) {
                killMsg.broadcast();
            }
        });
    }

    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    public static class DamageEntry {

        Player player;
        Entity damager;
        EntityDamageEvent.DamageCause cause;
        long time;

        public DamageEntry(@NonNull Player player) {
            this(player, null, null, System.currentTimeMillis());
        }

        public Player getDamager() {
            return isDamagedByPlayer() ? (Player) damager : null;
        }

        public boolean isDamagedByPlayer() {
            return damager instanceof Player;
        }

        public EntityDamageEvent.DamageCause getCause() {
            EntityDamageEvent.DamageCause cause = this.cause;

            if (cause == null) {
                val event = player.getLastDamageCause();

                cause = event == null ? EntityDamageEvent.DamageCause.CUSTOM : event.getCause();
            }

            if (cause == EntityDamageEvent.DamageCause.FIRE_TICK) {
                cause = EntityDamageEvent.DamageCause.FIRE;
            }

            if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                cause = EntityDamageEvent.DamageCause.BLOCK_EXPLOSION;
            }

            return cause;
        }
    }

}

