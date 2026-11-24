package club.abelix.api.base.hotbar_new.builder;

import club.abelix.api.base.hotbar_new.Hotbar;
import club.abelix.api.base.hotbar_new.HotbarIcon;
import club.abelix.api.base.hotbar_new.builder.standard.StandardHotbarBuilder;

import java.util.Collection;

public interface HotbarBuilder {

    static HotbarBuilder of(String id) {
        return new StandardHotbarBuilder(id);
    }

    HotbarBuilder copyFrom(Collection<HotbarIcon> icons);

    HotbarIconBuilder slot(int slot);

    Hotbar build();

}
