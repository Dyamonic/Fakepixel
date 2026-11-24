package club.abelix.commons.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class JsonUtility {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SneakyThrows
    public String toJson(@NotNull Object object) {
        return MAPPER.writeValueAsString(object);
    }

    @SneakyThrows
    public <T> T fromJson(@NotNull String json, @NotNull Class<T> tClass) {
        return MAPPER.readValue(json, tClass);
    }

    @SneakyThrows
    public <T> T fromJson(@NotNull String json, @NotNull TypeReference<T> typeReference) {
        return MAPPER.readValue(json, typeReference);
    }

    public <T> T fromJsonThrowable(@NotNull String json, @NotNull Class<T> tClass) throws JsonProcessingException {
        return MAPPER.readValue(json, tClass);
    }

    public <T> T fromJsonThrowable(@NotNull String json, @NotNull TypeReference<T> typeReference) throws JsonProcessingException {
        return MAPPER.readValue(json, typeReference);
    }
}
