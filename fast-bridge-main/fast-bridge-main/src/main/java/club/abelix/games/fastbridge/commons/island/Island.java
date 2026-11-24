package club.abelix.games.fastbridge.commons.island;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.builder.PlayerTopBuilder;
import club.abelix.api.base.playertop.type.PlayerTop;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.nms.packet.IVisibleObject;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.language.Lang;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.commons.config.IslandConfig;
import club.abelix.games.fastbridge.commons.object.RelativeLocation;
import club.abelix.games.fastbridge.commons.object.TopEntryProvider;
import club.abelix.games.fastbridge.commons.utils.RecordFormat;
import club.abelix.games.fastbridge.commons.utils.RelativeUtils;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import club.abelix.games.fastbridge.game.events.GamerBeatOwnLifetimeRecordEvent;
import club.abelix.games.fastbridge.game.events.GamerSuccessfulEndSessionEvent;
import club.abelix.games.fastbridge.game.gui.DetailedBridgingStatsGui;
import club.abelix.games.fastbridge.game.gui.WorldChangeGui;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EqualsAndHashCode(exclude = "occupiedBy")
public class Island {

    int number;
    RelativeLocation islandOriginLocation;
    Location respawnLocation;
    Location settingsNpcLocation;
    Location getStatisticLocation;
    Location topLocation;
    IslandConfig rawConfig;
    LinkedList<Block> placedBlocks = new LinkedList<>();
    @Getter
    @NonFinal
    private final List<IVisibleObject> visibleObjects = new ArrayList<>(3);
    @Setter
    @NonFinal
    private @Nullable FastBridgeGamer occupiedBy;
    @Setter
    @NonFinal
    private @Nullable Hologram statsHologram;

    public Island(Location islandOriginLocation, IslandConfig config, int number) {
        val world = islandOriginLocation.getWorld();

        this.islandOriginLocation = new RelativeLocation(islandOriginLocation).asImmutable();
        this.rawConfig = config;
        this.number = number;

        this.respawnLocation = config.relSpawnLocation().clone()
                .add(islandOriginLocation)
                .add(new Vector(.5, 0, .5))
                .toLocation(world);

        this.settingsNpcLocation = config.settingsNpc().relLocation().clone()
                .add(islandOriginLocation)
                .add(new Vector(.5, 0, .5))
                .toLocation(world);

        this.getStatisticLocation = config.relHologramLocation().clone()
                .add(islandOriginLocation)
                .add(new Vector(.5, 1.75, .5))
                .toLocation(world);

        this.topLocation = config.relHologramLocation().clone()
                .add(islandOriginLocation)
                .add(new Vector(-4, 1, 8))
                .toLocation(world);
    }

    public void joinGamer(final FastBridgeGamer gamer) {
        setOccupiedBy(gamer);
        gamer.setIsland(this);

        gamer.resetItems();

        createStatsHologram(gamer);
        createSettingsNpc(gamer);

        teleport(gamer.getGamer());
    }

    public void exitGamer() {
        checkGamer(gamer -> {
            deleteBuildingSession();

            visibleObjects.forEach(IVisibleObject::remove);
            visibleObjects.clear();

            setOccupiedBy(null);
            gamer.setIsland(null);
        });
    }

    public boolean containsAllowedBuildingZone(Location worldPoint) {
        val relativePoint = RelativeUtils.convertWorldPointToLocalPoint(worldPoint, islandOriginLocation);
        return rawConfig.relAllowedBuildingRelZone().contains(relativePoint.getBlockX(), relativePoint.getBlockY(), relativePoint.getBlockZ());
    }

    public boolean containsPlayableZone(Location worldPoint) {
        val relativePoint = RelativeUtils.convertWorldPointToLocalPoint(worldPoint, islandOriginLocation);
        return rawConfig.relPlayableZone().contains(relativePoint.getBlockX(), relativePoint.getBlockY(), relativePoint.getBlockZ());
    }

    private void checkGamer(Consumer<FastBridgeGamer> success) {
        if (getOccupiedBy() == null) return;
        if (!getOccupiedBy().getIsland().equals(this)) return;

        success.accept(getOccupiedBy());
    }

    public void handleGameZoneLeave() {
        checkGamer(fbGamer -> {
            val gamer = fbGamer.getGamer();
            val player = fbGamer.getGamer().getPlayer();
            val location = player.getLocation();
            val island = fbGamer.getIsland();

            if ((gamer.getRanks().isStaff() && gamer.isVanished())
                    && location.getY() >= rawConfig.relPlayableZone().getMinimumPoint().getY())
                return;

            if ((location.getYaw() > 135 || location.getYaw() < -135 ||
                    location.getYaw() < 55 || location.getYaw() > -55)
                    && getRespawnLocation().distance(location) < 13
                    && location.getY() >= getRespawnLocation().getY() - 1.5) {

                val found = FastBridgeGame.getInstance()
                        .getIslandSelector()
                        .getFreeIsland(
                                getRawConfig(),
                                player.getLocation(),
                                location.getDirection()
                        );

                if (found.isEmpty()) {
                    gamer.sendLocalizedMessage("island_no_free_islands");
                    island.teleport(gamer);
                    return;
                }

                val freeIsland = found.get();

                val result = FastBridgeGame.getInstance()
                        .getIslandService()
                        .switchIsland(island, freeIsland, fbGamer);

                if (!result) {
                    player.kickPlayer("§cError while switching islands");
                    return;
                }

                island.resetBuildingSession();
                FastBridgeGame.getInstance()
                        .getIslandService()
                        .switchIsland(island, freeIsland, fbGamer);

                return;
            }

            fbGamer.addAttempt();
            resetBuildingSession();
        });
    }

    public void resetBuildingSession() {
        checkGamer(gamer -> {
            teleport(gamer.getGamer());

            List<Block> blocksToRemove = new ArrayList<>(placedBlocks);
            placedBlocks.clear();
            gamer.getBlockRemover().remove(blocksToRemove, gamer);

            gamer.setCurrentBuildingSession(null);
            gamer.resetItems();

            updateHologram();
        });
    }

    public void deleteBuildingSession() {
        checkGamer(gamer -> {
            resetBuildingSession();

            setOccupiedBy(null);
            gamer.getEndedSessions().clear();

            visibleObjects.forEach(IVisibleObject::remove);
            visibleObjects.clear();

            setStatsHologram(null);
        });
    }

    public void endSession() {
        checkGamer(fbGamer -> {
            val lastSession = fbGamer.getCurrentBuildingSession();
            val gamer = fbGamer.getGamer();

            if (getPlacedBlocks().size() < 10) {
                gamer.getPlayer().teleport(getRespawnLocation());
                resetBuildingSession();

                gamer.sendLocalizedMessage("fastbridge_not_enough_blocks");
                return;
            }

            if (lastSession != null) {
                lastSession.setEndBuildingAt(System.currentTimeMillis());
                lastSession.setBlockCount(placedBlocks.size());

                val lifetime = fbGamer.checkAndApplyLifetimeBestTime(lastSession.getDelta());

                fbGamer.getEndedSessions().add(lastSession);
                fbGamer.addSuccessfulAttempt();
                fbGamer.addAttempt();

                val lang = gamer.getLanguage();
                val time = System.currentTimeMillis() - lastSession.getStartBuildingAt();

                if (lifetime) {
                    gamer.sendTitle(lang.getMessage("new_lifetime_record"), "§a" + RecordFormat.formatTime(time));
                    HeartBeat.callEvent(new GamerBeatOwnLifetimeRecordEvent(fbGamer));
                } else {
                    gamer.sendTitle("", "§d" + RecordFormat.formatTime(time));
                }

                gamer.sendActionBar("");

                val dailyTop = FastBridgeGame.getInstance()
                        .getDailyTop()
                        .get(getRawConfig());

                if (dailyTop.containsKey(getOccupiedBy())) {
                    val existing = dailyTop.get(getOccupiedBy());
                    if (existing > time) {
                        dailyTop.put(getOccupiedBy(), time);
                    }
                } else {
                    dailyTop.put(getOccupiedBy(), time);
                }

                HeartBeat.callEvent(new GamerSuccessfulEndSessionEvent(fbGamer));
            }

            resetBuildingSession();
            gamer.playSound(YSound.LEVEL_UP, .75f, 2);
        });
    }

    public @Nullable Hologram createStatsHologram(final FastBridgeGamer gamer) {
        val hologram = new Hologram(getGetStatisticLocation());

        hologram.addTextLine(Lang.of("fastbridge_island_statistics_title")
                .set("%gamer%", gamer.getGamer().getDisplayName()).compile());
        hologram.addTextLine(Lang.of("fastbridge_island_statistics_self_record")
                .set("%record%", RecordFormat.formatTime(gamer.getLifetimeBestTime())).compile());
        hologram.addTextLine(Lang.of("fastbridge_island_statistics_try")
                .set("%try%", gamer.getAttempts()).compile());
        hologram.addTextLine(Lang.of("fastbridge_island_statistics_success_try")
                .set("%try%", gamer.getSuccessfulAttempts()).compile());

        hologram.setPublicStatus(true);
        setStatsHologram(hologram);
        hologram.setClickAction((__) -> new DetailedBridgingStatsGui(getOccupiedBy().getGamer()));

        registerVisibleObject(hologram);

        return hologram;
    }

    public void updateHologram() {
        val gamer = getOccupiedBy();

        if (gamer == null) return;

        if (getStatsHologram() == null) return;

        val hologram = getStatsHologram();

        hologram.modifyTextLine(2, Lang.of("fastbridge_island_statistics_self_record")
                .set("%record%", RecordFormat.formatTime(gamer.getLifetimeBestTime())).compile());
        hologram.modifyTextLine(3, Lang.of("fastbridge_island_statistics_try")
                .set("%try%", gamer.getAttempts()).compile());
        hologram.modifyTextLine(4, Lang.of("fastbridge_island_statistics_success_try")
                .set("%try%", gamer.getSuccessfulAttempts()).compile());
    }

    public @Nullable FakeEntityPlayer createSettingsNpc(final FastBridgeGamer fbGamer) {
        val gamer = fbGamer.getGamer();

        val fakePlayerLocation = settingsNpcLocation.clone()
                .setDirection(RelativeUtils.convertWorldPointToLocalPoint(settingsNpcLocation, islandOriginLocation).multiply(-1));

        val fakePlayer = FalconAPI.getNmsApi()
                .getFakeEntityApi()
                .createPlayer(fakePlayerLocation, gamer.getSkin());

        fakePlayer.startPlayerLooker(2);
        fakePlayer.look(gamer.getPlayer());
        fakePlayer.setClickAction(event -> new WorldChangeGui(gamer).draw());
        fakePlayer.setPublicStatus(true);

        val hologram = new Hologram(fakePlayerLocation.clone().add(0, 1, 0));

        hologram.addTextLine(Lang.of("settings").compile());
        hologram.setPublicStatus(true);

        registerVisibleObject(fakePlayer);
        registerVisibleObject(hologram);

        return fakePlayer;
    }

    public void teleport(final Gamer gamer) {
        gamer.getPlayer().teleport(getRespawnLocation());
    }

    private void registerVisibleObject(final IVisibleObject object) {
        visibleObjects.add(object);
    }

    @Override
    public String toString() {
        return "%s_%d".formatted(rawConfig.worldName(), number);
    }
}
