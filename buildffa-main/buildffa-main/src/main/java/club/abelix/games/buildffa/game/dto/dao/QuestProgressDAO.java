package club.abelix.games.buildffa.game.dto.dao;

import java.util.concurrent.CompletableFuture;

public interface QuestProgressDAO {
    CompletableFuture<QuestProgress> getProgress(int playerId, String questId, String periodType, String periodKey);

    CompletableFuture<Void> ensureProgress(int playerId, String questId, String periodType, String periodKey);

    CompletableFuture<Void> addProgress(int playerId, String questId, String periodType, String periodKey, int amount, int requiredProgress);

    CompletableFuture<Void> setMaxProgress(int playerId, String questId, String periodType, String periodKey, int value, int requiredProgress);

    CompletableFuture<ClaimResult> claim(int playerId, String questId, String periodType, String periodKey, int requiredProgress);

    boolean isAvailable();

    InitializationState getInitializationState();

    CompletableFuture<Void> retryInitialization();

    CompletableFuture<Void> resetPlayerProgress(int playerId);

    enum InitializationState {
        INITIALIZING,
        READY,
        FAILED
    }

    enum ClaimResult {
        CLAIMED,
        INCOMPLETE,
        ALREADY_CLAIMED,
        FAILED
    }

    class QuestProgress {
        private final int progress;
        private final boolean claimed;

        public QuestProgress(int progress, boolean claimed) {
            this.progress = progress;
            this.claimed = claimed;
        }

        public int getProgress() {
            return progress;
        }

        public boolean isClaimed() {
            return claimed;
        }
    }
}
