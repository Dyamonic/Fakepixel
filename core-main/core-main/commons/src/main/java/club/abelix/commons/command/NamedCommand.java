package club.abelix.commons.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <br>Главная аннотация для команды
 * <br>
 * <br>Команда регистрируется в {@link CommandManager}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NamedCommand {

    String value();

}
