package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.live.LiveControlManager;
import club.abelix.games.fakevival.util.CC;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Map;

public final class LiveControlListener implements Listener {
    private final LiveControlManager liveControls;
    private final Map<String, String> systemCommands = Map.ofEntries(
            Map.entry("bazaar", "bazaar"),
            Map.entry("bz", "bazaar"),
            Map.entry("market", "bazaar"),
            Map.entry("guild", "guilds"),
            Map.entry("guilds", "guilds"),
            Map.entry("dungeon", "dungeons"),
            Map.entry("battlepass", "battlepass"),
            Map.entry("bp", "battlepass"),
            Map.entry("lootbox", "loot-boxes"),
            Map.entry("lootboxes", "loot-boxes"),
            Map.entry("quests", "quests"),
            Map.entry("quest", "quests"),
            Map.entry("store", "shops"),
            Map.entry("shop", "shops"),
            Map.entry("auctionhouse", "auctionhouse"),
            Map.entry("ah", "auctionhouse")
    );
    private final Map<String, String> guiCommands = Map.ofEntries(
            Map.entry("menu", "main-menu"),
            Map.entry("mainmenu", "main-menu"),
            Map.entry("skills", "skills"),
            Map.entry("jobs", "jobs"),
            Map.entry("bazaar", "bazaar"),
            Map.entry("bz", "bazaar"),
            Map.entry("market", "bazaar"),
            Map.entry("quests", "quests"),
            Map.entry("quest", "quests")
    );

    public LiveControlListener(LiveControlManager liveControls) {
        this.liveControls = liveControls;
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (liveControls.isMaintenanceEnabled() || liveControls.isJoiningBlocked()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    liveControls.isMaintenanceEnabled() ? liveControls.maintenanceMessage() : liveControls.joiningBlockedMessage());
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.length() <= 1) return;
        String command = message.substring(1).split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        String system = systemCommands.get(command);
        if (system != null && liveControls.isSystemDisabled(system)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cThat SMP system is temporarily disabled."));
            return;
        }
        String gui = guiCommands.get(command);
        if (gui != null && liveControls.isGuiDisabled(gui)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(CC.color("&cThat SMP GUI is temporarily disabled."));
        }
    }
}
