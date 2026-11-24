package club.abelix.api.base.division.gui;

import club.abelix.api.base.division.Division;
import club.abelix.api.base.division.DivisionManager;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.network.type.mode.Mini;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Material;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>ModeDivisionList creation on 02.04.2024 at 23:36</p>
 */

@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class DivisionListGui extends Gui {

    final Mini mini;

    final PagedZone<Division> divisionPagedZone;

    public DivisionListGui(@NonNull Gamer gamer, Mini mini, @Nullable Gui parent) {
        super(gamer, Lang.of("division_gui_mode").set("%mode%", mini != null ? mini.getDisplayName() : ""), 6);

        this.mini = mini;
        setParent(parent);

        divisionPagedZone = new PagedZone<>(this) {
            @Override
            protected List<Division> getItems() {
                if (DivisionManager.IMP.isGlobalMode()) {
                    return DivisionManager.IMP.getDivisions();
                }

                return DivisionManager.IMP.getDivisions(mini);
            }

            @Override
            protected void drawItem(Division division, int slot) {
                Division primaryDivision;

                if (DivisionManager.IMP.isGlobalMode()) {
                    primaryDivision = DivisionManager.IMP.getBestDivision(gamer);
                } else {
                    primaryDivision = DivisionManager.IMP.getBestDivision(gamer, mini);
                }

                add(DivisionGuiUtil.getDivisionIcon(division, getGamer(), division.equals(primaryDivision)), slot,
                        clickEvent ->
                                new SingleDivisionGui(getGamer(), division, DivisionListGui.this).draw()
                );
            }
        };

        divisionPagedZone.addSlotRect(2, 3, 5, 1);
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        Division primaryDivision;

        if (DivisionManager.IMP.isGlobalMode()) {
            primaryDivision = DivisionManager.IMP.getBestDivision(gamer);
        } else {
            primaryDivision = DivisionManager.IMP.getBestDivision(gamer, mini);
        }
        add(DivisionGuiUtil.getDivisionIcon(primaryDivision, getGamer(), true), 12);

        val selectedStar = DivisionManager.IMP.getSelectedStar(gamer);
        if (selectedStar != null) {
            add(DivisionGuiUtil.getStarIcon(selectedStar, getGamer()), 14,
                    clickEvent ->
                            new SingleDivisionGui(getGamer(), primaryDivision, DivisionListGui.this).draw());
        } else {
            add(ItemUtil.getBuilder(Material.GLASS_BOTTLE)
                    .setName(getGamer().getLanguage().getMessage("division_star_empty"))
                    .build(), 14, clickEvent -> {

                if (DivisionManager.IMP.getDivisionStars().isEmpty()) {
                    return;
                }

                new SingleDivisionGui(getGamer(), primaryDivision, DivisionListGui.this).draw();
            });
        }

        divisionPagedZone.draw();
        divisionPagedZone.drawButtons(28, 34);


        if (getParent() != null) {
            generateBackItem(49, clickEvent -> getParent().reopen());
        }
    }
}
