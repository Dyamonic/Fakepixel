package club.abelix.core.app.api.plugin;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.config.Configuration;
import club.abelix.core.app.api.config.ConfigurationProvider;
import club.abelix.core.app.api.config.YamlConfiguration;
import club.abelix.network.common.logging.Logging;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Represents any CorePlugin that may be loaded at runtime to enhance existing
 * functionality.
 */
public class CorePlugin {

    @Getter
    private CoreApplication core;

    @Getter
    private PluginDescription description;

    @Getter
    @Setter
    private PluginLifeCycle lifeCycle;

    @Getter
    private Path path;

    private File configFile;

    @Getter
    private Configuration config;

    private YamlConfiguration yaml;

    /**
     * Called when the plugin has just been loaded. Most of the proxy will not
     * be initialized, so only use it for registering
     */
    public void onLoad() {

    }

    /**
     * Called when this plugin is enabled.
     */
    public void onEnable() {

    }

    /**
     * Called when this plugin is disabled.
     */
    public void onDisable() {

    }

    /**
     * @return the connection folder of this plugin
     */
    public final File getDataFolder() {
        return getDataPath().toFile();
    }

    public final Path getDataPath() {
        return core.getPluginsDirectory().resolve(getDescription().getName());
    }

    /**
     * Get a resource from within this plugins jar or container. Care must be
     * taken to close the returned stream.
     *
     * @param name the full path name of this resource
     * @return the stream for getting this resource, or null if it does not
     * exist
     */
    public final InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    /**
     * Called by the loader to initialize the fields in this plugin.
     *
     * @param description the description that describes this plugin
     */
    final void init(@NonNull CoreApplication core, @NonNull PluginDescription description) {
        this.core = core;
        this.description = description;
        this.path = description.getPath();
        this.lifeCycle = PluginLifeCycle.DISABLE;

        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        yaml = (YamlConfiguration) ConfigurationProvider.getProvider(YamlConfiguration.class);
        if (configFile.exists()) {
            try {
                config = yaml.load(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveConfig() {
        try {
            yaml.save(config, configFile);
        } catch (IOException ex) {
            Logging.IMP.stacktrace(ex, "[{}] Error occurred whilst saving a configuration",
                    getDescription().getName());
        }
    }

    public void saveDefaultConfig() {
        if (config != null) {
            return;
        }

        try {
            if (!getDataFolder().exists()) {
                //noinspection ResultOfMethodCallIgnored
                getDataFolder().mkdir();
            }

            if (!configFile.exists()) {
                InputStream is = getResourceAsStream("config.yml");
                if (is == null) {
                    throw new IllegalStateException("config.yml not found in plugin jar file");
                }

                yaml.save(yaml.load(is), configFile);
                loadConfig();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
