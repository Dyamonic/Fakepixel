package club.abelix.games.buildffa.game.gui.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.commons.language.Lang;
import club.abelix.games.buildffa.game.kit.BaseKit;
import club.abelix.games.buildffa.game.kit.registry.KitRegistry;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LayoutChangeGui extends Gui {
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

    public LayoutChangeGui(final @NotNull Player player) {
        super(player, Lang.of("buildffa_layout_change_gui_title"), 6);

        draw();
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);
        addKitSelectionItems();
    }

    private void addKitSelectionItems() {
        val kits = KitRegistry.REGISTRY.getKits();

        for (val kit : kits) {
            val slot = kitSlots.getOrDefault(kit.getId(), -1);
            if (slot == -1) continue;

            val kitItem = createKitItem(kit);
            add(kitItem, slot, event -> {
                if (event.getWhoClicked() instanceof Player viewer)
                    new KitViewerGui(viewer, kit).reopen();
            });
        }
    }

    private ItemStack createKitItem(final @NotNull BaseKit kit) {
        val language = Gamer.getGamer(player).getLanguage();
        val item = kit.getIcon(language);
        val meta = item.getItemMeta();

        if (meta != null) {
            val kitName = language.getMessage("gui_kit_name", "%kit_name%", kit.getName());

            meta.setDisplayName(kitName);
            meta.setLore(List.of(language.getMessage("gui_kit_layout_change_lore")));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            item.setItemMeta(meta);
        }

        return item;
    }
}
