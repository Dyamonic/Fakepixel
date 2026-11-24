package club.abelix.games.fakevival.managers;

public final class BazaarAlgorithm {
    private static final double PRESSURE_WEIGHT = 0.25D;
    private static final double MIN_MULTIPLIER = 0.5D;
    private static final double MAX_MULTIPLIER = 3.0D;
    private static final double MAX_STEP = 0.05D;
    private static final double BUY_SPREAD = 1.03D;
    private static final double SELL_SPREAD = 0.97D;

    public BazaarPrices recalculate(double basePrice, double previousMarketPrice, long recentBuys, long recentSells) {
        basePrice = Double.isFinite(basePrice) ? Math.max(1.0D, basePrice) : 1.0D;
        previousMarketPrice = Double.isFinite(previousMarketPrice) && previousMarketPrice > 0.0D
                ? previousMarketPrice
                : basePrice;

        double totalTransactions = Math.max(1.0D, (double) recentBuys + (double) recentSells);
        double pressure = ((double) recentBuys - (double) recentSells) / totalTransactions;
        double target = basePrice * (1.0D + pressure * PRESSURE_WEIGHT);
        target = clamp(target, basePrice * MIN_MULTIPLIER, basePrice * MAX_MULTIPLIER);

        double maxChange = Math.max(1.0D, previousMarketPrice * MAX_STEP);
        double marketPrice = clamp(target, previousMarketPrice - maxChange, previousMarketPrice + maxChange);
        marketPrice = clamp(marketPrice, basePrice * MIN_MULTIPLIER, basePrice * MAX_MULTIPLIER);

        double buyPrice = round(Math.max(1.0D, marketPrice * BUY_SPREAD));
        double sellPrice = round(Math.max(1.0D, marketPrice * SELL_SPREAD));
        return new BazaarPrices(round(marketPrice), buyPrice, sellPrice);
    }

    public BazaarPrices defaults(double basePrice) {
        double marketPrice = round(Math.max(1.0D, basePrice));
        return new BazaarPrices(marketPrice, round(marketPrice * BUY_SPREAD), round(Math.max(1.0D, marketPrice * SELL_SPREAD)));
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }

        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.max(1.0D, Math.rint(value));
    }

    public record BazaarPrices(double marketPrice, double buyPrice, double sellPrice) {
    }
}
