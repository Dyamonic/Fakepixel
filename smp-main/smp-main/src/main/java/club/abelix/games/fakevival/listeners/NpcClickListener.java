package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.Game;
import club.abelix.games.fakevival.managers.NpcManager;
import club.abelix.games.fakevival.managers.QuestManager;
import club.abelix.games.fakevival.managers.TablistManager;
import club.abelix.games.fakevival.menus.BankMenu;
import club.abelix.games.fakevival.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class NpcClickListener implements Listener {
    private final JavaPlugin plugin;
    private final NpcManager npcManager;
    private final TablistManager tablistManager;
    private final QuestManager questManager;
    private final club.abelix.games.fakevival.managers.MenuManager menuManager;
    private final BankMenu bankMenu;
    private final java.util.Map<java.util.UUID, Long> lastClick = new java.util.HashMap<>();

    public NpcClickListener(JavaPlugin plugin, NpcManager npcManager, TablistManager tablistManager, QuestManager questManager, club.abelix.games.fakevival.managers.MenuManager menuManager, BankMenu bankMenu) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.tablistManager = tablistManager;
        this.questManager = questManager;
        this.menuManager = menuManager;
        this.bankMenu = bankMenu;
    }

    @EventHandler
    public void onNpcClick(PlayerInteractAtEntityEvent event) {
        handleClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler
    public void onNpcEntityClick(PlayerInteractEntityEvent event) {
        handleClick(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler
    public void onNpcAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        handleClick(player, event.getEntity(), event);
    }

    private void handleClick(Player player, Entity entity, org.bukkit.event.Cancellable event) {
        if (!club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getBoolean("npcs.enabled", true)) return;

        String id = npcManager.getNpcId(entity);
        if (id == null && entity.getCustomName() == null) return;

        ConfigurationSection section = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("npcs.talking");
        String key = null;
        if (section != null) key = id != null && section.contains(id) ? id : findNpcKeyByName(entity, section);
        if (key == null && id != null) key = id;
        if (key == null) return;
        if (plugin instanceof Game game && (game.getLiveControlManager().isSystemDisabled("npcs") || game.getLiveControlManager().isNpcDisabled(key))) {
            player.sendMessage(CC.color("&cThat NPC is temporarily disabled."));
            return;
        }

        long now = System.currentTimeMillis();
        Long last = lastClick.get(player.getUniqueId());
        if (last != null && now - last < 350L) return;
        lastClick.put(player.getUniqueId(), now);

        event.setCancelled(true);
        String normalizedKey = key.toLowerCase();
        String normalizedId = id == null ? null : id.toLowerCase();
        if (normalizedId != null && handleCoreNpcAction(player, normalizedId)) return;
        if (normalizedId == null || !normalizedId.equals(normalizedKey)) {
            if (handleCoreNpcAction(player, normalizedKey)) return;
        }
        if (questManager != null && questManager.handleNpcQuest(player, normalizedKey)) return;
        if (handleShopNpc(player, normalizedKey)) return;
        if (section == null || !section.contains(key)) return;
        for (String line : section.getStringList(key + ".messages")) player.sendMessage(tablistManager.parse(player, line));
        for (String cmd : section.getStringList(key + ".commands")) runNpcCommand(player, cmd);
        playSound(player, section.getString(key + ".sound", "ENTITY_VILLAGER_YES"));
    }

    private boolean handleCoreNpcAction(Player player, String key) {
        String action = getAction(key);
        if (action.isBlank() || action.equalsIgnoreCase("talk") || action.equalsIgnoreCase("none")) {
            return false;
        }

        String lower = action.toLowerCase();
        if (plugin instanceof Game game && game.getMayorManager().handleNpcAction(player, lower)) {
            playSound(player, "ENTITY_VILLAGER_YES");
            return true;
        }
        if (lower.equals("auctionhouse") || lower.equals("auction_house") || lower.equals("ah")) {
            playSound(player, "ENTITY_VILLAGER_YES");
            menuManager.openAuctionHouse(player);
            return true;
        }
        if (lower.equals("bazaar") || lower.equals("bz")) {
            playSound(player, "ENTITY_VILLAGER_TRADE");
            menuManager.openBazaar(player);
            return true;
        }
        if (lower.equals("bank") || lower.equals("open_bank") || lower.equals("banker")) {
            playSound(player, "ENTITY_VILLAGER_TRADE");
            bankMenu.open(player);
            return true;
        }
        if (lower.equals("skills")) {
            playSound(player, "ENTITY_EXPERIENCE_ORB_PICKUP");
            menuManager.openSkills(player);
            return true;
        }
        if (lower.equals("jobs")) {
            playSound(player, "ENTITY_VILLAGER_YES");
            menuManager.openJobs(player);
            return true;
        }
        if (lower.equals("battlepass") || lower.equals("bp")) {
            playSound(player, "ENTITY_PLAYER_LEVELUP");
            menuManager.openBattlePass(player);
            return true;
        }
        if (lower.equals("adventureguild") || lower.equals("adventure_guild") || lower.equals("adventuresguild") || lower.equals("adventures_guild") || lower.equals("guild")) {
            playSound(player, "ENTITY_VILLAGER_CELEBRATE");
            menuManager.openAdventureGuild(player);
            return true;
        }
        if (lower.startsWith("shop:")) {
            String shopId = action.substring("shop:".length()).trim().toLowerCase(java.util.Locale.ROOT);
            if (shopId.isEmpty()) return false;
            if (!isActionShopAvailable(shopId)) {
                player.sendMessage(CC.color("&cUnknown shop: &e" + shopId));
                return true;
            }
            playSound(player, "ENTITY_VILLAGER_TRADE");
            openActionShop(player, shopId);
            return true;
        }
        if (lower.startsWith("player:") || lower.startsWith("console:") || lower.startsWith("command:") || lower.startsWith("cmd:")) {
            if (lower.startsWith("command:")) {
                runNpcCommand(player, "console:" + action.substring("command:".length()).trim());
            } else if (lower.startsWith("cmd:")) {
                runNpcCommand(player, "console:" + action.substring("cmd:".length()).trim());
            } else {
                runNpcCommand(player, action);
            }
            return true;
        }
        if (action.startsWith("/") || action.contains(" ")) {
            runNpcCommand(player, "console:" + action);
            return true;
        }
        return false;
    }

    private boolean handleShopNpc(Player player, String key) {
        ConfigurationSection shop = club.abelix.games.fakevival.config.RuntimeSettings.config(plugin).getConfigurationSection("npcs.shops." + key);
        if (shop == null) return false;
        for (String line : shop.getStringList("messages")) player.sendMessage(tablistManager.parse(player, line));
        playSound(player, shop.getString("sound", "ENTITY_VILLAGER_YES"));
        int delay = Math.max(1, shop.getInt("open-delay-ticks", 20));
        String shopId = shop.getString("shop", key).trim().toLowerCase(java.util.Locale.ROOT);
        if (!isActionShopAvailable(shopId)) {
            player.sendMessage(CC.color("&cUnknown shop: &e" + shopId));
            return true;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> openActionShop(player, shopId), delay);
        return true;
    }

    private String getAction(String key) {
        return npcManager.getEffectiveNpcAction(key);
    }

    private void openActionShop(Player player, String shopId) {
        String normalized = shopId == null ? "" : shopId.trim().toLowerCase();
        if (normalized.equals("castle_banker")) {
            bankMenu.open(player);
        } else if (normalized.equals("castle_guild_clerk")) {
            menuManager.openAdventureGuild(player);
        } else if (normalized.equals("castle_auction_master")) {
            menuManager.openAuctionHouse(player);
        } else if (normalized.equals("hayvillage_farming_collection_npc")) {
            player.performCommand("collections");
        } else if (java.util.Set.of("purple_wizard", "medieval_town_guard", "forest_nature_quest_npc", "cherry_spirit_quest_npc").contains(normalized)) {
            player.performCommand("quests");
        } else if (normalized.equals("desert_travel_npc")) {
            menuManager.openWarps(player, 0);
        } else if (normalized.equals("palm_dungeon_gatekeeper")) {
            menuManager.openDungeons(player);
        } else if (normalized.equals("bazaar") || normalized.equals("market") || normalized.equals("bz")) {
            menuManager.openBazaar(player);
        } else if (normalized.equals("event_vendor") || normalized.equals("event_shop")) {
            menuManager.openEventShop(player, 0);
        } else if (normalized.equals("furniture") && plugin instanceof club.abelix.games.fakevival.Game game) {
            game.getFurnitureManager().openShop(player, club.abelix.games.fakevival.furniture.FurnitureDefinition.Category.CHAIRS);
        } else if (normalized.equals("auctionhouse") || normalized.equals("auction_house") || normalized.equals("ah")) {
            menuManager.openAuctionHouse(player);
        } else if (normalized.equals("adventureguild") || normalized.equals("adventure_guild") || normalized.equals("guild")) {
            menuManager.openAdventureGuild(player);
        } else {
            if (!menuManager.shopExists(normalized)) {
                player.sendMessage(CC.color("&cUnknown shop: &e" + normalized));
                return;
            }
            menuManager.openShop(player, normalized);
        }
    }

    private boolean isActionShopAvailable(String shopId) {
        if (shopId == null) return false;
        String id = shopId.trim().toLowerCase(java.util.Locale.ROOT);
        return java.util.Set.of("bazaar", "market", "bz", "event_vendor", "event_shop", "furniture",
                        "auctionhouse", "auction_house", "ah", "adventureguild", "adventure_guild", "guild").contains(id)
                || club.abelix.games.fakevival.config.defaults.NpcShopDefaults.isSpecialMenu(id)
                || menuManager.shopExists(id);
    }

    private String findNpcKeyByName(Entity entity, ConfigurationSection section) {
        if (entity.getCustomName() == null) return null;
        String cleanName = ChatColor.stripColor(entity.getCustomName());
        for (String key : section.getKeys(false)) {
            String configured = ChatColor.stripColor(CC.color(section.getString(key + ".name", key)));
            if (cleanName.equalsIgnoreCase(configured)) return key;
        }
        return null;
    }

    private void runNpcCommand(Player player, String command) {
        String parsed = ChatColor.stripColor(tablistManager.parse(player, command).replace("%player%", player.getName()));
        if (parsed.toLowerCase().startsWith("player:")) {
            player.performCommand(stripLeadingSlash(parsed.substring("player:".length()).trim()));
            return;
        }
        if (parsed.toLowerCase().startsWith("console:")) parsed = parsed.substring("console:".length()).trim();
        parsed = stripLeadingSlash(parsed);
        if (parsed.isBlank()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
    }

    private String stripLeadingSlash(String command) {
        while (command.startsWith("/")) command = command.substring(1).trim();
        return command;
    }

    private void playSound(Player player, String sound) {
        try {
            Sound parsed = SoundUtil.parse(sound);
            if (parsed != null) player.playSound(player.getLocation(), parsed, 1F, 1F);
        } catch (Throwable ignored) {
        }
    }
}
