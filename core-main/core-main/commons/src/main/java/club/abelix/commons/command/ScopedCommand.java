package club.abelix.commons.command;

import club.abelix.commons.project.Project;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScopedCommand {

    Project value() default Project.ABELIX_MC;

}
