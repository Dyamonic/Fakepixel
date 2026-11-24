package club.abelix.commons.command.execution;

import club.abelix.commons.command.CommandParams;
import lombok.Getter;
import lombok.NonNull;

@Getter
public class WrappedSubcommandExecutor extends WrappedCommandExecutor {

    private final String[] names;

    public WrappedSubcommandExecutor(@NonNull String[] names, @NonNull CommandParams params) {
        super(params);

        this.names = names;
    }

    @Override
    protected String asKey() {
        return super.asKey() + "." + names[0];
    }

    @Override
    public String asLocalizationKey() {
        return super.asLocalizationKey() + "_" + names[0];
    }

    @Override
    public String toString() {
        return super.toString() + " " + names[0];
    }
}
