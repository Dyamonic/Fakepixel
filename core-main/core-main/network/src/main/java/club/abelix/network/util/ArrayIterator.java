package club.abelix.network.util;

import java.util.Iterator;

public final class ArrayIterator<T> implements Iterator<T> {

    private final T[] array;

    private int position;

    public ArrayIterator(T[] array) {
        this.array = array;
    }

    @Override
    public boolean hasNext() {
        return position != array.length;
    }

    @Override
    public T next() {
        return array[position++];
    }
}
