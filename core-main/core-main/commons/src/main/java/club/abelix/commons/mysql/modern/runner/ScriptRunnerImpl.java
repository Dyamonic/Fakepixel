package club.abelix.commons.mysql.modern.runner;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public final class ScriptRunnerImpl implements ScriptRunner {

    Connection connection;
    ClassLoader classLoader;
    String schema;

    @Override
    public @NotNull ScriptRunner classLoader(final @NonNull ClassLoader cl) {
        this.classLoader = cl;
        return this;
    }

    @Override
    public @NotNull ScriptRunner schema(final @NonNull String schema) {
        this.schema = schema;
        return this;
    }

    @Override
    @SneakyThrows
    public @NotNull ScriptRunner run(final @NonNull String resource) {
        val resourceStream = classLoader.getResourceAsStream(resource);

        if (resourceStream == null) {
            throw new FileNotFoundException("No resource with name " + resource + " found");
        }

        try (val reader = new BufferedReader(new InputStreamReader(resourceStream))) {
            return _run(reader);
        }
    }

    @Override
    @SneakyThrows
    public @NotNull ScriptRunner run(final @NonNull File file) {
        try (val reader = new BufferedReader(new FileReader(file))) {
            return _run(reader);
        }
    }

    @Override
    @SneakyThrows
    public @NotNull ScriptRunner run(final @NonNull Path path) {
        try (val reader = Files.newBufferedReader(path)) {
            return _run(reader);
        }
    }

    @Override
    @SneakyThrows
    public @NotNull ScriptRunner run(final @NonNull InputStream is) {
        return _run(new BufferedReader(new InputStreamReader(is)));
    }

    @Override
    @SneakyThrows
    public @NotNull ScriptRunner run(final @NonNull Reader reader) {
        return !(reader instanceof BufferedReader)
                ? _run(new BufferedReader(reader))
                : _run((BufferedReader) reader);
    }

    private ScriptRunner _run(final BufferedReader reader) throws IOException, SQLException {
        val query = new StringBuilder();

        val oldAutoCommit = connection.getAutoCommit();

        if (oldAutoCommit) {
            connection.setAutoCommit(false);
        }

        val oldSchema = connection.getSchema();

        if (schema != null) {
            connection.setSchema(schema);
        }

        String line;

        while ((line = reader.readLine()) != null) {
            val trimmedLine = line.trim();

            if (trimmedLine.isEmpty()
                    || trimmedLine.startsWith("--")
                    || trimmedLine.startsWith("//")) {
                continue;
            }

            if (trimmedLine.endsWith(";")) {
                query.append(line, 0, line.lastIndexOf(';'));

                val statement = connection.createStatement();
                statement.execute(query.toString());

                query.setLength(0);

                try {
                    statement.close();
                } catch (final Exception ignored) {
                }

                continue;
            }

            query.append(line);
            query.append(" ");
        }

        connection.commit();

        if (schema != null) {
            connection.setSchema(oldSchema);
        }

        if (oldAutoCommit) {
            connection.setAutoCommit(true);
        }

        return this;
    }
}