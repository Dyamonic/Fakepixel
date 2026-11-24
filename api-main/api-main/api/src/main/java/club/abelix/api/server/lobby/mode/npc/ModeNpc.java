package club.abelix.api.server.lobby.mode.npc;

import club.abelix.api.base.util.hologram.Hologram;
import club.abelix.api.nms.entity.type.FakeLivingEntity;
import club.abelix.commons.data.skin.Skin;
import club.abelix.network.online.query.StatusQueryResult;
import club.abelix.network.type.mode.ServerMode;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface ModeNpc {

    void createEntities(
            @NotNull Skin skin,
            @NotNull Location location
    );

    void addLabyEmote(int animation);

    @NotNull
    FakeLivingEntity getEntity();

    @NotNull
    Hologram getHologram();

    @NotNull
    ServerMode getMode();

    void updateOnlineStatus(
            @NotNull StatusQueryResult result
    );

}
