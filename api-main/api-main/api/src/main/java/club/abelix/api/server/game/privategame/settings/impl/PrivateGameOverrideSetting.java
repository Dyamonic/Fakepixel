package club.abelix.api.server.game.privategame.settings.impl;

import club.abelix.api.server.game.Game;
import club.abelix.api.server.game.privategame.settings.BasePrivateGameSettingValue;
import club.abelix.api.server.game.setting.GameSetting;
import lombok.experimental.FieldDefaults;
import org.bukkit.inventory.ItemStack;


@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class PrivateGameOverrideSetting extends BasePrivateGameSettingValue {

    GameSetting gameSetting;
    Object gameSettingValue;

    public PrivateGameOverrideSetting(ItemStack icon, String key, GameSetting gameSetting, Object gameSettingValue) {
        super(icon, key);
        this.gameSetting = gameSetting;
        this.gameSettingValue = gameSettingValue;
    }

    @Override
    public void apply(Game game) {
        game.setSetting(gameSetting, gameSettingValue);
    }
}
