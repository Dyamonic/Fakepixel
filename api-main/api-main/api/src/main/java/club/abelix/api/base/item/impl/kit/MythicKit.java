package club.abelix.api.base.item.impl.kit;

import lombok.NonNull;

/**
 * @author KaiJQ
 * @project Api
 */

public abstract class MythicKit extends Kit {

    protected MythicKit(int id, @NonNull String localizationKey, @NonNull KitCategory category) {
        super(id, localizationKey, category);
    }
}
