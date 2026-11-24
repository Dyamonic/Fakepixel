package club.abelix.api.base.playertop.listener;

import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.gamer.event.GamerLanguageChangeEvent;
import club.abelix.api.base.listener.YListener;
import club.abelix.api.base.playertop.storage.PlayerTopStorage;
import club.abelix.api.base.playertop.type.PlayerTop;
import club.abelix.core.bukkit.HeartBeat;
import lombok.NonNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class PlayerTopListener extends YListener {

    public PlayerTopListener(@NonNull Plugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        HeartBeat.runLaterSync(
                () -> PlayerTopStorage.IMP.getRegisteredTops()
                        .forEach(playerTop -> playerTop.showTo(gamer)), 30
        );
    }

    @EventHandler
    public void onLanguageChange(GamerLanguageChangeEvent event) {
        Gamer gamer = event.getGamer();

        for (PlayerTop<?> playerTop : PlayerTopStorage.IMP.getRegisteredTops()) {
            playerTop.hideFrom(gamer);
            playerTop.showTo(gamer);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Gamer gamer = Gamer.getGamer(event.getPlayer());

        PlayerTopStorage.IMP.getRegisteredTops()
                .forEach(playerTop -> playerTop.hideFrom(gamer));
    }


}
