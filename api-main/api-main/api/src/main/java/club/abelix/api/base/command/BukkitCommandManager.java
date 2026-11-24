package club.abelix.api.base.command;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.nms.command.CommandApi;
import club.abelix.commons.command.CommandManager;
import club.abelix.commons.command.ICommand;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.project.Project;
import club.abelix.core.bukkit.StarshipBukkit;
import lombok.Setter;
import lombok.val;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class BukkitCommandManager extends CommandManager {

    private final Plugin plugin;

    @Setter
    @Deprecated
    private String prefix = "abelix";

    public BukkitCommandManager(Plugin plugin) {
        super();

        this.plugin = plugin;
    }

    private CommandApi getCommandApi() {
        return FalconAPI.getNmsApi().getCommandApi();
    }

    @Override
    public void registerCommand(String name, ICommand command) {
        if (name.equals(command.getName())) {
            getCommandApi().registerCommand(plugin, new CommandProxy(command));
        }
    }

    public void unregisterCommand(String name) {
        getCommandApi().unregisterCommand(name);
    }

    @Override
    public String getServerName() {
        return CoreUtil.getServerName();
    }

    @Override
    public BukkitExecutionSession newCommandSession(WrappedCommandExecutor executor, ICommandSender sender, IGamer gamer, String[] args) {
        return new BukkitExecutionSession(prefix, executor, (ProxiedCommandSender) sender,
                gamer == null ? null : (Gamer) gamer, args);
    }

    public Map<String, String> buildCommandLokiParameters(
            WrappedCommandExecutor executor, ICommandSender sender, IGamer gamer, String[] args
    ) {
        val params = new HashMap<String, String>();

        params.put("job", "commands-logging");
        params.put("env", "BUKKIT");
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
                params.put("server", StarshipBukkit.getInstance().getServerInfo().getServerName());
            }

            if (coreGamer.getFakeName() != null) {
                params.put("fake_nick", coreGamer.getFakeName().getNickname().getName());
                params.put("fake_nick_id", String.valueOf(coreGamer.getFakeName().getNickname().getNickId()));
            }
        } else {
            params.put("source", "CONSOLE");
        }

        return params;
    }
}
