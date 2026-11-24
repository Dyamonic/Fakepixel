package club.abelix.api.base.division.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.division.DivisionManager;
import club.abelix.api.base.event.chat.AsyncChatFormatEvent;
import club.abelix.api.base.gamer.event.GamerJoinEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.game.event.GameEndEvent;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.network.type.mode.Mini;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Collections;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>DivisionListener creation on 14.03.2024 at 17:51</p>
 */

public class DivisionListener extends YListener {

    public DivisionListener(AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onChatMessage(AsyncChatFormatEvent event) {
        val gamer = event.getSender();
        val selectedDivision = DivisionManager.IMP.getDisplayText(gamer);

        if (selectedDivision == null) {
            return;
        }

        event.appendFormat(selectedDivision + " ");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGamerLoad(GamerJoinEvent event) {
        val gamer = event.getGamer();

        val selectedDivision = DivisionManager.IMP.getSelectedDivision(gamer);

        if (selectedDivision == null) {
            if (!DivisionManager.IMP.isGlobalMode()) {
                return;
            }

            val bestDivision = DivisionManager.IMP.getBestDivision(gamer);
            DivisionManager.IMP.activateDivision(gamer, bestDivision, null, false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGameEnd(GameEndEvent event) {
        val game = event.getGame();
        val gamers = Collections.unmodifiableCollection(game.getGamers());

        Mini mini;
        if (DivisionManager.IMP.isGlobalMode()) {
            mini = null;
        } else {
            mini = game.getMini();
        }

        HeartBeat.runAsync(() -> {
            for (val gamer : gamers) {
                val currentDivision = DivisionManager.IMP.getSelectedDivision(gamer);
                val primaryDivision = DivisionManager.IMP.getBestDivision(gamer, mini);

                if (currentDivision == null || currentDivision.getMini() != primaryDivision.getMini()
                        || currentDivision.equals(primaryDivision)) {
                    continue;
                }

                DivisionManager.IMP.activateDivision(gamer, primaryDivision, mini, true);
            }
        });
    }

}
