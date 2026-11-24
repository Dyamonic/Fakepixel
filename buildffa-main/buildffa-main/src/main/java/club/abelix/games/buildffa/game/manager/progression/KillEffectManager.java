package club.abelix.games.buildffa.game.manager.progression;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.games.buildffa.game.BuildFFAGame;
import club.abelix.games.buildffa.game.purchase.Purchase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KillEffectManager extends YListener implements Listener {
    private static final String SELECTED_EFFECT_PROPERTY = "buildffa_selected_kill_effect";
    private static final String OWNED_EFFECT_PROPERTY_PREFIX = "buildffa_kill_effect_owned_";
    private static final int[] COSMETIC_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private final BuildFFAGame game;
    private final List<KillEffect> effects = new ArrayList<>();

    public KillEffectManager(BuildFFAGame game) {
        super(game.getPlugin());
        this.game = game;

        effects.add(new KillEffect(1, "&aNone", Material.BARRIER, null, 0, 0, true));
        effects.add(new KillEffect(2, "&cHearts", Material.RED_ROSE, "HEART", 170500, 100, false));
        effects.add(new KillEffect(3, "&6Flames", Material.BLAZE_POWDER, "FLAME", 110250, 125, false));
        effects.add(new KillEffect(4, "&bCrit", Material.DIAMOND_SWORD, "CRIT", 110750, 150, false));
        effects.add(new KillEffect(5, "&dMagic Crit", Material.ENCHANTMENT_TABLE, "MAGIC_CRIT", 120250, 175, false));
        effects.add(new KillEffect(6, "&fCloud", Material.FEATHER, "CLOUD", 112750, 200, false));
        effects.add(new KillEffect(7, "&aHappy Villager", Material.EMERALD, "HAPPY_VILLAGER", 130500, 225, false));
        effects.add(new KillEffect(8, "&eFirework Spark", Material.FIREWORK, "FIREWORKS_SPARK", 150000, 250, false));

        effects.add(new KillEffect(9, "&5Witch Magic", Material.POTION, "WITCH_MAGIC", 150500, 275, false));
        effects.add(new KillEffect(10, "&9Portal", Material.ENDER_PEARL, "PORTAL", 160000, 300, false));
        effects.add(new KillEffect(11, "&cRedstone", Material.REDSTONE, "REDSTONE", 160500, 300, false));
        effects.add(new KillEffect(12, "&6Lava Pop", Material.LAVA_BUCKET, "LAVA_POP", 170000, 300, false));
        effects.add(new KillEffect(13, "&bWater Drip", Material.WATER_BUCKET, "WATERDRIP", 170500, 300, false));
        effects.add(new KillEffect(14, "&8Large Smoke", Material.COAL, "LARGE_SMOKE", 180000, 300, false));
        effects.add(new KillEffect(15, "&eNote", Material.NOTE_BLOCK, "NOTE", 180500, 300, false));
        effects.add(new KillEffect(16, "&dSpell", Material.BOOK, "SPELL", 190000, 300, false));
        effects.add(new KillEffect(17, "&fInstant Spell", Material.PAPER, "INSTANT_SPELL", 190500, 300, false));
        effects.add(new KillEffect(18, "&4Explosion", Material.TNT, "EXPLOSION", 126000, 300, false));
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        int maxPage = Math.max(0, (effects.size() - 1) / COSMETIC_SLOTS.length);
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        Inventory inv = Bukkit.createInventory(new KillEffectHolder(page), 54, color("&8Kill Effects"));
        ItemStack filler = named(Material.STAINED_GLASS_PANE, (short) 15, " ", Collections.emptyList());
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        int start = page * COSMETIC_SLOTS.length;
        for (int i = 0; i < COSMETIC_SLOTS.length; i++) {
            int index = start + i;
            if (index >= effects.size()) break;
            inv.setItem(COSMETIC_SLOTS[i], icon(player, effects.get(index)));
        }

        if (page > 0) {
            inv.setItem(48, named(Material.INK_SACK, (short) 8, "&7Previous Page", lore("&eClick to go back.", "&8killeffect-page:" + (page - 1))));
        }
        inv.setItem(49, named(Material.BARRIER, (short) 0, "&cBack", lore("&7Return to BuildFFA Cosmetics.", "&8cosmetic-back")));
        if (page < maxPage) {
            inv.setItem(50, named(Material.INK_SACK, (short) 10, "&aNext Page", lore("&eClick to go forward.", "&8killeffect-page:" + (page + 1))));
        }

        player.openInventory(inv);
    }

    private ItemStack icon(Player player, KillEffect effect) {
        boolean owned = isOwned(player, effect);
        boolean canUse = game.getCosmeticManager().canUse(player, owned);
        boolean selected = getSelectedEffectId(player) == effect.id;
        List<String> lore = new ArrayList<>();

        if (!canUse) {
            lore.add(color("&7Price: &e" + effect.coinPrice + " coins &7or &6" + effect.goldPrice + " gold"));
            lore.add(color("&eClick To Buy"));
        } else {
            lore.addAll(game.getCosmeticManager().availabilityLore(player, selected));
            lore.add(color("&7Plays every time you kill a player."));
            lore.add(color("&8Cosmetic only."));
        }

        lore.add(color("&8killeffect:" + effect.id));
        return named(effect.material, (short) 0, effect.name, lore);
    }

    private boolean isOwned(Player player, KillEffect effect) {
        if (effect.free) return true;
        return Gamer.getGamer(player).getProperty(OWNED_EFFECT_PROPERTY_PREFIX + effect.id) > 0;
    }

    private void setOwned(Player player, KillEffect effect) {
        Gamer.getGamer(player).setProperty(OWNED_EFFECT_PROPERTY_PREFIX + effect.id, 1);
    }

    public void resetForProgressionWipe(Gamer gamer) {
        gamer.setProperty(SELECTED_EFFECT_PROPERTY, 1);
        for (KillEffect effect : effects) gamer.setProperty(OWNED_EFFECT_PROPERTY_PREFIX + effect.id, 0);
    }

    private int getSelectedEffectId(Player player) {
        int selected = Gamer.getGamer(player).getProperty(SELECTED_EFFECT_PROPERTY);
        return selected <= 0 ? 1 : selected;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof KillEffectHolder)) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return;

        if (isBack(item)) {
            game.getCosmeticManager().open(player);
            return;
        }

        int page = readId(item, "killeffect-page:");
        if (page >= 0) {
            open(player, page);
            return;
        }

        int id = readId(item, "killeffect:");
        if (id <= 0) return;

        KillEffect effect = getEffect(id);
        if (effect == null) return;

        Gamer gamer = Gamer.getGamer(player);
        if (!game.getCosmeticManager().canUse(player, isOwned(player, effect))) {
            Purchase.builder()
                    .onSuccess(() -> {
                        setOwned(player, effect);
                        gamer.setProperty(SELECTED_EFFECT_PROPERTY, effect.id);
                    })
                    .purchaseAllowed(() -> !isOwned(player, effect))
                    .coinPrice(effect.coinPrice)
                    .goldPrice(effect.goldPrice)
                    .player(player)
                    .build()
                    .execute();
            return;
        }

        gamer.setProperty(SELECTED_EFFECT_PROPERTY, effect.id);
        player.closeInventory();
        player.sendMessage(color("&aKill effect selected."));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof KillEffectHolder) event.setCancelled(true);
    }

    public void playSelectedKillEffect(Player killer, Location deathLocation) {
        KillEffect effect = getEffect(getSelectedEffectId(killer));
        if (effect == null || effect.effectName == null
                || !game.getCosmeticManager().canUse(killer, isOwned(killer, effect))) return;
        if (!game.getPlugin().isEnabled() || deathLocation == null || deathLocation.getWorld() == null) return;
        if (Bukkit.getWorld(deathLocation.getWorld().getUID()) != deathLocation.getWorld()) return;

        Effect bukkitEffect;
        try {
            bukkitEffect = Effect.valueOf(effect.effectName);
        } catch (Exception ignored) {
            return;
        }

        Location base = deathLocation.clone().add(0, 1.0, 0);
        for (int i = 0; i < 8; i++) {
            double angle = (Math.PI * 2 * i) / 8;
            Location loc = base.clone().add(Math.cos(angle) * 0.6, 0.15 * (i % 3), Math.sin(angle) * 0.6);
            base.getWorld().playEffect(loc, bukkitEffect, 0);
        }
    }

    private KillEffect getEffect(int id) {
        for (KillEffect effect : effects) if (effect.id == id) return effect;
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

    private static class KillEffectHolder implements InventoryHolder {

        KillEffectHolder(int page) {
        }

        public Inventory getInventory() { return null; }
    }

    private static class KillEffect {
        final int id;
        final String name;
        final Material material;
        final String effectName;
        final int coinPrice;
        final int goldPrice;
        final boolean free;

        KillEffect(int id, String name, Material material, String effectName, int coinPrice, int goldPrice, boolean free) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.effectName = effectName;
            this.coinPrice = coinPrice;
            this.goldPrice = goldPrice;
            this.free = free;
        }
    }
}
