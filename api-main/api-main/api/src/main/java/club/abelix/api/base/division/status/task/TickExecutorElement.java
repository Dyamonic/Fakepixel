package club.abelix.api.base.division.status.task;

import club.abelix.api.base.gamer.Gamer;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author pro0xy
 * <p>Created on 06.09.2023 at 00:14</p>
 */

@Data
@AllArgsConstructor
public class TickExecutorElement {
    Gamer gamer;
    Runnable tickExecute;
}
