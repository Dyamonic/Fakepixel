package club.abelix.api.server.lobby.listener;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.camera.CameraDestroyReason;
import club.abelix.api.base.camera.event.CameraDestroyEvent;
import club.abelix.api.base.event.skin.SkinChangedEvent;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.gamer.event.GamerLanguageChangeEvent;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.level.event.ExperienceChangeEvent;
import club.abelix.api.base.level.event.LevelUpEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.parkour.LobbyParkour;
import club.abelix.api.base.parkour.ParkourState;
import club.abelix.api.nms.sound.SoundType;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.event.PlayerDoubleJumpEvent;
import club.abelix.api.server.lobby.event.SocialMediaLinkChangeEvent;
import club.abelix.api.server.lobby.profile.ProfileGuisFactory;
import club.abelix.commons.data.level.Leveling;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.NonNull;
import lombok.val;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class LobbyPlayerListener extends YListener {

    private final BaseLobbyServer baseLobbyServer;

    /**
     * Мапа с теми кто хочет изменить ссылку на соц. сеть
     */
    private final Map<Player, Function<String, String>> socialLinkChangeMap = new HashMap<>();

    public LobbyPlayerListener(@NonNull BaseLobbyServer server) {
        super(server.getPlugin());

        this.baseLobbyServer = server;
    }

    @EventHandler
    public void onCameraDestroy(CameraDestroyEvent event) {
        if (event.getReason() == CameraDestroyReason.PLAYER_QUIT)
            return;

        val camera = event.getCamera();
        camera.getPlayer().teleport(camera.getInitialPosition());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);

        //делать с ним ниче не получится, а почему его нет - да бог его знает...
        if (gamer == null) {
            player.kickPlayer("[LOBBY] Failed load gamer");
            return;
        }

        val spawnLocation = baseLobbyServer.getSpawnLocation().clone();

        player.getInventory().setArmorContents(null);
        player.setMaxHealth(20.0);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.ADVENTURE);
        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        player.teleport(spawnLocation);

        if (gamer.hasPermission("lobby.fly")) {
            player.setAllowFlight(true);
            player.setFlying(true);
            player.setVelocity(player.getVelocity().setY(.5));
        }

        player.setLevel(gamer.getLevel());
        setPlayerExperienceScale(gamer);

        baseLobbyServer.onPlayerJoin(gamer);
    }

    @EventHandler
    private void onDoubleJumpMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (!canUseDoubleJump(player) || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        PlayerDoubleJumpEvent doubleJumpEvent = new PlayerDoubleJumpEvent(player);
        plugin.getServer().getPluginManager().callEvent(doubleJumpEvent);

        //отменить его может, к примеру, паркур
        if (doubleJumpEvent.isCancelled()) {
            return;
        }

        Location loc = player.getLocation().clone();

        if (loc.subtract(0.0, 1.0, 0.0).getBlock().getType() != Material.AIR) {
            player.setAllowFlight(true);
        }
    }

    @EventHandler
    public void onDoubleJumpToggle(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (!canUseDoubleJump(player)) {
            return;
        }

        event.setCancelled(true);

        player.setFlying(false);
        player.setAllowFlight(false);

        player.setVelocity(player.getLocation().clone().getDirection().multiply(1.5).setY(0.8));

        FalconAPI.getNmsApi().getSoundApi().playSound(player, SoundType.BLAZE_SHOOT);
    }

    private boolean canUseDoubleJump(@NonNull Player player) {
        Gamer gamer = Gamer.getGamer(player);
        if (gamer == null) {
            return false;
        }

        boolean hasFlyPermission = gamer.getRanks().hasPermission("lobby.fly");
        return !hasFlyPermission;
    }

    @EventHandler
    public void onLanguageChange(GamerLanguageChangeEvent event) {
        baseLobbyServer.onPlayerLanguageChanged(event.getGamer(), event.getOldLanguage());
    }

    @EventHandler
    public void onLevelChange(LevelUpEvent event) {
        val gamer = event.getGamer();

        gamer.getPlayer().setLevel(gamer.getLevel());
        setPlayerExperienceScale(gamer);
    }

    @EventHandler
    public void onSkinChange(SkinChangedEvent event) {
        val gamer = Gamer.getGamer(event.getPlayer());

        HotbarManager.INSTANCE.updateIcon(gamer, 1);
    }

    @EventHandler
    public void onSocialMediaLinkChange(SocialMediaLinkChangeEvent event) {
        socialLinkChangeMap.put(event.getGamer().getPlayer(), event.getFunction());
    }

    @EventHandler
    public void onDisplayNameUpdateChange(GamerDisplayNameUpdateEvent event) {
        val gamer = event.getGamer();
        val player = gamer.getPlayer();

        if (gamer.hasPermission("lobby.fly") && LobbyParkour.getParkourState(gamer) == null) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Function<String, String> function = socialLinkChangeMap.remove(player);

        if (function == null) {
            return;
        }

        event.setCancelled(true);

        String value = function.apply(event.getMessage());

        if (value == null) {
            DiBukkitPlatform.getFactory()
                    .getBeanService()
                    .withBean(ProfileGuisFactory.class, f -> f.openSettingsGui(Gamer.getGamer(player), null));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        val player = event.getPlayer();
        val gamer = Gamer.getGamer(player);

        baseLobbyServer.onPlayerQuit(gamer);
        socialLinkChangeMap.remove(player);
    }

    @EventHandler
    public void onExperienceChange(ExperienceChangeEvent event) {
        setPlayerExperienceScale(event.getGamer());
    }

    @EventHandler
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        event.setSpawnLocation(baseLobbyServer.getSpawnLocation());
    }

    @EventHandler
    public void onPlayerFallAndMultiply(PlayerMoveEvent event) {
        val location = event.getTo();
        val player = event.getPlayer();

        if (location.getBlockY() < 0)
            player.teleport(baseLobbyServer.getSpawnLocation());
    }

    private void setPlayerExperienceScale(@NonNull Gamer gamer) {
        gamer.getPlayer().setExp(Leveling.getPercentageToNextLevel(gamer.getExperience()));
    }

}
