package club.abelix.api.server.game.privategame.settings;


import club.abelix.commons.language.Language;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false, exclude = {
        "values", "selected", "icon"
})
@AllArgsConstructor
public class PrivateGameSetting {
    ItemStack icon;
    String key;
    List<BasePrivateGameSettingValue> values;
    @Setter
    BasePrivateGameSettingValue selected;


    public String getDisplayName(@NonNull Language language) {
        return language.getMessage("private_game_settings_" + key + "_name");
    }

    public List<String> getLore(@NonNull Language language) {
        return language.getMessageList("private_game_settings_" + key + "_lore",
                "%selected%", selected.getDisplayName(language));
    }

}
