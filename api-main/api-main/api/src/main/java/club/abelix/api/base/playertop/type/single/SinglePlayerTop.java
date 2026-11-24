package club.abelix.api.base.playertop.type.single;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.api.base.playertop.type.PlayerTop;
import club.abelix.api.base.playertop.view.HologramPlayerTopView;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.commons.language.Language;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class SinglePlayerTop extends PlayerTop<SinglePlayerTop.SinglePlayerTopView> {

    private static final double Y_INITIAL_OFFSET = -0.5 + 1.9;
    private static final double Y_DELTA = 0.3;

    // ---------------------------------------------------------------------

    /**
     * Базовая локация топа игроков
     */
    private final Location topLocation;

    private final String localizationKey;

    public SinglePlayerTop(@NonNull Location topLocation,
                           @NonNull String localizationKey,
                           int maxPlayers) {

        super(maxPlayers);

        this.topLocation = topLocation;
        this.localizationKey = localizationKey;
    }

    @Override
    protected SinglePlayerTopView createPlayerTopView(@NonNull Gamer gamer) {
        return new SinglePlayerTopView(gamer);
    }

    protected String getDisplayName(@NonNull Gamer gamer) {
        return gamer.getColoredNameAndSuffix();
    }

    public class SinglePlayerTopView extends HologramPlayerTopView {

        public SinglePlayerTopView(Gamer gamer) {
            super(gamer);
        }

        @Override
        public void create() {
            Player player = gamer.getPlayer();

            Language language = gamer.getLanguage();

            Location loc = topLocation.clone().add(0, Y_INITIAL_OFFSET, 0);

            Hologram typeHologram = new Hologram(loc.clone().add(0, Y_DELTA * 2, 0));
            typeHologram.addReceiver(player);

            typeHologram.addTextLine(
                    language.getMessage(
                            "prop_player_top_title",
                            "%count%", maxPlayers
                    )
            );

            typeHologram.addTextLine(
                    language.getMessage(
                            "prop_player_top_subtitle",
                            "%type%", language.getMessage(localizationKey)
                    )
            );

            addHologram(typeHologram);

            loc.add(0, -Y_DELTA, 0);

            Hologram valueHologram = new Hologram(loc);
            valueHologram.addReceiver(player);

            CachedEntryList playerTop = getPlayerTop();

            if (playerTop == null) {
                return;
            }

            List<PlayerTopEntry> entries = playerTop.getPlayerTopEntries();

            for (int place = 0; place < maxPlayers; place++) {
                PlayerTopEntry entry = entries.size() > place ? entries.get(place) : null;

                String value;

                String topEntryKey = "player_top_entry";

                if (entry == null) {
                    value = language.getMessage("player_top_empty_entry");
                } else {
                    Gamer gamer = entry.getGamer();

                    value = language.getMessage(
                            "player_top_player_entry",
                            "%name%", gamer.getDisplayName(),
                            "%value%", playerTop.getInfoProvider().format(entry, language)
                    );

                    if (entry.getGamer().equals(this.gamer)) {
                        topEntryKey += "_you";
                    }
                }

                valueHologram.addTextLine(
                        language.getMessage(
                                topEntryKey,
                                "%place%", place + 1,
                                "%player%", value
                        )
                );
            }

            addHologram(valueHologram);
        }
    }

}
