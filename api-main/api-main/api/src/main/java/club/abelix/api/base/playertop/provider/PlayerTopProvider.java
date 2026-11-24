package club.abelix.api.base.playertop.provider;

import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.commons.language.Language;

import java.util.List;

public interface PlayerTopProvider {

    /**
     * Обновление информации (топа игроков)
     *
     * @return - игроки в топе (в порядке убывания)
     */
    List<PlayerTopEntry> updatePlayerTop() throws Exception;

    /**
     * Форматировать значение для отображения в топе
     *
     * @param entry - игрок из топа
     * @return - форматированное значение
     */
    default String format(PlayerTopEntry entry, Language language) {
        return String.valueOf(entry.getValue());
    }

}
