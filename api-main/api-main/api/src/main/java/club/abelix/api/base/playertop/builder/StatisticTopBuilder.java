package club.abelix.api.base.playertop.builder;

import club.abelix.api.base.playertop.type.PlayerTop;
import club.abelix.api.base.playertop.type.statistic.StatisticPlayerTop;
import club.abelix.commons.data.stat.StatisticPeriod;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import lombok.NonNull;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class StatisticTopBuilder extends PlayerTopBuilder<StatisticTopBuilder> {

    private final List<StatisticPlayerTop.StatisticTopMode> modes = new ArrayList<>();
    private final List<StatisticPlayerTop.StatisticTopField> fields = new ArrayList<>();
    private final List<StatisticPeriod> periods = new ArrayList<>();
    private Location modeChangerLocation;

    public StatisticTopBuilder modeChangerLocation(@NonNull Location location) {
        this.modeChangerLocation = location;
        return this;
    }

    public StatisticTopBuilder addMode(@NonNull StatsMode mode, String displayLocalizationKey) {
        this.modes.add(new StatisticPlayerTop.StatisticTopMode(mode, displayLocalizationKey));
        return this;
    }

    public StatisticTopBuilder addMode(@NonNull StatsMode mode) {
        return addMode(mode, null);
    }

    public StatisticTopBuilder addMode(@NonNull String modeName, String displayLocalizationKey) {
        return addMode(StatsManager.INSTANCE.tryGetMode(modeName), displayLocalizationKey);
    }

    public StatisticTopBuilder addMode(@NonNull String modeName) {
        return addMode(modeName, null);
    }

    public StatisticTopBuilder addPeriod(@NonNull String periodName) {
        return addPeriod(StatisticPeriod.valueOf(periodName.toUpperCase()));
    }

    public StatisticTopBuilder addPeriod(@NonNull StatisticPeriod period) {
        this.periods.add(period);
        return this;
    }

    public StatisticTopBuilder addField(@NonNull Location location, @NonNull String localizationKey, @NonNull String field) {
        this.fields.add(new StatisticPlayerTop.StatisticTopField(location, localizationKey, field));
        return this;
    }

    @Override
    protected PlayerTop<?> generate() {
        return new StatisticPlayerTop(modeChangerLocation, periods, modes, fields, maxPlayers);
    }
}
