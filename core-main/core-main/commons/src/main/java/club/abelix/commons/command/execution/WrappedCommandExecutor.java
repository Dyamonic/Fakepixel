package club.abelix.commons.command.execution;

import club.abelix.commons.command.CommandExecutor;
import club.abelix.commons.command.CommandParams;
import club.abelix.commons.command.CommandWrapper;
import club.abelix.commons.command.condition.WrappedCompiledCommandCondition;
import club.abelix.commons.command.cooldown.WrappedCooldown;
import club.abelix.commons.command.exception.CommandTranslationException;
import club.abelix.commons.command.tabcomplete.Tabcompleter;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.util.time.Cooldown;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import lombok.Getter;
import lombok.NonNull;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class WrappedCommandExecutor implements Serializable {

    public static final long serialVersionUID = 348956695953L;
    private final int methodHash;
    private final CommandWrapper wrapper;
    private final ExecutorService threadExecutor;
    private final List<WrappedCompiledCommandCondition> conditions;
    private final CommandExecutor<ExecutionSession> executor;
    private final Tabcompleter[] tabcompleters;
    private final boolean withoutHelp;
    private final WrappedCooldown cooldown;
    private final boolean isLogsIgnored;

    private String localizationKey;

    public WrappedCommandExecutor(@NonNull CommandParams params) {
        this.methodHash = params.getMethodHash();
        this.wrapper = params.getWrapper();
        this.conditions = params.getConditions();
        this.threadExecutor = params.getThreadExecutor();
        this.withoutHelp = params.isWithoutHelp();
        this.cooldown = params.getCooldown();
        this.localizationKey = params.getLocalizationKey();
        this.tabcompleters = params.getTabcompleters();
        this.isLogsIgnored = params.isLogsIgnored();

        CommandExecutor<ExecutionSession> executor = params.getExecutor();

        if (cooldown.getValue() > 0) {
            String key = getPropertyKey();

            this.executor = session -> {
                if (session.isConsole()) {
                    executor.execute(session);

                    return;
                }

                IGamer gamer = session.getGamer();
                Cooldown gamerCooldown = gamer.getCooldown(key);

                if (gamerCooldown.isActive()) {
                    throw new CommandTranslationException(
                            "command_unavailable_cooldown",
                            "%time%", Duration.durationWordsSec(session.getLanguage(), gamerCooldown.getAvailableAt() - TimeUtil.getNow())
                    );
                }

                gamer.setCooldown(key, cooldown.getValue(), TimeUnit.MILLISECONDS);

                executor.execute(session);
            };
        } else {
            this.executor = executor;
        }
    }

    public boolean hasTabcompleters() {
        return tabcompleters != null;
    }

    protected String getPropertyKey() {
        String key = asKey();
        String serverName = wrapper.getCommandManager().getServerName();

        if (serverName != null && cooldown.isCurrentServerOnly()) {
            key = serverName + "/" + key;
        }

        return key;
    }

    protected String asKey() {
        return wrapper.getName();
    }

    protected String asLocalizationKey() {
        return "command_" + wrapper.getName();
    }

    /**
     * Возвращает ключ локализации в формате command_$команда$(_$подкоманда$)
     */
    public String getLocalizationKey() {
        if (localizationKey == null) {
            localizationKey = asLocalizationKey();
        }

        return localizationKey;
    }

    @Override
    public String toString() {
        return "/" + wrapper.getName();
    }

}
