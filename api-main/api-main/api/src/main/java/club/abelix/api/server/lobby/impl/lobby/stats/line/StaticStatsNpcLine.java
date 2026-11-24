package club.abelix.api.server.lobby.impl.lobby.stats.line;

import club.abelix.api.base.gamer.Gamer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>StaticStatsNpcLine creation on 30.06.2023 at 02:56</p>
 */

@RequiredArgsConstructor
public class StaticStatsNpcLine implements StatsNpcLine {

    private final String line;

    @Override
    public String getLine(@NonNull Gamer gamer) {
        return line;
    }
}
