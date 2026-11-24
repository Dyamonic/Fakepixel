package club.abelix.core.app.console;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.ConsoleCommandSender;
import lombok.RequiredArgsConstructor;
import net.minecrell.terminalconsole.SimpleTerminalConsole;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

@RequiredArgsConstructor
public class CoreConsole extends SimpleTerminalConsole {

    private final CoreApplication core;

    @Override
    protected boolean isRunning() {
        return core.isRunning();
    }

    @Override
    protected void runCommand(String s) {
        core.executeCommand(ConsoleCommandSender.INSTANCE, "/" + s);
    }

    @Override
    protected void shutdown() {
        core.shutdown();
    }

    @Override
    protected LineReader buildReader(LineReaderBuilder builder) {
        try {
            System.out.println("OUT = " + System.out.getClass());
            System.err.println("ERR = " + System.err.getClass());

            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));

            Terminal terminal = TerminalBuilder.builder()
                    .system(false)
                    .streams(System.in, new PrintStream(new FileOutputStream(FileDescriptor.out)))
                    .build();

            builder.terminal(terminal);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return super.buildReader(builder
                .appName("AbelixCoreApplication")
                .completer(new CoreConsoleCompleter(core)));
    }

}
