package club.abelix.commons.data.skin.mojang;

import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.data.skin.exception.MojangCooldownException;
import club.abelix.commons.data.skin.exception.ProfileNotFoundException;
import club.abelix.commons.data.skin.exception.SkinException;
import club.abelix.commons.data.skin.exception.SkinNotFoundException;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ru.yooxa.json.JArray;
import ru.yooxa.json.JObject;
import ru.yooxa.json.JParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;

@UtilityClass
public class MojangApi {

    private final String UUID_BY_NAME_STRING = "https://api.mojang.com/users/profiles/minecraft/%s";
    private final String SKIN_BY_UUID_STRING = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";

    private String readURL(@NonNull String url) throws SkinException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AbelixCore");
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            connection.setDoOutput(true);

            if (connection.getResponseCode() == 429) {
                throw new MojangCooldownException();
            }

            StringBuilder output = new StringBuilder();

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                while (in.ready()) {
                    output.append(in.readLine());
                }
            }

            return output.toString();
        } catch (IOException exception) {
            return null;
        }
    }

    public Skin getSkinTextures(@NonNull String name) throws SkinException {
        String uuidResponse = readURL(String.format(UUID_BY_NAME_STRING, name));

        if (uuidResponse == null || uuidResponse.isEmpty()) {
            throw new ProfileNotFoundException();
        }

        String playerUniqueId = JParser.parse(uuidResponse)
                .getAsObject()
                .getAsString("id");

        String skinResponse = readURL(String.format(SKIN_BY_UUID_STRING, playerUniqueId));

        if (skinResponse == null || skinResponse.isEmpty()) {
            throw new SkinNotFoundException();
        }

        JArray<?> propertiesArray = JParser.parse(skinResponse)
                .getAsObject()
                .getAsArray("properties");

        if (propertiesArray.isEmpty()) {
            throw new SkinNotFoundException();
        }

        JObject textureProperty = propertiesArray.get(0).getAsObject();

        String value = textureProperty.getAsString("value");
        String signature = textureProperty.getAsString("signature");

        return new Skin(name, playerUniqueId, value, signature, LocalDateTime.now());
    }

}
