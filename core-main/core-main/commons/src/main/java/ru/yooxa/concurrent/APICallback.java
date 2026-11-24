package ru.yooxa.concurrent;

import java.util.function.BiConsumer;

/**
 * Class of project Base on 22.02.2019 in 0:21.
 */
@FunctionalInterface
public interface APICallback<T> extends BiConsumer<T, Throwable> {

    APICallback VOID = (o, o2) -> {
    };
}
