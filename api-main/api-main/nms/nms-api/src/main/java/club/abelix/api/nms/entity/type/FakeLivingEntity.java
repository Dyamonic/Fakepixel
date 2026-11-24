package club.abelix.api.nms.entity.type;

import club.abelix.api.nms.entity.interact.InteractAction;
import lombok.NonNull;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface FakeLivingEntity extends FakeEntity {

    BiConsumer<Player, InteractAction> getClickAction();

    void setClickAction(@NonNull BiConsumer<Player, InteractAction> consumer);

    default void setClickAction(@NonNull Consumer<Player> consumer) {
        setClickAction((player, interactAction) -> {
            if (interactAction != InteractAction.RIGHT_CLICK) {
                return;
            }

            consumer.accept(player);
        });
    }

    boolean isAI();

    void setAI(boolean b);

    void setHealth(float health);

    void addPotionEffect(@NonNull PotionEffect effect);

    float getHealth();
}
