package club.abelix.commons.command;

import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithMultipleConditions;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.condition.WrappedCompiledCommandCondition;
import club.abelix.commons.command.cooldown.WithCooldown;
import club.abelix.commons.command.cooldown.WrappedCooldown;
import club.abelix.commons.command.execution.ExecutionSession;
import club.abelix.commons.command.execution.help.WithoutHelp;
import club.abelix.commons.command.localization.WithLocalizationKey;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.command.tabcomplete.TabcompleteManager;
import club.abelix.commons.command.tabcomplete.Tabcompleter;
import lombok.Getter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandParams {

    @Getter
    private final int methodHash;
    @Getter
    private final CommandWrapper wrapper;
    @Getter
    private final ExecutorService threadExecutor;

    @Getter
    private final List<WrappedCompiledCommandCondition> conditions;
    @Getter
    private final CommandExecutor<ExecutionSession> executor;

    @Getter
    private final WrappedCooldown cooldown;
    @Getter
    private final Tabcompleter[] tabcompleters;

    @Getter
    private final boolean withoutHelp;
    @Getter
    private final String localizationKey;

    @Getter
    private final boolean isLogsIgnored;

    public CommandParams(CommandWrapper wrapper, int hash, CommandExecutor<ExecutionSession> executor, boolean isLogsIgnored) {
        this(
                wrapper, null, hash, Collections.emptyList(),
                executor, WrappedCooldown.EMPTY, null,
                true, null, isLogsIgnored
        );
    }

    public CommandParams(CommandWrapper wrapper, ExecutorService threadExecutor, int methodHash,
                         List<WrappedCompiledCommandCondition> conditions, CommandExecutor<ExecutionSession> executor,
                         WrappedCooldown cooldown, Tabcompleter[] tabcompleters, boolean withoutHelp,
                         String localizationKey, boolean isLogsIgnored) {
        this.wrapper = wrapper;
        this.methodHash = methodHash;
        this.threadExecutor = threadExecutor;
        this.conditions = conditions;
        this.executor = executor;
        this.cooldown = cooldown;
        this.tabcompleters = tabcompleters;
        this.withoutHelp = withoutHelp;
        this.localizationKey = localizationKey;
        this.isLogsIgnored = isLogsIgnored;
    }

    public CommandParams(CommandWrapper wrapper, Method method, CommandExecutor<ExecutionSession> executor, boolean isLogsIgnored) {
        this.wrapper = wrapper;
        this.methodHash = method.getName().hashCode();
        this.threadExecutor = getThreadExecutor(method);
        this.conditions = getConditions(method);
        this.executor = executor;
        this.cooldown = getCooldown(method);
        this.tabcompleters = getTabcompleters(method);
        this.withoutHelp = method.isAnnotationPresent(WithoutHelp.class);
        this.localizationKey = getLocalizationKey(method);
        this.isLogsIgnored = isLogsIgnored;
    }

    private Tabcompleter[] getTabcompleters(Method method) {
        if (method.isAnnotationPresent(Tabcomplete.class)) {
            Tabcomplete tabcomplete = method.getDeclaredAnnotation(Tabcomplete.class);

            return TabcompleteManager.IMP.parsePattern(tabcomplete.value());
        }

        return null;
    }

    private WrappedCooldown getCooldown(Method method) {
        if (method.isAnnotationPresent(WithCooldown.class)) {
            WithCooldown cooldown = method.getDeclaredAnnotation(WithCooldown.class);

            return new WrappedCooldown(cooldown.value(), cooldown.currentServerOnly());
        }

        return WrappedCooldown.EMPTY;
    }

    private String getLocalizationKey(Method method) {
        if (method.isAnnotationPresent(WithLocalizationKey.class)) {
            WithLocalizationKey key = method.getDeclaredAnnotation(WithLocalizationKey.class);

            return key.value();
        }

        return null;
    }

    private ExecutorService getThreadExecutor(Method method) {
        return method.isAnnotationPresent(Async.class)
                ? Executors.newVirtualThreadPerTaskExecutor()
                : null;
    }

    private List<WrappedCompiledCommandCondition> getConditions(Method method) {
        List<String> names = new ArrayList<>();

        WithMultipleConditions withMultipleConditions = method.getDeclaredAnnotation(WithMultipleConditions.class);

        if (withMultipleConditions != null) {
            WithCondition[] withConditionArray = withMultipleConditions.value();

            names.addAll(Arrays.stream(withConditionArray).map(WithCondition::value)
                    .toList());
        }

        WithCondition withCondition = method.getDeclaredAnnotation(WithCondition.class);

        if (withCondition != null) {
            names.add(withCondition.value());
        }

        WithPermission withPermission = method.getDeclaredAnnotation(WithPermission.class);

        if (withPermission != null) {
            names.add("permission: " + withPermission.value());
        }

        return wrapper.getCommandManager().getConditions(names);
    }


}
