package club.abelix.api.base.division.gui;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.division.DivisionManager;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.base.util.item.ModeIcons;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.commons.language.Lang;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>DivisionModeGui creation on 02.04.2024 at 23:56</p>
 */
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class DivisionModeGui extends Gui {

    PagedZone<Mini> divisionPagedZone;


    public DivisionModeGui(@NonNull Gamer gamer, @Nullable Gui parent) {
        super(gamer, Lang.of("division_gui_modes"), 6);

        setParent(parent);

        divisionPagedZone = new PagedZone<>(this) {
            @Override
            protected Collection<Mini> getItems() {
                return CoreUtil.getMaster().getMinis();
            }

            @Override
            protected void drawItem(Mini mini, int slot) {
                val item = ItemUtil.getBuilder(ModeIcons.get(mini))
                        .setName(gamer.getLanguage().getMessage("division_gui_mode", "%mode%", mini.getDisplayName()))
                        .addLore(gamer.getLanguage().getMessage("division_gui_mode_lore", "%mode%", mini.getDisplayName(), "%active_division%", DivisionManager.IMP.getBestDivision(gamer, mini)))
                        .build();

                add(item, slot, clickEvent -> {
                    if (clickEvent.isLeftClick()) {
                        new DivisionListGui(gamer, mini, DivisionModeGui.this).draw();
                    } else {
                        val bestCategoryDivision = DivisionManager.IMP.getBestDivision(gamer, mini);
                        if (bestCategoryDivision != null) {
                            DivisionManager.IMP.activateDivision(gamer, bestCategoryDivision, mini, false);
                            FalconAPI.getNmsApi().getSoundApi().playSound(gamer.getPlayer(), YSound.VILLAGER_YES);
                            reopen();
                        }
                    }
                });
            }
        };

        divisionPagedZone.addSlotRect(1, 2, 7, 3);
    }


    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        ItemStack selectedDivisionItem;
        val selectedDivision = DivisionManager.IMP.getSelectedDivision(gamer);
        if (selectedDivision == null) {
            selectedDivisionItem = ItemUtil.getBuilder(Material.GLASS_BOTTLE)
                    .setName(getLanguage().getMessage("division_gui_no_selected"))
                    .setLore(getLanguage().getMessage("division_gui_no_selected_lore"))
                    .build();
        } else {
            selectedDivisionItem = DivisionGuiUtil.getDivisionIcon(selectedDivision, gamer, true);
        }

        add(selectedDivisionItem, 13);

        divisionPagedZone.draw();
        divisionPagedZone.drawButtons(46, 52);


        if (getParent() != null) {
            generateBackItem(49, clickEvent -> getParent().reopen());
        }
    }
}
