package club.abelix.api.base.util.item;

import club.abelix.commons.language.Language;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

@UtilityClass
public class LanguageIcons {

    private final Map<Language, ItemStack> LANGUAGE_ITEMS = new EnumMap<Language, ItemStack>(Language.class) {
        {
            put(Language.RUSSIAN, Heads.Flag_Russia.getItem());
            put(Language.ENGLISH, Heads.Flag_USA.getItem());
            //put(Language., Heads.Flag_Germany.getItem());
        }
    };

    public ItemStack getLanguageIcon(@NonNull Language language) {
        return LANGUAGE_ITEMS.computeIfAbsent(language, m -> Heads.Block_Clay.getItem()).clone();
    }

}
