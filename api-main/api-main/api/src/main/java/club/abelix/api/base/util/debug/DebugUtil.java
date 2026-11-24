package club.abelix.api.base.util.debug;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DebugUtil {

    public void stacktrace() {
        new Throwable().printStackTrace();
    }

    public void stacktrace(Object source, String comment) {
        System.out.println("THIS IS DEBUG MESSAGE");
        System.out.println("Details: ");
        System.out.printf(" Source  | %s@%s\n", source.getClass().getSimpleName(), Integer.toHexString(source.hashCode()));
        System.out.printf(" Comment | %s\n", comment);
        System.out.println("Stacktrace: ");
        stacktrace();
    }

}
