package club.abelix.core.app.api.plugin;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.event.Event;
import club.abelix.network.common.logging.Logging;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.NonNull;
import org.greenrobot.eventbus.EventBus;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PluginManager {

    private final CoreApplication core;

    private final Yaml yaml;
    private final EventBus eventBus; // https://greenrobot.org/eventbus/documentation/delivery-threads-threadmode/
    private final Map<String, CorePlugin> plugins = new LinkedHashMap<>();

    private final Multimap<CorePlugin, Listener> listenersByPlugin = ArrayListMultimap.create();
    private Map<String, PluginDescription> toLoad = new HashMap<>();

    public PluginManager(@NonNull CoreApplication core) {
        this.core = core;

        // Ignore unknown entries in the plugin descriptions
        Constructor yamlConstructor = new Constructor(new LoaderOptions());

        PropertyUtils propertyUtils = yamlConstructor.getPropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        yamlConstructor.setPropertyUtils(propertyUtils);

        yaml = new Yaml(yamlConstructor);

        eventBus = EventBus.builder()
                .executorService(core.getExecutor())
                .sendNoSubscriberEvent(false)
                .logNoSubscriberMessages(false)
                .build();
    }

    /**
     * Returns the {@link CorePlugin} objects corresponding to all loaded plugins.
     *
     * @return the set of loaded plugins
     */
    public Collection<CorePlugin> getPlugins() {
        return plugins.values();
    }

    /**
     * Returns a loaded plugin identified by the specified name.
     *
     * @param name of the plugin to retrieve
     * @return the retrieved plugin or null if not loaded
     */
    public CorePlugin getPlugin(String name) {
        return plugins.get(name);
    }

    public void loadPlugins() {
        Map<PluginDescription, Boolean> pluginStatuses = new HashMap<>();
        for (Map.Entry<String, PluginDescription> entry : toLoad.entrySet()) {
            PluginDescription plugin = entry.getValue();
            if (!enablePlugin(pluginStatuses, new Stack<>(), plugin)) {
                Logging.IMP.error("Failed to enable {}", entry.getKey());
            }
        }
        toLoad.clear();
        toLoad = null;
    }

    public void enablePlugins() {
        for (CorePlugin corePlugin : plugins.values()) {
            enablePlugin(corePlugin);
        }
    }

    public void enablePlugin(@NonNull CorePlugin plugin) {
        PluginDescription description = plugin.getDescription();
        try {
            plugin.onEnable();
            plugin.setLifeCycle(PluginLifeCycle.ENABLE);

            Logging.IMP.info(
                    "Enabled CorePlugin {} version {} by {}",
                    description.getName(),
                    description.getVersion(),
                    description.getAuthor()
            );
        } catch (Throwable t) {
            Logging.IMP.stacktrace(t, "Exception encountered when loading corePlugin: {}",
                    description.getName());

            // Ставим ошибку запуска и на всякий разрегаем листенеры
            disablePlugin(plugin);
            plugin.setLifeCycle(PluginLifeCycle.ENABLE_ERROR);
        }

    }

    public void disablePlugin(@NonNull CorePlugin plugin) {
        try {
            plugin.onDisable();
            this.unregisterListeners(plugin);
            CoreApplication.getInstance().getCommandManager().unregisterPluginCommand(plugin);
        } catch (Exception any) {
            Logging.IMP.error("Failed disable plugin {}. Details:", plugin.getDescription().getName());
            any.printStackTrace();
        } finally {
            plugin.setLifeCycle(PluginLifeCycle.DISABLE);
        }
    }

    public void disablePlugin(@NonNull String pluginName) {
        CorePlugin plugin = this.getPlugin(pluginName);

        if (plugin == null) {
            Logging.IMP.error("Failed disable plugin {}. Is unknown plugin", pluginName);
            return;
        }

        disablePlugin(this.getPlugin(pluginName));
    }

    private boolean enablePlugin(Map<PluginDescription, Boolean> pluginStatuses, Stack<PluginDescription> dependStack, PluginDescription plugin) {
        if (pluginStatuses.containsKey(plugin)) {
            return pluginStatuses.get(plugin);
        }

        // Combine all dependencies for 'for loop'
        Set<String> dependencies = new HashSet<>(plugin.getDepends());
        dependencies.addAll(plugin.getSoftDepends());

        // Success status
        boolean status = true;

        // Try to load dependencies first
        for (String dependName : dependencies) {
            PluginDescription depend = toLoad.get(dependName);
            Boolean dependStatus = (depend != null) ? pluginStatuses.get(depend) : Boolean.FALSE;

            if (dependStatus == null) {
                if (dependStack.contains(depend)) {
                    StringBuilder dependencyGraph = new StringBuilder();
                    for (PluginDescription element : dependStack) {
                        dependencyGraph.append(element.getName()).append(" -> ");
                    }
                    dependencyGraph.append(plugin.getName()).append(" -> ").append(dependName);

                    Logging.IMP.warn("Circular dependency detected: {}", dependencyGraph);

                    status = false;
                } else {
                    dependStack.push(plugin);
                    dependStatus = this.enablePlugin(pluginStatuses, dependStack, depend);
                    dependStack.pop();
                }
            }

            if (dependStatus == Boolean.FALSE && plugin.getDepends().contains(dependName)) { // Only fail if this wasn't a soft dependency
                Logging.IMP.error("{} (required by {}) is unavailable", String.valueOf(dependName), plugin.getName());
                status = false;
            }

            if (!status) {
                break;
            }
        }

        // Do actual loading
        if (status) {
            try {
                URLClassLoader loader = new PluginClassloader(new URL[]{plugin.getPath().toUri().toURL()});
                Class<?> main = loader.loadClass(plugin.getMain());
                CorePlugin clazz = (CorePlugin) main.getDeclaredConstructor().newInstance();

                clazz.init(core, plugin);
                plugins.put(plugin.getName(), clazz);
                clazz.onLoad();
                clazz.setLifeCycle(PluginLifeCycle.LOAD);

                Logging.IMP.info("Loaded plugin {} version {} by {}", plugin.getName(), plugin.getVersion(), plugin.getAuthor());
            } catch (Throwable throwable) {
                Logging.IMP.error("Error load plugin {} version {}: {}", plugin.getName(), plugin.getVersion(), throwable);
                throwable.printStackTrace();
            }
        }

        pluginStatuses.put(plugin, status);
        return status;
    }

    public void detectPlugins(@NonNull Path folder) {
        if (!Files.isDirectory(folder)) {
            throw new IllegalStateException(folder + " must be a directory");
        }

        try {
            Files.list(folder)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .forEach(path -> {
                        try (JarFile jar = new JarFile(path.toFile())) {
                            JarEntry pluginData = jar.getJarEntry("core.yml");
                            if (pluginData == null) {
                                pluginData = jar.getJarEntry("plugin.yml");
                            }
                            Preconditions.checkNotNull(pluginData, "Plugin must have a core.yml or plugin.yml");

                            try (InputStream in = jar.getInputStream(pluginData)) {
                                PluginDescription desc = yaml.loadAs(in, PluginDescription.class);
                                desc.setPath(path);
                                toLoad.put(desc.getName(), desc);
                            }
                        } catch (IOException ex) {
                            Logging.IMP.stacktrace(ex, "Unable to load plugin from file {}", path);
                        }
                    });
        } catch (IOException e) {
            Logging.IMP.stacktrace(e, "Can't list files");
        }
    }

    /**
     * Dispatch an event to all subscribed listeners and return the event once
     * it has been handled by these listeners.
     *
     * @param <T>   the type bounds, must be a class which extends event
     * @param event the event to call
     * @return the called event
     */
    public <T extends Event> T callEvent(T event) {
        Preconditions.checkNotNull(event, "event");

        long start = System.currentTimeMillis();
        eventBus.post(event);
        event.postCall();

        long elapsed = start - System.currentTimeMillis();

        if (elapsed > 20) {
            Logging.IMP.warn("Event {} took more {} ns to process!", event, elapsed);
        }

        return event;
    }

    public void registerListener(Listener listener) {

        if (eventBus.isRegistered(listener)) {
            Logging.IMP.warn("[PluginManager] The listener {} is already registered, so we will not register it", listener);
            return;
        }

        eventBus.register(listener);
    }

    public void registerListener(CorePlugin corePlugin, Listener listener) {
        listenersByPlugin.put(corePlugin, listener);
        eventBus.register(listener);
    }

    public void unregisterListener(Listener listener) {
        eventBus.unregister(listener);
        listenersByPlugin.values().remove(listener);
    }

    public void unregisterListeners(CorePlugin corePlugin) {
        for (Iterator<Listener> it = listenersByPlugin.get(corePlugin).iterator(); it.hasNext(); ) {
            eventBus.unregister(it.next());
            it.remove();
        }
    }

}
