package club.abelix.commons.data.currency;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Unidentified Person
 */
public class EmptyExchangeSupplyStrategy implements ExchangeSupplyStrategy {

    private static final EmptyExchangeSupplyStrategy instance = new EmptyExchangeSupplyStrategy();

    private EmptyExchangeSupplyStrategy() {
    }

    public static EmptyExchangeSupplyStrategy getInstance() {
        return instance;
    }

    @Override
    public boolean shouldSupply() {
        return false;
    }

    @Override
    public CompletableFuture<?> supplyExchanges(Map<Currency, Double> result) {
        return null;
    }
}
