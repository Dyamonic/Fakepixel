package club.abelix.core.velocity.auth.task;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>AuthTaskUnit creation on 03.03.2023 at 00:38</p>
 */

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(
        of = {"taskIdentification"}
)
public class AuthTaskUnit {
    // Тк second изменяется, это нужно для идентификации юнита в сете
    String taskIdentification;
    Runnable runnable;
    int seconds;
}
