package club.abelix.api.base.prefixcolor;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerDisplayNameUpdateEvent;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.SoundType;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.data.suffix.color.SuffixColor;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.color.ChatColor;
import club.abelix.core.bukkit.HeartBeat;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;

import javax.annotation.Nullable;

public class PrefixColorGui extends Gui {

    private static final TObjectIntMap<SuffixColor> DONATE_COLORS = new TObjectIntHashMap<>() {{
        put(SuffixColor.AQUA, 350);
        put(SuffixColor.BLUE, 350);
        put(SuffixColor.DARK_AQUA, 350);

        put(SuffixColor.DARK_GREEN, 500);
        put(SuffixColor.DARK_PURPLE, 500);
        put(SuffixColor.GREEN, 350);

        put(SuffixColor.GOLD, 500);
        put(SuffixColor.GRAY, 350);

        put(SuffixColor.PINK, 500);
        put(SuffixColor.RED, 350);
        put(SuffixColor.YELLOW, 350);

        put(SuffixColor.WHITE, 500);
        put(SuffixColor.BLACK, 500);
        put(SuffixColor.DARK_BLUE, 500);
        put(SuffixColor.DARK_RED, 850);
    }};

    public PrefixColorGui(@Nullable Gui patent, @NonNull Gamer gamer) {
        super(gamer, Lang.of("prefix_color_title"), 4);

        setParent(patent);

        draw();
    }

    private String prefixColor(SuffixColor color, String prefix) {
        return "§" + color.getColorCode() + ChatColor.stripColor(prefix);
    }

    @Override
    public void draw() {
        Gamer gamer = getGamer();

        int x = 0;
        int y = 1;

        val selectedColor = gamer.getSelectedPrefixColor();

        if (getParent() != null) generateBackItem(toSlot(4, 3), clickEvent -> getParent().reopen());

        for (SuffixColor color : SuffixColor.values()) {
            if (++x >= 8) {
                y++;
                x = 1;
            }

            if (color == SuffixColor.DARK_RED) {
                y = 3;
                x = 4;
            }

            ClickAction clickAction = null;

            ItemUtil.Builder builder = ItemUtil.getBuilder(Material.STAINED_GLASS);
            builder.setDurability(color.getDurability());
            builder.setName(color.getDisplayName(language));
            builder.setLore(
                    translatedLore("prefix_color_future_lore",
                            "%name%", prefixColor(color, gamer.getDisplayName())
                    )
            );

            if (color == selectedColor) {
                builder.glowing();
                builder.addLore(
                        translated("prefix_color_already_set")
                );
            } else {
                // Проверяем на то что цвет платный
                boolean isNeedBuy = false;

                if (DONATE_COLORS.containsKey(color)) {
                    if (!gamer.hasProperty("Suffix/Donate/" + color.getId())) {
                        isNeedBuy = true;

                        builder.addLore(
                                translated("prefix_color_need_buy", "%price%",
                                        DONATE_COLORS.get(color))
                        );
                    }
                }

                if (!isNeedBuy) {
                    builder.addLore(
                            translated("prefix_color_click_to_change")
                    );
                }

                final boolean clickBuy = isNeedBuy;
                clickAction = clickEvent -> {
                    if (!gamer.getRanks().hasPermission("prefix.color")) {
                        return;
                    }

                    if (!gamer.hasPermission("prefix.colored.allowed") && gamer.hasPermission("prefix.colored.disallowed")) {
                        return;
                    }

                    if (clickBuy) {
                        int price = DONATE_COLORS.get(color);

                        if (gamer.hasGold(price)) {
                            gamer.takeGold(price);
                            gamer.setProperty("Suffix/Donate/" + color.getId(), 1);
                            gamer.playSound(SoundType.LEVEL_UP);
                            draw();
                        } else {
                            gamer.sendLocalizedMessage("purchase_unavailable_cost_message");
                            gamer.playSound(SoundType.BREAK);
                        }
                        return;
                    }

                    gamer.changePrefixColor(color);
                    gamer.updateDisplayName();
                    HeartBeat.callEvent(new GamerDisplayNameUpdateEvent(gamer));

                    getGamer().sendLocalizedMessage("prefix_color_changed",
                            "%color%", color.getDisplayName(language));

                    gamer.playSound(SoundType.NOTIFICATION);
                    gamer.getPlayer().closeInventory();
                };
            }

            add(builder.build(), toSlot(x, y), clickAction);
        }
    }
}