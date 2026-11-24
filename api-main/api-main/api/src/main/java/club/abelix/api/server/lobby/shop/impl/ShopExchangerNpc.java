package club.abelix.api.server.lobby.shop.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.api.server.lobby.shop.ShopNpc;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>ExchangerNpc creation on 10.03.2024 at 16:02</p>
 */

public class ShopExchangerNpc extends ShopNpc {

    public ShopExchangerNpc() {
        super(Material.GOLD_NUGGET, "exchanger");
    }

    @Override
    public void clickAction(Player player) {
    }
}
