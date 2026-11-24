package club.abelix.commons.util.iterator;

import com.google.common.collect.Iterators;

import java.lang.reflect.Array;
import java.util.Iterator;

/**
 * @author WhileInside
 */
public class Scroller<T> {

    private final int size;

    private final Iterator<T> iterator;
    private final T[] visibleElements;
    private T selectedElement;
    private int selectedVisibleRow;

    public Scroller(int size, T... elements) {
        if (elements.length == 0) {
            throw new IllegalArgumentException();
        }

        this.size = size;

        this.iterator = Iterators.cycle(elements);
        this.visibleElements = (T[]) Array.newInstance(elements.getClass().getComponentType(), size);

        for (int i = 0; i < size; i++) {
            this.visibleElements[i] = iterator.next();
        }

        this.selectedElement = visibleElements[0];
    }

    public T[] getVisibleElements() {
        return visibleElements;
    }

    public T getSelectedElement() {
        return selectedElement;
    }

    public int getSelectedVisibleRow() {
        return selectedVisibleRow;
    }

    public void scroll() {
        scroll(false);
    }

    public synchronized void scroll(boolean fullStep) {
        if (fullStep) {
            if (++selectedVisibleRow == size) {
                for (int i = 0; i < size; i++) {
                    visibleElements[i] = iterator.next();
                }

                selectedVisibleRow = 0;
            }

            selectedElement = visibleElements[selectedVisibleRow];
        } else {
            if (size / 2 == selectedVisibleRow) {
                System.arraycopy(visibleElements, 1, visibleElements, 0, size - 1);

                selectedElement = visibleElements[size - 1];
                visibleElements[size - 1] = iterator.next();
            } else {
                selectedElement = visibleElements[++selectedVisibleRow];
            }
        }
    }

}