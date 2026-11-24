package club.abelix.games.buildffa.game.manager.progression;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.board.BuildFFABoard;
import club.abelix.games.buildffa.game.data.BuildFFAGamer;
import club.abelix.games.buildffa.game.division.AbstractDivision;
import club.abelix.games.buildffa.game.gui.standard.DivisionGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PrestigeManager extends YListener implements Listener {
    public static final String PRESTIGE_PROPERTY = BuildFFAGamer.PRESTIGE_PROPERTY_NAME;
    public static final int MAX_PRESTIGE = 90;

    private final BuildFFAGame game;
    public PrestigeManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;
    }

    public int getPrestige(Gamer gamer) {
        return Math.max(0, gamer.getProperty(PRESTIGE_PROPERTY));
    }

    public String getIcon(Gamer gamer) {
        return getIcon(getPrestige(gamer));
    }

    public String getIcon(int prestige) {
        return BuildFFAGamer.getPrestigeIcon(Math.clamp(prestige, 0, MAX_PRESTIGE));
    }

    public double getMultiplier(Gamer gamer) {
        return getMultiplier(getPrestige(gamer));
    }

    public int applyMultiplier(Gamer gamer, int amount) {
        return (int) ((long) amount * (100 + Math.min(getPrestige(gamer), MAX_PRESTIGE) * 2) / 100);
    }

    public double getMultiplier(int prestige) {
        return 1.0D + Math.clamp(prestige, 0, MAX_PRESTIGE) * 0.02D;
    }

    public String formatMultiplier(int prestige) {
        return String.format(java.util.Locale.ROOT, "%.2fx", getMultiplier(prestige));
    }

    public String getStatus(Gamer gamer) {
        return gamer.getLanguage().getMessage(getPrestige(gamer) >= MAX_PRESTIGE
                ? "buildffa_prestige_maximum"
                : canPrestige(gamer) ? "buildffa_prestige_eligible" : "buildffa_prestige_requires_ruby");
    }

    public String getNextMultiplier(Gamer gamer) {
        return getPrestige(gamer) >= MAX_PRESTIGE ? getStatus(gamer) : formatMultiplier(getPrestige(gamer) + 1);
    }

    public boolean canPrestige(Gamer gamer) {
        return BuildFFAGamer.get(gamer).getRank() == AbstractDivision.RUBY
                && getPrestige(gamer) < MAX_PRESTIGE;
    }

    public void prestige(Player player) {
        openConfirmation(player);
    }

    public void openLevels(Player player) {
        openLevels(player, Math.min(getPrestige(Gamer.getGamer(player)), MAX_PRESTIGE) / 21);
    }

    private void openLevels(Player player, int requestedPage) {
        int page = Math.clamp(requestedPage, 0, MAX_PRESTIGE / 21);
        Gamer gamer = Gamer.getGamer(player);
        int currentPrestige = getPrestige(gamer);

        Inventory inv = Bukkit.createInventory(
            new PrestigeLevelsHolder(page),
            45,
            gamer.getLanguage().getMessage("buildffa_prestige_levels_title")
        );

        ItemStack filler = named(
            Material.STAINED_GLASS_PANE,
            (short) 15,
            " ",
            Collections.emptyList()
        );

        for (int i = 0; i < inv.getSize(); i++) {
        inv.setItem(i, filler);
        }

        for (int index = 0; index < 21; index++) {
            int prestige = page * 21 + index;
            if (prestige > MAX_PRESTIGE) break;
            inv.setItem(10 + (index / 7) * 9 + index % 7,
                    createPrestigeLevelItem(gamer, prestige, currentPrestige));
        }
        if (page > 0) inv.setItem(36, named(Material.ARROW, (short) 0,
                gamer.getLanguage().getMessage("buildffa_prestige_page_previous"), Collections.emptyList()));
        if (page < MAX_PRESTIGE / 21) inv.setItem(44, named(Material.ARROW, (short) 0,
                gamer.getLanguage().getMessage("buildffa_prestige_page_next"), Collections.emptyList()));
        inv.setItem(38, named(Material.NETHER_STAR, (short) 0,
                getStatus(gamer), gamer.getLanguage().getMessageList("buildffa_prestige_multiplier_lore",
                        "%prestige_multiplier%", formatMultiplier(currentPrestige),
                        "%next_prestige_multiplier%", getNextMultiplier(gamer),
                        "%prestige_status%", getStatus(gamer))));
        if (canPrestige(gamer)) inv.setItem(42, named(Material.EMERALD, (short) 0,
                gamer.getLanguage().getMessage("buildffa_prestige_purchase"), Collections.emptyList()));

        inv.setItem(40, named(
            Material.BARRIER,
            (short) 0,
            gamer.getLanguage().getMessage("buildffa_prestige_levels_back"),
            gamer.getLanguage().getMessageList("buildffa_prestige_levels_back_lore")
        ));

        player.openInventory(inv);
    }

    private ItemStack createPrestigeLevelItem(Gamer gamer, int displayedPrestige, int playerPrestige) {
        boolean unlocked = displayedPrestige <= playerPrestige;
        Material material = unlocked ? Material.PAPER : Material.BOOK;
        String state;
        if (displayedPrestige == playerPrestige) {
            state = gamer.getLanguage().getMessage("buildffa_prestige_level_current");
        } else if (unlocked) {
            state = gamer.getLanguage().getMessage("buildffa_prestige_level_unlocked");
        } else {
            state = gamer.getLanguage().getMessage("buildffa_prestige_level_locked");
        }

        String icon = getIcon(displayedPrestige).trim();
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(gamer.getLanguage().getMessage("buildffa_prestige_level_name",
                "%prestige_icon%", icon,
                "%prestige%", String.valueOf(displayedPrestige))));
        List<String> lore = gamer.getLanguage().getMessageList("buildffa_prestige_level_lore",
                "%prestige_icon%", icon,
                "%prestige%", String.valueOf(displayedPrestige),
                "%prestige_status%", displayedPrestige == MAX_PRESTIGE
                        ? gamer.getLanguage().getMessage("buildffa_prestige_maximum") : state,
                "%prestige_multiplier%", formatMultiplier(displayedPrestige),
                "%next_prestige_multiplier%", displayedPrestige == MAX_PRESTIGE
                        ? gamer.getLanguage().getMessage("buildffa_prestige_maximum") : formatMultiplier(displayedPrestige + 1),
                "%prestige_requirement%", AbstractDivision.RUBY.getName(gamer.getLanguage()),
                "%required_rating%", String.valueOf(AbstractDivision.RUBY.getRequiredRating()));
        List<String> coloredLore = new java.util.ArrayList<>();
        for (String line : lore) coloredLore.add(color(line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    public void openConfirmation(Player player) {
        Gamer gamer = Gamer.getGamer(player);

        if (getPrestige(gamer) >= MAX_PRESTIGE) {
            player.sendMessage(color(getStatus(gamer)));
            return;
        }

        if (!canPrestige(gamer)) {
            player.sendMessage(color(getStatus(gamer)));
            return;
        }

        Inventory inv = Bukkit.createInventory(new PrestigeConfirmHolder(), 27, color("&4Confirm Prestige"));
        ItemStack filler = named(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        int nextPrestige = getPrestige(gamer) + 1;
        String nextIcon = BuildFFAGamer.getPrestigeIcon(nextPrestige);

        inv.setItem(11, named(Material.WOOL, (short) 5, "&aYes, Prestige", Arrays.asList(
                "&fIf you prestige your current ratings &b" + gamer.getLifetimeStat("rating"),
                "&fwill be reset and you'll be given a prestige icon",
                nextIcon + "&fnext to your rating rank. Each time you",
                "&fprestige a new color of the prestige icon will change.",
                "",
                "&eClick to prestige!"
        )));

        inv.setItem(13, named(Material.NETHER_STAR, (short) 0, getStatus(gamer),
                gamer.getLanguage().getMessageList("buildffa_prestige_multiplier_lore",
                        "%prestige_multiplier%", formatMultiplier(getPrestige(gamer)),
                        "%next_prestige_multiplier%", getNextMultiplier(gamer),
                        "%prestige_status%", getStatus(gamer))));

        inv.setItem(15, named(Material.WOOL, (short) 14, "&cNo, Cancel", List.of(
                "&7Keep your current rating and close this menu."
        )));

        player.openInventory(inv);
    }

    private void confirmPrestige(Player player) {
        Gamer gamer = Gamer.getGamer(player);

        if (getPrestige(gamer) >= MAX_PRESTIGE) {
            player.sendMessage(color(getStatus(gamer)));
            return;
        }

        if (!canPrestige(gamer)) {
            player.sendMessage(color(getStatus(gamer)));
            return;
        }

        int currentPrestige = getPrestige(gamer);
        int rating = gamer.getLifetimeStat("rating");
        int division = gamer.getLifetimeStat("division");

        gamer.setProperty(PRESTIGE_PROPERTY, currentPrestige + 1);
        if (rating > 0) gamer.addStat("rating", -rating);
        if (division > 0) gamer.addStat("division", -division);

        player.closeInventory();
        game.resetPlayer(player);
        BuildFFAGamer buildFFAGamer = BuildFFAGamer.get(gamer);
        BuildFFABoard.refresh(gamer);
        broadcastPrestige(gamer, buildFFAGamer.getPrestigeIcon(), currentPrestige + 1);
        player.sendMessage(color("&aYou prestiged to " + getIcon(gamer) + "&aPrestige " + (currentPrestige + 1) + "&a!"));
        openLevels(player);
    }

    private void broadcastPrestige(Gamer gamer, String prestigeIcon, int prestige) {
        String formattedName = gamer.getDisplayName();
        for (Gamer recipient : Gamer.getGamers()) {
            if (!recipient.isOnline()) continue;
            String message = recipient.getLanguage().getMessage("buildffa_prestige_broadcast",
                    "%player%", formattedName,
                    "%prestige_icon%", prestigeIcon,
                    "%prestige%", String.valueOf(prestige));
            if (message == null || message.startsWith("No translation (buildffa_prestige_broadcast)")) {
                message = formattedName + " §aprestiged to " + prestigeIcon + "§aPrestige " + prestige + "§a!";
            }
            recipient.sendMessage(message);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof PrestigeLevelsHolder) {
            event.setCancelled(true);
            if (event.getRawSlot() == 40 && event.getWhoClicked() instanceof Player) {
                new DivisionGui((Player) event.getWhoClicked(), this).reopen();
            }
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                int page = ((PrestigeLevelsHolder) event.getInventory().getHolder()).page;
                if (event.getRawSlot() == 36 && page > 0) openLevels(player, page - 1);
                if (event.getRawSlot() == 44 && page < MAX_PRESTIGE / 21) openLevels(player, page + 1);
                if (event.getRawSlot() == 42) openConfirmation(player);
            }
            return;
        }
        if (!(event.getInventory().getHolder() instanceof PrestigeConfirmHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        if (event.getRawSlot() == 11) {
            confirmPrestige(player);
            return;
        }
        if (event.getRawSlot() == 15) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PrestigeConfirmHolder
                || event.getInventory().getHolder() instanceof PrestigeLevelsHolder) event.setCancelled(true);
    }

    private ItemStack named(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        List<String> coloredLore = new java.util.ArrayList<>();
        for (String line : lore) coloredLore.add(color(line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private static class PrestigeConfirmHolder implements InventoryHolder {
        public Inventory getInventory() { return null; }
    }

    private static class PrestigeLevelsHolder implements InventoryHolder {
        private final int page;
        private PrestigeLevelsHolder(int page) { this.page = page; }
        public Inventory getInventory() { return null; }
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
