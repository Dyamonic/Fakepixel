package club.abelix.commons.data.permission.prefix.defaults;

import club.abelix.commons.data.permission.prefix.PrefixModifier;
import club.abelix.commons.data.permission.prefix.predicator.GiftRankRequirement;
import club.abelix.commons.data.permission.prefix.predicator.IRankPrefixModifierPredicator;
import club.abelix.commons.data.permission.prefix.predicator.LevelRequirement;
import club.abelix.commons.util.color.ChatColor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * @author <a href="https://github.com/lesslyrose">PbIBa (milansky)</a> in November 2022
 * <br>
 * Пока сделал так, потому что пока что ранги не требуют дополнительной коррекции префиксов.
 * В случае нужды нужно будет написать более полноценный модификатор префиксов.
 * @see PrefixModifier
 */
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Getter
public enum RankPrefixModifier implements PrefixModifier {

    RED(ChatColor.RED, new LevelRequirement(0)),
    GOLD(ChatColor.GOLD, new LevelRequirement(35)),
    GREEN(ChatColor.GREEN, new LevelRequirement(45)),
    YELLOW(ChatColor.YELLOW, new LevelRequirement(55)),
    PURPLE(ChatColor.LIGHT_PURPLE, new LevelRequirement(65)),
    WHITE(ChatColor.WHITE, new LevelRequirement(75)),
    BLUE(ChatColor.BLUE, new LevelRequirement(85)),
    DARK_GREEN(ChatColor.DARK_GREEN, new LevelRequirement(95)),
    DARK_RED(ChatColor.DARK_RED, new LevelRequirement(150)),
    DARK_AQUA(ChatColor.DARK_AQUA, new LevelRequirement(150)),
    DARK_PURPLE(ChatColor.DARK_PURPLE, new LevelRequirement(200)),
    DARK_GRAY(ChatColor.DARK_GRAY, new LevelRequirement(200)),
    BLACK(ChatColor.BLACK, new LevelRequirement(250)),
    DARK_BLUE(ChatColor.DARK_BLUE, new GiftRankRequirement(100));

    public static final RankPrefixModifier[] VALUES = values(); // кэшируем значения

    ChatColor color;
    IRankPrefixModifierPredicator predicator;

    @Override
    public String modify(@NonNull String prefix) {
        return prefix.replace("<plus>", getColor() + "+");
    }
}
