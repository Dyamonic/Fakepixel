package club.abelix.core.app.command;

import club.abelix.commons.command.NamedCommand;
import club.abelix.commons.command.condition.WithCondition;
import club.abelix.commons.command.condition.WithPermission;
import club.abelix.commons.command.dependency.Dependency;
import club.abelix.commons.command.exception.CommandMessageException;
import club.abelix.commons.command.execution.Executor;
import club.abelix.commons.command.tabcomplete.Tabcomplete;
import club.abelix.commons.util.regex.Patterns;
import club.abelix.core.app.api.command.CoreExecutionSession;
import club.abelix.core.app.connection.server.ServerManager;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@NamedCommand("setproperty")
@RequiredArgsConstructor
public final class SetPropertyCommand {

    @Dependency
    private static ServerManager manager;

    @Executor
    @Tabcomplete("[playerName]")
    @WithPermission("command.setproperty")
    @WithCondition("minArgs: 3")
    public static void execute(final CoreExecutionSession session) throws CommandMessageException {
        Gamer target = session.getOfflineGamer(0, loadingGamer -> {
            loadingGamer.loadRanks();
            loadingGamer.loadPrefix();
            loadingGamer.loadProperties();
        });

        PropertyChangeMode selectedMode = null;

        int value = -1;

        for (PropertyChangeMode mode : PropertyChangeMode.values()) {
            try {
                value = mode.processArgument(session.getArgument(2));

                selectedMode = mode;

                break;
            } catch (IllegalArgumentException ignored) {

            }
        }

        if (selectedMode == null) {
            throw new CommandMessageException("Вы указали неверное значение");
        }

        String key = session.getArgument(1);

        selectedMode.apply(target, key, value);

        manager.broadcastStaff("command.staffchat", "core_setproperty_broadcast",
                "%admin%", session.getDisplayName(),
                "%key%", key,
                "%player%", target.getDisplayName(),
                "%value%", session.getArgument(2)
        );
    }

    private enum PropertyChangeMode {

        ADD_PROPERTY {
            @Override
            public int processArgument(@NonNull String argument) {
                boolean isAdding = argument.startsWith("+");

                if (isAdding) {
                    argument = argument.replace("+", "");
                }

                if (isAdding && Patterns.isValidInteger(argument)) {
                    return Integer.parseInt(argument);
                }

                throw new IllegalArgumentException("Invalid argument");
            }

            @Override
            public void apply(@NonNull Gamer gamer, @NonNull String key, int value) {
                gamer.addProperty(key, value);
            }
        },
        SUBSTRACT_PROPERTY {
            @Override
            public int processArgument(@NonNull String argument) {
                boolean isSubstracting = argument.startsWith("-");

                if (isSubstracting) {
                    argument = argument.replace("-", "");
                }

                if (isSubstracting && Patterns.isValidInteger(argument)) {
                    return Integer.parseInt(argument);
                }

                throw new IllegalArgumentException("Invalid argument");
            }

            @Override
            public void apply(@NonNull Gamer gamer, @NonNull String key, int value) {
                gamer.addProperty(key, -value);
            }
        },
        CHANGE_PROPERTY {
            @Override
            public int processArgument(@NonNull String argument) {
                if (Patterns.isValidInteger(argument)) {
                    return Integer.parseInt(argument);
                }

                throw new IllegalArgumentException("Invalid argument");
            }

            @Override
            public void apply(@NonNull Gamer gamer, @NonNull String key, int value) {
                gamer.setProperty(key, value);
            }
        };

        public abstract int processArgument(@NonNull String argument) throws IllegalArgumentException;

        public abstract void apply(@NonNull Gamer gamer, @NonNull String key, int value);

    }
}
