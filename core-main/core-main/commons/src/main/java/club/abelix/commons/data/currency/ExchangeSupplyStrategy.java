package club.abelix.commons.data.currency;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Unidentified Person
 */
public interface ExchangeSupplyStrategy {

    boolean shouldSupply();

    CompletableFuture<?> supplyExchanges(Map<Currency, Double> result);

}
