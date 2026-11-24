package club.abelix.games.buildffa.game.receivable.entity.phantom;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.language.Lang;
import club.abelix.games.buildffa.game.config.FakePlayerConfig;
import club.abelix.games.buildffa.game.gui.standard.AdditionalItemGui;
import club.abelix.games.buildffa.game.gui.standard.SkinShopGui;
import club.abelix.games.buildffa.game.receivable.AbstractPhantom;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author milansky
 */
public final class ShopPhantom extends AbstractPhantom {
    public ShopPhantom(final @NotNull FakePlayerConfig config) {
        super(config);
    }

    @Override
    public Hologram createHologram(@NotNull Player receiver) {
        val hologram = new Hologram(hologramLocation());

        hologram.addTextLine(Lang.of("hologram_shop_info").compile());
        hologram.addTextLine(Lang.of("stats_npc_footer").compile());

        return hologram;
    }

    @Override
    public FakeEntityPlayer createPlayer(@NotNull Player receiver) {
        val fakePlayer = FalconAPI.getNmsApi().getFakeEntityApi().createPlayer(fakePlayerLocation(), skin());

        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(event -> new ShopGui(event.getPlayer()));

        return fakePlayer;
    }

    private static final class ShopGui extends Gui {
        private static final ItemStack ADDITIONAL_ITEMS_ITEMSTACK, SKINS_ITEMSTACK;

        static {
            ADDITIONAL_ITEMS_ITEMSTACK = ItemUtil.getBuilder(Material.GOLDEN_APPLE)
                    .setName(Lang.of("hologram_additional_shop_info"))
                    .build();

            SKINS_ITEMSTACK = ItemUtil.getBuilder(Material.SANDSTONE)
                    .setName(Lang.of("hologram_block_shop_info"))
                    .build();
        }

        public ShopGui(@NonNull Player player) {
            super(player, Lang.of("buildffa_shop_gui"), 4);

            draw();
        }

        @Override
        public void draw() {
            drawDynamicBorders();

            add(SKINS_ITEMSTACK, toSlot(2, 2), event -> new SkinShopGui(this, player));
            add(ADDITIONAL_ITEMS_ITEMSTACK, toSlot(6, 2), event -> new AdditionalItemGui(this, player));
        }
    }
}
