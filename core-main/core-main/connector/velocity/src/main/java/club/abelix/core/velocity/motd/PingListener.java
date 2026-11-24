package club.abelix.core.velocity.motd;

import club.abelix.commons.language.Language;
import club.abelix.commons.version.GameVersion;
import club.abelix.core.velocity.connection.Connection;
import club.abelix.core.velocity.util.Constants;
import club.abelix.core.velocity.util.LanguageUtil;
import club.abelix.network.common.logging.Logging;
import club.abelix.network.online.query.StatusQueries;
import club.abelix.network.online.query.execution.StatusQueryExecutionService;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PingListener {

    MotdManager motdManager;
    StatusQueryExecutionService queryExecutionService;

    Favicon defaultFavicon;
    Favicon maintenanceFavicon;

    public PingListener(
            MotdManager motdManager,
            StatusQueryExecutionService queryExecutionService
    ) {
        this.motdManager = motdManager;
        this.queryExecutionService = queryExecutionService;
        this.defaultFavicon = loadDefaultFavicon();
        this.maintenanceFavicon = tryLoadFavicon("server-icon.png");
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        ServerPing.Builder response = event.getPing().asBuilder();
        InboundConnection connection = event.getConnection();
        Language language = LanguageUtil.getLanguage(motdManager.getStarship().getGeoLocationManager(), connection);

        ServerPing.Version defaultVersion = new ServerPing.Version(
                event.getPing().getVersion().getProtocol(),
                getBrand()
        );

        int protocolVersion = connection.getProtocolVersion().getProtocol();

        if (!Connection.isConnected()) {
            String notConnectedVersionText = language.getMessage("not_connected_version");

            ServerPing.Version notConnectedVersion = new ServerPing.Version(-1, notConnectedVersionText);
            event.setPing(buildPing(
                    connection,
                    response,
                    -1,
                    buildRawMessage(language, "motd_not_connected"),
                    notConnectedVersion,
                    true
            ));
            return;
        }

        int onlinePlayers = queryExecutionService.executeSync(StatusQueries.totalStatusQuery()).getTotalOnline();

        if (protocolVersion < GameVersion.MINIMUM_VERSION.getVersion()) {
            ServerPing.Version notSupportedVersion = new ServerPing.Version(
                    GameVersion.MINIMUM_VERSION.getVersion(),
                    defaultVersion.getName()
            );

            event.setPing(buildPing(
                    connection,
                    response,
                    onlinePlayers,
                    buildRawMessage(language, "motd_version_minimal_not_supported"),
                    notSupportedVersion,
                    true
            ));
            return;
        }

        if (protocolVersion > GameVersion.MAXIMUM_VERSION.getVersion()) {
            ServerPing.Version notSupportedVersion = new ServerPing.Version(
                    GameVersion.MAXIMUM_VERSION.getVersion(),
                    defaultVersion.getName()
            );

            event.setPing(buildPing(
                    connection,
                    response,
                    onlinePlayers,
                    buildRawMessage(language, "motd_version_maximal_not_supported"),
                    notSupportedVersion,
                    true
            ));
            return;
        }

        Component serverMotd = motdManager.getNextMotd(language, protocolVersion);

        if (motdManager.getStarship().getExclusivePeriod() != null) {
            String exclusiveKey = "motd_" + motdManager.getStarship().getExclusivePeriod().getExclusiveIdent();

            if (language.isMessageTranslated(exclusiveKey)) {
                serverMotd = motdManager.buildMotd(language, exclusiveKey);
            }
        }

        event.setPing(buildPing(
                connection,
                response,
                onlinePlayers,
                serverMotd,
                defaultVersion,
                false
        ));
    }

    private Component buildRawMessage(Language language, String localizationKey) {
        return Component.text(
                language.getMessage(
                        localizationKey,
                        "%region%", Constants.REGION,
                        "%min_version%", GameVersion.MINIMUM_VERSION.getName(),
                        "%max_version%", GameVersion.MAXIMUM_VERSION.getMostRecentSupportedVersionName()
                )
        );
    }

    private Favicon tryLoadFavicon(String fileName) {
        try {
            return Favicon.create(motdManager.getStarship().getDataFolder().resolve(fileName));
        } catch (IOException exception) {
            Logging.IMP.warn(fileName + " favicon not found!");
            return null;
        }
    }

    private Favicon loadDefaultFavicon() {
        String baseIconName = "server-icon.png";

        if (motdManager.getStarship().getExclusivePeriod() != null) {
            String iconPeriodName = motdManager.getStarship().getExclusivePeriod().getExclusiveIdent() + "-" + baseIconName;

            Favicon periodIcon = tryLoadFavicon(iconPeriodName);

            if (periodIcon != null) {
                return periodIcon;
            }
        }

        return tryLoadFavicon(baseIconName);
    }

    private String getBrand() {
        if (!Constants.PROXY_BRAND_AUTOGEN) {
            return Constants.PROXY_BRAND;
        }

        return String.format(
                "%s / %s",
                GameVersion.MINIMUM_VERSION.getName(),
                GameVersion.MAXIMUM_VERSION.getMostRecentSupportedVersionName()
        );
    }

    private ServerPing buildPing(InboundConnection connection,
                                 ServerPing.Builder builder,
                                 int onlinePlayers,
                                 @Nullable Component description,
                                 @Nullable ServerPing.Version version,
                                 boolean useMaintenanceFavicon) {
        if (description == null) {
            Language language = LanguageUtil.getLanguage(motdManager.getStarship().getGeoLocationManager(), connection);
            description = buildRawMessage(language, "proxy_ping_error");
        }

        builder.description(description);
        builder.onlinePlayers(onlinePlayers);

        if (version != null) {
            builder.version(version);
        }

        builder.favicon(useMaintenanceFavicon ? maintenanceFavicon : defaultFavicon);

        return builder.build();
    }
}