package ru.yooxa.http.header;

import lombok.Getter;
import ru.yooxa.json.JObject;
import ru.yooxa.json.object.JsonObject;

/**
 * Class of project CryptoPidors on 13.06.2019 in 12:57.
 */
@Getter
public class Header implements JsonObject {

    private final String name;
    private final String value;

    public Header(String key, String value) {
        this.name = key;
        this.value = value;
    }

    @Override
    public String toString() {
        return name + " = " + value;
    }

    @Override
    public JObject toJSON() {
        JObject json = new JObject();

        json.addProperty("name", name);
        json.addProperty("value", value);

        return json;
    }
}
