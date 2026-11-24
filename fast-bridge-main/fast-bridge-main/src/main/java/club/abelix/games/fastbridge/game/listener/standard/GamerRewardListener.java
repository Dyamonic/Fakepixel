package club.abelix.games.fastbridge.game.listener.standard;

import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import club.abelix.games.fastbridge.game.events.GamerBeatOwnLifetimeRecordEvent;
import club.abelix.games.fastbridge.game.events.GamerSuccessfulEndSessionEvent;
import club.abelix.games.fastbridge.game.listener.FastBridgeListener;
import club.abelix.network.type.mode.Uncommon;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

public class GamerRewardListener extends FastBridgeListener {

    public GamerRewardListener(FastBridgeGame game) {
        super(game);
    }

    @EventHandler
    public void onSuccessfulEndSession(final GamerSuccessfulEndSessionEvent event) {
        val gamer = event.getGamer();
        val sessions = gamer.getEndedSessions();

        int totalReward = 35;

        try {
            if (sessions.get(sessions.size() - 1).getDelta() < sessions.get(sessions.size() - 2).getDelta()) {
                totalReward += 15;
            }
        } catch (Exception __) {
        }

        addCoins(gamer, totalReward);
    }

    @EventHandler
    public void onGamerBeatOwnRecord(GamerBeatOwnLifetimeRecordEvent event) {
        val gamer = event.getGamer();

        addCoins(gamer, 150);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        checkSession(event, (e, gamer) -> {
            if (gamer.getBlockPlaced().get() >= 5) {
                gamer.getBlockPlaced().set(0);
                addCoins(gamer, 2);
            }
        });
    }

    private void addCoins(FastBridgeGamer fbGamer, int coins) {
        val gamer = fbGamer.getGamer();
        gamer.addCoins(Uncommon.FASTBRIDGE, coins);
    }

}
