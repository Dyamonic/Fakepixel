package club.abelix.api.server.lobby.impl.lobby.stats.line;

import club.abelix.api.base.gamer.Gamer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>LocalizableStatsNpcLine creation on 30.06.2023 at 02:57</p>
 */

@RequiredArgsConstructor
public class LocalizableStatsNpcLine implements StatsNpcLine {

    private final String key;

    @Override
    public String getLine(@NonNull Gamer gamer) {
        return gamer.getLanguage().getMessage(key);
    }
}
