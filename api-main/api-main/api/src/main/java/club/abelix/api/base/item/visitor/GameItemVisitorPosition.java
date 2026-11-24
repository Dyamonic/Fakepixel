package club.abelix.api.base.item.visitor;

import club.abelix.api.base.item.GameItem;

/**
 * @author Unidentified Person
 */
public enum GameItemVisitorPosition {

    FIRST {
        @Override
        public GameItem mov(GameItem item) {
            GameItem prev;

            while ((prev = item.getPrev()) != null) {
                item = prev;
            }

            return item;
        }
    },
    LAST {
        @Override
        public GameItem mov(GameItem item) {
            GameItem next;

            while ((next = item.getNext()) != null) {
                item = next;
            }

            return item;
        }
    },
    NEXT {
        @Override
        public GameItem mov(GameItem item) {
            return item.getNext();
        }
    },
    PREV {
        @Override
        public GameItem mov(GameItem item) {
            return item.getPrev();
        }
    };

    public abstract GameItem mov(GameItem item);

}
