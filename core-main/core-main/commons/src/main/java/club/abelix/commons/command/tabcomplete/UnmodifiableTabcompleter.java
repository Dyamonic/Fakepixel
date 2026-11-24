package club.abelix.commons.command.tabcomplete;

import club.abelix.commons.gamer.ICommandSender;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * @author artem
 */
@RequiredArgsConstructor
public class UnmodifiableTabcompleter extends AbstractTabcompleter {

    private final String[] variants;

    @Override
    public List<String> complete(ICommandSender sender, String label) {
        return any(label, variants);
    }

}
