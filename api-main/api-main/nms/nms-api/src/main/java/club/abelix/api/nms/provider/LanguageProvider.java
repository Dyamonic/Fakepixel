package club.abelix.api.nms.provider;

import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.bukkit.entity.Player;

/**
 * Сервис для получения языка игрока из НМС
 * <p>
 * У нмс нет прямого доступа к lite-api, поэтому
 * Gamer и прочие прелести ему недоступны
 */
public interface LanguageProvider {

    /**
     * Получить язык игрока
     *
     * @param player - игрок
     * @return - язык игрока
     */
    Language getLanguage(@NonNull Player player);

}
