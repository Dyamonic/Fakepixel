package net.fakepixel.housing.function;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FunctionService implements Listener {
    private static final int MAX_DEPTH = 5;
    private static final String[] TYPES = new String[]{
            "MESSAGE", "TITLE", "ACTION_BAR", "TELEPORT", "PLAY_SOUND", "GIVE_ITEM", "POTION_EFFECT",
            "START_PARKOUR", "RESET_PARKOUR", "CHECKPOINT_PARKOUR", "OPEN_MENU", "CHANGE_TIME", "CHANGE_WEATHER", "CALL_FUNCTION"
    };

    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Prompt> prompts = new HashMap<>();
    private final ThreadLocal<Set<String>> activeFunctions = ThreadLocal.withInitial(HashSet::new);
    private final String listTitle = "House Functions";
    private final String editTitle = "Edit Function";
    private final String addTitle = "Add Function Action";

    public FunctionService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public void open(Player player, House house) {
        if (house == null || !canEdit(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, listTitle);
        int slot = 0;
        for (House.Function function : house.getFunctions().values()) {
            if (slot >= 45) break;
            inv.setItem(slot++, functionItem(function));
        }
        inv.setItem(45, named(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aCreate Function", "&7Create a reusable action list.", "", "&eClick to create!"));
        inv.setItem(49, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to House Settings."));
        player.openInventory(inv);
    }

    public boolean isMenuTitle(String title) {
        return listTitle.equals(title) || editTitle.equals(title) || addTitle.equals(title);
    }

    public void runFunction(Player player, House house, String functionId) {
        Set<String> stack = activeFunctions.get();
        boolean root = stack.isEmpty();
        runFunction(player, house, functionId, stack.size(), stack);
        if (root) stack.clear();
    }

    public void runAction(Player player, House house, House.ButtonAction action) {
        Set<String> stack = activeFunctions.get();
        boolean root = stack.isEmpty();
        runAction(player, house, action, stack.size(), stack);
        if (root) stack.clear();
    }

    private void runFunction(Player player, House house, String functionId, int depth, Set<String> stack) {
        if (player == null || house == null || functionId == null) return;
        if (!house.getWorldName().equals(player.getWorld().getName())) return;
        String id = House.cleanFunctionId(functionId);
        House.Function function = house.getFunction(id);
        if (function == null) return;
        if (depth >= MAX_DEPTH || stack.contains(id)) {
            plugin.getLogger().warning("Cancelled looping Housing function '" + id + "' in house " + house.getWorldName() + ".");
            return;
        }
        String cooldownKey = house.getWorldName() + ":" + id + ":" + player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.getOrDefault(cooldownKey, 0L) > now) return;
        if (function.getCooldownMs() > 0L) cooldowns.put(cooldownKey, now + function.getCooldownMs());
        stack.add(id);
        for (House.ButtonAction action : function.getActions()) runAction(player, house, action, depth + 1, stack);
        stack.remove(id);
    }

    private void runAction(Player player, House house, House.ButtonAction action, int depth, Set<String> stack) {
        String type = action.getType();
        String value = action.getValue();
        if ("MESSAGE".equals(type)) player.sendMessage(plugin.color(value.isEmpty() ? "&aFunction action!" : value));
        else if ("TITLE".equals(type)) {
            String[] split = (value.isEmpty() ? "&aFunction" : value).split("\\n", 2);
            sendTitle(player, split[0], split.length > 1 ? split[1] : "");
        } else if ("ACTION_BAR".equals(type)) sendActionBar(player, value.isEmpty() ? "&aFunction action!" : value);
        else if ("TELEPORT".equals(type)) {
            Location target = parseLocation(player.getWorld(), value);
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
        else if ("CALL_FUNCTION".equals(type)) runFunction(player, house, value, depth, stack);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!isMenuTitle(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEdit(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (listTitle.equals(title)) {
            int slot = event.getRawSlot();
            if (slot == 45) beginPrompt(player, house, null, "create");
            else if (slot == 49) plugin.getHousingMenu().openSettingsMenu(player, house);
            else {
                House.Function function = functionFromItem(house, event.getCurrentItem());
                if (function != null) openEditor(player, function);
            }
            return;
        }
        if (addTitle.equals(title)) {
            House.Function function = functionFromToken(house, event.getView().getTopInventory().getItem(26));
            if (function == null) { player.closeInventory(); return; }
            String type = typeFromSlot(event.getRawSlot());
            if (type != null) {
                function.getActions().add(defaultAction(type, player));
                houses.saveAll();
                openEditor(player, function);
            } else if (event.getRawSlot() == 24) openEditor(player, function);
            return;
        }
        House.Function function = functionFromToken(house, event.getView().getTopInventory().getItem(35));
        if (function == null) { player.closeInventory(); return; }
        int slot = event.getRawSlot();
        if (slot == 10) beginPrompt(player, house, function, "rename");
        else if (slot == 11) openAddAction(player, function);
        else if (slot == 12) { if (!function.getActions().isEmpty()) function.getActions().remove(function.getActions().size() - 1); saveAndOpen(player, function); }
        else if (slot == 13) { if (function.getActions().size() > 1) function.getActions().add(0, function.getActions().remove(function.getActions().size() - 1)); saveAndOpen(player, function); }
        else if (slot == 14) beginPrompt(player, house, function, "action");
        else if (slot == 15) beginPrompt(player, house, function, "cooldown");
        else if (slot == 16) { runFunction(player, house, function.getId()); openEditor(player, function); }
        else if (slot == 22) { house.removeFunction(function.getId()); houses.saveAll(); player.sendMessage(plugin.color("&cDeleted function.")); open(player, house); }
        else if (slot == 31) open(player, house);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Prompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handlePrompt(event.getPlayer(), prompt, message));
    }

    private void handlePrompt(Player player, Prompt prompt, String message) {
        House house = houses.getHouseByWorld(prompt.worldName);
        if (house == null || !canEdit(player, house)) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if ("cancel".equalsIgnoreCase(message.trim())) {
            player.sendMessage(plugin.color("&cFunction edit cancelled."));
            open(player, house);
            return;
        }
        House.Function function = prompt.functionId.isEmpty() ? null : house.getFunction(prompt.functionId);
        if ("create".equals(prompt.field)) {
            House.Function created = house.addFunction(message);
            if (created == null) {
                player.sendMessage(plugin.color("&cCould not create that function."));
                open(player, house);
                return;
            }
            houses.saveAll();
            openEditor(player, created);
            return;
        }
        if (function == null) { player.sendMessage(plugin.color("&cUnknown function.")); open(player, house); return; }
        if ("rename".equals(prompt.field)) {
            if (!house.renameFunction(function.getId(), message)) {
                player.sendMessage(plugin.color("&cCould not rename that function."));
                openEditor(player, function);
                return;
            }
            function = house.getFunction(message);
        } else if ("cooldown".equals(prompt.field)) {
            try { function.setCooldownMs(Math.max(0L, Long.parseLong(message.trim())) * 1000L); } catch (NumberFormatException ignored) {}
        } else if ("action".equals(prompt.field) && !function.getActions().isEmpty()) {
            function.getActions().get(function.getActions().size() - 1).setValue(message.trim());
        }
        houses.saveAll();
        player.sendMessage(plugin.color("&aSaved function."));
        openEditor(player, function == null ? house.getFunction(prompt.functionId) : function);
    }

    private boolean canEdit(Player player, House house) {
        if (player == null || house == null) return false;
        if (player.hasPermission("housing.functions.admin") || player.hasPermission("housing.admin")) return true;
        return house.getOwner().equals(player.getUniqueId())
                || house.hasGroupPermission(player.getUniqueId(), "settings")
                || house.hasGroupPermission(player.getUniqueId(), "function");
    }

    private void openEditor(Player player, House.Function function) {
        Inventory inv = Bukkit.createInventory(null, 36, editTitle);
        inv.setItem(4, named(net.fakepixel.housing.util.Compat.mat("REDSTONE"), "&a" + function.getDisplayName(),
                "&7ID: &f" + function.getId(),
                "&7Cooldown: &f" + (function.getCooldownMs() / 1000L) + "s",
                "&7Actions: &f" + function.getActions().size()));
        inv.setItem(10, named(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aRename Function"));
        inv.setItem(11, named(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aAdd Action"));
        inv.setItem(12, named(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cRemove Action", "&7Removes the last action."));
        inv.setItem(13, named(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aReorder Actions", "&7Moves the last action to the top."));
        inv.setItem(14, named(net.fakepixel.housing.util.Compat.mat("BOOK_AND_QUILL"), "&aEdit Action", "&7Edits the last action value."));
        inv.setItem(15, named(net.fakepixel.housing.util.Compat.mat("WATCH"), "&aSet Cooldown"));
        inv.setItem(16, named(net.fakepixel.housing.util.Compat.limeDye(), "&aTest Function"));
        inv.setItem(22, named(net.fakepixel.housing.util.Compat.mat("TNT"), "&cDelete Function"));
        inv.setItem(31, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(35, token(function));
        player.openInventory(inv);
    }

    private void openAddAction(Player player, House.Function function) {
        Inventory inv = Bukkit.createInventory(null, 27, addTitle);
        int[] slots = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        for (int i = 0; i < TYPES.length; i++) inv.setItem(slots[i], named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&a" + TYPES[i]));
        inv.setItem(24, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(26, token(function));
        player.openInventory(inv);
    }

    private void beginPrompt(Player player, House house, House.Function function, String field) {
        prompts.put(player.getUniqueId(), new Prompt(house.getWorldName(), function == null ? "" : function.getId(), field));
        player.closeInventory();
        if ("create".equals(field)) player.sendMessage(plugin.color("&eType the function id in chat. Type &ccancel &eto cancel."));
        else if ("cooldown".equals(field)) player.sendMessage(plugin.color("&eType cooldown seconds. Type &ccancel &eto cancel."));
        else if ("action".equals(field)) player.sendMessage(plugin.color("&eType the last action value or function id. Type &ccancel &eto cancel."));
        else player.sendMessage(plugin.color("&eType the new function id in chat. Type &ccancel &eto cancel."));
    }

    private void saveAndOpen(Player player, House.Function function) {
        houses.saveAll();
        openEditor(player, function);
    }

    private House.ButtonAction defaultAction(String type, Player player) {
        if ("MESSAGE".equals(type)) return new House.ButtonAction(type, "&aFunction action!");
        if ("TITLE".equals(type)) return new House.ButtonAction(type, "&aFunction");
        if ("ACTION_BAR".equals(type)) return new House.ButtonAction(type, "&aFunction action!");
        if ("TELEPORT".equals(type)) return new House.ButtonAction(type, serialize(player.getLocation()));
        if ("PLAY_SOUND".equals(type)) return new House.ButtonAction(type, "NOTE_PLING");
        if ("GIVE_ITEM".equals(type)) return new House.ButtonAction(type, "STONE:1");
        if ("POTION_EFFECT".equals(type)) return new House.ButtonAction(type, "SPEED:100:1");
        if ("CHANGE_TIME".equals(type)) return new House.ButtonAction(type, "6000");
        if ("CHANGE_WEATHER".equals(type)) return new House.ButtonAction(type, "sunny");
        return new House.ButtonAction(type, "");
    }

    private ItemStack functionItem(House.Function function) {
        return named(net.fakepixel.housing.util.Compat.mat("REDSTONE"),
                "&a" + function.getDisplayName(),
                "&7ID: &f" + function.getId(),
                "&7Actions: &f" + function.getActions().size(),
                "&7Cooldown: &f" + (function.getCooldownMs() / 1000L) + "s",
                "&8Function ID: " + function.getId(),
                "",
                "&eClick to edit!");
    }

    private House.Function functionFromItem(House house, ItemStack item) {
        if (house == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Function ID: ")) return house.getFunction(clean.substring("Function ID: ".length()));
        }
        return null;
    }

    private House.Function functionFromToken(House house, ItemStack item) {
        return functionFromItem(house, item);
    }

    private ItemStack token(House.Function function) {
        return named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Function Token", "&8Function ID: " + function.getId());
    }

    private String typeFromSlot(int slot) {
        int[] slots = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        for (int i = 0; i < slots.length; i++) if (slots[i] == slot) return TYPES[i];
        return null;
    }

    private void giveItem(Player player, String raw) {
        String[] split = raw == null ? new String[0] : raw.split(":", 2);
        Material material = split.length == 0 ? Material.STONE : Material.matchMaterial(split[0]);
        if (material == null) material = net.fakepixel.housing.util.Compat.mat(split.length == 0 ? "STONE" : split[0]);
        int amount = 1;
        if (split.length > 1) try { amount = Math.max(1, Math.min(64, Integer.parseInt(split[1]))); } catch (Exception ignored) {}
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
        player.setPlayerTime(Math.max(0L, Math.min(24000L, ticks)), false);
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

    private Location parseLocation(World fallbackWorld, String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String[] split = raw.split(",");
        try {
            if (split.length >= 6) {
                World world = Bukkit.getWorld(split[0]);
                if (world == null) world = fallbackWorld;
                return new Location(world, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]), Float.parseFloat(split[4]), Float.parseFloat(split[5]));
            }
            if (split.length >= 3) return new Location(fallbackWorld, Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
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
            sendPacket(player, constructor.newInstance(Enum.valueOf((Class<Enum>) titleAction, "TITLE"), titleComponent));
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

    private String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class Prompt {
        private final String worldName;
        private final String functionId;
        private final String field;

        private Prompt(String worldName, String functionId, String field) {
            this.worldName = worldName;
            this.functionId = functionId;
            this.field = field;
        }
    }
}
