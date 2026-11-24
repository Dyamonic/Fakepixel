package club.abelix.api.base.util.thread;

import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

@UtilityClass
public class ThreadUtil {

    public <ToObject> void processIntArray(@NonNull int[] array,
                                           @NonNull IntFunction<ToObject[]> arrayFactory,
                                           @NonNull IntFunction<ToObject> processor,
                                           @NonNull Consumer<ToObject[]> callback) {
        processIntArray(array, arrayFactory, processor, callback, Throwable::printStackTrace);
    }

    /**
     * Переводит int[] в ToObject[] асинхронно в неск. потоках
     */
    public <ToObject> void processIntArray(@NonNull int[] array,
                                           @NonNull IntFunction<ToObject[]> arrayFactory,
                                           @NonNull IntFunction<ToObject> processor,
                                           @NonNull Consumer<ToObject[]> callback,
                                           @NonNull Consumer<Throwable> cause) {
        if (array.length == 0) {
            try {
                callback.accept(arrayFactory.apply(0));
            } catch (Throwable t) {
                cause.accept(t);
            }

            return;
        }

        ToObject[] output = arrayFactory.apply(array.length);

        int i = 0;
        AtomicInteger k = new AtomicInteger();

        for (int v : array) {
            final int j = i++;

            HeartBeat.runAsync(() -> {
                try {
                    output[j] = processor.apply(v);

                    if (k.incrementAndGet() == array.length) {
                        HeartBeat.runSync(() -> callback.accept(output));
                    }
                } catch (Throwable t) {
                    cause.accept(t);
                }
            });
        }
    }

    public <FromObject, ToObject> void processArray(@NonNull FromObject[] array,
                                                    @NonNull IntFunction<ToObject[]> arrayFactory,
                                                    @NonNull Function<FromObject, ToObject> processor,
                                                    @NonNull Consumer<ToObject[]> callback) {
        processArray(array, arrayFactory, processor, callback, Throwable::printStackTrace);
    }

    /**
     * <br>Переводит FromObject[] в ToObject[] асинхронно в неск. потоках
     * <br>
     * <br>Если нужно перевести int[] в ToObject[], то смотрите {@link #processIntArray(int[], IntFunction, IntFunction, Consumer)}
     */
    public <FromObject, ToObject> void processArray(@NonNull FromObject[] array,
                                                    @NonNull IntFunction<ToObject[]> arrayFactory,
                                                    @NonNull Function<FromObject, ToObject> processor,
                                                    @NonNull Consumer<ToObject[]> callback,
                                                    @NonNull Consumer<Throwable> cause) {
        if (array.length == 0) {
            try {
                callback.accept(arrayFactory.apply(0));
            } catch (Throwable t) {
                cause.accept(t);
            }

            return;
        }

        ToObject[] output = arrayFactory.apply(array.length);

        int i = 0;
        AtomicInteger k = new AtomicInteger();

        for (FromObject v : array) {
            final int j = i++;

            HeartBeat.runAsync(() -> {
                try {
                    output[j] = processor.apply(v);

                    if (k.incrementAndGet() == array.length) {
                        HeartBeat.runSync(() -> callback.accept(output));
                    }
                } catch (Throwable t) {
                    cause.accept(t);
                }
            });
        }
    }
}