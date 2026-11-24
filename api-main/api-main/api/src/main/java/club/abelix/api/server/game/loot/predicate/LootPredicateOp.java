package club.abelix.api.server.game.loot.predicate;

/**
 * @author Unidentified Person
 */
public enum LootPredicateOp {

    EQ {
        @Override
        public boolean test(int first, int second) {
            return first == second;
        }
    },
    LEQ {
        @Override
        public boolean test(int first, int second) {
            return first <= second;
        }
    },
    REQ {
        @Override
        public boolean test(int first, int second) {
            return first >= second;
        }
    },
    L {
        @Override
        public boolean test(int first, int second) {
            return first < second;
        }
    },
    R {
        @Override
        public boolean test(int first, int second) {
            return first > second;
        }
    };

    public abstract boolean test(int first, int second);

}
