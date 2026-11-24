package club.abelix.api.nms.scoreboard.objective;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class BoardObjective {

    private final String name;

    @Setter
    private String displayName;

    private final ObjectiveType objectiveType;


}
