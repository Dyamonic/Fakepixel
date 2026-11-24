package club.abelix.core.telegram.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.val;
import w.geo.api.Country;

import java.util.Set;

@UtilityClass
public final class GeoLocationUtil {

    private final Set<String> CIS_COUNTRIES = Set.of(
            "RU",
            "UA",
            "KZ",
            "BY",
            "AZ",
            "AM",
            "KG",
            "MD",
            "TJ",
            "UZ",
            "LV",
            "LT",
            "EE"
    );

    public boolean isCIS(@NonNull Country country) {
        return isCIS(country.getIsoCode());
    }

    public boolean isCIS(@NonNull String country) {
        return CIS_COUNTRIES.contains(country);
    }

    public String getCountryFlag(@NonNull String countryCode) {
        val upperCaseCountryCode = countryCode.toUpperCase();

        if (upperCaseCountryCode.length() != 2) {
            throw new IllegalArgumentException("Country code must be 2 characters");
        }

        val firstChar = Character.codePointAt(upperCaseCountryCode, 0) - 0x41 + 0x1F1E6;
        val secondChar = Character.codePointAt(upperCaseCountryCode, 1) - 0x41 + 0x1F1E6;

        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }

}
