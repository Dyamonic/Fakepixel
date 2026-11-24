package club.abelix.api.base.parkour;

import club.abelix.api.base.camera.state.PlayerState;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.hotbar_new.Hotbar;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.base.hotbar_new.builder.HotbarBuilder;
import club.abelix.api.base.playertop.builder.PlayerTopBuilder;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.api.base.playertop.provider.PropertyTopProvider;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.collection.CollectionUtil;
import club.abelix.commons.util.time.Duration;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.di.api.annotation.Autowired;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.Getter;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Map;

public class LobbyParkour {

    private static final String PARKOUR_HOTBAR_ID = "parkour";

    private static final ItemStack PARKOUR_CHECKPOINT_ICON = ItemUtil.getBuilder(Material.IRON_PLATE)
            .setName(Lang.of("parkour_checkpoint_icon"))
            .build();

    private static final ItemStack PARKOUR_RESET_ICON = ItemUtil.getBuilder(Material.WOOD_DOOR)
            .setName(Lang.of("parkour_reset_icon"))
            .build();

    private static final ItemStack PARKOUR_LEAVE_ICON = ItemUtil.getBuilder(Material.BED)
            .setName(Lang.of("parkour_leave_icon"))
            .build();

    private static final Hotbar PARKOUR_HOTBAR;

    static {
        PARKOUR_HOTBAR = HotbarBuilder.of(PARKOUR_HOTBAR_ID)
                .slot(3)
                .item(PARKOUR_CHECKPOINT_ICON)
                .onAny(action -> {
                    val gamer = action.gamer();
                    val parkourState = getParkourState(gamer);

                    if (parkourState == null) return;

                    int checkpointIndex = parkourState.getCheckpoint();

                    ParkourCheckpoint checkpoint = parkourState.getParkour().getCheckpoint(checkpointIndex);

                    checkpoint.teleport(gamer.getPlayer());
                }).done()
                .slot(4)
                .item(PARKOUR_RESET_ICON)
                .onAny(action -> {
                    val gamer = action.gamer();
                    val parkourState = getParkourState(gamer);

                    if (parkourState == null) return;

                    val parkour = parkourState.getParkour();
                    val player = gamer.getPlayer();

                    val checkpoint = parkour.getCheckpoint(0);

                    checkpoint.teleport(player);
                    removeParkourState(gamer);
                    parkour.startParkour(gamer);
                }).done()
                .slot(5)
                .item(PARKOUR_LEAVE_ICON)
                .onAny(action -> {
                    val gamer = action.gamer();
                    val parkourState = getParkourState(gamer);

                    if (parkourState != null) {
                        parkourState.getParkour().stopParkour(gamer);
                    }
                }).done()
                .build();
    }

    @Getter
    private final String name;

    @Getter
    private final String propertyName;

    private static final Map<Vector, ParkourCheckpoint> checkpointByVector = CollectionUtil.createConcurrentMap();
    private static final Map<Gamer, ParkourState> parkourStates = CollectionUtil.createConcurrentMap();

    @Getter
    private final Location parkourLocation;
    private final ParkourCheckpoint[] checkpoints;

    @Autowired
    private HotbarService hotbarService;

    public LobbyParkour(Plugin plugin, ConfigurationSection section) {
        DiBukkitPlatform.getFactory().getBeanService().processInjection(this);
        name = section.getName();
        propertyName = "Parkour/" + name;

        String topLocationString = section.getString("TopLocation");
        if (topLocationString != null) {
            PlayerTopBuilder.defaultTopBuilder()
                    .provider(new ParkourTopProvider(propertyName))
                    .localizationKey("parkour_player_top_name")
                    .location(LocationUtil.stringToLocation(topLocationString))
                    .updateInterval(HeartBeat.FIVE_MINUTES)
                    .create();
        }

        parkourLocation = LocationUtil.stringToLocation(section.getString("StartLocation"));
        Collection<String> checkpointSections = section.getStringList("Checkpoints");

        checkpoints = new ParkourCheckpoint[checkpointSections.size()];

        int index = 0;

        for (String checkpointRawLocation : checkpointSections) {
            Location location = LocationUtil.stringToLocation(checkpointRawLocation);

            ParkourCheckpoint checkpoint = new ParkourCheckpoint(
                    this, location.getBlockY() - 70,
                    location, index
            );

            checkpoints[index] = checkpoint;
            checkpointByVector.put(checkpoint.getLocation().toVector().toBlockVector(), checkpoint);

            index++;
        }

        for (ParkourCheckpoint checkpoint : checkpoints) {
            checkpoint.createHologram();
        }

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Gamer gamer : parkourStates.keySet()) {
                ParkourState state = getParkourState(gamer);

                long millis = System.currentTimeMillis() - state.getStart();

                gamer.sendLocalizedActionBar(
                        "parkour_action_bar",
                        "%parkour_time%", format(millis),
                        "%checkpoints%", state.getCheckpoint()
                );
            }
        }, 1, 1);

        hotbarService.registerHotbar(PARKOUR_HOTBAR);
    }

    public ParkourCheckpoint getCheckpoint(int checkpoint) {
        return checkpoints[checkpoint];
    }

    public static ParkourState removeParkourState(Gamer gamer) {
        return parkourStates.remove(gamer);
    }

    public synchronized void stopParkour(Gamer gamer) {
        ParkourState oldState = removeParkourState(gamer);

        if (oldState != null) {
            oldState.getPlayerState().apply(gamer);
        }

        hotbarService.applyDefault(gamer);

        gamer.getPlayer()
                .addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));
        gamer.getPlayer()
                .addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
    }

    public synchronized void startParkour(Gamer gamer) {
        Player player = gamer.getPlayer();

        ParkourState state = new ParkourState(this);
        state.setPlayerState(new PlayerState(player));
        state.runStartTimer();

        parkourStates.put(gamer, state);

        hotbarService.apply(PARKOUR_HOTBAR, gamer);

        player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
        player.setAllowFlight(false);
        player.setFlying(false);

        gamer.getPlayer()
                .addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
    }

    public synchronized int getTotalCheckpoints() {
        return checkpoints.length;
    }

    public synchronized static ParkourState getParkourState(Gamer gamer) {
        return parkourStates.get(gamer);
    }

    public synchronized static ParkourCheckpoint getCheckpointAt(Vector vector) {
        return checkpointByVector.get(vector);
    }

    public static String format(long millis) {
        long seconds = millis / 1000;
        long difference = millis % 1000 / 10;

        return Duration.simple(seconds) + String.format(".%02d", difference);
    }

    public static class ParkourTopProvider extends PropertyTopProvider {

        public ParkourTopProvider(String property) {
            super(property, 10, true);
        }

        public String format(PlayerTopEntry entry, Language language) {
            return LobbyParkour.format(entry.getValue());
        }
    }

}
