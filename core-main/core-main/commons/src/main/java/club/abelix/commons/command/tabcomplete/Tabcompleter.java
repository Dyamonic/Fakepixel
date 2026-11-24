package club.abelix.commons.command.tabcomplete;

import club.abelix.commons.gamer.ICommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface Tabcompleter {

    List<String> complete(ICommandSender sender, String label);

    default Tabcompleter with(Tabcompleter another) {
        return (sender, label) -> {
            List<String> first = complete(sender, label);
            List<String> second = another.complete(sender, label);

            if (first == null && second == null) {
                return Collections.emptyList();
            }

            List<String> result = new ArrayList<>();

            if (first != null) {
                result.addAll(first);
            }

            if (second != null) {
                result.addAll(second);
            }

            return result.stream().sorted().limit(50)
                    .collect(Collectors.toList());
        };
    }

}
