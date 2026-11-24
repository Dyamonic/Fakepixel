package club.abelix.games.fastbridge.game.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.commons.island.IslandSelector;
import club.abelix.games.fastbridge.commons.island.IslandService;
import club.abelix.games.fastbridge.commons.utils.WorldUtility;
import club.abelix.games.fastbridge.cosmetic.api.gui.CosmeticCategoryList;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;

import java.util.ArrayList;

public class WorldChangeGui extends Gui {

    private static final IslandSelector islandSelector = FastBridgeGame.getInstance().getIslandSelector();
    private static final IslandService islandService = FastBridgeGame.getInstance().getIslandService();

    public WorldChangeGui(@NonNull Gamer gamer) {
        super(gamer.getPlayer(), gamer.getLanguage().getMessage("fast_bridge_world_change_gui_name"),
                ONE_SLOT_BORDER_SLOTS_PROVIDER.getRawCountForItems(islandSelector.getIslandConfigs().size()) + 1);
    }

    @Override
    public void draw() {
        val list = new ArrayList<>(islandSelector.getIslandsByConfig().keySet());

        for (int i = 0; i < list.size(); i++) {
            val index = ONE_SLOT_BORDER_SLOTS_PROVIDER.getSlotForItem(i, list.size());
            val islandConfig = list.get(i);
            add(islandConfig.getIcon(gamer), index, __ -> {
                val fbGamer = FastBridgeGamer.get(gamer);
                if (fbGamer.getIsland() != null && fbGamer.getIsland().getRawConfig() == islandConfig) return;
                if (!WorldUtility.canJoinWorld(islandConfig)) {
                    gamer.sendLocalizedMessage("fast_bridge_teleport_impossible");
                    return;
                }
                islandService.switchIsland(fbGamer, islandConfig);
            });
        }

        add(ItemUtil.getBuilder(Material.DIAMOND_BLOCK).setName(
                getLanguage().getMessage("fastbridge_shop")
        ).build(), toSlot(3, 3), __ ->
                new CosmeticCategoryList(getGamer(), this).draw());

        add(ItemUtil.getBuilder(Material.GOLD_BLOCK).setName(
                getLanguage().getMessage("fastbridge_exchanger")
        ).build(), toSlot(5, 3), __ -> ShopUtil.openExchanger(getGamer()));
    }
}
