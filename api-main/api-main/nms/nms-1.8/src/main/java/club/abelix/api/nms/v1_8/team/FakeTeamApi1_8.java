package club.abelix.api.nms.v1_8.team;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.team.FakeTeam;
import club.abelix.api.nms.team.FakeTeamApiBase;

public class FakeTeamApi1_8 extends FakeTeamApiBase {

    public FakeTeamApi1_8(NmsApi nmsApi) {
        super(nmsApi);
    }

    @Override
    public FakeTeam createTeam(String name) {
        return new FakeTeam1_8(nmsApi, name);
    }

    @Override
    public FakeTeam createTeam(String name, String prefix, String suffix) {
        return new FakeTeam1_8(nmsApi, name, prefix, suffix);
    }

}
