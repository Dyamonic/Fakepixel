package club.abelix.network.util;

/**
 * @author Unidentified Person
 */
public final class FastStrings {

    private final static ThreadLocal<StringBuilder> CACHE = ThreadLocal.withInitial(StringBuilder::new);

    private FastStrings() {
        throw new UnsupportedOperationException();
    }

    public static StringBuilder getEmptyBuilder() {
        StringBuilder sb = CACHE.get();
        sb.setLength(0);

        return sb;
    }

}
