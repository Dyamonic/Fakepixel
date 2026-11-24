package club.abelix.network.common.impl;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class Impl {

    private static final Map<String, Supplier<?>> IMPLEMENTATIONS = new HashMap<>();

    public static void set(@NotNull String name, @NotNull Supplier<?> factory) {
        IMPLEMENTATIONS.put(name, factory);
    }

    public static <T> Optional<T> getOptional(String name, Class<T> cls) {
        Supplier<?> factory = IMPLEMENTATIONS.get(name);
        return factory != null ? Optional.of(cls.cast(factory.get())) : Optional.empty();
    }

    public static <T> T get(String name, Class<T> cls) {
        Supplier<?> factory = IMPLEMENTATIONS.get(name);
        if (factory == null) {
            throw new IllegalStateException("Implementation not found: " + name + "@" + cls.getSimpleName());
        }
        return cls.cast(factory.get());
    }

}
