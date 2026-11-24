package club.abelix.api.base.util.scoreboard.task;

import club.abelix.api.base.util.scoreboard.Board;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>BoardUpdater creation on 28.02.2023 at 12:29</p>
 */

@EqualsAndHashCode(
        exclude = {"tick"}
)
public class BoardUpdater {

    private final Runnable runnable;
    private final int ticks;
    @Getter
    private final Board board;
    private int tick;


    public BoardUpdater(Runnable runnable, int ticks, Board board) {
        this.runnable = runnable;
        this.ticks = ticks;
        this.board = board;
    }

    public void call() {
        runnable.run();
        tick = 0;
    }

    // todo: это какое-то костыльное решение, но как бы да, есть утечка памяти
    public void updateBoardReceivers() {
        List<Player> offlinePlayers = new ArrayList<>();


        for (Player receiver : board.getReceivers()) {
            if (!receiver.isOnline()) {
                offlinePlayers.add(receiver);
            }
        }

        offlinePlayers.forEach(board::removePlayer);
    }

    public void doTick() {
        if (++tick == ticks) {
            call();
        }
    }
}