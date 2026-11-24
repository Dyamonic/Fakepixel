package club.abelix.api.base.util.random;

import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// todo move to core
@Deprecated
@UtilityClass
public class RandomUtil {

    private static final Comparator RANDOM_COMPARATOR
            = (o1, o2) -> ThreadLocalRandom.current().nextInt(-1, 1);

    public static <T> Comparator<T> randomComparator() {
        return RANDOM_COMPARATOR;
    }

    private <T> Collection<T> getRandomElements(@NonNull List<T> from, int n, boolean unique) {
        List<T> list = new ArrayList<>(n);

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < n; i++) {
            int elementIndex = random.nextInt(from.size());
            T element = unique ? from.remove(elementIndex) : from.get(elementIndex);

            list.add(element);
        }

        return list;
    }

    public <T> Collection<T> getRandomElements(@NonNull T[] array, int n, boolean unique) {
        return getRandomElements(Lists.newArrayList(array), n, unique);
    }

    public <T> Collection<T> getRandomElements(@NonNull Collection<T> collection, int n, boolean unique) {
        return getRandomElements(new ArrayList<>(collection), n, unique);
    }

    public <T> T getRandomElement(@NonNull List<T> list) {
        if (list.isEmpty()) {
            return null;
        }

        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    public <T> T removeRandomElement(@NonNull List<T> list) {
        if (list.isEmpty()) {
            return null;
        }

        return list.remove(ThreadLocalRandom.current().nextInt(list.size()));
    }

    @SafeVarargs
    public <T> T getRandomElement(@NonNull T... array) {
        if (array.length == 0) {
            return null;
        }

        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }

    public <T> T weightedRandom(@NonNull Map<T, Double> weights) {
        double sum = 0.0;

        for (Double value : weights.values()) {
            sum += value;
        }

        double rand = ThreadLocalRandom.current().nextDouble() * sum;
        double from = 0.0;

        T last = null;

        for (Map.Entry<T, Double> entry : weights.entrySet()) {
            double d2 = entry.getValue();

            if (rand >= from && rand < from + d2) {
                return entry.getKey();
            }

            last = entry.getKey();
            from += d2;
        }

        return last;
    }
}
