package club.abelix.commons.data.cases.rarity;

import club.abelix.commons.language.Language;
import club.abelix.commons.util.color.ChatColor;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Rarity {

    /**
     * Предмет без рарити, он никогда не выпадет, например,
     * в кейсах. Используется для прочих наград
     */
    NONE(0, ChatColor.GRAY, "", 0.0, 0),

    /**
     * Обычный предмет
     */
    COMMON(1, ChatColor.GREEN, "✰", 0.52, 0),

    /**
     * Более редкий предмет
     */
    RARE(2, ChatColor.AQUA, "✰✰", 0.28, 0),

    /**
     * Эпический предмет
     */
    EPIC(3, ChatColor.DARK_PURPLE, "✰✰✰", 0.15, 0.01),

    /**
     * Легендарный предмет, выпадает пиздец как редко
     */
    LEGENDARY(4, ChatColor.GOLD, "✰✰✰✰", 0.05, 0.0025);

    public static final Rarity[] VALUES = values();
    private static final TIntObjectMap<Rarity> BY_ID = new TIntObjectHashMap<>();

    static {
        for (Rarity rarity : VALUES) {
            BY_ID.put(rarity.getId(), rarity);
        }
    }

    private final int id;
    private final ChatColor color;

    // ------------------------------------------------------
    private final String stars;
    private final double chance, changeChance;

    public static Rarity getById(int id) {
        return BY_ID.get(id);
    }

    public String getName(@NonNull Language language) {
        return language.getMessage("rarity_" + name());
    }

}
