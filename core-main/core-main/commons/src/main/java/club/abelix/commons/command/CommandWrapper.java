package club.abelix.commons.command;

import club.abelix.commons.command.condition.WrappedCompiledCommandCondition;
import club.abelix.commons.command.exception.CommandMessageException;
import club.abelix.commons.command.exception.CommandRelayException;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import club.abelix.commons.command.execution.WrappedSubcommandExecutor;
import club.abelix.commons.command.execution.help.HelpCommandExecutor;
import club.abelix.commons.command.tabcomplete.StaticTabcompleter;
import club.abelix.commons.command.tabcomplete.Tabcompleter;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.loki.LokiHttpClient;
import club.abelix.di.core.DiStaticAccess;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
@Getter
@ToString(exclude = "commandManager")
@RequiredArgsConstructor
public class CommandWrapper implements ICommand, Serializable {

    public static final long serialVersionUID = 7485784578L;

    private final CommandManager commandManager;

    private final String name;

    private final String[] aliases;
    private final boolean isLogsIgnored;

    private final Map<String, WrappedSubcommandExecutor> subcommands = new LinkedHashMap<>();

    @Setter
    private CommandExecutor<ExecutionSession> prepareSession;
    @Setter
    private WrappedCommandExecutor fallback;
    private WrappedCommandExecutor executor;
    private Tabcompleter[] subcommandCompleters;
    private Tabcompleter[] cachedCompleters;

    public void setExecutor(WrappedCommandExecutor executor) {
        if (executor instanceof HelpCommandExecutor) {
            if (this.executor != null) {
                this.fallback = this.executor;
            }
        } else {
            if (this.executor instanceof HelpCommandExecutor) {
                this.fallback = executor;

                return;
            }
        }

        this.executor = executor;
    }

    public void addSubcommand(WrappedSubcommandExecutor executor) {
        for (String name : executor.getNames()) {
            subcommands.put(name.toLowerCase(), executor);
        }

        // ну на всякий случай :)
        subcommandCompleters = null;
        cachedCompleters = null;
    }

    private boolean doChecked(CommandExecutor<ExecutionSession> sessionConsumer, ExecutionSession session) throws Exception {
        try {
            sessionConsumer.execute(session);

            return false;
        } catch (CommandMessageException e) {
            e.printMessages(session);

            return true;
        }
    }

    public boolean hasFallback() {
        return fallback != null;
    }

    public boolean hasExecutor() {
        return executor != null;
    }

    public WrappedCommandExecutor getExecutor(String[] args) {
        WrappedCommandExecutor executor = args.length == 0
                ? CommandWrapper.this.executor
                : subcommands.get(args[0].toLowerCase());

        if (executor == null) {
            executor = hasFallback() ? fallback : CommandWrapper.this.executor;
        }

        return executor;
    }

    public void run(ICommandSender sender, IGamer gamer, String[] args, WrappedCommandExecutor executor) {
        try {
            if (executor == null) {
                sender.sendMessage("No executor found");
                return;
            }

            if (executor instanceof WrappedSubcommandExecutor) {
                args = Arrays.copyOfRange(args, 1, args.length);
            }

            ExecutionSession session = commandManager.newCommandSession(executor, sender, gamer, args);

            if (prepareSession != null) {
                doChecked(prepareSession, session);
            }

            for (WrappedCompiledCommandCondition condition : executor.getConditions()) {
                if (doChecked(condition.getChecker(), session)) {
                    return;
                }
            }

            doChecked(executor.getExecutor(), session);

            recordCommandExecution(sender, gamer, args, executor);
        } catch (CommandRelayException relay) {
            sender.dispatchCommand(relay.getRelay());
        } catch (Throwable t) {
            log.error("Error occurred while executing command {}", name, t);

            sender.sendMessage("§cAn error occurred while executing the command.");
        }
    }

    @Override
    public void execute(final IGamer gamer, final ICommandSender sender, final String[] args) {
        WrappedCommandExecutor executor = getExecutor(args);

        ExecutorService threadExecutor = executor == null ? null : executor.getThreadExecutor();
        Runnable runnable = () -> run(sender, gamer, args, executor);

        if (threadExecutor == null) {
            runnable.run();
        } else {
            threadExecutor.submit(runnable);
        }
    }

    private void recordCommandExecution(ICommandSender sender, IGamer gamer, String[] args, WrappedCommandExecutor executor) {
        if (isLogsIgnored || (executor != null && executor.isLogsIgnored())) {
            return;
        }

        DiStaticAccess.BEAN_SERVICE.withBean(LokiHttpClient.class, client -> {
            val params = commandManager.buildCommandLokiParameters(executor, sender, gamer, args);

            if (params.isEmpty()) return;

            client.queue(
                    params,
                    "%s executed command: /%s %s".formatted(sender.getName(), name, String.join(" ", args))
            );
        });
    }

    public Tabcompleter[] getSubcommandCompleters() {
        if (subcommandCompleters == null) {
            subcommandCompleters = new Tabcompleter[]{
                    new StaticTabcompleter(subcommands.keySet().toArray(new String[0]))
            };
        }

        return subcommandCompleters;
    }

    public Tabcompleter[] getCachedCompleters() {
        if (cachedCompleters == null) {
            if (hasExecutor() && executor.hasTabcompleters()) {
                cachedCompleters = executor.getTabcompleters();
            } else if (hasFallback() && fallback.hasTabcompleters()) {
                cachedCompleters = fallback.getTabcompleters();
            }

            if (subcommands.size() > 0) {
                cachedCompleters = merge(cachedCompleters, getSubcommandCompleters());
            }
        }

        return cachedCompleters;
    }

    private Tabcompleter[] merge(Tabcompleter[] first, Tabcompleter[] second) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        Tabcompleter[] newTabcompleters = new Tabcompleter[Math.max(first.length, second.length)];

        for (int i = 0; i < newTabcompleters.length; i++) {
            Tabcompleter a = i >= first.length ? null : first[i];
            Tabcompleter b = i >= second.length ? null : second[i];

            newTabcompleters[i] = a == null ? b : b == null ? a : a.with(b);
        }

        return newTabcompleters;
    }

    @Override
    public List<String> tabcomplete(ICommandSender sender, String[] args) {
        int offset = 0;
        Tabcompleter[] tabcompleters = null;

        if (args.length < 2) {
            tabcompleters = getCachedCompleters();
        } else {
            WrappedSubcommandExecutor subcommand = subcommands.get(args[0].toLowerCase());

            if (subcommand != null && subcommand.hasTabcompleters()) {
                tabcompleters = subcommand.getTabcompleters();
                offset = 1;
            } else if (hasFallback() && fallback.hasTabcompleters()) {
                tabcompleters = fallback.getTabcompleters();
            } else if (hasExecutor() && executor.hasTabcompleters()) {
                tabcompleters = executor.getTabcompleters();
            }
        }

        if (tabcompleters == null) {
            return Collections.emptyList();
        }

        if (args.length - offset > tabcompleters.length) {
            return Collections.emptyList();
        }

        int arg = args.length - 1;
        List<String> result = tabcompleters[arg - offset].complete(sender, args[arg]);

        return result == null ? Collections.emptyList() : result;
    }
}
