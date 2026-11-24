package club.abelix.api.base.hotbar_new.builder.standard;

import club.abelix.api.base.hotbar_new.ActionType;
import club.abelix.api.base.hotbar_new.Hotbar;
import club.abelix.api.base.hotbar_new.HotbarClickAction;
import club.abelix.api.base.hotbar_new.HotbarIcon;
import club.abelix.api.base.hotbar_new.builder.HotbarBuilder;
import club.abelix.api.base.hotbar_new.builder.HotbarIconBuilder;
import club.abelix.api.base.hotbar_new.standard.StandardImmutableHotbar;
import club.abelix.api.base.hotbar_new.standard.StandardImmutableHotbarIcon;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StandardHotbarBuilder implements HotbarBuilder {

    final String hotbarId;
    final Map<Integer, StandardHotbarIconBuilder> iconBuilders = new HashMap<>();

    @Override
    public HotbarBuilder copyFrom(Collection<HotbarIcon> icons) {
        if (icons == null || icons.isEmpty()) return this;

        for (HotbarIcon original : icons) {
            if (original == null) continue;
            int s = original.slot();
            if (s < 0 || s > 8) throw new IllegalArgumentException("slot out of range: " + s);

            iconBuilders.put(s, new StandardHotbarIconBuilder(this, s) {
                @Override
                public StandardImmutableHotbarIcon toImmutable() {
                    EnumMap<ActionType, Consumer<HotbarClickAction>> fwd = new EnumMap<>(ActionType.class);

                    for (ActionType t : ActionType.values()) {
                        fwd.put(t, original::onClick);
                    }

                    return new StandardImmutableHotbarIcon(
                            original.slot(),
                            original::getItem,
                            fwd
                    );
                }
            });
        }
        return this;
    }
    @Override
    public HotbarIconBuilder slot(int slot) {
        return iconBuilders.computeIfAbsent(slot, s -> new StandardHotbarIconBuilder(this, slot));
    }

    @Override
    public Hotbar build() {
        Map<Integer, HotbarIcon> icons = new HashMap<>();
        iconBuilders.forEach((slot, b) -> icons.put(slot, b.toImmutable()));

        return new StandardImmutableHotbar(
                hotbarId,
                Map.copyOf(icons)
        );
    }
}
