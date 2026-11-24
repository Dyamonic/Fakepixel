package club.abelix.api.base.hotbar_new.compose;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.Hotbar;
import club.abelix.api.base.hotbar_new.HotbarIcon;
import club.abelix.api.base.hotbar_new.builder.HotbarBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HotbarComposer {

    public Hotbar compose(Hotbar base,
                          Gamer gamer,
                          List<HotbarIconProvider> providers) {
        Map<Integer, HotbarIcon> slots = new HashMap<>();
        base.getIcons().forEach(i -> slots.put(i.slot(), i));

        providers.stream()
                .filter(c -> c.appliesTo(gamer))
                .sorted(Comparator.comparingInt(HotbarIconProvider::priority).reversed())
                .forEach(c -> c.provide(gamer).forEach(icon -> {
                    int s = icon.slot();
                    if (!slots.containsKey(s)) {
                        slots.put(s, icon);
                        return;
                    }

                    log.info("Composing hotbar conflict at slot {} by {}", s, c);

                    switch (c.policy()) {
                        case REPLACE -> slots.put(s, icon);
                        case SKIP -> {
                        }
                        case ERROR -> throw new IllegalStateException(
                                "Slot conflict at " + s + " by " + c);
                    }
                }));

        return HotbarBuilder.of(base.getId())
                .copyFrom(slots.values())
                .build();
    }

}
