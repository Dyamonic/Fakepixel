package club.abelix.api.server.lobby.impl.lobby.stats.line;

import club.abelix.api.base.gamer.Gamer;
import lombok.NonNull;

public interface StatsNpcLine {
    String getLine(@NonNull Gamer gamer);
}
