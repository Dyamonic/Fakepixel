package club.abelix.api.server.lobby.reward;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.gamer.Gamer;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.player.FakeEntityPlayer;
import club.abelix.commons.data.dailyrewards.DailyRewardEntry;
import club.abelix.commons.data.dailyrewards.DailyRewardSqlHandler;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import club.abelix.commons.util.time.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public class DailyRewardNpc {

    private static final Skin DAILY_NPC_SKIN = new Skin(
            "",
            "",
            "ewogICJ0aW1lc3RhbXAiIDogMTYyMTk5MDU5MjUyMSwKICAicHJvZmlsZUlkIiA6ICI0NWY3YTJlNjE3ODE0YjJjODAwODM5MmRmN2IzNWY0ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJfSnVzdERvSXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGYwOWUwZWRjZjViNjY5Y2ZmZGY2NmNmYmUyNDJkZWJlYTI3NDBkZmUyMzM5YTRjNzQwYWFhNDUzZTZlZTkzNSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
            "cEiHF0QZ0XwQQa8hA/B4Mc20xbZMCbgHxxsiAeK+2nEdzZU7AAZ2EG7tzBGg8I8iGeoTQWqWwArKSX+/x/lhYZ04VCfnTAUgikukdB+t4aCEZwneHCif12J9J8ZYZyAgweSZ5mrFx+WsnQ0WHqDy6IamFWe+YTS2Z4Mkm+F2DEBYvwHFqdYT5Tnv8jg3WilcNuTEHnoeuoHMnu6SrWnxOvC/8zVLPlF4sTKlq/W2sJMh4/5AFtCtdq+Cpl+TxiDzlqspz169n9jZtiHiKPDnz/lTiiCd+NBwowEK+lDIk4z0zpJGwbPd0eFZ2fpjcjAK82ovRMppT1V+EGvfS1NWiBPig0yk+OVJodYk2oEu8w2eVe8tUa5BzVtbSonOQPvBh6bNgHoHtUjV7Bf6N9cQ/xrLxtta35DrFFHPZW/xJfJAqvRkaHMaT0fhvSZ7/kt2WPzI3/v16O22ilnE58SDwJQRTYwTXsmhJBzm1Ezt+MotLqR1qPmsWgkFt2pPv6t4OtHnEynFTXcRiJbUi0Fkuc86zBJR4HTrtbcISMNqY5g0p/nG7/Ki55HIFZMvzcZtUNAvyc+z22grFtoioYfdf5UxSzP2Pqbg2KHDkYB+y2XS0Wq8HBXE8rhKo/s1lF3YjlEkYbecN0HLJ9pTZYjQBjKm6DVL4FD4j1jY6VGHG3g="
    );

    private final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

    private final Location location;

    public void create() {
        FakeEntityPlayer fakePlayer = fakeEntityApi.createPlayer(location, DAILY_NPC_SKIN);

        fakePlayer.startPlayerLooker(2);
        fakePlayer.setClickAction(player -> DailyRewardsGui.open(Gamer.getGamer(player)));
        fakePlayer.setPublicStatus(true);

        Hologram hologram = new Hologram(location.clone().add(0, 1, 0));
        hologram.setPublicStatus(true);
        hologram.addTextLine(Lang.of("rewards_npc_hologram_title").compile());
        hologram.addTextLine(Lang.of("rewards_npc_hologram_action").compile());
    }


//    private boolean hasUnclaimedRewards(Gamer gamer) {
//        DailyRewardEntry entry = DailyRewardSqlHandler.INSTANCE.loadPlayerInfo(gamer.getPlayerId()).join();
//        Instant now = Instant.now();
//        Instant lastRewardDate = Instant.ofEpochMilli(entry.getLastRewardTimestamp());
//        long daysSinceLastReward = ChronoUnit.DAYS.between(lastRewardDate, now);
//
//        return daysSinceLastReward >= 3;
//    }

}

