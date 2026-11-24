package club.abelix.api.base.item.impl.kit;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.GameItemStorage;
import club.abelix.api.base.item.gui.GameItemPurchaseConfirmGui;
import club.abelix.api.base.item.gui.PredicateMatcher;
import club.abelix.api.base.item.predicate.PredicateType;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.SoundType;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class KitSettingsGui extends Gui {

    private final Kit kit;

    public KitSettingsGui(@NonNull Gamer gamer, @NonNull Kit kit, Gui parent) {
        super(gamer.getPlayer(), Lang.of("game_item_settings_kit_title")
                .set("%kit%", kit.getDisplayName(gamer.getLanguage())), 6);

        this.kit = kit;

        if (parent != null) {
            generateBackItem(toSlot(3, 5), clickEvent -> parent.reopen());
        }

        draw();
    }

    @Override
    public void draw() {
        val gameItem = (GameItem) kit;
        val kitIcon = kit.getIcon(language);

        add(kitIcon, toSlot(3, 1));

        // рисуем остальные предметы
        drawActionIcon(gameItem, kitIcon);
        drawCustomizeIcon(gameItem);

        // TODO: 25.06.2023 stats items

        generateShopItem(toSlot(4, 5));
    }

//    private void drawStats(@NonNull GameItem gameItem) {
//
//    }

    private void drawCustomizeIcon(@NonNull GameItem gameItem) {
        val builder = ItemUtil.getBuilder(Material.ANVIL)
                .setName(Lang.of("kit_customize_name"))
                .setLore(Lang.of("kit_customize_description"));

        val ownMatch = new PredicateMatcher(gamer, gameItem, PredicateType.OWNERSHIP, false);

        builder.addLore(Lang.of("kit_customize_" + (ownMatch.isMatches() ? "click" : "unavailable")));

        add(builder.build(), toSlot(5, 1));
    }

    private void drawActionIcon(@NonNull GameItem gameItem, @NonNull ItemStack kitIcon) {
        val storage = GameItemStorage.INSTANCE;

        val selected = storage.getSelected(kit.getCategory(), gamer);

        val ownMatch = new PredicateMatcher(gamer, gameItem, PredicateType.OWNERSHIP, false);

        ClickAction action = null;

        ItemUtil.Builder showIcon = ItemUtil.getBuilder(kitIcon)
                .setType(Material.STAINED_GLASS_PANE);

        List<String> lore = new ArrayList<>();

        if (ownMatch.isMatches()) {
            showIcon.setDurability(5);

            boolean isSelected = selected != null && selected.equals(gameItem);

            if (isSelected) {
                lore.add("");
                lore.add(language.getMessage("game_item_unselect_available"));
            } else {
                val selectMatch = new PredicateMatcher(gamer, gameItem, PredicateType.SELECT, false);

                if (selectMatch.isMatches()) {
                    lore.add("");
                    lore.add(translated("game_item_select_click"));

                    action = e -> {
                        if (selected != null) {
                            selected.onUnselect(player);
                        }

                        storage.selectItem(gamer, gameItem);

                        gamer.sendLocalizedMessage(
                                "game_item_selected",
                                "%item%", gameItem.getDisplayName(language)
                        );

                        FalconAPI.getNmsApi().getSoundApi().playSound(player, SoundType.NOTIFICATION);

                        draw();
                    };
                } else {
                    lore.add("");

                    appendFailedPredicates(gameItem, lore, selectMatch);
                }
            }
        } else {
            val purchMatch = new PredicateMatcher(gamer, gameItem, PredicateType.PURCHASE, true);

            if (purchMatch.isMatches()) {
                showIcon.setDurability(4);

                val predicate = gameItem.predicates(PredicateType.PURCHASE)
                        .stream().findFirst()
                        .orElse(null);

                if (predicate != null) {
                    lore.add(translated("game_item_unlock_click"));

                    action = e -> GameItemPurchaseConfirmGui.create(gamer, gameItem, this, predicate);
                }
            } else {
                showIcon.setDurability(14);

                lore.add("");

                appendFailedPredicates(gameItem, lore, purchMatch);
            }
        }

        showIcon.addLore(lore);

        add(showIcon.build(), toSlot(4, 1), action);
    }

    private void appendFailedPredicates(@NonNull GameItem item, List<String> append, PredicateMatcher matcher) {
        val failed = matcher.getFailed();

        for (val predicate : failed) {
            val lore = predicate.getRequirementLore(item, getGamer());

            append.addAll(lore);
        }
    }
}
