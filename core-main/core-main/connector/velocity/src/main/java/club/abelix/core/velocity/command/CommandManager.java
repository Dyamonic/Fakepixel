package club.abelix.core.velocity.command;

import club.abelix.commons.command.ICommand;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.project.Project;
import club.abelix.core.velocity.StarshipVelocity;
import club.abelix.core.velocity.gamer.Gamer;
import lombok.Setter;
import lombok.val;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommandManager extends club.abelix.commons.command.CommandManager {

    private final StarshipVelocity starship;

    @Setter
    @Deprecated
    private String prefix = "§7";

    public CommandManager(StarshipVelocity plugin) {
        super();

        this.starship = plugin;
    }

    @Override
    public void registerCommand(String name, ICommand command) {
        if (name.equals(command.getName())) { // регистрируем только по названию команды. алиасы зарегистрирует сама банджа
            starship.getProxy().getCommandManager().register(command.getName(), new CommandProxy(command), command.getAliases());
        }
    }

    @Override
    public String getServerName() {
        return "abelix";
    }

    @Override
    public ExecutionSession newCommandSession(WrappedCommandExecutor executor, ICommandSender sender, IGamer gamer, String[] args) {
        return new CommandExecutionSession(prefix, executor, (ProxiedCommandSender) sender,
                gamer == null ? null : (Gamer) gamer, args);
    }

    @Override
    public Map<String, String> buildCommandLokiParameters(WrappedCommandExecutor executor, ICommandSender sender, IGamer gamer, String[] args) {
        val params = new HashMap<String, String>();

        params.put("job", "commands-logging");
        params.put("env", "VELOCITY");
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
                params.put("server", coreGamer.getCurrentServer().getName());
                params.put("ip", coreGamer.getAddress().getHostAddress());
            }
        } else {
            params.put("source", "CONSOLE");
        }

        return params;
    }
}
