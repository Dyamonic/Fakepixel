package club.abelix.api.server.game.privategame.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.game.privategame.settings.PrivateGameSetting;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

import javax.annotation.Nullable;

@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class PrivateGameSettingsValueGui extends Gui {
    PrivateGameSetting setting;

    public PrivateGameSettingsValueGui(@NonNull Gamer gamer,
                                       @NonNull PrivateGameSetting setting,
                                       @Nullable Gui parent) {
        super(gamer, Lang.of("private_game_settings_title"), 6);
        this.setting = setting;
        setParent(parent);

        draw();
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        val language = getLanguage();
        val variants = setting.getValues();
        val slots = Gui.BEAUTIFUL_SLOT_POSITIONS[variants.size() - 1];

        int slot = 0;
        for (val variant : variants) {
            add(ItemUtil.getBuilder(variant.getIcon())
                            .setName(variant.getDisplayName(language))
                            .setLore(variant.getLore(language)).build(),
                    slots[slot++], clickEvent -> {
                setting.setSelected(variant);
                gamer.playSound(YSound.VILLAGER_YES);
                gamer.sendLocalizedMessage("private_game_setting_update",
                        "%setting%", setting.getDisplayName(language),
                        "%value%", variant.getDisplayName(language));

                getParent().reopen();
                    });
        }

        if (getParent() != null) {
            generateBackItem(49, clickEvent -> getParent().reopen());
        }
    }
}
