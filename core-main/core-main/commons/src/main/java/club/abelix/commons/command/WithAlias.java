package club.abelix.commons.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Назначить алиас команде
 */
@Repeatable(WithMultipleAliases.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithAlias {

    /**
     * Алиас
     */
    String value();

}
