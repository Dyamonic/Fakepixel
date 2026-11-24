package club.abelix.api.base.util.jmx;

import club.abelix.api.FalconAPI;

public class TPS implements TPSMBean {

    @Override
    public double getTPS() {
        return FalconAPI.getNmsApi().getTPS()[0];
    }
}
