package club.abelix.commons.mysql.modern;

import club.abelix.network.common.logging.Logging;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Database {
    private static final Map<DatabaseCredentials, DatabaseCredentialsManager> REGISTRY
            = new ConcurrentHashMap<>();

    private final HikariDataSource src;
    private Connection con;

    public Database(final DatabaseCredentials credentials) {
        DatabaseCredentialsManager manager = REGISTRY.computeIfAbsent(credentials, this::initManager);
        src = manager.getSource();
    }

    public static Collection<DatabaseCredentialsManager> getAllManagers() {
        return REGISTRY.values();
    }

    public static void closeConnections() {
        getAllManagers().forEach(manager -> {
            HikariDataSource con = manager.getSource();
            try {
                if (con != null) {
                    con.close();
                }
            } catch (Exception any) {
                Logging.IMP.error("Failed close database connection {}. Details: ", any);
                any.printStackTrace();
            }
        });
    }

    private DatabaseCredentialsManager initManager(final DatabaseCredentials credentials) {
        return new DatabaseCredentialsManager(initSource(credentials), new AtomicInteger());
    }

    protected abstract HikariDataSource initSource(DatabaseCredentials credentials);

    /**
     * Close connection
     */
    public void close() {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException t) {
                throw new IllegalStateException(t);
            }

            con = null;
        }
    }

    /**
     * Get or create connection
     */
    public Connection getConnection() throws SQLException {
        synchronized (this) {
            return src.getConnection();
        }
    }

    private DatabaseStatement makeStatement(final int keys, final String query, final Object... objects) throws SQLException {
        Connection connection = getConnection();
        PreparedStatement statement = connection.prepareStatement(query, keys);

        if (objects != null) {
            for (int i = 0; i < objects.length; i++) {
                Object value = objects[i];

                if (value instanceof LocalDate) {
                    value = Date.valueOf((LocalDate) value);
                }

                if (value == null) {
                    statement.setNull(i + 1, Types.NULL);
                } else {
                    statement.setObject(i + 1, value);
                }
            }
        }

        return new DatabaseStatement(connection, statement);
    }

    /**
     * Execute <code>async</code> query to the database
     *
     * @return Future
     */
    public CompletableFuture<BufferedQuery> executeQuery(String sql, Object... objects) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            try {
                try (DatabaseStatement statement = makeStatement(Statement.NO_GENERATED_KEYS, sql, objects)) {
                    try (ResultSet rs = statement.executeQuery()) {
                        return new BufferedQuery(rs);
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            } finally {
                club.abelix.network.metrics.DatabasePerformanceMetrics.record(sql, System.nanoTime() - start);
            }
        });
    }

    /**
     * Execute <code>async</code> update to the database and return generated keys
     *
     * @return Future
     */
    public CompletableFuture<BufferedExecutionWithGeneratedKeys> executeUpdate(final String sql, final Object... objects) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            try (DatabaseStatement statement = makeStatement(Statement.RETURN_GENERATED_KEYS, sql, objects)) {
                int affectedRows = statement.executeUpdate();

                try (ResultSet rs = statement.getGeneratedKeys()) {
                    return new BufferedExecutionWithGeneratedKeys(
                            rs.next() ? rs.getInt(1) : 0,
                            affectedRows
                    );
                }
            } catch (SQLException e) {
                e.printStackTrace();
                throw new IllegalStateException(e);
            } finally {
                club.abelix.network.metrics.DatabasePerformanceMetrics.record(sql, System.nanoTime() - start);
            }
        });
    }

    /**
     * Execute <code>async</code> update to the database
     *
     * @return Future
     */
    public CompletableFuture<BufferedExecution> executeVoidUpdate(final String sql, final Object... objects) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            try (DatabaseStatement statement = makeStatement(Statement.NO_GENERATED_KEYS, sql, objects)) {
                return new BufferedExecution(statement.executeUpdate());
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            } finally {
                club.abelix.network.metrics.DatabasePerformanceMetrics.record(sql, System.nanoTime() - start);
            }
        });
    }

}
