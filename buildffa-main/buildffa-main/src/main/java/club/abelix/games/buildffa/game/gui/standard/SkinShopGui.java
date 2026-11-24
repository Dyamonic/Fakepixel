package club.abelix.games.buildffa.game.gui.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.ClickAction;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.language.Lang;
import club.abelix.commons.language.Language;
import club.abelix.games.buildffa.game.dto.SkinDTO;
import club.abelix.games.buildffa.game.dto.dao.SkinDAO;
import club.abelix.games.buildffa.game.dto.dao.impl.SkinDAOImpl;
import club.abelix.games.buildffa.game.purchase.Purchase;
import club.abelix.games.buildffa.game.utility.ConvertUtility;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SkinShopGui extends Gui {

    private final Language language;
    private final int playerId;
    private final SkinDAO skinDAO;

    public SkinShopGui(Gui parent, Player player) {
        super(player, Lang.of("buildffa_block_shop_gui_title"), 6);

        setParent(parent);

        this.language = Gamer.getGamer(player).getLanguage();
        this.playerId = Gamer.getGamer(player).getPlayerId();
        this.skinDAO = SkinDAOImpl.getInstance();

        draw();
    }

    @Override
    public void draw() {
        drawBorder(Gui.BORDER_SLOTS_FOR_SIX_ROWS_GUI);

        addItem(Material.SANDSTONE, 1, 20, "block_shop_sandstone", 1);
        addItem(Material.BRICK, 1, 21, "block_shop_brick", 45000);
        addItem(Material.STONE, 1, 22, "block_shop_andesite", 45000, (short) 5);
        addItem(Material.RED_SANDSTONE, 1, 23, "block_shop_red_sandstone", 45000);
        addItem(Material.WOOD, 1, 24, "block_shop_jungle_planks", 90000);
        addItem(Material.WOOD, 1, 29, "block_shop_birch_planks", 90000, (short) 2);
        addItem(Material.STAINED_CLAY, 1, 30, "block_shop_yellow_hard_clay", 90000, (byte) 4);
        addItem(Material.QUARTZ_BLOCK, 1, 31, "block_shop_quartz_block", 90000);
        addItem(Material.IRON_BLOCK, 1, 32, "block_shop_iron_block", 170000);
        addItem(Material.GOLD_BLOCK, 1, 33, "block_shop_gold_block", 180000);
        addItem(Material.EMERALD_BLOCK, 1, 38, "block_shop_emerald_block", 180000);
        addItem(Material.DIAMOND_BLOCK, 1, 39, "block_shop_diamond_block", 190000);
        addItem(Material.OBSIDIAN, 1, 40, "block_shop_obsidian", 200000);
        addItem(Material.COAL_ORE, 1, 41, "block_shop_coal_ore", 210000);
        addItem(Material.DIAMOND_ORE, 1, 42, "block_shop_diamond_ore", 230000);
        addItem(Material.GLOWSTONE, 1, 11, "block_shop_glowstone", 240000);
        addItem(Material.SEA_LANTERN, 1, 47, "block_shop_sea_lantern", 250000);
        addItem(Material.REDSTONE_LAMP_OFF, 1, 48, "block_shop_redstone_lamp", 260000);
        addItem(Material.SLIME_BLOCK, 1, 49, "block_shop_slime", 270000);
        addItem(Material.SOUL_SAND, 1, 50, "block_shop_soul_sand", 280000);
        addItem(Material.PACKED_ICE, 1, 51, "block_shop_packed_ice", 290000);
    }

    private void addItem(Material material, int amount, int slot, String nameKey, int price) {
        addItem(material, amount, slot, nameKey, price, (short) 0);
    }

    private void addItem(Material material, int amount, int slot, String nameKey, int price, short durability) {
        String name = language.getMessage(nameKey);

        skinDAO.isSkinPurchased(playerId, name).thenAccept(purchased -> {
            String loreKey = purchased ? "skin_item_lore_without_price" : "skin_item_lore_with_price";
            String lore = language.getMessage(loreKey, "%price%", price, "%golds%", ConvertUtility.toGold(price));

            ItemStack item = ItemUtil.getBuilder(material)
                    .setAmount(amount)
                    .setDurability(durability)
                    .setName(name)
                    .setLore(lore)
                    .build();
            add(item, slot, createClickAction(playerId, name, price, purchased));
        });
    }

    private ClickAction createClickAction(int playerId, String skinId, int price, boolean purchased) {
        return event -> {
            Player player = (Player) event.getWhoClicked();

            if (purchased) {
                handleSkinSelection(player, playerId, skinId, event.getCurrentItem().getType(), event.getCurrentItem().getData().getData());
            } else {
                handleSkinPurchase(player, playerId, skinId, price);
            }

            event.setCancelled(true);
        };
    }

    private void handleSkinSelection(Player player, int playerId, String skinId, Material material, byte materialData) {
        SkinDTO skinDTO = SkinDTO.builder()
                .playerId(playerId)
                .skinId(skinId)
                .material(material)
                .materialData(materialData)
                .build();

        skinDAO.setSelectedSkin(skinDTO).thenAccept(result -> {
            player.sendMessage(language.getMessage("selected_skin_message").replace("%skin_name%", skinId));
        });
    }

    private void handleSkinPurchase(Player player, int playerId, String skinId, int price) {
        val purchase = Purchase.builder()
                .onSuccess(() -> skinDAO.addPlayerSkin(playerId, skinId))
                .coinPrice(price)
                .player(player)
                .build();

        purchase.execute();
    }
}
