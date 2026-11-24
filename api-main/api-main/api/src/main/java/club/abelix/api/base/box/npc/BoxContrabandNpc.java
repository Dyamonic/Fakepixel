package club.abelix.api.base.box.npc;

import club.abelix.api.base.box.gui.PlayerBoxesGui;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.shop.ShopNpc;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BoxContrabandNpc extends ShopNpc {

    public BoxContrabandNpc() {
        super(Material.GOLD_NUGGET, "box_contraband");
    }

    @Override
    public void clickAction(Player player) {
        new PlayerBoxesGui(Gamer.getGamer(player));
    }
}
