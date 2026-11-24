package ru.yooxa.json;

import com.google.common.base.Strings;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class JArray<T extends JElement> extends JElement implements Collection<T> {

    public static final JArray<JElement> EMPTY = new JArray<>();

    private final ArrayList<T> elements;

    public JArray() {
        this.elements = new ArrayList<>();
    }

    public JArray(int capacity) {
        this.elements = new ArrayList<>(capacity);
    }

    public boolean add(T element) {
        return this.elements.add(element);
    }

    @Override
    public boolean remove(Object o) {
        return this.elements.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.elements.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return this.elements.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.elements.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.elements.retainAll(c);
    }

    @Override
    public void clear() {
        this.elements.clear();
    }

    public T get(int i) {
        return elements.get(i);
    }

    @Override
    public int size() {
        return this.elements.size();
    }

    @Override
    public boolean isEmpty() {
        return this.elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.elements.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return this.elements.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.elements.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return this.elements.toArray(a);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        this.elements.forEach(action);
    }

    @Override
    public Spliterator<T> spliterator() {
        return this.elements.spliterator();
    }

    @Override
    public JArray<T> copy() {
        JArray<T> array = new JArray<>();

        for (T element : this.elements) {
            array.add(element.copy());
        }

        return array;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");

        boolean first = true;
        for (JElement element : elements) {
            if (first) {
                first = false;
            } else {
                builder.append(",");
            }

            builder.append(element.toString());
        }

        return builder.append(']').toString();
    }

    @Override
    public void toBeautyString(int tabs, Writer builder) throws IOException {
        builder.append("[\n");

        boolean first = true;
        for (JElement element : elements) {
            if (first) {
                first = false;
            } else {
                builder.append(",\n");
            }

            builder.append(Strings.repeat("  ", tabs));

            if (element == null) {
                builder.append("null");
            } else if (element instanceof JField) {
                JField field = (JField) element;

                //У чисел ковычки не нужны
                if (field.isNumber() || field.isBoolean()) {
                    builder.append(element.toString());
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
        builder.append(']');
        builder.flush();
    }

    @Override
    public String toBeautyString(int tabs) {
        StringBuilder builder = new StringBuilder("[\n");

        boolean first = true;
        for (JElement element : elements) {
            if (first) {
                first = false;
            } else {
                builder.append(",\n");
            }

            builder.append(Strings.repeat("  ", tabs));

            if (element == null) {
                builder.append("null");
            } else if (element instanceof JField) {
                JField field = (JField) element;

                //У чисел ковычки не нужны
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
        builder.append(']');

        return builder.toString();
    }
}
