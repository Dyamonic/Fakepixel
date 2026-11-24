package club.abelix.api.server.lobby.warmode;

import club.abelix.api.AbstractPlugin;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.hotbar.manager.HotbarManager;
import club.abelix.api.base.hotbar_new.HotbarService;
import club.abelix.api.base.util.task.AbstractTask;
import club.abelix.api.server.lobby.BaseLobbyServer;
import club.abelix.api.server.lobby.listener.LobbyProtectionListener;
import club.abelix.api.server.lobby.warmode.command.WarModeCommand;
import club.abelix.api.server.lobby.warmode.event.WarModeStopEvent;
import club.abelix.api.server.lobby.warmode.listener.WarModeListener;
import club.abelix.commons.language.Language;
import club.abelix.core.bukkit.HeartBeat;
import club.abelix.core.bukkit.StarshipBukkit;
import club.abelix.core.bukkit.connection.Connection;
import club.abelix.di.platform.bukkit.DiBukkitPlatform;
import club.abelix.network.packet.play.client.CBroadcastStaff;
import lombok.NonNull;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * @author pro0xy
 * <p>Created on 03.09.2023 at 15:09</p>
 */

public class WarModeManager {

    public static final WarModeManager INSTANCE = new WarModeManager();

    private boolean warMode = false;

    private AbstractPlugin plugin;

    public void register(@NonNull BaseLobbyServer server){
        this.plugin = server.getPlugin();
        new WarModeListener(server);

        this.plugin.getCommandManager().addCommand(WarModeCommand.class);
    }

    public void startWar(@NonNull Gamer gamer) {
        CBroadcastStaff packet = new CBroadcastStaff(
                CBroadcastStaff.GroupWrapper.HELPER.getValue(),
                Language.defaultLanguage().getMessage("staff_war_mode_started", "%admin%", gamer.getRealDisplayName(), "%server%", gamer.getServer())
        );

        Connection.sendPacket(packet);

        LobbyProtectionListener.setWarMode(true);
        warMode = true;

        Player adminPlayer = gamer.getPlayer();
        Location location = adminPlayer.getLocation();
        World world = location.getWorld();

        world.strikeLightningEffect(location);
        world.setTime(13000);

        DiBukkitPlatform.getFactory().getBeanService().withBean(HotbarService.class, (hotbarService ->
                hotbarService.clear(gamer)));

        for (Gamer target : Gamer.getGamers()){
            Player player = target.getPlayer();

            giveKit(player);

            target.sendLocalizedTitle(0, 50, 3, "war_mode_start_title", "war_mode_start_subtitle", "%admin%", gamer.getRealDisplayName());
        }

        new AbstractTask(this.plugin){
            @Override
            protected void run() {
                endWar();
            }
        }.later((5 * 60) * 20);
    }

    public void endWar() {
        LobbyProtectionListener.setWarMode(false);
        warMode = false;

        HeartBeat.callEvent(new WarModeStopEvent());
    }

    public boolean getWarMode() {
        return warMode;
    }

    public void giveKit(Player player){
        PlayerInventory playerInventory = player.getInventory();

        playerInventory.clear();

        playerInventory.setItem(0, new ItemStack(Material.WOOD_SWORD));
        playerInventory.setItem(1, new ItemStack(Material.BOW));
        playerInventory.setItem(2, new ItemStack(Material.IRON_PICKAXE));
        playerInventory.setItem(3, new ItemStack(Material.COBBLESTONE, 16));
        playerInventory.setItem(4, new ItemStack(Material.BREAD, 4));
        playerInventory.setItem(5, new ItemStack(Material.ARROW, 32));

        playerInventory.setArmorContents(new ItemStack[]{
                new ItemStack(Material.LEATHER_BOOTS),
                new ItemStack(Material.IRON_LEGGINGS),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE)
        });

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setAllowFlight(false);
        player.getActivePotionEffects().clear();
    }
}
