package club.abelix.core.app.api.command;

import club.abelix.commons.command.CommandManager;
import club.abelix.commons.command.ICommand;
import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.project.Project;
import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.plugin.CorePlugin;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.NonNull;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CoreCommandManager extends CommandManager {

    private static final HashMap<CorePlugin, ArrayList<ICommand>> commandToPlugins = new HashMap<>();

    public CoreCommandManager() {
        super();
    }

    @Override
    public void registerCommand(String name, ICommand command) {
        CoreApplication.getInstance().
                registerCommand(name, command);
    }

    public void addCommand(@NonNull CorePlugin plugin, @NotNull Object instance) {
        addCommand(instance);

        if (!commandToPlugins.containsKey(plugin)) {
            commandToPlugins.put(plugin, new ArrayList<>(Collections.emptyList()));
            return;
        }
        NamedCommand commandName = instance.getClass().getDeclaredAnnotation(NamedCommand.class);
        commandToPlugins.get(plugin).add(CoreApplication.getInstance().getCommand(commandName.value()));
    }

    public void unregisterPluginCommand(@NonNull CorePlugin plugin) {
        ArrayList<ICommand> pluginCommandList = commandToPlugins.get(plugin);

        if (pluginCommandList == null) {
            return;
        }

        for (ICommand command : pluginCommandList) {
            CoreApplication.getInstance().unregisterCommand(command.getName());

            for (String alias : command.getAliases()) {
                CoreApplication.getInstance().unregisterCommand(alias);
            }
        }
    }

    @Override
    public String getServerName() {
        return null;
    }

    @Override
    public CoreExecutionSession newCommandSession(WrappedCommandExecutor executor, ICommandSender sender, IGamer gamer, String[] args) {
        return new CoreExecutionSession(executor, (CommandSender) sender, gamer == null ? null : (Gamer) gamer, args);
    }

    @Override
    public Map<String, String> buildCommandLokiParameters(WrappedCommandExecutor executor, ICommandSender sender, IGamer gamer, String[] args) {
        val params = new HashMap<String, String>();

        params.put("job", "commands-logging");
        params.put("env", "CORE");
        params.put("namespace", Project.CURRENT.getNamespace());

        params.put("command", executor.getWrapper().getName());
        params.put("args", String.join(" ", args));

        val now = Instant.now();

        params.put("timestamp_raw", String.valueOf(now.toEpochMilli()));
        params.put("timestamp_formatted", DateTimeFormatter.ISO_INSTANT.format(now));

        if (sender instanceof Gamer coreGamer) {
            params.put("source", "PLAYER");
            params.put("id", String.valueOf(gamer.getPlayerId()));
            params.put("name", sender.getName());

            if (gamer.isOnline()) {
                params.put("server", coreGamer.getCurrentServerName());
                params.put("ip", coreGamer.getAddress().getHostAddress());
            }

            if (coreGamer.getFakeNickname() != null) {
                params.put("fake_nick", coreGamer.getFakeNickname().getNickname().getName());
                params.put("fake_nick_id", String.valueOf(coreGamer.getFakeNickname().getNickname().getNickId()));
            }
        } else {
            params.put("source", "CONSOLE");
        }

        return params;
    }
}
