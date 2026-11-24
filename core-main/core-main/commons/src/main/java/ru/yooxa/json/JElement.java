package ru.yooxa.json;

import ru.yooxa.json.object.JsonObject;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public abstract class JElement {

    static final Map<Class<? extends JsonObject>, Function<JElement, ? extends JsonObject>> DESERIALIZERS = new HashMap<>();

    public JElement() {
    }

    public boolean isArray() {
        return this instanceof JArray;
    }

    public boolean isObject() {
        return this instanceof JObject;
    }

    public boolean isJsonObject() {
        return isObject();
    }

    public boolean isField() {
        return this instanceof JField;
    }

    public long getAsLong() {
        throw new IllegalStateException();
    }

    public int getAsInt() {
        throw new IllegalStateException();
    }

    public String getAsString() {
        throw new IllegalStateException();
    }

    public double getAsDouble() {
        throw new IllegalStateException();
    }

    public boolean getAsBoolean() {
        throw new IllegalStateException();
    }

    public BigDecimal getAsBigDecimal() {
        throw new IllegalStateException();
    }

    public BigDecimal getAsBigDecimal(String key) {
        throw new IllegalStateException();
    }

    public <T extends JsonObject> T readClass(Class<? extends T> c) {
        Function<JElement, ? extends JsonObject> function = DESERIALIZERS.get(c);
        if (function == null) {
            try {
                Class.forName(c.getName());
                function = DESERIALIZERS.get(c);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            if (function == null) {
                throw new IllegalStateException("No deserializer for class " + c);
            }
        }

        return (T) function.apply(this);
    }

    public JObject getAsObject() {
        if (this.isObject()) {
            return (JObject) this;
        } else {
            throw new IllegalStateException("Not a JObject: " + this);
        }
    }

    public <T extends JElement> JArray<T> getAsArray() {
        if (this.isArray()) {
            return (JArray<T>) this;
        } else {
            throw new IllegalStateException("Not a JArray: " + this);
        }
    }

    public JField getAsField() {
        if (this.isField()) {
            return (JField) this;
        } else {
            throw new IllegalStateException("Not a JField: " + this);
        }
    }

    public abstract <T extends JElement> T copy();

    public abstract String toString();

    public void toBeautyString(int tabs, Writer builder) throws IOException {
        builder.append(toBeautyString(tabs));
        builder.flush();
    }

    public final String toBeautyString() {
        return toBeautyString(1);
    }

    public String toBeautyString(int tabs) {
        return toString();
    }
}
