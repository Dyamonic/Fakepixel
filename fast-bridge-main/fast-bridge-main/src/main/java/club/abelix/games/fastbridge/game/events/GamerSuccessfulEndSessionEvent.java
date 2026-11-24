package club.abelix.games.fastbridge.game.events;

import club.abelix.games.fastbridge.commons.FastBridgeGamer;

public class GamerSuccessfulEndSessionEvent extends FastBridgeGamerRecordEvent {

    public GamerSuccessfulEndSessionEvent(FastBridgeGamer gamer) {
        super(gamer);
    }

}