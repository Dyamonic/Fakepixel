package club.abelix.api.nms.team.rule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CollisionRule {

    ALWAYS("always"),
    PUSH_OTHER_TEAMS("pushOtherTeams"),
    PUSH_OWN_TEAMS("pushOwnTeam"),
    NEVER("never");

    @Getter
    private final String ruleName;

}