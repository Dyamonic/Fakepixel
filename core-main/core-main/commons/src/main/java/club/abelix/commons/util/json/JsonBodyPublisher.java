package club.abelix.commons.util.json;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

import java.net.http.HttpRequest;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class JsonBodyPublisher implements HttpRequest.BodyPublisher {

    @Delegate
    HttpRequest.BodyPublisher downstream;

    private JsonBodyPublisher(@NonNull Object object) {
        this.downstream = HttpRequest.BodyPublishers.ofString(JsonUtility.toJson(object));
    }

    public static JsonBodyPublisher create(@NonNull Object object) {
        return new JsonBodyPublisher(object);
    }
}
