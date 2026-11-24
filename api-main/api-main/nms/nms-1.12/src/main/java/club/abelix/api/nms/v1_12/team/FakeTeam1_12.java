package club.abelix.api.nms.v1_12.team;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.team.FakeTeamBase;
import club.abelix.api.nms.team.rule.CollisionRule;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class FakeTeam1_12 extends FakeTeamBase {

    private CollisionRule collisionRule = CollisionRule.NEVER;

    public FakeTeam1_12(@NonNull NmsApi nmsApi, @NonNull String name) {
        super(nmsApi, name);
    }

    public FakeTeam1_12(@NonNull NmsApi nmsApi, @NonNull String name, @NonNull String prefix, @NonNull String suffix) {
        super(nmsApi, name, prefix, suffix);
    }

    @Override
    public void setCollisionRule(CollisionRule collisionRule) {
        if (this.collisionRule == collisionRule) return;

        this.collisionRule = collisionRule;

        update();
    }

}
