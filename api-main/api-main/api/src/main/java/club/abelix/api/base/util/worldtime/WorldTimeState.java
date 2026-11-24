package club.abelix.api.base.util.worldtime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorldTimeState {
    BEGIN_SUNRISE(22550), // Начинается восход. Горизонт начинает менять цвет.
    BEGIN_DAY(24000), // Начало дня в Minecraft.
    SUNRISE(450), // Заканчивается восход. Горизонт больше не меняет цвет.
    DAY(10000), // День
    LIGHT_PEAK(4284), // Уровень освещения достигает своего пикового значения в 15 единиц.
    MIDDAY(6000), // Полдень. Солнце находится в зените
    BEGIN_SUNSET(11617), // Начинается закат. Горизонт начинает темнеть.
    MOON(12567), // Появляется луна
    NIGHT(13050), // Ночь
    MIDDLE_NIGHT(18000); // Полночь. Луна находится в зените.

    private final int time;

}
