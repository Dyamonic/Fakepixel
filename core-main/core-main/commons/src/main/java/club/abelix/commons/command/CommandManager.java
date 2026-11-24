package club.abelix.commons.command;

import club.abelix.commons.command.condition.CommandConditionManager;
import club.abelix.commons.command.condition.WrappedCompiledCommandCondition;
import club.abelix.commons.command.dependency.DependencyInjection;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.execution.FallbackExecutor;
import club.abelix.commons.command.execution.PrepareExecution;
import club.abelix.commons.command.execution.SubcommandExecutor;
import club.abelix.commons.command.execution.WrappedCommandExecutor;
import club.abelix.commons.command.execution.WrappedSubcommandExecutor;
import club.abelix.commons.command.execution.help.HelpCommandExecutor;
import club.abelix.commons.command.execution.help.HelpSubcommandExecutor;
import club.abelix.commons.command.execution.help.WithHelp;
import club.abelix.commons.gamer.ICommandSender;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.project.Project;
import club.abelix.commons.util.metafactory.MetafactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class CommandManager implements Serializable {

    public static final long serialVersionUID = 34895669595L;

    private final CommandConditionManager commandConditionManager;

    public CommandManager() {
        this.commandConditionManager = new CommandConditionManager();
    }

    private static WrappedCommandExecutor createCommandExecutor(CommandWrapper wrapper, Object instance,
                                                                Method method) {
        return new WrappedCommandExecutor(
                new CommandParams(wrapper, method, createExecutor(instance, method), method.isAnnotationPresent(IgnoreLogs.class))
        );
    }

    private static WrappedSubcommandExecutor createSubcommandExecutor(CommandWrapper wrapper, Object instance,
                                                                      Method method) {
        SubcommandExecutor subcommandExecutor = method.getDeclaredAnnotation(SubcommandExecutor.class);

        String[] aliases = getAliases(method);
        String[] names = new String[1 + aliases.length];
        names[0] = subcommandExecutor.value();

        System.arraycopy(aliases, 0, names, 1, aliases.length);

        return new WrappedSubcommandExecutor(
                names, new CommandParams(wrapper, method, createExecutor(instance, method), method.isAnnotationPresent(IgnoreLogs.class))
        );
    }

    private static String[] getAliases(AnnotatedElement element) {
        List<String> totalAliases = new ArrayList<>();

        WithAlias withAlias = element.getDeclaredAnnotation(WithAlias.class);

        if (withAlias != null) {
            totalAliases.add(withAlias.value());
        }

        WithMultipleAliases withMultipleAliases = element.getDeclaredAnnotation(WithMultipleAliases.class);

        if (withMultipleAliases != null) {
            totalAliases.addAll(Arrays.stream(withMultipleAliases.value()).map(WithAlias::value)
                    .collect(Collectors.toList()));
        }

        return totalAliases.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private static CommandExecutor<ExecutionSession> createExecutor(@Nullable Object object, @NotNull Method method) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        if (!isStatic && object == null) {
            throw new IllegalStateException("Can't get invoker for non-static method without instance");
        }

        return isStatic
                ? new MetafactoryBuilder<>(CommandExecutor.class).target(method).buildLambda()
                : new MetafactoryBuilder<>(CommandExecutor.class).target(method).buildLambda(object);
    }

    public abstract void registerCommand(String name, ICommand command);

    public void addCondition(Object o) {
        commandConditionManager.addCondition(o);
    }

    public void addCondition(Class<?> cls) {
        commandConditionManager.addCondition(cls);
    }

    public List<WrappedCompiledCommandCondition> getConditions(List<String> conditions) {
        return commandConditionManager.getConditions(conditions);
    }

    /**
     * <br>Зарегистрировать команду (класс)
     * <br>
     * <br>Если в команде есть не статичные методы, какие-нибудь
     * <br>переменные или конструкторы, то используйте {@link #addCommand(Object)}
     */
    public void addCommand(@NotNull Class<?> cls) {
        addCommand(cls, null);
    }

    /**
     * <br>Зарегистрировать команду (экземпляр)
     * <br>
     * <br>См. также: {@link #addCommand(Class)}
     */
    public void addCommand(@NotNull Object instance) {
        addCommand(instance.getClass(), instance);
    }

    public abstract String getServerName();

    public abstract ExecutionSession newCommandSession(
            WrappedCommandExecutor executor, ICommandSender sender, IGamer gamer, String[] args
    );

    public abstract Map<String, String> buildCommandLokiParameters(
            WrappedCommandExecutor executor, ICommandSender sender, IGamer gamer, String[] args
    );

    private void addCommand(@NotNull Class<?> cls, @Nullable Object instance) {
        if (!cls.isAnnotationPresent(NamedCommand.class)) {
            throw new IllegalStateException("Command must be annotated with @NamedCommand");
        }

        if (cls.isAnnotationPresent(ScopedCommand.class)) {
            ScopedCommand sc = cls.getAnnotation(ScopedCommand.class);
            Project p = sc.value();

            if (p != Project.CURRENT) {
                log.info("Will skip command {} because is scoped for {}", cls.getSimpleName(), p.name());
                return;
            }
        }

        DependencyInjection.injectDependencies(cls, instance);

        NamedCommand command = cls.getDeclaredAnnotation(NamedCommand.class);
        String commandName = command.value();
        String[] commandAliases = getAliases(cls);

        val isLogsIgnored = cls.isAnnotationPresent(IgnoreLogs.class);

        CommandWrapper wrapper = new CommandWrapper(this, commandName, commandAliases, isLogsIgnored);

        if (cls.isAnnotationPresent(WithHelp.class)) {
            WithHelp help = cls.getDeclaredAnnotation(WithHelp.class);

            boolean primary = help.primary();

            if (primary) {
                wrapper.setExecutor(new HelpCommandExecutor(wrapper));
            }

            wrapper.addSubcommand(new HelpSubcommandExecutor(wrapper));
        }

        for (Method method : cls.getMethods()) {
            try {
                if (method.isAnnotationPresent(PrepareExecution.class)) {
                    wrapper.setPrepareSession(createExecutor(instance, method));

                    continue;
                }

                for (CommandType type : CommandType.values()) {
                    if (method.isAnnotationPresent(type.cls)) {
                        type.apply(wrapper, instance, method);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error occurred until adding a command: [Method: " + method.getName() + "; Class: " + cls + "]");

                e.printStackTrace();
            }
        }

        registerCommand(commandName, wrapper);

        for (String alias : wrapper.getAliases()) {
            registerCommand(alias, wrapper);
        }

        log.info("Registered command: /{} (ignoreLogs: {}) (aliases: {})", commandName, isLogsIgnored,
                String.join(", ", wrapper.getAliases()));
    }

    public enum CommandType {

        MAIN(Executor.class) {
            @Override
            public void apply(CommandWrapper wrapper, Object instance, Method method) {
                wrapper.setExecutor(createCommandExecutor(wrapper, instance, method));
            }
        },
        SUB(SubcommandExecutor.class) {
            @Override
            public void apply(CommandWrapper wrapper, Object instance, Method method) {
                wrapper.addSubcommand(createSubcommandExecutor(wrapper, instance, method));
            }
        },
        FALLBACK(FallbackExecutor.class) {
            @Override
            public void apply(CommandWrapper wrapper, Object instance, Method method) {
                wrapper.setFallback(createCommandExecutor(wrapper, instance, method));
            }
        };

        private final Class<? extends Annotation> cls;

        CommandType(Class<? extends Annotation> cls) {
            this.cls = cls;
        }

        public abstract void apply(CommandWrapper wrapper, Object instance, Method method);

    }


}
