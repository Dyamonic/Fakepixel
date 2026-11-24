package net.fakepixel.housing.region;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class RegionService implements Listener {
    private static final String[] TYPES = new String[]{
            "MESSAGE", "TITLE", "ACTION_BAR", "TELEPORT", "PLAY_SOUND", "GIVE_ITEM", "POTION_EFFECT",
            "START_PARKOUR", "RESET_PARKOUR", "CHECKPOINT_PARKOUR", "OPEN_MENU", "CHANGE_TIME", "CHANGE_WEATHER", "CALL_FUNCTION"
    };

    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final Map<UUID, String> currentRegion = new HashMap<>();
    private final Map<String, Long> whileCooldowns = new HashMap<>();
    private final Map<UUID, GameMode> previousGameModes = new HashMap<>();
    private final Map<UUID, Prompt> prompts = new HashMap<>();
    private final String editorTitle = "Region Editor";
    private final String rulesTitle = "Region Rules";
    private final String actionsTitle = "Region Actions";
    private final String addActionTitle = "Add Region Action";

    public RegionService(HousingPlugin plugin, HouseManager houses) {
        this.plugin = plugin;
        this.houses = houses;
        Bukkit.getScheduler().runTaskTimer(plugin, this::runWhileInsideActions, 20L, 40L);
    }

    public boolean handleCommand(Player player, String[] args) {
        if (args.length < 2) {
            help(player);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ENGLISH);
        if ("wand".equals(sub)) { giveWand(player); return true; }
        if ("pos1".equals(sub)) { setPos(player, player.getLocation(), true); return true; }
        if ("pos2".equals(sub)) { setPos(player, player.getLocation(), false); return true; }
        if ("list".equals(sub)) { list(player); return true; }
        if ("create".equals(sub)) { create(player, args); return true; }
        if ("delete".equals(sub)) { delete(player, args); return true; }
        if ("edit".equals(sub)) { edit(player, args); return true; }
        help(player);
        return true;
    }

    public List<String> tab(Player player, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ENGLISH);
            for (String value : new String[]{"wand", "create", "delete", "list", "edit", "pos1", "pos2"}) if (value.startsWith(prefix)) out.add(value);
            return out;
        }
        if (args.length == 3 && ("delete".equalsIgnoreCase(args[1]) || "edit".equalsIgnoreCase(args[1]))) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house != null) {
                String prefix = args[2].toLowerCase(Locale.ENGLISH);
                for (String id : house.getRegions().keySet()) if (id.startsWith(prefix)) out.add(id);
            }
        }
        return out;
    }

    public boolean isRegionWand(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            if ("Housing Special: region-wand".equals(ChatColor.stripColor(line))) return true;
        }
        return false;
    }

    public boolean canEditRegions(Player player, House house) {
        if (player == null || house == null) return false;
        UUID uuid = player.getUniqueId();
        if (house.getOwner().equals(uuid)) return true;
        if (house.hasGroupPermission(uuid, "region") || house.hasGroupPermission(uuid, "region-edit")) return true;
        return plugin.canBypassHouseMode(player) || player.hasPermission("housing.admin");
    }

    public boolean canBuildAt(Player player, House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null || region.isBuild();
    }

    public boolean canBreakAt(Player player, House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null || region.isBlockBreaking();
    }

    public boolean allowsPvp(House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null ? house == null || house.isPvpEnabled() : region.isPvp();
    }

    public boolean allowsDamage(House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null || region.isDamage();
    }

    public boolean allowsFallDamage(House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null ? house == null || house.isFallDamageEnabled() : region.isFallDamage();
    }

    public boolean allowsHunger(House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null ? house == null || house.isHungerEnabled() : region.isHunger();
    }

    public boolean allowsProjectiles(House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null || region.isAllowProjectiles();
    }

    public boolean allowsItemDrops(House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null || region.isAllowItemDrops();
    }

    public boolean allowsItemPickup(House house, Location location) {
        House.Region region = topRegion(house, location);
        return region == null || region.isAllowItemPickup();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWandClick(PlayerInteractEvent event) {
        if (!isRegionWand(event.getItem())) return;
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        if (event.getClickedBlock() == null) return;
        setPos(event.getPlayer(), event.getClickedBlock().getLocation(), event.getAction() == Action.LEFT_CLICK_BLOCK);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (sameBlock(event.getFrom(), event.getTo())) return;
        updateRegion(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        leaveCurrent(event.getPlayer(), null);
        Bukkit.getScheduler().runTask(plugin, () -> updateRegion(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        currentRegion.remove(event.getPlayer().getUniqueId());
        previousGameModes.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!editorTitle.equals(title) && !rulesTitle.equals(title) && !actionsTitle.equals(title) && !addActionTitle.equals(title)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditRegions(player, house)) {
            player.closeInventory();
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (addActionTitle.equals(title)) {
            Token token = token(event.getView().getTopInventory().getItem(26));
            House.Region region = token == null ? null : house.getRegion(token.regionId);
            if (region == null) { player.closeInventory(); return; }
            String type = typeFromSlot(event.getRawSlot());
            if (type != null) {
                actionList(region, token.actionSet).add(defaultAction(type, player));
                houses.saveAll();
                openActions(player, region, token.actionSet);
            } else if (event.getRawSlot() == 24) openActions(player, region, token.actionSet);
            return;
        }
        Token token = token(event.getView().getTopInventory().getItem(35));
        House.Region region = token == null ? null : house.getRegion(token.regionId);
        if (region == null) { player.closeInventory(); return; }
        int slot = event.getRawSlot();
        if (editorTitle.equals(title)) {
            if (slot == 10) beginPrompt(player, house, region, "name", "");
            else if (slot == 11) beginPrompt(player, house, region, "priority", "");
            else if (slot == 12) openRules(player, region);
            else if (slot == 13) openActions(player, region, "enter");
            else if (slot == 14) openActions(player, region, "leave");
            else if (slot == 15) openActions(player, region, "while");
            else if (slot == 19) { region.setPvp(!region.isPvp()); saveEdit(player, region); }
            else if (slot == 20) { region.setBuild(!region.isBuild()); saveEdit(player, region); }
            else if (slot == 21) { region.setFly(!region.isFly()); saveEdit(player, region); }
            else if (slot == 22) { region.setDamage(!region.isDamage()); saveEdit(player, region); }
            else if (slot == 23) { house.removeRegion(region.getId()); houses.saveAll(); player.closeInventory(); player.sendMessage(plugin.color("&cDeleted region.")); }
        } else if (rulesTitle.equals(title)) {
            if (slot == 10) region.setPvp(!region.isPvp());
            else if (slot == 11) region.setBuild(!region.isBuild());
            else if (slot == 12) region.setBlockBreaking(!region.isBlockBreaking());
            else if (slot == 13) region.setFly(!region.isFly());
            else if (slot == 14) region.setDamage(!region.isDamage());
            else if (slot == 15) region.setFallDamage(!region.isFallDamage());
            else if (slot == 16) region.setHunger(!region.isHunger());
            else if (slot == 19) beginPrompt(player, house, region, "effects", "");
            else if (slot == 20) beginPrompt(player, house, region, "gamemode", "");
            else if (slot == 21) region.setApplyPvpKit(!region.isApplyPvpKit());
            else if (slot == 22) region.setAllowProjectiles(!region.isAllowProjectiles());
            else if (slot == 23) region.setAllowItemDrops(!region.isAllowItemDrops());
            else if (slot == 24) region.setAllowItemPickup(!region.isAllowItemPickup());
            else if (slot == 31) { openEditor(player, region); return; }
            houses.saveAll();
            if (slot != 19 && slot != 20) openRules(player, region);
        } else if (actionsTitle.equals(title)) {
            String set = token.actionSet;
            List<House.ButtonAction> actions = actionList(region, set);
            if (slot == 10) openAddAction(player, region, set);
            else if (slot == 11) { if (!actions.isEmpty()) actions.remove(actions.size() - 1); houses.saveAll(); openActions(player, region, set); }
            else if (slot == 12) beginPrompt(player, house, region, "action", set);
            else if (slot == 13) { if (actions.size() > 1) actions.add(0, actions.remove(actions.size() - 1)); houses.saveAll(); openActions(player, region, set); }
            else if (slot == 31) openEditor(player, region);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Prompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handlePrompt(event.getPlayer(), prompt, message));
    }

    private void giveWand(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditRegions(player, house)) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        player.getInventory().addItem(wand()).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        player.sendMessage(plugin.color("&aRegion Wand added to your inventory."));
    }

    private ItemStack wand() {
        ItemStack stack = new ItemStack(net.fakepixel.housing.util.Compat.mat("BLAZE_ROD"));
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(plugin.color("&aRegion Wand"));
        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Left-click to set position 1."));
        lore.add(plugin.color("&7Right-click to set position 2."));
        lore.add(plugin.color("&8Housing Special: region-wand"));
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private void setPos(Player player, Location location, boolean first) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditRegions(player, house)) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), ignored -> new Selection(house.getWorldName()));
        if (!house.getWorldName().equals(selection.worldName)) selection = new Selection(house.getWorldName());
        if (first) selection.pos1 = location.clone();
        else selection.pos2 = location.clone();
        selections.put(player.getUniqueId(), selection);
        player.sendMessage(plugin.color("&aRegion position " + (first ? "1" : "2") + " set to &e" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + "&a."));
    }

    private void create(Player player, String[] args) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditRegions(player, house)) { player.sendMessage(plugin.msg("no-permission")); return; }
        if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /housing region create <name>")); return; }
        Selection selection = selections.get(player.getUniqueId());
        if (selection == null || selection.pos1 == null || selection.pos2 == null) {
            player.sendMessage(plugin.color("&cSet both region positions first."));
            return;
        }
        House.Region region = house.addRegion(join(args, 2), selection.pos1, selection.pos2);
        if (region == null) {
            player.sendMessage(plugin.color("&cCould not create region. The name may already exist or positions are invalid."));
            return;
        }
        houses.saveAll();
        player.sendMessage(plugin.color("&aCreated region &e" + region.getName() + "&a."));
        openEditor(player, region);
    }

    private void delete(Player player, String[] args) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditRegions(player, house)) { player.sendMessage(plugin.msg("no-permission")); return; }
        if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /housing region delete <name>")); return; }
        House.Region region = house.getRegion(args[2]);
        if (region == null) { player.sendMessage(plugin.color("&cUnknown region.")); return; }
        house.removeRegion(region.getId());
        houses.saveAll();
        player.sendMessage(plugin.color("&cDeleted region &e" + region.getName() + "&c."));
    }

    private void list(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditRegions(player, house)) { player.sendMessage(plugin.msg("no-permission")); return; }
        player.sendMessage(plugin.color("&6Regions"));
        if (house.getRegions().isEmpty()) {
            player.sendMessage(plugin.color("&7No regions created."));
            return;
        }
        for (House.Region region : house.getRegions().values()) {
            player.sendMessage(plugin.color("&e" + region.getId() + " &7- priority &f" + region.getPriority()));
        }
    }

    private void edit(Player player, String[] args) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || !canEditRegions(player, house)) { player.sendMessage(plugin.msg("no-permission")); return; }
        if (args.length < 3) { player.sendMessage(plugin.color("&cUsage: /housing region edit <name>")); return; }
        House.Region region = house.getRegion(args[2]);
        if (region == null) { player.sendMessage(plugin.color("&cUnknown region.")); return; }
        openEditor(player, region);
    }

    private void updateRegion(Player player) {
        if (player == null || !player.isOnline()) return;
        House house = houses.getHouseByWorld(player.getWorld().getName());
        House.Region region = house == null ? null : house.getRegionAt(player.getLocation());
        String next = house == null || region == null ? null : house.getWorldName() + ":" + region.getId();
        String current = currentRegion.get(player.getUniqueId());
        if (Objects.equals(next, current)) return;
        leaveCurrent(player, house);
        if (region != null) {
            currentRegion.put(player.getUniqueId(), next);
            applyRegion(player, house, region);
            for (House.ButtonAction action : region.getEnterActions()) runAction(player, house, action);
        }
    }

    private void leaveCurrent(Player player, House fallbackHouse) {
        String key = currentRegion.remove(player.getUniqueId());
        if (key == null) return;
        String[] split = key.split(":", 2);
        House house = fallbackHouse == null ? houses.getHouseByWorld(split[0]) : fallbackHouse;
        House.Region region = house == null || split.length < 2 ? null : house.getRegion(split[1]);
        if (house != null && region != null) {
            for (House.ButtonAction action : region.getLeaveActions()) runAction(player, house, action);
        }
        GameMode previous = previousGameModes.remove(player.getUniqueId());
        if (previous != null && player.isOnline()) player.setGameMode(previous);
        if (house != null && player.isOnline()) {
            player.setAllowFlight(house.isFlyEnabled());
            if (!house.isFlyEnabled()) player.setFlying(false);
        }
    }

    private void applyRegion(Player player, House house, House.Region region) {
        if (!region.isFly()) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        if (!region.getGameMode().isEmpty()) {
            try {
                previousGameModes.putIfAbsent(player.getUniqueId(), player.getGameMode());
                player.setGameMode(GameMode.valueOf(region.getGameMode()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        applyPotionEffects(player, region.getPotionEffects());
        if (region.isApplyPvpKit()) houses.applyPvpLayout(player, house);
    }

    private void runWhileInsideActions() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            House.Region region = house == null ? null : house.getRegionAt(player.getLocation());
            if (house == null || region == null) continue;
            if (!region.isFly()) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
            if (region.getWhileInsideActions().isEmpty()) continue;
            String key = house.getWorldName() + ":" + region.getId() + ":" + player.getUniqueId();
            if (whileCooldowns.getOrDefault(key, 0L) > now) continue;
            whileCooldowns.put(key, now + 2000L);
            for (House.ButtonAction action : region.getWhileInsideActions()) runAction(player, house, action);
        }
    }

    private void runAction(Player player, House house, House.ButtonAction action) {
        String type = action.getType();
        String value = action.getValue();
        if ("MESSAGE".equals(type)) player.sendMessage(plugin.color(value.isEmpty() ? "&aRegion action!" : value));
        else if ("TITLE".equals(type)) {
            String[] title = (value.isEmpty() ? "&aRegion" : value).split("\\n", 2);
            sendTitle(player, title[0], title.length > 1 ? title[1] : "");
        }
        else if ("ACTION_BAR".equals(type)) sendActionBar(player, value.isEmpty() ? "&aRegion" : value);
        else if ("TELEPORT".equals(type)) {
            Location target = parseLocation(player.getWorld(), value);
            if (target != null && target.getWorld() != null && house.getWorldName().equals(target.getWorld().getName())) player.teleport(target);
        } else if ("PLAY_SOUND".equals(type)) playSound(player, value.isEmpty() ? "NOTE_PLING" : value);
        else if ("GIVE_ITEM".equals(type)) giveItem(player, value);
        else if ("POTION_EFFECT".equals(type)) applyPotionEffects(player, value);
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

    private House.Region topRegion(House house, Location location) {
        return house == null ? null : house.getRegionAt(location);
    }

    private void openEditor(Player player, House.Region region) {
        Inventory inv = Bukkit.createInventory(null, 36, editorTitle);
        inv.setItem(4, named(net.fakepixel.housing.util.Compat.mat("MAP"), "&a" + region.getName(), "&7ID: &f" + region.getId(), "&7Priority: &f" + region.getPriority()));
        inv.setItem(10, named(net.fakepixel.housing.util.Compat.mat("OAK_SIGN"), "&aRename Region"));
        inv.setItem(11, named(net.fakepixel.housing.util.Compat.mat("REDSTONE"), "&aChange Priority", "&7Higher priority wins overlaps."));
        inv.setItem(12, named(net.fakepixel.housing.util.Compat.mat("BOOK"), "&aEdit Rules"));
        inv.setItem(13, named(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aEdit Enter Actions"));
        inv.setItem(14, named(net.fakepixel.housing.util.Compat.mat("REDSTONE"), "&aEdit Leave Actions"));
        inv.setItem(15, named(net.fakepixel.housing.util.Compat.mat("WATCH"), "&aEdit While Inside Actions"));
        inv.setItem(19, toggle("&aPvP", region.isPvp()));
        inv.setItem(20, toggle("&aBuild", region.isBuild()));
        inv.setItem(21, toggle("&aFly", region.isFly()));
        inv.setItem(22, toggle("&aDamage", region.isDamage()));
        inv.setItem(23, named(net.fakepixel.housing.util.Compat.mat("TNT"), "&cDelete Region"));
        inv.setItem(35, token(region, ""));
        player.openInventory(inv);
    }

    private void openRules(Player player, House.Region region) {
        Inventory inv = Bukkit.createInventory(null, 36, rulesTitle);
        inv.setItem(10, toggle("&aPvP", region.isPvp()));
        inv.setItem(11, toggle("&aBuild", region.isBuild()));
        inv.setItem(12, toggle("&aBlock Breaking", region.isBlockBreaking()));
        inv.setItem(13, toggle("&aFly", region.isFly()));
        inv.setItem(14, toggle("&aDamage", region.isDamage()));
        inv.setItem(15, toggle("&aFall Damage", region.isFallDamage()));
        inv.setItem(16, toggle("&aHunger", region.isHunger()));
        inv.setItem(19, named(net.fakepixel.housing.util.Compat.mat("POTION"), "&aPotion Effects", "&7Current: &f" + empty(region.getPotionEffects())));
        inv.setItem(20, named(net.fakepixel.housing.util.Compat.mat("DIAMOND_SWORD"), "&aGamemode Override", "&7Current: &f" + empty(region.getGameMode())));
        inv.setItem(21, toggle("&aApply PvP Kit", region.isApplyPvpKit()));
        inv.setItem(22, toggle("&aAllow Projectiles", region.isAllowProjectiles()));
        inv.setItem(23, toggle("&aAllow Item Drops", region.isAllowItemDrops()));
        inv.setItem(24, toggle("&aAllow Item Pickup", region.isAllowItemPickup()));
        inv.setItem(31, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(35, token(region, ""));
        player.openInventory(inv);
    }

    private void openActions(Player player, House.Region region, String set) {
        List<House.ButtonAction> actions = actionList(region, set);
        Inventory inv = Bukkit.createInventory(null, 36, actionsTitle);
        inv.setItem(4, named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&a" + prettySet(set) + " Actions", "&7Actions: &f" + actions.size()));
        inv.setItem(10, named(net.fakepixel.housing.util.Compat.mat("EMERALD"), "&aAdd Action"));
        inv.setItem(11, named(net.fakepixel.housing.util.Compat.mat("BARRIER"), "&cRemove Action", "&7Removes the last action."));
        inv.setItem(12, named(net.fakepixel.housing.util.Compat.mat("BOOK_AND_QUILL"), "&aEdit Action", "&7Edits the last action value."));
        inv.setItem(13, named(net.fakepixel.housing.util.Compat.mat("HOPPER"), "&aReorder Actions", "&7Moves the last action to the top."));
        inv.setItem(31, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(35, token(region, set));
        player.openInventory(inv);
    }

    private void openAddAction(Player player, House.Region region, String set) {
        Inventory inv = Bukkit.createInventory(null, 27, addActionTitle);
        int[] slots = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        for (int i = 0; i < TYPES.length; i++) inv.setItem(slots[i], named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&a" + TYPES[i]));
        inv.setItem(24, named(net.fakepixel.housing.util.Compat.mat("ARROW"), "&aBack"));
        inv.setItem(26, token(region, set));
        player.openInventory(inv);
    }

    private void beginPrompt(Player player, House house, House.Region region, String field, String actionSet) {
        prompts.put(player.getUniqueId(), new Prompt(house.getWorldName(), region.getId(), field, actionSet));
        player.closeInventory();
        player.sendMessage(plugin.color("&aType the new region " + field + " in chat. Type &ccancel &ato cancel."));
    }

    private void handlePrompt(Player player, Prompt prompt, String message) {
        House house = houses.getHouseByWorld(prompt.worldName);
        House.Region region = house == null ? null : house.getRegion(prompt.regionId);
        if (house == null || region == null || !canEditRegions(player, house)) {
            player.sendMessage(plugin.color("&cYou cannot edit that region."));
            return;
        }
        if ("cancel".equalsIgnoreCase(message.trim())) {
            player.sendMessage(plugin.color("&cRegion edit cancelled."));
            openEditor(player, region);
            return;
        }
        if ("name".equals(prompt.field)) region.setName(message.trim());
        else if ("priority".equals(prompt.field)) {
            try { region.setPriority(Integer.parseInt(message.trim())); } catch (NumberFormatException ignored) {}
        } else if ("effects".equals(prompt.field)) region.setPotionEffects(message.trim());
        else if ("gamemode".equals(prompt.field)) region.setGameMode(message.trim());
        else if ("action".equals(prompt.field)) {
            List<House.ButtonAction> actions = actionList(region, prompt.actionSet);
            if (!actions.isEmpty()) actions.get(actions.size() - 1).setValue(message.trim());
        }
        houses.saveAll();
        player.sendMessage(plugin.color("&aSaved region " + prompt.field + "."));
        openEditor(player, region);
    }

    private void saveEdit(Player player, House.Region region) {
        houses.saveAll();
        openEditor(player, region);
    }

    private List<House.ButtonAction> actionList(House.Region region, String set) {
        if ("leave".equals(set)) return region.getLeaveActions();
        if ("while".equals(set)) return region.getWhileInsideActions();
        return region.getEnterActions();
    }

    private House.ButtonAction defaultAction(String type, Player player) {
        if ("MESSAGE".equals(type)) return new House.ButtonAction(type, "&aRegion action!");
        if ("TITLE".equals(type)) return new House.ButtonAction(type, "&aRegion");
        if ("ACTION_BAR".equals(type)) return new House.ButtonAction(type, "&aRegion action!");
        if ("TELEPORT".equals(type)) return new House.ButtonAction(type, serialize(player.getLocation()));
        if ("PLAY_SOUND".equals(type)) return new House.ButtonAction(type, "NOTE_PLING");
        if ("GIVE_ITEM".equals(type)) return new House.ButtonAction(type, "STONE:1");
        if ("POTION_EFFECT".equals(type)) return new House.ButtonAction(type, "SPEED:100:1");
        if ("CHANGE_TIME".equals(type)) return new House.ButtonAction(type, "6000");
        if ("CHANGE_WEATHER".equals(type)) return new House.ButtonAction(type, "sunny");
        return new House.ButtonAction(type, "");
    }

    private void applyPotionEffects(Player player, String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        String[] entries = raw.split(",");
        for (String entry : entries) {
            String[] split = entry.trim().split(":");
            PotionEffectType type = PotionEffectType.getByName(split.length > 0 ? split[0] : "SPEED");
            int duration = 100;
            int amplifier = 0;
            if (split.length > 1) try { duration = Integer.parseInt(split[1]); } catch (Exception ignored) {}
            if (split.length > 2) try { amplifier = Integer.parseInt(split[2]); } catch (Exception ignored) {}
            if (type != null) player.addPotionEffect(new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier)));
        }
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
            Object packet = constructor.newInstance(Enum.valueOf((Class<Enum>) titleAction, "TITLE"), titleComponent);
            sendPacket(player, packet);
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

    private ItemStack toggle(String name, boolean enabled) {
        return named(enabled ? net.fakepixel.housing.util.Compat.limeDye() : net.fakepixel.housing.util.Compat.grayDye(), name + ": " + (enabled ? "&aEnabled" : "&cDisabled"));
    }

    private ItemStack token(House.Region region, String set) {
        return named(net.fakepixel.housing.util.Compat.mat("PAPER"), "&7Region Token", "&8Region ID: " + region.getId(), "&8Action Set: " + (set == null ? "" : set));
    }

    private Token token(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return null;
        String region = "";
        String set = "";
        for (String line : item.getItemMeta().getLore()) {
            String clean = ChatColor.stripColor(line);
            if (clean.startsWith("Region ID: ")) region = clean.substring("Region ID: ".length());
            if (clean.startsWith("Action Set: ")) set = clean.substring("Action Set: ".length());
        }
        return region.isEmpty() ? null : new Token(region, set);
    }

    private String typeFromSlot(int slot) {
        int[] slots = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22};
        for (int i = 0; i < slots.length; i++) if (slots[i] == slot) return TYPES[i];
        return null;
    }

    private String prettySet(String set) {
        if ("leave".equals(set)) return "Leave";
        if ("while".equals(set)) return "While Inside";
        return "Enter";
    }

    private String empty(String raw) {
        return raw == null || raw.isEmpty() ? "none" : raw;
    }

    private String escape(String text) {
        return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return false;
        return a.getWorld().equals(b.getWorld()) && a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ();
    }

    private void help(Player player) {
        player.sendMessage(plugin.color("&cUsage: /housing region <wand|create|delete|list|edit|pos1|pos2>"));
    }

    private static class Selection {
        private final String worldName;
        private Location pos1;
        private Location pos2;
        private Selection(String worldName) { this.worldName = worldName; }
    }

    private static class Prompt {
        private final String worldName;
        private final String regionId;
        private final String field;
        private final String actionSet;
        private Prompt(String worldName, String regionId, String field, String actionSet) {
            this.worldName = worldName;
            this.regionId = regionId;
            this.field = field;
            this.actionSet = actionSet;
        }
    }

    private static class Token {
        private final String regionId;
        private final String actionSet;
        private Token(String regionId, String actionSet) {
            this.regionId = regionId;
            this.actionSet = actionSet == null ? "" : actionSet;
        }
    }
}
