package club.abelix.api.base.division;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.stat.StatsMode;
import lombok.NonNull;

import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>DivisionRequerements creation on 10.03.2024 at 01:56</p>
 */

public interface DivisionRequirements {

    boolean check(@NonNull Gamer gamer, @NonNull StatsMode statsMode);

    List<String> getLore(@NonNull Gamer gamer, @NonNull final StatsMode statsMode);
}
