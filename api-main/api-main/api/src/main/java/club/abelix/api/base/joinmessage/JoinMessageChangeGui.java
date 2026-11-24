package club.abelix.api.base.joinmessage;

import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.base.shop.ShopGuiFactory;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.commons.data.joinmessage.JoinMessage;
import club.abelix.commons.data.setting.Setting;
import club.abelix.commons.language.Lang;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class JoinMessageChangeGui extends Gui {

    private static final ItemStack ABOUT_JOIN_MESSAGES_ITEM = ItemUtil.getBuilder(Material.BOOK)
            .setName(Lang.of("joinmessage_info"))
            .setLore(Lang.of("joinmessage_info_lore"))
            .build();

    private final PagedZone<JoinMessage> pagedZone;

    public JoinMessageChangeGui(Gui parentGui, @NonNull Player player) {
        super(player, Lang.of("joinmessage_change_title"), 5);

        setParent(parentGui);

        JoinMessage activeJoinMessage = gamer.getActiveJoinMessage();

        pagedZone = new PagedZone<>(this) {

            @Override
            protected Collection<JoinMessage> getItems() {
                return gamer.getPurchasedJoinMessages();
            }

            @Override
            protected void drawItem(JoinMessage message, int slot) {
                ClickAction action = null;

                boolean active = message == activeJoinMessage;

                String displayMessage = message.getText(language);

                ItemUtil.Builder builder = ItemUtil.getBuilder(active ? Material.EMPTY_MAP : Material.PAPER)
                        .setName(translated("joinmessage_name",
                                "%name%", message.getName(language)
                        ))
                        .setLore(translatedLore("joinmessage_lore",
                                "%player%", gamer.getDisplayName(),
                                "%text%", displayMessage,
                                "%price%", message.getCost()
                        ));

                if (active) {
                    builder.glowing();
                    builder.addLore(
                            translatedLore("joinmessage_selected_lore")
                    );
                } else {
                    builder.addLore(
                            translatedLore("joinmessage_click")
                    );

                    action = clickEvent -> {
                        if (clickEvent.getClick() != ClickType.LEFT) {
                            return;
                        }

                        gamer.changeActiveJoinMessage(message);
                        gamer.sendLocalizedMessage("joinmessage_changed",
                                "%name%", message.getName(language));

                        player.closeInventory();

                        gamer.playSound(YSound.ORB_PICKUP, 0.75F, 2F);
                    };
                }

                add(builder.build(), slot, action);
            }
        };

        //слоты для списка сообщений о входе
        this.pagedZone.addSlotRect(1, 1, 7, 2);

        draw();
    }

    @Override
    public void draw() {
        Gui parent = getParent();

        if (parent != null) {
            generateBackItem(toSlot(3, 3), clickEvent -> parent.reopen());
        }

        add(ABOUT_JOIN_MESSAGES_ITEM, toSlot(parent == null ? 4 : 5, 3));


        add(ItemUtil.getBuilder(Material.NETHER_STAR)
                .setName(language.getMessage("joinmessage_gui_shop_name"))
                .setLore(language.getMessageList("joinmessage_gui_shop_lore"))
                .build(), 36, clickEvent -> {
            DiBukkitPlatform.withBeanOrElse(
                    ShopGuiFactory.class,
                    bean -> bean.open(getGamer(), null),
                    () -> ShopUtil.open(getGamer())
            );
        });

        drawToggleItem();

        //рисуем гуи
        this.pagedZone.draw();
        //кнопки назад/вперед
        this.pagedZone.drawButtons(toSlot(1, 3), toSlot(7, 3));
    }

    private void drawToggleItem() {
        boolean enabled = !gamer.isSettingSet(Setting.HIDE_JOIN_MESSAGE);

        String status = language.getMessage(enabled ? "enabled" : "disabled");

        ItemStack toggleItem = ItemUtil.getBuilder(Material.INK_SACK)
                .setDurability(enabled ? 10 : 8)
                .setName(language.getMessage("joinmessage_toggle", "%status%", status))
                .setLore(language.getMessageList("joinmessage_toggle_lore"))
                .build();

        add(toggleItem, toSlot(8, 4), clickEvent -> {
            if (enabled) {
                gamer.setSettingState(Setting.HIDE_JOIN_MESSAGE, true);
                gamer.sendLocalizedMessage("joinmessage_disabled");
            } else {
                gamer.setSettingState(Setting.HIDE_JOIN_MESSAGE, false);
                gamer.sendLocalizedMessage("joinmessage_enabled");
            }

            drawToggleItem();
        });
    }
}
