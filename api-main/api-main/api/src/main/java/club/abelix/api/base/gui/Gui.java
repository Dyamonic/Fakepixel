package club.abelix.api.base.gui;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.slots.BeautifulSlotsProvider;
import club.abelix.api.base.gui.slots.impl.OneSlotBorderSlotsProvider;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.commons.project.Project;
import club.abelix.core.bukkit.HeartBeat;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;


@Getter
public abstract class Gui implements InventoryHolder {

    protected static final BeautifulSlotsProvider ONE_SLOT_BORDER_SLOTS_PROVIDER = OneSlotBorderSlotsProvider.INSTANCE;

    protected static final int[][] BEAUTIFUL_SLOT_POSITIONS = {
            {13},
            {11, 15},
            {11, 13, 15},
            {10, 12, 14, 16},
            {19, 21, 23, 25, 31},
            {10, 11, 12, 14, 15, 16},
            {10, 12, 14, 16, 29, 31, 33},
            {10, 12, 14, 16, 28, 30, 32, 34},
            {10, 11, 12, 13, 14, 15, 16, 21, 23},
            {10, 11, 12, 13, 14, 15, 16, 19, 21, 23},
            {10, 11, 12, 13, 14, 15, 16, 19, 21, 23, 25},
            {10, 11, 12, 13, 14, 15, 16, 19, 20, 22, 24, 25},
            {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 23, 24, 25},
            {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25},
            {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 26},
            {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29},
            {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30}
    };

    public static final int[] BORDER_SLOTS_FOR_SIX_ROWS_GUI = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 18, 27, 36, 45, 17, 26, 35, 44, 53};

    protected static final ItemStack DATA_IS_LOADING_ICON = ItemUtil.getBuilder(Material.BARRIER)
            .setName(Lang.of("gui_loading_icon_name"))
            .setLore(Lang.of("gui_loading_icon_lore"))
            .build();

    protected static final ItemStack BACK_TO_MENU_ICON = ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmU5YWU3YTRiZTY1ZmNiYWVlNjUxODEzODlhMmY3ZDQ3ZTJlMzI2ZGI1OWVhM2ViNzg5YTkyYzg1ZWE0NiJ9fX0=")
            .setName(Lang.of("gui_back_to_menu_icon_name"))
            .setLore(Lang.of("gui_back_to_menu_icon_lore"))
            .build();

    protected static final ItemStack CLOSE_MENU_ICON = ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjEyZjNlZmU4NGEwZjY2NDZhODBkNDVjZWZlNDE4ZTE5OWQ5NjE5ZjhjMWZiNWY1YzVjMDA4YzYwMzA1OWFjMyJ9fX0=")
            .setName(Lang.of("gui_close_menu_icon_name"))
            .build();

    @Getter
    protected final Player player;
    protected final String title; //Only for debug will be removed soon
    @Getter
    protected final Gamer gamer;

    protected final Language language;

    protected final int size;

    protected final int rows;

    private final Inventory inventory;

    private final ClickAction[] actions;

    @Setter
    private boolean allowPlayerInvInteraction = false;

    @Setter
    private boolean allowPlayerMoveItem = false;

    private final List<Integer> allowedMoveSlots = new ArrayList<>();

    @Setter
    private Gui parent;
    private BukkitTask repaintTask;

    public Gui(@NonNull Player player, @NonNull Lang lang, int rows) {
        this(player, lang.compile(), rows);
    }

    public Gui(@NonNull Gamer gamer, @NonNull Lang lang, int rows) {
        this(gamer.getPlayer(), lang.compile(), rows);
    }

    public Gui(@NonNull Player player, @NonNull String title, int rows) {
        Preconditions.checkArgument(rows >= 0 && rows <= 6, "Invalid rows count: " + rows);
        this.player = player;

        this.gamer = Gamer.getGamer(player);
        this.language = gamer.getLanguage();
        this.title = title;
        this.size = rows * 9;
        this.rows = rows;

        this.inventory = Bukkit.createInventory(this, size, title);
        this.actions = new ClickAction[size];

        this.reopen();

        HeartBeat.runAsync(() -> {
            if (isOpened()) {
                player.updateInventory();
            }
        });
    }

    public static Gui getOpened(@NonNull Player player) {
        InventoryView inv = player.getOpenInventory();

        if (inv != null) {
            InventoryHolder holder = inv.getTopInventory().getHolder();

            if (holder instanceof Gui) {
                return (Gui) holder;
            }
        }

        return null;
    }

    public static <T extends Gui> void drawIf(@NonNull Player player, @NonNull Class<T> cls, @NonNull Predicate<T> check) {
        InventoryView inv = player.getOpenInventory();

        if (inv != null) {
            InventoryHolder holder = inv.getTopInventory().getHolder();

            if (holder == null) {
                return;
            }

            if (cls.isAssignableFrom(holder.getClass())) {
                T gui = cls.cast(holder);

                if (check.test(gui)) {
                    gui.draw();
                }
            }
        }
    }

    public static <T extends Gui> void drawAllIf(@NonNull Class<T> cls, @NonNull Predicate<T> check) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            drawIf(player, cls, check);
        }
    }

    public static <T extends Gui> void forEachOpened(@NonNull Class<T> cls, @NonNull Consumer<T> callback) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryView inv = player.getOpenInventory();

            if (inv != null) {
                InventoryHolder holder = inv.getTopInventory().getHolder();

                if (holder == null) {
                    return;
                }

                if (holder.getClass() == cls || cls.isAssignableFrom(holder.getClass())) {
                    callback.accept(cls.cast(holder));
                }
            }
        }
    }

    /**
     * Преобразует координаты слота в индекс инвентаря.
     *
     * Для двойного сундука (6 рядов по 9 слотов):
     * y=0:  0, 1, 2, 3, 4, 5, 6, 7, 8
     * y=1:  9,10,11,12,13,14,15,16,17
     * y=2: 18,19,20,21,22,23,24,25,26
     * y=3: 27,28,29,30,31,32,33,34,35
     * y=4: 36,37,38,39,40,41,42,43,44
     * y=5: 45,46,47,48,49,50,51,52,53
     *
     * Примеры через toSlot:
     * y=0: toSlot(0,0)=0, toSlot(1,0)=1, toSlot(2,0)=2, toSlot(3,0)=3, toSlot(4,0)=4, toSlot(5,0)=5, toSlot(6,0)=6, toSlot(7,0)=7, toSlot(8,0)=8
     * y=1: toSlot(0,1)=9, toSlot(1,1)=10, toSlot(2,1)=11, toSlot(3,1)=12, toSlot(4,1)=13, toSlot(5,1)=14, toSlot(6,1)=15, toSlot(7,1)=16, toSlot(8,1)=17
     * y=2: toSlot(0,2)=18, toSlot(1,2)=19, toSlot(2,2)=20, toSlot(3,2)=21, toSlot(4,2)=22, toSlot(5,2)=23, toSlot(6,2)=24, toSlot(7,2)=25, toSlot(8,2)=26
     * y=3: toSlot(0,3)=27, toSlot(1,3)=28, toSlot(2,3)=29, toSlot(3,3)=30, toSlot(4,3)=31, toSlot(5,3)=32, toSlot(6,3)=33, toSlot(7,3)=34, toSlot(8,3)=35
     * y=4: toSlot(0,4)=36, toSlot(1,4)=37, toSlot(2,4)=38, toSlot(3,4)=39, toSlot(4,4)=40, toSlot(5,4)=41, toSlot(6,4)=42, toSlot(7,4)=43, toSlot(8,4)=44
     * y=5: toSlot(0,5)=45, toSlot(1,5)=46, toSlot(2,5)=47, toSlot(3,5)=48, toSlot(4,5)=49, toSlot(5,5)=50, toSlot(6,5)=51, toSlot(7,5)=52, toSlot(8,5)=53
     */
    public static int toSlot(int x, int y) {
        return y * 9 + x;
    }

    public static ClickAction compose(ClickAction a, ClickAction b) {
        return a != null && b != null ? e -> {
            a.perform(e);
            b.perform(e);
        } : a != null ? a : b != null ? b : e -> {
        };
    }

    /**
     * Full gui redraw
     */
    public void draw() {

    }

    protected Language getLanguage() {
        return language;
    }

    protected String translated(@NonNull String key, Object... objects) {
        return language.getMessage(key, objects);
    }

    protected List<String> translatedLore(@NonNull String key, Object... objects) {
        return language.getMessageList(key, objects);
    }

    protected void repaintAfter(int sec) {
        if (repaintTask != null) {
            repaintTask.cancel();
        }

        this.repaintTask = HeartBeat.runTimerAsync(() -> {
            if (isOpened()) {
                draw();

                player.updateInventory();
            } else {
                repaintTask.cancel();
            }
        }, sec * 20L, sec * 20L);
    }

    public void add(ItemStack item, int slot, ClickAction action) {
        Preconditions.checkArgument(slot >= 0 && slot <= size, "Slot is out of inventory bounds: " + slot + ", " + size);

        inventory.setItem(slot, item);

        actions[slot] = action;
    }

    public void add(ItemStack item, int slot) {
        add(item, slot, null);
    }

    public void remove(int slot) {
        add(null, slot);
    }

    public void removeRange(int from, int len) {
        for (int i = 0; i < len; ++i) {
            remove(from + i);
        }
    }

    public ItemStack get(int slot) {
        return inventory.getItem(slot);
    }

    public ClickAction getAction(int slot) {
        return actions[slot];
    }

    public boolean isOpened() {
        return player.getOpenInventory().getTopInventory().getHolder() == this;
    }

    public void reopen() {
        if (!isOpened()) {
            //if (player.getOpenInventory() != null) {
            //  WAPI.getNmsApi().getSoundApi().playSound(player, SoundType.CLICK);
            //}

            player.openInventory(inventory);
        }
    }

    protected void generateBackItem(@NonNull ItemStack itemStack, int slot, ClickAction clickAction) {
        add(itemStack, slot, clickAction);
    }

    protected void generateBackItem(int slot, ClickAction clickAction) {
        generateBackItem(BACK_TO_MENU_ICON, slot, clickAction);
    }

    protected void generateShopItem(int slot) {
        val itemBuilder = ItemUtil.getSkullBuilder("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI4MDMyOWRjMDNhZTU0MWE3YjNmYjdlMWNmZTYzYThkZDg1ZjNlY2I5N2YzNTNiMDkyYWQ2YzBlMGZiMWM5NSJ9fX0=");

        int amount = 0;

        if (CoreUtil.getMaster() == null) {
            amount = gamer.getGold();
            itemBuilder.setName(translated("gui_shop_currency_gold", "%amount%", Leveling.formatGrouped(amount)));
        } else {
            amount = gamer.getCoins(CoreUtil.getMaster());
            itemBuilder.setName(translated("gui_shop_currency_coins", "%amount%", Leveling.formatGrouped(amount)));
        }

        itemBuilder.setLore(translatedLore("gui_shop_icon_lore"));

        add(itemBuilder.build(), slot, null);
    }


    protected void generateCloseItem() {
        add(CLOSE_MENU_ICON, toSlot(4, rows - 1), e -> player.closeInventory());
    }

    protected void generateCloseItem(int slot) {
        add(CLOSE_MENU_ICON, slot, e -> player.closeInventory());
    }

    public void drawBorder(final int[] slots) {

        val item = ItemUtil.getBuilder(Material.STAINED_GLASS_PANE).setDurability(3).build();

        for (int slot : slots) {
            add(item, slot);
        }
    }

    public void drawDynamicBorders() {
        val item = ItemUtil.getBuilder(Material.STAINED_GLASS_PANE).setDurability(3).build();

        for (int slot = 0; slot < 9; slot++) {
            add(item, slot);
        }

        for (int i = 0; i < rows; i++) {
            add(item, i * 9);
            add(item, i * 9 + 8);
        }
    }

    public final void handleDrag(@NonNull InventoryDragEvent event) {
        event.setCancelled(true);
        event.setCursor(null);

        /*if (allowPlayerInvInteraction) {
            for (int slot : event.getRawSlots()) {
                if (slot >= 0 && slot < size) {
                    return;
                }
            }

            event.setCancelled(false);
        }

         */
    }

    public final void handleClick(@NonNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot >= 0 && slot < size) {
            // если передвигать можно, то проверяем, является
            // ли кликнутый слот разрешенным для передвижения
            if (allowPlayerInvInteraction) {
                // todo если будут баги, фиксить здесь
                InventoryAction action = event.getAction();

                if (action != InventoryAction.PICKUP_ALL
                        && action != InventoryAction.PLACE_ALL
                        && action != InventoryAction.SWAP_WITH_CURSOR) {
                    return;
                }

                if (allowedMoveSlots.contains(slot)) {
                    event.setCancelled(false);
                    return; // если двигать можно, не надо производить никаких действий дальше
                } else if (action == InventoryAction.SWAP_WITH_CURSOR) {
                    event.setCancelled(true);
                    return;
                }
            }

            ClickAction action = actions[slot];

            if (event.getCursor() != null && action == null) {
                return;
            }

            if (action != null) {
                action.perform(event);
            }
        }
    }

    public final void handleClose(@NonNull InventoryCloseEvent event) {
        event.getPlayer().setItemOnCursor(null);

        if (repaintTask != null) {
            repaintTask.cancel();
        }

        onClose();
    }

    public void onClose() {

    }

}

