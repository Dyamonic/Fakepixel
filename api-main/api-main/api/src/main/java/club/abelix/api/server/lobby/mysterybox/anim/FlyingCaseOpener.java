package club.abelix.api.server.lobby.mysterybox.anim;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.server.lobby.mysterybox.MysteryBox;
import club.abelix.commons.data.cases.CaseType;
import lombok.NonNull;

public class FlyingCaseOpener extends AbstractCaseOpener {

    public FlyingCaseOpener(@NonNull AbstractPlugin plugin,
                            @NonNull MysteryBox mysteryBox,
                            @NonNull Gamer gamer,
                            @NonNull CaseType caseType) {

        super(plugin, mysteryBox, gamer, caseType);
    }

    @Override
    protected boolean onTick() {
        return false;
    }
}
