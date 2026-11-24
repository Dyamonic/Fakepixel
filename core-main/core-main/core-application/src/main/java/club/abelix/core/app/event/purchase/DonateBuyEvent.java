package club.abelix.core.app.event.purchase;

import club.abelix.commons.language.Language;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class DonateBuyEvent extends Event {

    private final Map<String, Function<Language, Object>> parameters = new HashMap<>();

    @Getter
    private final Gamer gamer;
    @Getter
    private final String category;

    public DonateBuyEvent(Gamer gamer, String category) {
        this.gamer = gamer;
        this.category = category;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParam(@NonNull Language language, @NonNull String key) {
        return (T) parameters.get(key).apply(language);
    }

    public Map<String, Object> getParameters(@NonNull Language language) {
        Map<String, Object> parameterValues = new HashMap<>();

        for (String parameterName : parameters.keySet()) {
            parameterValues.put(parameterName, parameters.get(parameterName).apply(language));
        }

        return parameterValues;
    }

    public void setParam(@NonNull String key, Object value) {
        parameters.put(key, language -> value);
    }

    public void setParam(@NonNull String key, Function<Language, Object> valueProvider) {
        parameters.put(key, valueProvider);
    }
}
