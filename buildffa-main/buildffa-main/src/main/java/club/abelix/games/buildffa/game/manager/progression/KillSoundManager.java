package club.abelix.games.buildffa.game.manager.progression;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.purchase.Purchase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KillSoundManager extends YListener {
    private static final String SELECTED_SOUND_PROPERTY = "buildffa_selected_kill_sound";
    private static final String OWNED_SOUND_PROPERTY_PREFIX = "buildffa_kill_sound_owned_";
    private static final int[] COSMETIC_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final BuildFFAGame game;
    private final List<KillSound> sounds = new ArrayList<>();

    public KillSoundManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;

        sounds.add(new KillSound(1, "&aDefault", Material.NOTE_BLOCK, Sound.NOTE_PLING, 0, 0, true));
        sounds.add(new KillSound(2, "&bLevel Up", Material.EXP_BOTTLE, Sound.LEVEL_UP, 17500, 100, false));
        sounds.add(new KillSound(3, "&eOrb Pickup", Material.EMERALD, Sound.ORB_PICKUP, 110250, 125, false));
        sounds.add(new KillSound(4, "&6Firework", Material.FIREWORK, Sound.FIREWORK_BLAST, 120000, 150, false));
        sounds.add(new KillSound(5, "&dAnvil Drop", Material.ANVIL, Sound.ANVIL_LAND, 130000, 200, false));
        sounds.add(new KillSound(6, "&5Dragon Growl", Material.DRAGON_EGG, Sound.ENDERDRAGON_GROWL, 150000, 300, false));
        sounds.add(new KillSound(7, "&8Wither", Material.SKULL_ITEM, Sound.WITHER_SPAWN, 178500, 300, false));

        sounds.add(new KillSound(8, "&9Teleport", Material.ENDER_PEARL, Sound.ENDERMAN_TELEPORT, 120500, 175, false));
        sounds.add(new KillSound(9, "&cBlaze Hit", Material.BLAZE_ROD, Sound.BLAZE_HIT, 120750, 175, false));
        sounds.add(new KillSound(10, "&7Click", Material.STONE_BUTTON, Sound.CLICK, 130000, 200, false));
        sounds.add(new KillSound(11, "&fItem Pickup", Material.IRON_INGOT, Sound.ITEM_PICKUP, 130250, 200, false));
        sounds.add(new KillSound(12, "&aBass Note", Material.INK_SACK, Sound.NOTE_BASS, 135000, 225, false));
        sounds.add(new KillSound(13, "&2Snare Note", Material.INK_SACK, Sound.NOTE_SNARE_DRUM, 140000, 250, false));
        sounds.add(new KillSound(14, "&3Sticks Note", Material.STICK, Sound.NOTE_STICKS, 145000, 275, false));
        sounds.add(new KillSound(15, "&6Ghast Scream", Material.GHAST_TEAR, Sound.GHAST_SCREAM, 155000, 300, false));
        sounds.add(new KillSound(16, "&cZombie Metal", Material.IRON_CHESTPLATE, Sound.ZOMBIE_METAL, 165000, 300, false));
        sounds.add(new KillSound(17, "&dBat Takeoff", Material.FEATHER, Sound.BAT_TAKEOFF, 180000, 300, false));
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        int maxPage = Math.max(0, (sounds.size() - 1) / COSMETIC_SLOTS.length);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        Inventory inv = Bukkit.createInventory(new KillSoundHolder(page), 54, color("&8Kill Sounds"));
        ItemStack filler = named(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        int start = page * COSMETIC_SLOTS.length;
        for (int i = 0; i < COSMETIC_SLOTS.length; i++) {
            int index = start + i;
            if (index >= sounds.size()) break;
            inv.setItem(COSMETIC_SLOTS[i], icon(player, sounds.get(index)));
        }

        if (page > 0) {
            inv.setItem(48, named(Material.INK_SACK, (short) 8, "&7Previous Page", lore("&eClick to go back.", "&8killsound-page:" + (page - 1))));
        }
        inv.setItem(49, named(Material.BARRIER, (short) 0, "&cBack", backLore()));
        if (page < maxPage) {
            inv.setItem(50, named(Material.INK_SACK, (short) 10, "&aNext Page", lore("&eClick to go forward.", "&8killsound-page:" + (page + 1))));
        }

        player.openInventory(inv);
    }

    private ItemStack icon(Player player, KillSound sound) {
        boolean owned = isOwned(player, sound);
        boolean canUse = game.getCosmeticManager().canUse(player, owned);
        boolean selected = getSelectedSoundId(player) == sound.id;
        List<String> lore = new ArrayList<>();

        if (!canUse) {
            lore.add(color("&7Price: &e" + sound.coinPrice + " coins &7or &6" + sound.goldPrice + " gold"));
            lore.add(color("&eClick To Buy"));
        } else {
            lore.addAll(game.getCosmeticManager().availabilityLore(player, selected));
            lore.add(color("&7Plays every time you kill a player."));
            lore.add(color("&8Cosmetic only. No speed or strength."));
        }

        lore.add(color("&8killsound:" + sound.id));
        return named(sound.material, (short) 0, sound.name, lore);
    }

    private boolean isOwned(Player player, KillSound sound) {
        if (sound.free) return true;
        return Gamer.getGamer(player).getProperty(OWNED_SOUND_PROPERTY_PREFIX + sound.id) > 0;
    }

    private void setOwned(Player player, KillSound sound) {
        Gamer.getGamer(player).setProperty(OWNED_SOUND_PROPERTY_PREFIX + sound.id, 1);
    }

    public void resetForProgressionWipe(Gamer gamer) {
        gamer.setProperty(SELECTED_SOUND_PROPERTY, 1);
        for (KillSound sound : sounds) gamer.setProperty(OWNED_SOUND_PROPERTY_PREFIX + sound.id, 0);
    }

    private int getSelectedSoundId(Player player) {
        int selected = Gamer.getGamer(player).getProperty(SELECTED_SOUND_PROPERTY);
        return selected <= 0 ? 1 : selected;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof KillSoundHolder)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return;

        if (isBack(item)) {
            game.getCosmeticManager().open(player);
            return;
        }

        int page = readId(item, "killsound-page:");
        if (page >= 0) {
            open(player, page);
            return;
        }

        int id = readId(item, "killsound:");
        if (id <= 0) return;

        KillSound sound = getSound(id);
        if (sound == null) return;

        Gamer gamer = Gamer.getGamer(player);
        if (!game.getCosmeticManager().canUse(player, isOwned(player, sound))) {
            Purchase.builder()
                    .onSuccess(() -> {
                        setOwned(player, sound);
                        gamer.setProperty(SELECTED_SOUND_PROPERTY, sound.id);
                    })
                    .purchaseAllowed(() -> !isOwned(player, sound))
                    .coinPrice(sound.coinPrice)
                    .goldPrice(sound.goldPrice)
                    .player(player)
                    .build()
                    .execute();
            return;
        }

        gamer.setProperty(SELECTED_SOUND_PROPERTY, sound.id);
        player.closeInventory();
        player.sendMessage(color("&aKill sound selected."));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof KillSoundHolder) event.setCancelled(true);
    }

    public void playSelectedKillSound(Player player) {
        KillSound sound = getSound(getSelectedSoundId(player));
        if (sound == null || !game.getCosmeticManager().canUse(player, isOwned(player, sound))) sound = getSound(1);
        if (sound == null) return;

        player.playSound(player.getLocation(), sound.sound, 1.0F, 1.0F);
    }

    private KillSound getSound(int id) {
        for (KillSound sound : sounds) if (sound.id == id) return sound;
        return null;
    }

    private int readId(ItemStack item, String prefix) {
        for (String line : item.getItemMeta().getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (!stripped.startsWith(prefix)) continue;
            try { return Integer.parseInt(stripped.substring(prefix.length())); } catch (Exception ignored) { }
        }
        return -1;
    }

    private boolean isBack(ItemStack item) {
        for (String line : item.getItemMeta().getLore()) {
            if (ChatColor.stripColor(line).equals("cosmetic-back")) return true;
        }
        return false;
    }

    private List<String> backLore() {
        return lore("&7Return to BuildFFA Cosmetics.", "&8cosmetic-back");
    }

    private List<String> lore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String line : lines) lore.add(color(line));
        return lore;
    }

    private ItemStack named(Material material, short data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static class KillSoundHolder implements InventoryHolder {
        private final int page;

        KillSoundHolder(int page) {
            this.page = page;
        }

        public Inventory getInventory() { return null; }
    }

    private static class KillSound {
        final int id;
        final String name;
        final Material material;
        final Sound sound;
        final int coinPrice;
        final int goldPrice;
        final boolean free;

        KillSound(int id, String name, Material material, Sound sound, int coinPrice, int goldPrice, boolean free) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.sound = sound;
            this.coinPrice = coinPrice;
            this.goldPrice = goldPrice;
            this.free = free;
        }
    }
}
