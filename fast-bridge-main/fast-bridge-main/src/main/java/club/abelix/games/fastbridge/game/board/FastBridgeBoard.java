package club.abelix.games.fastbridge.game.board;

import club.abelix.api.base.util.scoreboard.Board;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.commons.island.Island;
import club.abelix.games.fastbridge.commons.utils.RecordFormat;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import club.abelix.network.type.mode.Uncommon;
import lombok.NonNull;
import lombok.val;

import java.util.Comparator;
import java.util.Map;

public class FastBridgeBoard extends Board {

    private final FastBridgeGamer gamer;

    private FastBridgeBoard(@NonNull FastBridgeGamer gamer) {
        super(FastBridgeGame.getInstance().getPlugin(), "Bridging");

        this.gamer = gamer;
        val language = gamer.getGamer().getLanguage();

        setAnimatedDisplayName("Bridging");

        setBoardWebsite(0, language);
        setLine(1, "");
        setLine(2, " §d" + RecordFormat.formatTime(0));
        setLine(3, language.getMessage("board_time"));
        setLine(4, "");
        setLine(5, " §7-");
        setLine(6, language.getMessage("board_blocks"));
        setLine(7);
        setLine(10);
        setLine(11, " §b" + RecordFormat.formatTime(0));
        setLine(12, language.getMessage("board_self_record"));
        setLine(13, "");
        setBoardDateTime(14, language);

        addPlayer(gamer.getGamer().getPlayer());
        startBoardUpdater();
    }

    public static void createForGamer(@NonNull FastBridgeGamer gamer) {
        new FastBridgeBoard(gamer);
    }

    private void startBoardUpdater() {
        update(this::updateDynamicLines, 2, true);
    }

    private void updateDynamicLines() {
        val lifetimeBestTime = gamer.getLifetimeBestTime();
        val language = gamer.getGamer().getLanguage();

        setLine(11, " §b" + RecordFormat.formatTime(lifetimeBestTime));
        setLine(8, language.getMessage("board_gold", "%gold%", gamer.getGamer().getGold()));
        setLine(9, language.getMessage("board_coins", "%coins%", gamer.getGamer().getCoins(Uncommon.FASTBRIDGE)));

        val currentBuildingSession = gamer.getCurrentBuildingSession();
        val island = gamer.getIsland();

        if (currentBuildingSession != null && island != null) {
            if (currentBuildingSession.isStarted() && !currentBuildingSession.isEnded()) {
                val time = System.currentTimeMillis() - currentBuildingSession.getStartBuildingAt();
                setLine(2, " §d" + RecordFormat.formatTime(time));
                setLine(5, " §d" + island.getPlacedBlocks().size());
                return;
            }
        }

        updateLastSession(island);
    }
    private void updateLastSession(Island island) {
        if (island == null || gamer.getEndedSessions().isEmpty()) {
            setLine(2, " §d" + RecordFormat.formatTime(0));
            setLine(5, " §d-");
            return;
        }

        val lastSession = gamer.getEndedSessions().get(gamer.getEndedSessions().size() - 1);
        if (lastSession != null && lastSession.getConfig().equals(island.getRawConfig())) {
            setLine(2, " §b" + RecordFormat.formatTime(lastSession.getDelta()));
            setLine(5, " §b" + lastSession.getBlockCount());
        } else {
            setLine(2, " §d" + RecordFormat.formatTime(0));
            setLine(5, " §d-");
        }
    }

}
