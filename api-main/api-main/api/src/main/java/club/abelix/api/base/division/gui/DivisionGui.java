package club.abelix.api.base.division.gui;

import club.abelix.api.base.division.DivisionManager;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import lombok.NonNull;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;

/**
 * @author Egor Skorikov (Krashe85)
 * <p>Project: agera-api</p>
 * <p>DivisionGui creation on 03.04.2024 at 00:08</p>
 */

@UtilityClass
public class DivisionGui {
    public void openGui(@NonNull Gamer gamer, @Nullable Gui parent) {
        if (DivisionManager.IMP.isGlobalMode()) {
            new DivisionListGui(gamer, null, parent).draw();
        } else {
            new DivisionModeGui(gamer, parent).draw();
        }
    }

    public void openGui(@NonNull Gamer gamer) {
        openGui(gamer, null);
    }
}
