package club.abelix.commons.mysql.modern;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import w.util.Env;

import java.util.Objects;

@Slf4j
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class DatabaseCredentials {

    public static final DatabaseCredentials MAIN
            = new DatabaseCredentials(
            null,
            Env.getString("MYSQL_USER", "root"),
            Env.getString("MYSQL_PASSWORD", "root"),
            Env.getString("MYSQL_HOST", "localhost"),
            Env.getInt("MYSQL_PORT", 3306),
            Env.getInt("MYSQL_MAX_POOL_SIZE", 4)
    );

    @Nullable
    String database;
    String user;
    String password;
    String host;
    int port;
    int maximumPoolSize;

    public DatabaseCredentials withDatabase(@NonNull String database) {
        return new DatabaseCredentials(
                database,
                user,
                password,
                host,
                port,
                maximumPoolSize
        );
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;

        if (!(o instanceof DatabaseCredentials preset))
            return false;

        return Objects.equals(database, preset.database);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = 31 * hash + (database == null ? 0 : database.hashCode());

        return hash;
    }

}
