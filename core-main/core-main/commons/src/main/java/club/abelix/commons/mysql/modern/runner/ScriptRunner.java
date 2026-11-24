package club.abelix.commons.mysql.modern.runner;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;

public interface ScriptRunner {

    @NotNull ScriptRunner classLoader(@NotNull ClassLoader cl);

    @NotNull ScriptRunner schema(@NotNull String schema);

    @NotNull ScriptRunner run(@NotNull String resource);

    @NotNull ScriptRunner run(@NotNull File file);

    @NotNull ScriptRunner run(@NotNull Path path);

    @NotNull ScriptRunner run(@NotNull InputStream is);

    @NotNull ScriptRunner run(@NotNull Reader reader);

}