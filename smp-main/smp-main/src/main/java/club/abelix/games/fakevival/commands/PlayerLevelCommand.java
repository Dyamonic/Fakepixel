package club.abelix.games.fakevival.commands;

import club.abelix.games.fakevival.managers.PlayerLevelManager;
import club.abelix.games.fakevival.menus.MenuHolder;
import club.abelix.games.fakevival.util.CC;
import club.abelix.games.fakevival.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PlayerLevelCommand implements CommandExecutor, Listener {
    private final PlayerLevelManager playerLevelManager;

    public PlayerLevelCommand(PlayerLevelManager playerLevelManager) {
        this.playerLevelManager = playerLevelManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return CommandUtil.playersOnly(sender);
        if (args.length > 0 && args[0].equalsIgnoreCase("upgrade")) {
            playerLevelManager.upgrade(player);
            return true;
        }
        openMenu(player);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder(String type)) || !type.equals("playerlevel")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).toLowerCase(Locale.ROOT)
                : "";
        if (name.equals("close")) {
            player.closeInventory();
            return;
        }
        if (name.equals("upgrade player level")) {
            playerLevelManager.upgrade(player);
            openMenu(player);
        }
    }

    private void openMenu(Player player) {
        int level = playerLevelManager.getLevel(player);
        int next = level + 1;
        Map<String, Integer> skills = playerLevelManager.skillRequirements(next);
        Map<String, Integer> items = playerLevelManager.itemRequirements(next);
        boolean maxed = level >= playerLevelManager.getMaxLevel();
        boolean xpProgression = !maxed;

        Inventory inventory = Bukkit.createInventory(new MenuHolder("playerlevel"), 54, CC.color("&8Player Levels"));
        inventory.setItem(13, item(Material.NETHER_STAR, "&6Current Level " + playerLevelManager.getLevelDisplay(level), List.of(
                "&7XP: &a" + playerLevelManager.getXp(player) + "&7/&a" + playerLevelManager.getXpNeeded(player),
                "&7Progress: &a" + progressBar(playerLevelManager.getProgress(player)),
                "", "&7PlayerLevel Bonus:", "&c+" + one(playerLevelManager.getStatBonus(player, "health")) + "❤ Health",
                "&a+" + one(playerLevelManager.getStatBonus(player, "defense")) + "❈ Defense",
                "&b+" + one(playerLevelManager.getStatBonus(player, "mana")) + "✎ Mana", "", "&7Max Level: &e10,000")));
        inventory.setItem(22, item(maxed ? Material.EMERALD_BLOCK : Material.LIME_CONCRETE,
                maxed ? "&aMaximum Level" : "&aUpgrade Player Level",
                maxed ? List.of("&7You reached the configured maximum level.") : List.of(
                        "&7Next: " + playerLevelManager.getLevelDisplay(next),
                        "&7XP: &a" + playerLevelManager.getXp(player) + "&7/&a" + playerLevelManager.getXpNeeded(player),
                        "&7Requirements: &e" + playerLevelManager.nextRequirementsSummary(player), "", "&eClick to claim when ready.")));

        inventory.setItem(30, item(Material.DIAMOND_SWORD, "&aRequired Skills", skillLore(player, next, skills, maxed)));
        boolean hasRequirements=!skills.isEmpty()||!items.isEmpty();boolean requirementsMet=!maxed&&playerLevelManager.requirementsMet(player,next,false);
        List<String> combined=new ArrayList<>();
        if(maxed)combined.add("&aMaximum level reached.");
        else if(!hasRequirements){combined.add("&aAll requirements met!");combined.add("&7This level only requires XP.");}
        else if(requirementsMet){combined.add("&aAll requirements met!");combined.add("&7You can level up when you have enough XP.");}
        else{combined.add("&cMissing requirements.");combined.add("&7Check the Required Skills and Required Items buttons.");}
        if (!maxed) combined.add(0, "&eFor Level " + next + ":");
        inventory.setItem(31, item(Material.WRITABLE_BOOK, "&eRequirements", combined));
        inventory.setItem(32, item(Material.CHEST, "&6Required Items", itemLore(player, next, items, maxed)));
        inventory.setItem(40, item(Material.BEACON, "&bNext Level Stats", statLore(player, next, maxed)));

        inventory.setItem(49, item(Material.BARRIER, "&cClose", List.of("&7Close this menu.")));
        fill(inventory);
        player.openInventory(inventory);
    }

    private List<String> skillLore(Player player, int nextLevel, Map<String, Integer> skills, boolean maxed) {
        List<String> lore = new ArrayList<>();
        lore.add("&eFor Level " + nextLevel + ":");
        if (maxed || skills.isEmpty()) {
            lore.add("&7No skill requirements for this level.");
            return lore;
        }
        for (Map.Entry<String, Integer> entry : skills.entrySet()) {
            int current = playerLevelManager.getSkillLevel(player, entry.getKey());
            lore.add(current >= entry.getValue() ? "&a✔ " + pretty(entry.getKey()) + " Level " + entry.getValue()
                    : "&c✖ " + pretty(entry.getKey()) + " Level " + entry.getValue() + " &7(Current: " + current + ")");
        }
        lore.clear(); lore.add("&eFor Level " + nextLevel + ":");
        for (Map.Entry<String,Integer> entry:skills.entrySet()){int current=playerLevelManager.getSkillLevel(player,entry.getKey());lore.add(current>=entry.getValue()?"&a\u2714 "+pretty(entry.getKey())+" Level "+entry.getValue():"&c\u2716 "+pretty(entry.getKey())+" Level "+entry.getValue()+" &7(Current: "+current+")");}
        return lore;
    }

    private List<String> itemLore(Player player, int nextLevel, Map<String, Integer> items, boolean maxed) {
        List<String> lore = new ArrayList<>();
        lore.add("&eFor Level " + nextLevel + ":");
        if (maxed || items.isEmpty()) {
            lore.add("&7No item requirements for this level.");
            return lore;
        }
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            if(!playerLevelManager.isRequirementItemRegistered(entry.getKey())){lore.add("&cMissing registry item: "+entry.getKey());continue;}
            int current = playerLevelManager.getCustomItemCount(player, entry.getKey());
            lore.add(current >= entry.getValue() ? "&a✔ " + entry.getValue() + "x " + pretty(entry.getKey())
                    : "&c✖ " + entry.getValue() + "x " + pretty(entry.getKey()) + " &7(You have " + current + ")");
        }
        lore.clear(); lore.add("&eFor Level " + nextLevel + ":");
        for(Map.Entry<String,Integer> entry:items.entrySet()){if(!playerLevelManager.isRequirementItemRegistered(entry.getKey())){lore.add("&cMissing registry item: "+entry.getKey());continue;}int current=playerLevelManager.getCustomItemCount(player,entry.getKey());String name=playerLevelManager.requirementItemDisplayName(entry.getKey());lore.add(current>=entry.getValue()?"&a\u2714 "+entry.getValue()+"x "+name:"&c\u2716 "+entry.getValue()+"x "+name+" &7(You have "+current+")");}
        return lore;
    }

    private List<String> statLore(Player player, int next, boolean maxed) {
        if (maxed) return List.of("&7No more configured stat rewards.");
        double hp = playerLevelManager.getStatBonusForLevel(next, "health");
        double mana = playerLevelManager.getStatBonusForLevel(next, "mana");
        double damage = playerLevelManager.getStatBonusForLevel(next, "damage");
        double defense = playerLevelManager.getStatBonusForLevel(next, "defense");
        double strength = playerLevelManager.getStatBonusForLevel(next, "strength");
        double critChance = playerLevelManager.getStatBonusForLevel(next, "crit_chance");
        double critDamage = playerLevelManager.getStatBonusForLevel(next, "crit_damage");
        List<String> lore = new ArrayList<>(); lore.add("&eRewards for Level " + next + ":");
        addReward(lore,hp,"&c","Health",false);addReward(lore,defense,"&a","Defense",false);addReward(lore,mana,"&b","Mana",false);
        addReward(lore,damage,"&c","Damage",false);addReward(lore,strength,"&c","Strength",false);
        addReward(lore,critChance,"&9","Crit Chance",true);addReward(lore,critDamage,"&9","Crit Damage",true);if(lore.size()==1)lore.set(0,"&7No stat rewards for this level.");return lore;
    }

    private void addReward(List<String> lore,double value,String color,String name,boolean percent){if(value>0.0D)lore.add(color+"+"+number(value)+(percent?"% ":" ")+name);}

    private String number(double value){return Math.abs(value-Math.rint(value))<0.0001D?String.valueOf((long)Math.rint(value)):one(value);}

    private String progressBar(double percent) {
        int filled = Math.clamp((int) Math.floor(percent / 10.0D), 0, 10);
        return "&a" + "■".repeat(filled) + "&7" + "■".repeat(10 - filled) + " &f" + String.format(Locale.US, "%.1f%%", percent);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.color(name));
            meta.setLore(CC.colorList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
    }

    private String whole(double value) {
        return String.valueOf((int) Math.round(value));
    }

    private String one(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String pretty(String text) {
        text = text == null ? "item" : text.toLowerCase(Locale.ROOT).replace('_', ' ');
        if(text.equals("woodcutting"))return "Foraging";
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }
}
