package club.abelix.api.server.lobby.tutorial;

import club.abelix.api.base.camera.PlayerCameraManager;
import club.abelix.api.base.camera.follow.FollowCamera;
import club.abelix.api.base.camera.follow.FollowCameraProperties;
import club.abelix.api.base.util.book.PagedBook;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import lombok.Getter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

@Getter
public class LobbyTutorial {

    private final List<LobbyTutorialSegment> segments = new ArrayList<>();

    private final FollowCameraProperties properties = FollowCameraProperties.newProperties();

    private final PagedBook tutorialBook;

    public LobbyTutorial(Plugin plugin, ConfigurationSection section) {
        PlayerCameraManager.IMP.registerListeners(plugin);

        TIntList speed = new TIntArrayList();
        ConfigurationSection tutorialSection = section.getConfigurationSection("Segments");

        for (String key : tutorialSection.getKeys(false)) {
            LobbyTutorialSegment segment = new LobbyTutorialSegment();
            segment.loadSegment(tutorialSection.getConfigurationSection(key));

            speed.add(segment.getDuration());

            properties.locations(segment.getFollowingLocations());
            segments.add(segment);
        }

        properties.speed(speed);

        TextComponent component = new TextComponent("Приветствуем на AgeraPvP! У вас есть вопросы по серверу? " +
                "Посмотрите небольшой туториал и начинайте свою историю!\n\n ");

        TextComponent acceptComponent = new TextComponent("§2[Посмотреть туториал]");
        acceptComponent.setBold(true);
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tutorial"));
        acceptComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{
                                new TextComponent("§fНажмите, чтобы посмотреть туториал")
                        }
                )
        );

        TextComponent rejectComponent = new TextComponent("\n\nНе нужна помощь? Просто закройте книгу и начинайте игру!");

        tutorialBook = new PagedBook();
        tutorialBook.addPage().addComponents(component, acceptComponent, rejectComponent);

        String tutorialNpcLocationString = section.getString("Location");

        if (tutorialNpcLocationString != null) {
            Location tutorialNpcLocation = new Location(Bukkit.getWorld("fp_hub"), -55, 129, -23, 40, 0);

            ConfigurationSection skinSection = section.getConfigurationSection("Skin");

            if (skinSection != null) {
                LobbyTutorialNpc lobbyTutorialNpc = new LobbyTutorialNpc(tutorialNpcLocation, this);
                lobbyTutorialNpc.create();
            }
        }
    }

    public void openBook(Player player) {
        tutorialBook.openBook(player);
    }

    public void startTutorial(Player player) {
        FollowCamera camera = new LobbyTutorialCamera(this, player);
        PlayerCameraManager.IMP.addCamera(camera);
    }

}
