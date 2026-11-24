package club.abelix.api.nms.team.rule;

import lombok.Getter;

public enum FakeTeamFlag {
    //Join
    SEND_TEAM_ON_JOIN,
    ADD_PLAYER_ON_JOIN,

    //Quit
    REMOVE_PLAYER_ON_QUIT;

    @Getter
    private final int mask = 1 << ordinal();
}