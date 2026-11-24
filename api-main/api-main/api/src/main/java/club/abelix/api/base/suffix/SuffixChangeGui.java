package club.abelix.api.base.suffix;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.SoundType;
import club.abelix.api.server.base.shop.ShopGuiFactory;
import club.abelix.api.server.base.shop.ShopUtil;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.data.suffix.Suffix;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.time.Duration;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class SuffixChangeGui extends Gui {

    /**
     * Интервал изменения суффикса
     */
    private static final long SUFFIX_CHANGE_INTERVAL = TimeUnit.HOURS.toSeconds(1);

    /**
     * Иконка о том, что у игрока нет суффиксов
     */
    private static final ItemStack NO_SUFFIXES_ITEM = ItemUtil.getBuilder(Material.BUCKET)
            .setName(Lang.of("suffix_empty"))
            .setLore(Lang.of("suffix_empty_lore"))
            .build();

    private static final ItemStack ABOUT_SUFFIXES_ITEM = ItemUtil.getBuilder(Material.BOOK)
            .setName(Lang.of("suffix_info"))
            .setLore(Lang.of("suffix_info_lore"))
            .build();

    private final PagedZone<Suffix> pagedZone;

    public SuffixChangeGui(Gui parentGui, @NonNull Player player) {
        super(player, Lang.of("suffix_change_title"), 5);

        Gamer gamer = getGamer();

        setParent(parentGui);

        pagedZone = new PagedZone<>(this) {

            @Override
            protected Collection<Suffix> getItems() {
                return getGamer().getPurchasedSuffixes();
            }

            @Override
            protected void drawItem(Suffix suffix, int slot) {
                ClickAction action = null;

                String displaySuffix = suffix.getDisplaySuffix();

                boolean uncolored = !gamer.getRanks().hasPermission("suffix.colored");

                if (uncolored) {
                    displaySuffix = "§7" + ChatColor.stripColor(displaySuffix);
                }

                ItemUtil.Builder builder = ItemUtil.getBuilder(Material.NAME_TAG)
                        .setName(translated("suffix_name", "%suffix%", suffix.getDisplaySuffix()))
                        .setLore(translatedLore("suffix_lore", "%display%", gamer.getDisplayName() + " " + displaySuffix));

//                if (gamer.getRanks().canApply(Group.ULTRA)) {
//                    builder.addLore(
//                            translatedLore(
//                                    "suffix_colored_lore",
//                                    "%group%", Group.ULTRA.getDisplayName()
//                            )
//                    );
//
////                    action = compose(null, clickEvent -> {
////                        if (clickEvent.getClick() == ClickType.RIGHT) {
////                            new SuffixColorChangeGui(SuffixChangeGui.this, player, suffix);
////                        }
////                    });
//                } else if (uncolored) {
//                    builder.addLore(
//                            translatedLore(
//                                    "suffix_uncolored_lore",
//                                    "%group%", COLORED_SUFFIX_MINIMAL_GROUP.getDisplayName()
//                            )
//                    );
//                }

                int lastChangeSeconds = gamer.getProperty("LastSuffixChange");
                int currentSeconds = (int) TimeUtil.currentTimeSeconds();

                int changeSuffixExpiration = (int) (lastChangeSeconds + SUFFIX_CHANGE_INTERVAL);

                if (gamer.getActiveSuffix() == suffix) {
                    builder.glowing();
                    builder.addLore(
                            translatedLore("suffix_selected_lore")
                    );
                } else if (lastChangeSeconds + SUFFIX_CHANGE_INTERVAL > currentSeconds) {
                    builder.addLore(
                            translatedLore("suffix_cooldown_lore",
                                    "%time%",
                                    Duration.durationWordsSec(getLanguage(), changeSuffixExpiration - currentSeconds)
                            )
                    );

                    if (gamer.getGold() >= 2) {
                        builder.addLore(
                                translatedLore("suffix_force_change_lore")
                        );

                        action = compose(action, clickEvent -> {
                            if (clickEvent.getClick() != ClickType.LEFT) {
                                return;
                            }

                            if (gamer.getGold() < 2) {
                                gamer.sendLocalizedMessage("suffix_unavailable_clouds");
                                return;
                            }

                            gamer.takeGold(2);
                            gamer.changeActiveSuffix(suffix);

                            gamer.sendLocalizedMessage("suffix_force_changed");
                            player.closeInventory();

                            FalconAPI.getNmsApi().getSoundApi().playSound(player, SoundType.NOTIFICATION);
                        });
                    }
                } else {
                    builder.addLore(
                            translatedLore("suffix_click")
                    );

                    action = compose(action, clickEvent -> {
//                        if (clickEvent.getClick() != ClickType.LEFT) {
//                            return;
//                        }

                        gamer.changeActiveSuffix(suffix);
                        gamer.setProperty("LastSuffixChange", (int) TimeUtil.currentTimeSeconds());

                        player.sendMessage(translated("suffix_changed", "%suffix%", suffix.getDisplaySuffix()));
                        player.closeInventory();

                        FalconAPI.getNmsApi().getSoundApi().playSound(player, SoundType.NOTIFICATION);
                    });
                }

                add(builder.build(), slot, action);
            }
        };

        //слоты для списка друзей
        this.pagedZone.addSlotRect(1, 1, 7, 2);

        draw();
    }

    @Override
    public void draw() {
        Player player = getPlayer();
        Gamer gamer = getGamer();

        Gui parent = getParent();

        if (parent != null) {
            generateBackItem(toSlot(4, 3), clickEvent -> parent.reopen());
        }

        add(ABOUT_SUFFIXES_ITEM, toSlot(5, 3));

        add(ItemUtil.getBuilder(Material.NETHER_STAR)
                .setName(language.getMessage("suffix_gui_shop_name"))
                .setLore(language.getMessageList("suffix_gui_shop_lore"))
                .build(), 36, clickEvent -> {
            DiBukkitPlatform.withBeanOrElse(
                    ShopGuiFactory.class,
                    bean -> bean.open(getGamer(), null),
                    () -> ShopUtil.open(getGamer())
            );
        });

        ItemUtil.Builder resetSuffixItem = ItemUtil.getBuilder(Material.BARRIER)
                .setName(translated("suffix_reset_name"))
                .setLore(translatedLore("suffix_reset_lore"));

        ClickAction action = null;

        Suffix active = gamer.getActiveSuffix();

        if (active != null) {
            resetSuffixItem.addLore(
                    translatedLore("suffix_reset_lore_available",
                            "%suffix%", active.getDisplaySuffix(active.getColor())
                    )
            );

            action = clickEvent -> {
                gamer.changeActiveSuffix(null);
                gamer.sendLocalizedMessage("suffix_reset");

                player.closeInventory();

                FalconAPI.getNmsApi().getSoundApi().playSound(player, SoundType.ITEM_PICKUP);
            };
        } else {
            resetSuffixItem.addLore(
                    translatedLore("suffix_reset_lore_unavailable")
            );
        }

        add(resetSuffixItem.build(), toSlot(3, 3), action);

        if (gamer.getPurchasedSuffixes().isEmpty()) {
            add(NO_SUFFIXES_ITEM, toSlot(4, 2));
            return;
        }

        //рисуем гуи
        this.pagedZone.draw();
        //кнопки назад/вперед
        this.pagedZone.drawButtons(toSlot(1, 3), toSlot(7, 3));
    }
}
