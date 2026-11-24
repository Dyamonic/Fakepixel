package club.abelix.api.base.gui.slots.impl;

import club.abelix.api.base.gui.slots.BeautifulSlotsProvider;
import club.abelix.commons.util.math.MathUtil;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

public class OneSlotBorderSlotsProvider implements BeautifulSlotsProvider {

    public static final BeautifulSlotsProvider INSTANCE = new OneSlotBorderSlotsProvider();

    private OneSlotBorderSlotsProvider() {}

    private static final int[][] BEAUTIFUL_SLOTS_IN_ROW = {
            {3},
            {1, 5},
            {1, 3, 5},
            {0, 2, 4, 6},
            {0, 1, 3, 5, 6},
            {0, 1, 2, 4, 5, 6},
            {0, 1, 2, 3, 4, 5, 6},
    };

    private static final int[][] BEAUTIFUL_SLOT_POSITIONS;

    static {
        final int rowCount = 4;
        final int slotStep = 9;
        final List<int[]> patterns = new ArrayList<>(rowCount * BEAUTIFUL_SLOTS_IN_ROW.length);

        int currentRowSlot = 10;

        int[][][] patternGroups = new int[rowCount][][];
        int[][] fullPatterns = new int[rowCount][];

        for (int groupIndex = 0; groupIndex < rowCount; groupIndex++) {
            patternGroups[groupIndex] = generatePatternedRow(currentRowSlot);
            fullPatterns[groupIndex] = patternGroups[groupIndex][patternGroups[groupIndex].length - 1];
            currentRowSlot += slotStep;
        }

        for (int groupIndex = 0; groupIndex < rowCount; groupIndex++) {
            int[] prefix = getCumulativePrefix(fullPatterns, groupIndex);
            for (int[] pattern : patternGroups[groupIndex]) {
                int[] resultPattern = concatArrays(prefix, pattern);
                patterns.add(resultPattern);
            }
        }

        BEAUTIFUL_SLOT_POSITIONS = patterns.toArray(new int[0][]);
    }

    private static int[][] generatePatternedRow(int startsFromIndex) {
        int[][] result = new int[BEAUTIFUL_SLOTS_IN_ROW.length][];

        for (int i = 0; i < BEAUTIFUL_SLOTS_IN_ROW.length; i++) {
            int[] basePattern = BEAUTIFUL_SLOTS_IN_ROW[i];
            result[i] = new int[basePattern.length];
            for (int j = 0; j < basePattern.length; j++) {
                result[i][j] = basePattern[j] + startsFromIndex;
            }
        }

        return result;
    }

    private static int[] getCumulativePrefix(int[][] fullPatterns, int upToIndex) {
        int totalLength = 0;

        for (int i = 0; i < upToIndex; i++) {
            totalLength += fullPatterns[i].length;
        }

        int[] prefix = new int[totalLength];
        int pos = 0;

        for (int i = 0; i < upToIndex; i++) {
            for (int value : fullPatterns[i]) {
                prefix[pos++] = value;
            }
        }

        return prefix;
    }

    private static int[] concatArrays(int[] first, int[] second) {
        int[] result = new int[first.length + second.length];

        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);

        return result;
    }

    @Override
    public int getRawCountForItems(int itemsCount)  {
        Preconditions.checkState(itemsCount <= BEAUTIFUL_SLOT_POSITIONS.length);

        return Math.max(3, MathUtil.ceil(itemsCount / 7f) + 2);
    }

    @Override
    public int getSlotForItem(int itemIndex, int itemsCount) {
        Preconditions.checkState(itemsCount <= BEAUTIFUL_SLOT_POSITIONS.length);
        Preconditions.checkElementIndex(itemIndex, itemsCount);

        return BEAUTIFUL_SLOT_POSITIONS[itemsCount-1][itemIndex];
    }
}
