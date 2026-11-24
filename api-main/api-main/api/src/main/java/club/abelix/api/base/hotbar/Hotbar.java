package club.abelix.api.base.hotbar;

import club.abelix.api.base.gamer.Gamer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

@RequiredArgsConstructor
@Getter
public class Hotbar {

    private final String id;
    private final Icon[] icons = new Icon[9];

    public void apply(Player player) {
        apply(Gamer.getGamer(player));
    }

    public void apply(Gamer gamer) {
        Player player = gamer.getPlayer();

        if (player == null || !player.isOnline()) {
            return;
        }

        PlayerInventory inv = player.getInventory();

        for (int slot = 0; slot < 9; slot++) {
            Icon icon = this.icons[slot];

            inv.setItem(slot, icon != null ? icon.getIcon().apply(gamer) : null);
        }

        player.updateInventory();
    }

}
