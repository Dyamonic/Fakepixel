package club.abelix.commons.util;

public interface ThrowableConsumer<Object, Cause extends Throwable> {

    void accept(Object object) throws Cause;

}
