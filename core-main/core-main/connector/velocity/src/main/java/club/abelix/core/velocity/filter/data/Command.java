package club.abelix.core.velocity.filter.data;

import lombok.Getter;

import java.util.Arrays;

@Getter
public class Command {

    private final String command;
    private final String[] allArgs;
    private final String[] staticArgs;

    public Command(String command) {
        this.command = command;

        this.allArgs = command.split(" +");

        String[] args = new String[allArgs.length];

        int i = 0;


        for (String arg : allArgs) {
            if (arg.equals("{arg}")) {
                break;
            }
            args[i] = arg;
            i++;
        }

        this.staticArgs = Arrays.copyOf(args, i);
    }

}
