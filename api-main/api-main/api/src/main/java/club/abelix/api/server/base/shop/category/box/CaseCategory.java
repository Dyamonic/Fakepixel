package club.abelix.api.server.base.shop.category.box;

import club.abelix.api.server.base.shop.ShopCategory;
import club.abelix.api.server.lobby.mysterybox.gui.CaseListGui;
import club.abelix.commons.data.cases.CaseType;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>CaseCategory creation on 09.03.2024 at 01:59</p>
 */

public class CaseCategory extends ShopCategory {
    public CaseCategory(@NonNull Player player, @NonNull ItemStack item) {
        super(player, "donate_description_gui_title", item);
        draw();
    }

    @Override
    public void drawCategory() {

        List<CaseType> caseTypes =
                Arrays.asList(CaseType.GOLD_CASE,
                        CaseType.DONATE_CASE,
                        CaseType.SUFFIX_CASE,
                        CaseType.EXPERIENCE_CASE);

        int[] slots = SLOTS_POSITIONS[caseTypes.size() - 1];
        for (int i = 0; i < caseTypes.size(); i++) {
            drawType(caseTypes.get(i), slots[i]);
        }
    }

    private void drawType(CaseType caseType, int slot) {
        add(CaseListGui.getCaseTypeBuilder(getGamer(), caseType).build(), slot, clickEvent -> {
            CaseListGui.purchase(getGamer(), caseType);
        });
    }

}
