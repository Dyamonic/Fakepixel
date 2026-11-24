package club.abelix.api.base.division.gui;

import club.abelix.api.base.division.Division;
import club.abelix.api.base.division.DivisionManager;
import club.abelix.api.base.division.DivisionStar;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.stat.StatsManager;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.bukkit.Material;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>SingleDivisionGui creation on 02.04.2024 at 21:26</p>
 */

@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SingleDivisionGui extends Gui {

    final Division division;
    final PagedZone<DivisionStar> starsZone;

    public SingleDivisionGui(@NonNull Gamer gamer, @NonNull Division division, @Nullable Gui parent) {
        super(gamer, Lang.of("division_gui_single"), 6);

        this.division = division;
        this.setParent(parent);

        starsZone = new PagedZone<>(this) {
            @Override
            protected Collection<DivisionStar> getItems() {
                return DivisionManager.IMP.getDivisionStars();
            }

            @Override
            protected void drawItem(DivisionStar star, int slot) {
                val item = DivisionGuiUtil.getStarIcon(star, getGamer());


                if (!star.isPurchased(gamer)) {
                    add(item, slot, (clickEvent) -> {
                        star.processPurchase(gamer);
                        DivisionManager.IMP.activateDivisionStar(gamer, star, true);
                    });
                } else {
                    add(item, slot, (clickEvent) -> {
                        StatsMode statsMode = StatsManager.INSTANCE.getMode(CoreUtil.getMaster());

                        if (star.getMini() != null) {
                            statsMode = StatsManager.INSTANCE.getMode(star.getMini());
                        }

                        boolean canUse = star.check(gamer, statsMode);

                        if (canUse) {
                            DivisionManager.IMP.activateDivisionStar(gamer, star, true);
                        } else {
                            gamer.sendMessage("division_star_not_available");
                        }
                    });
                }

            }
        };

        starsZone.addSlotRect(2, 2, 5, 2);
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        val selectedDivision = DivisionManager.IMP.getSelectedDivision(gamer);
        add(DivisionGuiUtil.getDivisionIcon(division, getGamer(), division.equals(selectedDivision)), 13);

        if (!DivisionManager.IMP.getDivisionStars().isEmpty()) {
            starsZone.draw();
            starsZone.drawButtons(28, 34);
        } else {
            add(ItemUtil.getBuilder(Material.BARRIER)
                    .setName(getLanguage().getMessage("division_no_star")).build(),
                    22);
        }

        if (getParent() != null) {
            generateBackItem(49, clickEvent -> getParent().reopen());
        }
    }
}
