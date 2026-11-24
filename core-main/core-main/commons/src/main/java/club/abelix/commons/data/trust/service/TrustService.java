package club.abelix.commons.data.trust.service;

import club.abelix.commons.data.trust.TrustFactor;
import club.abelix.commons.data.trust.TrustLevel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface TrustService {

    /**
     * Загрузить trust factor игрока по ID
     * Использует кеш для онлайн игроков
     *
     * @param playerId ID игрока
     * @return Trust factor или пустой Optional если не найден
     */
    @NotNull
    CompletableFuture<Optional<TrustFactor>> getTrustFactor(int playerId);

    /**
     * Загрузить trust factors для нескольких игроков
     *
     * @param playerIds список ID игроков
     * @return список trust factors
     */
    @NotNull
    CompletableFuture<List<TrustFactor>> getTrustFactors(@NotNull List<Integer> playerIds);

    /**
     * Пересчитать trust factor для игрока
     * Вычисляет score на основе playtime, donation, punishments
     *
     * @param playerId ID игрока
     * @return обновленный trust factor
     */
    @NotNull
    CompletableFuture<TrustFactor> recalculateTrustFactor(int playerId);

    @NotNull
    CompletableFuture<Integer> countByLevel(@NotNull TrustLevel level);

    /**
     * Принудительно выставить уровень доверия игроку
     */
    @NotNull
    CompletableFuture<TrustFactor> setForcedLevel(int playerId, @NotNull TrustLevel level);

    /**
     * Сбросить принудительный уровень, вернув автоматический расчет
     */
    @NotNull
    CompletableFuture<TrustFactor> clearForcedLevel(int playerId);

    /**
     * Инвалидировать кеш для игрока
     *
     * @param playerId ID игрока
     */
    void invalidateCache(int playerId);
}
