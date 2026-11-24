package club.abelix.commons.util.collection;


import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.Consumer;

public class UpdatableTreeSet<T> extends TreeSet<T> {
    private final Consumer<T> updateConsumer;

    public UpdatableTreeSet(Consumer<T> updateConsumer, Comparator<? super T> comparator) {
        super(comparator);
        this.updateConsumer = updateConsumer;
    }

    public boolean update(T e) {
        if (remove(e)) {
            updateConsumer.accept(e);
            return add(e);
        } else {
            return false;
        }
    }
}
