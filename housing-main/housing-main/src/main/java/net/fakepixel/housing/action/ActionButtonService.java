package net.fakepixel.housing.action;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ActionButtonService implements Listener {
    private static final String[] TYPES = new String[]{"MESSAGE", "TELEPORT", "PLAY_SOUND", "GIVE_ITEM", "POTION_EFFECT", "START_PARKOUR", "RESET_PARKOUR", "OPEN_MENU", "CALL_FUNCTION"};

    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Prompt> prompts = new HashMap<>();
    private final String editorTitle = "Action Button Editor";
    private final String addTitle = "Add Action";

    public ActionButtonService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public ItemStack item() {
        ItemStack stack = new ItemStack(net.fakepixel.housing.util.Compat.mat("STONE_BUTTON"));
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color("&aAction Button"));
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Place this inside your house"));
        lore.add(plugin.color("&7to create an Action Button."));
        lore.add(plugin.color(""));
        lore.add(plugin.color("&7Special Housing item."));
        lore.add(plugin.color("&eClick to receive 1!"));
        lore.add(plugin.color("&8Housing Special: action-button"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isActionButtonItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            if ("Housing Special: action-button".equals(ChatColor.stripColor(line))) return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isActionButtonItem(event.getItemInHand())) return;
        Player player = event.getPlayer();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !plugin.canEditHousingObjects(player, house)) {
            event.setCancelled(true);
            player.sendMessage(plugin.color("&cYou need build permission in this house to place Action Buttons."));
            return;
        }
        House.ActionButton button = house.addActionButton(event.getBlock().getLocation());
        house.markPlayerEditedBlock(event.getBlock().getLocation());
        houses.saveAll();
        player.sendMessage(plugin.color("&aCreated &e" + button.getName() + "&a."));
        Bukkit.getScheduler().runTask(plugin, () -> openEditor(player, house, button));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        House house = houses.getHouseByWorld(event.getBlock().getWorld().getName());
        if (house == null) return;
        House.ActionButton button = house.getActionButtonAt(event.getBlock().getLocation());
        if (button == null) return;
        if (!plugin.canEditHousingObjects(event.getPlayer(), house)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.color("&cYou need build permission to remove Action Buttons."));
            return;
        }
        house.removeActionButton(button.getId());
        house.markPlayerEditedBlock(event.getBlock().getLocation());
        houses.saveAll();
        event.getPlayer().sendMessage(plugin.color("&cRemoved Action Button."));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        if (isActionButtonItem(event.getItem())) return;
        House house = houses.getHouseByWorld(event.getClickedBlock().getWorld().getName());
        if (house == null) return;
        House.ActionButton button = house.getActionButtonAt(event.getClickedBlock().getLocation());
        if (button == null) return;
        event.setCancelled(true);
        runButton(event.getPlayer(), house, button);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!editorTitle.equals(title) && !addTitle.equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !plugin.canEditHousingObjects(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (addTitle.equals(title)) {
            House.ActionButton button = buttonFromToken(house, event.getView().getTopInventory().getItem(26));
            if (button == null) { player.closeInventory(); return; }
            String type = typeFromSlot(event.getRawSlot());
            if (type != null) {
                button.getActions().add(defaultAction(type, player));
                houses.saveAll();
                openEditor(player, house, button);
            } else if (event.getRawSlot() == 22) openEditor(player, house, button);
            return;
        }

        House.ActionButton button = buttonFromToken(house, event.getView().getTopInventory().getItem(35));
        if (button == null) { player.closeInventory(); return; }
        int slot = event.getRawSlot();
        if (slot == 10) openAddAction(player, button);
        else if (slot == 11) { if (!button.getActions().isEmpty()) button.getActions().remove(button.getActions().size() - 1); houses.saveAll(); openEditor(player, house, button); }
        else if (slot == 12) { if (button.getActions().size() > 1) button.getActions().add(0, button.getActions().remove(button.getActions().size() - 1)); houses.saveAll(); openEditor(player, house, button); }
        else if (slot == 13) beginPrompt(player, house, button, "cooldown");
        else if (slot == 14) { button.setOneTime(!button.isOneTime()); houses.saveAll(); openEditor(player, house, button); }
        else if (slot == 15) { button.setParticles(!button.isParticles()); houses.saveAll(); openEditor(player, house, button); }
        else if (slot == 16) { button.setSound(!button.isSound()); houses.saveAll(); openEditor(player, house, button); }
        else if (slot == 21) beginPrompt(player, house, button, "name");
        else if (slot == 23) { house.removeActionButton(button.getId()); houses.saveAll(); player.closeInventory(); player.sendMessage(plugin.color("&cDeleted Action Button.")); }
        else if (slot == 31) player.closeInventory();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Prompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = event.getPlayer();
            House house = houses.getHouseByWorld(prompt.worldName);
            House.ActionButton button = house == null ? null : house.getActionButton(prompt.buttonId);
            if (house == null || button == null || !plugin.canEditHousingObjects(player, house)) {
                player.sendMessage(plugin.color("&cYou cannot edit that Action Button."));
                return;
            }
            if ("cancel".equalsIgnoreCase(message.trim())) {
                player.sendMessage(plugin.color("&cAction Button edit cancelled."));
                openEditor(player, house, button);
                return;
            }
            if ("name".equals(prompt.field)) button.setName(message.trim());
            else if ("cooldown".equals(prompt.field)) button.setCooldownMs(parseCooldown(message));
            houses.saveAll();
            player.sendMessage(plugin.color("&aSaved Action Button " + prompt.field + "."));
            openEditor(player, house, button);
        });
    }

    private void runButton(Player player, House house, House.ActionButton button) {
        if (button.isOneTime() && button.getUsedBy().contains(player.getUniqueId())) {
            player.sendMessage(plugin.color("&cYou have already used this Action Button."));
            return;
        }
        String cooldownKey = house.getWorldName() + ":" + button.getId() + ":" + player.getUniqueId();
        long next = cooldowns.getOrDefault(cooldownKey, 0L);
        if (next > System.currentTimeMillis()) {
            player.sendMessage(plugin.color("&cAction Button cooldown: &e" + ((next - System.currentTimeMillis()) / 1000L + 1L) + "s&c."));
            return;
        }
        if (button.isParticles()) player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 0);
        if (button.isSound()) playSound(player, "CLICK");
        for (House.ButtonAction action : button.getActions()) runAction(player, house, action);
        if (button.isOneTime()) {
            button.getUsedBy().add(player.getUniqueId());
            houses.saveAll();
        }
        if (button.getCooldownMs() > 0L) cooldowns.put(cooldownKey, System.currentTimeMillis() + button.getCooldownMs());
    }

    private void runAction(Player player, House house, House.ButtonAction action) {
        String type = action.getType();
        String value = action.getValue();
        if ("MESSAGE".equals(type)) player.sendMessage(plugin.color(value.isEmpty() ? "&aAction Button!" : value));
        else if ("TELEPORT".equals(type)) {
            Location target = parseLocation(player.getWorld(), value);
            if (target != null) player.teleport(target);
        } else if ("PLAY_SOUND".equals(type)) playSound(player, value.isEmpty() ? "NOTE_PLING" : value);
        else if ("GIVE_ITEM".equals(type)) giveItem(player, value);
        else if ("POTION_EFFECT".equals(type)) potion(player, value);
        else if ("START_PARKOUR".equals(type) && plugin.getParkourService() != null) plugin.getParkourService().start(player, house);
        else if ("RESET_PARKOUR".equals(type) && plugin.getParkourService() != null) plugin.getParkourService().reset(player, house);
        else if ("OPEN_MENU".equals(type) && plugin.getHousingMenu() != null) {
            if (house.canManage(player.getUniqueId())) plugin.getHousingMenu().openOwnerMenu(player);
            else plugin.getHousingMenu().openGuestMenu(player);
        } else if ("CALL_FUNCTION".equals(type) && plugin.getFunctionService() != null) plugin.getFunctionService().runFunction(player, house, value);
    }

    private void openEditor(Player player, House house, House.ActionButton button) {
        Inventory inv = Bukkit.createInventory(null, 36, editorTitle);
        inv.setItem(4, named(net.fakepixel.housing.util.Compat.mat("STONE_BUTTON"), "&a" + button.getName(),
                "&7Actions: &f" + button.getActions().size(),
                "&7Cooldown: &f" + (button.getCooldownMs() / 1000L) + "s",
                "&7One-Time: " + (button.isOneTime() ? "&aEnabled" : "&cDisabled"),
                "&7Particles: " + (button.isParticles() ? "&aEnabled" : "&cDisabled"),
                "&7Sound: " + (button.isSound() ? "&aEnabled" : "&cDisabled")));
        inv.setItem(10, named(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aAdd Action", "&7Add a safe action.", "", "&eClick to add!"));
        inv.setItem(11, named(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cRemove Action", "&7Removes the last action.", "", "&eClick to remove!"));
        inv.setItem(12, named(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aReorder Actions", "&7Moves the last action to the top.", "", "&eClick to reorder!"));
        inv.setItem(13, named(net.fakepixel.housing.util.Compat.mat("WATCH"), "&aSet Cooldown", "&7Current: &f" + (button.getCooldownMs() / 1000L) + "s", "", "&eClick to set!"));
        inv.setItem(14, named(button.isOneTime() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aOne-Time Use: " + (button.isOneTime() ? "&aEnabled" : "&cDisabled"), "&7Toggle per-player one-time use."));
        inv.setItem(15, named(button.isParticles() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aParticles: " + (button.isParticles() ? "&aEnabled" : "&cDisabled"), "&7Toggle click particles."));
        inv.setItem(16, named(button.isSound() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aSound: " + (button.isSound() ? "&aEnabled" : "&cDisabled"), "&7Toggle click sound."));
        inv.setItem(21, named(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aRename Action Button", "&7Change this button's name.", "", "&eClick to rename!"));
        inv.setItem(23, named(net.fakepixel.housing.util.Compat.mat("TNT"), "&cDelete Action Button", "&7Delete this Action Button.", "", "&eClick to delete!"));
        inv.setItem(31, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aClose"));
        inv.setItem(35, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Action Button Token", "&8Action Button ID: " + button.getId()));
        player.openInventory(inv);
    }

    private void openAddAction(Player player, House.ActionButton button) {
        Inventory inv = Bukkit.createInventory(null, 27, addTitle);
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18};
        for (int i = 0; i < TYPES.length; i++) inv.setItem(slots[i], named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&a" + TYPES[i], "&7Add this safe action."));
        inv.setItem(22, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(26, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Action Button Token", "&8Action Button ID: " + button.getId()));
        player.openInventory(inv);
    }

    private House.ButtonAction defaultAction(String type, Player player) {
        if ("MESSAGE".equals(type)) return new House.ButtonAction(type, "&aAction Button!");
        if ("TELEPORT".equals(type)) return new House.ButtonAction(type, serialize(player.getLocation()));
        if ("PLAY_SOUND".equals(type)) return new House.ButtonAction(type, "NOTE_PLING");
        if ("GIVE_ITEM".equals(type)) return new House.ButtonAction(type, "STONE:1");
        if ("POTION_EFFECT".equals(type)) return new House.ButtonAction(type, "SPEED:100:1");
        return new House.ButtonAction(type, "");
    }

    private void beginPrompt(Player player, House house, House.ActionButton button, String field) {
        prompts.put(player.getUniqueId(), new Prompt(house.getWorldName(), button.getId(), field));
        player.closeInventory();
        player.sendMessage(plugin.color("&aType the new Action Button " + field + " in chat. Type &ccancel &ato cancel."));
        if ("cooldown".equals(field)) player.sendMessage(plugin.color("&7Use seconds, like &e5&7 or &e0&7."));
    }

    private House.ActionButton buttonFromToken(House house, ItemStack item) {
        if (house == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Action Button ID: ")) return house.getActionButton(clean.substring("Action Button ID: ".length()));
        }
        return null;
    }

    private String typeFromSlot(int slot) {
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18};
        for (int i = 0; i < slots.length; i++) if (slots[i] == slot) return TYPES[i];
        return null;
    }

    private long parseCooldown(String raw) {
        try { return Math.max(0L, Long.parseLong(raw.trim()) * 1000L); }
        catch (Exception ignored) { return 0L; }
    }

    private void giveItem(Player player, String raw) {
        String[] split = raw == null ? new String[0] : raw.split(":", 2);
        Material material = split.length == 0 ? Material.STONE : Material.matchMaterial(split[0]);
        if (material == null) material = net.fakepixel.housing.util.Compat.mat(split.length == 0 ? "STONE" : split[0]);
        int amount = 1;
        if (split.length > 1) {
            try { amount = Math.max(1, Math.min(64, Integer.parseInt(split[1]))); } catch (Exception ignored) {}
        }
        player.getInventory().addItem(new ItemStack(material == null ? Material.STONE : material, amount)).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
    }

    private void potion(Player player, String raw) {
        String[] split = raw == null ? new String[0] : raw.split(":");
        PotionEffectType type = PotionEffectType.getByName(split.length > 0 ? split[0] : "SPEED");
        int duration = 100;
        int amplifier = 0;
        if (split.length > 1) try { duration = Integer.parseInt(split[1]); } catch (Exception ignored) {}
        if (split.length > 2) try { amplifier = Integer.parseInt(split[2]); } catch (Exception ignored) {}
        if (type != null) player.addPotionEffect(new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier)));
    }

    private void playSound(Player player, String raw) {
        try { player.playSound(player.getLocation(), Sound.valueOf(raw == null || raw.isEmpty() ? "NOTE_PLING" : raw.toUpperCase()), 1.0F, 1.0F); }
        catch (Throwable ignored) {}
    }

    private String serialize(Location location) {
        return location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private Location parseLocation(World world, String raw) {
        if (world == null || raw == null || raw.trim().isEmpty()) return null;
        String[] split = raw.split(",");
        if (split.length < 3) return null;
        try {
            Location location = new Location(world, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
            if (split.length >= 5) {
                location.setYaw(Float.parseFloat(split[3]));
                location.setPitch(Float.parseFloat(split[4]));
            }
            return location;
        } catch (Exception ex) {
            return null;
        }
    }

    private ItemStack named(Material material, String name, String... lore) {
        return named(new ItemStack(material), name, lore);
    }

    private ItemStack named(ItemStack stack, String name, String... lore) {
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color(name));
        List<String> lines = new ArrayList<>();
        for (String line : lore) lines.add(plugin.color(line));
        meta.setLore(lines);
        stack.setItemMeta(meta);
        return stack;
    }

    private static class Prompt {
        private final String worldName;
        private final String buttonId;
        private final String field;

        private Prompt(String worldName, String buttonId, String field) {
            this.worldName = worldName;
            this.buttonId = buttonId;
            this.field = field;
        }
    }
}
