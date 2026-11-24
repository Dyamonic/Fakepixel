package club.abelix.api.nms.v1_12.bossbar;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.bossbar.AbstractFakeBossbarApi;
import club.abelix.api.nms.bossbar.FakeBossbar;

public class FakeBossbarApi1_12 extends AbstractFakeBossbarApi {

    public FakeBossbarApi1_12(NmsApi api) {
        super(api);
    }

    @Override
    protected FakeBossbar newBossbar(String title, float v) {
        return new FakeBossbar1_12(api, title, v);
    }
}
