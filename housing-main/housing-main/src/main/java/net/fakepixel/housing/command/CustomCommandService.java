package net.fakepixel.housing.command;

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
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CustomCommandService implements Listener {
    private static final String[] TYPES = new String[]{
            "MESSAGE", "TITLE", "ACTION_BAR", "TELEPORT", "PLAY_SOUND", "GIVE_ITEM", "POTION_EFFECT",
            "START_PARKOUR", "RESET_PARKOUR", "CHECKPOINT_PARKOUR", "OPEN_MENU", "CHANGE_TIME", "CHANGE_WEATHER", "CALL_FUNCTION"
    };
    private static final Set<String> PROTECTED = new HashSet<>(Arrays.asList(
            "housing", "house", "h", "npc", "adminbuild", "buildmode", "borderbypass",
            "gmc", "gms", "gma", "wand", "set", "walls", "undo", "copy", "pos1", "pos2",
            "tp", "tphere", "groupname", "grouptag", "visitingrule", "visitingrules"
    ));

    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Prompt> prompts = new HashMap<>();
    private final String listTitle = "House Commands";
    private final String editTitle = "Edit House Command";
    private final String addActionTitle = "Add Command Action";

    public CustomCommandService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
    }

    public void open(Player player, House house) {
        House currentHouse = currentPlayerHouse(player);
        if (house == null || currentHouse == null || !house.getWorldName().equals(currentHouse.getWorldName()) || !canEdit(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 54, listTitle);
        int slot = 0;
        for (House.CustomCommand command : house.getCustomCommands().values()) {
            if (slot >= 45) break;
            inv.setItem(slot++, commandItem(command));
        }
        inv.setItem(45, named(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aCreate Command", "&7Create a house command.", "", "&eClick to create!"));
        inv.setItem(49, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack", "&7Back to House Settings."));
        player.openInventory(inv);
    }

    public boolean isMenuTitle(String title) {
        return listTitle.equals(title) || editTitle.equals(title) || addActionTitle.equals(title);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        House house = currentPlayerHouse(player);
        if (house == null) return;
        String raw = event.getMessage();
        if (raw == null || !raw.startsWith("/")) return;
        String label = raw.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ENGLISH);
        if (PROTECTED.contains(label) || isStaffLike(label)) return;
        House.CustomCommand command = house.getCustomCommand(label);
        if (command == null || !command.isEnabled()) return;
        event.setCancelled(true);
        if (!canUse(player, house, command)) {
            player.sendMessage(plugin.color("&cYou do not have permission to use house commands."));
            return;
        }
        run(player, house, command);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!isMenuTitle(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        House house = currentPlayerHouse(player);
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
                House.CustomCommand command = commandFromItem(house, event.getCurrentItem());
                if (command != null) openEditor(player, house, command);
            }
            return;
        }
        if (addActionTitle.equals(title)) {
            House.CustomCommand command = commandFromToken(house, event.getView().getTopInventory().getItem(26));
            if (command == null) { player.closeInventory(); return; }
            String type = typeFromSlot(event.getRawSlot());
            if (type != null) {
                command.getActions().add(defaultAction(type, player));
                houses.saveAll();
                openEditor(player, house, command);
            } else if (event.getRawSlot() == 24) openEditor(player, house, command);
            return;
        }
        House.CustomCommand command = commandFromToken(house, event.getView().getTopInventory().getItem(35));
        if (command == null) { player.closeInventory(); return; }
        int slot = event.getRawSlot();
        if (slot == 10) beginPrompt(player, house, command, "rename");
        else if (slot == 11) beginPrompt(player, house, command, "aliases");
        else if (slot == 12) openAddAction(player, command);
        else if (slot == 13) { if (!command.getActions().isEmpty()) command.getActions().remove(command.getActions().size() - 1); saveAndOpen(player, house, command); }
        else if (slot == 14) { if (command.getActions().size() > 1) command.getActions().add(0, command.getActions().remove(command.getActions().size() - 1)); saveAndOpen(player, house, command); }
        else if (slot == 15) beginPrompt(player, house, command, "cooldown");
        else if (slot == 16) { command.setEnabled(!command.isEnabled()); saveAndOpen(player, house, command); }
        else if (slot == 17) { command.setGuestsAllowed(!command.isGuestsAllowed()); saveAndOpen(player, house, command); }
        else if (slot == 20) beginPrompt(player, house, command, "action");
        else if (slot == 22) {
            if (!canDelete(player, house)) { player.sendMessage(plugin.msg("no-permission")); return; }
            house.removeCustomCommand(command.getName());
            houses.saveAll();
            player.sendMessage(plugin.color("&cDeleted command."));
            open(player, house);
        }
        else if (slot == 31) open(player, house);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Prompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handlePrompt(event.getPlayer(), prompt, message));
    }

    private void run(Player player, House house, House.CustomCommand command) {
        House currentHouse = currentPlayerHouse(player);
        if (currentHouse == null || house == null || command == null || !currentHouse.getWorldName().equals(house.getWorldName())) return;
        String key = house.getWorldName() + ":" + command.getName() + ":" + player.getUniqueId();
        long now = System.currentTimeMillis();
        long next = cooldowns.getOrDefault(key, 0L);
        if (next > now) return;
        if (command.getCooldownMs() > 0L) cooldowns.put(key, now + command.getCooldownMs());
        for (House.ButtonAction action : command.getActions()) runAction(player, house, action);
    }

    private House currentPlayerHouse(Player player) {
        if (player == null || player.getWorld() == null) return null;
        String worldName = player.getWorld().getName();
        if (isNonHouseWorld(worldName)) return null;
        House house = houses.getHouseByWorld(worldName);
        if (house == null || house.getWorldName() == null) return null;
        return worldName.equals(house.getWorldName()) ? house : null;
    }

    private boolean isNonHouseWorld(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) return true;
        if (houses.isLobbyWorld(worldName)) return true;
        for (String staticWorld : plugin.getStaticConfig().getStringList("settings.static-worlds")) {
            if (worldName.equalsIgnoreCase(staticWorld)) return true;
        }
        String mainWorld = plugin.getStaticConfig().getString("settings.main-world", "");
        if (!mainWorld.isEmpty() && worldName.equalsIgnoreCase(mainWorld)) return true;
        String fallbackWorld = plugin.getStaticConfig().getString("settings.fallback-spawn-world", "");
        return !fallbackWorld.isEmpty() && worldName.equalsIgnoreCase(fallbackWorld);
    }

    private void runAction(Player player, House house, House.ButtonAction action) {
        String type = action.getType();
        String value = action.getValue();
        if ("MESSAGE".equals(type)) player.sendMessage(plugin.color(value.isEmpty() ? "&aCommand action!" : value));
        else if ("TITLE".equals(type)) {
            String[] split = (value.isEmpty() ? "&aCommand" : value).split("\\n", 2);
            sendTitle(player, split[0], split.length > 1 ? split[1] : "");
        } else if ("ACTION_BAR".equals(type)) sendActionBar(player, value.isEmpty() ? "&aCommand action!" : value);
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
        else if ("CALL_FUNCTION".equals(type) && plugin.getFunctionService() != null) plugin.getFunctionService().runFunction(player, house, value);
    }

    private void openEditor(Player player, House house, House.CustomCommand command) {
        Inventory inv = Bukkit.createInventory(null, 36, editTitle);
        inv.setItem(4, named(net.fakepixel.housing.util.Compat.mat("COMMAND"), "&a/" + command.getName(),
                "&7Enabled: " + enabledText(command.isEnabled()),
                "&7Guests: " + enabledText(command.isGuestsAllowed()),
                "&7Aliases: &f" + (command.getAliases().isEmpty() ? "none" : String.join(", ", command.getAliases())),
                "&7Cooldown: &f" + (command.getCooldownMs() / 1000L) + "s",
                "&7Actions: &f" + command.getActions().size()));
        inv.setItem(10, named(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aRename Command"));
        inv.setItem(11, named(net.fakepixel.housing.util.Compat.mat("NAME_TAG"), "&aEdit Aliases", "&7Comma-separated aliases."));
        inv.setItem(12, named(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aAdd Action"));
        inv.setItem(13, named(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cRemove Action", "&7Removes the last action."));
        inv.setItem(14, named(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aReorder Actions", "&7Moves the last action to the top."));
        inv.setItem(15, named(net.fakepixel.housing.util.Compat.mat("WATCH"), "&aSet Cooldown"));
        inv.setItem(16, named(command.isEnabled() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aEnabled: " + enabledText(command.isEnabled())));
        inv.setItem(17, named(command.isGuestsAllowed() ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), "&aGuests: " + enabledText(command.isGuestsAllowed()), "&7Allow guests to use this command."));
        inv.setItem(20, named(net.fakepixel.housing.util.Compat.mat("BOOK_AND_QUILL"), "&aEdit Last Action", "&7Edits the last action value."));
        inv.setItem(22, named(net.fakepixel.housing.util.Compat.mat("TNT"), "&cDelete Command"));
        inv.setItem(31, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(35, token(command));
        player.openInventory(inv);
    }

    private void openAddAction(Player player, House.CustomCommand command) {
        Inventory inv = Bukkit.createInventory(null, 27, addActionTitle);
        int[] slots = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        for (int i = 0; i < TYPES.length; i++) inv.setItem(slots[i], named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&a" + TYPES[i]));
        inv.setItem(24, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(26, token(command));
        player.openInventory(inv);
    }

    private void beginPrompt(Player player, House house, House.CustomCommand command, String field) {
        if (command == null && !"create".equals(field)) return;
        prompts.put(player.getUniqueId(), new Prompt(house.getWorldName(), command == null ? "" : command.getName(), field));
        player.closeInventory();
        if ("create".equals(field)) player.sendMessage(plugin.color("&eType the command name in chat. Type &ccancel &eto cancel."));
        else if ("aliases".equals(field)) player.sendMessage(plugin.color("&eType aliases separated by commas. Type &ccancel &eto cancel."));
        else if ("cooldown".equals(field)) player.sendMessage(plugin.color("&eType cooldown seconds. Type &ccancel &eto cancel."));
        else if ("action".equals(field)) player.sendMessage(plugin.color("&eType the last action value. Type &ccancel &eto cancel."));
        else player.sendMessage(plugin.color("&eType the new command name. Type &ccancel &eto cancel."));
    }

    private void handlePrompt(Player player, Prompt prompt, String message) {
        House house = currentPlayerHouse(player);
        if (house == null || !house.getWorldName().equals(prompt.worldName) || !canEdit(player, house)) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if ("cancel".equalsIgnoreCase(message.trim())) {
            player.sendMessage(plugin.color("&cCommand edit cancelled."));
            open(player, house);
            return;
        }
        House.CustomCommand command = prompt.commandName.isEmpty() ? null : house.getCustomCommand(prompt.commandName);
        if ("create".equals(prompt.field)) {
            if (!canCreate(player, house)) { player.sendMessage(plugin.msg("no-permission")); return; }
            House.CustomCommand created = house.addCustomCommand(message);
            if (created == null || isProtected(created.getName())) {
                if (created != null) house.removeCustomCommand(created.getName());
                player.sendMessage(plugin.color("&cThat command name cannot be used."));
                open(player, house);
                return;
            }
            houses.saveAll();
            openEditor(player, house, created);
            return;
        }
        if (command == null) { player.sendMessage(plugin.color("&cUnknown command.")); open(player, house); return; }
        if ("rename".equals(prompt.field)) {
            if (!canEdit(player, house) || isProtected(message)) { player.sendMessage(plugin.msg("no-permission")); openEditor(player, house, command); return; }
            if (!house.renameCustomCommand(command.getName(), message)) player.sendMessage(plugin.color("&cCould not rename command."));
            command = house.getCustomCommand(message);
        } else if ("aliases".equals(prompt.field)) {
            List<String> aliases = new ArrayList<>();
            for (String alias : message.split(",")) if (!isProtected(alias)) aliases.add(alias.trim());
            command.setAliases(aliases);
        } else if ("cooldown".equals(prompt.field)) {
            try { command.setCooldownMs(Math.max(0L, Long.parseLong(message.trim())) * 1000L); } catch (NumberFormatException ignored) {}
        } else if ("action".equals(prompt.field) && !command.getActions().isEmpty()) {
            command.getActions().get(command.getActions().size() - 1).setValue(message);
        }
        houses.saveAll();
        player.sendMessage(plugin.color("&aSaved command."));
        openEditor(player, house, command == null ? house.getCustomCommand(prompt.commandName) : command);
    }

    private boolean canEdit(Player player, House house) {
        if (player == null || house == null) return false;
        UUID uuid = player.getUniqueId();
        if (house.getOwner().equals(uuid)) return true;
        if (player.hasPermission("housing.commands.admin") || player.hasPermission("housing.admin")) return true;
        return house.hasGroupPermission(uuid, "settings")
                || house.hasGroupPermission(uuid, "custom-command");
    }

    private boolean canUse(Player player, House house, House.CustomCommand command) {
        if (player == null || house == null || command == null) return false;
        UUID uuid = player.getUniqueId();
        if (house.getOwner().equals(uuid)) return true;
        if (player.hasPermission("housing.commands.admin") || player.hasPermission("housing.admin")) return true;
        if (command.isGuestsAllowed()) return true;
        return house.hasGroupPermission(uuid, "custom-command")
                || house.hasGroupPermission(uuid, "command")
                || house.hasGroupPermission(uuid, "commands")
                || house.hasGroupPermission(uuid, "commands-use")
                || house.hasGroupPermission(uuid, "use-command");
    }

    private boolean canCreate(Player player, House house) {
        return canEdit(player, house);
    }

    private boolean canDelete(Player player, House house) {
        return canEdit(player, house);
    }

    private void saveAndOpen(Player player, House house, House.CustomCommand command) {
        houses.saveAll();
        openEditor(player, house, command);
    }

    private House.ButtonAction defaultAction(String type, Player player) {
        if ("MESSAGE".equals(type)) return new House.ButtonAction(type, "&aCommand action!");
        if ("TITLE".equals(type)) return new House.ButtonAction(type, "&aCommand");
        if ("ACTION_BAR".equals(type)) return new House.ButtonAction(type, "&aCommand action!");
        if ("TELEPORT".equals(type)) return new House.ButtonAction(type, serialize(player.getLocation()));
        if ("PLAY_SOUND".equals(type)) return new House.ButtonAction(type, "NOTE_PLING");
        if ("GIVE_ITEM".equals(type)) return new House.ButtonAction(type, "STONE:1");
        if ("POTION_EFFECT".equals(type)) return new House.ButtonAction(type, "SPEED:100:1");
        if ("CHANGE_TIME".equals(type)) return new House.ButtonAction(type, "6000");
        if ("CHANGE_WEATHER".equals(type)) return new House.ButtonAction(type, "sunny");
        return new House.ButtonAction(type, "");
    }

    private ItemStack commandItem(House.CustomCommand command) {
        return named(command.isEnabled() ? new ItemStack(net.fakepixel.housing.util.Compat.mat("PAPER")) : net.fakepixel.housing.util.Compat.grayDye(),
                "&a/" + command.getName(),
                "&7Aliases: &f" + (command.getAliases().isEmpty() ? "none" : String.join(", ", command.getAliases())),
                "&7Actions: &f" + command.getActions().size(),
                "&7Cooldown: &f" + (command.getCooldownMs() / 1000L) + "s",
                "&8Command ID: " + command.getName(),
                "",
                "&eClick to edit!");
    }

    private House.CustomCommand commandFromItem(House house, ItemStack item) {
        if (house == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Command ID: ")) return house.getCustomCommand(clean.substring("Command ID: ".length()));
        }
        return null;
    }

    private House.CustomCommand commandFromToken(House house, ItemStack item) {
        return commandFromItem(house, item);
    }

    private ItemStack token(House.CustomCommand command) {
        return named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Command Token", "&8Command ID: " + command.getName());
    }

    private String typeFromSlot(int slot) {
        int[] slots = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        for (int i = 0; i < slots.length; i++) if (slots[i] == slot) return TYPES[i];
        return null;
    }

    private boolean isProtected(String command) {
        String clean = clean(command);
        return PROTECTED.contains(clean) || isStaffLike(clean);
    }

    private boolean isStaffLike(String command) {
        String clean = clean(command);
        return clean.startsWith("op") || clean.startsWith("deop") || clean.startsWith("perm") || clean.equals("stop") || clean.equals("restart") || clean.equals("reload") || clean.equals("ban") || clean.equals("kick") || clean.equals("mute");
    }

    private String clean(String command) {
        if (command == null) return "";
        String clean = command.trim().toLowerCase(Locale.ENGLISH);
        if (clean.startsWith("/")) clean = clean.substring(1);
        int space = clean.indexOf(' ');
        if (space >= 0) clean = clean.substring(0, space);
        return clean.replaceAll("[^a-z0-9_-]", "");
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

    private String enabledText(boolean enabled) {
        return enabled ? "&aEnabled" : "&cDisabled";
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static class Prompt {
        private final String worldName;
        private final String commandName;
        private final String field;

        private Prompt(String worldName, String commandName, String field) {
            this.worldName = worldName;
            this.commandName = commandName;
            this.field = field;
        }
    }
}
