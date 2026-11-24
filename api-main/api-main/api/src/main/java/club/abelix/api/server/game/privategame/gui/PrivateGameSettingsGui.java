package club.abelix.api.server.game.privategame.gui;


import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.server.game.Game;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class PrivateGameSettingsGui extends Gui {

    Game game;

    public PrivateGameSettingsGui(@NonNull Gamer gamer, @NonNull Game game) {
        super(gamer, Lang.of("private_game_settings_title"), 6);
        this.game = game;
        draw();
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        val language = getLanguage();
        val gameSettings = game.getPrivateGameSettings().getSettings();
        val slots = Gui.BEAUTIFUL_SLOT_POSITIONS[gameSettings.size() - 1];

        int slot = 0;
        for (val setting : gameSettings) {
            add(ItemUtil.getBuilder(setting.getIcon())
                    .setName(setting.getDisplayName(language))
                    .setLore(setting.getLore(language)).build(),
                    slots[slot++], clickEvent -> new PrivateGameSettingsValueGui(gamer, setting, this));
        }

        add(Gui.CLOSE_MENU_ICON,
                49,
                e -> getPlayer().closeInventory());
    }
}
