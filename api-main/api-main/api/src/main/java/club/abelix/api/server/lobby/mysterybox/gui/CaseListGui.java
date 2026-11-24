package club.abelix.api.server.lobby.mysterybox.gui;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.lobby.mysterybox.MysteryBox;
import club.abelix.api.server.lobby.mysterybox.anim.SpinCaseOpener;
import club.abelix.api.server.lobby.mysterybox.item.CaseManager;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.word.CommonWords;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CaseListGui extends Gui {

    /**
     * Позиции кейсов
     */
    private static final int[][] SLOTS_POSITIONS = {
            {22},
            {21, 23},
            {20, 22, 24},
            {19, 21, 23, 25},
            {19, 21, 23, 25, 31},
            {19, 21, 23, 25, 29, 33},
            {19, 21, 23, 25, 29, 31, 33},
    };

    /**
     * Иконка для обводки в гуи
     */
    private static final ItemStack OUTLINE_ICON = ItemUtil.getBuilder(Material.STAINED_GLASS_PANE)
            .setDurability(7)
            .setName(" ")
            .build();

    private final AbstractPlugin plugin;

    private final MysteryBox mysteryBox;

    private final List<CaseType> caseTypeList;

    public CaseListGui(AbstractPlugin plugin, Player player, MysteryBox mysteryBox) {
        super(player, Lang.of("case_list_title"), 6);

        this.plugin = plugin;
        this.mysteryBox = mysteryBox;
        this.caseTypeList = mysteryBox.getCaseTypes();

        draw();
    }

    @Override
    public void draw() {
        drawOutlines();

        int[] slots = SLOTS_POSITIONS[caseTypeList.size() - 1];

        for (int i = 0; i < caseTypeList.size(); i++) {
            drawType(caseTypeList.get(i), slots[i]);
        }
    }

    public static ItemUtil.Builder getCaseTypeBuilder(Gamer gamer, CaseType caseType) {
        Language language = gamer.getLanguage();

        int total = gamer.getProperty(caseType.name());
        boolean have = total > 0;

        ItemUtil.Builder builder = ItemUtil.getBuilder(CaseManager.IMP.getIcon(caseType, language))
                .addLore(language.getMessage("case_purchase_keys_click",
                        "%total%", total,
                        "%cost%", caseType.getPlazmaCost()
                ));

        if (have) {
            builder.setAmount(total);
            builder.addLore(language.getMessage("case_open_click"));
        } else {
            builder.setType(Material.STAINED_GLASS);
            builder.setDurability(14);
        }

        return builder;
    }

    private void drawType(CaseType caseType, int slot) {
//        Gamer gamer = getGamer();
//
//        int total = gamer.getProperty(caseType.name());
//        boolean have = total > 0;
//
//        Group required = caseType.getRequiredGroup();
//        boolean canUse = required == null || gamer.getRanks().canApply(required);
//
//        add(getCaseTypeBuilder(getGamer(), caseType).build(), slot, clickEvent -> {
//            if (clickEvent.getClick() == ClickType.RIGHT) {
//                purchase(getGamer(), caseType);
//                return;
//            }
//
//            if (!canUse || !have) {
//                return;
//            }
//
//            if (mysteryBox.getCaseOpener() != null) {
//                gamer.sendLocalizedMessage("case_open_unavailable_processing");
//                return;
//            }
//
//            player.closeInventory();
//
//            SpinCaseOpener opener = new SpinCaseOpener(plugin, mysteryBox, gamer, caseType);
//
//            if (opener.open()) {
//                mysteryBox.setCaseOpener(opener);
//                gamer.addProperty(caseType.name(), -1);
//            }
//        });
    }

    public static void purchase(@NonNull Gamer gamer, @NonNull CaseType caseType) {

        Language language = gamer.getLanguage();

        int gold = gamer.getGold();

        if (gold < caseType.getPlazmaCost()) {
            gamer.sendLocalizedMessage("purchase_unavailable_cost_message");
            return;
        }

        gamer.getPlayer().closeInventory();

        gamer.takeGold(caseType.getPlazmaCost());
        gamer.addProperty(caseType.name(), 1);

        gamer.sendLocalizedMessage("case_purchase",
                "%name%", caseType.getDisplayName(language),
                "%cost%", CommonWords.PLAZMA.get(language).withNumber(caseType.getPlazmaCost())
        );
    }

    private void drawOutlines() {
        for (int x = 0; x < 8; x++) {
            add(OUTLINE_ICON, toSlot(x, 0));
            add(OUTLINE_ICON, toSlot(x, 5));
        }

        for (int y = 0; y < 6; y++) {
            add(OUTLINE_ICON, toSlot(0, y));
            add(OUTLINE_ICON, toSlot(8, y));
        }
    }
}
