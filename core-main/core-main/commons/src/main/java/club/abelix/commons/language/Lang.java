package club.abelix.commons.language;

import lombok.val;

/**
 * @author WhileInside
 */
public class Lang {

    private final StringBuilder builder;

    public Lang(String localizationKey) {
        this.builder = new StringBuilder("lang:").append(localizationKey);
    }

    public static Lang of(String localizationKey) {
        return new Lang(localizationKey);
    }

    public Lang set(String key, Object value) {
        builder.append(':').append(key).append(':');
        if (value instanceof Lang nested) {
            val inner = nested.compile();
            builder.append('[').append(inner, 0, inner.length() - 1).append(']');
        } else {
            builder.append('{').append(value).append('}');
        }
        return this;
    }

    public Lang setRaw(String key, Object value) {
        builder.append(':').append(key).append(':').append(value.toString());
        return this;
    }

    public Lang setAll(Object... objects) {
        if ((objects.length % 2) != 0) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < objects.length; i += 2) {
            set(objects[i].toString(), objects[i + 1]);
        }

        return this;
    }

    @Override
    public String toString() {
        return builder.append(';').toString();
    }

    public String compile() {
        return toString();
    }

}
