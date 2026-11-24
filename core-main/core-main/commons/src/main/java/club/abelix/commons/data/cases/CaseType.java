package club.abelix.commons.data.cases;

import club.abelix.commons.language.Language;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public enum CaseType {

    COSMETIC_CASE(1, 25, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGQ2YTYwZTU3OTlmNGVkYjFiZjUyZGY0OTQ3NGQ3YjQyOMODERATOR5NjRlNGE5NDEzMjhlMjQxYTFlYWI5NjAyNTg2OSJ9fX0=", 0d),
    DONATE_CASE(2, 199, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDQ2NzUxNThjMDc2N2VlNTA4YzUyZDUyNDI2Y2VmM2EyYzJiMjliN2U0ODdjOTI5NTNhMzgyM2Y1NjFkMDZhZiJ9fX0=", 0d),
    SUFFIX_CASE(3, 99, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDE2ZDI1OTUzZDRiOGU2MzM0OTI2ZTIwNmFjMzIzZTc3NzMzZWQ3MDQwMWZkMWE4ZWVmN2UyMzkwZTQwZGIzZCJ9fX0=", 0d),
    EXPERIENCE_CASE(4, 75, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGViZjZiZTgyZTgyM2U1ODJmZjhmNDVlMDFjZjUxNzUxNzM0ZDlhNzc2Y2ZmNTE0ZTg4ZWEyNmQ0OTczYmJmMCJ9fX0=", 0d),
    MONEY_CASE(5, 49, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Y2NmY3ZjAzMTI1Y2Y1NDczMzY5NmYzNjMyZjBkOWU2NDcwYmFhYjg0OTg0N2VhNWVhMmQ3OTE1NmFkMGY0MCJ9fX0=", 0d),
    GOLD_CASE(6, 59, "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjJjZGEwNWVhNWQ5Y2NhNzAyMDdhOGIyOWZkZTFjOTY5NmM3NTI3YzYyZTU5OWUzZGEyZDI3NWYyMGUwYmE5YiJ9fX0=", 0d);

    private static final TIntObjectMap<CaseType> BY_ID = new TIntObjectHashMap<>();

    static {
        for (CaseType caseType : CaseType.values()) {
            BY_ID.put(caseType.getId(), caseType);
        }
    }

    private final int id;
    private final int plazmaCost;
    private final String texture;
    private final double caseChance;

    public static CaseType getById(int id) {
        return BY_ID.get(id);
    }

    public static CaseType getByName(@NonNull String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public String getDisplayName(@NonNull Language language) {
        return language.getMessage(name() + "_name");
    }

    public String getDisplayName() {
        return getDisplayName(Language.defaultLanguage());
    }

    public List<String> getDescription(@NonNull Language language) {
        return language.getMessageList(name() + "_description");
    }

}
