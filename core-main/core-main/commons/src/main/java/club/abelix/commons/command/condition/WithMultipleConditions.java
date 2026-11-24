package club.abelix.commons.command.condition;

/**
 * @author WhileInside
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @see WithCondition
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WithMultipleConditions {

    WithCondition[] value();

}
