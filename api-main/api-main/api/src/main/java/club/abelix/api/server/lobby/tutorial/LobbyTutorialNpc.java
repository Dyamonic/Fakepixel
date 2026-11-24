package club.abelix.api.server.lobby.tutorial;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import lombok.NonNull;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.util.UUID;


public class LobbyTutorialNpc {

    private final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

    private final LobbyTutorial tutorial;

    private final Location location;
    private final Skin skin;

    public LobbyTutorialNpc(@NonNull Location location, @NonNull LobbyTutorial tutorial) {
        this.tutorial = tutorial;
        this.location = location;
        this.skin = new Skin("test", UUID.randomUUID().toString(), "ewogICJ0aW1lc3RhbXAiIDogMTY2ODI3OTcyNTkwOCwKICAicHJvZmlsZUlkIiA6ICIxZGQ4YmU5YTljMDc0OGU4OTZjZjgwNDNjNTMxMWEzNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJHaWdhQ2F0VG95IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzNjY2RmNTE5NDcwMjhhMDIyNDk5YjY5NjRhMTZjNzY4NWFjNmRmNTljOGM5NDEwYTY1NTMzOTRkNzYwYWU3ZDIiCiAgICB9CiAgfQp9", "JNEmcxbuHNmSBD/uLsiPef2wFsN3Aq0+WWnpGwzkadOvRbXcCnHfB2OcBzzbPAlb3vzgGWQsJcMgf4GPcFKnUsV3guwA5KOP3PtbbXrM3e9htdPlCtP6+IwFewZBFRV8Hp6gHh0zgxyr8RIiInmILPKTwE5TPMVoE9UzCIC6uvzcXf1Tbq61JNpPm/CnOlj77BJwd7xvpPt4bMFWhq7jWTCMg1qLCVgVms0MIFiSYRXHffHqXVDnfhjVbWyZ3WrYJyrCldWiHaZ/h6zeaWYix4XCCw4MqlGFlnWMmQy1MnthoczliDkuRN0R54a5nRUbENL3j5x0289c+l1uvilWeWCVZBu+xt7s6ivcCpy/mzIqEGCx/ERIs3djpLdqnNigxDq6VV+vKnJlfKqHextbMqhUfp+F1HCx37MAzhVvFlatuLQfO9jEsJ2tsbTrVtTTCUpbJLrv4HnOpjRuy+gW3/7PokVMmrdBe+tGHzXa9o1jBJR2o7GSbFElJBpMcdkmDYHuRRsGJIcWnYs1slNl9r7Od0Sdkr6LIoxRel7D1XQ6sHzJ9LmbyEhlRWTMwE+tT9kSx0tmfOjtVPXl1GPVpgluRlW05b0Dn9hh2hdXaCrUf+F7lbSIAfNPko6f4/ZTcOm83bU2pJGgkUmT0e4KOO491eMsFnHsu04iBBlbvck=");
    }

    public void create() {
        FakeEntityPlayer player = fakeEntityApi.createPlayer(location, skin);
        player.setGlowing(true);
        player.setGlowingColor(ChatColor.GREEN);
        player.startPlayerLooker(2);
        player.setClickAction(tutorial::startTutorial);
        player.setPublicStatus(true);

        Hologram hologram = new Hologram(location.clone().add(0, 1.0, 0));
        hologram.setPublicStatus(true);
        hologram.addTextLine(Lang.of("tutorial_npc_hologram_title").compile());
        hologram.addTextLine(Lang.of("tutorial_npc_hologram_action").compile());
    }
}
