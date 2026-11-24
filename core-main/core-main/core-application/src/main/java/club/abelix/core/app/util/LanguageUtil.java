package club.abelix.core.app.util;

import club.abelix.commons.language.Language;
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
            InetAddress address) {

        if (address == null) {
            return Language.defaultLanguage();
        }

        Country country = manager.lookup(address).getCountry();

        if (country == null) {
            return Language.defaultLanguage();
        }

        return switch (country.getIsoCode()) {
            case "RU", "UA", "CZ", "BY", "AZ", "AM", "KG", "MD", "TJ", "UZ" -> Language.RUSSIAN;
            default -> Language.defaultLanguage();
        };
    }


}