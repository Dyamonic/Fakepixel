package club.abelix.api.server.lobby.mysterybox.listener;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.server.lobby.mysterybox.MysteryBox;
import club.abelix.api.server.lobby.mysterybox.MysteryBoxStorage;
import club.abelix.api.server.lobby.mysterybox.anim.AbstractCaseOpener;
import club.abelix.api.server.lobby.mysterybox.gui.CaseListGui;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class MysteryBoxListener extends YListener {

    public MysteryBoxListener(AbstractPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        for (MysteryBox mysteryBox : MysteryBoxStorage.getMysteryBoxes()) {
            AbstractCaseOpener opener = mysteryBox.getCaseOpener();

            if (opener != null) {
                opener.playerJoined(gamer);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENDER_CHEST) {
            return;
        }

        MysteryBox mysteryBox = MysteryBoxStorage.getMysteryBox(block.getLocation());
        if (mysteryBox != null) {

            event.setCancelled(true);

            new CaseListGui(plugin, event.getPlayer(), mysteryBox);
        }
    }

}
