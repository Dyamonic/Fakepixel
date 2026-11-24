package club.abelix.commons.data.currency;

import lombok.val;
import ru.yooxa.json.JElement;
import ru.yooxa.json.JObject;

import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author Unidentified Person
 */
public class CurrencyConverter {

    private final EnumMap<Currency, Double> currencies;
    private final ExchangeSupplyStrategy supplyStrategy;

    private CurrencyConverter(final EnumMap<Currency, Double> currencies, final ExchangeSupplyStrategy strategy) {
        this.currencies = currencies;
        this.supplyStrategy = strategy;

        if (strategy.shouldSupply()) {
            strategy.supplyExchanges(currencies).join();
        }
    }

    public CurrencyConverter(final ExchangeSupplyStrategy strategy) {
        this(new EnumMap<>(Currency.class), strategy);
    }

    public CurrencyConverter(final CurrencyConverter another) {
        this(new EnumMap<>(another.currencies), EmptyExchangeSupplyStrategy.getInstance());
    }

    public boolean isEmpty() {
        return currencies.isEmpty();
    }

    public CurrencyConverter delegate() {
        return new CurrencyConverter(this);
    }

    public void read(final JObject object) {
        for (Currency currency : Currency.values()) {
            val rate = Optional.ofNullable(object.get(currency.name()))
                    .map(JElement::getAsDouble)
                    .orElse(1D);

            currencies.put(currency, rate);
        }
    }

    public void write(final JObject object) {
        currencies.forEach((key, value) -> object.addProperty(key.name(), value));
    }

    public CompletableFuture<Integer> convertRubbles(final int value, final Currency to) {
        return convert(value, Currency.RUB, to);
    }

    public CompletableFuture<Integer> convert(final int value, final Currency from, final Currency to) {
        if (from == null || from == to) {
            return CompletableFuture.completedFuture(value);
        }

        if (supplyStrategy.shouldSupply()) {
            return supplyStrategy.supplyExchanges(currencies).thenApply(x -> _convert(value, from, to));
        }

        return CompletableFuture.completedFuture(_convert(value, from, to));
    }

    private Integer _convert(final int value, final Currency from, final Currency to) {
        // from 100 RUB to USD
        // 100 / <rate rub to rub> * <rate usd to rub>

        double fromRate = currencies.get(from);
        double toRate = currencies.get(to);

        return (int) (value / fromRate * toRate);
    }

}
