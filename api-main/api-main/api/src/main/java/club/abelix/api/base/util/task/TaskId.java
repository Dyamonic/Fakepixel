package club.abelix.api.base.util.task;

import lombok.NonNull;

import java.util.concurrent.atomic.AtomicInteger;

@Deprecated
public interface TaskId {

    AtomicInteger UNIQUE = new AtomicInteger(0);

    static TaskId staticName(@NonNull String name) {
        return () -> name;
    }

    static TaskId uniqueName(@NonNull String name) {
        return formattedName("%s-%012d", name, UNIQUE.getAndIncrement());
    }

    static TaskId uniqueName() {
        return uniqueName("Unique");
    }

    static TaskId formattedName(@NonNull String format, @NonNull Object... objects) {
        return () -> String.format(format, objects);
    }

    String get();

}
