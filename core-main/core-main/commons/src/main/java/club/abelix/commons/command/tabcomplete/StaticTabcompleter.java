package club.abelix.commons.command.tabcomplete;

import club.abelix.commons.gamer.ICommandSender;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.List;

public class StaticTabcompleter extends AbstractTabcompleter {

    private final String[] variants;

    public StaticTabcompleter(String... variants) {
        Preconditions.checkNotNull(variants, "variants is null");
        Preconditions.checkState(variants.length != 0, "variants is empty");

        this.variants = variants;
    }

    @Override
    public List<String> complete(ICommandSender sender, String label) {
        return any(label, Arrays.stream(variants));
    }

}
