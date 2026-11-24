package club.abelix.commons.data.skin.mojang;

import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.exception.InvalidSkinImageException;
import club.abelix.commons.data.skin.exception.MojangCooldownException;
import club.abelix.commons.data.skin.exception.SkinException;
import club.abelix.commons.data.skin.exception.SkinNotFoundException;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ru.yooxa.http.HttpRequest;
import ru.yooxa.http.YHttpRequest;
import ru.yooxa.http.header.HttpHeaders;
import ru.yooxa.http.netty.HttpResponse;
import ru.yooxa.json.JArray;
import ru.yooxa.json.JElement;
import ru.yooxa.json.JObject;

import java.time.LocalDateTime;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;

@UtilityClass
public class ElybyApi {

    private final int TOO_MANY_REQUESTS = 429;

    private final String SKIN_BY_NAME_STRING = "http://skinsystem.ely.by/textures/signed/%s?proxy=true";

    private JElement readURL(@NonNull String url) throws SkinException {
        HttpResponse response;

        try {
            YHttpRequest request = new YHttpRequest(HttpRequest.Type.GET, url);

            request.addHeader(HttpHeaders.USER_AGENT, "Abelix");
            request.setConnectTimeout(30000);
            request.setReadTimeout(30000);

            response = request.sendRequest()
                    .get();
        } catch (Throwable exception) {
            throw new InvalidSkinImageException();
        }

        if (response.getCode() == TOO_MANY_REQUESTS) {
            throw new MojangCooldownException();
        }

        if (response.getCode() == HTTP_NO_CONTENT) {
            throw new SkinNotFoundException();
        }

        return response.asJson();
    }

    public Skin getSkinTextures(@NonNull String name) throws SkinException {
        JElement skinResponse = readURL(String.format(SKIN_BY_NAME_STRING, name));

        if (skinResponse == null) {
            throw new SkinNotFoundException();
        }

        JObject object = skinResponse.getAsObject();

        String uuid = object.get("id").getAsString();
        JArray<JObject> dataProperty = object.getAsArray("properties");

        if (dataProperty == null || dataProperty.size() == 0) {
            throw new SkinNotFoundException();
        }

        JObject textureProperty = dataProperty.get(0);

        String value = textureProperty.getAsString("value");
        String signature = textureProperty.getAsString("signature");

        return new Skin(name, uuid, value, signature, LocalDateTime.now());
    }

}
