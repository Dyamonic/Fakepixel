package club.abelix.core.velocity.util;

import club.abelix.commons.language.Language;
import com.velocitypowered.api.proxy.InboundConnection;
import lombok.experimental.UtilityClass;
import w.geo.api.Country;
import w.geo.api.GeoLocationManager;

import java.net.InetAddress;

/**
 * @author Unidentified Person
 */
@UtilityClass
public class LanguageUtil {

    public Language getLanguage(
            GeoLocationManager manager,
            InboundConnection connection) {

        if (connection == null) {
            return Language.defaultLanguage();
        }

        InetAddress address = connection.getRemoteAddress().getAddress();
        Country country = manager.lookup(address).getCountry();

        if (country == null) {
            return Language.defaultLanguage();
        }

        return switch (country.getIsoCode()) {
            case "RU", "UA", "CZ", "BY", "AZ", "AM", "KG", "MD", "TJ", "UZ" -> Language.RUSSIAN;
            default -> Language.ENGLISH;
        };
    }


}