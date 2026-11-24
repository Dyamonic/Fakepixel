package club.abelix.api.server.lobby.impl.lobby.quest;

import club.abelix.api.FalconAPI;
import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.villager.FakeEntityVillager;
import club.abelix.api.nms.entity.type.villager.VillagerProfession;
import club.abelix.commons.data.skin.Skin;
import club.abelix.commons.language.Lang;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;


@RequiredArgsConstructor
public class QuestsNpc {

    private final FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

    private final Location location;

    public void create() {
        val fakeVillager = fakeEntityApi.createPlayer(location,
                new Skin("", "",
                        "ewogICJ0aW1lc3RhbXAiIDogMTYxNzEyMjg5ODEyMywKICAicHJvZmlsZUlkIiA6ICI5ZDQyNWFiOGFmZjg0MGU1OWM3NzUzZjc5Mjg5YjMyZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUb21wa2luNDIiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmYwMGQyYzc3ODE0ZjhlNGJmNGRmNGFlNzhmNDlhYTRlNTg5MTM1NjU4Y2U2YjkzODU5MDg2N2ViZmRkNzYxOSIKICAgIH0KICB9Cn0=",
                        "o/zUz3zw0EaZCRl9uYjZmP+AwuKyRayx51wCPAzX+ME+zVP01QXBo0HDU/8rKzWFuI7VS1zt4hGZDAMiIWga7ijpBa1FXXAOyrkhAGf/oQ4rsDaozUnvkhTi5669U7UyubSpDGNB+66PS5D4Gc7PZeng5e/BkgrenbvdzFhiwaDk2JxpO/nLcizlScr5meJtQ/KDutL7sFcDEck5ouVVP64YTI8H1n+hcRnxo6enV5XsKfOMljMkv2kBO3TjMuj6JokyA7KwVjNdKwLrzHp5NsnKDwp7Uuz4VEDPPKSUE5ywPoSHnZ+lpygXeT2bNQhRd5eCjrMgFkXNX0NA0v4vRrK32o7JxC2uVc+MjZWHUGwwsziEy/wwyydPkLYFU4+I+sEH1FMtIixLcliLqHFSkIkp9GxFIe0Mx2GWayHhbbIKvRm5Yk6b5SJNWdJF+BEGAQow4WSBOgmKIbOw84SV9djvpnU+GNagTDZBnq+nj7CIjiTrYBdQIfbPzWDvmQuCVMWyKJ8Qj0eTmUbKg7EYk8QL3akNuG6cmfxIwkQsH1fWTUZlWqFt6EZaWWeM9nZGUTZvFuMOYe2e4H/Y3V5tM6mARao2dy8XXf0oYd0Qil68vS0zto5Ja2Lht/9X7D5+gWCNunx69Pdb7bIrfbPQJ2m5osFJ69IceMLva2iYdco="
                ));
        fakeVillager.startPlayerLooker(2);
        fakeVillager.setClickAction(player -> player.chat("/quests"));
        fakeVillager.setPublicStatus(true);

        Hologram questHologram = new Hologram(location.clone().add(0, 1.5, 0));
        questHologram.addTextLine(Lang.of("quest_npc_hologram_title").compile());
        questHologram.addTextLine(Lang.of("quest_npc_hologram_action").compile());
        questHologram.setPublicStatus(true);
    }
}
