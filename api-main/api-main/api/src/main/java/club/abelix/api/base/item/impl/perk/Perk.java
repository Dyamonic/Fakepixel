package club.abelix.api.base.item.impl.perk;

import club.abelix.api.base.item.Applicable;
import club.abelix.api.base.item.GameItem;
import lombok.NonNull;

/**
 * Created by saharNooby on 19:57 18.10.2017
 */
public abstract class Perk extends GameItem implements Applicable {

    // Перк - некая выбираемая способность
    // От этого класса должны наследоваться перки конкретных миниигр

    protected Perk(int id, @NonNull String localizationKey, @NonNull PerkCategory category) {
        super(id, localizationKey, category);
    }

}
