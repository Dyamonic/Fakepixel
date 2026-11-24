package club.abelix.games.buildffa.game.dto.dao;

import club.abelix.games.buildffa.game.dto.SkinDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SkinDAO {
    CompletableFuture<Boolean> isSkinPurchased(int playerId, String skinId);

    void addPlayerSkin(int playerId, String skinId);

    CompletableFuture<SkinDTO> getSelectedSkin(int playerId);

    CompletableFuture<Void> setSelectedSkin(SkinDTO skinDTO);

    CompletableFuture<List<String>> getPlayerSkins(int playerId);

    void recreateTables();

    CompletableFuture<Void> wipePlayer(int playerId);
}
