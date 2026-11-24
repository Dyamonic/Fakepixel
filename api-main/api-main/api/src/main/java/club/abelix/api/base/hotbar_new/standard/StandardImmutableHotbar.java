package club.abelix.api.base.hotbar_new.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar_new.Hotbar;
import club.abelix.api.base.hotbar_new.HotbarIcon;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@EqualsAndHashCode(of = "id")
@RequiredArgsConstructor
public class StandardImmutableHotbar implements Hotbar {

    @Getter
    final String id;

    final Map<Integer, HotbarIcon> icons;

    @Override
    public @NotNull @UnmodifiableView List<@NotNull HotbarIcon> getIcons() {
        return List.copyOf(icons.values());
    }

    @Override
    public @NotNull Optional<@NotNull HotbarIcon> getIcon(@Range(from = 0, to = 8) int slot) {
        return Optional.ofNullable(icons.get(slot));
    }

    @Override
    public void apply(@NotNull Gamer gamer) {
        icons.forEach((slot, icon) -> {
            val item = icon.getItem(gamer);

            gamer.getPlayer().getInventory().setItem(slot, item);
        });
    }

}
