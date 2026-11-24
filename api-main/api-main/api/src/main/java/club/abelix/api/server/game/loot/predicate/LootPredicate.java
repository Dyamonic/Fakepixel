package club.abelix.api.server.game.loot.predicate;

import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.loot.predicate.value.LootPredicateIntegerValue;
import club.abelix.api.server.game.loot.predicate.value.LootPredicateMetadataValue;
import club.abelix.api.server.game.loot.predicate.value.LootPredicateValue;

/**
 * @author Unidentified Person
 */
public class LootPredicate {

    private final LootPredicateValue left;
    private final LootPredicateValue right;

    private final LootPredicateOp op;

    public LootPredicate(LootPredicateValue left, LootPredicateValue right, LootPredicateOp op) {
        this.left = left;
        this.right = right;
        this.op = op;
    }

    public static LootPredicate parse(String text) {
        LootPredicateOp op = null;
        int separatorStart = 0;
        int separatorEnd = 0;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            switch (ch) {
                case '<':
                    op = LootPredicateOp.L;
                    break;
                case '>':
                    op = LootPredicateOp.R;
                    break;
                case '=':
                    if (op == null) {
                        op = LootPredicateOp.EQ;
                    } else {
                        switch (op) {
                            case L:
                                op = LootPredicateOp.LEQ;
                                break;
                            case R:
                                op = LootPredicateOp.REQ;
                                break;
                        }
                    }

                    break;
                default:
                    continue;
            }

            if (separatorStart == 0) {
                separatorStart = i;
            }

            separatorEnd = i;
        }

        return new LootPredicate(
                parseValue(0, separatorStart, text),
                parseValue(separatorEnd + 1, text.length(), text),
                op
        );
    }

    private static LootPredicateValue parseValue(int from, int to, String text) {
        int bufferedInt = 0;

        for (int i = from; i < to; i++) {
            char ch = text.charAt(i);

            if (ch >= '0' && ch <= '9') {
                bufferedInt = bufferedInt * 10 + (ch & 0xF);
            } else {
                return new LootPredicateMetadataValue(text.substring(from, to));
            }
        }

        return new LootPredicateIntegerValue(bufferedInt);
    }

    public boolean eval(Game game) {
        return op.test(left.getValue(game), right.getValue(game));
    }
}
