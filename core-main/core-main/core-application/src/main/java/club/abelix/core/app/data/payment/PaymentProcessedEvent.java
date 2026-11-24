package club.abelix.core.app.data.payment;

import club.abelix.commons.data.currency.Currency;
import club.abelix.core.app.api.event.Event;
import club.abelix.core.app.data.gamer.Gamer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;


@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public final class PaymentProcessedEvent extends Event {
    Gamer gamer;
    LocalDateTime dateTime;
    double price;
    Currency currency;

    int paymentNumber;
    String product;
    String paymentSystem;
    String promoCode;
}
