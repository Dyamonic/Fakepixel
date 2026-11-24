package club.abelix.api.base.playertop.type.statistic;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.api.base.playertop.identifier.StatisticTopIdentifier;
import club.abelix.api.base.playertop.provider.StatisticTopProvider;
import club.abelix.api.base.playertop.type.PlayerTop;
import club.abelix.api.base.playertop.view.HologramPlayerTopView;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.commons.data.stat.StatisticPeriod;
import club.abelix.commons.data.stat.StatsField;
import club.abelix.commons.data.stat.StatsMode;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.iterator.Scroller;
import club.abelix.network.common.logging.Logging;
import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

public class StatisticPlayerTop extends PlayerTop<StatisticPlayerTop.StatisticPlayerTopView> {

    private static final double Y_INITIAL_OFFSET = -0.5 + 2.2;
    private static final double Y_DELTA = 0.3;

    // ---------------------------------------------------------------------

    /**
     * Режимы, по которым можно будет просматривать топ
     */
    private final StatisticTopMode[] modes;

    /**
     * Периоды, по которым можно будет просматривать топ
     */
    private final StatisticPeriod[] periods;

    /**
     * Поля, по которым можно будет просматривать топ
     */
    private final StatisticTopField[] fields;

    /**
     * Локация переключателя режимов
     */
    private final Location modeChangerLocation;

    public StatisticPlayerTop(Location modeChangerLocation,
                              @NonNull Collection<StatisticPeriod> periods,
                              @NonNull Collection<StatisticTopMode> modes,
                              @NonNull Collection<StatisticTopField> fields,
                              int maxPlayers) {

        super(maxPlayers);

        this.modeChangerLocation = modeChangerLocation;

        this.periods = periods.toArray(new StatisticPeriod[0]);
        this.modes = modes.toArray(new StatisticTopMode[0]);
        this.fields = fields.toArray(new StatisticTopField[0]);

        Preconditions.checkArgument(periods.size() > 0, "At least 1 period must be specified");
        Preconditions.checkArgument(modes.size() > 0, "At least 1 mode must be specified");
        Preconditions.checkArgument(fields.size() > 0, "At least 1 field must be specified");

        for (StatisticTopField field : fields) {
            modes.removeIf(mode -> mode.statsMode.getOptionalField(field.fieldName) == null);

            for (StatisticTopMode mode : modes) {
                StatsField statsField = mode.statsMode.getOptionalField(field.fieldName);

                for (StatisticPeriod period : periods) {
                    register(
                            new StatisticTopIdentifier(statsField, period),
                            new StatisticTopProvider(statsField, period, maxPlayers)
                    );
                }
            }
        }
    }

    @Override
    protected StatisticPlayerTopView createPlayerTopView(@NonNull Gamer gamer) {
        return new StatisticPlayerTopView(gamer);
    }

    @RequiredArgsConstructor
    public static class StatisticTopField {

        private final Location location;

        private final String localizationKey;
        private final String fieldName;

    }

    @RequiredArgsConstructor
    public static class StatisticTopMode {

        private final StatsMode statsMode;
        private final String displayLocalizationKey;

        public String getDisplayName(@NonNull Language language) {
            return displayLocalizationKey == null ?
                    statsMode.getDisplayName() :
                    language.getMessage(displayLocalizationKey);
        }

    }

    public class StatisticPlayerTopView extends HologramPlayerTopView {

        /**
         * Параметры отображения топа - текущий период по конкретному полю
         */
        private final Map<StatisticTopField, Scroller<StatisticPeriod>> periodScrollers = new HashMap<>();
        /**
         * Параметры отображения топа - текущий режим
         */
        private Scroller<StatisticTopMode> modeScroller = new Scroller<>(3, modes);

        public StatisticPlayerTopView(Gamer gamer) {
            super(gamer);
        }

        Scroller<StatisticPeriod> getPeriodScroller(@NonNull StatisticTopField field) {
            return periodScrollers.computeIfAbsent(field, k -> new Scroller<>(2, periods));
        }

        StatisticPeriod rollPeriodIndex(@NonNull StatisticTopField field) {
            Scroller<StatisticPeriod> scroller = getPeriodScroller(field);

            scroller.scroll(true);

            return scroller.getSelectedElement();
        }

        StatisticTopMode rollModeIndex() {
            modeScroller.scroll();

            return modeScroller.getSelectedElement();
        }

        @Override
        public void create() {
            StatisticTopMode currentMode = modeScroller.getSelectedElement();

            Language language = gamer.getLanguage();

            // топы игроков по полям
            for (StatisticTopField field : fields) {
                Scroller<StatisticPeriod> periodScroller = getPeriodScroller(field);
                StatisticPeriod currentPeriod = periodScroller.getSelectedElement();

                Location loc = field.location.clone().add(0, Y_INITIAL_OFFSET, 0);

                Hologram typeHologram = createHologram(loc.clone().add(0, Y_DELTA * 2, 0));

                typeHologram.addTextLine(language.getMessage(
                        "stat_player_top_title",
                        "%mode%", currentMode.getDisplayName(language)
                ));

                typeHologram.addTextLine(language.getMessage(
                        "stat_player_top_subtitle",
                        "%field%",
                        language.getMessage(field.localizationKey),
                        "%timestamp%",
                        currentPeriod.getDisplayName(language).toLowerCase()
                ));

                addHologram(typeHologram);

                loc.add(0, -Y_DELTA, 0);

                Hologram valueHologram = createHologram(loc);

                StatsField statsField = currentMode.statsMode.getOptionalField(field.fieldName);

                CachedEntryList playerTop = getPlayerTop(new StatisticTopIdentifier(statsField, currentPeriod));

                List<PlayerTopEntry> entries = playerTop == null ?
                        Collections.emptyList() :
                        playerTop.getPlayerTopEntries();

                for (int place = 0; place < maxPlayers; place++) {
                    PlayerTopEntry entry = entries.size() > place ? entries.get(place) : null;

                    String value;

                    String topEntryKey = "player_top_entry";

                    if (entry == null) {
                        value = language.getMessage("player_top_empty_entry");
                    } else {
                        Gamer gamer = entry.getGamer();

                        if (gamer == null) {
                            Logging.IMP.error("[Top] Failed generate place, because gamer is null! ({})", entry.toString());
                            continue;
                        }

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

                if (periods.length == 1) {
                    continue;
                }

                StringBuilder timestamps = new StringBuilder();

                for (StatisticPeriod period : periodScroller.getVisibleElements()) {
                    if (timestamps.length() > 0) {
                        timestamps.append(" ");
                    }

                    String key = "stat_player_top_changer_value" + (period == currentPeriod ? "_active" : "");

                    timestamps.append(
                            language.getMessage(key, "%value%", period.getDisplayName(language))
                    );
                }

                Consumer<Player> timestampChangerAction = player -> {
                    StatisticPeriod nextPeriod = rollPeriodIndex(field);

                    if (nextPeriod == currentPeriod) {
                        return;
                    }

                    gamer.sendLocalizedMessage(
                            "stat_player_period_changed",
                            "%period%", nextPeriod.getDisplayName(language)
                    );

                    update();
                };

                Hologram timestampChangerHologram = createHologram(field.location.clone().add(0, -(maxPlayers / 2f) * Y_DELTA, 0));

                timestampChangerHologram.addTextLine(language.getMessage(
                        "stat_player_top_changer_scroll_horizontal",
                        "%values%", timestamps.toString()
                ));

                timestampChangerHologram.setClickAction(timestampChangerAction);
                valueHologram.setClickAction(timestampChangerAction);
            }

            if (modeChangerLocation == null || modes.length == 1) {
                return;
            }

            Hologram modeControl = createHologram(modeChangerLocation);
            modeControl.addTextLine(language.getMessage("stat_player_top_mode_changer_title"));
            modeControl.addTextLine("§f");

            for (StatisticTopMode mode : modeScroller.getVisibleElements()) {
                modeControl.addTextLine(
                        language.getMessage("stat_player_top_changer_value" + (mode == currentMode ? "_active" : ""),
                                "%value%",
                                mode.getDisplayName(language)
                        )
                );
            }

            modeControl.addTextLine(language.getMessage("stat_player_top_changer_scroll_vertical"));

            modeControl.setClickAction(clicker -> {
                StatisticTopMode nextMode = rollModeIndex();

                if (nextMode == currentMode) {
                    return;
                }

                gamer.sendLocalizedMessage(
                        "stat_player_mode_changed",
                        "%mode%", nextMode.getDisplayName(language)
                );

                update();
            });
        }
    }

}
