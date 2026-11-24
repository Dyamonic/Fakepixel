package club.abelix.commons.command.execution;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <br>Экзекутор подкоманды.
 * <br>
 * <br>См. также: {@link Executor}, {@link FallbackExecutor}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubcommandExecutor {

    /**
     * Название подкоманды
     */
    String value();

}
