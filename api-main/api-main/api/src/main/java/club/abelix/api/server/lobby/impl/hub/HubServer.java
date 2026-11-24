package club.abelix.api.server.lobby.impl.hub;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamersession.GamerSessionManager;
import club.abelix.api.base.playertop.builder.PlayerTopBuilder;
import club.abelix.api.base.playertop.entry.PlayerTopEntry;
import club.abelix.api.base.playertop.provider.PropertyTopProvider;
import club.abelix.api.base.util.location.LocationUtil;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.board.BoardFactory;
import club.abelix.api.server.lobby.exclusive.newyear.NewYearHubListener;
import club.abelix.api.server.lobby.impl.hub.discount.DiscountApiService;
import club.abelix.api.server.lobby.impl.hub.discount.listener.DiscountListener;
import club.abelix.api.server.lobby.impl.hub.portal.HubPortalListener;
import club.abelix.api.server.lobby.impl.hub.present.PresentGamerSession;
import club.abelix.api.server.lobby.impl.hub.present.listener.PresentListener;
import club.abelix.api.server.lobby.impl.hub.present.storage.PresentStorage;
import club.abelix.api.server.lobby.impl.hub.present.storage.YamlPresentStorage;
import club.abelix.api.server.lobby.portal.PortalAnimation;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.gamer.IGamer;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.DurationField;
import club.abelix.commons.util.time.DurationFormatter;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import w.util.Env;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class HubServer extends BaseLobbyServer {

    public HubServer(AbstractPlugin plugin) {
        super(plugin);
    }

    @Override
    public void onExclusivePeriod() {
        if (Objects.requireNonNull(getExclusivePeriod()) == ExclusivePeriod.NEW_YEAR) {
            new NewYearHubListener(this.getPlugin());
//            new FireworkScheduler(this.getPlugin()).scheduleFireworkTask();
        }
    }

    @Override
    public void onLobbyLoad() {
        if (configuration == null) {
            return;
        }

        if (Env.findString("WEBSTORE_API_ENDPOINT").isPresent()) {
            plugin.getServer()
                    .getPluginManager()
                    .registerEvents(
                            new DiscountListener(new DiscountApiService(Env.getString("WEBSTORE_API_ENDPOINT"))),
                            plugin);
        }

        if (getConfiguration().getStringList("Presents") != null) {
            GamerSessionManager.INSTANCE.register(PresentGamerSession.class);

            PresentStorage storage = new YamlPresentStorage(getConfiguration());
            storage.load();

            plugin.getServer()
                    .getPluginManager()
                    .registerEvents(new PresentListener(storage), plugin);
        }

        String experienceTopLocationString = configuration.getString("ExperienceTopLocation");
        String timeTopLocationString = configuration.getString("TimeTopLocation");


        if (experienceTopLocationString != null) {
            Location expTopLocation = LocationUtil.stringToLocation(experienceTopLocationString);

            PlayerTopBuilder.defaultTopBuilder()
                    .provider(new LevelTopProvider())
                    .localizationKey("level_player_top_name")
                    .location(expTopLocation)
                    .create();
        }

        if (timeTopLocationString != null) {
            Location timeTopLocation = LocationUtil.stringToLocation(timeTopLocationString);

            PlayerTopBuilder.defaultTopBuilder()
                    .provider(new TimePlayedTopProvider())
                    .localizationKey("time_player_top_name")
                    .location(timeTopLocation)
                    .create();
        }

        //портал в хабе
        String portalCenterLocationString = configuration.getString("PortalCenter");

        if (portalCenterLocationString != null) {
            Location portalCenterLocation = LocationUtil.stringToLocation(portalCenterLocationString);

            new PortalAnimation(
                    plugin, portalCenterLocation, Lang.of("start_game_random_gamemode").compile()
            );

            new HubPortalListener();
        }
    }

    @Override
    public void sendBoard(@NonNull Gamer gamer) {
        DiBukkitPlatform.getFactory().getBeanService().withBean(BoardFactory.class, f -> f.createBoard(gamer));
    }

    public static class LevelTopProvider extends PropertyTopProvider {

        public LevelTopProvider() {
            super("Experience", 10, false);
        }

        public String format(PlayerTopEntry entry, Language language) {
            return String.valueOf(Leveling.getLevel(entry.getValue()));
        }
    }

    public static class TimePlayedTopProvider extends PropertyTopProvider {

        public TimePlayedTopProvider() {
            super(IGamer.GLOBAL_TIME_PROPERTY, 10, false);
        }

        public String format(PlayerTopEntry entry, Language language) {
            Duration.FormatParams params = new Duration.FormatParams();

            params.sourceUnit = TimeUnit.SECONDS;
            params.min = DurationField.HOUR;
            params.max = DurationField.MONTH;
            params.locale = language == Language.RUSSIAN ?
                    DurationFormatter.RU_SHORT :
                    DurationFormatter.EN_SHORT;

            return Duration.format(entry.getValue(), params);
        }
    }

}
