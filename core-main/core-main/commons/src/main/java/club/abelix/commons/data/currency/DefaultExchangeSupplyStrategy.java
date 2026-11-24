package club.abelix.commons.data.currency;

import lombok.SneakyThrows;
import ru.yooxa.json.JElement;
import ru.yooxa.json.JObject;
import ru.yooxa.json.JParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Unidentified Person
 */
public class DefaultExchangeSupplyStrategy implements ExchangeSupplyStrategy {

    private static final String API = "https://v6.exchangerate-api.com/v6/5cc022a415aaa62ebbaf31c3/latest/RUB";

    private final AtomicLong lastSupply = new AtomicLong();
    private final long supplyTime = TimeUnit.MINUTES.toMillis(10);

    private final AtomicReference<CompletableFuture<Void>> supplyFuture = new AtomicReference<>();

    @Override
    public boolean shouldSupply() {
        long lastSupply = this.lastSupply.get();
        long elapsed = System.currentTimeMillis() - lastSupply;

        return (elapsed > supplyTime);
    }

    @SneakyThrows
    private void _supplyExchanges(Map<Currency, Double> result) {
        HttpURLConnection connection = (HttpURLConnection) new URL(API).openConnection();
        JObject object;

        try (InputStream is = connection.getInputStream()) {
            object = JParser.parse(is).getAsObject();
        }

        JObject rates = object.getAsObject("conversion_rates");

        if (rates != null) {
            for (Currency currency : Currency.values()) {
                result.put(currency, Optional.ofNullable(rates.get(currency.name()))
                        .map(JElement::getAsDouble)
                        .orElse(1D));
            }
        }
    }

    @Override
    public CompletableFuture<?> supplyExchanges(Map<Currency, Double> result) {
        CompletableFuture<Void> currentFuture = supplyFuture.get();

        if (currentFuture == null) {
            currentFuture = CompletableFuture.runAsync(() -> {
                _supplyExchanges(result);

                supplyFuture.set(null);
            });

            supplyFuture.set(currentFuture);
        }

        return currentFuture;
    }
}
