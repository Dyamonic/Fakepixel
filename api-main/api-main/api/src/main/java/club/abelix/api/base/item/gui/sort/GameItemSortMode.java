package club.abelix.api.base.item.gui.sort;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.gui.PredicateMatcher;
import club.abelix.api.base.item.predicate.CoinsPredicate;
import club.abelix.api.base.item.predicate.PredicateType;
import club.abelix.commons.language.Language;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Getter
public enum GameItemSortMode {

    A_Z(1, "a_z") {
        @Override
        public void sort(@NonNull Gamer gamer, @NonNull List<GameItem> items) {
            Language language = gamer.getLanguage();

            items.sort(Comparator.comparing(item -> item.getDisplayName(language)));
        }
    },
    Z_A(2, "z_a") {
        @Override
        public void sort(@NonNull Gamer gamer, @NonNull List<GameItem> items) {
            A_Z.sort(gamer, items);
            Collections.reverse(items);
        }
    },
    FIRST_AVAILABLE(3, "first_available") {
        @Override
        public void sort(@NonNull Gamer gamer, @NonNull List<GameItem> items) {
            items.sort(((Comparator<GameItem>) (item, anotherItem) -> {
                PredicateMatcher matcher = new PredicateMatcher(
                        gamer, item, PredicateType.OWNERSHIP, false
                );

                PredicateMatcher anotherMatcher = new PredicateMatcher(
                        gamer, anotherItem, PredicateType.OWNERSHIP, false
                );

                return Boolean.compare(matcher.isMatches(), anotherMatcher.isMatches());
            }).reversed());
        }
    },
    BY_PRICE_ASC(4, "price_asc") {
        @Override
        public void sort(@NonNull Gamer gamer, @NonNull List<GameItem> items) {
            items.sort((item, anotherItem) -> {
                CoinsPredicate first = item.getCoinsPredicate();
                CoinsPredicate second = anotherItem.getCoinsPredicate();

                int firstPrice = first == null ? 0 : first.getCost();
                int secondPrice = second == null ? 0 : second.getCost();

                return Integer.compare(firstPrice, secondPrice);
            });
        }
    },
    BY_PRICE_DESC(5, "price_desc") {
        @Override
        public void sort(@NonNull Gamer gamer, @NonNull List<GameItem> items) {
            BY_PRICE_ASC.sort(gamer, items);
            Collections.reverse(items);
        }
    };

    // ---------------------------------------------------

    private static final GameItemSortMode[] VALUES = values();

    private static final TIntObjectMap<GameItemSortMode> BY_ID = new TIntObjectHashMap<>();

    static {
        for (GameItemSortMode type : VALUES) {
            BY_ID.put(type.id, type);
        }
    }

    private final int id;

    // ---------------------------------------------------
    private final String name;

    public static GameItemSortMode getById(int id) {
        return BY_ID.containsKey(id) ? BY_ID.get(id) : FIRST_AVAILABLE;
    }

    public String getName(@NonNull Language language) {
        return language.getMessage("game_item_sort_mode_" + name);
    }

    public abstract void sort(@NonNull Gamer gamer, @NonNull List<GameItem> items);

    public GameItemSortMode next() {
        int next = ordinal() + 1;

        return VALUES.length == next ? A_Z : VALUES[next];
    }

}
