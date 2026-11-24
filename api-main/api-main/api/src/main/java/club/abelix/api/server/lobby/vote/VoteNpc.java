package club.abelix.api.server.lobby.vote;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.base.vote.gui.VoteGui;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;

@RequiredArgsConstructor
public class VoteNpc {

    private static final Skin VOTE_NPC_SKIN = new Skin(
            "vote_npc",
            "",
            "ewogICJ0aW1lc3RhbXAiIDogMTcxNDk0ODU1ODMzMCwKICAicHJvZmlsZUlkIiA6ICI4ZGUyNDAzYTEyMjU0ZmFkOTM1OTYxYWFlYmQwNGUyOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJkZXNydHB1bWEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTBjOGIwODUxYTJmMDkzZWZkMmFlMjFiOGZlZTUyMDUwODZhMTc4MjgzZTQ3NDNiZjlmY2ZmNzRiOWNmYmFiMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
            "wrtI98jXmo+MtU2G6VyUsaLs6YyXDofuR11iNm5Xf5pzRXbOFFcllaFYmXVgxS9dBZYfq7kXcXJYiUYFH+EL9QQEFjFO9BeePn8/5Ks5rf7zhRxsE2dS7qdQ/LrT8Ri/iaeZdYCUvwI3QLDn7ng9rcc9f6/vvfxKyTlgzt0yEeyyVBiNBf4hE5JOYVyYi0bw95XKik+ciHWDvsTNcfWBaIT7Iw20Bsa9Lvxi/KBm008onk9VSn9m5D+zseCPmVURCbFuwbE+B+vMKOyHUmpI8zH6YqH5drcFCpZKkg6cBvqiJ26MGHHnx5WiKCTnI4NOQNpSAKNxb6CWM4QCR4iSeLZn71cOKAGYymAv7kPijB0uzIoNbA55zzAkjasuMTAsXFSWv4kwjZgEUR/t+Geb+EdhBaE4bueBgYFbNJPLhFIbzLYHF4YTu9EWE91rNf0kA1514lKWYagN4k2LPZc62tz6/4qjklWJ+7U2L/jb8118FdT4oQS7wIj5mrB7i9XWR2j3xzQ4a/GIDmru9s7kTD4HZ6tz6GrGo/1xwLYNsuk8jtavi+ANBRtVZjp9pUZ8yxlALmcRnvCdZVv2cY0x4FIt1kAe85ZXa/VeQzIRCZN8KCcRnGdqhMDge+/A157QAsLQCe/raB73eqoDkNtO8aJfv61fPvCYHssCCabaRnU="
    );

    private final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

    private final Location location;

    public void create() {
        FakeEntityPlayer fakePlayer = fakeEntityApi.createPlayer(location, VOTE_NPC_SKIN);
        fakePlayer.setPublicStatus(true);
        fakePlayer.setGlowing(true);
        fakePlayer.setGlowingColor(ChatColor.GOLD);
        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(VoteGui::new);

        FalconAPI.getInstance().getLabyNpcEmotesService().addNPCEmotes(fakePlayer, 92);

        Hologram hologram = new Hologram(location.clone().add(0, 1.0, 0));
        hologram.setPublicStatus(true);
        hologram.addTextLine(Lang.of("votes_npc_hologram_title").compile());
        hologram.addTextLine(Lang.of("votes_npc_hologram_action").compile());
    }
}