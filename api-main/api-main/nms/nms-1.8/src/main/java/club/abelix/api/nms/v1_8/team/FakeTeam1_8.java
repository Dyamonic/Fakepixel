package club.abelix.api.nms.v1_8.team;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.team.FakeTeamBase;
import club.abelix.api.nms.team.rule.CollisionRule;
import lombok.Getter;

@Getter
public class FakeTeam1_8 extends FakeTeamBase {

    public FakeTeam1_8(NmsApi nmsApi, String name) {
        super(nmsApi, name);
    }

    public FakeTeam1_8(NmsApi nmsApi, String name, String prefix, String suffix) {
        super(nmsApi, name, prefix, suffix);
    }

    @Override
    public CollisionRule getCollisionRule() {
        return CollisionRule.NEVER;
    }

    @Override
    public void setCollisionRule(CollisionRule collisionRule) {
        //method is unsupported on 1.8
    }

}
