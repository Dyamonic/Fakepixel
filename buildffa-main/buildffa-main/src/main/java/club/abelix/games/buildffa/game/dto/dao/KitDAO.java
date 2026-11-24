package club.abelix.games.buildffa.game.dto.dao;

import club.abelix.games.buildffa.game.dto.KitDTO;
import club.abelix.games.buildffa.game.kit.BaseKit;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface KitDAO {
    CompletableFuture<List<BaseKit>> getPlayerKits(int playerId);

    CompletableFuture<Boolean> isKitPurchased(int playerId, String kitId);

    void addPlayerKit(int playerId, String kitId);

    void deletePlayerKit(int playerId, String kitId);

    void resetPlayerKit(int playerId, String kitId);

    void saveItemSlot(KitDTO kitDTO);

    CompletableFuture<Map<Integer, Integer>> loadItemSlots(int playerId, String kitId);

    CompletableFuture<Void> wipePlayer(int playerId);
}
