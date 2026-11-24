package club.abelix.api.nms.v1_8.bossbar;

import club.abelix.api.nms.NmsApi;
import club.abelix.api.nms.bossbar.AbstractFakeBossbarApi;
import club.abelix.api.nms.bossbar.FakeBossbar;

public class FakeBossbarApi1_8 extends AbstractFakeBossbarApi {

    public FakeBossbarApi1_8(NmsApi api) {
        super(api);
    }

    @Override
    protected FakeBossbar newBossbar(String title, float v) {
        return new FakeBossbar1_8(api, title, v);
    }

}
