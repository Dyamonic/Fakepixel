package club.abelix.api.base.playertop.identifier;

import club.abelix.commons.data.stat.StatisticPeriod;
import club.abelix.commons.data.stat.StatsField;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Одноразовый объект, создается каждый раз когда нужно получить
 * ТОП игроков по определенному полю и периоду
 */

@RequiredArgsConstructor
@EqualsAndHashCode
@Getter
public class StatisticTopIdentifier implements PlayerTopIdentifier {

    private final StatsField statsField;
    private final StatisticPeriod statisticPeriod;

}
