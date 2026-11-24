package club.abelix.commons.data.skin;

import club.abelix.commons.data.skin.exception.SkinException;
import club.abelix.commons.data.skin.mojang.MojangApi;
import club.abelix.commons.util.EnvUtility;
import club.abelix.commons.util.regex.Patterns;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class SkinsApi {

    /**
     * Скин по умолчанию
     */
    public final Skin DEFAULT_SKIN = new Skin("Agera001", "",
            EnvUtility.getEnvStrict("DEFAULT_SKIN_VALUE"),
            EnvUtility.getEnvStrict("DEFAULT_SKIN_SIGNATURE"));

    /**
     * Получить выбранный скин игрока по его id
     *
     * @param playerId     - id игрока
     * @param fallbackName - скин по умолчанию
     *                     <p>
     *                     Если у игрока не установлен кастомный скин, будет использован скин по умолчанию,
     *                     переданный в параметрах метода. Однако если при получении данных скина из моджанга
     *                     или из базы возникнет ошибка, то выбранным станет скин по умолчанию (DEFAULT_SKIN)
     * @return - скин игрока
     */
    public CompletableFuture<Skin> getPlayerSkin(int playerId, @NonNull String fallbackName) {
        return SkinsSqlHandler.INSTANCE.getCustomSkin(playerId).thenApply((skinOwner) -> {
            if (skinOwner == null) {
                skinOwner = fallbackName;
            }
            try {
                return getCachedSkin(skinOwner);
            } catch (SkinException skinException) {
                return DEFAULT_SKIN;
            }
        });
    }

    public Skin getCachedSkin(@NonNull String skinName) throws SkinException {
        return getCachedSkin(skinName, false);
    }

    /**
     * Получить данные о скине игрока из базы или из моджанга
     *
     * @param skinName имя скина
     * @return - данные о скине
     * @throws SkinException если при получении данных о скине
     *                       возникла ошибка
     */
    public Skin getCachedSkin(@NonNull String skinName, boolean refresh) throws SkinException {
        //если скин - дефолтный скин, то ничего не грузим, это не надо
        if (skinName.equalsIgnoreCase(DEFAULT_SKIN.getName())) {
            return DEFAULT_SKIN;
        }

        Skin skin = SkinsSqlHandler.INSTANCE.getSkinByName(skinName);

        if (skin != null && !skin.isExpired() && !refresh) {
            return skin;
        }

        skin = DEFAULT_SKIN;

        if (Patterns.isMojangNick(skinName)) {
            skin = MojangApi.getSkinTextures(skinName);
            SkinsSqlHandler.INSTANCE.addSkin(skin);
        }

        return skin;
    }

}
