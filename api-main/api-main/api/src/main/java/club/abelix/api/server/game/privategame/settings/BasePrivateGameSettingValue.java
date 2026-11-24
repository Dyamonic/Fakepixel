package club.abelix.api.server.game.privategame.settings;

import club.abelix.api.server.game.Game;
import club.abelix.commons.language.Language;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public abstract class BasePrivateGameSettingValue {
    ItemStack icon;
    String key;

    public String getDisplayName(@NonNull Language language) {
        return language.getMessage("private_game_settings_value_" + key + "_name");
    }

    public List<String> getLore(@NonNull Language language) {
        return language.getMessageList("private_game_settings_value_" + key + "_name");
    }


    public abstract void apply(Game game);
}
