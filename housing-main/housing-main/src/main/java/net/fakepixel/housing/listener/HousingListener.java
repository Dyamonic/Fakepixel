package net.fakepixel.housing.listener;

import net.fakepixel.housing.HousingPlugin;
import net.fakepixel.housing.hook.AbelixHook;
import net.fakepixel.housing.house.House;
import net.fakepixel.housing.house.HouseManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HousingListener implements Listener {
    private final HousingPlugin plugin;
    private final HouseManager houses;
    private final AbelixHook abelix;
    private final Map<UUID, Long> teleportPadCooldowns = new HashMap<>();
    private final Map<UUID, String> pvpDeathHouses = new HashMap<>();
    private final Map<UUID, String> parkourDeathHouses = new HashMap<>();

    public HousingListener(HousingPlugin plugin, HouseManager houses, AbelixHook abelix) {
        this.plugin = plugin;
        this.houses = houses;
        this.abelix = abelix;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (plugin.getNametagService() != null) {
            plugin.getNametagService().update(player);
        } else {
            player.setDisplayName(plugin.applyPlaceholders(player, plugin.getStaticConfig().getString("display-names.display-name-format", "%abelix_prefix%%player%")));
            String tab = plugin.applyPlaceholders(player, plugin.getStaticConfig().getString("display-names.tablist-format", "%abelix_prefix%%player%"));
            try {
                player.setPlayerListName(tab);
            } catch (IllegalArgumentException ignored) {
                player.setPlayerListName(player.getName());
            }
        }
        if (plugin.getTablistService() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getTablistService().update(player), 5L);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld() == null) return;
        if (houses.getHouseByWorld(event.getLocation().getWorld().getName()) == null) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house != null) {
            event.setCancelled(true);
            if (!house.hasGroupPermission(player.getUniqueId(), "chat") && !house.canManage(player.getUniqueId()) && !houses.isStaff(player)) {
                player.sendMessage(plugin.color("&cYour group cannot chat in this house."));
                return;
            }
            if (house.isMuted(player.getUniqueId()) && !house.canManage(player.getUniqueId()) && !houses.isStaff(player)) {
                player.sendMessage(plugin.color("&cYou are muted in this house."));
                return;
            }
            String formatted = housingChatMessage(player, house, event.getMessage());
            for (Player recipient : player.getWorld().getPlayers()) {
                if (recipient.getWorld().getName().equals(house.getWorldName())) recipient.sendMessage(formatted);
            }
            return;
        }
        if (houses.isLobbyWorld(player.getWorld().getName())) {
            event.setCancelled(true);
            String formatted = lobbyChatMessage(player, event.getMessage());
            for (Player recipient : player.getWorld().getPlayers()) {
                if (houses.isLobbyWorld(recipient.getWorld().getName())) recipient.sendMessage(formatted);
            }
        }
    }

    private String housingChatMessage(Player player, House house, String message) {
        String plus = player.hasPermission("housing.plus") ? "&6+ " : "";
        String housingPrefix = house.groupSuffix(house.getGroup(player.getUniqueId()));
        String rankPrefix = abelix == null ? "" : abelix.getPrefix(player);
        return plugin.color(plus + housingPrefix + " " + rankPrefix + player.getName() + "&f: " + message);
    }

    private String lobbyChatMessage(Player player, String message) {
        String plus = player.hasPermission("housing.plus") ? "&6+ " : "";
        String rankPrefix = abelix == null ? "" : abelix.getPrefix(player);
        return plugin.color(plus + rankPrefix + player.getName() + "&f: " + message);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        boolean namedItem = event.getItem() != null && event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasDisplayName();
        String name = namedItem ? event.getItem().getItemMeta().getDisplayName() : "";

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && plugin.getHousingMenu() != null
                && plugin.getHousingMenu().isParkourLeaderboardItem(event.getItem())) {
            event.setCancelled(true);
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) {
                player.sendMessage(plugin.color("&cYou can only place this inside a player house."));
                return;
            }
            if (!canEditParkourPads(player, house)) {
                player.sendMessage(plugin.color("&cOnly groups with build permission can place Parkour Leaderboards."));
                return;
            }
            if (plugin.getParkourService() == null) return;
            if (player.isSneaking()) {
                plugin.getParkourService().removeLeaderboard(house, true);
                player.sendMessage(plugin.color("&cRemoved the Parkour Leaderboard."));
                return;
            }
            Location location = event.getClickedBlock().getLocation().add(0.5D, 1.8D, 0.5D);
            plugin.getParkourService().placeLeaderboard(player, house, location);
            consumeOne(player);
            return;
        }

        if (plugin.getSelectionService().isWand(event.getItem())) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) return;
            if (plugin.getProToolsService() == null || !plugin.getProToolsService().canUse(player, house)) {
                if (shouldCancelBlockInteraction(player)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.color("&cYou do not have permission to use Pro Tools in this house."));
                }
                return;
            }
            Block clicked = event.getClickedBlock();
            if (clicked == null) return;
            event.setCancelled(true);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) plugin.getSelectionService().setPos1(player, clicked.getLocation());
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) plugin.getSelectionService().setPos2(player, clicked.getLocation());
            return;
        }

        if (plugin.getRegionService() != null && plugin.getRegionService().isRegionWand(event.getItem())) {
            return;
        }

        if (namedItem && event.getItem().getType() == net.fakepixel.housing.util.Compat.mat("NETHER_STAR") && name.contains("Housing Menu")) {
            event.setCancelled(true);
            if (plugin.getHousingMenu().isThemeApplying(player)) {
                player.sendMessage(plugin.color("&eYour theme is still applying. Please wait..."));
                return;
            }
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null || (!house.canManage(player.getUniqueId()) && !house.hasGroupPermission(player.getUniqueId(), "settings"))) {
                player.sendMessage(plugin.color("&cOnly the owner/co-owner or groups with settings permission can open the housing menu."));
                return;
            }
            plugin.getHousingMenu().openOwnerMenu(player);
            return;
        }

        if (namedItem && event.getItem().getType().name().contains("DOOR") && (name.contains("Housing Menu") || name.contains("Guest Menu") || name.contains("Players"))) {
            event.setCancelled(true);
            plugin.getHousingMenu().openGuestMenu(player);
            return;
        }

        if (namedItem && event.getItem().getType() == net.fakepixel.housing.util.Compat.mat("GOLD_NUGGET") && name.contains("Housing Staff Menu")) {
            event.setCancelled(true);
            plugin.getStaffMenu().open(player);
            return;
        }

        if (namedItem && event.getItem().getType() == net.fakepixel.housing.util.Compat.mat("COOKIE") && name.contains("ADMIN Cookies")) {
            event.setCancelled(true);
            if (!player.hasPermission("housing.staff.award") && !player.hasPermission("housing.staff.admin") && !player.hasPermission("housing.admin")) {
                player.sendMessage(plugin.msg("no-permission"));
                return;
            }
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) {
                player.sendMessage(plugin.color("&cYou are not inside a player house."));
                return;
            }
            houses.giveAdminCookies(player, house);
            return;
        }

        if (namedItem && event.getItem().getType() == net.fakepixel.housing.util.Compat.mat("COOKIE") && name.contains("Give Cookie")) {
            event.setCancelled(true);
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house == null) {
                player.sendMessage(plugin.color("&cYou are not inside a player house."));
                return;
            }
            if (houses.giveCookie(player, house)) {
                player.sendMessage(plugin.color("&aYou gave &e" + house.getName() + " &aa cookie!"));
            } else {
                player.sendMessage(plugin.color("&cYou already gave this house a cookie."));
            }
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house != null && shouldCancelBlockInteraction(player)) {
                event.setCancelled(true);
                sendBuildDenied(player, house);
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player)) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInHand();
        if (!isHousingMenuItem(hand) || hand.getType() != net.fakepixel.housing.util.Compat.mat("NETHER_STAR")) return;
        event.setCancelled(true);
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || (!house.canManage(player.getUniqueId()) && !house.hasGroupPermission(player.getUniqueId(), "settings"))) {
            player.sendMessage(plugin.color("&cOnly the owner/co-owner or groups with settings permission can open the housing menu."));
            return;
        }
        plugin.getHousingMenu().openOwnerMenu(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        final Player player = (Player) event.getWhoClicked();
        if (isLockedMenuSlot(player, event) || isHousingMenuItem(event.getCurrentItem()) || isHousingMenuItem(event.getCursor())
                || houses.isLockedLobbyItem(event.getCurrentItem()) || houses.isLockedLobbyItem(event.getCursor())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> refreshLockedItems(player));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        final Player player = (Player) event.getWhoClicked();
        int ownerSlot = plugin.getStaticConfig().getInt("owner-menu.item-slot", 8);
        int guestSlot = plugin.getStaticConfig().getInt("guest-menu.item-slot", 8);
        int topSize = event.getView().getTopInventory().getSize();
        boolean houseSlot = houses.getHouseByWorld(player.getWorld().getName()) != null
                && (event.getRawSlots().contains(topSize + ownerSlot) || event.getRawSlots().contains(topSize + guestSlot));
        if (houseSlot
                || containsLobbySlot(event, topSize) || isHousingMenuItem(event.getOldCursor()) || houses.isLockedLobbyItem(event.getOldCursor())) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> refreshLockedItems(player));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!isHousingMenuItem(event.getItemDrop().getItemStack()) && !houses.isLockedLobbyItem(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> refreshLockedItems(player));
    }

    @EventHandler
    public void onRegionDrop(PlayerDropItemEvent event) {
        House house = houses.getHouseByWorld(event.getPlayer().getWorld().getName());
        if (house == null || plugin.getRegionService() == null) return;
        if (!plugin.getRegionService().allowsItemDrops(house, event.getPlayer().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        House house = houses.getHouseByWorld(event.getPlayer().getWorld().getName());
        if (house == null || plugin.getRegionService() == null) return;
        if (!plugin.getRegionService().allowsItemPickup(house, event.getPlayer().getLocation())) event.setCancelled(true);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        House fromHouse = event.getFrom() == null ? null : houses.getHouseByWorld(event.getFrom().getName());
        if (fromHouse != null && plugin.getJukeboxService() != null) plugin.getJukeboxService().stopHouseMusic(event.getPlayer());
        houses.updateVisitItems(event.getPlayer());
        houses.enforceVisitorState(event.getPlayer());
        House currentHouse = houses.getHouseByWorld(event.getPlayer().getWorld().getName());
        if (currentHouse != null) houses.refreshBorderVisibility(event.getPlayer(), currentHouse);
        if (plugin.getHousingScoreboard() != null) plugin.getHousingScoreboard().update(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (!player.isOnline()) return;
            House house = houses.getHouseByWorld(player.getWorld().getName());
            if (house != null && house.isPvpEnabled()) houses.applyPvpLayout(player, house);
            if (house != null && plugin.getJukeboxService() != null) plugin.getJukeboxService().playHouseMusic(player, house);
            if (plugin.getHousingScoreboard() != null) plugin.getHousingScoreboard().update(player);
        }, 2L);
        if (plugin.getNametagService() != null) plugin.getNametagService().update(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (sameBlock(event.getFrom(), event.getTo())) return;
        Player player = event.getPlayer();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) return;
        House.TeleportPad pad = teleportPadAt(house, event.getTo());
        if (pad == null || pad.getLinkedPadId() == null || pad.getLinkedPadId().trim().isEmpty()) return;
        long now = System.currentTimeMillis();
        Long last = teleportPadCooldowns.get(player.getUniqueId());
        if (last != null && now - last < 2000L) return;
        House.TeleportPad linked = house.getTeleportPad(pad.getLinkedPadId());
        if (linked == null || !pad.getId().equals(linked.getLinkedPadId())) return;
        Location target = teleportPadLocation(house, linked);
        if (target == null) return;
        teleportPadCooldowns.put(player.getUniqueId(), now);
        if (pad.isParticles()) {
            player.getWorld().playEffect(player.getLocation(), Effect.ENDER_SIGNAL, 0);
            target.getWorld().playEffect(target, Effect.ENDER_SIGNAL, 0);
        }
        if (pad.isSound()) {
            player.getWorld().playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
        player.teleport(target);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (!houses.isVisitingOtherHouse(player) || event.getNewGameMode() == GameMode.SURVIVAL) return;
        if (isAbelixStaff(player) && player.hasPermission("housing.staff.gamemode")) return;
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && houses.isVisitingOtherHouse(player)) player.setGameMode(GameMode.SURVIVAL);
        });
        player.sendMessage(plugin.color("&cYou cannot switch gamemodes in another player's house."));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getJukeboxService() != null) plugin.getJukeboxService().stopHouseMusic(event.getPlayer());
        houses.clearVisitItems(event.getPlayer());
        teleportPadCooldowns.remove(event.getPlayer().getUniqueId());
        pvpDeathHouses.remove(event.getPlayer().getUniqueId());
        parkourDeathHouses.remove(event.getPlayer().getUniqueId());
        if (plugin.getNametagService() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getNametagService().updateAll(), 1L);
        }
    }


    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (houses.isLobbyWorld(player.getWorld().getName())) {
            event.setCancelled(true);
            player.setFireTicks(0);
            player.setFallDistance(0.0F);
            return;
        }
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) return;
        if (plugin.getRegionService() != null && !plugin.getRegionService().allowsDamage(house, player.getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && plugin.getRegionService() != null && !plugin.getRegionService().allowsFallDamage(house, player.getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && plugin.getRegionService() == null && !house.isFallDamageEnabled()) {
            event.setCancelled(true);
            return;
        }
        if (!house.isFireDamageEnabled() && isFireOrLavaDamage(event.getCause())) {
            event.setCancelled(true);
            return;
        }
        if (!house.isPvpEnabled() && isPotionDamage(event.getCause())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (houses.isLobbyWorld(player.getWorld().getName())) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            return;
        }
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) return;
        boolean hunger = plugin.getRegionService() == null ? house.isHungerEnabled() : plugin.getRegionService().allowsHunger(house, player.getLocation());
        if (hunger) return;
        event.setCancelled(true);
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) return;
        pvpDeathHouses.put(player.getUniqueId(), house.getWorldName());
        if (!house.isKillDeathMessagesEnabled()) event.setDeathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        if (plugin.getParkourService() != null && plugin.getParkourService().isRunning(player, house)) {
            parkourDeathHouses.put(player.getUniqueId(), house.getWorldName());
        }
        if (house.isPvpEnabled()) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
        if (house.isInstantRespawnEnabled()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> respawnNow(player), 1L);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final String deathHouseWorld = pvpDeathHouses.remove(player.getUniqueId());
        final String parkourHouseWorld = parkourDeathHouses.remove(player.getUniqueId());
        final House deathHouse = deathHouseWorld == null ? null : houses.getHouseByWorld(deathHouseWorld);
        if (deathHouse != null) {
            Location respawn = parkourRespawnLocation(player, deathHouse, parkourHouseWorld);
            if (respawn == null) respawn = houses.spawnLocation(deathHouse, Bukkit.getWorld(deathHouse.getWorldName()), !deathHouse.canManage(player.getUniqueId()));
            if (respawn != null) event.setRespawnLocation(respawn);
        } else {
            Location lobby = houses.lobbySpawnLocation();
            if (lobby != null) event.setRespawnLocation(lobby);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            House house = deathHouse != null ? deathHouse : houses.getHouseByWorld(player.getWorld().getName());
            if (house != null) {
                Location safe = parkourRespawnLocation(player, house, parkourHouseWorld);
                if (safe == null) safe = houses.spawnLocation(house, Bukkit.getWorld(house.getWorldName()), !house.canManage(player.getUniqueId()));
                if (safe != null && !player.getWorld().getName().equals(house.getWorldName())) player.teleport(safe);
                else if (safe != null && parkourHouseWorld != null) player.teleport(safe);
                if (house.isPvpEnabled()) houses.applyPvpLayout(player, house);
                else houses.updateVisitItems(player);
                houses.ensureHouseMenuItem(player);
            }
        }, 2L);
    }

    private Location parkourRespawnLocation(Player player, House house, String parkourHouseWorld) {
        if (player == null || house == null || parkourHouseWorld == null || !parkourHouseWorld.equals(house.getWorldName())) return null;
        return plugin.getParkourService() == null ? null : plugin.getParkourService().deathRespawnLocation(player, house);
    }



    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!plugin.getStaticConfig().getBoolean("protection.disable-explosions-in-houses", true)) return;
        if (event.getLocation() == null || event.getLocation().getWorld() == null) return;
        if (houses.getHouseByWorld(event.getLocation().getWorld().getName()) == null) return;
        event.blockList().clear();
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.getStaticConfig().getBoolean("protection.disable-explosions-in-houses", true)) return;
        if (event.getBlock() == null || event.getBlock().getWorld() == null) return;
        if (houses.getHouseByWorld(event.getBlock().getWorld().getName()) == null) return;
        event.blockList().clear();
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!plugin.getStaticConfig().getBoolean("protection.disable-fire-spread-in-houses", true)) return;
        if (event.getBlock() == null || event.getBlock().getWorld() == null) return;
        if (houses.getHouseByWorld(event.getBlock().getWorld().getName()) == null) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!plugin.getStaticConfig().getBoolean("protection.disable-fire-spread-in-houses", true)) return;
        if (event.getBlock() == null || event.getBlock().getWorld() == null) return;
        if (houses.getHouseByWorld(event.getBlock().getWorld().getName()) == null) return;
        Material sourceType = event.getSource() == null ? net.fakepixel.housing.util.Compat.mat("AIR") : event.getSource().getType();
        if (sourceType == net.fakepixel.housing.util.Compat.mat("FIRE") || sourceType == net.fakepixel.housing.util.Compat.mat("SOUL_FIRE")) event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!plugin.getStaticConfig().getBoolean("protection.disable-fire-spread-in-houses", true)) return;
        if (event.getBlock() == null || event.getBlock().getWorld() == null) return;
        if (houses.getHouseByWorld(event.getBlock().getWorld().getName()) == null) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!plugin.getStaticConfig().getBoolean("protection.disable-lava-flow-in-houses", true)) return;
        if (event.getBlock() == null || event.getBlock().getWorld() == null) return;
        if (houses.getHouseByWorld(event.getBlock().getWorld().getName()) == null) return;
        if (event.getBlock().getType() == net.fakepixel.housing.util.Compat.mat("LAVA")) event.setCancelled(true);
    }

    private boolean isFireOrLavaDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA;
    }

    private boolean isPotionDamage(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.MAGIC
                || cause == EntityDamageEvent.DamageCause.POISON
                || cause == EntityDamageEvent.DamageCause.WITHER;
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player damager = playerDamager(event.getDamager());
        if (houses.isLobbyWorld(victim.getWorld().getName()) || (damager != null && houses.isLobbyWorld(damager.getWorld().getName()))) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof Player && damager != null && isHousingMenuItem(damager.getInventory().getItemInHand())) {
            House house = houses.getHouseByWorld(damager.getWorld().getName());
            if (house != null && victim.getWorld().getName().equals(house.getWorldName())) {
                event.setCancelled(true);
                if (plugin.getHousingMenu().canOpenPlayerOptions(damager, house)) {
                    plugin.getHousingMenu().openPlayerManageMenu(damager, victim);
                } else {
                    damager.sendMessage(plugin.msg("no-permission"));
                }
                return;
            }
        }
        House house = houses.getHouseByWorld(victim.getWorld().getName());
        if (house == null) return;
        if (damager == null) return;
        if (!victim.getWorld().equals(damager.getWorld())
                || !house.isPvpEnabled()
                || !houses.isInsidePlotBounds(victim.getLocation())
                || !houses.isInsidePlotBounds(damager.getLocation())) {
            event.setCancelled(true);
            return;
        }
        boolean pvp = plugin.getRegionService() == null
                || (plugin.getRegionService().allowsPvp(house, victim.getLocation())
                && plugin.getRegionService().allowsPvp(house, damager.getLocation()));
        boolean projectiles = plugin.getRegionService() == null || plugin.getRegionService().allowsProjectiles(house, victim.getLocation());
        if (!pvp || (!projectiles && isProjectileOrPotion(event.getDamager()))) {
            event.setCancelled(true);
        }
    }

    private Player playerDamager(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof org.bukkit.entity.Projectile) {
            Object shooter = ((org.bukkit.entity.Projectile) entity).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player player = (Player) event.getEntity().getShooter();
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || plugin.getRegionService() == null) return;
        if (!plugin.getRegionService().allowsProjectiles(house, player.getLocation())) event.setCancelled(true);
    }

    private boolean isProjectileOrPotion(org.bukkit.entity.Entity entity) {
        if (entity == null) return false;
        if (entity instanceof org.bukkit.entity.Projectile) return true;
        String name = entity.getType().name();
        return name.contains("POTION") || entity.getClass().getName().toLowerCase(java.util.Locale.ENGLISH).contains("potion");
    }

    private void respawnNow(Player player) {
        if (player == null || !player.isOnline()) return;
        try {
            Object spigot = player.getClass().getMethod("spigot").invoke(player);
            spigot.getClass().getMethod("respawn").invoke(spigot);
        } catch (Throwable ignored) {
        }
    }

    private House.TeleportPad teleportPadAt(House house, Location location) {
        if (house == null || location == null) return null;
        House.TeleportPad pad = house.getTeleportPadAt(location);
        if (pad != null) return pad;
        return house.getTeleportPadAt(location.clone().subtract(0, 1, 0));
    }

    private Location teleportPadLocation(House house, House.TeleportPad pad) {
        if (house == null || pad == null) return null;
        org.bukkit.World world = Bukkit.getWorld(house.getWorldName());
        if (world == null) return null;
        String[] split = pad.getLocation().split(",");
        if (split.length != 3) return null;
        try {
            int x = Integer.parseInt(split[0]);
            int y = Integer.parseInt(split[1]);
            int z = Integer.parseInt(split[2]);
            Material type = world.getBlockAt(x, y, z).getType();
            double yOffset = type.name().contains("PLATE") ? 0.2D : 1.0D;
            return new Location(world, x + 0.5D, y + yOffset, z + 0.5D);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) return false;
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (plugin.getCurrentServer() == null || !plugin.getCurrentServer().isInstance()) { event.setCancelled(true); return; }
        if (shouldCancelLobbyBuild(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        if (shouldCancelBuildHeight(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (shouldCancelBorder(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        House house = houses.getHouseByWorld(event.getBlock().getWorld().getName());
        if (house != null && shouldCancelBuild(event.getPlayer())) {
            event.setCancelled(true);
            sendBuildDenied(event.getPlayer(), house);
            return;
        }
        if (house != null && house.parkourPadTypeAt(event.getBlock().getLocation()) != null) {
            if (!canEditParkourPads(event.getPlayer(), house)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.color("&cOnly groups with build permission can remove parkour pads."));
                return;
            }
            String padKey = house.parkourPadKeyAt(event.getBlock().getLocation());
            house.removeParkourPad(event.getBlock().getLocation());
            house.markPlayerEditedBlock(event.getBlock().getLocation());
            removeParkourPadHologram(house, padKey);
            renumberParkourCheckpointHolograms(house, event.getBlock().getWorld());
            suppressBreakDrops(event);
            houses.markDirty(house);
            event.getPlayer().sendMessage(plugin.color("&cRemoved a Parkour pad from this house."));
            return;
        }
        if (house != null && house.getTeleportPadAt(event.getBlock().getLocation()) != null) {
            if (!plugin.canEditHousingObjects(event.getPlayer(), house)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.color("&cOnly groups with build permission can remove teleport pads."));
                return;
            }
            house.removeTeleportPad(event.getBlock().getLocation());
            house.markPlayerEditedBlock(event.getBlock().getLocation());
            suppressBreakDrops(event);
            houses.markDirty(house);
            event.getPlayer().sendMessage(plugin.color("&cRemoved a Teleport Pad from this house."));
            return;
        }
        if (house != null && house.getActionPadAt(event.getBlock().getLocation()) != null) {
            if (plugin.getActionPadService() == null || !plugin.getActionPadService().canEditActionPads(event.getPlayer(), house)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.color("&cOnly groups with build permission can remove Action Pads."));
                return;
            }
            suppressBreakDrops(event);
            houses.markDirty(house);
            return;
        }
        if (house != null && plugin.getRegionService() != null && !plugin.getRegionService().canBreakAt(event.getPlayer(), house, event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.color("&cYou cannot break blocks in this region."));
            return;
        }
        if (house != null) {
            house.markPlayerEditedBlock(event.getBlock().getLocation());
            suppressBreakDrops(event);
            houses.markDirty(house);
        }
    }

    @SuppressWarnings("deprecation")
    private void suppressBreakDrops(BlockBreakEvent event) {
        event.setCancelled(false);
        event.setExpToDrop(0);
        try { BlockBreakEvent.class.getMethod("setDropItems", boolean.class).invoke(event, false); }
        catch (Throwable ignored) { event.setCancelled(true); event.getBlock().setType(net.fakepixel.housing.util.Compat.mat("AIR")); }
    }

    private String prettyParkourType(String type) {
        if ("start".equals(type)) return "Start";
        if ("checkpoint".equals(type)) return "Checkpoint";
        if ("finish".equals(type)) return "Finish";
        return type;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (shouldCancelLobbyBuild(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        if (shouldCancelBuildHeight(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (shouldCancelBorder(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        String parkourType = plugin.getHousingMenu() == null ? null : plugin.getHousingMenu().parkourType(event.getItemInHand());
        boolean teleportPadItem = plugin.getHousingMenu() != null && plugin.getHousingMenu().isTeleportPadItem(event.getItemInHand());
        boolean actionPadItem = plugin.getActionPadService() != null && plugin.getActionPadService().isActionPadItem(event.getItemInHand());
        House house = houses.getHouseByWorld(event.getBlock().getWorld().getName());
        if (house != null && shouldCancelBuild(event.getPlayer())) {
            event.setCancelled(true);
            sendBuildDenied(event.getPlayer(), house);
            return;
        }
        if (parkourType != null && house != null && !canEditParkourPads(event.getPlayer(), house)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.color("&cOnly groups with build permission can place parkour pads."));
            return;
        }
        if (teleportPadItem && house != null && !plugin.canEditHousingObjects(event.getPlayer(), house)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.color("&cOnly groups with build permission can place housing pads."));
            return;
        }
        if (actionPadItem && house != null && !plugin.getActionPadService().canEditActionPads(event.getPlayer(), house)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.color("&cOnly groups with build permission can place Action Pads."));
            return;
        }
        if (parkourType == null && !teleportPadItem && !actionPadItem && house != null && plugin.getRegionService() != null && !plugin.getRegionService().canBuildAt(event.getPlayer(), house, event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.color("&cYou cannot build in this region."));
            return;
        }
        if (parkourType != null) {
            if (house != null) {
                String padKey = house.setParkourPad(parkourType, event.getBlock().getLocation());
                house.markPlayerEditedBlock(event.getBlock().getLocation());
                createParkourPadHologram(house, padKey, event.getBlock().getLocation());
                renumberParkourCheckpointHolograms(house, event.getBlock().getWorld());
                houses.saveAll();
                event.getPlayer().sendMessage(plugin.color("&aSaved &eParkour " + prettyParkourType(parkourType) + " &apad for this house."));
            }
            return;
        }
        if (teleportPadItem) {
            if (house != null) {
                House.TeleportPad pad = house.addTeleportPad(event.getBlock().getLocation());
                house.markPlayerEditedBlock(event.getBlock().getLocation());
                houses.saveAll();
                event.getPlayer().sendMessage(plugin.color("&aSaved &e" + pad.getName() + " &aas a Teleport Pad."));
            }
            return;
        }
        if (actionPadItem) return;
        if (house != null) {
            house.markPlayerEditedBlock(event.getBlock().getLocation());
            houses.saveAll();
        }
    }

    private void createParkourPadHologram(House house, String padKey, Location blockLocation) {
        if (house == null || padKey == null || blockLocation == null || plugin.getHologramService() == null) return;
        removeParkourPadHologram(house, padKey);
        java.util.List<String> lines = parkourHologramLines(house, padKey);
        Location hologramLocation = blockLocation.clone().add(0.5D, 1.25D, 0.5D);
        House.Hologram hologram = house.addHologram(hologramLocation, lines);
        if (hologram == null) return;
        hologram.setIdentity(parkourHologramType(padKey), padKey);
        house.setParkourPadHologram(padKey, hologram.getId());
        plugin.getHologramService().spawnHologram(house, hologram, blockLocation.getWorld());
    }

    private void removeParkourPadHologram(House house, String padKey) {
        if (house == null || padKey == null) return;
        String hologramId = house.removeParkourPadHologram(padKey);
        if (plugin.getHologramService() != null) plugin.getHologramService().deleteHologram(house, hologramId);
        else house.removeHologram(hologramId);
    }

    private void renumberParkourCheckpointHolograms(House house, org.bukkit.World world) {
        if (house == null || world == null || plugin.getHologramService() == null) return;
        for (String padKey : house.getParkourPads().keySet()) {
            if (!padKey.startsWith("checkpoint:")) continue;
            String hologramId = house.getParkourPadHologram(padKey);
            House.Hologram hologram = house.getHologram(hologramId);
            if (hologram == null) continue;
            hologram.setLines(parkourHologramLines(house, padKey));
            hologram.setIdentity(parkourHologramType(padKey), padKey);
            plugin.getHologramService().spawnHologram(house, hologram, world);
        }
    }

    private java.util.List<String> parkourHologramLines(House house, String padKey) {
        if ("start".equals(padKey)) return java.util.Arrays.asList("&a&lParkour Start", "&eStep here to begin!");
        if ("finish".equals(padKey)) return java.util.Arrays.asList("&c&lParkour End", "&eStep here to finish!");
        int number = house == null ? 0 : house.parkourCheckpointNumber(padKey);
        return java.util.Arrays.asList("&e&lCheckpoint #" + Math.max(1, number), "&7Step here to save progress.");
    }

    private String parkourHologramType(String padKey) {
        if ("start".equals(padKey)) return "PARKOUR_START";
        if ("finish".equals(padKey)) return "PARKOUR_END";
        return "PARKOUR_CHECKPOINT";
    }

    private boolean canEditParkourPads(Player player, House house) {
        return plugin.canEditHousingObjects(player, house);
    }

    private void consumeOne(Player player) {
        if (player == null) return;
        ItemStack hand = player.getItemInHand();
        if (hand == null) return;
        if (hand.getAmount() <= 1) player.setItemInHand(null);
        else hand.setAmount(hand.getAmount() - 1);
    }


    private boolean shouldCancelLobbyBuild(Player player) {
        if (!houses.isLobbyWorld(player.getWorld().getName())) return false;
        if (player.hasPermission("housing.admin") || player.hasPermission("housing.staff.build")) return false;
        player.sendMessage(plugin.color("&cYou cannot build in the housing lobby."));
        return true;
    }

    private boolean shouldCancelBuildHeight(Player player, org.bukkit.Location location) {
        if (!plugin.getStaticConfig().getBoolean("border.min-build-y-enabled", true)) return false;
        if (location == null || location.getWorld() == null) return false;
        House house = houses.getHouseByWorld(location.getWorld().getName());
        if (house == null || plugin.isBorderBypass(player)) return false;
        int minBuildY = plugin.getStaticConfig().getInt("border.min-build-y", 90);
        if (location.getBlockY() >= minBuildY) return false;
        player.sendMessage(plugin.color("&cYou cannot build outside this house border."));
        return true;
    }

    private boolean shouldCancelBorder(Player player, org.bukkit.Location location) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null || plugin.isBorderBypass(player) || houses.isInsideBuildBorder(location)) return false;
        player.sendMessage(plugin.color("&cYou cannot build outside this house border."));
        return true;
    }

    private boolean shouldCancelBuild(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) return false;
        if (plugin.canBypassHouseMode(player)) return false;
        if (house.getOwner().equals(player.getUniqueId())) return !house.isBuildMode();
        if (!house.isBuildMode()) return true;
        return !house.hasGroupPermission(player.getUniqueId(), "build");
    }

    private void sendBuildDenied(Player player, House house) {
        if (house != null && !house.isBuildMode() && house.getOwner().equals(player.getUniqueId())) {
            sendBuildModePrompt(player);
            return;
        }
        player.sendMessage(plugin.color("&cYou cannot interact with blocks in another player's house."));
    }

    @SuppressWarnings("deprecation")
    private void sendBuildModePrompt(Player player) {
        TextComponent message = new TextComponent("");
        for (net.md_5.bungee.api.chat.BaseComponent component : TextComponent.fromLegacyText(
                plugin.color("&cYou aren't in build mode! Want to build or break blocks? "))) {
            message.addExtra(component);
        }
        TextComponent click = new TextComponent(TextComponent.fromLegacyText(plugin.color("&b&lClick Here")));
        click.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/housing buildmode owner"));
        click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                TextComponent.fromLegacyText(plugin.color("&eClick to switch this house to Build Mode."))));
        message.addExtra(click);
        for (net.md_5.bungee.api.chat.BaseComponent component : TextComponent.fromLegacyText(
                plugin.color(" &cto switch to Build Mode!"))) {
            message.addExtra(component);
        }
        player.spigot().sendMessage(message);
    }

    private boolean shouldCancelBlockInteraction(Player player) {
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house == null) return false;
        if (plugin.canBypassHouseMode(player)) return false;
        if (house.getOwner().equals(player.getUniqueId())) return !house.isBuildMode();
        if (!house.isBuildMode()) return true;
        return !house.hasGroupPermission(player.getUniqueId(), "build");
    }

    private boolean isAbelixStaff(Player player) {
        return plugin.getRankPermissionService() != null && plugin.getRankPermissionService().isHousingStaff(player);
    }

    private boolean isLockedMenuSlot(Player player, InventoryClickEvent event) {
        int topSize = event.getView().getTopInventory().getSize();
        int slot = event.getRawSlot() - topSize;
        if (slot < 0 || slot >= player.getInventory().getSize()) return false;
        int ownerSlot = plugin.getStaticConfig().getInt("owner-menu.item-slot", 8);
        House house = houses.getHouseByWorld(player.getWorld().getName());
        if (house != null && (slot == ownerSlot || slot == 8)) return true;
        return houses.isLobbyWorld(player.getWorld().getName()) && houses.isLobbyItemSlot(slot);
    }

    private boolean isHousingMenuItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String name = item.getItemMeta().getDisplayName();
        if (!name.contains("Housing Menu") && !name.contains("Guest Menu") && !name.contains("Players")) return false;
        return item.getType() == net.fakepixel.housing.util.Compat.mat("NETHER_STAR") || item.getType().name().contains("DOOR");
    }

    private boolean containsLobbySlot(InventoryDragEvent event, int topSize) {
        if (!houses.isLobbyWorld(event.getWhoClicked().getWorld().getName())) return false;
        for (Integer rawSlot : event.getRawSlots()) {
            int slot = rawSlot - topSize;
            if (slot >= 0 && houses.isLobbyItemSlot(slot)) return true;
        }
        return false;
    }

    private void refreshLockedItems(Player player) {
        houses.updateVisitItems(player);
        if (houses.isLobbyWorld(player.getWorld().getName())) houses.giveLobbyItems(player);
    }
}
