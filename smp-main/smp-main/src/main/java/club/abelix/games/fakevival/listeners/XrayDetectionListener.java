package club.abelix.games.fakevival.listeners;

import club.abelix.games.fakevival.managers.XrayDetectionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class XrayDetectionListener implements Listener {
    private final XrayDetectionManager xrayDetectionManager;

    public XrayDetectionListener(XrayDetectionManager xrayDetectionManager) {
        this.xrayDetectionManager = xrayDetectionManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        xrayDetectionManager.handleOreBreak(event.getPlayer(), event.getBlock());
    }
}
