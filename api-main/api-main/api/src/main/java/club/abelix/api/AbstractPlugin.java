package club.abelix.api;

import club.abelix.api.base.command.BukkitCommandManager;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.base.util.task.registration.TaskRegistrationProvider;
import lombok.NonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class AbstractPlugin extends JavaPlugin implements TaskRegistrationProvider {

    public final Logger log = LogManager.getLogger(getClass().getSimpleName());

    private final Map<UUID, BukkitTask> tasks
            = new ConcurrentHashMap<>();

    private final Path pluginPath = getDataFolder().toPath();

    private final Deque<Runnable> shutdownTasks = new ConcurrentLinkedDeque<>();

    private BukkitCommandManager commandManager;

    public Path getPluginPath() {
        return pluginPath;
    }

    public BukkitCommandManager getCommandManager() {
        if (commandManager == null) {
            commandManager = new BukkitCommandManager(this);
        }

        return commandManager;
    }

    public void addShutdownTask(Runnable runnable) {
        synchronized (shutdownTasks) {
            shutdownTasks.addLast(runnable);
        }
    }

    @Override
    public final void onDisable() {
        synchronized (shutdownTasks) {
            while (!shutdownTasks.isEmpty()) {
                shutdownTasks.removeFirst().run();
            }
        }

        onPluginDisable();
    }

    public void onPluginDisable() {

    }

    @Override
    public AbstractPlugin getPlugin() {
        return this;
    }

    @Override
    public List<UUID> getRunningTaskIds() {
        return new ArrayList<>(tasks.keySet());
    }

    @Override
    public void registerTask(UUID s, BukkitTask task) {
        BukkitTask ex = tasks.put(s, task);

        if (ex != null) {
            ex.cancel();
        }
    }

    @Override
    public boolean hasTask(@NonNull UUID taskId) {
        return tasks.containsKey(taskId);
    }

    @Override
    public boolean removeTask(UUID s) {
        BukkitTask ex = tasks.remove(s);
        boolean cancelled = ex != null;

        if (cancelled) {
            ex.cancel();
        }

        return cancelled;
    }

    public void sync(Runnable runnable) {
        new AbstractTask(this) {
            @Override
            protected void run() {
                runnable.run();
            }
        }.now();
    }

    public YamlConfiguration loadConfig(@NonNull String name) {
        return loadConfig(getDataFolder().toPath(), name);
    }

    public YamlConfiguration loadConfig(@NonNull Path dataFolderPath, @NonNull String name) {
        Path configPath = dataFolderPath.resolve(name + ".yml");

        if (!Files.exists(configPath)) {
            try (InputStream is = getResource(name + ".yml");
                 OutputStream os = Files.newOutputStream(configPath)) {
                if (is == null) {
                    throw new IllegalStateException("Configuration " + name + ".yml not found");
                }

                byte[] buf = new byte[1024];
                int n;

                while ((n = is.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return YamlConfiguration.loadConfiguration(configPath.toFile());
    }

}
