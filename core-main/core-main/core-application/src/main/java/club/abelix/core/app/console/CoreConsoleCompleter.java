package club.abelix.core.app.console;

import club.abelix.core.app.CoreApplication;
import club.abelix.core.app.api.command.ConsoleCommandSender;
import lombok.RequiredArgsConstructor;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author artem
 */
@RequiredArgsConstructor
public class CoreConsoleCompleter implements Completer {

    private final CoreApplication core;


    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        List<String> suggestions = core.tabComplete(ConsoleCommandSender.INSTANCE, "/" + line.line());

        if (suggestions == null) {
            suggestions = core.getCommands().stream().sorted().limit(50)
                    .collect(Collectors.toList());
        }

        candidates.addAll(suggestions.stream()
                .map(Candidate::new)
                .toList());
    }
}
