package club.abelix.api.server.lobby.impl.lobby.stats.line;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>FiledStatsNpcLine creation on 30.06.2023 at 02:49</p>
 */

@RequiredArgsConstructor
public class FiledStatsNpcLine implements StatsNpcLine {

    private final String filedName;

    @Override
    public String getLine(@NonNull Gamer gamer) {
        return gamer.getLanguage().getMessage("stats_npc_line_" + filedName,
                "%value%", gamer.getLifetimeStat(CoreUtil.getMaster(), filedName));
    }
}
