package club.abelix.games.buildffa.game.purchase;

import club.abelix.games.buildffa.game.utility.ConvertUtility;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;

import java.util.function.BooleanSupplier;

/**
 * @author milansky
 */
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class Purchase {
    int coinPrice;
    Integer goldPrice;
    Player player;
    Runnable onSuccess, onFailure;
    BooleanSupplier purchaseAllowed;

    public void execute() {
        new PurchaseGui(player, this);
    }

    public int getResolvedGoldPrice() {
        return goldPrice == null ? ConvertUtility.toGold(coinPrice) : goldPrice;
    }

    public boolean isPurchaseAllowed() {
        return purchaseAllowed == null || purchaseAllowed.getAsBoolean();
    }

    public void executeSuccess() {
        if (onSuccess != null) onSuccess.run();
    }

    public void executeFailure() {
        if (onFailure != null) onFailure.run();
    }
}
