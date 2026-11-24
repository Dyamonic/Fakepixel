package club.abelix.commons.data.skin.render;

import club.abelix.commons.data.skin.Skin;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.binary.Base64;
import ru.yooxa.json.JParser;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-core</p>
 * <p>SkinRender creation on 21.11.2023 at 00:02</p>
 */

@UtilityClass
public class SkinRender {

    private final String RENDER_URL = "https://skin.abelixservices.online/";


    private String getSkinUUIDbyValue(@NonNull String value) {
        String decode = new String(Base64.decodeBase64(value));
        return JParser.parse(decode).getAsObject().getAsString("profileId");
    }

    private String getRenderUrl(@NonNull String uuid, @NonNull String renderUrl, int size) {
        return "https://skin.abelixservices.online/%s/%s?size=%d&scale=%d&overlay"
                .formatted(renderUrl, uuid, size, size);
    }

    public String get(@NonNull Skin skin, @NonNull SkinRenderType renderType) {
        return getRenderUrl(skin.getUuid(), renderType.getUrl(), renderType.getDefaultSize());
    }

    public String get(@NonNull Skin skin, @NonNull SkinRenderType renderType, int size) {
        return getRenderUrl(skin.getUuid(), renderType.getUrl(), size);
    }

}
