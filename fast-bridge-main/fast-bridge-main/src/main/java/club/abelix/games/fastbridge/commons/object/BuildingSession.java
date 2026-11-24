package club.abelix.games.fastbridge.commons.object;

import club.abelix.games.fastbridge.commons.config.IslandConfig;
import com.google.common.base.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class BuildingSession {

    private final IslandConfig config;
    @Setter
    private long startBuildingAt = 0;
    @Setter
    private long endBuildingAt = 0;
    @Setter
    private int blockCount = 0;

    public boolean isStarted() {
        return startBuildingAt != 0;
    }

    public boolean isEnded() {
        return endBuildingAt != 0;
    }

    public int getDelta() {
        if (!isStarted() || !isEnded()) throw new IllegalStateException("Session isn't stated/ended yet");
        return (int) (endBuildingAt - startBuildingAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildingSession that = (BuildingSession) o;
        return startBuildingAt == that.startBuildingAt && endBuildingAt == that.endBuildingAt && Objects.equal(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startBuildingAt, endBuildingAt, config);
    }
}
