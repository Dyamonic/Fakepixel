package club.abelix.games.buildffa.game.receivable.entity.hologram;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.games.buildffa.game.division.AbstractDivision;
import club.abelix.games.buildffa.game.receivable.AbstractHologram;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class RatingHologram extends AbstractHologram {

    public RatingHologram(Location location) {
        super(location);
    }

    @Override
    public Hologram createHologram(@NotNull Player receiver) {

        val gamer = Gamer.getGamer(receiver);
        val hologram = new Hologram(hologramLocation());

        val content = createHologramContent(gamer);

        content.forEach(hologram::addTextLine);

        return hologram;
    }

    private List<String> createHologramContent(Gamer gamer) {

        val language = gamer.getLanguage();
        List<String> lines = new ArrayList<>();

        lines.add(language.getMessage("buildffa_division_hologram_name"));

        val template = language.getMessage("buildffa_division_hologram");

        for (AbstractDivision division : AbstractDivision.values()) {
            lines.add(
                    template.replace(
                            "%rating%",
                            String.valueOf(
                                    division.getRequiredRating()
                            )
                    )
                            .replace(
                                    "%division%",
                                    division.getName(language)
                            )
            );
        }

        return lines;
    }
}
