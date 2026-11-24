package club.abelix.games.fakevival.smpworlds;

import java.util.List;

public record SMPWorldSaveResult(List<String> savedWorlds, List<String> failedWorlds, List<String> skippedWorlds) {
    public int savedCount() {
        return savedWorlds.size();
    }

    public int failedCount() {
        return failedWorlds.size();
    }

    public int skippedCount() {
        return skippedWorlds.size();
    }

    public boolean hasFailures() {
        return !failedWorlds.isEmpty();
    }
}
