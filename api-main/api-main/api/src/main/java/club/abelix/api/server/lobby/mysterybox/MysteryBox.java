package club.abelix.api.server.lobby.mysterybox;

import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.server.lobby.mysterybox.anim.AbstractCaseOpener;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.language.Lang;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class MysteryBox {

    @Getter
    private final Location location;

    @Getter
    private AbstractCaseOpener caseOpener;

    @Getter
    @Setter
    private List<CaseType> caseTypes = Arrays.asList(CaseType.DONATE_CASE,
            CaseType.SUFFIX_CASE, CaseType.EXPERIENCE_CASE, CaseType.GOLD_CASE);

    @Getter
    @Setter
    private boolean mysteryBoxCraft;

    private Hologram hologram;

    public void create() {
        Block block = location.getBlock();

        if (block.getType() != Material.ENDER_CHEST) {
            block.setType(Material.ENDER_CHEST);
        }

        createHologram();
    }

    public void setCaseOpener(AbstractCaseOpener caseOpener) {
        this.caseOpener = caseOpener;

        if (caseOpener == null) {
            createHologram();
        } else {
            hologram.remove();
        }
    }

    private void createHologram() {
        hologram = new Hologram(location.clone().add(0.5, 0.3, 0.5));
        hologram.setPublicStatus(true);
        hologram.addTextLine(Lang.of("mysterybox_hologram_title").compile());
        hologram.addTextLine(Lang.of("mysterybox_hologram_title_action").compile());
    }
}
