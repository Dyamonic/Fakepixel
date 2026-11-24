package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.WithAlias;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.app.api.plugin.PluginDescription;
import club.abelix.core.app.api.plugin.PluginLifeCycle;
import club.abelix.core.app.api.plugin.PluginManager;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>CorePluginManagerCommand creation on 29.08.2022 at 20:08</p>
 */

@WithAlias("coreplugin")
@WithAlias("coreplugins")
@WithAlias("coremodules")
@WithAlias("cp")
@WithAlias("cpl")
@WithAlias("cm")
@WithAlias("module")
@NamedCommand("corepluginmanager")
public class CorePluginManagerCommand {

    @SubcommandExecutor("list")
    @WithAlias("l")
    @WithPermission("command.core.plugin.list")
    public static void pluginList(CoreExecutionSession session) {
        Collection<CorePlugin> corePluginList = CoreApplication.getInstance().getPluginManager().getPlugins();

        ArrayList<String> coloredPlugins = new ArrayList<>();

        for (CorePlugin plugin : corePluginList) {
            coloredPlugins.add(String.format("%s%s", plugin.getLifeCycle().getColor().toString(), plugin.getDescription().getName()));
        }

        session.sendMessage(String.format("Core plugins (%d): %s", corePluginList.size(), String.join(", ", coloredPlugins)));
    }


    @SubcommandExecutor("reload")
    @Tabcomplete("[pluginName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.core.plugin.reload")
    public static void pluginReload(CoreExecutionSession session) {
        PluginManager pluginManager = CoreApplication.getInstance().getPluginManager();
        CorePlugin corePlugin = pluginManager.getPlugin(session.getArgument(0));

        if (corePlugin == null) {
            session.sendMessage(String.format("§cPlugin with name %s does not exist!", session.getArgument(0)));
            return;
        }

        if (corePlugin.getLifeCycle() != PluginLifeCycle.ENABLE) {
            session.sendMessage(String.format("§cCore plugin is not active. Current status: %s", corePlugin.getLifeCycle().getColoredName()));
            return;
        }

        CoreApplication.getInstance().getPluginManager().disablePlugin(corePlugin);
        CoreApplication.getInstance().getPluginManager().enablePlugin(corePlugin);
        session.sendMessage(String.format("§2Core plugin %s Ver. %s has been successfully reloaded", corePlugin.getDescription().getName(), corePlugin.getDescription().getVersion()));
    }


    @SubcommandExecutor("disable")
    @Tabcomplete("[pluginName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.core.plugin.disable")
    public static void pluginDisable(CoreExecutionSession session) {
        PluginManager pluginManager = CoreApplication.getInstance().getPluginManager();
        CorePlugin corePlugin = pluginManager.getPlugin(session.getArgument(0));

        if (corePlugin == null) {
            session.sendMessage(String.format("§cPlugin with name %s does not exist!", session.getArgument(0)));
            return;
        }

        if (corePlugin.getLifeCycle() != PluginLifeCycle.ENABLE) {
            session.sendMessage(String.format("§cCore plugin is not active. Current status: %s", corePlugin.getLifeCycle().getColoredName()));
            return;
        }

        CoreApplication.getInstance().getPluginManager().disablePlugin(corePlugin);
        session.sendMessage(String.format("§2Core plugin %s Ver. %s has been successfully disabled", corePlugin.getDescription().getName(), corePlugin.getDescription().getVersion()));
    }

    @SubcommandExecutor("enable")
    @Tabcomplete("[pluginName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.core.plugin.enable")
    public static void pluginEnable(CoreExecutionSession session) {
        PluginManager pluginManager = CoreApplication.getInstance().getPluginManager();
        CorePlugin corePlugin = pluginManager.getPlugin(session.getArgument(0));

        if (corePlugin == null) {
            session.sendMessage(String.format("§cCore plugin with name %s does not exist!", session.getArgument(0)));
            return;
        }

        if (corePlugin.getLifeCycle() == PluginLifeCycle.ENABLE) {
            session.sendMessage(String.format("§2Core plugin %s Ver. %s is already enabled!", corePlugin.getDescription().getName(), corePlugin.getDescription().getVersion()));
            return;
        }

        pluginManager.enablePlugin(corePlugin);
        session.sendMessage(String.format("§2Core plugin %s Ver. %s has been successfully enabled!", corePlugin.getDescription().getName(), corePlugin.getDescription().getVersion()));
    }

    @SubcommandExecutor("about")
    @Tabcomplete("[pluginName]")
    @WithCondition("minArgs: 1")
    @WithPermission("command.core.plugin.about")
    public static void pluginAbout(CoreExecutionSession session) {
        PluginManager pluginManager = CoreApplication.getInstance().getPluginManager();
        CorePlugin corePlugin = pluginManager.getPlugin(session.getArgument(0));

        if (corePlugin == null) {
            session.sendMessage(String.format("§cPlugin with name %s does not exist!", session.getArgument(0)));
            return;
        }

        PluginDescription description = corePlugin.getDescription();

        session.sendMessage(String.format("§2Information about the Core Plugin %s\n" +
                        "§f%s Ver. %s" +
                        "\nCycle: %s" +
                        "\n§f%s" +
                        "\n§fAuthor: %s" +
                        "\n§fDependencies: %s" +
                        "\n§fSoft Dependencies: %s" +
                        "\n§fLocated at: %s",
                description.getName(),
                description.getName(), description.getVersion(),
                corePlugin.getLifeCycle().getColoredName(),
                description.getDescription(),
                description.getAuthor(),
                description.getDepends(),
                description.getSoftDepends(),
                corePlugin.getPath().toString()));

    }
}
