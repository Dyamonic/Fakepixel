package club.abelix.games.fastbridge.game.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.games.fastbridge.commons.FastBridgeGamer;
import club.abelix.games.fastbridge.commons.config.IslandConfig;
import club.abelix.games.fastbridge.commons.island.IslandSelector;
import club.abelix.games.fastbridge.commons.utils.RecordFormat;
import club.abelix.games.fastbridge.cosmetic.api.gui.CosmeticCategoryList;
import club.abelix.games.fastbridge.game.FastBridgeGame;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class DetailedBridgingStatsGui extends Gui {

    private static final IslandSelector islandSelector = FastBridgeGame.getInstance().getIslandSelector();

    public DetailedBridgingStatsGui(@NonNull Gamer gamer) {
        super(gamer.getPlayer(), gamer.getLanguage().getMessage("fast_bridge_detailed_stats_gui_title"),
                ONE_SLOT_BORDER_SLOTS_PROVIDER.getRawCountForItems(islandSelector.getIslandConfigs().size()) + 1);

        this.draw();
    }

    @Override
    public void draw() {
        val list = new ArrayList<>(islandSelector.getIslandsByConfig().keySet());
        val fbGamer = FastBridgeGamer.get(getGamer());

        for (int i = 0; i < list.size(); i++) {
            val index = ONE_SLOT_BORDER_SLOTS_PROVIDER.getSlotForItem(i, list.size());
            val islandConfig = list.get(i);
            add(createStatsItems(fbGamer, islandConfig), index);
        }

        add(ItemUtil.getBuilder(Material.DIAMOND_BLOCK).setName(
                getLanguage().getMessage("fastbridge_shop")
        ).build(), toSlot(3, 3), __ ->
                new CosmeticCategoryList(getGamer(), this).draw());

        add(ItemUtil.getBuilder(Material.GOLD_BLOCK).setName(
                getLanguage().getMessage("fastbridge_exchanger")
        ).build(), toSlot(5, 3), __ -> ShopUtil.openExchanger(getGamer()));
    }

    private ItemStack createStatsItems(
            final FastBridgeGamer fbGamer,
            final IslandConfig config
    ) {
        val gamer = fbGamer.getGamer();
        val itemBuilder = ItemUtil.getBuilder(config.getIcon(gamer));

        itemBuilder.setLore(
                gamer.getLanguage()
                        .getMessageList("fastbridge_detailed_stat_gui",
                                "%record%", RecordFormat.formatTime(fbGamer.getLifetimeBestTime(config)),
                                "%attempts%", fbGamer.getAttempts(config),
                                "%successAttempts%", fbGamer.getSuccessfulAttempts(config)
                        )

        );

        return itemBuilder.build();
    }
}
