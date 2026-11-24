package club.abelix.games.buildffa.game.gui.standard;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gui.Gui;
import club.abelix.api.base.util.item.ItemUtil;
import club.abelix.commons.data.level.Leveling;
import club.abelix.commons.language.Lang;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.division.AbstractDivision;
import club.abelix.games.buildffa.game.manager.progression.PrestigeManager;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class DivisionGui extends Gui {
    private static final ItemStack DIVISION_LIST_ITEMSTACK;
    private final PrestigeManager prestigeManager;

    static {
        DIVISION_LIST_ITEMSTACK = ItemUtil.getBuilder(Material.PAPER)
                .setName(Lang.of("buildffa_division_gui_list_name"))
                .setLore(Lang.of("buildffa_division_gui_list_lore"))
                .build();
    }

    public DivisionGui(Player player, PrestigeManager prestigeManager) {
        super(player, Lang.of("buildffa_division_gui_title"), 4);
        this.prestigeManager = prestigeManager;
        draw();
    }

    @Override
    public void draw() {
        drawDynamicBorders();

        val gamer = Gamer.getGamer(player);
        val rating = gamer.getLifetimeStat("rating");
        val division = AbstractDivision.getRankForRating(rating);

        add(createPlayerHead(player, division, rating), toSlot(4, 1));
        add(DIVISION_LIST_ITEMSTACK, toSlot(4, 2));
        add(createPrestigeItem(gamer, division), toSlot(4, 3), event -> prestigeManager.openLevels((Player) event.getWhoClicked()));
    }

    private ItemStack createPlayerHead(Player player, AbstractDivision division, int rating) {
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setOwner(player.getName());
            meta.setDisplayName(player.getDisplayName());
            meta.setLore(createLore(player, division, rating));
            head.setItemMeta(meta);
        }

        return head;
    }

    private List<String> createLore(Player player, AbstractDivision division, int rating) {
        val gamer = Gamer.getGamer(player);
        val buildFFAGamer = BuildFFAGamer.get(gamer);
        val language = gamer.getLanguage();

        val nextRating = getNextRating(division, rating);
        val countForNextRating = getCountForNextRating(division, rating);

        val currentProgress = rating - division.getRequiredRating();
        val neededRating = nextRating - division.getRequiredRating();
        val percent = currentProgress / neededRating * 100;

        val levelScale = Leveling.getScale(10, Leveling.getPercentageToNextLevel(percent), "▮", "§a", "§7");

        return language.getMessageList("buildffa_division_gui",
                "%prestige_multiplier%", prestigeManager.formatMultiplier(buildFFAGamer.getPrestige()),
                "%next_prestige_multiplier%", prestigeManager.getNextMultiplier(gamer),
                "%prestige_icon%", buildFFAGamer.getPrestigeIcon().trim(),
                "%prestige%", String.valueOf(buildFFAGamer.getPrestige()),
                "%prestige_status%", prestigeManager.getStatus(gamer),
                "%division_name%", division.getName(gamer.getLanguage()),
                "%current_rating%", String.valueOf(rating),
                "%level_scale%", levelScale,
                "%next_rating%", String.valueOf(nextRating),
                "%count_for_next_rating%", countForNextRating,
                "%percent%", percent
        );
    }

    private ItemStack createPrestigeItem(Gamer gamer, AbstractDivision division) {
        BuildFFAGamer buildFFAGamer = BuildFFAGamer.get(gamer);
        String prestigeIcon = buildFFAGamer.getPrestigeIcon().trim();
        String prestigeStatus = prestigeManager.getStatus(gamer);
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(language.getMessage("buildffa_prestige_gui_name",
                "%prestige_multiplier%", prestigeManager.formatMultiplier(buildFFAGamer.getPrestige()),
                "%next_prestige_multiplier%", prestigeManager.getNextMultiplier(gamer),
                "%prestige_icon%", prestigeIcon,
                "%prestige%", String.valueOf(buildFFAGamer.getPrestige()),
                "%prestige_status%", prestigeStatus,
                "%division_name%", division.getName(language)));
        meta.setLore(language.getMessageList("buildffa_prestige_gui_lore",
                "%prestige_multiplier%", prestigeManager.formatMultiplier(buildFFAGamer.getPrestige()),
                "%next_prestige_multiplier%", prestigeManager.getNextMultiplier(gamer),
                "%prestige_icon%", prestigeIcon,
                "%prestige%", String.valueOf(buildFFAGamer.getPrestige()),
                "%prestige_status%", prestigeStatus,
                "%division_name%", division.getName(language),
                "%prestige_requirement%", AbstractDivision.RUBY.getName(language)));
        item.setItemMeta(meta);
        return item;
    }

    private int getNextRating(AbstractDivision division, int currentRating) {
        AbstractDivision nextDivision = getNextDivision(division);
        return nextDivision != null ? nextDivision.getRequiredRating() - currentRating : 0;
    }

    private String getCountForNextRating(AbstractDivision division, int currentRating) {
        AbstractDivision nextDivision = getNextDivision(division);
        return nextDivision != null ? String.valueOf(nextDivision.getRequiredRating() - currentRating) : "Максимальный уровень";
    }

    private AbstractDivision getNextDivision(AbstractDivision currentDivision) {
        AbstractDivision[] divisions = AbstractDivision.values();
        int ordinal = currentDivision.ordinal();

        return ordinal < divisions.length - 1 ? divisions[ordinal + 1] : null;
    }
}
