package me.outerspace.thepit.util;

import me.outerspace.thepit.ThePitPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LegacyDataFolderMigrator {
    private LegacyDataFolderMigrator() {}

    public static void migrate(ThePitPlugin plugin) {
        File target = plugin.getDataFolder();
        File parent = target.getParentFile();
        File legacy = new File(parent, "BuildFFA");
        if (!legacy.isDirectory()) return;
        boolean targetContainsFiles = containsFiles(target);
        List<File> candidates = new ArrayList<>();
        collect(legacy, candidates);
        int copied = 0;
        int skipped = 0;
        int invalid = 0;
        for (File source : candidates) {
            Path relative = legacy.toPath().relativize(source.toPath());
            if (targetContainsFiles && !relative.toString().replace('\\', '/').equalsIgnoreCase("arenas.yml")) continue;
            File destination = new File(target, relative.toString());
            if (destination.exists()) {
                skipped++;
                continue;
            }
            if (isYaml(source) && !validYaml(source)) {
                invalid++;
                plugin.getLogger().warning("Skipped invalid legacy YAML " + relative + ".");
                continue;
            }
            try {
                Files.createDirectories(destination.toPath().getParent());
                Path temporary = destination.toPath().resolveSibling(destination.getName() + ".migration.tmp");
                Files.copy(source.toPath(), temporary, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                Files.move(temporary, destination.toPath(), StandardCopyOption.ATOMIC_MOVE);
                copied++;
            } catch (Exception failure) {
                skipped++;
                plugin.getLogger().warning("Could not migrate legacy file " + relative + ": " + failure.getMessage());
            }
        }
        plugin.getLogger().info("Legacy data migration copied=" + copied + " skipped=" + skipped + " invalid=" + invalid + "; plugins/BuildFFA was preserved.");
    }

    private static boolean containsFiles(File directory) {
        if (!directory.isDirectory()) return false;
        File[] files = directory.listFiles();
        if (files == null) return false;
        for (File file : files) if (file.isFile() || containsFiles(file)) return true;
        return false;
    }

    private static void collect(File directory, List<File> output) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                if (!excluded(file.getName())) collect(file, output);
            } else if (eligible(file)) output.add(file);
        }
    }

    private static boolean eligible(File file) {
        String name = file.getName().toLowerCase(Locale.ENGLISH);
        return !excluded(name) && !name.endsWith(".tmp") && !name.endsWith(".lock") && !name.endsWith(".log") && (name.endsWith(".yml") || name.endsWith(".yaml") || name.endsWith(".json") || name.endsWith(".bak"));
    }

    private static boolean excluded(String value) {
        String name = value.toLowerCase(Locale.ENGLISH);
        return name.equals("cache") || name.equals("logs") || name.equals("worlds") || name.equals("runtime") || name.equals("temp") || name.equals("tmp");
    }

    private static boolean isYaml(File file) {
        String name = file.getName().toLowerCase(Locale.ENGLISH);
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private static boolean validYaml(File file) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            new Yaml().load(reader);
            return true;
        } catch (Throwable failure) {
            return false;
        }
    }
}
