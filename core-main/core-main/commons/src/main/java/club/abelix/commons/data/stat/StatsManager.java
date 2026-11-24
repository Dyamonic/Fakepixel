package club.abelix.commons.data.stat;

import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.DurationField;
import club.abelix.commons.util.time.DurationFormatter;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.type.mode.Master;
import club.abelix.network.type.mode.Mini;
import club.abelix.network.type.mode.Uncommon;
import lombok.Getter;
import lombok.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class StatsManager {

    public static final StatsManager INSTANCE = new StatsManager();

    @Getter
    private final Duration.FormatParams params = new Duration.FormatParams() {
        {
            sourceUnit = TimeUnit.SECONDS;
            locale = DurationFormatter.RU;
            min = DurationField.MIN;
            max = DurationField.HOUR;
        }
    };

    private final Map<String, StatsMode> modes = new ConcurrentHashMap<>();

    public void loadStatsModes() {
        modes.clear();

        for (String table : StatsSqlHandler.INSTANCE.loadTables().join()) {
            StatsMode mode = StatsSqlHandler.INSTANCE.loadMode(table)
                    .join();

            modes.put(table.toUpperCase(), mode);

            Logging.IMP.info("[Statistic] Table {} loaded", table);
        }
    }

    public StatsMode tryGetMode(String mode) {
        return mode == null ? null : modes.get(mode.toUpperCase());
    }

    public StatsMode tryGetMode(@NonNull Mini mini) {
        return tryGetMode(mini.name());
    }

    public StatsMode tryGetMode(@NonNull Master master) {
        return tryGetMode(master.name());
    }

    public StatsMode tryGetMode(@NonNull Uncommon uncommon) {
        return tryGetMode(uncommon.name());
    }

    public StatsMode getMode(@NonNull String mode) {
        StatsMode result = modes.get(mode.toUpperCase());

        if (result == null) {
            throw new IllegalStateException("Warning: StatsMode for '" + mode + "' not found");
        }

        return result;
    }

    public StatsMode getMode(@NonNull Master mode) {
        return getMode(mode.name());
    }

    public StatsMode getMode(@NonNull Mini mini) {
        return getMode(mini.name());
    }

    public StatsMode getMode(@NonNull Uncommon uncommon) {
        return getMode(uncommon.name());
    }

    public Collection<StatsMode> getModes() {
        return modes.values();
    }

}
