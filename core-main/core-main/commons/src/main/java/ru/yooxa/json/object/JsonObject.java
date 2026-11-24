package ru.yooxa.json.object;

import ru.yooxa.json.JArray;
import ru.yooxa.json.JElement;
import ru.yooxa.json.JField;
import ru.yooxa.json.JObject;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class of project collector on 18.03.2020 in 21:29.
 */
public interface JsonObject extends JsonString {

    static JElement toJSON(Object obj) {
        return toJSON(obj, new HashMap<>());
    }

    static JElement toJSON(Object obj, Map<Class<?>, Map<Integer, Object>> map) {
        if (obj == null) return null;

        Class<?> c = obj.getClass();
        if (c.isPrimitive()
                || CharSequence.class.isAssignableFrom(c)
                || Boolean.class.isAssignableFrom(c)
                || Number.class.isAssignableFrom(c)) {
            return new JField(obj.toString());
        }

        if (c == Class.class) {
            return new JField(((Class<?>) obj).getName());
        }

        Map<Integer, Object> set = map.computeIfAbsent(c, s -> new HashMap<>());
        String className = c.getName();
        int hashcode = System.identityHashCode(obj);

        if (set.containsKey(hashcode)) {
            return new JField("Reference to " + className + " #" + hashcode);
        } else {
            set.put(hashcode, obj);


            JElement element;

            if (Map.class.isAssignableFrom(c)) {
                JArray<JArray<JElement>> array = new JArray<>();

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                    JArray<JElement> ar = new JArray<>();
                    ar.add(toJSON(entry.getKey(), map));
                    ar.add(toJSON(entry.getValue(), map));

                    array.add(ar);
                }

                element = array;
            } else if (Collection.class.isAssignableFrom(c)) {
                JArray<JElement> array = new JArray<>();

                for (Object e : (Collection<?>) obj) {
                    array.add(toJSON(e, map));
                }

                element = array;
            } else if (c.isArray()) {
                JArray<JElement> array = new JArray<>();

                for (int i = 0; i < Array.getLength(obj); i++) {
                    Object object = Array.get(obj, i);
                    array.add(toJSON(object, map));
                }

                element = array;
            } else {
                List<Class<?>> supers = new ArrayList<>();

                Class<?> last = c;
                supers.add(last);

                while ((last = last.getSuperclass()) != Object.class) {
                    supers.add(last);
                }

                JObject json = new JObject();
                try {
                    for (Class<?> cc : supers) {
                        for (Field field : cc.getDeclaredFields()) {
                            if (Modifier.isStatic(field.getModifiers())) {
                                continue;
                            }

                            String name = field.getName();

                            field.setAccessible(true);
                            Object o = field.get(obj);
                            json.add(name, toJSON(o, map));
                        }
                    }
                } catch (Throwable ex) {
                    json.addProperty("error", ex.getMessage());
                }

                element = json;
            }

            JObject json = new JObject();
            json.addProperty("class", className);
            json.addProperty("hashcode", hashcode);
            json.addProperty("value", element);

            return json;
        }
    }

    JElement toJSON();

    @Override
    default String toJsonString(int tabs) {
        return toJSON().toBeautyString(tabs);
    }
}
