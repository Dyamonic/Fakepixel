package club.abelix.api.server.lobby.donate;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.server.base.shop.ShopGuiFactory;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.language.Lang;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.NonNull;
import org.bukkit.Location;

import java.util.LinkedHashMap;

/**
 * @author ildar (quiu)
 * @since 4/15/2024 at 8:24 PM</p>
 * For Api project.
 */

public final class DonateHologram {

    private static final LinkedHashMap<Group, Integer> donateGroups = new LinkedHashMap<>() {{
        put(Group.IRON, 99);
        put(Group.GOLD, 265);
        put(Group.DELUXE, 515);
        put(Group.MASTER, 1190);
        put(Group.RUBIUM, 2940);
        put(Group.ULTRA, 225);
        put(Group.SPONSOR, 9999);
    }};

    public DonateHologram(@NonNull final Location location) {
        Hologram hologram = new Hologram(location);

        hologram.addTextLine(Lang.of("donate_hologram_info").compile());

        donateGroups.forEach((group, cost) -> hologram.addTextLine(
                Lang.of("donate_hologram_line")
                        .set("%group%", group.getDisplayName())
                        .set("%cost%", cost)
                        .compile()));

        hologram.addTextLine(Lang.of("donate_hologram_website").compile());
        hologram.setPublicStatus(true);
        hologram.setClickAction(player -> {
            DiBukkitPlatform.withBeanOrElse(
                    ShopGuiFactory.class,
                    bean -> bean.open(Gamer.getGamer(player), null),
                    () -> ShopUtil.open(Gamer.getGamer(player))
            );
        });
    }
}
