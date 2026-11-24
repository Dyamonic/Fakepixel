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
import org.bukkit.WeatherType;
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ActionPadService implements Listener {
    private static final String[] TYPES = new String[]{
            "MESSAGE", "TITLE", "ACTION_BAR", "TELEPORT", "PLAY_SOUND", "GIVE_ITEM", "POTION_EFFECT",
            "START_PARKOUR", "RESET_PARKOUR", "CHECKPOINT_PARKOUR", "OPEN_MENU", "CHANGE_TIME", "CHANGE_WEATHER", "CALL_FUNCTION"
    };

    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Prompt> prompts = new HashMap<>();
    private final String editorTitle = "Action Pad Editor";
    private final String addTitle = "Add Pad Action";

    public ActionPadService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public ItemStack item() {
        ItemStack stack = new ItemStack(padMaterial());
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color("&aAction Pad"));
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Place this inside your house"));
        lore.add(plugin.color("&7to run saved actions when"));
        lore.add(plugin.color("&7players step on it."));
        lore.add(plugin.color(""));
        lore.add(plugin.color("&7Special Housing item."));
        lore.add(plugin.color("&eClick to receive 1!"));
        lore.add(plugin.color("&8Housing Special: action-pad"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isActionPadItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            if ("Housing Special: action-pad".equals(ChatColor.stripColor(line))) return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isActionPadItem(event.getItemInHand())) return;
        Player player = event.getPlayer();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditActionPads(player, house)) {
            event.setCancelled(true);
            player.sendMessage(plugin.color("&cYou need build permission in this house to place Action Pads."));
            return;
        }
        House.ActionPad pad = house.addActionPad(event.getBlock().getLocation());
        event.getBlock().setType(padMaterial());
        house.markPlayerEditedBlock(event.getBlock().getLocation());
        houses.saveAll();
        player.sendMessage(plugin.color("&aCreated &e" + pad.getName() + "&a."));
        Bukkit.getScheduler().runTask(plugin, () -> openEditor(player, house, pad));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        House house = houses.getHouseByWorld(event.getBlock().getWorld().getName());
        if (house == null) return;
        House.ActionPad pad = house.getActionPadAt(event.getBlock().getLocation());
        if (pad == null) return;
        if (!canEditActionPads(event.getPlayer(), house)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.color("&cYou need build permission to remove Action Pads."));
            return;
        }
        house.removeActionPad(pad.getId());
        house.markPlayerEditedBlock(event.getBlock().getLocation());
        houses.saveAll();
        event.getPlayer().sendMessage(plugin.color("&cRemoved Action Pad."));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPhysicalInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL || event.getClickedBlock() == null) return;
        if (!isPressurePlate(event.getClickedBlock().getType())) return;
        House house = houses.getHouseByWorld(event.getClickedBlock().getWorld().getName());
        if (house == null) return;
        House.ActionPad pad = house.getActionPadAt(event.getClickedBlock().getLocation());
        if (pad == null) return;
        runPad(event.getPlayer(), house, pad);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!editorTitle.equals(title) && !addTitle.equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditActionPads(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (addTitle.equals(title)) {
            House.ActionPad pad = padFromToken(house, event.getView().getTopInventory().getItem(26));
            if (pad == null) { player.closeInventory(); return; }
            String type = typeFromSlot(event.getRawSlot());
            if (type != null) {
                pad.getActions().add(defaultAction(type, player));
                houses.saveAll();
                openEditor(player, house, pad);
            } else if (event.getRawSlot() == 24) openEditor(player, house, pad);
            return;
        }

        House.ActionPad pad = padFromToken(house, event.getView().getTopInventory().getItem(35));
        if (pad == null) { player.closeInventory(); return; }
        int slot = event.getRawSlot();
        if (slot == 10) openAddAction(player, pad);
        else if (slot == 11) { if (!pad.getActions().isEmpty()) pad.getActions().remove(pad.getActions().size() - 1); houses.saveAll(); openEditor(player, house, pad); }
        else if (slot == 12) beginPrompt(player, house, pad, "action");
        else if (slot == 13) { if (pad.getActions().size() > 1) pad.getActions().add(0, pad.getActions().remove(pad.getActions().size() - 1)); houses.saveAll(); openEditor(player, house, pad); }
        else if (slot == 14) beginPrompt(player, house, pad, "cooldown");
        else if (slot == 15) { pad.setOneTime(!pad.isOneTime()); houses.saveAll(); openEditor(player, house, pad); }
        else if (slot == 16) { pad.setSound(!pad.isSound()); houses.saveAll(); openEditor(player, house, pad); }
        else if (slot == 19) { pad.setParticles(!pad.isParticles()); houses.saveAll(); openEditor(player, house, pad); }
        else if (slot == 21) beginPrompt(player, house, pad, "name");
        else if (slot == 23) { house.removeActionPad(pad.getId()); houses.saveAll(); player.closeInventory(); player.sendMessage(plugin.color("&cDeleted Action Pad.")); }
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
            House.ActionPad pad = house == null ? null : house.getActionPad(prompt.padId);
            if (house == null || pad == null || !canEditActionPads(player, house)) {
                player.sendMessage(plugin.color("&cYou cannot edit that Action Pad."));
                return;
            }
            if ("cancel".equalsIgnoreCase(message.trim())) {
                player.sendMessage(plugin.color("&cAction Pad edit cancelled."));
                openEditor(player, house, pad);
                return;
            }
            if ("name".equals(prompt.field)) pad.setName(message.trim());
            else if ("cooldown".equals(prompt.field)) pad.setCooldownMs(parseCooldown(message));
            else if ("action".equals(prompt.field) && !pad.getActions().isEmpty()) pad.getActions().get(pad.getActions().size() - 1).setValue(message.trim());
            houses.saveAll();
            player.sendMessage(plugin.color("&aSaved Action Pad " + prompt.field + "."));
            openEditor(player, house, pad);
        });
    }

    private void runPad(Player player, House house, House.ActionPad pad) {
        if (pad.isOneTime() && pad.getUsedBy().contains(player.getUniqueId())) return;
        String cooldownKey = house.getWorldName() + ":" + pad.getId() + ":" + player.getUniqueId();
        long now = System.currentTimeMillis();
        long next = cooldowns.getOrDefault(cooldownKey, 0L);
        if (next > now) return;
        if (pad.isParticles()) player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 0);
        if (pad.isSound()) playSound(player, "CLICK");
        for (House.ButtonAction action : pad.getActions()) runAction(player, house, action);
        if (pad.isOneTime()) {
            pad.getUsedBy().add(player.getUniqueId());
            houses.saveAll();
        }
        if (pad.getCooldownMs() > 0L) cooldowns.put(cooldownKey, now + pad.getCooldownMs());
    }

    private void runAction(Player player, House house, House.ButtonAction action) {
        String type = action.getType();
        String value = action.getValue();
        if ("MESSAGE".equals(type)) player.sendMessage(plugin.color(value.isEmpty() ? "&aAction Pad!" : value));
        else if ("TITLE".equals(type)) sendTitle(player, value.isEmpty() ? "&aAction Pad!" : value, "");
        else if ("ACTION_BAR".equals(type)) sendActionBar(player, value.isEmpty() ? "&aAction Pad!" : value);
        else if ("TELEPORT".equals(type)) {
            Location target = parseLocation(house, player.getWorld(), value);
            if (target != null && target.getWorld() != null && house.getWorldName().equals(target.getWorld().getName())) player.teleport(target);
        } else if ("PLAY_SOUND".equals(type)) playSound(player, value.isEmpty() ? "NOTE_PLING" : value);
        else if ("GIVE_ITEM".equals(type)) giveItem(player, value);
        else if ("POTION_EFFECT".equals(type)) potion(player, value);
        else if ("START_PARKOUR".equals(type) && plugin.getParkourService() != null) plugin.getParkourService().start(player, house);
        else if ("RESET_PARKOUR".equals(type) && plugin.getParkourService() != null) plugin.getParkourService().reset(player, house);
        else if ("CHECKPOINT_PARKOUR".equals(type) && plugin.getParkourService() != null) plugin.getParkourService().checkpointAction(player, house, player.getLocation());
        else if ("OPEN_MENU".equals(type) && plugin.getHousingMenu() != null) {
            if (house.canManage(player.getUniqueId())) plugin.getHousingMenu().openOwnerMenu(player);
            else plugin.getHousingMenu().openGuestMenu(player);
        } else if ("CHANGE_TIME".equals(type)) changeTime(player, value);
        else if ("CHANGE_WEATHER".equals(type)) changeWeather(player, value);
        else if ("CALL_FUNCTION".equals(type) && plugin.getFunctionService() != null) plugin.getFunctionService().runFunction(player, house, value);
    }

    private void openEditor(Player player, House house, House.ActionPad pad) {
        Inventory inv = Bukkit.createInventory(null, 36, editorTitle);
        inv.setItem(4, named(padMaterial(), "&a" + pad.getName(),
                "&7Actions: &f" + pad.getActions().size(),
                "&7Cooldown: &f" + (pad.getCooldownMs() / 1000L) + "s",
                "&7One-Time: " + (pad.isOneTime() ? "&aEnabled" : "&cDisabled"),
                "&7Particles: " + (pad.isParticles() ? "&aEnabled" : "&cDisabled"),
                "&7Sound: " + (pad.isSound() ? "&aEnabled" : "&cDisabled")));
        inv.setItem(10, named(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aAdd Action", "&7Add a safe action.", "", "&eClick to add!"));
        inv.setItem(11, named(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cRemove Action", "&7Removes the last action.", "", "&eClick to remove!"));
        inv.setItem(12, named(net.fakepixel.housing.util.Compat.mat("BOOK_AND_QUILL"), "&aEdit Action", "&7Edits the last action value.", "", "&eClick to edit!"));
        inv.setItem(13, named(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aReorder Actions", "&7Moves the last action to the top.", "", "&eClick to reorder!"));
        inv.setItem(14, named(net.fakepixel.housing.util.Compat.mat("WATCH"), "&aSet Cooldown", "&7Current: &f" + (pad.getCooldownMs() / 1000L) + "s", "", "&eClick to set!"));
        inv.setItem(15, named(pad.isOneTime() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aOne-Time Use: " + (pad.isOneTime() ? "&aEnabled" : "&cDisabled"), "&7Toggle per-player one-time use."));
        inv.setItem(16, named(pad.isSound() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aSound: " + (pad.isSound() ? "&aEnabled" : "&cDisabled"), "&7Toggle activation sound."));
        inv.setItem(19, named(pad.isParticles() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aParticles: " + (pad.isParticles() ? "&aEnabled" : "&cDisabled"), "&7Toggle activation particles."));
        inv.setItem(21, named(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aRename Action Pad", "&7Change this pad's name.", "", "&eClick to rename!"));
        inv.setItem(23, named(net.fakepixel.housing.util.Compat.mat("TNT"), "&cDelete Action Pad", "&7Delete this Action Pad.", "", "&eClick to delete!"));
        inv.setItem(31, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aClose"));
        inv.setItem(35, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Action Pad Token", "&8Action Pad ID: " + pad.getId()));
        player.openInventory(inv);
    }

    private void openAddAction(Player player, House.ActionPad pad) {
        Inventory inv = Bukkit.createInventory(null, 27, addTitle);
        int[] slots = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        for (int i = 0; i < TYPES.length; i++) inv.setItem(slots[i], named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&a" + TYPES[i], "&7Add this safe action."));
        inv.setItem(24, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(26, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Action Pad Token", "&8Action Pad ID: " + pad.getId()));
        player.openInventory(inv);
    }

    private House.ButtonAction defaultAction(String type, Player player) {
        if ("MESSAGE".equals(type)) return new House.ButtonAction(type, "&aAction Pad!");
        if ("TITLE".equals(type)) return new House.ButtonAction(type, "&aAction Pad!");
        if ("ACTION_BAR".equals(type)) return new House.ButtonAction(type, "&aAction Pad!");
        if ("TELEPORT".equals(type)) return new House.ButtonAction(type, serialize(player.getLocation()));
        if ("PLAY_SOUND".equals(type)) return new House.ButtonAction(type, "NOTE_PLING");
        if ("GIVE_ITEM".equals(type)) return new House.ButtonAction(type, "STONE:1");
        if ("POTION_EFFECT".equals(type)) return new House.ButtonAction(type, "SPEED:100:1");
        if ("CHANGE_TIME".equals(type)) return new House.ButtonAction(type, "6000");
        if ("CHANGE_WEATHER".equals(type)) return new House.ButtonAction(type, "sunny");
        return new House.ButtonAction(type, "");
    }

    private void beginPrompt(Player player, House house, House.ActionPad pad, String field) {
        prompts.put(player.getUniqueId(), new Prompt(house.getWorldName(), pad.getId(), field));
        player.closeInventory();
        player.sendMessage(plugin.color("&aType the new Action Pad " + field + " in chat. Type &ccancel &ato cancel."));
        if ("cooldown".equals(field)) player.sendMessage(plugin.color("&7Use seconds, like &e5&7 or &e0&7."));
        if ("action".equals(field)) player.sendMessage(plugin.color("&7This edits the last action's value."));
    }

    private House.ActionPad padFromToken(House house, ItemStack item) {
        if (house == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Action Pad ID: ")) return house.getActionPad(clean.substring("Action Pad ID: ".length()));
        }
        return null;
    }

    private String typeFromSlot(int slot) {
        int[] slots = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        for (int i = 0; i < slots.length; i++) if (slots[i] == slot) return TYPES[i];
        return null;
    }

    public boolean canEditActionPads(Player player, House house) {
        return plugin.canEditHousingObjects(player, house);
    }

    private boolean isPressurePlate(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_PLATE") || name.endsWith("_PRESSURE_PLATE");
    }

    private Material padMaterial() {
        return net.fakepixel.housing.util.Compat.mat("LIGHT_WEIGHTED_PRESSURE_PLATE");
    }

    private long parseCooldown(String raw) {
        try { return Math.max(0L, Long.parseLong(raw.trim()) * 1000L); }
        catch (Exception ignored) { return 2000L; }
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
        try { player.playSound(player.getLocation(), Sound.valueOf(raw == null || raw.isEmpty() ? "NOTE_PLING" : raw.toUpperCase(Locale.ENGLISH)), 1.0F, 1.0F); }
        catch (Throwable ignored) {}
    }

    private void changeTime(Player player, String raw) {
        long ticks;
        try { ticks = Long.parseLong(raw.trim()); } catch (Exception ex) { ticks = 6000L; }
        ticks = Math.max(0L, Math.min(24000L, ticks));
        player.setPlayerTime(ticks, false);
    }

    private void changeWeather(Player player, String raw) {
        String weather = raw == null ? "sunny" : raw.trim().toLowerCase(Locale.ENGLISH);
        boolean rain = "rain".equals(weather) || "storm".equals(weather) || "thunder".equals(weather);
        player.setPlayerWeather(rain ? WeatherType.DOWNFALL : WeatherType.CLEAR);
    }

    private String serialize(Location location) {
        String world = location.getWorld() == null ? "" : location.getWorld().getName();
        return world + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "," + location.getYaw() + "," + location.getPitch();
    }

    private Location parseLocation(House house, World fallbackWorld, String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String[] split = raw.split(",");
        try {
            if (split.length >= 6) {
                World world = Bukkit.getWorld(split[0]);
                if (world == null) world = fallbackWorld;
                return new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
            }
            if (split.length >= 3) {
                return new Location(fallbackWorld, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void sendActionBar(Player player, String message) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Class<?> component = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Object text = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escape(plugin.color(message)) + "\"}");
            Object packet = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat").getConstructor(component, byte.class).newInstance(text, (byte) 2);
            sendPacket(player, packet);
        } catch (Throwable ignored) {
            player.sendMessage(plugin.color(message));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void sendTitle(Player player, String title, String subtitle) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> chatSerializer = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");
            Class<?> component = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> titleAction = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle$EnumTitleAction");
            Class<?> packetTitle = Class.forName("net.minecraft.server." + version + ".PacketPlayOutTitle");
            Constructor<?> constructor = packetTitle.getConstructor(titleAction, component);
            Object titleComponent = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escape(plugin.color(title)) + "\"}");
            Object packet = constructor.newInstance(Enum.valueOf((Class<Enum>) titleAction, "TITLE"), titleComponent);
            sendPacket(player, packet);
            if (subtitle != null && !subtitle.isEmpty()) {
                Object subtitleComponent = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\":\"" + escape(plugin.color(subtitle)) + "\"}");
                sendPacket(player, constructor.newInstance(Enum.valueOf((Class<Enum>) titleAction, "SUBTITLE"), subtitleComponent));
            }
        } catch (Throwable ignored) {
            player.sendMessage(plugin.color(title));
        }
    }

    private void sendPacket(Player player, Object packet) throws Exception {
        Method getHandle = player.getClass().getMethod("getHandle");
        Object handle = getHandle.invoke(player);
        Object connection = handle.getClass().getField("playerConnection").get(handle);
        connection.getClass().getMethod("sendPacket", Class.forName(packet.getClass().getPackage().getName() + ".Packet")).invoke(connection, packet);
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
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
        private final String padId;
        private final String field;

        private Prompt(String worldName, String padId, String field) {
            this.worldName = worldName;
            this.padId = padId;
            this.field = field;
        }
    }
}
