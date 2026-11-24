package club.abelix.api.base.item.gui;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.gui.PagedZone;
import club.abelix.api.base.gui.icon.GuiIcon;
import club.abelix.api.base.item.GameItem;
import club.abelix.api.base.item.GameItemCategory;
import club.abelix.api.base.item.GameItemStorage;
import club.abelix.api.base.item.gui.sort.GameItemSortMode;
import club.abelix.api.base.item.impl.kit.MythicKit;
import club.abelix.api.base.item.predicate.CoinsPredicate;
import club.abelix.api.base.item.predicate.GameItemPredicate;
import club.abelix.api.base.item.predicate.PredicateType;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.api.nms.sound.SoundType;
import club.abelix.api.nms.sound.type.YSound;
import club.abelix.api.server.lobby.mysterybox.content.CaseContent;
import club.abelix.commons.data.cases.CaseType;
import club.abelix.commons.data.cases.rarity.Rarity;
import club.abelix.commons.util.exclusive.ExclusivePeriod;
import club.abelix.commons.util.time.TimeUtil;
import club.abelix.commons.util.word.CommonWords;
import club.abelix.commons.util.word.Noun;
import club.abelix.network.common.logging.Logging;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by saharNooby on 23:29 17.10.2017
 */
public class GameItemGui extends Gui {

    // Меню со списком вещей в категории. Поддерживает выбор и покупку вещей.

    protected final GameItemCategory category;
    protected final PagedZone<GameItem> zone;

    protected GameItem selected;

    protected GameItemStorage storage;
    protected Class<? extends GameItemPurchaseConfirmGui> confirmGui;

    public GameItemGui(@NonNull Player player, @NonNull GameItemCategory category) {
        this(player, 5, category);
    }

    public GameItemGui(@NonNull Player player, @NonNull GameItemCategory category, GameItemStorage storage, Class<? extends GameItemPurchaseConfirmGui> confirmGui) {
        this(player, 5, category, storage, confirmGui);
    }

    public GameItemGui(@NonNull Player player, @NonNull GameItemCategory category, Gui parent) {
        this(player, 6, category, parent);
    }

    public GameItemGui(@NonNull Player player, @NonNull GameItemCategory category, Gui parent, GameItemStorage storage, Class<? extends GameItemPurchaseConfirmGui> confirmGui) {
        this(player, 6, category, parent, storage, confirmGui);
    }

    public GameItemGui(@NonNull Player player, int rows, @NonNull GameItemCategory category) {
        this(player, rows, category, null, null);
    }

    public GameItemGui(@NonNull Player player, int rows, @NonNull GameItemCategory category, GameItemStorage storage, Class<? extends GameItemPurchaseConfirmGui> confirmGui) {
        this(player, rows, category, null, storage, confirmGui);
    }

    public GameItemGui(@NonNull Player player, int rows, @NonNull GameItemCategory category, Gui parent) {
        this(player, rows, category, parent, null, GameItemPurchaseConfirmGui.class);
    }

    public GameItemGui(@NonNull Player player, int rows, @NonNull GameItemCategory category, Gui parent, GameItemStorage storage, Class<? extends GameItemPurchaseConfirmGui> confirmGui) {
        super(player, category.getWord().get(Gamer.getGamer(player).getLanguage()).plural().firstUp().toString(), rows);

        this.storage = storage != null ? storage : GameItemStorage.INSTANCE;
        assert storage != null;

        this.confirmGui = confirmGui;

        setParent(parent);

        this.category = category;
        this.selected = category.isSelectable() ? storage.getSelected(category, gamer) : null;

        this.zone = new PagedZone<>(this) {

            @Override
            protected Collection<GameItem> getItems() {
                return getSortedItems();
            }

            @Override
            protected void drawItem(GameItem item, int slot) {
                GuiIcon icon = getIcon(item);

                add(icon.getItem(), slot, icon.getAction());
            }

        };

        this.zone.addSlotRect(1, 1, 7, 3);

        draw();
    }

    @Override
    public void draw() {
        this.zone.draw();

        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        drawSortIcon();
        drawShopIcon(toSlot(4, rows - 1));

        if (this.category.isSelectable() && getParent() != null) {
            this.zone.drawButtons(toSlot(1, 5), toSlot(7, 5));

            drawBackIcon(toSlot(3, 5));
        } else {
            this.zone.drawButtons(toSlot(1, 5), toSlot(7, 5));

            if (getParent() != null) {
                drawBackIcon(toSlot(3, 5));
            }
        }
    }

    protected void drawBackIcon(int slot) {
        generateBackItem(slot, clickEvent -> {
            getParent().reopen();
            getParent().draw();
        });
    }

    protected void drawShopIcon(int slot) {
        generateShopItem(slot);
    }

    protected void drawSortIcon() {
        GameItemSortMode sortMode = getSortMode();

        ItemUtil.Builder builder = ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGQ1NmQ3ZWNlMWVjMWMwOWY0MzE0YTVmMWFiMGZiZDAzN2RkMTExYmNjZDM1OWRlZDQyN2QzNDg4YmY4NDkyYyJ9fX0=")
                .setName(language.getMessage("game_item_gui_sort_mode", "%current%", sortMode.getName(language)));

        builder.addLore(language.getMessageList("game_item_gui_sort_mode_lore",
                "%current%", sortMode.getName(language),
                "%next%", sortMode.next().getName(language)));

        add(builder.build(), toSlot(5, rows - 1), clickEvent -> changeSortMode());
    }

    protected GameItem getVisibleItem(GameItem gameItem) {
        if (gameItem.isUpgradeable()) {
            GameItem item = gameItem.getLastPurchased(gamer);

            if (item == null) {
                return gameItem;
            }

            if (item.getNext() == null) {
                return item;
            }

            return item.getNext();
        }

        return gameItem;
    }


    protected GuiIcon getIcon(@NonNull GameItem gameItem) {
        GameItem purchasedBest = gameItem.getLastPurchased(gamer);
        GameItem item = getVisibleItem(gameItem);

        Noun word = item.getCategory().getWord();

        ItemStack icon = item.getIcon(language);

        ClickAction action = null;

        PredicateMatcher ownMatch = new PredicateMatcher(gamer, item, PredicateType.OWNERSHIP, false);

        ItemMeta meta = icon.getItemMeta();
        List<String> lore = new ArrayList<>();

        Rarity rarity = gameItem.getRarity();

        if (item.getCategory() != null) {
            lore.add("§8" + ChatColor.stripColor(item.getCategory().getDisplayName(language)));
            lore.add("");
        }

        if (meta != null && meta.hasLore()) {
            lore.addAll(meta.getLore());
        }

        if (rarity != null && rarity != Rarity.NONE) {
            lore.add("");
            lore.add(gamer.getLanguage().getMessage("game_item_rarity",
                    "%rarity%", rarity.getColor() + rarity.name()));
        }

        if (ownMatch.isMatches() || gamer.getRanks().hasPermission("gameitem.bypass")) {
            if (item.isSelectable()) {
                boolean isSelected = selected != null && selected.equals(item);

                if (isSelected) {
                    lore.add("");
                    lore.add(language.getMessage("game_item_unselect_available"));

                    action = e -> {
                        // nothing

//                        if (!category.isDefault(selected)) {
//                            selected.onUnselect(player);
//                            selected = null;
//
//                            storage.clearSelection(category, gamer);
//
//                            gamer.sendLocalizedMessage("game_item_unselected_from",
//                                    "%label%", word.with(getLanguage(), CommonWords.THIS).gen());
//
//                            FalconAPI.getNmsApi().getSoundApi().playSound(player, SoundType.NOTIFICATION);
//                        }
//
//                        draw();
                    };
                } else {
                    PredicateMatcher selectMatch = new PredicateMatcher(gamer, item, PredicateType.SELECT, false);

                    if (selectMatch.isMatches()) {
                        lore.add("");
                        lore.add(translated("game_item_select_click",
                                "%label%", word.get(getLanguage()).acc()));

                        action = e -> {
                            if (selected != null) {
                                selected.onUnselect(player);
                            }

                            selected = purchasedBest;
                            selected.onSelect(player);

                            storage.selectItem(gamer, selected);

                            gamer.sendLocalizedMessage(
                                    "game_item_selected",
                                    "%label%", word.get(language).acc(),
                                    "%item%", selected.getDisplayName(language)
                            );

                            FalconAPI.getNmsApi().getSoundApi().playSound(player, SoundType.NOTIFICATION);

                            draw();
                        };
                    } else {
                        lore.add("");

                        appendFailedPredicates(item, lore, selectMatch);
                    }
                }
            }
        } else if (item.isExclusive()) {
            ExclusivePeriod period = item.getExclusivePeriod();

            String startString = TimeUtil.getDateString(language, period.getStartDate());
            String endString = TimeUtil.getDateString(language, period.getEndDate());

            lore.add(translated("game_item_exclusive_unavailable",
                    "%label%", word.with(getLanguage(), CommonWords.THIS).firstUp(),
                    "%start%", startString,
                    "%end%", endString));
        } else {
            List<Map.Entry<CaseType, CaseContent>> cases = item.getCases();

            if (!cases.isEmpty()) {
                lore.addAll(language.getMessageList("game_item_case_hint_title"));

                for (Map.Entry<CaseType, CaseContent> entry : cases) {
                    CaseType type = entry.getKey();
                    Rarity caseRarity = entry.getValue().getRarity();

                    lore.add(language.getMessage("game_item_case_hint_entry",
                            "%type%", type.getDisplayName(language),
                            "%rarity%", caseRarity.getColor() + caseRarity.getName(language)
                    ));
                }
            }

            if (item.isPurchasable()) {
                PredicateMatcher purchMatch = new PredicateMatcher(gamer, item, PredicateType.PURCHASE, true);

                if (purchMatch.isMatches()) {

                    CoinsPredicate coins = item.getCoinsPredicate();

                    if (coins != null) {
                        lore.addAll(coins.getPurchaseAvailableLore(gamer));
                    }

                    action = e -> { // ОЧЕНЬ ХУЕВО, //todo: переписать этот лютый пиздец
                        Logging.IMP.debug("[GameItem] Open confirm item gui {}", this.confirmGui);
                        try {
                            confirmGui.getMethod(
                                    "create", Gamer.class, GameItem.class, Gui.class
                            ).invoke(null, gamer, item, this);
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                            ex.printStackTrace();
                            gamer.sendLocalizedMessage("internal_error");
                        }
                    };
                } else {
                    lore.add("");

                    appendFailedPredicates(item, lore, purchMatch);
                }
            } else {
                lore.add("");

                appendFailedPredicates(item, lore, ownMatch);
            }
        }

        ItemUtil.Builder iconBuilder = ItemUtil.getBuilder(icon);

        // обновляем иконку которая будет отображаться в итоге
        iconBuilder.setType(icon.getType());
        iconBuilder.setDurability(icon.getDurability());

        iconBuilder.setLore(lore);

        if (action == null) {
            action = clickEvent -> gamer.playSound(YSound.VILLAGER_NO);
        }

        ClickAction finalAction = action;

        ClickAction clickAction = clickEvent -> {
            if (clickEvent.getClick() == ClickType.LEFT) {
                finalAction.perform(clickEvent);
            } else if (category.isPreviewAvailable() && clickEvent.getClick() == ClickType.RIGHT) {
                category.preview(item, gamer);
            }
        };

        return new GuiIcon(iconBuilder.build(), clickAction);
    }

    private void appendFailedPredicates(@NonNull GameItem item, List<String> append, PredicateMatcher matcher) {
        List<GameItemPredicate> failed = matcher.getFailed();

        for (GameItemPredicate predicate : failed) {
            List<String> lore = predicate.getRequirementLore(item, getGamer());

            append.addAll(lore);
        }
    }

    protected void changeSortMode() {
        GameItemSortMode mode = getSortMode();
        GameItemSortMode next = mode.next();

        gamer.setProperty("SORT/" + category.getId(), next.getId());
        gamer.sendLocalizedMessage("game_item_sort_mode_changed",
                "%mode%", next.getName(language));

        gamer.playSound(YSound.NOTE_PLING);

        // перерисовываем предметы
        zone.draw();

        // перересовываем иконку фильтра
        drawSortIcon();
    }


    protected GameItemSortMode getSortMode() {
        int filterMode = gamer.getProperty("SORT/" + category.getId());

        return GameItemSortMode.getById(filterMode);
    }

    private List<GameItem> getSortedItems() {
        List<GameItem> items = new ArrayList<>(storage.getItems(category));

        items.removeIf(item -> item instanceof MythicKit);

        // сортируем предметы по режиму игрока
        getSortMode().sort(gamer, items);

        return items;
    }

}
