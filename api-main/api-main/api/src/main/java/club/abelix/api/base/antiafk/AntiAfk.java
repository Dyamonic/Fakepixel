package club.abelix.api.base.antiafk;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.connect.ConnectStatus;
import club.abelix.network.type.mode.Special;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class AntiAfk {

    /**
     * Период таймера
     */
    public static final int TIMER_PERIOD = 20;

    /**
     * Максимальное кол-во мс в афк
     */
    public static final int PLAYER_CHECK_INTERVAL = 7 * 60 * 1000;

    /**
     * Время, через которое игроку будет срать варнингами что он афкшит
     */
    public static final int PLAYER_WARNING_INTERVAL = 6 * 60 * 1000;

    // -----------------------------------------------------------------------------

    @Getter
    private final FalconAPI plugin;

    private final Map<Player, MovementData> playerMovementData = new HashMap<>();

    public AntiAfk(@NonNull FalconAPI plugin) {
        this.plugin = plugin;

        new AntiAfkListener(this);

        new AbstractTask(plugin) {
            @Override
            protected void run() {
                playerMovementData.entrySet().removeIf(entry -> {
                    Player player = entry.getKey();
                    MovementData movementData = entry.getValue();

                    if (movementData.isStay(PLAYER_WARNING_INTERVAL)) {
                        Gamer gamer = Gamer.getGamer(player);

                        if (movementData.isStay(PLAYER_CHECK_INTERVAL)) {
                            sendPlayerToLimbo(gamer);

                            Logging.IMP.info(gamer.getName() + " was kicked for AFK");
                            return true;
                        }

                        gamer.sendLocalizedTitle("please_no_afk_title", "please_no_afk_subtitle");
                        gamer.playSound(YSound.NOTE_PLING, 10f, 1f);
                    }

                    return false;
                });
            }
        }.timer(TIMER_PERIOD, TIMER_PERIOD);
    }

    public MovementData getMovementData(Player player) {
        return playerMovementData.get(player);
    }

    public void setMovementData(Player player, MovementData data) {
        playerMovementData.put(player, data);
    }

    public void removeMovementData(Player player) {
        playerMovementData.remove(player);
    }

    private void sendPlayerToLimbo(@NonNull Gamer gamer) {
        gamer.sendLocalizedMessage("kicked_for_afk_message");

        gamer.connectMode(Special.LIMBO, true).whenComplete((result, cause) -> {
            if (result != ConnectStatus.SUCCESS) {
                gamer.disconnect("kicked_for_afk_reason");
            }
        });
    }
}
