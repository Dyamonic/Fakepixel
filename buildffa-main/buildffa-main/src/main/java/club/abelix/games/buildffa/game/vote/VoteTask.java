package club.abelix.games.buildffa.game.vote;

import club.abelix.games.buildffa.game.BuildFFAGame;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author milansky
 */
@RequiredArgsConstructor
public final class VoteTask implements Runnable {
    private static final int TIMER_SECONDS = 900;

    private final BuildFFAGame game;
    @Getter
    private int seconds = TIMER_SECONDS;

    @Override
    public void run() {
        seconds--;

        if (TIMER_SECONDS / 2 == seconds)
            game.broadcastLocalized("buildffa_vote_half");

        if (seconds == 0) {
            game.getVoteManager().apply(game);

            seconds = TIMER_SECONDS;
        }
    }
}
