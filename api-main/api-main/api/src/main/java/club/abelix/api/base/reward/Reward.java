package club.abelix.api.base.reward;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.bukkit.inventory.ItemStack;

/**
 * Представляет вещь, которая имеет некое отображение (вещь + имя)
 * и которая может быть выдана игроку (с проверкой, что вещь уже выдана)
 * Конкретные подклассы BoosterReward, EffectCaseItem... должны реализовывать
 * метод выдачи.
 */
public interface Reward {

    /**
     * Выдать предмет игроку
     *
     * @param gamer - игрок
     * @return true, если предмет выдан
     * false, если нет (например если уже имеется у игрока)
     */
    boolean give(@NonNull Gamer gamer);

    /**
     * Получить название награды
     *
     * @param language - язык игрока
     * @return - название награды
     */
    String getRewardName(@NonNull Language language);

    /**
     * Получить иконку для данной награды
     *
     * @param language - язык игрока для которого показываем
     * @return - иконка награды
     */
    ItemStack getRewardIcon(@NonNull Language language);

}
