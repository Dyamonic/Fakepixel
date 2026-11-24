package club.abelix.api.base.hotbar.common;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.util.item.ItemUtil;
import lombok.NonNull;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class CommonHotbars {

    /**
     * Предмет для выбора режиме
     */
    private static final ItemStack GAME_MODE_SELECT_ICON = ItemUtil.getBuilder(Material.NETHER_STAR)
            .setName("§bВыбор режима §7(ПКМ)")
            .setLore("§7Используйте этот предмет", "§7для выбора режима")
            .build();

    /**
     * Предмет для выбора лобби
     */
    private static final ItemStack LOBBY_SELECT_ICON = ItemUtil.getBuilder(Material.SLIME_BALL)
            .setName("§bВыбор лобби §7(ПКМ)")
            .setLore("§7Используйте этот предмет", "§7для выбора лобби")
            .build();

    private static void registerHub() {
        String id = "hub";

//        HotbarManager.INSTANCE.registerIcon(id, 0, GAME_MODE_SELECT_ICON, ServerSelectorGui::new);

//        if (CoreUtil.isLobby()) {
//            Lobby lobby = CoreUtil.getLobby();
//
//            HotbarManager.INSTANCE.registerIcon(
//                    id, 8, LOBBY_SELECT_ICON,
//                    clicker -> new LobbySelectorGui(clicker, lobby == Lobby.HUB ? "Выбор хаба" : "Выбор лобби")
//            );
//        }
    }

    public static void registerShopIcon(@NonNull Consumer<Gamer> callback) {
        ItemStack shopItem = ItemUtil.getBuilder(Material.CHEST).setName("§eПокупка и выбор вещей")
                .setLore("§7Покупка различных предметов", "§7для данного режима").build();

        HotbarManager.INSTANCE.registerIcon("hub", 6, shopItem, callback);
    }

}
