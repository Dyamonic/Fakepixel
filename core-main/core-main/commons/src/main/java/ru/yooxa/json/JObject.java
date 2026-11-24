package ru.yooxa.json;

import com.google.common.base.Strings;
import ru.yooxa.json.object.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class JObject extends JElement implements JsonObject {

    private Map<String, JElement> elements;
    private String cache;
    public JObject() {
        this.elements = new LinkedHashMap<>();
    }

    public static <T extends JsonObject> void addDeserializer(Class<T> c, Function<JElement, T> function) {
        DESERIALIZERS.put(c, function);
    }

    public boolean has(String key) {
        return elements.containsKey(key);
    }

    public Set<Map.Entry<String, JElement>> entrySet() {
        return elements.entrySet();
    }

    public JElement get(String key) {
        return this.elements.get(key);
    }

    public JObject add(String key) {
        JObject object = new JObject();

        add(key, object);

        return object;
    }


    public void add(String key, JElement value) {
        if (value == this) {
            throw new IllegalArgumentException("Recursion!");
        }

        this.elements.put(key, value);
        this.cache = null;
    }

    public JElement remove(String key) {
        JElement e = this.elements.remove(key);
        this.cache = null;
        return e;
    }

    public void addProperty(String key, long value) {
        this.add(key, new JField(value));
    }

    public void addProperty(String key, int value) {
        this.add(key, new JField(value));
    }

    public void addProperty(String key, BigDecimal value) {
        if (value == null) {
            this.add(key, null);
        } else {
            this.add(key, new JField(value));
        }
    }

    public void addProperty(String key, String value) {
        if (value == null) {
            this.add(key, null);
        } else {
            this.add(key, new JField(value));
        }
    }

    public void addProperty(String key, byte[] value) {
        if (value == null) {
            this.add(key, null);
        } else {
            this.add(key, new JField(value));
        }
    }

    public void addProperty(String key, Object value) {
        if (value instanceof JElement) {
            this.add(key, (JElement) value);
            return;
        }

        if (value instanceof JsonObject) {
            this.add(key, ((JsonObject) value).toJSON());
            return;
        }

        if (value == null) {
            this.add(key, null);
        } else {
            this.add(key, new JField(value.toString()));
        }
    }

    public JObject getAsObject(String key) {
        JElement e = this.get(key);

        return e == null ? null : e.getAsObject();
    }

    public JObject getAsJsonObject(String key) {
        return this.getAsObject(key);
    }

    public <T extends JElement> JArray<T> getAsArray(String key) {
        JElement e = this.get(key);

        return e == null ? null : e.getAsArray();
    }

    public String getAsString(String key) {
        JElement e = this.get(key);

        return e == null ? null : e.getAsString();
    }

    public Integer getAsInt(String key) {
        JElement e = this.get(key);

        return e == null ? null : e.getAsInt();
    }

    public Long getAsLong(String key) {
        JElement e = this.get(key);

        return e == null ? null : e.getAsLong();
    }

    public BigDecimal getAsBigDecimal(String key) {
        JElement e = this.get(key);

        if (e == null) {
            return null;
        }

        return e.getAsBigDecimal();
    }

    public <T extends JsonObject> T readClass(String key, Class<T> c) {
        JElement data = get(key);
        if (data == null) {
            return null;
        }

        return data.readClass(c);
    }

    public String getCache() {
        if (this.cache == null) return null;

        for (JElement e : this.elements.values()) {
            if (e.isJsonObject() && ((JObject) e).getCache() == null) {
                return null;
            }
        }

        return this.cache;
    }

    @Override
    public String toString() {
        String cache = this.getCache();

        if (cache != null) {
            return cache;
        }

        StringBuilder builder = new StringBuilder("{");

        boolean first = true;
        for (Map.Entry<String, JElement> entry : this.elements.entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }

            builder.append('\"').append(entry.getKey()).append('\"').append(":");

            JElement element = entry.getValue();
            if (element instanceof JField) {
                JField field = (JField) element;

                if (field.isNumber() || field.isBoolean()) {
                    builder.append(element);
                } else {
                    //У строк обязательны
                    builder.append('"')
                            .append(element.toString().replace("\"", "\\\""))
                            .append('"');
                }
            } else {
                builder.append(element);
            }
        }

        builder.append("}");
        return builder.toString();
    }

    @Override
    public void toBeautyString(int tabs, Writer builder) throws IOException {
        builder.append("{\n");

        boolean first = true;
        for (Map.Entry<String, JElement> entry : this.elements.entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(",\n");
            }

            builder.append(Strings.repeat("  ", tabs));
            builder.append('\"').append(entry.getKey()).append('\"').append(": ");

            JElement element = entry.getValue();

            if (element == null) {
                builder.append("null");
            } else if (element instanceof JField) {
                JField field = (JField) element;

                if (field.isNumber() || field.isBoolean()) {
                    builder.append(element.getAsString());
                } else {
                    //У строк обязательны
                    builder.append('"')
                            .append(element.toString().replace("\"", "\\\""))
                            .append('"');
                }
            } else {
                element.toBeautyString(tabs + 1, builder);
            }
        }

        builder.append('\n');
        builder.append(Strings.repeat("  ", tabs - 1));
        builder.append('}');
        builder.flush();
    }

    @Override
    public String toBeautyString(int tabs) {
        StringBuilder builder = new StringBuilder("{\n");

        boolean first = true;
        for (Map.Entry<String, JElement> entry : this.elements.entrySet()) {
            if (first) {
                first = false;
            } else {
                builder.append(",\n");
            }

            builder.append(Strings.repeat("  ", tabs));
            builder.append('\"').append(entry.getKey()).append('\"').append(": ");

            JElement element = entry.getValue();

            if (element == null) {
                builder.append("null");
            } else if (element instanceof JField) {
                JField field = (JField) element;

                if (field.isNumber() || field.isBoolean()) {
                    builder.append(element);
                } else {
                    //У строк обязательны
                    builder.append('"')
                            .append(element.toString().replace("\"", "\\\""))
                            .append('"');
                }
            } else {
                builder.append(element.toBeautyString(tabs + 1));
            }
        }

        builder.append('\n');
        builder.append(Strings.repeat("  ", tabs - 1));
        builder.append('}');
        return builder.toString();
    }

    @Override
    public JObject copy() {
        JObject object = new JObject();

        for (Map.Entry<String, JElement> entry : entrySet()) {
            object.add(entry.getKey(), entry.getValue().copy());
        }

        return object;
    }

    @Override
    public JObject toJSON() {
        return this;
    }
}
