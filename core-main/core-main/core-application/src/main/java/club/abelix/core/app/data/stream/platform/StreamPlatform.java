package club.abelix.core.app.data.stream.platform;

import club.abelix.core.app.data.stream.detail.AbstractStreamDetails;
import club.abelix.core.app.data.stream.exception.StreamException;
import lombok.NonNull;
import ru.yooxa.json.JObject;

public interface StreamPlatform<T extends AbstractStreamDetails> {

    /**
     * Вернуть новый объект информации о стриме по указанной ссылке на него
     */
    T parseStreamUrl(@NonNull String streamUrl);

    /**
     * Вернуть красивый URL (ну крч просто URL для пользователя)
     */
    String makeBeautifulUrl(@NonNull AbstractStreamDetails streamDetails);

    /**
     * Получить URL для отправки запроса
     */
    Object makeRequest(@NonNull String streamId);

    /**
     * Обновить инфо о стриме в зависимости от полученного ответа
     */
    void updateStreamDetails(@NonNull AbstractStreamDetails streamDetails,
                             @NonNull Object Object) throws StreamException;

    /**
     * Получить отображаемое имя платформы (для игроков)
     */
    String getDisplayName();

}
