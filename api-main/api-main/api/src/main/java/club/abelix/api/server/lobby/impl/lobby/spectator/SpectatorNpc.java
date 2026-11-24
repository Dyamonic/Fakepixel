package club.abelix.api.server.lobby.impl.lobby.spectator;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.core.CoreUtil;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.api.server.lobby.impl.lobby.gui.spectate.ArenaSpectateGui;
import club.abelix.commons.data.permission.Group;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;

@RequiredArgsConstructor
public class SpectatorNpc {

    private static final Skin SPECTATE_NPC_SKIN = new Skin(
            "",
            "",
            "ewogICJ0aW1lc3RhbXAiIDogMTY1MDQzOTE2NDc5MCwKICAicHJvZmlsZUlkIiA6ICI5ZWU3NTUxOGQyZWE0Y2Q4OGJiNGI1YTZkNmVhNTFjYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJNaWNyb3MxMTgyIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJiYTI0YjYwMDA1YzI1YWIzMDFmMzAwMThiNGI4MzIyZDgwYmJjZjVlZDFhN2ExNTA0NWM1ZjU1OTEwZWQ4ZjMiCiAgICB9CiAgfQp9",
            "Piml1wnRuRlgUcUEqphzSnVtencWWWGNsiqbq3eEFc9+4NfeQDPEl32ALWMYLXk4iOZv8GTo7Q0uPrPnYYu1lYrPYgH9N9I7z1S75qZJqO3RSvDLyI0Oc/OKDSXbMpg/+Loorf4OEaMhlKpt3L7UrnPfDm1D1z4xAuvY3aw1nWtLv36A9k7l1Il52Aljtn4l6e9HZmzH4vnkO/3Qc+vYNw5pgRtzx0fPtK/xVSWHHPnO5VYAKWg3OKAWpn1YQUVQIBlGKLgfBsh6PF0irfG1e7vW5LEK04RQ6bHqfjxm8L8M9UxYX5t+KpUDhhBtDbZouHD/yNklU8cBdOEAuvmUUvOKN+W0UKHW/VnWkuYaa1m0uWFBZySfeqectLDSw4III4k/LGKDNC8nvCuYnbqVNRjmafMU1EIywzIYmmaWmy5eCttaTAwCeqCrFs22ilap0Sh08hXQfBmixCiHSvCMUq29SHVB+6Wz5u7twjqhGcJK+bVS5QUER8AsMe+vlgThrk7NSxletRj+1/FI9Ce9xpKgg9rib+oDA/HC6oCKX9NNt1+rrKMeluLZR1scvKZ2lup8poTPc03eqIGSLRqyQib24USPPOr1LyWp8shT8hHhJsQv+pgn6VRlpiPgX0W7uB3OgQUkxr74UQsdCICPZEZSxe4bpCIFZ3FclneNQhs="
    );

    private final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

    private final Location location;

    public void create() {
        FakeEntityPlayer fakePlayer = fakeEntityApi.createPlayer(location, SPECTATE_NPC_SKIN);
        fakePlayer.setPublicStatus(true);
        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(player -> {
            val gamer = Gamer.getGamer(player);

            if (!gamer.getRanks().hasPermission("game.spectate")) {
                gamer.sendLocalizedMessage("spectate_unavailable_no_permission");
                return;
            }

            new ArenaSpectateGui(player, CoreUtil.getMaster().getMinis());
        });

        Hologram spectateHologram = new Hologram(location.clone().add(0, 1.0, 0));
        spectateHologram.setPublicStatus(true);
        spectateHologram.addTextLine(Lang.of("spectate_npc_hologram_title").compile());
        spectateHologram.addTextLine(Lang.of("spectate_npc_hologram_action").compile());
    }
}
