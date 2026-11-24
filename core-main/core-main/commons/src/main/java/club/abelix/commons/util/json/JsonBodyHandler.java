package club.abelix.commons.util.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JsonBodyHandler<T> implements HttpResponse.BodyHandler<T> {

    TypeReference<T> typeReference;

    private JsonBodyHandler(@NotNull TypeReference<T> typeReference) {
        this.typeReference = typeReference;
    }

    public static <T> JsonBodyHandler<T> create(TypeReference<T> typeReference) {
        return new JsonBodyHandler<T>(typeReference);
    }

    public static <T> JsonBodyHandler<T> create(Class<T> cls) {
        return new JsonBodyHandler<>(new TypeReference<T>() {
            @Override
            public Type getType() {
                return cls;
            }
        });
    }

    @Override
    public HttpResponse.BodySubscriber<T> apply(HttpResponse.ResponseInfo responseInfo) {
        return HttpResponse.BodySubscribers.mapping(
                HttpResponse.BodySubscribers.ofString(Charset.defaultCharset()),
                string -> {
                    try {
                        return JsonUtility.fromJsonThrowable(string, typeReference);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error while processing %s".formatted(typeReference.toString()), e);
                    }
                });
    }
}
