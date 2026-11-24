package club.abelix.commons.data.permission.prefix;

import lombok.NonNull;

/**
 * @author <a href="https://github.com/lesslyrose">PbIBa (milansky)</a> in November 2022
 */
public interface PrefixModifier {

    String modify(@NonNull String prefix);
}
