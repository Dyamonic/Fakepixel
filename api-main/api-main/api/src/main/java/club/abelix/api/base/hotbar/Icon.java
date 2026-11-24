package club.abelix.api.base.hotbar;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
@Getter
public class Icon {

    private final Function<Gamer, ItemStack> icon;
    private final Consumer<Gamer> action;

}
