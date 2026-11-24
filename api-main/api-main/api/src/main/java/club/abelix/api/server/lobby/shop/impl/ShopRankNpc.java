package club.abelix.api.server.lobby.shop.impl;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.base.shop.ShopGuiFactory;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.api.server.lobby.shop.ShopNpc;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: fakepixel-api</p>
 * <p>StoreNpc creation on 30.06.2023 at 04:03</p>
 */

public class ShopRankNpc extends ShopNpc {

    public ShopRankNpc() {
        super(Material.EMERALD,"store");
    }

    @Override
    public void clickAction(Player player) {
        DiBukkitPlatform.withBeanOrElse(
                ShopGuiFactory.class,
                bean -> bean.open(Gamer.getGamer(player), null),
                () -> ShopUtil.open(Gamer.getGamer(player))
        );
    }
}
