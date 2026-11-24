package club.abelix.api.base.gamer.event;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.commons.data.setting.Setting;
import lombok.Getter;

@Getter
public class GamerSettingToggleEvent extends GamerEvent {

    private final Setting setting;
    private final boolean state;

    public GamerSettingToggleEvent(Gamer gamer, Setting setting, boolean state) {
        super(true, gamer);

        this.setting = setting;
        this.state = state;
    }
}
