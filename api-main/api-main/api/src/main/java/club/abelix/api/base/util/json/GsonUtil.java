package club.abelix.api.base.util.json;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GsonUtil {

    private final static Gson GSON = new Gson();
    private final static JsonParser JSON_PARSER  = new JsonParser();


    /**
     * Parse Java-Object to JSON-String
     *
     * @param object - us object
     */
    public String toJson(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Paring JSON-String to Java-Object
     *
     * @param json - JSON
     * @param wannabeClass - us class
     */
    public <Wannabe> Wannabe fromJson(String json, Class<Wannabe> wannabeClass) {
        // suuft: LOL, I REALLY CAN USE ANY NAME, BUE-BUE FUCKING <T>
        return GSON.fromJson(json, wannabeClass);
    }
}