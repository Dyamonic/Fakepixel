package club.abelix.api.server.game.listener.game;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.event.skin.SkinChangeEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.gamer.event.GamerVanishEvent;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.nms.entity.custom.fireball.NmsFireball;
import club.abelix.api.nms.player.PlayerApi;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.event.GameJoinEvent;
import club.abelix.api.server.game.event.GameQuitEvent;
import club.abelix.api.server.game.listener.GameListener;
import club.abelix.api.server.game.manager.GameStatsManager;
import club.abelix.api.server.game.manager.TeamManager;
import club.abelix.api.server.game.setting.GameSetting;
import club.abelix.api.server.game.stats.GameStatsCache;
import club.abelix.api.server.game.team.GameTeam;
import club.abelix.api.server.game.team.color.TeamColor;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Cooldown;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.common.enums.GameState;
import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class DefaultGameListener extends GameListener<Game> {

    // Листенер слушает евенты и отменяет их в зависимости от установленных настроек

    /**
     * Предмет лазурита для стола зачарования, если вдруг включена настройка "ENABLE_SIMPLE_ENCHANTMENT_TABLE"
     */
    private static final ItemStack LAPIS_ITEM = new ItemStack(Material.INK_SACK, 64, (short) 4);
    private final Map<Player, Cooldown> fireballCooldown = new HashMap<>();
    private final Map<GameTeam, Inventory> chests = new HashMap<>();
    private List<String> goodPlayers = new ArrayList<>();

    public DefaultGameListener(@NonNull Game game) {
        super(game);
    }

    private static void playExplosion(@NonNull Block block) {
        float x = -1.0f + (float) (Math.random() * 3.0);
        float y = -2.0f + (float) (Math.random() * 4.0);
        float z = -1.5f + (float) (Math.random() * 2.0);

        Location location = block.getLocation();

        FallingBlock fallingBlock = location.getWorld().spawnFallingBlock(location, block.getType(), block.getData());
        fallingBlock.setVelocity(new Vector(x, y, z));
        fallingBlock.setDropItem(false);

        // block.setType(Material.AIR);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ALLOW_PLAYER_DAMAGE, true)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onArrowShootFix(@NonNull final EntityShootBowEvent event) {
        checkGame(event, () -> {
            if (!(event.getEntity() instanceof Player) || !(event.getProjectile() instanceof Arrow old)) return;

            final ProjectileSource source = old.getShooter();
            if (source == null) return;

            old.remove();

            final Arrow arrow = source.launchProjectile(Arrow.class);
            arrow.setBounce(false);
            arrow.setShooter(source);
            arrow.setVelocity(old.getVelocity());
            arrow.setKnockbackStrength(old.getKnockbackStrength());
            arrow.setFireTicks(old.getFireTicks());
            arrow.setCritical(old.isCritical());

            PlayerApi playerApi = FalconAPI.getNmsApi().getPlayerApi();
            playerApi.setArrowDamage(arrow, playerApi.getArrowDamage(old));
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGamerJoin(GameJoinEvent event) {
        checkGame(event, () -> {
            Gamer gamer = event.getGamer();

            GameStatsManager statsManager = game.getStatsManager();

            if (statsManager != null) {
                GameStatsCache cache = game.getStatsManager().getStatsCache(gamer);

                if (cache != null) {
                    cache.rejoin(gamer);
                }
            }

            TeamManager teamManager = game.getTeamManager();

            if (teamManager != null) {
                GameTeam team = teamManager.getTeam(gamer);

                if (team != null) {
                    team.rejoin(gamer);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGamerQuit(final @NonNull GameQuitEvent event) {
        checkGame(event, () -> {
            if (event.isChange()) {
                return;
            }

            val game = event.getGame();

            if (game == null) {
                return;
            }

            val gamer = event.getGamer();
            val statsManager = game.getStatsManager();

            if (statsManager != null) {
                statsManager.save(gamer);
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onTNTPlace(BlockPlaceEvent event) {
        checkGame(event, () -> {
            Block block = event.getBlock();

            if (!game.checkPlayableZone(block) || game.isProtectionLocation(block.getLocation())) {
                return;
            }

            if (game.getSetting(GameSetting.ENABLE_AUTO_TNT_IGNITION, true) && block.getType() == Material.TNT) {
                BlockState replaced = event.getBlockReplacedState();
                block.setTypeIdAndData(replaced.getTypeId(), replaced.getRawData(), true);

                TNTPrimed primed = block.getWorld().spawn(LocationUtil.getBlockCenter(block), TNTPrimed.class);
                primed.setFuseTicks(50);
                primed.setYield(6);
                primed.setIsIncendiary(false);

                EntitySpawnEvent e = new EntitySpawnEvent(primed);
                HeartBeat.callEvent(e);

                if (e.isCancelled()) {
                    primed.remove();
                }

                game.getMetadata().set(primed, "owner", event.getPlayer());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        fireballCooldown.remove(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteractWithFireball(PlayerInteractEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ENABLE_FIREBALLS, false)) {
                return;
            }

            Player player = event.getPlayer();
            ItemStack item = event.getItem();

            if (item == null || item.getType() != Material.FIREBALL) {
                return;
            }
            Gamer gamer = Gamer.getGamer(player);

            if (gamer.isSpectator()) {
                return;
            }

            if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            event.setCancelled(true);

            Cooldown cooldown = fireballCooldown.get(player);
            if (cooldown != null && cooldown.isActive()) {
                return;
            }

            Location eyeLocation = player.getEyeLocation().clone();
            Location spawnLocation = eyeLocation.add(eyeLocation.getDirection().clone().multiply(1.23));

            val speed = game.getSetting(GameSetting.FIREBALLS_SPEED, 1.0);

            NmsFireball nmsFireball = FalconAPI.getNmsApi().getWorldApi().launchFireball(
                    player, spawnLocation, eyeLocation.clone().getDirection().multiply(speed)
            );

            Fireball fireball = nmsFireball.asBukkitEntity();

            fireball.setShooter(player);
            fireball.setYield(game.getSetting(GameSetting.FIREBALLS_YIElD, 1.8f));
            fireball.setIsIncendiary(false);
            fireball.setFireTicks(0);
            nmsFireball.setSize(2.5f, 2.5f);

            game.getMetadata().set(fireball, "owner", player);

            int cooldownTime = game.getSetting(GameSetting.FIREBALLS_COOLDOWN, 1);
            fireballCooldown.put(player,
                    new Cooldown(TimeUtil.getNow(), TimeUtil.getNow() + cooldownTime)
            );

            int i = item.getAmount();

            if (i > 1) {
                item.setAmount(i - 1);
                player.updateInventory();
            } else {
                player.setItemInHand(null);
            }

            player.updateInventory();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFireballExplosion(EntityExplodeEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.FIREBALLS_CUSTOM_EXPLODE, false)) return;

            val type = event.getEntityType();
            if (type != EntityType.FIREBALL) return;

            val location = event.getLocation();

            val radius = game.getSetting(GameSetting.FIREBALLS_CUSTOM_EXPLODE_SEARCH_RADIUS, 8d);
            val heightForce = game.getSetting(GameSetting.FIREBALLS_CUSTOM_EXPLODE_HEIGHT, 1.15d);
            val radiusForce = game.getSetting(GameSetting.FIREBALLS_CUSTOM_EXPLODE_RADIUS, 1.15d);
            val multiplierBonus = game.getSetting(GameSetting.FIREBALLS_CUSTOM_EXPLODE_DISTANCE_MULTIPLIER_BONUS, 0.275d);
            val distanceDecreasementMultiplier = game.getSetting(GameSetting.FIREBALLS_CUSTOM_EXPLODE_DISTANCE_DECREASEMENT_MULTIPLIER, 1d);

            val damage = game.getSetting(GameSetting.FIREBALLS_DAMAGE, 1d);

            var distance = (event.getYield() * 16.0f) * 1;

            val nearbyEntities = location.getWorld()
                    .getNearbyEntities(location, radius, radius, radius)
                    .stream().filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity)
                    .toList();

            for (val player : nearbyEntities) {
                pushAway(player, location, heightForce, radiusForce, damage, distance, radius, multiplierBonus, distanceDecreasementMultiplier,
                        player == game.getMetadata().get(event.getEntity(), "owner"),
                        game.getSetting(GameSetting.FIREBALLS_ENABLE_DAMAGE, true)
                );

                // будь оно все нахуй проклято, особенно NMS
                EntityDamageByEntityEvent damageByEntityEvent = new EntityDamageByEntityEvent(
                        ((Player) ((Fireball) event.getEntity()).getShooter()),
                        player,
                        EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
                        damage
                );

                HeartBeat.callEvent(damageByEntityEvent);
            }

        });
    }

    @EventHandler
    public void onOwnerHitFireball(EntityDamageByEntityEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.FIREBALLS_CUSTOM_EXPLODE, false)) return;

            if (event.getDamager() instanceof Player player && event.getEntity() instanceof Fireball fireball) {
                if (player.equals(game.getMetadata().get(fireball, "owner"))) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFireballRadius(EntityDamageByEntityEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.FIREBALLS_CUSTOM_EXPLODE, false)) return;

            if (event.getEntity() instanceof Player && event.getDamager() instanceof Fireball) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTntRadius(EntityDamageByBlockEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.TNT_CUSTOM_EXPLODE, false)) return;

            if (event.getEntity() instanceof Player player
                    && event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                val radius = player.getLocation().distance(event.getDamager().getLocation());

                if (radius > game.getSetting(GameSetting.TNT_CUSTOM_EXPLODE_SEARCH_RADIUS, 6d)) {
                    event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTntExplosion(EntityExplodeEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.TNT_CUSTOM_EXPLODE, false)) return;

            val type = event.getEntityType();
            if (type != EntityType.PRIMED_TNT) return;

            val location = event.getLocation();

            val radius = game.getSetting(GameSetting.TNT_CUSTOM_EXPLODE_SEARCH_RADIUS, 6d);
            val heightForce = game.getSetting(GameSetting.TNT_CUSTOM_EXPLODE_HEIGHT, 1.15d);
            val radiusForce = game.getSetting(GameSetting.TNT_CUSTOM_EXPLODE_RADIUS, 1.15d);
            val multiplierBonus = game.getSetting(GameSetting.TNT_CUSTOM_EXPLODE_DISTANCE_MULTIPLIER_BONUS, 0.275d);
            val distanceDecreasementMultiplier = game.getSetting(GameSetting.TNT_CUSTOM_EXPLODE_DISTANCE_DECREASEMENT_MULTIPLIER, 1d);

            val damage = game.getSetting(GameSetting.TNT_DAMAGE, 1d);

            var distance = (event.getYield() * 16.0f) * 1;

            val nearbyEntities = location.getWorld()
                    .getNearbyEntities(location, radius, radius, radius)
                    .stream().filter(entity -> entity instanceof Player)
                    .map(entity -> (Player) entity)
                    .toList();

            for (val player : nearbyEntities) {
                pushAway(player, location, heightForce, radiusForce, damage, distance, radius, multiplierBonus, distanceDecreasementMultiplier,
                        player == game.getMetadata().get(event.getEntity(), "owner"),
                        game.getSetting(GameSetting.TNT_ENABLE_DAMAGE, true)
                );
            }
        });
    }

    private static void pushAway(
            Player player,
            Location fireballLocation,
            double heightForce,
            double radiusForce,
            final double damage,
            final double distance,
            final double searchRadius,
            final double multiplierBonus,
            final double distanceDecreasementMultiplier,
            boolean isOwner,
            boolean needDamage
    ) {
        // отсылаем ивент, на всякий, если например
        // чел в наблюдателях, или еще какая-то дичь
        val damageEvent = new EntityDamageEvent(player,
                EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, distance - (player.getLocation()
                .distance(fireballLocation.clone().multiply(isOwner ? 1.055 : 1.0)) + damage));

        // HeartBeat.callEvent(damageEvent);
        if (!damageEvent.isCancelled() && needDamage) {
            player.damage(Math.min(damageEvent.getFinalDamage(), 1));
        }

        val playerLocation = player.getLocation();

        val multiplyY = Math.max(-4, Math.min(4, heightForce));
        val multiplyRadius = Math.max(-4, Math.min(4, -1 * radiusForce));

        val dist = playerLocation.distance(fireballLocation);
        val multiplier = Math.max(Math.min(1 - Math.min(dist / searchRadius * distanceDecreasementMultiplier, 1) + multiplierBonus, 1), 0);

        player.setVelocity(fireballLocation.toVector().subtract(playerLocation.toVector())
                .normalize()
                .multiply(multiplyRadius * multiplier)
                .setY(multiplyY * multiplier));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ENABLE_BEAUTIFUL_EXPLOSIONS, true)) {
                return;
            }

            for (Block block : event.blockList()) {
                if (block.getType() == Material.SNOW) {
                    block.setType(Material.SNOW_BLOCK);
                }

                playExplosion(block);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArrowDamageMessage(EntityDamageByEntityEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.SHOW_HP_ON_ARROW_HIT, false)) {
                return;
            }

            if (!(event.getDamager() instanceof Arrow)) {
                return;
            }

            Entity entity = event.getEntity();
            Entity damager = event.getDamager();

            if (!(entity instanceof Player) || !(damager instanceof Projectile)) {
                return;
            }

            ProjectileSource projectileSource = ((Projectile) damager).getShooter();

            if (!(projectileSource instanceof Player)) {
                return;
            }

            Player player = ((Player) entity);
            Player shooter = ((Player) projectileSource);

            int maxHealth = (int) player.getMaxHealth() / 2;
            double health = (player.getHealth() - event.getFinalDamage()) / 2;

            if (health > 0) {
                Gamer shooterGamer = Gamer.getGamer(shooter);

                String healthFormat = String.format("%.2f", health);

                int fullHealth = (int) health;
                int remainingHealth = maxHealth - fullHealth;

                String heartsString = Strings.repeat("§4❤", fullHealth);

                if (health % 1 != 0) {
                    heartsString += "§c❤";
                    remainingHealth--;
                }

                heartsString += Strings.repeat("§0❤", remainingHealth);

                shooterGamer.sendLocalizedActionBar("game_bow_health_actionbar",
                        "%player%", player.getDisplayName(), "%health%", heartsString);

                game.sendLocalizedMessage(shooterGamer, "game_bow_health_message",
                        "%player%", player.getDisplayName(), "%health%", healthFormat);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeamDamage(EntityDamageByEntityEvent event) {
        checkGame(event, () -> {
            if (game.getSetting(GameSetting.ALLOW_FRIENDLY_FIRE, false)) {
                return;
            }

            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            Entity entity = event.getDamager();

            Player damager = null;

            if (entity instanceof Player) {
                damager = ((Player) entity);
            } else if (game.getMetadata().has(entity, "owner")) {
                damager = game.getMetadata().get(entity, "owner");
            } else if (entity instanceof Projectile) {
                damager = ((Player) ((Projectile) entity).getShooter());
            }

            Player player = (Player) event.getEntity();

            if (damager == null || damager == player) {
                return;
            }

            GameTeam damagerGameTeam = game.getTeamManager().getTeam(damager);
            GameTeam playerGameTeam = game.getTeamManager().getTeam(player);

            if (damagerGameTeam == playerGameTeam) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onSkinChange(SkinChangeEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());
        game.sendLocalizedMessage(gamer, "game_skin_change_disabled");

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        checkGame(event, () -> {
            switch (game.getSetting(GameSetting.ALLOW_BLOCK_BREAK, 1)) {
                case 0: {
                    event.setCancelled(true);
                    break;
                }
                case 2: {
                    if (!game.getMetadata().has(event.getBlock(), "owner")) {
                        event.setCancelled(true);
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ALLOW_BLOCK_PLACE, true)) {
                event.setCancelled(true);
                return;
            }

            game.getMetadata().set(event.getBlock(), "owner", event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockLimitPlace(BlockPlaceEvent event) {
        checkGame(event, () -> {
            int maxY = game.getSetting(GameSetting.MAX_Y_HEIGHT, 255);
            int minY = game.getSetting(GameSetting.MIN_Y_HEIGHT, 0);

            Block block = event.getBlock();

            Gamer gamer = Gamer.getGamer(event.getPlayer());

            if (block.getY() > maxY) {
                event.setCancelled(true);
                game.sendLocalizedMessage(gamer, "game_max_height_limit_reached");
                return;
            }

            if (block.getY() < minY) {
                event.setCancelled(true);
                game.sendLocalizedMessage(gamer, "game_min_height_limit_reached");
            }
        });
    }

    /**
     * Командный чат по умолчанию
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatFormatEvent event) {
        checkGame(event, () -> {
            String[] s = {"gg", "good game", "пп", "nice game", "ng"};
            if (game.getGameState().equals(GameState.ENDING) &&
                    Arrays.stream(s).toList().contains(event.getMessage().toLowerCase()) &&
                    !goodPlayers.contains(event.getSender().getName().toLowerCase())) {
                Gamer receiver = event.getReceiver();
                receiver.addExperience(10);
                receiver.addCoins(5);
                receiver.sendLocalizedMessage("nice_game_success");
                goodPlayers.add(event.getSender().getName().toLowerCase());
                event.setMessage("gg");
                return;
            }

            //тим чат работает только если он включен в настройках
            if (!game.getSetting(GameSetting.ENABLE_DEFAULT_TEAM_CHAT, true)) {
                return;
            }

            Gamer receiver = event.getReceiver();

            Game receiverGame = receiver.getGame();
            if (game != receiverGame) {
                event.setCancelled(true);
                return;
            }


            // sender
            Gamer gamer = event.getSender();

            GameTeam gameTeam = game.getTeamManager().getTeam(gamer);
            if (gamer.isSpectator() || gameTeam == null) {
                return;
            }

            String message = event.getMessage();
            TeamColor color = gameTeam.getColor();

            Language language = receiver.getLanguage();

            //если это соло режим, то используем особый чат
            if (game.getSetting(GameSetting.PLAYERS_PER_TEAM, 1) < 2) {
                event.unsafeAppendFormat(receiver.getLanguage().getMessage("game_chat_format",
                        "%color%", (color == null
                                ? language.getMessage("game_solo_chat_player")
                                : color.getNameThey(language))));
                return;
            }
            if (message.startsWith("!")) {
                String prefix = language.getMessage("game_team_chat_global");

                if (color != null) {
                    prefix = color.getColor() + ChatColor.stripColor(prefix);
                }

                event.unsafeAppendFormat(receiver.getLanguage().getMessage("game_chat_format", "%color%", prefix));
                event.setMessage(message.substring(1));
            } else {
                if (!gameTeam.getAliveGamers().contains(event.getReceiver())) {
                    event.setCancelled(true);

                    return;
                }
                event.unsafeAppendFormat(receiver.getLanguage().getMessage("game_chat_format",
                        "%color%", (color == null ? language.getMessage("game_team_chat_local") : color.getNameThem(language))));
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onGroupChange(GamerDisplayNameUpdateEvent event) {
        checkGame(event, () -> {
            Gamer gamer = event.getGamer();
            Player player = gamer.getPlayer();

            GameTeam team = game.getTeamManager().getTeam(player);

            if (team == null) {
                return;
            }

            TeamColor color = team.getColor();

            if (color != null) {
                player.setDisplayName(color.getColor() + player.getName());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplodeDelay(final @NotNull EntityExplodeEvent event) {
        checkGame(event, () -> {
            val delay = game.getSetting(GameSetting.DELAY_TNT_BLOCK_BREAK, 5L);

            if (delay == 0) return;

            val blocksSaved = new HashSet<>(event.blockList());
            event.blockList().clear();

            HeartBeat.runLaterSync(plugin, () -> {
                blocksSaved.forEach(block -> block.setType(Material.AIR));
            }, delay);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeamBlockExplosion(EntityExplodeEvent event) {
        checkGame(event, () -> {
            if (game.getSetting(GameSetting.ENABLE_TEAM_BLOCK_EXPLOSIONS, true)) {
                return;
            }

            Entity entity = event.getEntity();
            Player owner = game.getMetadata().get(entity, "owner");

            if (owner == null) {
                return;
            }

            GameTeam ownerTeam = game.getTeamManager().getTeam(owner);

            int oldSize = event.blockList().size();

            event.blockList().removeIf(block -> {
                Player blockOwner = game.getMetadata().get(block, "owner");

                if (blockOwner == null) {
                    return false;
                }

                GameTeam blockTeam = game.getTeamManager().getTeam(blockOwner);

                return blockOwner != owner && ownerTeam == blockTeam;
            });

            int newSize = event.blockList().size();

            if (oldSize != newSize) {
                Gamer gamer = Gamer.getGamer(owner);
                gamer.sendLocalizedMessage("game_blocks_not_exploded");
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(ItemSpawnEvent event) {
        checkGame(event, () -> game.addGameEntity(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitySpawn(CreatureSpawnEvent event) {
        checkGame(event, () -> game.addGameEntity(event.getEntity()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ALLOW_ITEM_DROP, true)) {
                event.setCancelled(true);
            }
        });
    }

    // region Physics

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ALLOW_ITEM_PICKUP, true)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ALLOW_FOOD_CHANGE, true)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled() && !game.getSetting(GameSetting.ENABLE_WATER_LAVA_FLOW, true)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeavesDecayEvent(LeavesDecayEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        checkGame(event, () -> {
            if (game.getSetting(GameSetting.ENABLE_FLINT_AND_STEEL, true)
                    && event.getCause() == BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL) {

                return;
            }

            if (isPhysicsDisabled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGlassBottleRemove(PlayerItemConsumeEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ENABLE_AUTO_REMOVE_GLASS_BOTTLES, true)) {
                return;
            }

            if (event.getItem().getType() != Material.POTION) {
                return;
            }

            // переписать
            event.getPlayer().setItemInHand(null);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled()) {
                Block block = event.getBlock();

                if ((block.getType() == Material.WATER
                        || block.getType() == Material.STATIONARY_WATER
                        || block.getType() == Material.LAVA
                        || block.getType() == Material.STATIONARY_LAVA)
                        && game.getSetting(GameSetting.ENABLE_WATER_LAVA_FLOW, true)) {
                    return;
                }

                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled()) {
                event.setCancelled(true);
            }
        });
    }

    // endregion

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled()) {
                event.setCancelled(true);
            }
        });
    }

    private boolean isPhysicsDisabled() {
        return !game.getSetting(GameSetting.ALLOW_BLOCK_PHYSICS, true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ALLOW_MOB_SPAWN, true)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        checkGame(event, () -> {
            if (isPhysicsDisabled()) {
                event.setCancelled(true);
            }
        });
    }

    /**
     * Общий эндер-сундук для одной команды
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ENABLE_TEAM_SHARED_CHEST, false)) {
                return;
            }

            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            if (event.useInteractedBlock() == Event.Result.DENY) {
                return;
            }

            if (event.getClickedBlock().getType() == Material.ENDER_CHEST) {
                event.setCancelled(true);

                Player player = event.getPlayer();

                GameTeam gameTeam = game.getTeamManager().getTeam(player);

                if (gameTeam == null) {
                    return;
                }

                player.openInventory(chests.computeIfAbsent(gameTeam,
                        k -> game.getPlugin().getServer().createInventory(null, 27, Lang.of("game_team_chest").compile())));
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onGamerVanish(GamerVanishEvent event) {
        checkGame(event, () -> {
            if (!event.isVanished()) {
                return;
            }

            Gamer gamer = event.getGamer();

            game.sendLocalizedMessage(gamer, "game_vanish_killed");

            game.getSpectatorManager().addSpectator(gamer);

            GameTeam team = game.getTeamManager().getTeam(gamer);
            team.removePlayer(gamer);
        });
    }

    @EventHandler
    public void onPearlHitSound(ProjectileHitEvent event) {
        checkGame(event, () -> {
            if (!(event.getEntity() instanceof EnderPearl)) return;
            if (!(event.getEntity().getShooter() instanceof Player)) return;
            if (!game.getSetting(GameSetting.ENDER_PEARL_SOUND_EFFECT, true)) return;

            Gamer gamer = Gamer.getGamer((Player) event.getEntity().getShooter());
            gamer.playSound(YSound.ENDERMAN_TELEPORT);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantmentInventoryOpen(InventoryOpenEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ENABLE_SIMPLE_ENCHANTMENT_TABLE, true)) {
                return;
            }

            Inventory inv = event.getInventory();

            if (inv.getType() == InventoryType.ENCHANTING) {
                inv.setItem(1, LAPIS_ITEM);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantmentInventoryClose(InventoryCloseEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.ENABLE_SIMPLE_ENCHANTMENT_TABLE, true)) {
                return;
            }

            Inventory inv = event.getInventory();

            if (inv.getType() == InventoryType.ENCHANTING) {
                inv.setItem(1, null);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantmentInventoryClick(InventoryClickEvent e) {
        checkGame(e, () -> {
            if (!game.getSetting(GameSetting.ENABLE_SIMPLE_ENCHANTMENT_TABLE, true)) {
                return;
            }

            if (e.getClickedInventory() == null) {
                return;
            }

            if (e.getClickedInventory().getType() == InventoryType.ENCHANTING && e.getSlot() == 1) {
                e.setCancelled(true);
            }
        });
    }

    /**
     * Погода
     */
    @EventHandler(ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        checkGame(event.getWorld().getSpawnLocation(), () -> {
            if (!game.getSetting(GameSetting.ALLOW_WEATHER_CHANGE, false)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnDamage(final EntityDamageByEntityEvent event) {
        checkGame(event, () -> {
            if (!game.getSetting(GameSetting.RESPAWN_PROTECTION_DAMAGE, false)) return;

            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            if (!(event.getDamager() instanceof Player player)) {
                return;
            }

            if (!game.getMetadata().has(player, "respawn")) {
                return;
            }

            val noDamageTime = game.getSetting(GameSetting.RESPAWN_PROTECTION_DAMAGE_TIME, 3) * 1000;
            val time = game.getMetadata().<Long>get(player, "respawn");
            val elapsedTime = System.currentTimeMillis() - time;

            if (elapsedTime <= noDamageTime) {
                player.setNoDamageTicks(0);

                val gamer = Gamer.getGamer(player);

                if (gamer != null) {
                    gamer.sendLocalizedMessage("respawn_damage");
                }

                game.getMetadata().remove(player, "respawn");
            } else if (elapsedTime > noDamageTime) {
                game.getMetadata().remove(player, "respawn");
            }
        });
    }

    // Игровая зона
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayableZoneBlockPlace(BlockPlaceEvent event) {
        checkGame(event, () -> {
            val block = event.getBlock();
            if (!game.checkPlayableZone(block) || game.isProtectionLocation(block.getLocation())) {
                event.setCancelled(true);

                val player = event.getPlayer();
                val gamer = Gamer.getGamer(event.getPlayer());

                gamer.sendLocalizedMessage("cant_place_blocks_here");
                player.updateInventory();
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayableZoneBlockBreak(BlockBreakEvent event) {
        checkGame(event, () -> {
            val block = event.getBlock();
            if (!game.checkPlayableZone(block) || game.isProtectionLocation(block.getLocation())) {
                event.setCancelled(true);

                val player = event.getPlayer();
                val gamer = Gamer.getGamer(event.getPlayer());

                gamer.sendLocalizedMessage("cant_place_blocks_here");
                player.updateInventory();
            }
        });
    }

    public void onUnloadChunkEvent(ChunkUnloadEvent event) {
        checkGame(event, () -> {
            if (game.getGameState() != GameState.GAME) return;

            event.setCancelled(true);
        });
    }
}
