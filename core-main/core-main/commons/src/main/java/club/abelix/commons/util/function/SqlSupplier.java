package club.abelix.commons.util.function;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlSupplier<T> {

    T get() throws SQLException;
}
