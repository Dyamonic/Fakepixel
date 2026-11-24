package club.abelix.core.velocity.punishment;

import club.abelix.commons.data.punishment.meta.MuteMeta;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.gamer.Gamer;
import club.abelix.core.velocity.punishment.listener.PunishmentMuteListener;
import gnu.trove.TCollections;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PunishmentsManager {

    private final StarshipVelocity starship;

    /**
     * Мапа с активными мутами онлайн игроков
     */
    private final TIntObjectMap<MuteMeta> activeMuteMetasMap
            = TCollections.synchronizedMap(new TIntObjectHashMap<>());

    public void initialize() {
        starship.getProxy().getEventManager().register(starship, new PunishmentMuteListener(this));
    }

    /**
     * Добавить активный мут указанному игроку
     *
     * @param muteMeta - информация о муте
     */
    public void addActiveMute(@NonNull MuteMeta muteMeta) {
        activeMuteMetasMap.put(muteMeta.getPlayerId(), muteMeta);
    }

    /**
     * Удалить активный мут у игрока
     *
     * @param playerId - id игрока
     */
    public void removeActiveMute(int playerId) {
        activeMuteMetasMap.remove(playerId);
    }

    /**
     * Получить активный мут у игрока по его ID
     *
     * @param playerId - id игрока
     * @return - информация об активном муте
     */
    public MuteMeta getActiveMute(int playerId) {
        MuteMeta meta = activeMuteMetasMap.get(playerId);

        if (meta != null && meta.isExpired()) {
            activeMuteMetasMap.remove(playerId);

            meta = null;
        }

        return meta;
    }

    /**
     * Получить активный мут у игрока
     *
     * @param gamer - игрок
     * @return - информация об активном муте
     */
    public MuteMeta getActiveMute(@NonNull Gamer gamer) {
        return getActiveMute(gamer.getPlayerId());
    }

}
