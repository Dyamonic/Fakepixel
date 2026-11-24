package club.abelix.games.buildffa.game.gui.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.commons.language.Lang;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.kit.BaseKit;
import club.abelix.games.buildffa.game.kit.standard.*;
import club.abelix.games.buildffa.game.purchase.Purchase;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class KitGui extends Gui {
    private final List<BaseKit> kits = Arrays.asList(
            new BasicKitItem(), new ArcherKitItem(), new FishermanKitItem(),
            new AssassinKitItem(), new TankKitItem(), new PyroKitItem(),
            new CrazyKitItem(), new WitchKitItem(), new UHCKitItem(),
            new SnowKitItem(), new LumberjackKitItem()
    );

    private final Map<Integer, Integer> kitSlots = new HashMap<>() {{
        put(8, 13); // kit_basic
        put(10, 29); // kit_archer
        put(6, 30); // kit_fisherman
        put(9, 31); // kit_assassin
        put(11, 32); // kit_tank
        put(4, 33); // kit_pyro
        put(7, 38); // kit_crazy
        put(1, 39); // kit_witch
        put(2, 40); // kit_uhc
        put(3, 41); // kit_snow
        put(5, 42); // kit_lumberjack
    }};

    private final BuildFFAGame game;

    public KitGui(Player player, BuildFFAGame game) {
        super(player, Lang.of("buildffa_kit_editor_gui_title"), 6);

        this.game = game;

        draw();
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        for (BaseKit kit : kits) {
            int slot = kitSlots.getOrDefault(kit.getId(), -1);
            if (slot == -1) continue;

            ItemStack kitItem = kit.getIcon(Gamer.getGamer(player).getLanguage()).clone();
            boolean isAvailable = kit.isAvailableTo(getPlayer());

            if (kit.getFreePermission() != null) {
                kitItem = addLore(kitItem, language.getMessage("gui_kit_available_from_group"));
            }

            if (isAvailable) {
                kitItem = addLore(kitItem, language.getMessage(game.getSelectedKit(gamer).getId() == kit.getId()
                        ? "gui_selected"
                        : "gui_available"));
            } else {
                kitItem = addLore(kitItem, language.getMessage("gui_kit_click_to_buy"));
            }

            hideAttributes(kitItem);
            add(kitItem, slot, event -> handleKitSelection(event.getClick(), kit, (Player) event.getWhoClicked()));
        }
    }

    private ItemStack addLore(ItemStack item, String loreText) {
        ItemStack newItem = item.clone();
        ItemMeta meta = newItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = Collections.singletonList(loreText);
            } else {
                lore.add(loreText);
            }
            meta.setLore(lore);
            newItem.setItemMeta(meta);
        }
        return newItem;
    }

    private void hideAttributes(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
    }

    private void handleKitSelection(ClickType click, BaseKit kit, Player player) {
        if (click.isRightClick()) {
            new KitViewerGui(player, kit).reopen();
            return;
        }

        if (kit.isAvailableTo(player)) {
            game.setSelectedKit(player, kit);
        } else {
            handleKitPurchase(kit, player);
        }
    }

    private void handleKitPurchase(BaseKit kit, Player player) {
        val purchase = Purchase.builder()
                .coinPrice(kit.getPrice())
                .onSuccess(() -> {
                    kit.purchaseKit(player);
                    game.setSelectedKit(player, kit);

                    gamer.sendLocalizedMessage("buildffa_kit_selected",
                            "%name%", kit.getDisplayName(language));

                    draw();
                    player.updateInventory();
                })
                .player(player)
                .build();

        purchase.execute();
    }
}
