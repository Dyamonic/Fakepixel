package club.abelix.network.online.field;

import java.util.Objects;

public interface ServerField {

    String getFieldName();

    boolean isDynamic();

    boolean isIndexed();

    default boolean isMultiValued() {
        return false;
    }

    default String toStored(Object value) {
        return Objects.requireNonNull(value).toString();
    }

}
