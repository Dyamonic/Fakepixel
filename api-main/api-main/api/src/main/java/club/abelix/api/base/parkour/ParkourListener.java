package club.abelix.api.base.parkour;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerUnloadEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.parkour.event.ParkourStartEvent;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.server.lobby.event.PlayerDoubleJumpEvent;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.core.bukkit.HeartBeat;
import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class ParkourListener extends YListener {

    public ParkourListener(@NonNull AbstractPlugin plugin) {
        super(plugin);
    }

    private final TObjectLongMap<Player> cooldown = new TObjectLongHashMap<>();

    @EventHandler
    public void onQuit(GamerUnloadEvent event) {
        Gamer gamer = event.getGamer();

        cooldown.remove(gamer.getPlayer());

        LobbyParkour.removeParkourState(gamer);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        ParkourState state = LobbyParkour.getParkourState(gamer);

        if (state == null) {
            return;
        }

        if (event.getTo().getY() <= state.getParkourCheckpoint().getMinY()) {
            event.setTo(state.getParkour().getCheckpoint(state.getCheckpoint()).getTeleportLocation());

            gamer.sendLocalizedMessage("parkour_you_fall");
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerDoubleJumpEvent event) {
        if (LobbyParkour.getParkourState(Gamer.getGamer(event.getPlayer())) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }

        Player player = event.getPlayer();
        long ms = cooldown.get(player);

        if (ms > 0 && System.currentTimeMillis() - ms < 1000) {
            return;
        }

        Location location = LocationUtil.getBlockCenter(event.getClickedBlock());

        Vector vector = location.toVector().toBlockVector();
        ParkourCheckpoint checkpoint = LobbyParkour.getCheckpointAt(vector);

        if (checkpoint == null) {
            return;
        }

        cooldown.put(player, System.currentTimeMillis());

        Gamer gamer = Gamer.getGamer(player);
        ParkourState state = LobbyParkour.getParkourState(gamer);

        LobbyParkour parkour = checkpoint.getParkour();

        if (state == null) {
            if (checkpoint.getIndex() != 0) {
                gamer.sendLocalizedMessage("parkour_not_started");
                return;
            }

            ParkourStartEvent parkourStart = new ParkourStartEvent(gamer, parkour);
            HeartBeat.callEvent(parkourStart);

            // паркур нельзя начать если кто-то отменил эвент
            // в основном его будут отменять гаджеты, чтобы
            // не было никакой хуеты
            if (parkourStart.isCancelled()) {
                return;
            }

            parkour.startParkour(gamer);

            gamer.sendLocalizedMessage("parkour_started");
            return;
        }

        Language language = gamer.getLanguage();

        int oldCheckpoint = state.getCheckpoint();
        int newCheckpoint = checkpoint.getIndex();

        if (newCheckpoint == 0) {
            state.runStartTimer();

            gamer.sendLocalizedMessage("parkour_started_again");
            return;
        }

        if (oldCheckpoint < newCheckpoint) {
            int dif = newCheckpoint - oldCheckpoint;

            if (dif > 1) {
                gamer.sendLocalizedMessage("parkour_checkpoint_hacking");
                return;
            }

            if (newCheckpoint == parkour.getTotalCheckpoints() - 1) {
                String property = parkour.getPropertyName();

                long elapsed = System.currentTimeMillis() - state.getStart();
                long previous = gamer.getProperty(property);

                boolean hasRecord = previous > 0;
                boolean isRecordBeaten = !hasRecord || previous > elapsed;

                gamer.sendLocalizedMessage("parkour_finished");

                int lastParkourDay = gamer.getProperty(property + "/Last");
                int today = (int) TimeUtil.getDayUTC();

                if (lastParkourDay != today || isRecordBeaten) {
                    gamer.setProperty(property + "/Last", today);;
                } else {
                    gamer.sendLocalizedMessage("parkour_no_reward");
                }

                String elapsedMessage = LobbyParkour.format(elapsed);

                if (isRecordBeaten) {
                    gamer.sendLocalizedMessage("parkour_record",
                            "%previous%", LobbyParkour.format(previous),
                            "%record_time%", elapsedMessage);

                    gamer.setProperty(property, (int) elapsed);
                } else {
                    gamer.sendLocalizedMessage("parkour_time", "%time%", elapsedMessage);
                }

                parkour.stopParkour(gamer);
            } else {
                long elapsed = System.currentTimeMillis() - state.getStartCheckpoint();

                gamer.sendLocalizedMessage("parkour_checkpoint_got",
                        "%time%", Duration.durationWordsMs(language, elapsed));

                state.runCheckpointTimer(newCheckpoint);
            }
        }
    }

}
