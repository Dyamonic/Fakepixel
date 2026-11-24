package club.abelix.games.fakevival.managers;

import java.util.UUID;

public record BankTransaction(
        UUID uuid,
        String type,
        double amount,
        double balanceBefore,
        double balanceAfter,
        String source,
        long createdAt
) {
}
