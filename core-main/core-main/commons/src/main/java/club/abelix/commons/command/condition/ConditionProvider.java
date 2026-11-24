package club.abelix.commons.command.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author WhileInside
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionProvider {

    String value();

    // Priority <= 0 is reserved for system
    // do not use values lower than or eq 0
    // please
    int priority() default 1;

    boolean showInHelp() default true;

}
